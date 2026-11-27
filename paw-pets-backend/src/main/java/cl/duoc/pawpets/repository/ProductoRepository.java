package cl.duoc.pawpets.repository;

import cl.duoc.pawpets.model.Producto;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Acceso a la colección "productos". Incluye el descuento atómico de stock (ProductoStockRepository). */
// Hereda DOS cosas: MongoRepository (CRUD estándar) y ProductoStockRepository (operaciones atómicas de stock hechas a mano).
// Spring Data une ambas: por eso desde BoletaService se puede llamar a productos.descontarStock(...).
public interface ProductoRepository extends MongoRepository<Producto, Integer>, ProductoStockRepository {

    // Producto con el id más alto (base del contador de ids).
    Optional<Producto> findTopByOrderByIdDesc();

    /** ¿Hay productos en esa categoría? (no se puede eliminar una categoría con productos). */
    // Consulta derivada: ¿existe algún producto con esa categoriaId? CategoriaService la usa para impedir borrar categorías con productos.
    boolean existsByCategoriaId(Integer categoriaId);
}
