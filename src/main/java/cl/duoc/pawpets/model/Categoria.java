package cl.duoc.pawpets.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** CATEGORIA de la Figura 1. Una categoría agrupa muchos productos. */
@Getter
@Setter
@Document(collection = "categorias")
public class Categoria {

    /** Identificador numérico (se guarda como _id en MongoDB). Lo asigna el servicio al crear. */
    @Id
    @Schema(description = "Identificador de la categoría", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer id;

    @NotBlank(message = "El nombre es obligatorio.")
    @Schema(description = "Nombre de la categoría", example = "Alimentos Perro")
    private String nombre;

    @NotBlank(message = "La imagen es obligatoria.")
    @Schema(description = "Ruta o URL de la imagen", example = "/img/categorias/categoria-perro.jpg")
    private String imagen;
}
