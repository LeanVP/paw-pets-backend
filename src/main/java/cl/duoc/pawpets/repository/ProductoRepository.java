package cl.duoc.pawpets.repository;

import cl.duoc.pawpets.model.Producto;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Acceso a la colección "productos". Incluye el descuento atómico de stock (ProductoStockRepository). */
public interface ProductoRepository extends MongoRepository<Producto, Integer>, ProductoStockRepository {

    Optional<Producto> findTopByOrderByIdDesc();

    /** ¿Hay productos en esa categoría? (no se puede eliminar una categoría con productos). */
    boolean existsByCategoriaId(Integer categoriaId);
}
