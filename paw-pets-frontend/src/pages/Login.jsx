// useState: guarda datos que cambian y obligan a redibujar la pantalla (errores, "enviando...").
import { useState } from 'react';
// Link: enlace sin recargar. useNavigate: cambia de ruta por código. useLocation: lee datos de la ruta actual (state.desde).
import { Link, useLocation, useNavigate } from 'react-router-dom';
// Componente reutilizable: etiqueta + input + mensaje de error.
import CampoFormulario from '../components/CampoFormulario';
// Cuadro rojo de error.
import { AlertaError } from '../components/Estados';
// Íconos Material Symbols.
import Icono from '../components/Icono';
// Hook propio para leer la sesión y sus funciones (AuthContext).
import useAuth from '../hooks/useAuth';
// Hook propio: maneja valores, errores y envío de un formulario.
import useFormulario from '../hooks/useFormulario';
// Lista de roles que entran al panel: ['admin', 'vendedor'].
import { ROLES_PANEL } from '../utils/roles';
// Reglas de validación del frontend (utils/validaciones.js).
import { correo, requerido } from '../utils/validaciones';

// Reglas por campo: correo obligatorio y con formato; clave obligatoria. La validación real y final la repite el backend (@Valid).
const REGLAS = { correo: [requerido, correo], clave: [requerido] };

/**
 * Inicio de sesión contra el backend (POST /api/v1/auth/login).
 * Admin y vendedor van al panel; el cliente vuelve a donde estaba (por ejemplo, el checkout).
 */
// PÁGINA /login. Flujo: formulario → AuthProvider.iniciarSesion → api.post('/auth/login') → backend → JWT → sesión guardada → navegación.
export default function Login() {
  // Obtiene del contexto la función que hace el login.
  const { iniciarSesion } = useAuth();
  // Función para ir a otra página.
  const navegar = useNavigate();
  // Datos que otra página pasó al navegar. Checkout manda state={{ desde: '/checkout' }} para volver después de iniciar sesión.
  const { state } = useLocation();
  // Estado: true si el backend respondió 401 (correo o clave incorrectos).
  const [credencialesInvalidas, setCredencialesInvalidas] = useState(false);
  // Estado: mensaje de errores técnicos (backend apagado, 500).
  const [errorServidor, setErrorServidor] = useState(null);
  // Estado: true mientras se espera la respuesta; desactiva el botón y muestra un spinner.
  const [enviando, setEnviando] = useState(false);

  // El hook entrega: valores (lo escrito), errores (por campo), cambiar (onChange) y enviar (onSubmit).
  const { valores, errores, cambiar, enviar } = useFormulario(
    // Valores iniciales de los campos.
    { correo: '', clave: '' },
    // Reglas de validación a aplicar antes de enviar.
    REGLAS,
    // Esta función SOLO se ejecuta si todas las reglas pasan.
    async ({ correo, clave }) => {
      // Bloquea el botón.
      setEnviando(true);
      // Limpia el error anterior.
      setCredencialesInvalidas(false);
      setErrorServidor(null);
      try {
        // LLAMADA AL BACKEND (vía AuthProvider → api.js → POST /auth/login). await espera la respuesta.
        const usuario = await iniciarSesion(correo, clave);
        // iniciarSesion devuelve null si fue 401.
        if (!usuario) {
          // Muestra "Correo o contraseña incorrectos."
          setCredencialesInvalidas(true);
          setEnviando(false);
          return;
        }
        // Redirección según rol: admin/vendedor → /admin; cliente → a donde estaba (state.desde) o al inicio "/".
        navegar(ROLES_PANEL.includes(usuario.rol) ? '/admin' : (state?.desde ?? '/'));
      // Errores que no son de credenciales (ej: el backend está apagado).
      } catch (e) {
        // Error que no es de credenciales: por ejemplo, el backend está apagado
        // Muestra el mensaje de ErrorApi.
        setErrorServidor(e.message);
        setEnviando(false);
      }
    },
  );

  // Lo que se dibuja en pantalla (JSX).
  return (
    <section className="col-md-7 col-lg-5 col-xl-4 mx-auto">
      {/* Título de la página. */}
      <h1 className="section-title">Iniciar sesión</h1>
      {/* Si llegó desde el checkout, avisa que debe iniciar sesión para continuar. (&& = "si lo de la izquierda es verdadero, dibuja lo de la derecha") */}
      {state?.desde && (
        <p className="text-secondary">Inicia sesión para continuar con tu compra.</p>
      )}
      {/* onSubmit={enviar}: al enviar corre la validación de useFormulario. noValidate desactiva la validación nativa del navegador para usar la propia. */}
      <form className="card card-body" onSubmit={enviar} noValidate>
        {/* Campo de correo. Es "controlado": su valor viene del estado (valores.correo) y cada tecla llama a cambiar. */}
        <CampoFormulario
          etiqueta="Correo electrónico"
          nombre="correo"
          // tipo = type del input; autoComplete ayuda al navegador a sugerir datos.
          tipo="email"
          autoComplete="email"
          // valor, error y onChange conectan el campo con useFormulario.
          valor={valores.correo}
          error={errores.correo}
          onChange={cambiar}
        />
        {/* Campo de contraseña (tipo="password" oculta lo escrito). */}
        <CampoFormulario
          etiqueta="Contraseña"
          nombre="clave"
          tipo="password"
          autoComplete="current-password"
          valor={valores.clave}
          error={errores.clave}
          onChange={cambiar}
        />
        {/* Error de credenciales (solo se dibuja si es true). */}
        {credencialesInvalidas && <AlertaError mensaje="Correo o contraseña incorrectos." />}
        {/* Error técnico; AlertaError no dibuja nada si mensaje es null. */}
        <AlertaError mensaje={errorServidor} />
        {/* Botón enviar: disabled mientras envía. */}
        <button type="submit" className="btn btn-primary" disabled={enviando}>
          {enviando ? (
            <span className="spinner-border spinner-border-sm" aria-hidden="true" />
          ) : (
            <Icono nombre="login" />
          )}
          Entrar
        </button>
        {/* Enlace al registro (Link evita recargar la página). */}
        <p className="mt-3 mb-0 text-center">
          ¿No tienes cuenta? <Link to="/registro">Regístrate</Link>
        </p>
      </form>
    </section>
  );
}
