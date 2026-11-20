package cl.duoc.pawpets.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * DETALLE_BOLETA de la Figura 1: una línea de la compra.
 * Guarda una copia del nombre y del precio para que la boleta no cambie si después se edita el producto.
 */
@Getter
@Setter
public class DetalleBoleta {

    @Schema(description = "Id del producto comprado", example = "1")
    private Integer productoId;

    @Schema(description = "Nombre del producto al momento de la compra")
    private String nombre;

    @Schema(description = "Precio unitario cobrado (CLP)", example = "14990")
    private Integer precio;

    @Schema(description = "Unidades compradas", example = "2")
    private Integer cantidad;

    @Schema(description = "precio x cantidad", example = "29980")
    private Integer subtotal;
}
