/**
 * Featured reviews for Inicio (destacadas/actual): pure selection rules.
 * The numbers live in config/destacadas so they can change without deploying;
 * missing or invalid values fall back to CONFIG_POR_DEFECTO.
 */
import { createHash } from "node:crypto";

/** Opaque key of a review (centro + author) so it can be withdrawn without publishing the uid. */
export const claveDestacada = (codigo: string, uid: string) =>
  createHash("sha256").update(`${codigo}/${uid}`).digest("hex").slice(0, 24);

export interface ConfigDestacadas {
  minCaracteres: number;
  maxDias: number;
  maxResenas: number;
  /** At most this fraction of the selection with nota ≥ umbralAlta. */
  maxFraccionAltas: number;
  umbralAlta: number;
  /** Include one with nota ≤ umbralBaja if there is any. */
  umbralBaja: number;
  /** The app shows the carousel only with at least this many. */
  minimoParaMostrar: number;
  longitudExtracto: number;
  /** Likes of the last diasLikes days set the order. */
  diasLikes: number;
  /** Places kept for new reviews (written ≤ diasNueva days ago, ≤ maxLikesNueva likes). */
  huecosNuevas: number;
  diasNueva: number;
  maxLikesNueva: number;
}

export const CONFIG_POR_DEFECTO: ConfigDestacadas = {
  minCaracteres: 120,
  maxDias: 180,
  maxResenas: 12,
  maxFraccionAltas: 1 / 3,
  umbralAlta: 4.5,
  umbralBaja: 3.5,
  minimoParaMostrar: 3,
  longitudExtracto: 200,
  diasLikes: 7,
  huecosNuevas: 3,
  diasNueva: 3,
  maxLikesNueva: 2,
};

const RANGOS: Record<keyof ConfigDestacadas, [number, number]> = {
  minCaracteres: [0, 1000],
  maxDias: [1, 3650],
  maxResenas: [1, 50],
  maxFraccionAltas: [0, 1],
  umbralAlta: [1, 5],
  umbralBaja: [1, 5],
  minimoParaMostrar: [0, 50],
  longitudExtracto: [50, 1000],
  diasLikes: [1, 90],
  huecosNuevas: [0, 12],
  diasNueva: [1, 60],
  maxLikesNueva: [0, 1000],
};

export function leerConfig(datos: Record<string, unknown> | undefined): ConfigDestacadas {
  const config = { ...CONFIG_POR_DEFECTO };
  for (const clave of Object.keys(RANGOS) as (keyof ConfigDestacadas)[]) {
    const v = datos?.[clave];
    const [min, max] = RANGOS[clave];
    if (typeof v === "number" && Number.isFinite(v) && v >= min && v <= max) config[clave] = v;
  }
  return config;
}

export interface Candidata {
  codigo: string;
  uid: string;
  alias: string;
  rolAutor: string;
  verificado: boolean;
  /** Overall note of the review (notaDeResena). */
  nota: number;
  texto: string;
  fecha: Date;
  actualizada: Date;
  estado: string;
  denunciasPendientes: boolean;
  /** Total likes of the review (its nLikes counter). */
  nLikes: number;
  /** Likes in the last diasLikes days, and today (Europe/Madrid). */
  likesVentana: number;
  likesHoy: number;
}

export interface Destacada {
  clave: string;
  codigo: string;
  /** Id of the review document (= author uid, already public as such). Needed to like it from Inicio. */
  autorUid: string;
  nLikes: number;
  alias: string;
  rolAutor: string;
  verificado: boolean;
  nota: number;
  extracto: string;
  fecha: Date;
}

/** Same rule as the stats (doc 02): mean of centro, instalaciones and, if any, the departments' mean. */
export function notaDeResena(v: { centro: { nota: number }; instalaciones: { nota: number }; profesorado?: { nota: number }[] }): number {
  const dims = [v.centro.nota, v.instalaciones.nota];
  const deps = v.profesorado ?? [];
  if (deps.length > 0) dims.push(deps.reduce((a, d) => a + d.nota, 0) / deps.length);
  return dims.reduce((a, b) => a + b, 0) / dims.length;
}

export function extracto(texto: string, max: number): string {
  const t = texto.trim().replace(/\s+/g, " ");
  if (t.length <= max) return t;
  const corte = t.lastIndexOf(" ", max);
  return `${t.slice(0, corte > max / 2 ? corte : max).replace(/[\s.,;:]+$/, "")}…`;
}

const referencia = (c: Candidata) => Math.max(c.fecha.getTime(), c.actualizada.getTime());

/**
 * Order: most likes in the last diasLikes days, then most likes today, then the most recent.
 * huecosNuevas places go to new reviews (few likes yet), shown at positions 3, 6, 9…
 * Always: one per centro and per author, at most a fraction of high notes, a low one if any.
 */
export function seleccionar(candidatas: Candidata[], cfg: ConfigDestacadas, ahora: Date): Destacada[] {
  const desde = ahora.getTime() - cfg.maxDias * 86400000;
  const popularidad = (a: Candidata, b: Candidata) =>
    b.likesVentana - a.likesVentana || b.likesHoy - a.likesHoy
    || referencia(b) - referencia(a) || b.texto.length - a.texto.length;
  const validas = candidatas
    .filter((c) => c.estado === "PUBLICADA" && !c.denunciasPendientes && c.texto.trim().length >= cfg.minCaracteres && referencia(c) >= desde)
    .sort(popularidad);
  const desdeNueva = ahora.getTime() - cfg.diasNueva * 86400000;
  const esNueva = (c: Candidata) => c.fecha.getTime() >= desdeNueva && c.likesVentana <= cfg.maxLikesNueva;
  const nuevas = validas.filter(esNueva).sort((a, b) => b.fecha.getTime() - a.fecha.getTime());

  const esAlta = (c: Candidata) => c.nota >= cfg.umbralAlta;
  const esBaja = (c: Candidata) => c.nota <= cfg.umbralBaja;
  const maxAltas = Math.floor(cfg.maxResenas * cfg.maxFraccionAltas);

  let elegidas: Candidata[] = [];
  const enHueco = new Set<Candidata>();
  const libre = (c: Candidata) => !elegidas.some((e) => e.codigo === c.codigo || e.uid === c.uid);
  const cabe = (c: Candidata) => libre(c) && !(esAlta(c) && elegidas.filter(esAlta).length >= maxAltas);

  // The places for new reviews first, so the popular ones cannot take all of them.
  for (const c of nuevas) {
    if (enHueco.size >= Math.min(cfg.huecosNuevas, cfg.maxResenas)) break;
    if (cabe(c)) { elegidas.push(c); enHueco.add(c); }
  }
  for (const c of validas) {
    if (elegidas.length >= cfg.maxResenas) break;
    if (!elegidas.includes(c) && cabe(c)) elegidas.push(c);
  }

  // At least one low note if there is any eligible one (replaces the least popular non-low pick when full).
  if (!elegidas.some(esBaja)) {
    const baja = validas.find((c) => esBaja(c) && libre(c));
    if (baja) {
      if (elegidas.length >= cfg.maxResenas) {
        const quitar = [...elegidas].reverse().find((e) => !esBaja(e) && !enHueco.has(e))
          ?? [...elegidas].reverse().find((e) => !esBaja(e));
        if (quitar) elegidas = elegidas.filter((e) => e !== quitar);
      }
      if (elegidas.length < cfg.maxResenas) elegidas.push(baja);
    }
  }

  // With few picks, keep the high-note share within the fraction of what was chosen.
  while (elegidas.filter(esAlta).length > Math.max(1, Math.floor(elegidas.length * cfg.maxFraccionAltas))) {
    const ultimaAlta = [...elegidas].reverse().find(esAlta)!;
    elegidas = elegidas.filter((e) => e !== ultimaAlta);
  }

  // Popular ones by popularity, with the new ones at positions 3, 6, 9…
  const populares = elegidas.filter((e) => !enHueco.has(e)).sort(popularidad);
  const huecos = elegidas.filter((e) => enHueco.has(e));
  const orden: Candidata[] = [];
  while (populares.length || huecos.length) {
    const tocaHueco = orden.length % 3 === 2;
    orden.push(((tocaHueco && huecos.length) || !populares.length ? huecos : populares).shift()!);
  }

  return orden
    .map((c) => ({
      clave: claveDestacada(c.codigo, c.uid),
      codigo: c.codigo,
      autorUid: c.uid,
      nLikes: c.nLikes,
      alias: c.alias,
      rolAutor: c.rolAutor,
      verificado: c.verificado,
      nota: c.nota,
      extracto: extracto(c.texto, cfg.longitudExtracto),
      fecha: c.fecha,
    }));
}
