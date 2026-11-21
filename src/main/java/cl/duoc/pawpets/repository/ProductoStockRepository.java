package cl.duoc.pawpets.repository;

/**
 * Operaciones de stock que deben ser atómicas (una sola operación en MongoDB).
 * La implementación está en ProductoStockRepositoryImpl; Spring Data la une a ProductoRepository.
 */
public interface ProductoStockRepository {

    /**
     * Descuenta stock SOLO si alcanza: "si stock >= cantidad, réstale cantidad".
     * MongoDB revisa y descuenta en la misma operación, así dos compras simultáneas no pueden
     * dejar el stock negativo.
     *
     * @return true si se descontó; false si ya no había stock suficiente
     */
    boolean descontarStock(Integer productoId, int cantidad);

    /** Devuelve stock (se usa para deshacer una compra que no se pudo completar). */
    void devolverStock(Integer productoId, int cantidad);
}
