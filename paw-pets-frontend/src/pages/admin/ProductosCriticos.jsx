import { Link } from 'react-router-dom';
import { EstadoCarga, EstadoVacio, EtiquetaEstado } from '../../components/Estados';
import Icono from '../../components/Icono';
import useColeccion from '../../hooks/useColeccion';
// Regla de stock bajo (<= 5) definida en utils/productos.js.
import { esCritico, UMBRAL_CRITICO } from '../../utils/productos';

/** Productos con poco stock (admin y vendedor). */
// PÁGINA /admin/productos/criticos (admin y vendedor).
export default function ProductosCriticos() {
  return (
    <>
      <Link to="/admin/productos" className="enlace-volver">
        <Icono nombre="arrow_back" />
        Volver a productos
      </Link>
      <h1 className="h3 mt-2">Productos críticos</h1>
      <p className="text-secondary">
        Productos con stock igual o menor a {UMBRAL_CRITICO} unidades.
      </p>
      {/* GET /api/v1/productos. */}
      <EstadoCarga colecciones={['productos']}>
        <Lista />
      </EstadoCarga>
    </>
  );
}

// Lista filtrada.
function Lista() {
  // filter(esCritico): conserva solo los productos con stock crítico.
  const criticos = useColeccion('productos').filter(esCritico);
  // Todo en orden.
  if (criticos.length === 0) {
    return <EstadoVacio icono="task_alt">No hay productos con stock crítico.</EstadoVacio>;
  }
  return (
    <ul className="list-group lista-paw">
      {/* Una línea por producto crítico. */}
      {criticos.map((p) => (
        <li
          className="list-group-item d-flex justify-content-between align-items-center gap-2"
          key={p.id}
        >
          {p.nombre}
          <EtiquetaEstado tipo="alerta">Stock: {p.stock}</EtiquetaEstado>
        </li>
      ))}
    </ul>
  );
}
