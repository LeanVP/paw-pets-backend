package cl.duoc.pawpets.service;

// DTOs de entrada/salida de la autenticación (LoginRequest, RegistroRequest, AuthResponse, UsuarioResponse).
import cl.duoc.pawpets.dto.AuthResponse;
import cl.duoc.pawpets.dto.LoginRequest;
import cl.duoc.pawpets.dto.RegistroRequest;
import cl.duoc.pawpets.dto.UsuarioResponse;
// Excepciones propias: GlobalExceptionHandler las traduce a 409 (conflicto) y 401 (credenciales inválidas).
import cl.duoc.pawpets.exception.ConflictoException;
import cl.duoc.pawpets.exception.CredencialesInvalidasException;
import cl.duoc.pawpets.model.Usuario;
// Acceso a la colección "usuarios".
import cl.duoc.pawpets.repository.UsuarioRepository;
// Fabrica el token JWT.
import cl.duoc.pawpets.security.JwtService;
// Interfaz para cifrar/comparar contraseñas (BCrypt).
import org.springframework.security.crypto.password.PasswordEncoder;
// @Service: componente de la capa de lógica de negocio.
import org.springframework.stereotype.Service;

/** Inicio de sesión, registro de clientes y renovación del token. */
// Spring crea una única instancia y la entrega a los controladores que la pidan.
// SERVICE de autenticación: contiene las REGLAS (comparar claves, evitar correos repetidos, forzar rol cliente). El controlador solo delega aquí.
@Service
public class AuthService {

    // Dependencias de esta clase; son final porque se asignan una sola vez en el constructor.
    private final UsuarioRepository usuarios;
    private final UsuarioService usuarioService;
    private final PasswordEncoder cifrador;
    private final JwtService jwt;

    // INYECCIÓN POR CONSTRUCTOR: Spring entrega los 4 componentes. Facilita las pruebas (Mockito) porque se pueden pasar falsos.
    public AuthService(UsuarioRepository usuarios, UsuarioService usuarioService,
                       PasswordEncoder cifrador, JwtService jwt) {
        this.usuarios = usuarios;
        this.usuarioService = usuarioService;
        this.cifrador = cifrador;
        this.jwt = jwt;
    }

    /** Compara la clave escrita con el hash BCrypt guardado; si coincide, entrega un JWT. */
    // Valida credenciales. Devuelve token + usuario o lanza CredencialesInvalidasException (401).
    public AuthResponse login(LoginRequest datos) {
        // 1) Busca al usuario por correo (sin importar mayúsculas y quitando espacios con trim).
        Usuario usuario = usuarios.findByCorreoIgnoreCase(datos.correo().trim())
                // 2) Si existe, compara la clave escrita con el hash BCrypt guardado. matches() cifra con la misma sal y compara; nunca "descifra".
                .filter(u -> cifrador.matches(datos.clave(), u.getClave()))
                // 3) Un solo mensaje para "correo no existe" y "clave mala": así un atacante no puede averiguar qué correos están registrados.
                // Mismo mensaje para correo inexistente y clave mala: no revela qué correos existen.
                // Si el Optional quedó vacío (por cualquiera de las dos causas) lanza la excepción que se convierte en HTTP 401.
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos."));
        // Credenciales correctas: arma la respuesta con token y datos del usuario.
        return respuesta(usuario);
    }

    /** Registro público: SIEMPRE crea un cliente (nadie puede registrarse como admin o vendedor). */
    // Crea una cuenta nueva desde el formulario público.
    public AuthResponse registrar(RegistroRequest datos) {
        // Limpia espacios accidentales del correo.
        String correo = datos.correo().trim();
        // Regla de negocio: el correo debe ser único.
        if (usuarios.existsByCorreoIgnoreCase(correo)) {
            // Correo repetido → HTTP 409 Conflict. El frontend lo interpreta (registrar() devuelve null).
            throw new ConflictoException("Ya existe una cuenta con ese correo.");
        }
        // Crea el modelo y lo rellena campo por campo con lo que vino en el DTO.
        Usuario nuevo = new Usuario();
        // Pide el siguiente id al contador atómico (nunca se repite aunque haya registros simultáneos).
        nuevo.setId(usuarioService.siguienteId());
        nuevo.setNombre(datos.nombre());
        nuevo.setApellidos(datos.apellidos());
        nuevo.setCorreo(correo);
        // Valida largo mínimo y guarda el HASH BCrypt, jamás la contraseña en claro.
        nuevo.setClave(usuarioService.cifrar(datos.clave()));
        // SEGURIDAD: el registro público SIEMPRE crea un cliente; el rol no viene del cliente HTTP.
        nuevo.setRol(Usuario.CLIENTE);
        nuevo.setCalle(datos.calle());
        nuevo.setRegion(datos.region());
        nuevo.setComuna(datos.comuna());
        // Guarda en MongoDB y devuelve token + usuario: el usuario queda con la sesión iniciada.
        return respuesta(usuarios.save(nuevo));
    }

    /** Renovación: con un token todavía válido entrega uno nuevo y los datos actualizados del usuario. */
    // Renueva el token: recibe el id sacado de un JWT todavía válido.
    public AuthResponse refrescar(Integer usuarioId) {
        // Relee al usuario de la BD: así el nuevo token refleja cambios de rol o datos hechos mientras tanto.
        Usuario usuario = usuarios.findById(usuarioId)
                // Si el usuario fue eliminado, el token deja de servir (401).
                .orElseThrow(() -> new CredencialesInvalidasException("La cuenta ya no existe."));
        return respuesta(usuario);
    }

    // Método auxiliar para no repetir código en login, registro y refresh.
    private AuthResponse respuesta(Usuario usuario) {
        // Token firmado + usuario SIN clave (UsuarioResponse).
        return new AuthResponse(jwt.generarToken(usuario), UsuarioResponse.de(usuario));
    }
}
