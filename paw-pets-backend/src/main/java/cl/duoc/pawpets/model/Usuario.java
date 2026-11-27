package cl.duoc.pawpets.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * USUARIO de la Figura 1. La clave se guarda cifrada con BCrypt y esta clase
 * nunca se devuelve directo al frontend (se usa UsuarioResponse, sin la clave).
 */
// MODELO: el USUARIO de la Figura 1. OJO: esta clase NUNCA se envía al frontend porque contiene la clave;
// para eso existe UsuarioResponse (sin clave).
@Getter
@Setter
@Document(collection = "usuarios")
public class Usuario {

    /** Roles del sistema (Anexo 1): administrador, vendedor y cliente. */
    // Constantes con los tres roles del sistema. Evitan errores de tipeo al comparar roles en el código.
    public static final String ADMIN = "admin";
    public static final String VENDEDOR = "vendedor";
    public static final String CLIENTE = "cliente";

    // Clave primaria.
    // Id numérico correlativo. Va en el JWT como "sub" (subject).
    @Id
    private Integer id;
    private String nombre;
    private String apellidos;

    /** Correo único (lo valida UsuarioService): se usa para iniciar sesión. */
    // Correo único (lo valida UsuarioService). Es el "usuario" del login.
    private String correo;

    /** Hash BCrypt de la contraseña (nunca texto plano). */
    // Hash BCrypt de la contraseña (nunca texto plano). Empieza con "$2a$...".
    private String clave;

    /** admin, vendedor o cliente. */
    // "admin", "vendedor" o "cliente". Va dentro del JWT y Spring Security lo convierte en ROLE_ADMIN, etc.
    private String rol;

    // Dirección del usuario (región, comuna, calle): se usa para precargar el checkout.
    private String region;
    private String comuna;
    private String calle;
}
