package cl.duoc.pawpets.service;

import cl.duoc.pawpets.dto.UsuarioRequest;
import cl.duoc.pawpets.dto.UsuarioResponse;
import cl.duoc.pawpets.exception.ConflictoException;
import cl.duoc.pawpets.exception.RecursoNoEncontradoException;
// Excepciones de negocio: 409 (correo repetido), 404 (no existe) y 400 (datos inválidos).
import cl.duoc.pawpets.exception.SolicitudInvalidaException;
import cl.duoc.pawpets.model.Usuario;
import cl.duoc.pawpets.repository.UsuarioRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Gestión de usuarios por el administrador. Nunca devuelve la clave. */
// Componente de la capa de negocio.
// SERVICE: gestión de usuarios por el ADMIN. Nunca devuelve la clave: convierte todo a UsuarioResponse.
@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;
    // Cifrador BCrypt, inyectado desde SecurityConfig.
    private final PasswordEncoder cifrador;
    private final SecuenciaService secuencias;

    public UsuarioService(UsuarioRepository repositorio, PasswordEncoder cifrador, SecuenciaService secuencias) {
        this.repositorio = repositorio;
        this.cifrador = cifrador;
        this.secuencias = secuencias;
    }

    // READ (lista).
    public List<UsuarioResponse> listar() {
        // Lee todos ordenados y convierte cada Usuario a DTO (sin clave) con un stream: findAll → map → toList.
        return repositorio.findAll(Sort.by("id")).stream().map(UsuarioResponse::de).toList();
    }

    // READ (uno) como DTO sin clave.
    public UsuarioResponse obtener(Integer id) {
        return UsuarioResponse.de(buscar(id));
    }

    // CREATE: el admin puede elegir el rol.
    public UsuarioResponse crear(UsuarioRequest datos) {
        String correo = datos.correo().trim();
        // Correo único → 409 si ya existe.
        if (repositorio.existsByCorreoIgnoreCase(correo)) {
            throw new ConflictoException("Ya existe una cuenta con ese correo.");
        }
        // Al crear la contraseña es obligatoria (al editar no).
        if (datos.clave() == null || datos.clave().isBlank()) {
            throw new SolicitudInvalidaException("La contraseña es obligatoria.");
        }
        Usuario nuevo = new Usuario();
        // Id con el contador atómico.
        nuevo.setId(siguienteId());
        // Copia nombre, apellidos, correo, rol y dirección del DTO al modelo.
        copiar(datos, nuevo);
        // Guarda el hash BCrypt.
        nuevo.setClave(cifrar(datos.clave()));
        // Guarda y devuelve el usuario sin la clave.
        return UsuarioResponse.de(repositorio.save(nuevo));
    }

    /** Al editar, una clave vacía significa "mantener la actual". */
    // UPDATE.
    public UsuarioResponse actualizar(Integer id, UsuarioRequest datos) {
        // Carga el usuario actual (404 si no existe).
        Usuario existente = buscar(id);
        // Verifica que el correo nuevo no pertenezca a OTRO usuario (si es el mismo usuario, se permite).
        repositorio.findByCorreoIgnoreCase(datos.correo().trim())
                // Descarta al propio usuario que se está editando.
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    // Si otro usuario ya lo usa → 409.
                    throw new ConflictoException("Ya existe una cuenta con ese correo.");
                });
        // Aplica los cambios al modelo cargado.
        copiar(datos, existente);
        // Clave vacía = mantener la actual; solo se recifra si se escribió una nueva.
        if (datos.clave() != null && !datos.clave().isBlank()) {
            existente.setClave(cifrar(datos.clave()));
        }
        // UPDATE en MongoDB y respuesta sin clave.
        return UsuarioResponse.de(repositorio.save(existente));
    }

    // DELETE.
    public void eliminar(Integer id) {
        // 404 si no existe.
        buscar(id);
        // Borra el usuario.
        repositorio.deleteById(id);
    }

    // Auxiliar (visibilidad de paquete: también lo usa AuthService): busca o lanza 404.
    Usuario buscar(Integer id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario " + id + " no existe."));
    }

    /** Id del próximo usuario, con el contador atómico (dos registros simultáneos nunca comparten id). */
    // Pide el próximo id al contador atómico; lo comparten el alta del admin y el registro público.
    Integer siguienteId() {
        return secuencias.siguiente("usuarios",
                () -> repositorio.findTopByOrderByIdDesc().map(Usuario::getId).orElse(0));
    }

    /** Valida el largo mínimo y devuelve el hash BCrypt. */
    // Auxiliar: valida el largo mínimo y devuelve el hash BCrypt.
    String cifrar(String clave) {
        // Regla: mínimo 6 caracteres.
        if (clave.length() < 6) {
            throw new SolicitudInvalidaException("La contraseña debe tener al menos 6 caracteres.");
        }
        // BCrypt con sal aleatoria: nunca se guarda texto plano.
        return cifrador.encode(clave);
    }

    // Copia los campos comunes de UsuarioRequest a Usuario (evita repetir código en crear y actualizar).
    private static void copiar(UsuarioRequest d, Usuario u) {
        u.setNombre(d.nombre());
        u.setApellidos(d.apellidos());
        // trim(): elimina espacios accidentales alrededor del correo.
        u.setCorreo(d.correo().trim());
        u.setRol(d.rol());
        u.setRegion(d.region());
        u.setComuna(d.comuna());
        u.setCalle(d.calle());
    }
}
