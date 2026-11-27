/**
 * Estados de una vista que consume la API: cargando, error, vacío, y la etiqueta de estado.
 * Cada estado se comunica con ícono + texto + color (nunca solo con color).
 */
// Hook que pide colecciones a la API y entrega { cargando, error, reintentar }.
import useCarga from '../hooks/useCarga';
import Icono from './Icono';

/** Indicador de carga accesible (role="status" lo anuncia el lector de pantalla). */
// Indicador de carga: spinner + texto.
export function Cargando({ texto = 'Cargando…' }) {
  return (
    // role="status": los lectores de pantalla anuncian el cambio.
    <div className="estado-bloque" role="status">
      <span className="spinner-border spinner-border-sm text-primary" aria-hidden="true" />
      <span>{texto}</span>
    </div>
  );
}

/** Mensaje de error con su ícono. Si el backend envió errores por campo, los lista. */
// Cuadro rojo de error. Recibe el mensaje (de ErrorApi), errores por campo opcionales y un botón Reintentar opcional.
export function AlertaError({ mensaje, campos, onReintentar }) {
  // Sin mensaje no se dibuja nada: por eso los formularios pueden incluir <AlertaError mensaje={error} /> siempre.
  if (!mensaje) return null;
  // Convierte { campo: mensaje } en una lista de textos.
  const lista = Object.values(campos ?? {});
  return (
    <div className="alert alert-danger alerta-paw" role="alert">
      <Icono nombre="error" />
      <div className="flex-grow-1">
        <p className="mb-0">{mensaje}</p>
        {/* Solo si hay errores por campo (400 de validación del backend). */}
        {lista.length > 0 && (
          <ul className="mb-0 mt-1 ps-3">
            {lista.map((texto) => (
              <li key={texto}>{texto}</li>
            ))}
          </ul>
        )}
      </div>
      {/* Botón Reintentar solo si el padre entregó la función. */}
      {onReintentar && (
        <button type="button" className="btn btn-sm btn-outline-danger" onClick={onReintentar}>
          <Icono nombre="refresh" /> Reintentar
        </button>
      )}
    </div>
  );
}

/** Mensaje de confirmación (por ejemplo, "Perfil actualizado"). */
// Cuadro verde de confirmación.
export function AlertaExito({ children }) {
  return (
    <p className="alert alert-success alerta-paw" role="status">
      <Icono nombre="check_circle" />
      <span>{children}</span>
    </p>
  );
}

/** Estado vacío: ícono grande, mensaje y una acción opcional. */
// Pantalla "no hay datos": ícono, mensaje y acción opcional.
export function EstadoVacio({ icono = 'inventory_2', children, accion }) {
  return (
    <div className="estado-vacio">
      <Icono nombre={icono} className="estado-vacio-icono" />
      <p className="mb-0">{children}</p>
      {accion}
    </div>
  );
}

/**
 * Muestra "Cargando…" o el error (con Reintentar) mientras las colecciones llegan del backend;
 * cuando están listas, dibuja el contenido.
 */
// COMPONENTE ENVOLTORIO clave: pide las colecciones al backend y decide qué mostrar: error, cargando o el contenido (children).
export function EstadoCarga({ colecciones, children }) {
  // Dispara las peticiones GET y se mantiene actualizado.
  const { cargando, error, reintentar } = useCarga(...colecciones);
  // Error del backend (ej: servidor apagado) con botón Reintentar.
  if (error) return <AlertaError mensaje={error} onReintentar={reintentar} />;
  // Mientras llegan los datos.
  if (cargando) return <Cargando />;
  // Datos listos: dibuja el contenido que se envolvió.
  return children;
}

// Tabla tipo de etiqueta → ícono (así el estado se comunica con ícono + texto + color).
const TIPOS = {
  ok: 'check_circle',
  error: 'cancel',
  alerta: 'warning',
  info: 'info',
  neutro: 'label',
};

/** Etiqueta de estado (píldora): tipo = ok | error | alerta | info | neutro. */
// Píldora de estado reutilizable.
export function EtiquetaEstado({ tipo = 'neutro', icono, children }) {
  return (
    <span className={`etiqueta etiqueta-${tipo}`}>
      <Icono nombre={icono ?? TIPOS[tipo]} />
      {children}
    </span>
  );
}

/** Etiqueta para el estado de una boleta: Pagada (verde) o Rechazada (rojo). */
// Etiqueta específica de boleta: Pagada = verde, Rechazada = roja.
export function EstadoBoleta({ estado }) {
  return <EtiquetaEstado tipo={estado === 'Pagada' ? 'ok' : 'error'}>{estado}</EtiquetaEstado>;
}
