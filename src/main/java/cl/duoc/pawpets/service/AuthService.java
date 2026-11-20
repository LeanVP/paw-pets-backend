package cl.duoc.pawpets.service;

import cl.duoc.pawpets.dto.AuthResponse;
import cl.duoc.pawpets.dto.LoginRequest;
import cl.duoc.pawpets.dto.RegistroRequest;
import cl.duoc.pawpets.dto.UsuarioResponse;
import cl.duoc.pawpets.exception.ConflictoException;
import cl.duoc.pawpets.exception.CredencialesInvalidasException;
import cl.duoc.pawpets.model.Usuario;
import cl.duoc.pawpets.repository.UsuarioRepository;
import cl.duoc.pawpets.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Inicio de sesión, registro de clientes y renovación del token. */
@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final UsuarioService usuarioService;
    private final PasswordEncoder cifrador;
    private final JwtService jwt;

    public AuthService(UsuarioRepository usuarios, UsuarioService usuarioService,
                       PasswordEncoder cifrador, JwtService jwt) {
        this.usuarios = usuarios;
        this.usuarioService = usuarioService;
        this.cifrador = cifrador;
        this.jwt = jwt;
    }

    /** Compara la clave escrita con el hash BCrypt guardado; si coincide, entrega un JWT. */
    public AuthResponse login(LoginRequest datos) {
        Usuario usuario = usuarios.findByCorreoIgnoreCase(datos.correo().trim())
                .filter(u -> cifrador.matches(datos.clave(), u.getClave()))
                // Mismo mensaje para correo inexistente y clave mala: no revela qué correos existen.
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos."));
        return respuesta(usuario);
    }

    /** Registro público: SIEMPRE crea un cliente (nadie puede registrarse como admin o vendedor). */
    public AuthResponse registrar(RegistroRequest datos) {
        String correo = datos.correo().trim();
        if (usuarios.existsByCorreoIgnoreCase(correo)) {
            throw new ConflictoException("Ya existe una cuenta con ese correo.");
        }
        Usuario nuevo = new Usuario();
        nuevo.setId(usuarioService.siguienteId());
        nuevo.setNombre(datos.nombre());
        nuevo.setApellidos(datos.apellidos());
        nuevo.setCorreo(correo);
        nuevo.setClave(usuarioService.cifrar(datos.clave()));
        nuevo.setRol(Usuario.CLIENTE);
        nuevo.setCalle(datos.calle());
        nuevo.setRegion(datos.region());
        nuevo.setComuna(datos.comuna());
        return respuesta(usuarios.save(nuevo));
    }

    /** Renovación: con un token todavía válido entrega uno nuevo y los datos actualizados del usuario. */
    public AuthResponse refrescar(Integer usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new CredencialesInvalidasException("La cuenta ya no existe."));
        return respuesta(usuario);
    }

    private AuthResponse respuesta(Usuario usuario) {
        return new AuthResponse(jwt.generarToken(usuario), UsuarioResponse.de(usuario));
    }
}
