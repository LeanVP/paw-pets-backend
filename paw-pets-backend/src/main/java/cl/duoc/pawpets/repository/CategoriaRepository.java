package cl.duoc.pawpets.repository;

import cl.duoc.pawpets.model.Categoria;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Acceso a la colección "categorias". Spring Data crea la implementación (findAll, save, deleteById...). */
// Repositorio de la colección "categorias". Hereda gratis findAll(), findById(), save(), deleteById(), existsById()...
public interface CategoriaRepository extends MongoRepository<Categoria, Integer> {

    /** La categoría con el id más alto (para calcular el siguiente id). */
    // Devuelve la categoría con el id más alto: se usa como valor inicial del contador de ids (SecuenciaService).
    Optional<Categoria> findTopByOrderByIdDesc();
}
