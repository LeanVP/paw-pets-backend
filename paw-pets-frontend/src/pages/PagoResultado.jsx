import { Link, useParams } from 'react-router-dom';
import { EstadoCarga } from '../components/Estados';
import Icono from '../components/Icono';
import ResumenOrden from '../components/ResumenOrden';
import useColeccion from '../hooks/useColeccion';

/** Vista de compra exitosa (exito = true) o de pago con error (exito = false). */
// PÁGINA para /compra/exitosa/:id y /compra/error/:id. La prop "exito" (definida en App.jsx) cambia el título y los botones.
export default function PagoResultado({ exito }) {
  return (
    // GET /api/v1/boletas: el backend entrega al cliente solo sus propias boletas.
    <EstadoCarga colecciones={['boletas']}>
      <Resultado exito={exito} />
    </EstadoCarga>
  );
}

function Resultado({ exito }) {
  // Número de boleta tomado de la URL.
  const { id } = useParams();
  // GET /boletas: el cliente solo recibe sus propias boletas
  // Busca esa boleta entre las del usuario.
  const orden = useColeccion('boletas').find((b) => b.id === Number(id));

  // Si no existe (o es de otro cliente) no se muestra.
  if (!orden) return <p className="alert alert-warning">Orden no encontrada.</p>;
  return (
    <section className="col-lg-8 mx-auto card card-body">
      {/* Clase CSS distinta según éxito o error. */}
      <div className={`resultado-pago ${exito ? 'resultado-ok' : 'resultado-error'}`}>
        {/* Ícono verde o rojo (además del texto: no se comunica solo con color). */}
        <Icono nombre={exito ? 'check_circle' : 'cancel'} relleno />
        <h1 className="h3 mb-0">
          {exito
            ? `Se ha realizado la compra. Nro #${orden.id}`
            : `No se pudo realizar el pago. Nro #${orden.id}`}
        </h1>
      </div>
      {/* Detalle de la boleta. */}
      <ResumenOrden orden={orden} />
      <div className="d-flex flex-wrap gap-2 justify-content-center">
        {exito ? (
          // Abre el diálogo de impresión del navegador.
          <button type="button" className="btn btn-danger" onClick={() => window.print()}>
            <Icono nombre="print" />
            Imprimir boleta
          </button>
        ) : (
          // Si el pago fue rechazado, vuelve al checkout (el carrito sigue guardado).
          <Link to="/checkout" className="btn btn-success">
            <Icono nombre="replay" />
            Volver a realizar el pago
          </Link>
        )}
        <Link to="/" className="btn btn-outline-dark">
          <Icono nombre="home" />
          Volver al inicio
        </Link>
      </div>
    </section>
  );
}
