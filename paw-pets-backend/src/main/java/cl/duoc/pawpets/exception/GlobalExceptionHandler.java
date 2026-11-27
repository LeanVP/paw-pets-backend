package cl.duoc.pawpets.exception;

// LinkedHashMap: mantiene el orden de los errores por campo.
import java.util.LinkedHashMap;
import java.util.Map;
// Sistema de logs para registrar errores inesperados en el servidor.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
// ResponseEntity: permite elegir el código HTTP y el cuerpo de la respuesta.
import org.springframework.http.ResponseEntity;
// Excepción de Spring cuando el JSON recibido está mal formado.
import org.springframework.http.converter.HttpMessageNotReadableException;
// Interfaz de las excepciones de Spring que ya traen su propio código HTTP (405, 415...).
import org.springframework.web.ErrorResponse;
// Excepción que lanza @Valid cuando un campo no cumple su validación.
import org.springframework.web.bind.MethodArgumentNotValidException;
// @ExceptionHandler: "este método atiende esta excepción".
import org.springframework.web.bind.annotation.ExceptionHandler;
// @RestControllerAdvice: clase que vigila TODOS los controladores y responde sus errores en JSON.
import org.springframework.web.bind.annotation.RestControllerAdvice;
// Se lanza cuando un parámetro de la URL no tiene el tipo esperado (ej. /productos/abc).
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
// Se lanza cuando la ruta pedida no existe.
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Manejo de excepciones centralizado (@ControllerAdvice, material 3.1.1):
 * convierte cada excepción en una respuesta JSON con el código HTTP correcto,
 * sin repetir try/catch en cada controlador.
 */
// MANEJO CENTRALIZADO DE ERRORES: cualquier excepción que escape de un controlador o servicio llega aquí y se transforma en JSON uniforme.
// Evita repetir try/catch en cada método.
// Cada método de esta clase traduce un tipo de excepción a un código HTTP.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Logger de la clase.
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Si algún servicio lanza RecursoNoEncontradoException...
    // ...se responde 404 Not Found con { estado, error, mensaje, campos }.
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(RecursoNoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // ConflictoException → 409 (correo repetido, stock insuficiente, categoría con productos).
    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorRespuesta> conflicto(ConflictoException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    // SolicitudInvalidaException → 400 (regla de negocio incumplida).
    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorRespuesta> invalida(SolicitudInvalidaException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // CredencialesInvalidasException → 401 (login incorrecto).
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorRespuesta> credenciales(CredencialesInvalidasException e) {
        return responder(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    /** Falla un @Valid: devuelve 400 con el mensaje de cada campo. */
    // Se activa cuando @Valid detecta datos inválidos en el cuerpo de la petición.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException e) {
        // Aquí se juntan los errores: nombre del campo → mensaje.
        Map<String, String> campos = new LinkedHashMap<>();
        // Recorre cada campo que falló la validación.
        e.getBindingResult().getFieldErrors()
                // Guarda un solo mensaje por campo (el primero). Ej: { "correo": "El correo no es válido." }.
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        // 400 Bad Request.
        HttpStatus status = HttpStatus.BAD_REQUEST;
        // Respuesta con el mapa "campos": el frontend lo usa para pintar el error bajo cada input.
        return ResponseEntity.status(status).body(new ErrorRespuesta(
                status.value(), status.getReasonPhrase(), "Hay datos inválidos.", campos));
    }

    /** JSON mal formado o con tipos incorrectos (por ejemplo, texto en un número). */
    // JSON mal escrito o con tipos incorrectos.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> jsonInvalido(HttpMessageNotReadableException e) {
        // 400 con mensaje genérico.
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido.");
    }

    /** Un id de la URL que no es número, por ejemplo /productos/abc. */
    // Ejemplo: GET /productos/abc (no es número).
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> tipoInvalido(MethodArgumentTypeMismatchException e) {
        // 400 indicando qué parámetro es inválido.
        return responder(HttpStatus.BAD_REQUEST, "El parámetro '" + e.getName() + "' no es válido.");
    }

    // Ruta que no existe en la API.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuesta> rutaInexistente(NoResourceFoundException e) {
        // 404.
        return responder(HttpStatus.NOT_FOUND, "La ruta pedida no existe.");
    }

    /**
     * Cualquier otro error. Las excepciones propias de Spring (método no permitido 405,
     * tipo de contenido 415, etc.) traen su código HTTP y se respeta; el resto es 500
     * sin mostrar detalles internos al cliente.
     */
    // Red de seguridad: atrapa cualquier excepción no prevista (Exception es la clase padre de todas).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception e) {
        // Si es una excepción de Spring que ya sabe su código (405 método no permitido, 415 tipo no soportado...), se respeta.
        if (e instanceof ErrorResponse conCodigo) {
            // Convierte ese código numérico en el enum HttpStatus.
            HttpStatus status = HttpStatus.valueOf(conCodigo.getStatusCode().value());
            return responder(status, status.getReasonPhrase());
        }
        // Los detalles técnicos se registran SOLO en el log del servidor...
        log.error("Error inesperado", e);
        // ...y al cliente se le responde 500 con un mensaje genérico (no se filtran datos internos por seguridad).
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado en el servidor.");
    }

    // Auxiliar para no repetir: arma ResponseEntity con código HTTP y ErrorRespuesta.
    private ResponseEntity<ErrorRespuesta> responder(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(ErrorRespuesta.de(status, mensaje));
    }
}
