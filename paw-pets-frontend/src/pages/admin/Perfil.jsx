import { useState } from 'react';
import { AlertaError, AlertaExito } from '../../components/Estados';
import FormularioEntidad from '../../components/FormularioEntidad';
import db from '../../data/db';
import useAuth from '../../hooks/useAuth';
import { ENTIDADES } from './entidades';

// Mismo formulario que usuarios, sin el rol: el administrador no puede quitarse permisos a sí mismo
// Reutiliza la configuración de "usuarios" pero sin el campo rol.
const camposPerfil = ENTIDADES.usuarios.campos([], true).filter((c) => c.nombre !== 'rol');
// Desestructuración: separa la regla de rol y deja el resto (así el admin no puede cambiarse el rol).
const { rol: _sinRol, ...reglasPerfil } = ENTIDADES.usuarios.reglas(true);

/** Edita los datos de la cuenta con sesión iniciada (PUT /usuarios/{id}). */
// PÁGINA /admin/perfil (solo admin): edita su propia cuenta.
export default function Perfil() {
  // Usuario de la sesión y función para actualizar sus datos en la sesión.
  const { usuario, actualizarUsuario } = useAuth();
  // Un solo estado agrupa enviando, guardado y error.
  const [estado, setEstado] = useState({ enviando: false, guardado: false, error: null });
  // Valores iniciales del formulario: los datos del usuario, con la clave vacía (vacía = no cambiarla).
  const iniciales = Object.fromEntries(
    camposPerfil.map((c) => [c.nombre, c.nombre === 'clave' ? '' : usuario[c.nombre]]),
  );

  // Se ejecuta si el formulario es válido.
  async function guardar(datos) {
    setEstado({ enviando: true, guardado: false, error: null });
    try {
      // PUT /api/v1/usuarios/<id>. Se envía { ...usuario, ...datos } para no perder el rol ni la dirección.
      const actualizado = await db.actualizar('usuarios', usuario.id, { ...usuario, ...datos });
      // Actualiza el usuario guardado en la sesión (Header y panel muestran el nombre nuevo).
      actualizarUsuario(actualizado);
      setEstado({ enviando: false, guardado: true, error: null });
    } catch (e) {
      setEstado({ enviando: false, guardado: false, error: e.message });
    }
  }

  return (
    <div className="col-lg-8 col-xl-6">
      <h1 className="h3">Perfil</h1>
      <p className="text-secondary">Tus datos de acceso al panel.</p>
      <AlertaError mensaje={estado.error} />
      <div className="card card-body">
        <FormularioEntidad
          campos={camposPerfil}
          reglas={reglasPerfil}
          valoresIniciales={iniciales}
          onGuardar={guardar}
          enviando={estado.enviando}
          textoBoton="Guardar cambios"
        />
      </div>
      {/* Mensaje de confirmación. */}
      {estado.guardado && <AlertaExito>Perfil actualizado.</AlertaExito>}
    </div>
  );
}
