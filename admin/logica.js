// Pure helpers of the verification panel (tested with node --test in admin/test).

export const URL_VERIFICACION_JUNTA = "https://ws050.juntadeandalucia.es/verificarFirma";

/** Only the custom claim admin === true gives access (set with tools/admin/hacer_admin.py). */
export const esAdmin = (claims) => claims?.admin === true;

const dos = (n) => String(n).padStart(2, "0");

/** 17/11/2026 10:05 in Spanish local time. */
export function formatearFecha(fecha, conHora = true) {
  if (!(fecha instanceof Date) || isNaN(fecha.getTime())) return "—";
  const partes = new Intl.DateTimeFormat("es-ES", {
    timeZone: "Europe/Madrid", day: "2-digit", month: "2-digit", year: "numeric",
    hour: "2-digit", minute: "2-digit", hour12: false,
  }).formatToParts(fecha).reduce((acc, p) => ({ ...acc, [p.type]: p.value }), {});
  const dia = `${partes.day}/${partes.month}/${partes.year}`;
  return conHora ? `${dia} ${dos(partes.hour)}:${partes.minute}` : dia;
}

export function diasDesde(fecha, ahora = new Date()) {
  return Math.floor((ahora.getTime() - fecha.getTime()) / 86400000);
}

/** Same limits as resolverVerificacion. Returns an error message or null. */
export function validarMotivo(motivo) {
  const m = (motivo ?? "").trim();
  return m.length < 3 || m.length > 300 ? "Escribe el motivo del rechazo (de 3 a 300 caracteres). El usuario lo verá." : null;
}

export function validarCentro(codigo) {
  const c = (codigo ?? "").trim();
  return c === "" || /^\d{8}$/.test(c) ? null : "El código del centro tiene 8 cifras (o déjalo vacío).";
}

// ---------- Moderation of reviews (Fase 6) ----------

/** Same fixed reasons as functions/src/moderacion.ts (checked against the shared cases). */
export const MOTIVOS_OCULTAR = {
  NOMBRE_PERSONA: "Contiene el nombre de una persona",
  INSULTOS: "Insultos o lenguaje ofensivo",
  DATOS_PERSONALES: "Datos personales",
  NO_TRATA_DEL_CENTRO: "No trata sobre el centro",
  OTRO: "Otro",
};

export const ORIGEN_REVISION = {
  FILTRO: "Retenida por el filtro automático",
  DENUNCIAS: "Tres o más denuncias",
  MODERACION: "Editada después de que un moderador la ocultara",
};

export const MOTIVO_DENUNCIA = {
  INSULTOS: "Insultos o faltas de respeto",
  NOMBRE_PROPIO: "Nombra a una persona",
  DATOS_PERSONALES: "Datos personales",
  FALSA_O_SPAM: "Falsa o spam",
  OTRO: "Otro motivo",
};

/**
 * Splits a text into plain and marked pieces from the filter fragments ({inicio, fin}),
 * so the panel can highlight them with DOM nodes (never innerHTML). Fragments outside
 * the text (e.g. in "Otro departamento", inicio = -1) are ignored here.
 */
export function segmentos(texto, fragmentos) {
  const validos = (fragmentos ?? [])
    .filter((f) => Number.isInteger(f.inicio) && f.inicio >= 0 && f.fin > f.inicio && f.fin <= texto.length)
    .sort((a, b) => a.inicio - b.inicio);
  const salida = [];
  let i = 0;
  for (const f of validos) {
    if (f.inicio < i) continue;
    if (f.inicio > i) salida.push({ texto: texto.slice(i, f.inicio), marcado: false });
    salida.push({ texto: texto.slice(f.inicio, f.fin), marcado: true, motivo: f.motivo });
    i = f.fin;
  }
  if (i < texto.length) salida.push({ texto: texto.slice(i), marcado: false });
  return salida;
}

/** Pending reports grouped by review: [{ centro, uid, n, motivos: {MOTIVO: count} }], most reported first. */
export function agruparDenuncias(denuncias) {
  const grupos = new Map();
  for (const d of denuncias) {
    const clave = `${d.centro}/${d.valoracionUid}`;
    const g = grupos.get(clave) ?? { centro: d.centro, uid: d.valoracionUid, n: 0, motivos: {} };
    g.n += 1;
    g.motivos[d.motivo] = (g.motivos[d.motivo] ?? 0) + 1;
    grupos.set(clave, g);
  }
  return [...grupos.values()].sort((a, b) => b.n - a.n);
}

/** Result of "Recalcular destacadas ahora", with the numbers in use (config/destacadas). */
export function textoDestacadas(n, c) {
  const pct = Math.round(c.maxFraccionAltas * 100);
  const aviso = n < c.minimoParaMostrar ? ` Son menos de ${c.minimoParaMostrar}: Inicio mostrará el texto de bienvenida.` : "";
  return `${n} ${n === 1 ? "reseña destacada" : "reseñas destacadas"} (texto ≥ ${c.minCaracteres} caracteres, últimos ${c.maxDias} días, `
    + `máx. ${c.maxResenas}, hasta un ${pct} % con ${c.umbralAlta} o más).${aviso}`;
}

export const validarNota =(nota) => ((nota ?? "").trim().length > 500 ? "La nota interna puede tener como máximo 500 caracteres." : null);

/** What the reviewer must check (doc 04). */
export function comprobaciones(fechaAlta) {
  return [
    "El documento existe y es auténtico (introduce el CSV en la web de verificación de la Junta).",
    "El nombre del documento coincide con el nombre declarado.",
    `El documento está expedido después del ${formatearFecha(fechaAlta, false)} (alta de la cuenta en Valoralo).`,
  ];
}
