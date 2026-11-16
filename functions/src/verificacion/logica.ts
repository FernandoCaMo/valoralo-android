/**
 * Teacher verification (doc 04): pure rules, no Firebase.
 */
import { createHash } from "node:crypto";

export const MAX_SOLICITUDES = 3;
export const VENTANA_SOLICITUDES_DIAS = 30;
export const CADUCIDAD_DIAS = 30;
const DIA_MS = 24 * 60 * 60 * 1000;

export class ErrorVerificacion extends Error {}

/** Junta de Andalucía CSV codes: letters and digits; spaces, dashes and dots are ignored. */
export function normalizarCsv(csv: unknown): string {
  const n = typeof csv === "string" ? csv.toUpperCase().replace(/[\s.-]+/g, "") : "";
  if (!/^[A-Z0-9]{10,64}$/.test(n)) {
    throw new ErrorVerificacion("El código CSV no es válido. Cópialo tal como aparece en el documento.");
  }
  return n;
}

/** Only the hash is kept after review: it blocks reusing the same document. */
export const hashCsv = (normalizado: string): string => createHash("sha256").update(normalizado).digest("hex");

export function validarNombre(nombre: unknown): string {
  const n = typeof nombre === "string" ? nombre.trim().replace(/\s+/g, " ") : "";
  const palabras = n.split(" ");
  if (n.length > 100 || palabras.length < 2 || palabras.some((p) => p.length < 2)
    || !/^[\p{L}][\p{L} '’.-]*$/u.test(n)) {
    throw new ErrorVerificacion("Escribe tu nombre y apellidos tal como aparecen en el documento.");
  }
  return n;
}

/** Optional PDF path: must be a .pdf directly inside verificaciones/{uid}/. */
export function validarPdf(uid: string, ruta: unknown): string | null {
  if (ruta === undefined || ruta === null || ruta === "") return null;
  const prefijo = `verificaciones/${uid}/`;
  if (typeof ruta !== "string" || !ruta.startsWith(prefijo) || !/^[A-Za-z0-9_-]{1,64}\.pdf$/.test(ruta.slice(prefijo.length))) {
    throw new ErrorVerificacion("El PDF no es válido.");
  }
  return ruta;
}

export function solicitudesRecientes(fechas: Date[], ahora: Date): number {
  const desde = ahora.getTime() - VENTANA_SOLICITUDES_DIAS * DIA_MS;
  return fechas.filter((f) => f.getTime() > desde).length;
}

export const caducaAntesDe = (ahora: Date): Date => new Date(ahora.getTime() - CADUCIDAD_DIAS * DIA_MS);

export interface Resolucion {
  uid: string;
  decision: "APROBAR" | "RECHAZAR";
  motivo: string | null;
  centroActualCodigo: string | null;
}

export function validarResolucion(datos: unknown): Resolucion {
  const d = (typeof datos === "object" && datos !== null ? datos : {}) as Record<string, unknown>;
  if (typeof d.uid !== "string" || !/^[A-Za-z0-9_-]{1,128}$/.test(d.uid)) throw new ErrorVerificacion("Usuario no válido.");
  if (d.decision !== "APROBAR" && d.decision !== "RECHAZAR") throw new ErrorVerificacion("Decisión no válida.");
  const motivo = typeof d.motivo === "string" ? d.motivo.trim() : "";
  if (d.decision === "RECHAZAR" && (motivo.length < 3 || motivo.length > 300)) {
    throw new ErrorVerificacion("Indica el motivo del rechazo (de 3 a 300 caracteres); el usuario lo verá.");
  }
  let centro: string | null = null;
  if (d.centroActualCodigo !== undefined && d.centroActualCodigo !== null && d.centroActualCodigo !== "") {
    if (typeof d.centroActualCodigo !== "string" || !/^\d{8}$/.test(d.centroActualCodigo)) {
      throw new ErrorVerificacion("El código del centro actual debe tener 8 cifras.");
    }
    centro = d.centroActualCodigo;
  }
  return { uid: d.uid, decision: d.decision, motivo: d.decision === "RECHAZAR" ? motivo : null, centroActualCodigo: centro };
}
