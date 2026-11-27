// Hooks de React: estado, efectos y funciones estables
// useState: guarda la sesión. useEffect: ejecuta efectos (temporizadores, eventos). useCallback: mantiene estable la identidad de una función entre renders.
import { useCallback, useEffect, useState } from 'react';
// db: se olvidan los datos privados (usuarios, boletas) al cambiar de sesión
// Para olvidar boletas/usuarios del usuario anterior al cambiar de sesión.
import db from '../data/db';
// api: cliente REST y utilidades de la sesión guardada
// Cliente REST y utilidades de sesión (leer/guardar/borrar, vigencia del token).
import api, {
  borrarSesion,
  EVENTO_SESION_EXPIRADA,
  guardarSesion,
  leerSesion,
  tokenVigente,
  vencimientoToken,
} from '../services/api';
// AuthContext: el contexto que este Provider llena con datos
// El contexto que este Provider llena con datos.
import { AuthContext } from './contextos';

/** Cada cuánto se pide un token nuevo al backend mientras la página está abierta. */
// 15 minutos expresados en milisegundos (15 × 60 × 1000).
const RENOVAR_CADA_MS = 15 * 60 * 1000;

// Sesión guardada solo si su token todavía no vence
// Se ejecuta UNA vez al abrir la página: recupera la sesión si el token aún no vence.
function sesionInicial() {
  // Lee { token, usuario } desde localStorage.
  const guardada = leerSesion();
  // Sesión válida: se retoma (por eso F5 no cierra la sesión).
  if (guardada && tokenVigente(guardada.token)) return guardada;
  // Token vencido o ausente: limpia lo guardado.
  borrarSesion();
  // null = nadie con sesión iniciada.
  return null;
}

/**
 * Sesión del usuario con JWT:
 * - Persistencia: { token, usuario } en localStorage, así la sesión sobrevive a recargar (F5).
 * - Vencimiento: al abrir la página se descarta un token vencido, y se cierra la sesión justo cuando vence.
 * - Renovación: al abrir la página y cada 15 minutos se pide un token nuevo (POST /auth/refresh).
 * - Si el backend responde 401 (token inválido o vencido), la sesión se cierra sola.
 * El rol que vale es el del token firmado: cambiarlo en localStorage no da permisos en el backend.
 */
// COMPONENTE PROVIDER: guarda la sesión y la comparte con toda la app vía Context. children = todo lo que envuelve (ver App.jsx).
export function AuthProvider({ children }) {
  // Estado: { token, usuario } o null. Se pasa la FUNCIÓN sesionInicial (sin paréntesis) para que se ejecute solo en el primer render.
  const [sesion, setSesion] = useState(sesionInicial);

  // Guarda la sesión y olvida los datos del usuario anterior
  // Deja iniciada una sesión nueva.
  const abrirSesion = useCallback((nueva) => {
    // La persiste en localStorage.
    guardarSesion(nueva);
    // Borra de la caché los datos del usuario anterior (boletas/usuarios).
    db.olvidarPrivadas();
    // Actualiza el estado de React: la interfaz se redibuja (Header muestra el usuario, etc.).
    setSesion(nueva);
  // Arreglo vacío: la función se crea una sola vez.
  }, []);

  // Cierra la sesión: borra localStorage, olvida datos privados y deja el estado en null.
  const cerrarSesion = useCallback(() => {
    borrarSesion();
    db.olvidarPrivadas();
    setSesion(null);
  }, []);

  /** Devuelve el usuario si las credenciales son correctas; null si no (401). */
  // Login. Devuelve el usuario si fue correcto, o null si las credenciales son malas.
  async function iniciarSesion(correo, clave) {
    try {
      // POST /api/v1/auth/login con { correo, clave }. El backend responde { token, usuario }.
      const respuesta = await api.post('/auth/login', { correo: correo.trim(), clave });
      // Guarda token y usuario.
      abrirSesion(respuesta);
      // Se devuelve el usuario: Login.jsx revisa su rol para decidir a dónde navegar.
      return respuesta.usuario;
    } catch (error) {
      // 401 = correo o clave incorrectos: no es un error "técnico", se devuelve null.
      if (error.estado === 401) return null;
      // Cualquier otro error (backend caído, 500) se propaga para mostrarlo.
      throw error;
    }
  }

  /** Crea un cliente y abre su sesión. Devuelve null si el correo ya existe (409). */
  // Crea una cuenta de cliente y abre su sesión.
  async function registrar(datos) {
    try {
      // POST /api/v1/auth/registro. El backend fuerza el rol "cliente".
      const respuesta = await api.post('/auth/registro', { ...datos, correo: datos.correo.trim() });
      abrirSesion(respuesta);
      return respuesta.usuario;
    } catch (error) {
      // 409 = correo ya registrado → null.
      if (error.estado === 409) return null;
      throw error;
    }
  }

  /** Actualiza los datos del usuario conectado (por ejemplo, después de editar el perfil). */
  // Actualiza solo los datos del usuario (ej: tras editar el perfil) conservando el token.
  const actualizarUsuario = useCallback((usuario) => {
    // Forma funcional de setState: recibe el estado más reciente.
    setSesion((actual) => {
      // Copia la sesión cambiando solo el usuario.
      const nueva = { ...actual, usuario };
      guardarSesion(nueva);
      return nueva;
    });
  }, []);

  // true/false: sirve como dependencia estable del efecto de renovación.
  const hayToken = Boolean(sesion?.token);
  // Token actual (o undefined).
  const token = sesion?.token;

  // EFECTO 1: renovación automática del token.
  // Renovación del token: al abrir la página y cada 15 minutos
  // Un efecto corre después de dibujar; aquí depende de [hayToken].
  useEffect(() => {
    // Sin sesión no hay nada que renovar.
    if (!hayToken) return undefined;
    // Función que pide un token nuevo.
    const renovar = () =>
      api
        // POST /api/v1/auth/refresh (con el token actual en Authorization). Devuelve un token nuevo con nuevo vencimiento.
        .post('/auth/refresh')
        .then((nueva) => {
          // Persiste el token renovado.
          guardarSesion(nueva);
          setSesion(nueva);
        })
        // Si falla por 401, ya lo maneja el evento de sesión expirada.
        .catch(() => {}); // si falla por 401, el evento de sesión expirada cierra la sesión
    // Se renueva apenas se abre la página...
    renovar();
    // ...y luego cada 15 minutos, así una sesión activa no se corta a la hora.
    const intervalo = setInterval(renovar, RENOVAR_CADA_MS);
    // LIMPIEZA: al cerrar sesión o desmontar, se cancela el temporizador.
    return () => clearInterval(intervalo);
  }, [hayToken]);

  // EFECTO 2: cierre automático cuando vence el token.
  // Cierra la sesión justo cuando vence el token (si no se alcanzó a renovar)
  // Se re-ejecuta cada vez que cambia el token (por ejemplo, tras renovarlo).
  useEffect(() => {
    if (!token) return undefined;
    // Milisegundos que le quedan al token.
    const restante = vencimientoToken(token) - Date.now();
    // Programa el cierre de sesión para ese instante (Math.max evita tiempos negativos).
    const temporizador = setTimeout(cerrarSesion, Math.max(restante, 0));
    // Limpieza: cancela el temporizador anterior.
    return () => clearTimeout(temporizador);
  }, [token, cerrarSesion]);

  // EFECTO 3: escucha el aviso de api.js.
  // El cliente de la API avisa cuando el backend rechaza el token (401)
  useEffect(() => {
    // Cuando api.js recibe un 401 dispara este evento y aquí se cierra la sesión.
    window.addEventListener(EVENTO_SESION_EXPIRADA, cerrarSesion);
    // Limpieza: quita el oyente.
    return () => window.removeEventListener(EVENTO_SESION_EXPIRADA, cerrarSesion);
  }, [cerrarSesion]);

  return (
    // Publica el valor para toda la app: cualquier componente lo lee con useAuth().
    <AuthContext.Provider
      value={{
        // El usuario conectado, o null si no hay sesión.
        usuario: sesion?.usuario ?? null,
        // Funciones disponibles para los componentes: login, registro, logout y actualizar perfil.
        iniciarSesion,
        registrar,
        cerrarSesion,
        actualizarUsuario,
      }}
    >
      {/* Aquí se dibuja el resto de la aplicación (todo lo que App.jsx puso dentro de <AuthProvider>). */}
      {children}
    </AuthContext.Provider>
  );
}
