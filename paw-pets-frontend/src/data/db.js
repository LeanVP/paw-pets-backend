/**
 * Almacén de datos del frontend, conectado a la API REST.
 * - Guarda en memoria cada colección que entrega el backend (caché compartida por toda la app).
 * - cargar() hace GET; crear/actualizar/eliminar hacen POST/PUT/DELETE y actualizan la caché.
 * - Por cada colección guarda también si está cargando y si hubo un error.
 * - Avisa a los suscriptores (hooks useColeccion/useCarga) para que React redibuje.
 * Los blogs no están en el modelo de la pauta: son contenido estático (seed.js).
 */
// Cliente HTTP: db.js nunca llama fetch directamente, siempre pasa por api.js.
import api from '../services/api';
// Los blogs son contenido estático local; el resto de datos viene del backend.
import { blogs } from './seed';

// Ruta de la API de cada colección
// Tabla colección → endpoint. Ej: 'productos' → GET /api/v1/productos.
const RUTAS = {
  categorias: '/categorias',
  productos: '/productos',
  usuarios: '/usuarios',
  boletas: '/boletas',
};
// Colecciones que dependen del usuario conectado (se olvidan al cambiar de sesión)
// Colecciones que dependen de quién inició sesión: se borran de la caché al cambiar de usuario.
const PRIVADAS = ['usuarios', 'boletas'];

// Un único arreglo vacío reutilizado: así React ve siempre la MISMA referencia y no redibuja de más.
const VACIO = [];
// Estado inicial de una colección: sin datos, sin cargar y sin error.
const INICIAL = { datos: VACIO, cargando: false, cargado: false, error: null };
// Colecciones locales que no se piden al backend (blogs), ya marcadas como cargadas.
const ESTATICAS = { blogs: { ...INICIAL, datos: blogs, cargado: true } };

// estados[coleccion] = { datos, cargando, cargado, error }
// CACHÉ EN MEMORIA: estados[coleccion] = { datos, cargando, cargado, error }. Es compartida por toda la app (un solo almacén).
const estados = {};
// Funciones de los componentes que quieren enterarse de cambios (patrón Observador/Publicador-Suscriptor).
const suscriptores = new Set();

// Notifica a todos los suscriptores que algo cambió.
function avisar() {
  // Cada función suscrita hace que React vuelva a leer el estado y redibuje.
  suscriptores.forEach((funcion) => funcion());
}

// Traduce el nombre de la colección a su endpoint.
function ruta(coleccion) {
  // Falla rápido si se escribe mal el nombre de una colección.
  if (!RUTAS[coleccion]) throw new Error(`Colección desconocida: ${coleccion}`);
  return RUTAS[coleccion];
}

// Reemplaza el estado por uno nuevo (objeto nuevo = React detecta el cambio) y avisa
// Única función que modifica la caché.
function fijar(coleccion, cambios) {
  // Crea un objeto NUEVO (copia + cambios): React detecta cambios por referencia, no por mutación.
  estados[coleccion] = { ...db.estado(coleccion), ...cambios };
  // Después de cambiar, se avisa a los suscriptores.
  avisar();
}

// Objeto "almacén de datos" con sus operaciones: estado, listar, obtener, cargar, crear, actualizar, eliminar...
const db = {
  /** Estado completo de una colección: { datos, cargando, cargado, error }. */
  // Devuelve { datos, cargando, cargado, error } de una colección.
  estado(coleccion) {
    // Los blogs se devuelven directo.
    if (ESTATICAS[coleccion]) return ESTATICAS[coleccion];
    ruta(coleccion);
    // Si aún no se ha pedido nada, el estado inicial.
    return estados[coleccion] ?? INICIAL;
  },

  // Devuelve solo el arreglo de datos.
  listar(coleccion) {
    return this.estado(coleccion).datos;
  },

  /** Busca por id. Number(id) porque el id puede venir como texto desde la URL. */
  // Busca un registro por id dentro de la caché (sin ir al backend).
  obtener(coleccion, id) {
    // Number(id) porque el id de la URL llega como texto ("3") y los ids de la API son números.
    return this.listar(coleccion).find((item) => item.id === Number(id));
  },

  /** GET a la API. No repite la petición si ya se cargó o si está en curso (salvo forzar). */
  // GET al backend. forzar=true ignora la caché.
  async cargar(coleccion, { forzar = false } = {}) {
    // Los blogs no se piden.
    if (ESTATICAS[coleccion]) return;
    // Estado actual de la colección.
    const actual = this.estado(coleccion);
    // Evita peticiones repetidas: si ya está cargando o ya se cargó, no vuelve a pedir.
    if (actual.cargando || (actual.cargado && !forzar)) return;
    // Marca "cargando" para que la pantalla muestre el spinner.
    fijar(coleccion, { cargando: true, error: null });
    try {
      // PETICIÓN REAL: api.get → fetch → backend → MongoDB. Devuelve el arreglo JSON.
      const datos = await api.get(ruta(coleccion));
      // Guarda los datos en la caché y avisa: todos los componentes suscritos se redibujan con los datos nuevos.
      fijar(coleccion, { datos, cargando: false, cargado: true });
    // Si la petición falló (backend apagado, 401, 500...).
    } catch (error) {
      // Se guarda el mensaje para que EstadoCarga lo muestre con el botón Reintentar.
      fijar(coleccion, { cargando: false, error: error.message });
    }
  },

  // Vuelve a pedir ignorando la caché (por ejemplo, tras una compra el stock cambió).
  recargar(coleccion) {
    return this.cargar(coleccion, { forzar: true });
  },

  /** POST: crea en el backend y agrega la respuesta (con su id) a la caché. */
  // POST: crea en el backend.
  async crear(coleccion, datos) {
    // El backend responde el objeto creado ya con su id.
    const nuevo = await api.post(ruta(coleccion), datos);
    // Solo actualiza la caché si esa colección ya estaba cargada.
    if (this.estado(coleccion).cargado) {
      // Agrega el registro nuevo al final SIN volver a pedir toda la lista (el spread crea un arreglo nuevo).
      fijar(coleccion, { datos: [...this.listar(coleccion), nuevo] });
    }
    // Devuelve el creado (Checkout usa su id y estado para navegar).
    return nuevo;
  },

  /** PUT: actualiza en el backend y reemplaza el registro en la caché. */
  // PUT: actualiza en el backend.
  async actualizar(coleccion, id, datos) {
    // El backend responde el registro actualizado.
    const actualizado = await api.put(`${ruta(coleccion)}/${id}`, datos);
    fijar(coleccion, {
      datos: this.listar(coleccion).map((item) =>
        // Reemplaza en la caché solo el registro modificado.
        item.id === actualizado.id ? actualizado : item,
      ),
    });
    return actualizado;
  },

  /** DELETE: elimina en el backend y lo saca de la caché. */
  // DELETE: elimina en el backend.
  async eliminar(coleccion, id) {
    // Si el backend responde error (ej: 409 categoría con productos), lanza excepción y NO se toca la caché.
    await api.eliminar(`${ruta(coleccion)}/${id}`);
    fijar(coleccion, {
      // Quita de la caché el registro eliminado.
      datos: this.listar(coleccion).filter((item) => item.id !== Number(id)),
    });
  },

  /** Carga datos directamente (pruebas unitarias). */
  // Carga datos a mano; se usa en las pruebas unitarias.
  establecer(coleccion, datos) {
    fijar(coleccion, { datos, cargando: false, cargado: true, error: null });
  },

  /** Permite que un componente se entere de los cambios; devuelve la función para cancelar. */
  // Registra un interesado en los cambios (lo usa useSyncExternalStore de React).
  suscribir(funcion) {
    // Lo agrega al Set.
    suscriptores.add(funcion);
    // Devuelve la función para DESUSCRIBIRSE (evita fugas de memoria al desmontar un componente).
    return () => suscriptores.delete(funcion);
  },

  /** Olvida los datos del usuario anterior (al iniciar o cerrar sesión). */
  // Al iniciar/cerrar sesión se descartan boletas y usuarios del usuario anterior.
  olvidarPrivadas() {
    // Elimina esas entradas de la caché.
    PRIVADAS.forEach((coleccion) => delete estados[coleccion]);
    avisar();
  },

  /** Vacía toda la caché (pruebas). */
  // Vacía TODA la caché (se usa en las pruebas).
  reiniciar() {
    Object.keys(estados).forEach((coleccion) => delete estados[coleccion]);
    avisar();
  },
};

// Exportación única: todos los componentes comparten este mismo objeto (patrón singleton de módulo).
export default db;
