package cl.duoc.pawpets.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Manejo de excepciones centralizado (@ControllerAdvice, material 3.1.1):
 * convierte cada excepción en una respuesta JSON con el código HTTP correcto,
 * sin repetir try/catch en cada controlador.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(RecursoNoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorRespuesta> conflicto(ConflictoException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorRespuesta> invalida(SolicitudInvalidaException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorRespuesta> credenciales(CredencialesInvalidasException e) {
        return responder(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    /** Falla un @Valid: devuelve 400 con el mensaje de cada campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ErrorRespuesta(
                status.value(), status.getReasonPhrase(), "Hay datos inválidos.", campos));
    }

    /** JSON mal formado o con tipos incorrectos (por ejemplo, texto en un número). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> jsonInvalido(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido.");
    }

    /** Un id de la URL que no es número, por ejemplo /productos/abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> tipoInvalido(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.BAD_REQUEST, "El parámetro '" + e.getName() + "' no es válido.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuesta> rutaInexistente(NoResourceFoundException e) {
        return responder(HttpStatus.NOT_FOUND, "La ruta pedida no existe.");
    }

    /**
     * Cualquier otro error. Las excepciones propias de Spring (método no permitido 405,
     * tipo de contenido 415, etc.) traen su código HTTP y se respeta; el resto es 500
     * sin mostrar detalles internos al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception e) {
        if (e instanceof ErrorResponse conCodigo) {
            HttpStatus status = HttpStatus.valueOf(conCodigo.getStatusCode().value());
            return responder(status, status.getReasonPhrase());
        }
        log.error("Error inesperado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado en el servidor.");
    }

    private ResponseEntity<ErrorRespuesta> responder(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(ErrorRespuesta.de(status, mensaje));
    }
}
