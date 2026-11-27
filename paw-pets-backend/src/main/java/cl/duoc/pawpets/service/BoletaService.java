package cl.duoc.pawpets.service;

// DTOs de entrada: lo que manda el checkout (BoletaRequest) y cada línea del carrito (ItemBoletaRequest).
import cl.duoc.pawpets.dto.BoletaRequest;
import cl.duoc.pawpets.dto.ItemBoletaRequest;
// Excepciones de negocio: 409 (sin stock), 404 (boleta ajena o inexistente) y 400 (producto inexistente).
import cl.duoc.pawpets.exception.ConflictoException;
import cl.duoc.pawpets.exception.RecursoNoEncontradoException;
import cl.duoc.pawpets.exception.SolicitudInvalidaException;
// Modelos: lo que se guarda en MongoDB.
import cl.duoc.pawpets.model.Boleta;
import cl.duoc.pawpets.model.DetalleBoleta;
import cl.duoc.pawpets.model.Producto;
import cl.duoc.pawpets.model.Usuario;
// Repositorios: acceso a las colecciones "boletas" y "productos".
import cl.duoc.pawpets.repository.BoletaRepository;
import cl.duoc.pawpets.repository.ProductoRepository;
// Para registrar la fecha y hora de la compra.
import java.time.Instant;
import java.util.ArrayList;
// LinkedHashMap: mapa que conserva el orden de inserción (las líneas salen en el orden del carrito).
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
// Sort: indica el orden de una consulta (por id).
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio de la compra (BOLETA + DETALLE_BOLETA).
 * El servidor busca cada producto, valida el stock, calcula precios, subtotales y total,
 * define el estado del pago y descuenta el stock. El navegador solo envía ids y cantidades.
 */
// Componente de lógica de negocio (capa Service).
// SERVICE más importante: aquí vive TODA la regla de la compra. El navegador solo envía ids y cantidades; el servidor busca precios, valida stock, calcula y descuenta.
@Service
public class BoletaService {

    /** Medio de pago de prueba que simula un pago rechazado (igual que en el frontend). */
    // Medio de pago de prueba: simula un pago rechazado (el mismo texto que usa el frontend en Checkout).
    public static final String MEDIO_RECHAZADO = "Tarjeta sin fondos (simula error)";

    // Dependencias: boletas, productos y generador de ids.
    private final BoletaRepository repositorio;
    private final ProductoRepository productos;
    private final SecuenciaService secuencias;

    // Inyección por constructor.
    public BoletaService(BoletaRepository repositorio, ProductoRepository productos, SecuenciaService secuencias) {
        this.repositorio = repositorio;
        this.productos = productos;
        this.secuencias = secuencias;
    }

    // Crea la compra. clienteId viene del JWT (el controlador lo extrae), nunca del cuerpo: así nadie compra "a nombre de otro".
    public Boleta crear(BoletaRequest datos, Integer clienteId) {
        // PASO 1 – Consolidar: si el carrito trae el mismo producto en dos líneas, se suman sus cantidades.
        // Junta las líneas repetidas del mismo producto (por ejemplo, 2 + 1 = 3 unidades).
        // Mapa productoId → cantidad total.
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        for (ItemBoletaRequest item : datos.items()) {
            // merge: si la clave no existe la inserta; si existe, suma ambas cantidades (Integer::sum).
            cantidades.merge(item.productoId(), item.cantidad(), Integer::sum);
        }

        // Aquí se irán armando las líneas definitivas de la boleta.
        List<DetalleBoleta> detalle = new ArrayList<>();
        // Acumulador del total de la compra.
        int total = 0;
        // PASO 2 – Por cada producto del carrito: buscarlo, validarlo y calcular su línea.
        for (Map.Entry<Integer, Integer> linea : cantidades.entrySet()) {
            // Busca el producto REAL en la base de datos (no confía en lo que mande el navegador).
            Producto producto = productos.findById(linea.getKey())
                    // Si el id no existe → HTTP 400 "El producto X no existe".
                    .orElseThrow(() -> new SolicitudInvalidaException(
                            "El producto " + linea.getKey() + " no existe."));
            // Cantidad total pedida de este producto.
            int cantidad = linea.getValue();
            // Primera validación de stock (rápida, para dar un mensaje claro antes de empezar).
            if (producto.getStock() < cantidad) {
                // HTTP 409 Conflict con el detalle de cuántas unidades quedan.
                throw new ConflictoException("Stock insuficiente para " + producto.getNombre()
                        + ": quedan " + producto.getStock() + " unidades.");
            }
            // Crea la línea de detalle y la llena con datos del servidor.
            DetalleBoleta d = new DetalleBoleta();
            d.setProductoId(producto.getId());
            // Guarda una COPIA del nombre y del precio: la boleta es un registro histórico que no debe cambiar si luego se edita el producto.
            d.setNombre(producto.getNombre());
            // Precio cobrado: la oferta si existe, si no el normal (método precioFinal() del modelo).
            d.setPrecio(producto.precioFinal());
            d.setCantidad(cantidad);
            // Subtotal = precio × cantidad, calculado en el servidor.
            d.setSubtotal(producto.precioFinal() * cantidad);
            // Agrega la línea a la boleta.
            detalle.add(d);
            // Suma al total de la compra.
            total += d.getSubtotal();
        }

        // PASO 3 – Simulación de pasarela de pago: aprobado salvo que el medio sea "Tarjeta sin fondos (simula error)".
        boolean aprobado = !MEDIO_RECHAZADO.equals(datos.medioPago());

        // PASO 4 – Arma la boleta con datos del servidor (fecha, cliente del JWT, total) y datos de envío del formulario.
        Boleta boleta = new Boleta();
        boleta.setFecha(Instant.now());
        // Dueño de la compra: el id que vino del token.
        boleta.setClienteId(clienteId);
        boleta.setNombre(datos.nombre());
        boleta.setApellidos(datos.apellidos());
        boleta.setCorreo(datos.correo());
        boleta.setCalle(datos.calle());
        boleta.setRegion(datos.region());
        boleta.setComuna(datos.comuna());
        boleta.setIndicaciones(datos.indicaciones());
        boleta.setMedioPago(datos.medioPago());
        // Incrusta el detalle dentro de la boleta (un solo documento en MongoDB).
        boleta.setItems(detalle);
        boleta.setTotal(total);
        // Estado final: "Pagada" o "Rechazada".
        boleta.setEstado(aprobado ? Boleta.PAGADA : Boleta.RECHAZADA);

        // PASO 5 – Stock.
        // Solo una compra pagada descuenta stock.
        // Un pago rechazado no vende nada: se guarda la boleta rechazada y no se toca el stock.
        if (!aprobado) {
            // Guarda la boleta Rechazada y termina.
            return guardar(boleta);
        }
        // Registro de lo ya descontado, para poder deshacerlo si algo falla (patrón de COMPENSACIÓN).
        Map<Integer, Integer> descontados = new LinkedHashMap<>();
        // Si cualquier descuento falla, se salta al catch para devolver lo que ya se restó.
        try {
            // Descuenta producto por producto.
            for (DetalleBoleta d : detalle) {
                // "Si stock >= cantidad, réstale cantidad" en UNA operación de MongoDB: si otro cliente
                // compró las últimas unidades un instante antes, no se descuenta y se avisa.
                // Descuento ATÓMICO en MongoDB: "si stock >= cantidad, réstalo". Protege contra dos clientes comprando las últimas unidades a la vez (condición de carrera).
                if (!productos.descontarStock(d.getProductoId(), d.getCantidad())) {
                    // Otro cliente compró antes: se lanza 409 y entra la compensación.
                    throw new ConflictoException("Stock insuficiente para " + d.getNombre()
                            + ": otro cliente acaba de comprar las últimas unidades.");
                }
                // Anota que este producto ya fue descontado (para devolverlo si luego falla otro).
                descontados.put(d.getProductoId(), d.getCantidad());
            }
            // Todo salió bien: asigna número de boleta y la guarda.
            return guardar(boleta);
        // Cualquier error (409, caída de la BD...) cae aquí.
        } catch (RuntimeException e) {
            // Compensación: si algo falla a mitad de camino, se devuelve el stock ya descontado
            // COMPENSACIÓN: devuelve el stock de lo ya descontado, así no se pierden unidades por una compra fallida (no se usa una transacción de MongoDB: se deshace a mano).
            descontados.forEach(productos::devolverStock);
            // Vuelve a lanzar el error original para que GlobalExceptionHandler responda al frontend.
            throw e;
        }
    }

    /**
     * Asigna el número de boleta recién al guardar (así una compra rechazada por stock no "gasta" un número)
     * con el contador atómico: dos compras simultáneas nunca reciben el mismo número.
     */
    // Asigna el id y guarda.
    private Boleta guardar(Boleta boleta) {
        // El número de boleta se asigna RECIÉN al guardar: una compra rechazada por falta de stock no "gasta" un número. Usa el contador atómico.
        boleta.setId(secuencias.siguiente("boletas",
                () -> repositorio.findTopByOrderByIdDesc().map(Boleta::getId).orElse(0)));
        // INSERT en la colección "boletas" de MongoDB. Devuelve la boleta ya con su id.
        return repositorio.save(boleta);
    }

    /** Admin y vendedor ven todas las boletas; un cliente solo las suyas. */
    // Lista boletas según el rol que viene del JWT.
    public List<Boleta> listar(Integer usuarioId, String rol) {
        // AUTORIZACIÓN A NIVEL DE DATOS: un cliente solo ve las suyas.
        if (Usuario.CLIENTE.equals(rol)) {
            // Consulta derivada: boletas cuyo clienteId es el del token.
            return repositorio.findByClienteIdOrderByIdAsc(usuarioId);
        }
        // Admin y vendedor ven todas, ordenadas por número.
        return repositorio.findAll(Sort.by("id"));
    }

    /** Un cliente que pide una boleta ajena recibe 404 (no se revela que existe). */
    // Devuelve una boleta con su detalle.
    public Boleta obtener(Integer id, Integer usuarioId, String rol) {
        // Busca por id; si no existe, 404.
        Boleta boleta = repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La boleta " + id + " no existe."));
        // Si un cliente pide una boleta que no es suya...
        if (Usuario.CLIENTE.equals(rol) && !usuarioId.equals(boleta.getClienteId())) {
            // ...recibe el mismo 404: así no se revela que esa boleta existe (seguridad por no-divulgación).
            throw new RecursoNoEncontradoException("La boleta " + id + " no existe.");
        }
        return boleta;
    }
}
