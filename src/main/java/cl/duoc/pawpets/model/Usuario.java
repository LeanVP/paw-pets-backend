package cl.duoc.pawpets.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * USUARIO de la Figura 1. La clave se guarda cifrada con BCrypt y esta clase
 * nunca se devuelve directo al frontend (se usa UsuarioResponse, sin la clave).
 */
@Getter
@Setter
@Document(collection = "usuarios")
public class Usuario {

    /** Roles del sistema (Anexo 1): administrador, vendedor y cliente. */
    public static final String ADMIN = "admin";
    public static final String VENDEDOR = "vendedor";
    public static final String CLIENTE = "cliente";

    @Id
    private Integer id;
    private String nombre;
    private String apellidos;

    /** Correo único (lo valida UsuarioService): se usa para iniciar sesión. */
    private String correo;

    /** Hash BCrypt de la contraseña (nunca texto plano). */
    private String clave;

    /** admin, vendedor o cliente. */
    private String rol;

    private String region;
    private String comuna;
    private String calle;
}
