/**
 * Configuración de las entidades administrables. Cada una define:
 * - roles: quién la ve en el panel (el vendedor solo ve productos, en modo lectura);
 * - columnas de la tabla, campos del formulario y reglas de validación.
 * Las vistas AdminEntidad y AdminFormulario son genéricas y usan esta configuración.
 */
// Link para la acción "Historial" de cada usuario.
import { Link } from 'react-router-dom';
// Píldora de color para estados (stock crítico, rol).
import { EtiquetaEstado } from '../../components/Estados';
import Icono from '../../components/Icono';
import { precioCLP } from '../../utils/formato';
// Regla de negocio: stock <= 5.
import { esCritico } from '../../utils/productos';
// Lista de roles válidos.
import { ROLES } from '../../utils/roles';
import {
  correo,
  minimo,
  minimoSiHay,
  numeroNoNegativo,
  requerido,
} from '../../utils/validaciones';

// CONFIGURACIÓN DIRIGIDA POR DATOS: cada entrada describe una entidad administrable. Con esta tabla, DOS pantallas genéricas
// (AdminEntidad = listado, AdminFormulario = crear/editar) sirven para productos, categorías y usuarios sin repetir código.
export const ENTIDADES = {
  // La clave 'productos' coincide con el parámetro :entidad de la URL (/admin/productos) y con la colección de db.js.
  productos: {
    // Textos que se muestran en títulos y botones.
    titulo: 'Productos',
    singular: 'producto',
    // Para los textos: "Nuevo producto", "Detalle del producto"
    nuevo: 'Nuevo',
    del: 'del',
    // Nombre del ícono Material Symbols.
    icono: 'inventory_2',
    // Quién puede ver esta sección (el vendedor solo en modo lectura).
    roles: ['admin', 'vendedor'],
    // Columnas de la tabla: [campo, etiqueta, función de formato opcional].
    columnas: [
      ['id', 'ID'],
      ['nombre', 'Nombre'],
      // Tercer elemento = formateador: aquí muestra $18.990.
      ['precio', 'Precio', precioCLP],
      // El stock crítico se marca con color + ícono + texto
      [
        // Columna con formateador propio: marca en alerta el stock crítico.
        'stock',
        'Stock',
        (stock, fila) =>
          esCritico(fila) ? (
            <EtiquetaEstado tipo="alerta">{stock} · crítico</EtiquetaEstado>
          ) : (
            String(stock)
          ),
      ],
    ],
    // Valores por defecto al CREAR un producto nuevo.
    inicial: { precioOferta: 0, destacado: false },
    // Campos del formulario. Es una función porque el select de categorías depende de los datos cargados.
    campos: (categorias) => [
      // Cada campo: nombre (clave del JSON), etiqueta, tipo de input, opciones (selects) y numerico (se convierte con Number antes de enviar).
      { nombre: 'nombre', etiqueta: 'Nombre' },
      {
        nombre: 'categoriaId',
        etiqueta: 'Categoría',
        tipo: 'select',
        // El input entrega texto; el backend espera número, así que FormularioEntidad lo convierte.
        numerico: true,
        // Opciones del select: valor = id de categoría, texto = su nombre.
        opciones: categorias.map((c) => ({ valor: c.id, texto: c.nombre })),
      },
      { nombre: 'precio', etiqueta: 'Precio', tipo: 'number', numerico: true },
      {
        nombre: 'precioOferta',
        etiqueta: 'Precio de oferta (0 = sin oferta)',
        tipo: 'number',
        numerico: true,
      },
      { nombre: 'stock', etiqueta: 'Stock', tipo: 'number', numerico: true },
      { nombre: 'imagen', etiqueta: 'URL de la imagen (opcional)' },
      { nombre: 'descripcion', etiqueta: 'Descripción', tipo: 'textarea' },
    ],
    // Reglas de validación del formulario (las mismas @NotBlank/@Min del backend, aplicadas antes de enviar).
    reglas: () => ({
      nombre: [requerido],
      categoriaId: [requerido],
      precio: [numeroNoNegativo],
      precioOferta: [numeroNoNegativo],
      stock: [numeroNoNegativo],
      descripcion: [requerido],
    }),
    // Botones extra en el listado: ruta, texto e ícono.
    enlaces: [['/admin/productos/criticos', 'Productos críticos', 'warning']],
  },
  // Segunda entidad: solo la ve el admin.
  categorias: {
    titulo: 'Categorías',
    singular: 'categoría',
    nuevo: 'Nueva',
    del: 'de la',
    icono: 'category',
    roles: ['admin'],
    columnas: [
      ['id', 'ID'],
      ['nombre', 'Nombre'],
    ],
    inicial: {},
    campos: () => [
      { nombre: 'nombre', etiqueta: 'Nombre' },
      { nombre: 'imagen', etiqueta: 'URL de la imagen' },
    ],
    reglas: () => ({ nombre: [requerido], imagen: [requerido] }),
  },
  // Tercera entidad: gestión de cuentas (solo admin).
  usuarios: {
    titulo: 'Usuarios',
    singular: 'usuario',
    nuevo: 'Nuevo',
    del: 'del',
    icono: 'group',
    roles: ['admin'],
    columnas: [
      ['id', 'ID'],
      ['nombre', 'Nombre'],
      ['correo', 'Correo'],
      ['rol', 'Rol', (rol) => <EtiquetaEstado icono="badge">{rol}</EtiquetaEstado>],
    ],
    // Al crear un usuario, el rol por defecto es cliente.
    inicial: { rol: 'cliente' },
    // editando permite cambiar las reglas: al editar, la contraseña es opcional.
    // editando = true: la contraseña es opcional (vacía = mantener la actual)
    campos: (_categorias, editando) => [
      { nombre: 'nombre', etiqueta: 'Nombre' },
      { nombre: 'apellidos', etiqueta: 'Apellidos' },
      { nombre: 'correo', etiqueta: 'Correo electrónico', tipo: 'email' },
      {
        nombre: 'clave',
        etiqueta: 'Contraseña',
        tipo: 'password',
        ayuda: editando
          ? 'Déjala vacía para mantener la contraseña actual.'
          : 'Mínimo 6 caracteres.',
      },
      {
        nombre: 'rol',
        etiqueta: 'Rol',
        tipo: 'select',
        opciones: ROLES.map((r) => ({ valor: r, texto: r })),
      },
    ],
    reglas: (editando) => ({
      nombre: [requerido],
      apellidos: [requerido],
      correo: [requerido, correo],
      // Al crear: obligatoria y mínimo 6. Al editar: opcional (vacía = mantener la actual, igual que UsuarioService).
      clave: editando ? [minimoSiHay(6)] : [requerido, minimo(6)],
      rol: [requerido],
    }),
    // Botón extra por fila: enlaza al historial de compras de ese usuario.
    accionesFila: (fila) => (
      <Link
        to={`/admin/usuarios/${fila.id}/historial`}
        className="btn btn-sm btn-outline-secondary"
      >
        <Icono nombre="history" />
        Historial
      </Link>
    ),
  },
};
