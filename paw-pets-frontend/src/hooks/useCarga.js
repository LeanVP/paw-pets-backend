// useEffect: dispara la carga al montar. useSyncExternalStore: hook oficial de React para leer datos que viven FUERA de React (aquí, db.js).
import { useEffect, useSyncExternalStore } from 'react';
// El almacén/caché de datos conectado a la API.
import db from '../data/db';

/**
 * Estado de carga de una o más colecciones: { cargando, error, reintentar }.
 * cargando = alguna todavía no llega del backend; error = el mensaje del primer error.
 */
// HOOK PROPIO. ...colecciones recoge varios nombres (ej: useCarga('boletas', 'productos')).
export default function useCarga(...colecciones) {
  // Convierte la lista en un texto estable ("boletas,productos") para usarlo como dependencia del efecto.
  const clave = colecciones.join(',');

  // Se ejecuta al montar el componente y cada vez que cambia "clave".
  useEffect(() => {
    // Pide cada colección al backend (db.cargar ignora las ya cargadas).
    clave.split(',').forEach((coleccion) => db.cargar(coleccion));
  // Solo se re-ejecuta si cambian las colecciones pedidas.
  }, [clave]);

  // El "resumen" es un texto (comparable por valor) para que React no redibuje de más.
  // Se suscribe a db: cada vez que db "avisa" un cambio, React vuelve a ejecutar la función y redibuja si el resultado cambió.
  const resumen = useSyncExternalStore(db.suscribir, () => {
    // Lee el estado actual de cada colección.
    const estados = clave.split(',').map((coleccion) => db.estado(coleccion));
    // Está cargando si alguna colección aún no llegó y no tiene error.
    const cargando = estados.some((e) => e.cargando || (!e.cargado && !e.error));
    // Toma el mensaje del primer error, si lo hay.
    const error = estados.find((e) => e.error)?.error ?? '';
    // Devuelve un TEXTO como resumen (comparable por valor) para que React no redibuje de más.
    return `${cargando ? 1 : 0}${error}`;
  });

  // Lo que usan los componentes (ver EstadoCarga en Estados.jsx).
  return {
    // El primer carácter indica si sigue cargando.
    cargando: resumen[0] === '1',
    // El resto del texto es el mensaje de error (o null si está vacío).
    error: resumen.slice(1) || null,
    // Función para el botón "Reintentar": vuelve a pedir ignorando la caché.
    reintentar: () => clave.split(',').forEach((coleccion) => db.recargar(coleccion)),
  };
}
