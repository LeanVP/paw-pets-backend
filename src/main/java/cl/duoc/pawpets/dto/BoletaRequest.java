package cl.duoc.pawpets.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Datos del checkout. No trae precios ni total: el servidor los calcula
 * desde la base de datos para que nadie pueda manipularlos en el navegador.
 */
public record BoletaRequest(
        @NotBlank(message = "El nombre es obligatorio.") String nombre,
        @NotBlank(message = "Los apellidos son obligatorios.") String apellidos,
        @NotBlank(message = "El correo es obligatorio.") @Email(message = "El correo no es válido.") String correo,
        @NotBlank(message = "La calle es obligatoria.") String calle,
        @NotBlank(message = "La región es obligatoria.") String region,
        @NotBlank(message = "La comuna es obligatoria.") String comuna,
        String indicaciones,
        @NotBlank(message = "El medio de pago es obligatorio.") String medioPago,
        @NotEmpty(message = "El carrito está vacío.") List<@Valid ItemBoletaRequest> items) {
}
