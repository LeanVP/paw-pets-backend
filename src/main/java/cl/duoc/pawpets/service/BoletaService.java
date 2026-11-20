package cl.duoc.pawpets.service;

import cl.duoc.pawpets.dto.BoletaRequest;
import cl.duoc.pawpets.dto.ItemBoletaRequest;
import cl.duoc.pawpets.exception.ConflictoException;
import cl.duoc.pawpets.exception.RecursoNoEncontradoException;
import cl.duoc.pawpets.exception.SolicitudInvalidaException;
import cl.duoc.pawpets.model.Boleta;
import cl.duoc.pawpets.model.DetalleBoleta;
import cl.duoc.pawpets.model.Producto;
import cl.duoc.pawpets.model.Usuario;
import cl.duoc.pawpets.repository.BoletaRepository;
import cl.duoc.pawpets.repository.ProductoRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio de la compra (BOLETA + DETALLE_BOLETA).
 * El servidor busca cada producto, valida el stock, calcula precios, subtotales y total,
 * define el estado del pago y descuenta el stock. El navegador solo envía ids y cantidades.
 */
@Service
public class BoletaService {

    /** Medio de pago de prueba que simula un pago rechazado (igual que en el frontend). */
    public static final String MEDIO_RECHAZADO = "Tarjeta sin fondos (simula error)";

    private final BoletaRepository repositorio;
    private final ProductoRepository productos;

    public BoletaService(BoletaRepository repositorio, ProductoRepository productos) {
        this.repositorio = repositorio;
        this.productos = productos;
    }

    public Boleta crear(BoletaRequest datos, Integer clienteId) {
        // Junta las líneas repetidas del mismo producto (por ejemplo, 2 + 1 = 3 unidades).
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        for (ItemBoletaRequest item : datos.items()) {
            cantidades.merge(item.productoId(), item.cantidad(), Integer::sum);
        }

        List<DetalleBoleta> detalle = new ArrayList<>();
        List<Producto> comprados = new ArrayList<>();
        int total = 0;
        for (Map.Entry<Integer, Integer> linea : cantidades.entrySet()) {
            Producto producto = productos.findById(linea.getKey())
                    .orElseThrow(() -> new SolicitudInvalidaException(
                            "El producto " + linea.getKey() + " no existe."));
            int cantidad = linea.getValue();
            if (producto.getStock() < cantidad) {
                throw new ConflictoException("Stock insuficiente para " + producto.getNombre()
                        + ": quedan " + producto.getStock() + " unidades.");
            }
            DetalleBoleta d = new DetalleBoleta();
            d.setProductoId(producto.getId());
            d.setNombre(producto.getNombre());
            d.setPrecio(producto.precioFinal());
            d.setCantidad(cantidad);
            d.setSubtotal(producto.precioFinal() * cantidad);
            detalle.add(d);
            comprados.add(producto);
            total += d.getSubtotal();
        }

        boolean aprobado = !MEDIO_RECHAZADO.equals(datos.medioPago());

        Boleta boleta = new Boleta();
        boleta.setId(repositorio.findTopByOrderByIdDesc().map(b -> b.getId() + 1).orElse(1));
        boleta.setFecha(Instant.now());
        boleta.setClienteId(clienteId);
        boleta.setNombre(datos.nombre());
        boleta.setApellidos(datos.apellidos());
        boleta.setCorreo(datos.correo());
        boleta.setCalle(datos.calle());
        boleta.setRegion(datos.region());
        boleta.setComuna(datos.comuna());
        boleta.setIndicaciones(datos.indicaciones());
        boleta.setMedioPago(datos.medioPago());
        boleta.setItems(detalle);
        boleta.setTotal(total);
        boleta.setEstado(aprobado ? Boleta.PAGADA : Boleta.RECHAZADA);

        // Solo una compra pagada descuenta stock.
        if (aprobado) {
            for (Producto producto : comprados) {
                producto.setStock(producto.getStock() - cantidades.get(producto.getId()));
            }
            productos.saveAll(comprados);
        }
        return repositorio.save(boleta);
    }

    /** Admin y vendedor ven todas las boletas; un cliente solo las suyas. */
    public List<Boleta> listar(Integer usuarioId, String rol) {
        if (Usuario.CLIENTE.equals(rol)) {
            return repositorio.findByClienteIdOrderByIdAsc(usuarioId);
        }
        return repositorio.findAll(Sort.by("id"));
    }

    /** Un cliente que pide una boleta ajena recibe 404 (no se revela que existe). */
    public Boleta obtener(Integer id, Integer usuarioId, String rol) {
        Boleta boleta = repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La boleta " + id + " no existe."));
        if (Usuario.CLIENTE.equals(rol) && !usuarioId.equals(boleta.getClienteId())) {
            throw new RecursoNoEncontradoException("La boleta " + id + " no existe.");
        }
        return boleta;
    }
}
