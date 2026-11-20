package cl.duoc.pawpets.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * BOLETA de la Figura 1: una compra de un USUARIO (clienteId).
 * Su DETALLE_BOLETA va incrustado en "items" porque siempre se lee junto con la boleta.
 */
@Getter
@Setter
@Document(collection = "boletas")
public class Boleta {

    public static final String PAGADA = "Pagada";
    public static final String RECHAZADA = "Rechazada";

    @Id
    @Schema(description = "Número de la boleta", example = "1")
    private Integer id;

    @Schema(description = "Fecha y hora de la compra (UTC)")
    private Instant fecha;

    @Schema(description = "Id del usuario que compró", example = "2")
    private Integer clienteId;

    // Datos de envío ingresados en el checkout
    private String nombre;
    private String apellidos;
    private String correo;
    private String calle;
    private String region;
    private String comuna;
    private String indicaciones;

    @Schema(description = "Medio de pago elegido", example = "Tarjeta de débito")
    private String medioPago;

    @Schema(description = "Detalle de la boleta (DETALLE_BOLETA)")
    private List<DetalleBoleta> items = new ArrayList<>();

    @Schema(description = "Total calculado por el servidor en CLP", example = "29980")
    private Integer total;

    @Schema(description = "Pagada o Rechazada", example = "Pagada")
    private String estado;
}
