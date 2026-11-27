// NavLink: Link que marca la ruta activa. Outlet: hueco donde se dibuja la página hija. useLocation: ruta actual.
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import { EtiquetaEstado } from '../components/Estados';
import Icono from '../components/Icono';
import Logo from '../components/Logo';
import useAuth from '../hooks/useAuth';

// [ruta, etiqueta, ícono Material Symbols, roles que la ven]
// Menú del panel: [ruta, etiqueta, ícono, roles que la ven].
const SECCIONES = [
  ['/admin', 'Dashboard', 'dashboard', ['admin']],
  ['/admin/ordenes', 'Órdenes', 'receipt_long', ['admin', 'vendedor']],
  ['/admin/productos', 'Productos', 'inventory_2', ['admin', 'vendedor']],
  ['/admin/categorias', 'Categorías', 'category', ['admin']],
  ['/admin/usuarios', 'Usuarios', 'group', ['admin']],
  ['/admin/reportes', 'Reportes', 'bar_chart', ['admin']],
  ['/admin/perfil', 'Perfil', 'person', ['admin']],
];

/**
 * Panel de administración: menú lateral (apilado en móvil), barra superior y contenido.
 * El menú se filtra por rol: el vendedor solo ve Órdenes y Productos (Anexo 1).
 */
// LAYOUT del panel: menú lateral + barra superior + contenido. Se aplica a todas las rutas hijas de /admin (ver App.jsx).
export default function AdminLayout() {
  // Usuario conectado y función para salir.
  const { usuario, cerrarSesion } = useAuth();
  // Ruta actual (ej: "/admin/productos").
  const { pathname } = useLocation();
  // Filtra el menú por rol: el vendedor solo ve Órdenes y Productos.
  const secciones = SECCIONES.filter(([, , , roles]) => roles.includes(usuario.rol));
  // Sección actual: la ruta más larga que coincide con la URL (para la barra superior)
  // Calcula la sección actual para el título de la barra superior.
  const actual = [...SECCIONES]
    // Ordena de la ruta más larga a la más corta para que "/admin/productos" gane sobre "/admin".
    .sort((a, b) => b[0].length - a[0].length)
    // Primera ruta que coincide con la URL.
    .find(([ruta]) => pathname === ruta || pathname.startsWith(`${ruta}/`));

  return (
    <div className="container-fluid">
      <div className="row">
        {/* Menú lateral (grilla de Bootstrap: 3 de 12 columnas). */}
        <aside className="col-md-3 col-lg-2 admin-menu py-3">
          <Link to={secciones[0][0]} className="d-block mb-3 text-decoration-none">
            <Logo />
          </Link>
          <nav className="nav nav-pills flex-md-column gap-1" aria-label="Administración">
            {/* Un enlace por cada sección permitida. */}
            {secciones.map(([ruta, etiqueta, icono]) => (
              // end: "/admin" solo se marca activo en la ruta exacta, no en todas las subrutas.
              <NavLink key={ruta} to={ruta} end={ruta === '/admin'} className="nav-link">
                <Icono nombre={icono} />
                {etiqueta}
              </NavLink>
            ))}
          </nav>
          <div className="admin-menu-pie">
            <Link to="/" className="btn btn-dark">
              <Icono nombre="storefront" />
              Tienda
            </Link>
            {/* Cerrar sesión: borra token y datos privados y vuelve al inicio. */}
            <Link to="/" className="btn btn-danger" onClick={cerrarSesion}>
              <Icono nombre="logout" />
              Cerrar sesión
            </Link>
          </div>
        </aside>
        <div className="col-md-9 col-lg-10 px-0">
          {/* Barra superior: sección actual, nombre y rol del usuario. */}
          <header className="admin-barra">
            <p className="admin-barra-ruta mb-0">
              <Icono nombre={actual?.[2] ?? 'dashboard'} />
              {actual?.[1] ?? 'Panel'}
            </p>
            <div className="d-flex align-items-center gap-2">
              <span className="small">
                {usuario.nombre} {usuario.apellidos}
              </span>
              <EtiquetaEstado tipo="info" icono="badge">
                {usuario.rol}
              </EtiquetaEstado>
            </div>
          </header>
          <main className="admin-contenido">
            {/* Aquí React Router dibuja la página de la ruta actual (Dashboard, Órdenes...). */}
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}
