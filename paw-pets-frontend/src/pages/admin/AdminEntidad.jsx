// useState: guarda el error y qué fila se está eliminando.
import { useState } from 'react';
// useParams: lee :entidad de la URL. Navigate: redirige.
import { Link, Navigate, useParams } from 'react-router-dom';
// Componentes de error, carga desde la API y lista vacía.
import { AlertaError, EstadoCarga, EstadoVacio } from '../../components/Estados';
import Icono from '../../components/Icono';
// Almacén de datos: db.eliminar = DELETE al backend.
import db from '../../data/db';
import useAuth from '../../hooks/useAuth';
// Hook que entrega la lista de la caché y redibuja al cambiar.
import useColeccion from '../../hooks/useColeccion';
// Tabla de configuración de cada entidad (columnas, roles, etc.).
import { ENTIDADES } from './entidades';

/**
 * Listado de una entidad (productos, categorías o usuarios).
 * Admin: Nuevo, Editar y Eliminar. Vendedor: solo "Ver detalle" (lectura).
 */
// PÁGINA GENÉRICA /admin/:entidad. Sirve para productos, categorías y usuarios según lo que diga la URL.
export default function AdminEntidad() {
  // Si la URL es /admin/productos, entidad = "productos".
  const { entidad } = useParams();
  // Rol del usuario conectado.
  const { usuario } = useAuth();
  // Busca la configuración de esa entidad.
  const config = ENTIDADES[entidad];
  // Entidad inexistente (ej: /admin/xyz).
  if (!config) return <p className="alert alert-warning">Sección no encontrada.</p>;
  // Una entidad que el rol no puede ver: vuelve al inicio del panel
  // Si el rol no puede ver esa sección, vuelve al inicio del panel.
  if (!config.roles.includes(usuario.rol)) return <Navigate to="/admin" replace />;
  // Pide la colección al backend y muestra el listado cuando llega.
  return (
    <EstadoCarga colecciones={[entidad]}>
      {/* esAdmin = true solo para admin: habilita crear, editar y eliminar. */}
      <Listado entidad={entidad} config={config} esAdmin={usuario.rol === 'admin'} />
    </EstadoCarga>
  );
}

// Tabla con los registros.
function Listado({ entidad, config, esAdmin }) {
  // Datos de la caché (GET /api/v1/<entidad>). Se redibuja sola cuando db cambia.
  const filas = useColeccion(entidad);
  // Mensaje de error del backend al eliminar.
  const [error, setError] = useState(null);
  // Id de la fila en proceso de eliminación (para desactivar su botón).
  const [eliminando, setEliminando] = useState(null);

  // DELETE en el backend; si lo rechaza (por ejemplo, categoría con productos) muestra el motivo
  // Elimina un registro.
  async function eliminar(id) {
    setError(null);
    setEliminando(id);
    try {
      // DELETE /api/v1/<entidad>/<id>. Si el backend lo rechaza (ej: 409 categoría con productos), lanza error y la caché no cambia.
      await db.eliminar(entidad, id);
    } catch (e) {
      // Muestra el motivo que envió el backend.
      setError(e.message);
    // finally se ejecuta siempre, haya error o no.
    } finally {
      // Reactiva el botón.
      setEliminando(null);
    }
  }

  // Se usa <> ... </> (Fragmento): agrupa elementos sin agregar un div extra.
  return (
    <>
      {/* Encabezado: título, cantidad de registros y botones. */}
      <div className="encabezado-pagina">
        <div>
          <h1 className="h3 mb-1">{config.titulo}</h1>
          <p className="text-secondary mb-0">
            {filas.length} {filas.length === 1 ? 'registro' : 'registros'}
            {!esAdmin && ' · modo lectura'}
          </p>
        </div>
        <div className="d-flex flex-wrap gap-2">
          {/* Enlaces extra de la entidad (ej: Productos críticos). ?. evita error si no existen. */}
          {config.enlaces?.map(([ruta, texto, icono]) => (
            <Link key={ruta} to={ruta} className="btn btn-outline-dark">
              <Icono nombre={icono} />
              {texto}
            </Link>
          ))}
          {/* Solo el admin ve el botón Nuevo. */}
          {esAdmin && (
            <Link to={`/admin/${entidad}/nuevo`} className="btn btn-primary">
              <Icono nombre="add" />
              {config.nuevo} {config.singular}
            </Link>
          )}
        </div>
      </div>
      {/* Muestra el error del backend (no dibuja nada si es null). */}
      <AlertaError mensaje={error} />
      {/* Sin datos → estado vacío; con datos → tabla. */}
      {filas.length === 0 ? (
        <EstadoVacio icono={config.icono}>Aún no hay {config.titulo.toLowerCase()}.</EstadoVacio>
      ) : (
        // Contenedor de la tabla (clases de Bootstrap).
        <div className="card tabla-card">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead>
                <tr>
                  {/* Encabezados generados desde la configuración. */}
                  {config.columnas.map(([campo, etiqueta]) => (
                    <th scope="col" key={campo}>
                      {etiqueta}
                    </th>
                  ))}
                  <th scope="col" className="text-end">
                    Acciones
                  </th>
                </tr>
              </thead>
              <tbody>
                {/* Una fila por registro. key={fila.id} le da a React una identidad estable. */}
                {filas.map((fila) => (
                  <tr key={fila.id}>
                    {/* Una celda por columna; formato (si existe) transforma el valor (ej: precio en CLP). String es el formato por defecto. */}
                    {config.columnas.map(([campo, , formato = String]) => (
                      <td key={campo}>{formato(fila[campo], fila)}</td>
                    ))}
                    <td>
                      <div className="d-flex flex-wrap gap-1 justify-content-end">
                        <Link
                          // Editar (admin) o Ver detalle (vendedor): ruta /admin/productos/3.
                          to={`/admin/${entidad}/${fila.id}`}
                          className="btn btn-sm btn-outline-primary"
                        >
                          <Icono nombre={esAdmin ? 'edit' : 'visibility'} />
                          {esAdmin ? 'Editar' : 'Ver detalle'}
                        </Link>
                        {/* Acciones extra de la fila (ej: Historial de un usuario). */}
                        {esAdmin && config.accionesFila?.(fila)}
                        {/* Botón Eliminar solo para admin. */}
                        {esAdmin && (
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-danger"
                            // Se desactiva mientras esa fila se elimina.
                            disabled={eliminando === fila.id}
                            // Al hacer clic ejecuta eliminar con el id de la fila.
                            onClick={() => eliminar(fila.id)}
                          >
                            <Icono nombre="delete" />
                            Eliminar
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </>
  );
}
