package cl.duoc.pawpets.service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;

/**
 * Versión en memoria de SecuenciaService para las pruebas unitarias (no necesita MongoDB).
 * Se comporta igual: parte desde el id más alto existente y suma 1 en cada llamada.
 */
class SecuenciaEnMemoria extends SecuenciaService {

    private final Map<String, Integer> contadores = new HashMap<>();

    SecuenciaEnMemoria() {
        super(null);
    }

    @Override
    public int siguiente(String nombre, IntSupplier maximoActual) {
        int actual = Math.max(contadores.getOrDefault(nombre, 0), maximoActual.getAsInt());
        contadores.put(nombre, actual + 1);
        return actual + 1;
    }
}
