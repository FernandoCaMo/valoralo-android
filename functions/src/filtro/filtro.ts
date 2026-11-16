/**
 * Text filter for reviews (docs/decisiones.md). Deterministic, no external services.
 *
 *  BLOQUEADO  insult, clear proper name of a person, or personal data (e-mail, phone, link):
 *             the review is not saved and the user sees the fragments.
 *  DUDOSO     ambiguous name or insult: the review is saved EN_REVISION until a moderator
 *             approves it (or the user edits it and it passes).
 *  OK         published.
 *
 * Teachers are rated by department, never by person (doc 01), hence the name rules.
 */
import {
  ANTE_INSULTO, APELLIDOS, INSULTOS, INSULTOS_AMBIGUOS, NOMBRES, NOMBRES_AMBIGUOS,
  NOMBRES_FRECUENTES_EN_MINUSCULA, PERMITIDAS_BASE, TRATAMIENTOS,
} from "./listas";

export type Motivo = "INSULTO" | "NOMBRE_PROPIO" | "DATO_PERSONAL" | "NOMBRE_DUDOSO" | "INSULTO_DUDOSO";
export interface Fragmento {
  inicio: number;
  fin: number;
  texto: string;
  motivo: Motivo;
}
export interface ResultadoFiltro {
  resultado: "OK" | "DUDOSO" | "BLOQUEADO";
  fragmentos: Fragmento[];
}
export interface ContextoFiltro {
  /** Words that are never people here: the centro's name, localidad, departments… */
  permitidas?: string[];
}

const BLOQUEANTES: Motivo[] = ["INSULTO", "NOMBRE_PROPIO", "DATO_PERSONAL"];

export function normalizar(texto: string): string {
  return texto.normalize("NFD").replace(/\p{Mn}+/gu, "").toLowerCase();
}

/** Undo "leet" tricks: g1l1poll4s → gilipollas. */
function desLeet(palabra: string): string {
  return palabra.replace(/[0134578@$]/g, (c) => ({ "0": "o", "1": "i", "3": "e", "4": "a", "5": "s", "7": "t", "8": "b", "@": "a", "$": "s" })[c] ?? c);
}

/** puuuuta → puta; gilipollas → gilipolas (both sides are compared collapsed). */
const colapsar = (palabra: string) => palabra.replace(/(.)\1+/g, "$1");

interface Insulto { forma: string; prefijo: boolean }
const compilar = (lista: string[]): Insulto[] =>
  lista.map((e) => ({ forma: colapsar(normalizar(e.replace(/\*$/, ""))), prefijo: e.endsWith("*") }));
const INSULTOS_C = compilar(INSULTOS);
const AMBIGUOS_C = new Set(INSULTOS_AMBIGUOS.map((e) => colapsar(normalizar(e))));
const ANTE_INSULTO_S = new Set(ANTE_INSULTO);
const TRATAMIENTOS_S = new Set(TRATAMIENTOS.map(normalizar));
const NOMBRES_AMBIGUOS_S = new Set(NOMBRES_AMBIGUOS.map(normalizar));
const NOMBRES_S = new Set(NOMBRES.map(normalizar));
const APELLIDOS_S = new Set(APELLIDOS.map(normalizar));
const NOMBRES_MINUSCULA_S = new Set(
  NOMBRES.slice(0, NOMBRES_FRECUENTES_EN_MINUSCULA).map(normalizar).filter((n) => !NOMBRES_AMBIGUOS_S.has(n)),
);
const PERMITIDAS_BASE_S = new Set(PERMITIDAS_BASE.map(normalizar));
const CONECTORES = new Set(["de", "del", "la", "el", "los", "las", "y", "e"]);

const esInsulto = (palabra: string) => {
  const p = colapsar(desLeet(palabra));
  return INSULTOS_C.some((i) => (i.prefijo ? p.startsWith(i.forma) : p === i.forma));
};

interface Token {
  texto: string;
  norma: string;
  inicio: number;
  fin: number;
  mayuscula: boolean;
  inicioFrase: boolean;
}

function tokenizar(texto: string, excluidos: [number, number][]): Token[] {
  const tokens: Token[] = [];
  for (const m of texto.matchAll(/[\p{L}\p{N}@$]+(?:['’-][\p{L}\p{N}]+)*/gu)) {
    const inicio = m.index!;
    const fin = inicio + m[0].length;
    if (excluidos.some(([a, b]) => inicio < b && fin > a)) continue;
    const antes = texto.slice(0, inicio).trimEnd();
    tokens.push({
      texto: m[0],
      norma: normalizar(m[0]),
      inicio,
      fin,
      mayuscula: /^\p{Lu}/u.test(m[0]),
      inicioFrase: antes === "" || /[.!?¡¿:\n"«(]$/.test(antes),
    });
  }
  return tokens;
}

/** E-mails, links and phone numbers. */
function datosPersonales(texto: string): Fragmento[] {
  const patrones = [
    /[\p{L}\p{N}._%+-]+@[\p{L}\p{N}.-]+\.\p{L}{2,}/gu,
    /\b(?:https?:\/\/|www\.)\S+/giu,
    /\b[\p{L}\p{N}-]+\.(?:com|es|net|org|eu|info|io|me)(?:\/\S*)?\b/giu,
  ];
  const fragmentos: Fragmento[] = [];
  const anadir = (inicio: number, encontrado: string) =>
    fragmentos.push({ inicio, fin: inicio + encontrado.length, texto: encontrado, motivo: "DATO_PERSONAL" });
  for (const patron of patrones) {
    for (const m of texto.matchAll(patron)) anadir(m.index!, m[0]);
  }
  // Spanish phone numbers: 9 digits starting with 6-9, in any grouping, optional +34.
  for (const m of texto.matchAll(/(?:\+34[\s.-]*)?(?<!\d)[6789](?:[\s.-]?\d){8}(?!\d)/g)) {
    anadir(m.index!, m[0]);
  }
  return fragmentos;
}

/** Spelled-out insults: p.u.t.a, m-i-e-r-d-a. */
function insultosDeletreados(texto: string): Fragmento[] {
  const fragmentos: Fragmento[] = [];
  for (const m of texto.matchAll(/(?<![\p{L}\p{N}])\p{L}(?:[.\-_*\s]+\p{L}(?![\p{L}\p{N}])){2,}/gu)) {
    const palabra = normalizar(m[0].replace(/[.\-_*\s]+/g, ""));
    if (esInsulto(palabra)) {
      fragmentos.push({ inicio: m.index!, fin: m.index! + m[0].length, texto: m[0], motivo: "INSULTO" });
    }
  }
  return fragmentos;
}

export function filtrarTexto(texto: string, contexto: ContextoFiltro = {}): ResultadoFiltro {
  const permitidas = new Set(PERMITIDAS_BASE_S);
  for (const frase of contexto.permitidas ?? []) {
    for (const palabra of normalizar(frase).split(/[^\p{L}\p{N}]+/u)) if (palabra) permitidas.add(palabra);
  }

  const fragmentos: Fragmento[] = [...datosPersonales(texto), ...insultosDeletreados(texto)];
  const excluidos = fragmentos.map((f) => [f.inicio, f.fin] as [number, number]);
  const tokens = tokenizar(texto, excluidos);
  const marcar = (t: Token, motivo: Motivo): void => {
    fragmentos.push({ inicio: t.inicio, fin: t.fin, texto: t.texto, motivo });
  };

  const esNombreOApellido = (t: Token) => NOMBRES_S.has(t.norma) || APELLIDOS_S.has(t.norma);
  const esPersona = (t: Token) =>
    !permitidas.has(t.norma) && (esNombreOApellido(t) || (t.mayuscula && !CONECTORES.has(t.norma)));

  tokens.forEach((t, i) => {
    // Insults
    if (esInsulto(t.norma)) return marcar(t, "INSULTO");
    if (AMBIGUOS_C.has(colapsar(t.norma))) {
      const previas = tokens.slice(Math.max(0, i - 2), i).map((p) => p.norma);
      if (previas.some((p) => ANTE_INSULTO_S.has(p))) marcar(t, "INSULTO_DUDOSO");
      return;
    }
    if (permitidas.has(t.norma)) return;

    const anterior = tokens[i - 1];
    const siguiente = tokens[i + 1];
    // A person right after a title: "la profe Ana", "el profesor de lengua, Pérez", "Don Manuel".
    const ventana = tokens.slice(Math.max(0, i - 3), i);
    if (ventana.some((p) => TRATAMIENTOS_S.has(p.norma)) && esPersona(t) && !TRATAMIENTOS_S.has(t.norma)) {
      return marcar(t, "NOMBRE_PROPIO");
    }
    if (!t.mayuscula) {
      if (NOMBRES_MINUSCULA_S.has(t.norma)) marcar(t, "NOMBRE_DUDOSO");
      return;
    }
    // Capitalised from here on.
    const unido = (o?: Token) => o !== undefined && o.mayuscula && esNombreOApellido(o) && !permitidas.has(o.norma)
      && texto.slice(Math.min(o.fin, t.fin), Math.max(o.inicio, t.inicio)).trim() === "";
    if (esNombreOApellido(t) && (unido(anterior) || unido(siguiente))) return marcar(t, "NOMBRE_PROPIO");
    if (NOMBRES_S.has(t.norma) && !NOMBRES_AMBIGUOS_S.has(t.norma)) return marcar(t, "NOMBRE_PROPIO");
    if (!t.inicioFrase && (NOMBRES_S.has(t.norma) || APELLIDOS_S.has(t.norma))) return marcar(t, "NOMBRE_DUDOSO");
  });

  // One fragment per span, keeping the most serious reason.
  const porTramo = new Map<string, Fragmento>();
  for (const f of fragmentos) {
    const clave = `${f.inicio}-${f.fin}`;
    const previo = porTramo.get(clave);
    if (!previo || (BLOQUEANTES.includes(f.motivo) && !BLOQUEANTES.includes(previo.motivo))) porTramo.set(clave, f);
  }
  const unicos = [...porTramo.values()]
    .filter((f, _, todos) => !todos.some((o) => o !== f && o.inicio <= f.inicio && o.fin >= f.fin && (o.fin - o.inicio) > (f.fin - f.inicio)))
    .sort((a, b) => a.inicio - b.inicio);

  const resultado = unicos.some((f) => BLOQUEANTES.includes(f.motivo)) ? "BLOQUEADO"
    : unicos.length > 0 ? "DUDOSO" : "OK";
  return { resultado, fragmentos: unicos };
}
