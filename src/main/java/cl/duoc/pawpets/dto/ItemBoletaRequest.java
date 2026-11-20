package cl.duoc.pawpets.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Una línea del carrito: solo el producto y la cantidad (el precio lo pone el servidor). */
public record ItemBoletaRequest(
        @NotNull(message = "El producto es obligatorio.") Integer productoId,
        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = 1, message = "La cantidad mínima es 1.") Integer cantidad) {
}
