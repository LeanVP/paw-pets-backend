import { useState } from 'react';
// useNavigate: volver al listado tras guardar. useParams: lee :entidad y :id.
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { AlertaError, EstadoCarga } from '../../components/Estados';
// Formulario genérico dirigido por configuración.
import FormularioEntidad from '../../components/FormularioEntidad';
import Icono from '../../components/Icono';
// db.crear = POST; db.actualizar = PUT.
import db from '../../data/db';
import useAuth from '../../hooks/useAuth';
import useColeccion from '../../hooks/useColeccion';
import { ENTIDADES } from './entidades';

/**
 * Crea (/nuevo) o edita (/:id) un registro de la entidad indicada en la URL.
 * El vendedor ve el mismo formulario en modo lectura ("Ver detalle").
 */
// PÁGINA GENÉRICA para crear (/admin/productos/nuevo) y editar (/admin/productos/3).
export default function AdminFormulario() {
  const { entidad } = useParams();
  const { usuario } = useAuth();
  // Configuración de la entidad.
  const config = ENTIDADES[entidad];
  if (!config) return <p className="alert alert-warning">Sección no encontrada.</p>;
  // Control de acceso en la interfaz (el backend igual valida con 403).
  if (!config.roles.includes(usuario.rol)) return <Navigate to="/admin" replace />;
  return (
    // Carga la entidad y las categorías (necesarias para el select de categoría de productos).
    <EstadoCarga colecciones={[entidad, 'categorias']}>
      {/* El vendedor ve el formulario sin poder editar. */}
      <Formulario entidad={entidad} config={config} soloLectura={usuario.rol !== 'admin'} />
    </EstadoCarga>
  );
}

// Formulario real.
function Formulario({ entidad, config, soloLectura }) {
  // Si la URL es /admin/productos/nuevo, id es undefined; si es /admin/productos/3, id = "3".
  const { id } = useParams();
  const navegar = useNavigate();
  // Categorías para las opciones del select.
  const categorias = useColeccion('categorias');
  // Registro que se está editando (undefined al crear).
  const existente = useColeccion(entidad).find((item) => item.id === Number(id));
  // Bloquea el botón mientras responde el backend.
  const [enviando, setEnviando] = useState(false);
  // Error general del backend.
  const [error, setError] = useState(null);
  // Errores POR CAMPO que devuelve el backend (400): { campo: mensaje }.
  const [erroresCampos, setErroresCampos] = useState({});

  // Hay id en la URL pero ese registro no existe.
  if (id && !existente) return <p className="alert alert-warning">Registro no encontrado.</p>;

  // true = edición; false = creación.
  const editando = Boolean(existente);
  // Lista de campos del formulario según la entidad.
  const campos = config.campos(categorias, editando);
  // Construye { campo: valor }: usa el valor existente, si no el inicial de la config, si no ''.
  const valoresIniciales = Object.fromEntries(
    campos.map((c) => [c.nombre, existente?.[c.nombre] ?? config.inicial[c.nombre] ?? '']),
  );

  // Se ejecuta cuando el formulario es válido.
  async function guardar(datos) {
    setEnviando(true);
    setError(null);
    // Limpia errores previos.
    setErroresCampos({});
    try {
      // Se mezcla el registro existente con los cambios para no perder campos que este formulario no muestra.
      // Al editar se envía el registro completo con los cambios encima
      // (así no se pierden campos que este formulario no muestra, como la dirección del usuario).
      // PUT /api/v1/<entidad>/<id>.
      if (editando) await db.actualizar(entidad, existente.id, { ...existente, ...datos });
      // POST /api/v1/<entidad>.
      else await db.crear(entidad, { ...config.inicial, ...datos });
      // Éxito: vuelve al listado, que ya muestra el cambio porque db actualizó la caché.
      navegar(`/admin/${entidad}`);
    // Error del backend.
    } catch (e) {
      setError(e.message);
      // Errores por campo para pintarlos bajo cada input.
      setErroresCampos(e.campos ?? {});
      setEnviando(false);
    }
  }

  // Título según el modo: crear, editar o solo lectura.
  let titulo = `${config.nuevo} ${config.singular}`;
  if (soloLectura) titulo = `Detalle ${config.del} ${config.singular}`;
  else if (editando) titulo = `Editar ${config.singular}`;

  // Dibuja la pantalla.
  return (
    <div className="col-lg-8 col-xl-6">
      {/* Enlace para volver al listado. */}
      <Link to={`/admin/${entidad}`} className="enlace-volver">
        <Icono nombre="arrow_back" />
        Volver a {config.titulo.toLowerCase()}
      </Link>
      <h1 className="h3 mt-2">{titulo}</h1>
      <AlertaError mensaje={error} />
      <div className="card card-body">
        {/* key={id ?? 'nuevo'} reinicia el formulario al cambiar de registro; el resto son props que lo configuran. */}
        <FormularioEntidad
          key={id ?? 'nuevo'}
          campos={campos}
          reglas={config.reglas(editando)}
          valoresIniciales={valoresIniciales}
          onGuardar={guardar}
          soloLectura={soloLectura}
          enviando={enviando}
          erroresServidor={erroresCampos}
        />
      </div>
    </div>
  );
}
