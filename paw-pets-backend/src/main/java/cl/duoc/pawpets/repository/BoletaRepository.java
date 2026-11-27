package cl.duoc.pawpets.repository;

import cl.duoc.pawpets.model.Boleta;
import java.util.List;
import java.util.Optional;
// MongoRepository: interfaz de Spring Data que ya trae findAll, findById, save, deleteById, count, etc.
import org.springframework.data.mongodb.repository.MongoRepository;

/** Acceso a la colección "boletas". */
// REPOSITORIO (capa de acceso a datos). Es una INTERFAZ: Spring Data genera la implementación en tiempo de ejecución.
// Los tipos genéricos significan: <entidad que maneja, tipo de su @Id>.
public interface BoletaRepository extends MongoRepository<Boleta, Integer> {

    // MÉTODO DERIVADO DEL NOMBRE: "Top" (el primero) + "OrderByIdDesc" (ordenado por id descendente) = la boleta con el id más alto.
    // Optional = puede no existir (tabla vacía). Sirve de punto de partida del contador de ids.
    Optional<Boleta> findTopByOrderByIdDesc();

    /** Compras de un cliente, ordenadas por número. */
    // Consulta derivada del nombre: "boletas cuyo clienteId es X, ordenadas por id ascendente".
    // Spring la convierte en un find de MongoDB sin escribir ninguna consulta. La usa el cliente para ver SOLO sus compras.
    List<Boleta> findByClienteIdOrderByIdAsc(Integer clienteId);
}
