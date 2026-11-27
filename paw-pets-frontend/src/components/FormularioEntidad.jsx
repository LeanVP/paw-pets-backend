// Hook de formularios: valores, errores y envío.
import useFormulario from '../hooks/useFormulario';
import CampoFormulario from './CampoFormulario';
import Icono from './Icono';

/**
 * Formulario genérico dirigido por configuración (lista de campos y reglas).
 * - Los campos marcados como numerico se convierten a número antes de guardar.
 * - soloLectura: muestra los datos sin poder editarlos (rol vendedor).
 * - enviando: desactiva el botón mientras el backend responde.
 * - erroresServidor: errores por campo que devolvió el backend (400).
 */
// COMPONENTE GENÉRICO: dibuja un formulario a partir de una lista de campos (props), sin saber si es de productos, categorías o usuarios.
export default function FormularioEntidad({
  campos,
  reglas,
  valoresIniciales,
  onGuardar,
  // Valores por defecto de las props opcionales.
  textoBoton = 'Guardar',
  soloLectura = false,
  enviando = false,
  erroresServidor = {},
}) {
  // Conecta el formulario con las reglas de validación.
  const { valores, errores, cambiar, enviar } = useFormulario(
    valoresIniciales,
    reglas,
    // Solo se ejecuta si las reglas pasan.
    (datos) => {
      // Copia de los datos para convertirlos sin mutar el original.
      const convertidos = { ...datos };
      campos
        // Selecciona los campos numéricos.
        .filter((c) => c.numerico)
        // Los inputs entregan texto ("1500"); el backend espera números: se convierte con Number.
        .forEach((c) => (convertidos[c.nombre] = Number(datos[c.nombre])));
      // Entrega los datos listos a quien lo use (AdminFormulario → db.crear/actualizar).
      onGuardar(convertidos);
    },
  );

  // Dibuja el formulario.
  return (
    // onSubmit dispara la validación; noValidate usa la validación propia.
    <form onSubmit={enviar} noValidate>
      {/* fieldset disabled desactiva todos los campos de una vez */}
      {/* Deshabilita todos los campos de una vez (modo lectura o mientras envía). */}
      <fieldset disabled={soloLectura || enviando}>
        {/* Un CampoFormulario por cada campo de la configuración. */}
        {campos.map(({ nombre, etiqueta, tipo, opciones, ayuda }) => (
          <CampoFormulario
            key={nombre}
            nombre={nombre}
            etiqueta={etiqueta}
            tipo={tipo}
            opciones={opciones}
            ayuda={ayuda}
            valor={valores[nombre]}
            // Muestra el error del frontend o, si no hay, el que devolvió el backend.
            error={errores[nombre] || erroresServidor[nombre]}
            onChange={cambiar}
          />
        ))}
      </fieldset>
      {/* En modo lectura no hay botón Guardar. */}
      {!soloLectura && (
        <button type="submit" className="btn btn-primary" disabled={enviando}>
          {enviando ? (
            <span className="spinner-border spinner-border-sm" aria-hidden="true" />
          ) : (
            <Icono nombre="save" />
          )}
          {textoBoton}
        </button>
      )}
    </form>
  );
}
