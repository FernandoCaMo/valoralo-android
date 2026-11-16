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

/** What the reviewer must check (doc 04). */
export function comprobaciones(fechaAlta) {
  return [
    "El documento existe y es auténtico (introduce el CSV en la web de verificación de la Junta).",
    "El nombre del documento coincide con el nombre declarado.",
    `El documento está expedido después del ${formatearFecha(fechaAlta, false)} (alta de la cuenta en Valoralo).`,
  ];
}
