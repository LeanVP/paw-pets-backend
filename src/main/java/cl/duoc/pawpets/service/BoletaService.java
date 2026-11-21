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
    private final SecuenciaService secuencias;

    public BoletaService(BoletaRepository repositorio, ProductoRepository productos, SecuenciaService secuencias) {
        this.repositorio = repositorio;
        this.productos = productos;
        this.secuencias = secuencias;
    }

    public Boleta crear(BoletaRequest datos, Integer clienteId) {
        // Junta las líneas repetidas del mismo producto (por ejemplo, 2 + 1 = 3 unidades).
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        for (ItemBoletaRequest item : datos.items()) {
            cantidades.merge(item.productoId(), item.cantidad(), Integer::sum);
        }

        List<DetalleBoleta> detalle = new ArrayList<>();
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
            total += d.getSubtotal();
        }

        boolean aprobado = !MEDIO_RECHAZADO.equals(datos.medioPago());

        Boleta boleta = new Boleta();
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
        if (!aprobado) {
            return guardar(boleta);
        }
        Map<Integer, Integer> descontados = new LinkedHashMap<>();
        try {
            for (DetalleBoleta d : detalle) {
                // "Si stock >= cantidad, réstale cantidad" en UNA operación de MongoDB: si otro cliente
                // compró las últimas unidades un instante antes, no se descuenta y se avisa.
                if (!productos.descontarStock(d.getProductoId(), d.getCantidad())) {
                    throw new ConflictoException("Stock insuficiente para " + d.getNombre()
                            + ": otro cliente acaba de comprar las últimas unidades.");
                }
                descontados.put(d.getProductoId(), d.getCantidad());
            }
            return guardar(boleta);
        } catch (RuntimeException e) {
            // Compensación: si algo falla a mitad de camino, se devuelve el stock ya descontado
            descontados.forEach(productos::devolverStock);
            throw e;
        }
    }

    /**
     * Asigna el número de boleta recién al guardar (así una compra rechazada por stock no "gasta" un número)
     * con el contador atómico: dos compras simultáneas nunca reciben el mismo número.
     */
    private Boleta guardar(Boleta boleta) {
        boleta.setId(secuencias.siguiente("boletas",
                () -> repositorio.findTopByOrderByIdDesc().map(Boleta::getId).orElse(0)));
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
