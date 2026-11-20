/**
 * Public alias (shown in reviews, Inicio and other users' block lists). Validated here, on the
 * server (callables crearPerfil and cambiarAlias, and again in publicarValoracion): the rules no
 * longer let the app write it. The app mirrors the format checks to warn at once
 * (domain/usecase/ValidacionRegistro.kt); insults are checked only here.
 *
 *  - 3–30 characters after trimming and collapsing spaces; letters, digits, spaces and `. _ -`.
 *  - No contact data: at most 5 digits in total (no phone numbers), no links ("algo.com", "www").
 *    E-mails can't be written (no `@`).
 *  - No reserved words that pass for a role, the moderation or the app: "docente", "verificado",
 *    "moderador", "admin", "Valoralo"… (accents, capitals, separators and leet are ignored).
 *  - No insults (same lists as the review filter).
 */
import { filtrarTexto, normalizar } from "./filtro/filtro";

export class ErrorAlias extends Error {}

export const ALIAS_MIN = 3;
export const ALIAS_MAX = 30;
const MAX_DIGITOS = 5;

/**
 * Compared on the alias reduced to letters (accents, capitals, separators and leet removed), so
 * "D0cente_Verificad@" or "v.a.l.o.r.a.l.o" are caught too.
 */
export const PALABRAS_RESERVADAS = [
  "docente", "verificad", "moderador", "moderacion", "admin", "valoralo",
  "juntadeandalucia", "consejeria", "soporte", "oficial",
];

const CARACTERES = /^[\p{L}\p{N} ._-]+$/u;
const ENLACE = /(^|[^\p{L}\p{N}])www([^\p{L}\p{N}]|$)|https?|[\p{L}\p{N}]\.(com|es|net|org|eu|info|io|me|app|tk|xyz|link|ly)([^\p{L}]|$)/iu;

export const normalizarAlias = (alias: string) => alias.trim().replace(/\s+/g, " ");

/** Lowercase letters only, accents and leet undone: "D0c-énte" → "docente". */
function soloLetras(alias: string): string {
  const sinLeet = normalizar(alias).replace(/[013457@$]/g, (c) => ({ "0": "o", "1": "i", "3": "e", "4": "a", "5": "s", "7": "t", "@": "a", "$": "s" })[c] ?? c);
  return sinLeet.replace(/[^a-zñ]/g, "");
}

/** Returns the normalised alias or throws [ErrorAlias] with a message for the user. */
export function validarAlias(valor: unknown): string {
  const alias = typeof valor === "string" ? normalizarAlias(valor) : "";
  if (alias.length < ALIAS_MIN) throw new ErrorAlias("El alias debe tener al menos 3 caracteres.");
  if (alias.length > ALIAS_MAX) throw new ErrorAlias("El alias puede tener como máximo 30 caracteres.");
  if (!CARACTERES.test(alias)) throw new ErrorAlias("El alias solo puede llevar letras, números, espacios y . _ -");
  if ((alias.match(/\p{N}/gu) ?? []).length > MAX_DIGITOS || ENLACE.test(alias)) {
    throw new ErrorAlias("El alias no puede llevar teléfonos, correos ni enlaces.");
  }
  const letras = soloLetras(alias);
  if (PALABRAS_RESERVADAS.some((p) => letras.includes(p))) {
    throw new ErrorAlias("Ese alias puede confundirse con un docente verificado, la moderación o la app. Elige otro.");
  }
  const filtro = filtrarTexto(alias);
  if (filtro.fragmentos.some((f) => f.motivo === "INSULTO" || f.motivo === "INSULTO_DUDOSO" || f.motivo === "DATO_PERSONAL")) {
    throw new ErrorAlias("El alias no puede llevar insultos ni palabras ofensivas.");
  }
  return alias;
}

/** For publicarValoracion and the production audit: true if the alias passes the current rules. */
export function aliasEsValido(alias: unknown): boolean {
  try {
    validarAlias(alias);
    return true;
  } catch (e) {
    if (e instanceof ErrorAlias) return false;
    throw e;
  }
}
