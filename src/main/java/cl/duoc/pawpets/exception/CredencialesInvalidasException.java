package cl.duoc.pawpets.exception;

/** Correo o contraseña incorrectos en el login: 401 Unauthorized. */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
