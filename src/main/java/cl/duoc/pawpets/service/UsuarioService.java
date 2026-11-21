package cl.duoc.pawpets.service;

import cl.duoc.pawpets.dto.UsuarioRequest;
import cl.duoc.pawpets.dto.UsuarioResponse;
import cl.duoc.pawpets.exception.ConflictoException;
import cl.duoc.pawpets.exception.RecursoNoEncontradoException;
import cl.duoc.pawpets.exception.SolicitudInvalidaException;
import cl.duoc.pawpets.model.Usuario;
import cl.duoc.pawpets.repository.UsuarioRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Gestión de usuarios por el administrador. Nunca devuelve la clave. */
@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final PasswordEncoder cifrador;
    private final SecuenciaService secuencias;

    public UsuarioService(UsuarioRepository repositorio, PasswordEncoder cifrador, SecuenciaService secuencias) {
        this.repositorio = repositorio;
        this.cifrador = cifrador;
        this.secuencias = secuencias;
    }

    public List<UsuarioResponse> listar() {
        return repositorio.findAll(Sort.by("id")).stream().map(UsuarioResponse::de).toList();
    }

    public UsuarioResponse obtener(Integer id) {
        return UsuarioResponse.de(buscar(id));
    }

    public UsuarioResponse crear(UsuarioRequest datos) {
        String correo = datos.correo().trim();
        if (repositorio.existsByCorreoIgnoreCase(correo)) {
            throw new ConflictoException("Ya existe una cuenta con ese correo.");
        }
        if (datos.clave() == null || datos.clave().isBlank()) {
            throw new SolicitudInvalidaException("La contraseña es obligatoria.");
        }
        Usuario nuevo = new Usuario();
        nuevo.setId(siguienteId());
        copiar(datos, nuevo);
        nuevo.setClave(cifrar(datos.clave()));
        return UsuarioResponse.de(repositorio.save(nuevo));
    }

    /** Al editar, una clave vacía significa "mantener la actual". */
    public UsuarioResponse actualizar(Integer id, UsuarioRequest datos) {
        Usuario existente = buscar(id);
        repositorio.findByCorreoIgnoreCase(datos.correo().trim())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ConflictoException("Ya existe una cuenta con ese correo.");
                });
        copiar(datos, existente);
        if (datos.clave() != null && !datos.clave().isBlank()) {
            existente.setClave(cifrar(datos.clave()));
        }
        return UsuarioResponse.de(repositorio.save(existente));
    }

    public void eliminar(Integer id) {
        buscar(id);
        repositorio.deleteById(id);
    }

    Usuario buscar(Integer id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario " + id + " no existe."));
    }

    /** Id del próximo usuario, con el contador atómico (dos registros simultáneos nunca comparten id). */
    Integer siguienteId() {
        return secuencias.siguiente("usuarios",
                () -> repositorio.findTopByOrderByIdDesc().map(Usuario::getId).orElse(0));
    }

    /** Valida el largo mínimo y devuelve el hash BCrypt. */
    String cifrar(String clave) {
        if (clave.length() < 6) {
            throw new SolicitudInvalidaException("La contraseña debe tener al menos 6 caracteres.");
        }
        return cifrador.encode(clave);
    }

    private static void copiar(UsuarioRequest d, Usuario u) {
        u.setNombre(d.nombre());
        u.setApellidos(d.apellidos());
        u.setCorreo(d.correo().trim());
        u.setRol(d.rol());
        u.setRegion(d.region());
        u.setComuna(d.comuna());
        u.setCalle(d.calle());
    }
}
