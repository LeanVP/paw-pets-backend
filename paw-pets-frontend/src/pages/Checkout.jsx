// useState: estados "enviando" y "error".
import { useState } from 'react';
// Link: enlaces internos. useNavigate: ir a la pantalla de resultado de la compra.
import { Link, useNavigate } from 'react-router-dom';
import CampoFormulario from '../components/CampoFormulario';
// Región → comuna dependientes (selects encadenados).
import CamposDireccion from '../components/CamposDireccion';
// Componentes de estado: error, carga desde la API y "vacío".
import { AlertaError, EstadoCarga, EstadoVacio } from '../components/Estados';
import Icono from '../components/Icono';
// Almacén de datos: aquí se usa db.crear('boletas') = POST al backend.
import db from '../data/db';
import useAuth from '../hooks/useAuth';
// Hook del carrito (items, total, vaciar).
import useCarrito from '../hooks/useCarrito';
import useFormulario from '../hooks/useFormulario';
// Da formato de pesos chilenos ($18.990).
import { precioCLP } from '../utils/formato';
import { correo, requerido } from '../utils/validaciones';

// Medio de pago de prueba: el backend (BoletaService) lo reconoce y deja la boleta Rechazada. Debe escribirse IGUAL en ambos lados.
const MEDIO_RECHAZADO = 'Tarjeta sin fondos (simula error)';
// Opciones del select: [{ valor, texto }].
const MEDIOS = ['Tarjeta de débito', 'Tarjeta de crédito', MEDIO_RECHAZADO].map((m) => ({
  valor: m,
  texto: m,
}));
// Campos obligatorios. La validación definitiva la repite el backend con BoletaRequest.
const REGLAS = {
  nombre: [requerido],
  apellidos: [requerido],
  correo: [requerido, correo],
  calle: [requerido],
  region: [requerido],
  comuna: [requerido],
  medioPago: [requerido],
};

/**
 * Finalizar compra. Exige sesión (la boleta queda asociada al usuario, Figura 1).
 * Envía al backend solo { productoId, cantidad }: el servidor calcula precios y total,
 * valida y descuenta el stock y decide si el pago queda Pagada o Rechazada.
 */
// PÁGINA /checkout: decide qué mostrar según sesión, rol y carrito.
export default function Checkout() {
  // Usuario conectado (o null).
  const { usuario } = useAuth();
  // Líneas del carrito.
  const { items } = useCarrito();

  // Sin sesión: se pide iniciar sesión (la compra queda asociada a un usuario).
  if (!usuario) {
    // Retorno anticipado: no se llega al formulario.
    return (
      <section className="col-lg-6 mx-auto">
        <h1 className="section-title">Finalizar compra</h1>
        <EstadoVacio
          icono="lock"
          // Botones que se pasan como prop al componente EstadoVacio.
          accion={
            <div className="d-flex flex-wrap gap-2 justify-content-center">
              {/* state viaja con la navegación: Login lo lee para volver al checkout. */}
              <Link to="/login" state={{ desde: '/checkout' }} className="btn btn-primary">
                <Icono nombre="login" />
                Iniciar sesión
              </Link>
              <Link to="/registro" className="btn btn-outline-dark">
                <Icono nombre="person_add" />
                Crear cuenta
              </Link>
            </div>
          }
        >
          Para pagar debes iniciar sesión. Tu carrito se mantiene guardado.
        </EstadoVacio>
      </section>
    );
  }

  // El vendedor solo consulta; además el backend lo rechazaría con 403 (SecurityConfig).
  if (usuario.rol === 'vendedor') {
    return (
      <AlertaError mensaje="El perfil vendedor puede consultar productos y órdenes, pero no realizar compras." />
    );
  }

  return (
    // Pide los productos al backend y muestra "Cargando…" o el error hasta que lleguen; luego dibuja su contenido.
    <EstadoCarga colecciones={['productos']}>
      {/* Carrito vacío → mensaje; si no → formulario de pago. */}
      {items.length === 0 ? (
        <EstadoVacio
          icono="shopping_cart"
          accion={
            <Link to="/productos" className="btn btn-primary">
              Ver productos
            </Link>
          }
        >
          No hay productos para pagar.
        </EstadoVacio>
      ) : (
        // Se le pasa el usuario para precargar sus datos.
        <FormularioPago usuario={usuario} />
      )}
    </EstadoCarga>
  );
}

// Formulario real de pago (componente separado para usar hooks solo cuando corresponde).
function FormularioPago({ usuario }) {
  // Del carrito: líneas, total mostrado (solo informativo) y función para vaciarlo.
  const { items, total, vaciar } = useCarrito();
  // Para ir a /compra/exitosa/:id o /compra/error/:id.
  const navegar = useNavigate();
  // true mientras el backend procesa la compra.
  const [enviando, setEnviando] = useState(false);
  // Mensaje de error del backend (ej: 409 stock insuficiente).
  const [error, setError] = useState(null);

  // Valores iniciales: precarga datos del usuario con sesión; ?? '' evita undefined.
  const inicial = {
    nombre: usuario.nombre ?? '',
    apellidos: usuario.apellidos ?? '',
    correo: usuario.correo ?? '',
    calle: usuario.calle ?? '',
    region: usuario.region ?? '',
    comuna: usuario.comuna ?? '',
    indicaciones: '',
    // Vacío: el usuario debe elegir uno.
    medioPago: '',
  };

  // Se ejecuta solo si el formulario es válido.
  const { valores, errores, cambiar, enviar } = useFormulario(inicial, REGLAS, async (datos) => {
    // Bloquea el botón para evitar doble pago.
    setEnviando(true);
    setError(null);
    try {
      // FLUJO CLAVE: db.crear → api.post → fetch con JWT → SecurityConfig (rol) → BoletaController → BoletaService → MongoDB.
      // POST /api/v1/boletas
      // Espera la boleta creada que devuelve el backend (con id, total y estado).
      const boleta = await db.crear('boletas', {
        // Datos de envío y medio de pago del formulario.
        ...datos,
        // SOLO se envían productoId y cantidad. Precios y total NO viajan: los calcula el servidor (seguridad).
        items: items.map(({ producto, cantidad }) => ({ productoId: producto.id, cantidad })),
      });
      // El stock cambió en el backend: se vuelve a pedir el catálogo
      // El stock cambió en el backend: se vuelve a pedir el catálogo.
      db.recargar('productos');
      // El backend decidió el estado.
      if (boleta.estado === 'Pagada') {
        // Pagada → pantalla de éxito con el número de boleta.
        navegar(`/compra/exitosa/${boleta.id}`);
        // Se vacía el carrito solo si se pagó.
        vaciar();
      } else {
        // Rechazada → pantalla de error (el carrito se conserva para reintentar).
        navegar(`/compra/error/${boleta.id}`);
      }
    // Error HTTP del backend (ErrorApi).
    } catch (e) {
      // Por ejemplo, 409 "Stock insuficiente..."
      // Se muestra el mensaje en pantalla (viene de GlobalExceptionHandler).
      setError(e.message);
      // Se reactiva el botón para reintentar.
      setEnviando(false);
    }
  });

  // Dibuja el formulario.
  return (
    <section className="col-lg-8 mx-auto">
      <h1 className="section-title">Finalizar compra</h1>
      {/* Al enviar corre la validación y luego la función de arriba. */}
      <form className="card card-body" onSubmit={enviar} noValidate>
        {/* Total mostrado al cliente; el total oficial lo calcula el servidor. */}
        <p className="h5 text-end">Total a pagar: {precioCLP(total)}</p>
        <h2 className="h5">Información del cliente</h2>
        <div className="row">
          <div className="col-md-6">
            {/* Campos controlados: valor, error y onChange conectados a useFormulario. */}
            <CampoFormulario
              etiqueta="Nombre"
              nombre="nombre"
              valor={valores.nombre}
              error={errores.nombre}
              onChange={cambiar}
            />
          </div>
          <div className="col-md-6">
            <CampoFormulario
              etiqueta="Apellidos"
              nombre="apellidos"
              valor={valores.apellidos}
              error={errores.apellidos}
              onChange={cambiar}
            />
          </div>
        </div>
        <CampoFormulario
          etiqueta="Correo electrónico"
          nombre="correo"
          tipo="email"
          valor={valores.correo}
          error={errores.correo}
          onChange={cambiar}
        />
        <h2 className="h5">Dirección de entrega</h2>
        {/* Selects región/comuna dependientes. */}
        <CamposDireccion valores={valores} errores={errores} cambiar={cambiar} />
        <CampoFormulario
          etiqueta="Indicaciones para la entrega (opcional)"
          nombre="indicaciones"
          tipo="textarea"
          valor={valores.indicaciones}
          onChange={cambiar}
        />
        <CampoFormulario
          etiqueta="Medio de pago"
          nombre="medioPago"
          tipo="select"
          opciones={MEDIOS}
          valor={valores.medioPago}
          error={errores.medioPago}
          onChange={cambiar}
        />
        {/* Muestra el error del backend (stock insuficiente, etc.). */}
        <AlertaError mensaje={error} />
        {/* Botón pagar; disabled mientras se procesa. */}
        <button type="submit" className="btn btn-success" disabled={enviando}>
          {enviando ? (
            <span className="spinner-border spinner-border-sm" aria-hidden="true" />
          ) : (
            <Icono nombre="lock" />
          )}
          Pagar ahora {precioCLP(total)}
        </button>
      </form>
    </section>
  );
}
