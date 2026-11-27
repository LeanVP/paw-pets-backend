import Icono from './Icono';

/**
 * Campo de formulario con etiqueta, ayuda opcional y mensaje de error (clases Bootstrap).
 * tipo: 'text' | 'email' | 'password' | 'number' | 'textarea' | 'select'
 * opciones (solo select): [{ valor, texto }]
 * El error se muestra con borde rojo + ícono + texto, y queda asociado al campo (aria-describedby).
 */
// COMPONENTE REUTILIZABLE: etiqueta + control + ayuda + error. Lo usan todos los formularios.
export default function CampoFormulario({
  etiqueta,
  nombre,
  // Tipo de control: text, email, password, number, textarea o select.
  tipo = 'text',
  valor,
  error,
  ayuda,
  onChange,
  opciones,
  // Cualquier otra prop (autoComplete, etc.) se pasa directo al control.
  ...resto
}) {
  // Clases de Bootstrap: form-select o form-control, más is-invalid (borde rojo) si hay error.
  const clase = `${tipo === 'select' ? 'form-select' : 'form-control'} ${error ? 'is-invalid' : ''}`;
  // ids para asociar el mensaje de error y la ayuda con el campo (accesibilidad para lectores de pantalla).
  const idError = `${nombre}-error`;
  const idAyuda = `${nombre}-ayuda`;
  // aria-describedby: lista de ids que describen el campo.
  const describe = [error && idError, ayuda && idAyuda].filter(Boolean).join(' ') || undefined;
  // Propiedades compartidas por input, select y textarea.
  const comunes = {
    id: nombre,
    name: nombre,
    // Campo CONTROLADO: su valor viene de React. ?? '' evita pasar undefined.
    value: valor ?? '',
    // Cada tecla llama a cambiar() de useFormulario.
    onChange,
    className: clase,
    // Atributos de accesibilidad.
    'aria-invalid': error ? true : undefined,
    'aria-describedby': describe,
    // Agrega las props extra recibidas.
    ...resto,
  };

  // Aquí se guarda el elemento HTML según el tipo.
  let control;
  // Lista desplegable.
  if (tipo === 'select') {
    control = (
      <select {...comunes}>
        {/* Opción vacía inicial (la regla "requerido" la rechaza). */}
        <option value="">Selecciona una opción</option>
        {opciones.map((op) => (
          <option key={op.valor} value={op.valor}>
            {op.texto}
          </option>
        ))}
      </select>
    );
  // Texto multilínea.
  } else if (tipo === 'textarea') {
    control = <textarea rows={3} {...comunes} />;
  } else {
    // Input normal (text, email, password, number).
    control = <input type={tipo} {...comunes} />;
  }

  // Dibuja etiqueta, control y mensajes.
  return (
    <div className="mb-3">
      {/* htmlFor enlaza la etiqueta con el input (clic en la etiqueta enfoca el campo). */}
      <label htmlFor={nombre} className="form-label fw-semibold">
        {etiqueta}
      </label>
      {/* Se inserta el control elegido arriba. */}
      {control}
      {/* Texto de ayuda: solo si no hay error. */}
      {ayuda && !error && (
        <div id={idAyuda} className="form-text">
          {ayuda}
        </div>
      )}
      {/* Mensaje de error con ícono (nunca solo con color). */}
      {error && (
        <div id={idError} className="invalid-feedback campo-error">
          <Icono nombre="error" />
          <span>{error}</span>
        </div>
      )}
    </div>
  );
}
