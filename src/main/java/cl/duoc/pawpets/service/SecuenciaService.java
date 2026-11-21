package cl.duoc.pawpets.service;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.util.function.IntSupplier;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Genera ids numéricos correlativos (1, 2, 3...) sin repetirse aunque lleguen dos peticiones al mismo tiempo.
 *
 * Antes se usaba "id más alto + 1": si dos compras llegaban a la vez, ambas leían el mismo máximo,
 * recibían el mismo id y MongoDB guardaba la segunda ENCIMA de la primera (se perdía una boleta).
 *
 * Ahora cada colección tiene un contador en la colección "contadores" que MongoDB incrementa con
 * findAndModify + $inc: es una sola operación atómica, así que dos peticiones nunca reciben el mismo número.
 */
@Service
public class SecuenciaService {

    static final String COLECCION = "contadores";

    private final MongoTemplate mongo;

    public SecuenciaService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /**
     * Entrega el siguiente id de la colección indicada.
     *
     * @param nombre        nombre del contador (por ejemplo "boletas")
     * @param maximoActual  id más alto que ya existe; con $max el contador nunca queda por debajo de los
     *                      datos cargados antes de que existiera (por ejemplo, la carga inicial)
     */
    public int siguiente(String nombre, IntSupplier maximoActual) {
        // 1) Crea el contador si no existe y lo sube al máximo actual si quedó atrás ($max no lo baja nunca)
        mongo.upsert(query(where("_id").is(nombre)), new Update().max("valor", maximoActual.getAsInt()), COLECCION);
        // 2) Suma 1 y devuelve el valor nuevo en la MISMA operación (atómica)
        Document contador = mongo.findAndModify(query(where("_id").is(nombre)), new Update().inc("valor", 1),
                options().returnNew(true), Document.class, COLECCION);
        return contador.getInteger("valor");
    }
}
