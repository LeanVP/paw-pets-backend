package cl.duoc.pawpets.repository;

import cl.duoc.pawpets.model.Usuario;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Acceso a la colección "usuarios". */
public interface UsuarioRepository extends MongoRepository<Usuario, Integer> {

    Optional<Usuario> findTopByOrderByIdDesc();

    /** Busca por correo sin importar mayúsculas (login). */
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);
}
