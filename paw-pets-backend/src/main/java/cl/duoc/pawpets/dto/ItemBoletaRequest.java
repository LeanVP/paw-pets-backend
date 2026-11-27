package cl.duoc.pawpets.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Una línea del carrito: solo el producto y la cantidad (el precio lo pone el servidor). */
// Una línea del carrito. Solo viaja el id del producto y la cantidad: el precio lo busca el servidor en la base de datos.
public record ItemBoletaRequest(
        // Id del producto a comprar.
        @NotNull(message = "El producto es obligatorio.") Integer productoId,
        // La cantidad debe venir informada...
        // ...y ser como mínimo 1 (impide comprar 0 o negativos).
        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = 1, message = "La cantidad mínima es 1.") Integer cantidad) {
}
