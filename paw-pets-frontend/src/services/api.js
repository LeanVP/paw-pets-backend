/**
 * Cliente de la API REST del backend (único lugar del frontend que hace peticiones HTTP).
 * - URL base desde la variable de entorno VITE_API_URL (archivo .env, ver .env.example).
 * - Agrega el token JWT de la sesión en el header Authorization.
 * - Convierte las respuestas de error del backend ({ estado, error, mensaje, campos }) en ErrorApi.
 * - Si el backend responde 401 a una petición con token, avisa que la sesión expiró.
 */

// import.meta.env lo completa Vite con las variables VITE_* del .env.
// El ?. evita un error cuando el código corre fuera de Vite (pruebas con Karma).
// URL BASE de la API. Vite reemplaza import.meta.env.VITE_API_URL por el valor del archivo .env (http://localhost:8080/api/v1).
// ?? = "si es null/undefined usa el valor por defecto". .replace(/\/$/, '') quita una "/" final para no generar "//" al concatenar rutas.
const URL_BASE = (import.meta.env?.VITE_API_URL ?? 'http://localhost:8080/api/v1').replace(
  /\/$/,
  '',
);

/** Nombre con que se guarda la sesión { token, usuario } en localStorage. */
// Nombre de la clave bajo la que se guarda la sesión { token, usuario } en el localStorage del navegador.
export const CLAVE_SESION = 'pawpets:sesion';
/** Evento que avisa al AuthProvider que el backend rechazó el token. */
// Nombre de un evento personalizado del navegador: api.js lo "dispara" y AuthProvider lo "escucha" (comunicación desacoplada entre capas).
export const EVENTO_SESION_EXPIRADA = 'pawpets:sesion-expirada';

/** Error de la API con el código HTTP y los errores por campo (si los hay). */
// Clase de error propia: además del mensaje lleva el código HTTP (estado) y los errores por campo del backend (campos).
export class ErrorApi extends Error {
  // campos = {} es el valor por defecto cuando el backend no manda errores por campo.
  constructor(mensaje, estado, campos = {}) {
    // Llama al constructor de Error para guardar el mensaje (se lee luego con error.message).
    super(mensaje);
    // Código HTTP (401, 404, 409...). Los componentes lo usan para decidir qué hacer (ej: 401 en el login = credenciales malas).
    this.estado = estado;
    // Mapa { campo: mensaje } que viene de GlobalExceptionHandler (400 de validación).
    this.campos = campos;
  }
}

// ---------- Sesión guardada en el navegador ----------

// Lee la sesión guardada en el navegador.
export function leerSesion() {
  // try/catch porque JSON.parse lanza error si el texto guardado está dañado.
  try {
    // localStorage solo guarda texto: getItem lo entrega y JSON.parse lo convierte en objeto { token, usuario }.
    return JSON.parse(localStorage.getItem(CLAVE_SESION));
  } catch {
    // Si falla o no hay nada, se considera "sin sesión".
    return null;
  }
}

// Guarda la sesión para que sobreviva a recargar la página (F5).
export function guardarSesion(sesion) {
  // JSON.stringify convierte el objeto en texto, porque localStorage solo admite texto.
  localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
}

// Cierra la sesión en el navegador.
export function borrarSesion() {
  // Elimina la clave: el siguiente pedir() ya no enviará Authorization.
  localStorage.removeItem(CLAVE_SESION);
}

/**
 * Lee el vencimiento (exp) del JWT. El payload es la parte del medio del token, en base64url.
 * Solo sirve para no usar un token muerto: la validación real (firma) la hace el backend.
 */
// Averigua cuándo vence el JWT leyendo su payload (sin verificar la firma: eso es trabajo del backend).
export function vencimientoToken(token) {
  try {
    // Un JWT son 3 partes separadas por puntos: cabecera.payload.firma. Se toma la del medio [1].
    // El payload está en base64url: se cambian "-" por "+" y "_" por "/" para que atob() lo entienda.
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    // atob decodifica base64 → texto JSON → objeto; "exp" viene en SEGUNDOS, se multiplica por 1000 para tener milisegundos (como Date.now()).
    return JSON.parse(atob(payload)).exp * 1000;
  } catch {
    // Si el token es inválido se devuelve 0 (= ya vencido).
    return 0;
  }
}

// true solo si hay token Y su vencimiento es posterior a ahora.
export const tokenVigente = (token) => Boolean(token) && vencimientoToken(token) > Date.now();

// ---------- Peticiones ----------

// FUNCIÓN CENTRAL: todas las llamadas HTTP del frontend pasan por aquí. async porque fetch es asíncrono (devuelve promesas).
async function pedir(metodo, ruta, cuerpo) {
  // Cabeceras (headers) de la petición; parte vacío.
  const cabeceras = {};
  // Si se envía cuerpo (POST/PUT), se avisa al backend que es JSON.
  if (cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json';
  // Lee el token guardado. El ?. evita error si no hay sesión.
  const token = leerSesion()?.token;
  // Solo si el token no ha vencido se agrega "Authorization: Bearer <token>": así el backend (SecurityConfig) identifica al usuario.
  if (tokenVigente(token)) cabeceras.Authorization = `Bearer ${token}`;

  // Se declara fuera del try para poder usarla después.
  let respuesta;
  try {
    // fetch envía la petición HTTP al backend; await espera la respuesta sin congelar la pantalla.
    respuesta = await fetch(URL_BASE + ruta, {
      // Verbo HTTP: GET, POST, PUT o DELETE.
      method: metodo,
      // Cabeceras armadas arriba.
      headers: cabeceras,
      // El cuerpo se convierte a texto JSON; en GET/DELETE no hay cuerpo.
      body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
    });
  // fetch solo lanza error si NO hubo respuesta (backend apagado, sin red o bloqueo CORS).
  } catch {
    // fetch solo falla así cuando no hay conexión con el servidor (apagado, CORS, sin red).
    // Se transforma en un ErrorApi con estado 0 y un mensaje claro para el usuario.
    throw new ErrorApi(
      'No se pudo conectar con el servidor. Revisa que el backend esté encendido.',
      0,
    );
  }

  // 204 No Content (por ejemplo, al eliminar): no hay cuerpo que leer.
  // 204 No Content (DELETE exitoso): no hay cuerpo que leer, se devuelve null.
  if (respuesta.status === 204) return null;
  // Lee el cuerpo como JSON; si no es JSON válido devuelve null en vez de romper.
  const datos = await respuesta.json().catch(() => null);

  // respuesta.ok es false para cualquier código 4xx/5xx: aquí se procesan los errores del backend.
  if (!respuesta.ok) {
    // 401 estando logueado = el token venció o es inválido.
    if (respuesta.status === 401 && cabeceras.Authorization) {
      // Se avisa a toda la app; AuthProvider escucha este evento y cierra la sesión.
      window.dispatchEvent(new Event(EVENTO_SESION_EXPIRADA));
    }
    // Convierte el JSON de error del backend { estado, error, mensaje, campos } en un ErrorApi.
    throw new ErrorApi(
      // Usa el "mensaje" del backend; si no existe, un texto genérico.
      datos?.mensaje ?? `Error ${respuesta.status} del servidor.`,
      respuesta.status,
      datos?.campos ?? {},
    );
  }
  // Éxito: devuelve el JSON ya convertido en objeto/arreglo JavaScript.
  return datos;
}

// Objeto con 4 atajos, uno por verbo HTTP, para que el resto del código escriba api.get('/productos') en vez de armar fetch.
const api = {
  // GET: consultar datos.
  get: (ruta) => pedir('GET', ruta),
  // POST: crear (el cuerpo por defecto es {} para poder llamar api.post('/auth/refresh') sin datos).
  post: (ruta, cuerpo = {}) => pedir('POST', ruta, cuerpo),
  // PUT: actualizar.
  put: (ruta, cuerpo) => pedir('PUT', ruta, cuerpo),
  // DELETE: eliminar (se llama "eliminar" porque delete es palabra reservada de JavaScript).
  eliminar: (ruta) => pedir('DELETE', ruta),
};

// Exportación por defecto: otros archivos lo importan con import api from '../services/api'.
export default api;
