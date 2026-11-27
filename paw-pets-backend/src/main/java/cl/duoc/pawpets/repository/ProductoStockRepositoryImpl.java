package cl.duoc.pawpets.repository;

// Imports estáticos: permiten escribir where(...) y query(...) directamente, sin el nombre de la clase.
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import cl.duoc.pawpets.model.Producto;
// MongoTemplate: API de bajo nivel de Spring Data para hacer consultas y actualizaciones personalizadas en MongoDB.
import org.springframework.data.mongodb.core.MongoTemplate;
// Update: construye operaciones de modificación de MongoDB, como $inc (incrementar).
import org.springframework.data.mongodb.core.query.Update;

/** Implementación con MongoTemplate de las operaciones atómicas de stock. */
// Implementación real de las operaciones de stock. Debe llamarse "<Repositorio>Impl" para que Spring Data la detecte.
public class ProductoStockRepositoryImpl implements ProductoStockRepository {

    // Herramienta para hablar directamente con MongoDB.
    private final MongoTemplate mongo;

    // Spring inyecta el MongoTemplate ya configurado con la conexión de MONGODB_URI.
    public ProductoStockRepositoryImpl(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    // Implementa el descuento atómico.
    @Override
    public boolean descontarStock(Integer productoId, int cantidad) {
        // Filtro: el producto Y que tenga stock suficiente. Si no cumple, no se modifica nada.
        // updateFirst = "busca el primer documento que cumpla el filtro y modifícalo", todo en una sola operación en el servidor de Mongo.
        // Si dos clientes compran a la vez, MongoDB las ejecuta una tras otra y el stock jamás queda negativo.
        var resultado = mongo.updateFirst(
                // FILTRO: _id igual al producto Y stock >= cantidad. Si el stock no alcanza, no encuentra nada y no modifica nada.
                query(where("_id").is(productoId).and("stock").gte(cantidad)),
                // MODIFICACIÓN: $inc con número negativo = restar la cantidad al stock.
                new Update().inc("stock", -cantidad),
                // Indica en qué colección operar (la de Producto = "productos").
                Producto.class);
        // Si se modificó 1 documento, se descontó (true). Si fue 0, ya no había stock suficiente (false).
        return resultado.getModifiedCount() == 1;
    }

    // Operación inversa para deshacer una compra que falló a mitad de camino.
    @Override
    public void devolverStock(Integer productoId, int cantidad) {
        // Sin condición de stock: simplemente suma (+cantidad) las unidades al producto.
        mongo.updateFirst(query(where("_id").is(productoId)), new Update().inc("stock", cantidad), Producto.class);
    }
}
