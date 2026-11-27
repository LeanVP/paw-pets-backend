package cl.duoc.pawpets.model;

import io.swagger.v3.oas.annotations.media.Schema;
// @Min: valor mínimo permitido de un número.
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
// @NotNull: el campo no puede venir ausente (sirve para números, donde @NotBlank no aplica).
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** PRODUCTO de la Figura 1. Pertenece a una CATEGORIA (categoriaId). */
// Colección "productos" en MongoDB.
// MODELO: el PRODUCTO de la Figura 1. Pertenece a una categoría mediante categoriaId.
@Getter
@Setter
@Document(collection = "productos")
public class Producto {

    // Id numérico correlativo, asignado por SecuenciaService.
    @Id
    @Schema(description = "Identificador del producto", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer id;

    // Validación del nombre (400 si falta).
    // Nombre que se muestra en la tienda.
    @NotBlank(message = "El nombre es obligatorio.")
    @Schema(description = "Nombre que se muestra en la tienda", example = "Alimento Perro Adulto 15 kg")
    private String nombre;

    // La categoría debe venir en el JSON. Que EXISTA realmente lo revisa ProductoService.validar().
    // RELACIÓN con Categoria por id (en MongoDB no hay JOIN ni clave foránea).
    @NotNull(message = "La categoría es obligatoria.")
    @Schema(description = "Id de la categoría a la que pertenece", example = "1")
    private Integer categoriaId;

    // Precio obligatorio.
    // Regla: no se aceptan precios negativos.
    // Precio normal en CLP (pesos chilenos no usan decimales, por eso Integer).
    @NotNull(message = "El precio es obligatorio.")
    @Min(value = 0, message = "El precio no puede ser negativo.")
    @Schema(description = "Precio normal en pesos chilenos (CLP)", example = "29990")
    private Integer precio;

    // Precio de oferta; 0 significa "sin oferta". Valor por defecto 0 si el JSON no lo trae.
    @Min(value = 0, message = "El precio de oferta no puede ser negativo.")
    @Schema(description = "Precio de oferta en CLP (0 = sin oferta)", example = "0")
    private Integer precioOferta = 0;

    // Unidades disponibles. BoletaService lo descuenta de forma atómica al vender.
    @NotNull(message = "El stock es obligatorio.")
    @Min(value = 0, message = "El stock no puede ser negativo.")
    @Schema(description = "Unidades disponibles", example = "20")
    private Integer stock;

    // Imagen opcional (sin validación): ruta o URL.
    @Schema(description = "Ruta o URL de la imagen (opcional)", example = "/img/productos/premium-perro.png")
    private String imagen;

    // Descripción obligatoria.
    // Texto que se ve en el detalle del producto.
    @NotBlank(message = "La descripción es obligatoria.")
    @Schema(description = "Texto del detalle del producto")
    private String descripcion;

    // Si aparece en "Productos destacados" del inicio. Por defecto false. Es Boolean (objeto) para poder detectar "no enviado".
    @Schema(description = "Si aparece en Productos destacados del inicio", example = "false")
    private Boolean destacado = false;

    /** Precio que se cobra: la oferta si existe, si no el precio normal. */
    // MÉTODO DE NEGOCIO dentro del modelo: devuelve el precio que realmente se cobra.
    public int precioFinal() {
        // Si hay oferta (>0) se cobra la oferta; si no, el precio normal. BoletaService lo usa para calcular subtotales.
        return precioOferta != null && precioOferta > 0 ? precioOferta : precio;
    }
}
