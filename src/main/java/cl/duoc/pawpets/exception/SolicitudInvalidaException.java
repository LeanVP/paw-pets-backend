package cl.duoc.pawpets.exception;

/** Los datos enviados no cumplen una regla de negocio: 400 Bad Request. */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
