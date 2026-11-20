package cl.duoc.pawpets.repository;

import cl.duoc.pawpets.model.Boleta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Acceso a la colección "boletas". */
public interface BoletaRepository extends MongoRepository<Boleta, Integer> {

    Optional<Boleta> findTopByOrderByIdDesc();

    /** Compras de un cliente, ordenadas por número. */
    List<Boleta> findByClienteIdOrderByIdAsc(Integer clienteId);
}
