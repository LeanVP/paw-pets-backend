package cl.duoc.pawpets.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * DETALLE_BOLETA de la Figura 1: una línea de la compra.
 * Guarda una copia del nombre y del precio para que la boleta no cambie si después se edita el producto.
 */
// MODELO INCRUSTADO: una línea de la compra. No tiene @Document ni @Id porque NO es una colección propia:
// vive dentro de Boleta.items.
@Getter
@Setter
public class DetalleBoleta {

    // Referencia (por id) al producto comprado.
    @Schema(description = "Id del producto comprado", example = "1")
    private Integer productoId;

    // COPIA del nombre al momento de comprar: si luego el admin renombra el producto, la boleta histórica no cambia.
    @Schema(description = "Nombre del producto al momento de la compra")
    private String nombre;

    // COPIA del precio unitario cobrado (oferta si existía). Mismo motivo: la boleta es un registro histórico.
    @Schema(description = "Precio unitario cobrado (CLP)", example = "14990")
    private Integer precio;

    // Unidades compradas de ese producto.
    @Schema(description = "Unidades compradas", example = "2")
    private Integer cantidad;

    // precio x cantidad, calculado en el servidor (BoletaService).
    @Schema(description = "precio x cantidad", example = "29980")
    private Integer subtotal;
}
