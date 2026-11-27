package cl.duoc.pawpets.exception;

/** Correo o contraseña incorrectos en el login: 401 Unauthorized. */
// Excepción de negocio → HTTP 401. Se lanza en AuthService cuando el login falla o la cuenta ya no existe.
public class CredencialesInvalidasException extends RuntimeException {
    // Recibe el mensaje para el usuario.
    public CredencialesInvalidasException(String mensaje) {
        // Guarda el mensaje en la excepción base.
        super(mensaje);
    }
}
