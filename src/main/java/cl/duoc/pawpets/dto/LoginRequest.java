package cl.duoc.pawpets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Datos del formulario de inicio de sesión. */
public record LoginRequest(
        @Schema(example = "admin@pawpets.cl")
        @NotBlank(message = "El correo es obligatorio.") @Email(message = "El correo no es válido.") String correo,
        @Schema(example = "Admin123")
        @NotBlank(message = "La contraseña es obligatoria.") String clave) {
}
