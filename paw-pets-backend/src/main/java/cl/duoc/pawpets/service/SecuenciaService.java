package cl.duoc.pawpets.service;

// Imports estáticos: permiten usar options(), where() y query() sin prefijo.
import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

// IntSupplier: función sin parámetros que devuelve un int. Aquí entrega "el id más alto que ya existe", solo cuando hace falta.
import java.util.function.IntSupplier;
// Document de BSON: un documento MongoDB genérico (clave-valor) para leer el contador.
import org.bson.Document;
// API de bajo nivel para operaciones personalizadas.
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
// Componente de lógica; lo usan Producto, Categoria, Usuario y Boleta Service.
// GENERADOR DE IDs: reemplaza al autoincremental de SQL, que MongoDB no tiene. Evita que dos peticiones simultáneas reciban el mismo id.
@Service
public class SecuenciaService {

    // Colección de MongoDB donde se guarda un contador por cada colección (productos, boletas...).
    static final String COLECCION = "contadores";

    private final MongoTemplate mongo;

    // Inyección del MongoTemplate.
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
    // Devuelve el próximo id de la colección "nombre". maximoActual se evalúa solo para sincronizar con datos preexistentes.
    public int siguiente(String nombre, IntSupplier maximoActual) {
        // 1) Crea el contador si no existe y lo sube al máximo actual si quedó atrás ($max no lo baja nunca)
        // PASO 1 (upsert = update + insert): si el contador no existe lo crea; si existe, aplica $max.
        // $max solo SUBE el valor si el máximo real es mayor: nunca baja el contador. Cubre los datos cargados por el DataSeeder antes de existir el contador.
        mongo.upsert(query(where("_id").is(nombre)), new Update().max("valor", maximoActual.getAsInt()), COLECCION);
        // 2) Suma 1 y devuelve el valor nuevo en la MISMA operación (atómica)
        // PASO 2: findAndModify busca y modifica EN UNA SOLA OPERACIÓN ATÓMICA del servidor; $inc suma 1 al contador.
        Document contador = mongo.findAndModify(query(where("_id").is(nombre)), new Update().inc("valor", 1),
                // Devuelve el documento YA incrementado (no el anterior). Así cada petición recibe un número distinto garantizado.
                options().returnNew(true), Document.class, COLECCION);
        // Extrae el nuevo número, que será el id del registro.
        return contador.getInteger("valor");
    }
}
