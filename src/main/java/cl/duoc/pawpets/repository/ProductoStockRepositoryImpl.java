package cl.duoc.pawpets.repository;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import cl.duoc.pawpets.model.Producto;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;

/** Implementación con MongoTemplate de las operaciones atómicas de stock. */
public class ProductoStockRepositoryImpl implements ProductoStockRepository {

    private final MongoTemplate mongo;

    public ProductoStockRepositoryImpl(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public boolean descontarStock(Integer productoId, int cantidad) {
        // Filtro: el producto Y que tenga stock suficiente. Si no cumple, no se modifica nada.
        var resultado = mongo.updateFirst(
                query(where("_id").is(productoId).and("stock").gte(cantidad)),
                new Update().inc("stock", -cantidad),
                Producto.class);
        return resultado.getModifiedCount() == 1;
    }

    @Override
    public void devolverStock(Integer productoId, int cantidad) {
        mongo.updateFirst(query(where("_id").is(productoId)), new Update().inc("stock", cantidad), Producto.class);
    }
}
