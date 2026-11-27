package cl.duoc.pawpets.dto;

// @Valid sobre elementos de una lista: valida CADA ItemBoletaRequest, no solo la lista.
import jakarta.validation.Valid;
// @Email: verifica que el texto tenga formato de correo.
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
// @NotEmpty: una colección no puede estar vacía (aquí: el carrito).
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Datos del checkout. No trae precios ni total: el servidor los calcula
 * desde la base de datos para que nadie pueda manipularlos en el navegador.
 */
// DTO de ENTRADA del checkout. Es lo que el frontend envía en POST /api/v1/boletas.
// A propósito NO incluye precios, total ni estado: el servidor los calcula, así nadie puede manipularlos desde el navegador.
public record BoletaRequest(
        // Cada componente del record lleva su propia validación; si falla, GlobalExceptionHandler devuelve 400 con el mensaje por campo.
        @NotBlank(message = "El nombre es obligatorio.") String nombre,
        @NotBlank(message = "Los apellidos son obligatorios.") String apellidos,
        // Dos reglas combinadas: obligatorio y con formato de correo.
        @NotBlank(message = "El correo es obligatorio.") @Email(message = "El correo no es válido.") String correo,
        @NotBlank(message = "La calle es obligatoria.") String calle,
        @NotBlank(message = "La región es obligatoria.") String region,
        @NotBlank(message = "La comuna es obligatoria.") String comuna,
        // Sin anotaciones = campo opcional.
        String indicaciones,
        // El medio de pago determina si el pago se aprueba o se rechaza (ver BoletaService).
        @NotBlank(message = "El medio de pago es obligatorio.") String medioPago,
        // No se puede comprar con el carrito vacío.
        @NotEmpty(message = "El carrito está vacío.") List<@Valid ItemBoletaRequest> items) {
}
