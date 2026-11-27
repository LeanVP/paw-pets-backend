package cl.duoc.pawpets.model;

// @Schema (springdoc): describe cada campo en Swagger UI (/swagger-ui.html). No afecta la lógica.
import io.swagger.v3.oas.annotations.media.Schema;
// Instant: fecha/hora en UTC. Se guarda en Mongo como fecha y viaja al frontend como texto ISO-8601.
import java.time.Instant;
// ArrayList/List: la boleta guarda una LISTA de líneas de detalle (DetalleBoleta) incrustadas.
import java.util.ArrayList;
import java.util.List;
// Lombok: genera automáticamente los métodos getXxx()/setXxx() al compilar, para no escribirlos a mano.
import lombok.Getter;
import lombok.Setter;
// @Id (Spring Data): marca el campo que será la clave primaria (_id en MongoDB).
import org.springframework.data.annotation.Id;
// @Document: indica que esta clase se guarda como documento en una colección de MongoDB.
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * BOLETA de la Figura 1: una compra de un USUARIO (clienteId).
 * Su DETALLE_BOLETA va incrustado en "items" porque siempre se lee junto con la boleta.
 */
// Lombok: crea todos los getters (getId, getFecha, getTotal...).
// Lombok: crea todos los setters (setId, setFecha, setTotal...). Los usa BoletaService para armar la boleta.
// Esta clase = una fila de la colección "boletas" en MongoDB (equivale a una tabla en SQL).
// MODELO/ENTIDAD: representa la BOLETA de la Figura 1 (una compra). Es la "M" de la arquitectura por capas.
@Getter
@Setter
@Document(collection = "boletas")
public class Boleta {

    // Constantes de los dos estados posibles, para no escribir textos sueltos ("Pagada") en distintos archivos.
    public static final String PAGADA = "Pagada";
    public static final String RECHAZADA = "Rechazada";

    // La clave primaria de la boleta (se guarda como _id).
    // Número de boleta. Lo asigna SecuenciaService (contador atómico), no lo manda el navegador.
    @Id
    @Schema(description = "Número de la boleta", example = "1")
    private Integer id;

    // Momento exacto de la compra; lo pone el servidor con Instant.now().
    @Schema(description = "Fecha y hora de la compra (UTC)")
    private Instant fecha;

    // RELACIÓN con Usuario: MongoDB no tiene JOIN, así que se guarda solo el id del cliente. Sale del JWT, no del cuerpo.
    @Schema(description = "Id del usuario que compró", example = "2")
    private Integer clienteId;

    // (Los campos siguientes se copian desde el formulario del checkout.)
    // Datos de envío ingresados en el checkout
    private String nombre;
    private String apellidos;
    private String correo;
    private String calle;
    private String region;
    private String comuna;
    private String indicaciones;

    // Medio de pago elegido. Si es "Tarjeta sin fondos (simula error)" la boleta queda Rechazada.
    @Schema(description = "Medio de pago elegido", example = "Tarjeta de débito")
    private String medioPago;

    // DETALLE_BOLETA INCRUSTADO: las líneas viven dentro del mismo documento porque siempre se leen junto con la boleta.
    // Se inicializa con una lista vacía para evitar NullPointerException.
    @Schema(description = "Detalle de la boleta (DETALLE_BOLETA)")
    private List<DetalleBoleta> items = new ArrayList<>();

    // Total en pesos chilenos calculado por el SERVIDOR (nunca se confía en el precio que envía el navegador).
    @Schema(description = "Total calculado por el servidor en CLP", example = "29980")
    private Integer total;

    // "Pagada" o "Rechazada". Decide si se descuenta el stock y qué pantalla ve el cliente.
    @Schema(description = "Pagada o Rechazada", example = "Pagada")
    private String estado;
}
