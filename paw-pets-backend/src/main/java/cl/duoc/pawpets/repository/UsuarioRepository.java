package cl.duoc.pawpets.repository;

import cl.duoc.pawpets.model.Usuario;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Acceso a la colección "usuarios". */
// Repositorio de la colección "usuarios".
public interface UsuarioRepository extends MongoRepository<Usuario, Integer> {

    // Usuario con el id más alto (base del contador de ids).
    Optional<Usuario> findTopByOrderByIdDesc();

    /** Busca por correo sin importar mayúsculas (login). */
    // Busca por correo ignorando mayúsculas/minúsculas. Es la consulta del LOGIN.
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    // ¿Ya existe una cuenta con ese correo? Evita correos duplicados en registro y creación de usuarios.
    boolean existsByCorreoIgnoreCase(String correo);
}
