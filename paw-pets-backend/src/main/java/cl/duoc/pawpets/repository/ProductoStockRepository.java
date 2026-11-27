package cl.duoc.pawpets.repository;

/**
 * Operaciones de stock que deben ser atómicas (una sola operación en MongoDB).
 * La implementación está en ProductoStockRepositoryImpl; Spring Data la une a ProductoRepository.
 */
// Interfaz con operaciones PERSONALIZADAS de stock (Spring Data no puede deducirlas por el nombre).
// Su implementación es ProductoStockRepositoryImpl; la convención "NombreImpl" hace que Spring las una automáticamente.
public interface ProductoStockRepository {

    /**
     * Descuenta stock SOLO si alcanza: "si stock >= cantidad, réstale cantidad".
     * MongoDB revisa y descuenta en la misma operación, así dos compras simultáneas no pueden
     * dejar el stock negativo.
     *
     * @return true si se descontó; false si ya no había stock suficiente
     */
    // Resta stock solo si alcanza, en UNA sola operación de MongoDB (atómica). Devuelve true si descontó, false si no había stock.
    boolean descontarStock(Integer productoId, int cantidad);

    /** Devuelve stock (se usa para deshacer una compra que no se pudo completar). */
    // Operación inversa (compensación): devuelve unidades si la compra falla a mitad de camino.
    void devolverStock(Integer productoId, int cantidad);
}
