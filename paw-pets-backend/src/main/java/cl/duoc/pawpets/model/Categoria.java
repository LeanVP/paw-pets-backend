package cl.duoc.pawpets.model;

import io.swagger.v3.oas.annotations.media.Schema;
// Bean Validation (Jakarta): anotaciones que validan datos. @NotBlank = no nulo, no vacío, no solo espacios.
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** CATEGORIA de la Figura 1. Una categoría agrupa muchos productos. */
// Colección "categorias" en MongoDB.
// MODELO: la CATEGORIA de la Figura 1. Se usa tanto para guardar en Mongo como para recibir el JSON del frontend (@RequestBody).
@Getter
@Setter
@Document(collection = "categorias")
public class Categoria {

    /** Identificador numérico (se guarda como _id en MongoDB). Lo asigna el servicio al crear. */
    // Clave primaria (_id). La asigna CategoriaService al crear, por eso es READ_ONLY en Swagger.
    // Id numérico correlativo (1, 2, 3...). Es Integer (objeto) para poder ser null antes de guardar.
    @Id
    @Schema(description = "Identificador de la categoría", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer id;

    // Si falta el nombre, el controlador (con @Valid) responde 400 y este mensaje llega al frontend.
    // Nombre visible de la categoría (ej: "Alimentos Perro").
    @NotBlank(message = "El nombre es obligatorio.")
    @Schema(description = "Nombre de la categoría", example = "Alimentos Perro")
    private String nombre;

    // Validación: la imagen es obligatoria en categorías.
    // Ruta de la imagen, por ejemplo /img/categorias/categoria-perro.jpg (el archivo vive en el frontend, carpeta public).
    @NotBlank(message = "La imagen es obligatoria.")
    @Schema(description = "Ruta o URL de la imagen", example = "/img/categorias/categoria-perro.jpg")
    private String imagen;
}
