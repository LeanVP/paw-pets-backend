package cl.duoc.pawpets.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** PRODUCTO de la Figura 1. Pertenece a una CATEGORIA (categoriaId). */
@Getter
@Setter
@Document(collection = "productos")
public class Producto {

    @Id
    @Schema(description = "Identificador del producto", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer id;

    @NotBlank(message = "El nombre es obligatorio.")
    @Schema(description = "Nombre que se muestra en la tienda", example = "Alimento Perro Adulto 15 kg")
    private String nombre;

    @NotNull(message = "La categoría es obligatoria.")
    @Schema(description = "Id de la categoría a la que pertenece", example = "1")
    private Integer categoriaId;

    @NotNull(message = "El precio es obligatorio.")
    @Min(value = 0, message = "El precio no puede ser negativo.")
    @Schema(description = "Precio normal en pesos chilenos (CLP)", example = "29990")
    private Integer precio;

    @Min(value = 0, message = "El precio de oferta no puede ser negativo.")
    @Schema(description = "Precio de oferta en CLP (0 = sin oferta)", example = "0")
    private Integer precioOferta = 0;

    @NotNull(message = "El stock es obligatorio.")
    @Min(value = 0, message = "El stock no puede ser negativo.")
    @Schema(description = "Unidades disponibles", example = "20")
    private Integer stock;

    @Schema(description = "Ruta o URL de la imagen (opcional)", example = "/img/productos/premium-perro.png")
    private String imagen;

    @NotBlank(message = "La descripción es obligatoria.")
    @Schema(description = "Texto del detalle del producto")
    private String descripcion;

    @Schema(description = "Si aparece en Productos destacados del inicio", example = "false")
    private Boolean destacado = false;

    /** Precio que se cobra: la oferta si existe, si no el precio normal. */
    public int precioFinal() {
        return precioOferta != null && precioOferta > 0 ? precioOferta : precio;
    }
}
