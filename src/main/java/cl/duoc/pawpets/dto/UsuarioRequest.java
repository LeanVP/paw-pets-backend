package cl.duoc.pawpets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Datos que el administrador envía al crear o editar un usuario.
 * Al editar, la clave puede ir vacía: significa "mantener la actual".
 */
public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio.") String nombre,
        @NotBlank(message = "Los apellidos son obligatorios.") String apellidos,
        @NotBlank(message = "El correo es obligatorio.") @Email(message = "El correo no es válido.") String correo,
        @Schema(description = "Mínimo 6 caracteres. Obligatoria al crear; vacía al editar = no cambiarla.")
        String clave,
        @NotBlank(message = "El rol es obligatorio.")
        @Pattern(regexp = "admin|vendedor|cliente", message = "El rol debe ser admin, vendedor o cliente.")
        String rol,
        String region,
        String comuna,
        String calle) {
}
