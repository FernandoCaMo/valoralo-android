/**
 * Moderation of reviews (Fase 6): the panel calls moderarValoracion, and
 * tools/moderacion/moderar.py applies the same transitions (shared cases in
 * functions/test/casos_moderacion.json) so both leave the same record.
 *
 *  APROBAR              EN_REVISION → PUBLICADA (revision removed)
 *  OCULTAR              EN_REVISION or PUBLICADA → OCULTA, with a fixed reason the author sees
 *  DESCARTAR_DENUNCIAS  PUBLICADA stays PUBLICADA; its pending reports are dismissed
 * Every action resolves the review's pending reports and records decision, date,
 * moderator, reason and an internal note.
 */
import type { Estado } from "./calculo";

export const MOTIVOS_OCULTAR = {
  NOMBRE_PERSONA: "Contiene el nombre de una persona",
  INSULTOS: "Insultos o lenguaje ofensivo",
  DATOS_PERSONALES: "Datos personales",
  NO_TRATA_DEL_CENTRO: "No trata sobre el centro",
  OTRO: "Otro",
} as const;
export type MotivoOcultar = keyof typeof MOTIVOS_OCULTAR;
export type AccionModeracion = "APROBAR" | "OCULTAR" | "DESCARTAR_DENUNCIAS";

const DECISION = { APROBAR: "APROBADA", OCULTAR: "OCULTADA", DESCARTAR_DENUNCIAS: "DENUNCIAS_DESCARTADAS" } as const;
const DECISION_DENUNCIAS = { APROBAR: "APROBADA", OCULTAR: "OCULTADA", DESCARTAR_DENUNCIAS: "DESCARTADA" } as const;
const ESTADOS_PERMITIDOS: Record<AccionModeracion, Estado[]> = {
  APROBAR: ["EN_REVISION"],
  OCULTAR: ["EN_REVISION", "PUBLICADA"],
  DESCARTAR_DENUNCIAS: ["PUBLICADA"],
};

export class ErrorModeracion extends Error {}

export interface Moderacion {
  codigo: string;
  uid: string;
  accion: AccionModeracion;
  motivo: MotivoOcultar | null;
  nota: string | null;
}

export function validarModeracion(datos: unknown): Moderacion {
  const d = (typeof datos === "object" && datos !== null ? datos : {}) as Record<string, unknown>;
  if (typeof d.codigo !== "string" || !/^\d{8}$/.test(d.codigo)) throw new ErrorModeracion("Centro no válido.");
  if (typeof d.uid !== "string" || !/^[A-Za-z0-9_-]{1,128}$/.test(d.uid)) throw new ErrorModeracion("Autor no válido.");
  if (typeof d.accion !== "string" || !(d.accion in DECISION)) throw new ErrorModeracion("Acción no válida.");
  const motivo = typeof d.motivo === "string" && d.motivo !== "" ? d.motivo : null;
  const nota = typeof d.nota === "string" && d.nota.trim() !== "" ? d.nota.trim() : null;
  return { codigo: d.codigo, uid: d.uid, accion: d.accion as AccionModeracion, motivo: motivo as MotivoOcultar | null, nota };
}

export function cambiosModeracion(
  estadoActual: Estado,
  accion: string,
  motivo: string | null,
  nota: string | null,
  moderadorUid: string,
  ahora: Date,
) {
  if (!(accion in DECISION)) throw new ErrorModeracion("Acción no válida.");
  const a = accion as AccionModeracion;
  if (!ESTADOS_PERMITIDOS[a].includes(estadoActual)) {
    throw new ErrorModeracion(`No se puede ${a === "APROBAR" ? "aprobar" : a === "OCULTAR" ? "ocultar" : "descartar las denuncias de"} una reseña en estado ${estadoActual}.`);
  }
  if (a === "OCULTAR" && (motivo === null || !(motivo in MOTIVOS_OCULTAR))) {
    throw new ErrorModeracion("Elige el motivo por el que se oculta; el autor lo verá.");
  }
  if (nota !== null && nota.length > 500) throw new ErrorModeracion("La nota interna puede tener como máximo 500 caracteres.");

  const motivoFinal = a === "OCULTAR" ? (motivo as MotivoOcultar) : null;
  const moderacion = {
    decision: DECISION[a],
    motivo: motivoFinal,
    motivoTexto: motivoFinal ? MOTIVOS_OCULTAR[motivoFinal] : null,
    nota,
    fecha: ahora,
    moderadorUid,
  };
  const valoracion: Record<string, unknown> =
    a === "APROBAR" ? { estado: "PUBLICADA", revision: null, moderacion }
      : a === "OCULTAR" ? { estado: "OCULTA", moderacion }
        : { moderacion };
  return {
    valoracion,
    denuncias: { estado: "RESUELTA", decision: DECISION_DENUNCIAS[a], resuelta: ahora, moderadorUid },
  };
}
