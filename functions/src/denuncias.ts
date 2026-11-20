/**
 * Limits on reports (QA 1.0.5, finding 14: one account could empty Inicio by reporting).
 * Numbers in config/denuncias, so they can change without deploying; missing or out-of-range
 * values fall back to CONFIG_DENUNCIAS_POR_DEFECTO.
 */
export const RUTA_CONFIG_DENUNCIAS = "config/denuncias";

export interface ConfigDenuncias {
  /** Reports one user can file per hour (counter usuarios/{uid}/limites/denuncias). */
  maxPorHora: number;
  /** Pending reports (from different users) needed to take a review out of Inicio. */
  paraRetirarDestacada: number;
}

export const CONFIG_DENUNCIAS_POR_DEFECTO: ConfigDenuncias = { maxPorHora: 10, paraRetirarDestacada: 2 };

const RANGOS: Record<keyof ConfigDenuncias, [number, number]> = {
  maxPorHora: [1, 1000],
  paraRetirarDestacada: [1, 50],
};

export function leerConfigDenuncias(datos: Record<string, unknown> | undefined): ConfigDenuncias {
  const config = { ...CONFIG_DENUNCIAS_POR_DEFECTO };
  for (const clave of Object.keys(RANGOS) as (keyof ConfigDenuncias)[]) {
    const v = datos?.[clave];
    const [min, max] = RANGOS[clave];
    if (typeof v === "number" && Number.isInteger(v) && v >= min && v <= max) config[clave] = v;
  }
  return config;
}

export interface Contador {
  inicio: Date;
  n: number;
}

const HORA_MS = 60 * 60 * 1000;

/**
 * Next value of the hourly counter, or null if the user has reached the limit this hour.
 * Same shape as the likes counter: { inicio, n }, a new hour starts again at 1.
 */
export function siguienteContador(actual: Contador | null, ahora: Date, max: number): Contador | null {
  if (!actual || ahora.getTime() - actual.inicio.getTime() >= HORA_MS) return { inicio: ahora, n: 1 };
  if (actual.n >= max) return null;
  return { inicio: actual.inicio, n: actual.n + 1 };
}
