package cl.duoc.pawpets.exception;

/** Los datos enviados no cumplen una regla de negocio: 400 Bad Request. */
// Excepción de negocio → HTTP 400. Se usa cuando los datos están bien formados pero rompen una regla (ej. oferta mayor al precio).
public class SolicitudInvalidaException extends RuntimeException {
    // Recibe el mensaje para el usuario.
    public SolicitudInvalidaException(String mensaje) {
        // Guarda el mensaje en la excepción base.
        super(mensaje);
    }
}
