/**
 * Version of the terms of use in force (web/condiciones.html). Google Play requires accepting
 * them before publishing content. Keep in step with firestore.rules (condicionesVigentes) and
 * the app (domain/model/Condiciones.kt).
 */
export const CONDICIONES_VIGENTES = "2026-11";

export const haAceptadoCondiciones = (perfil: { condiciones?: unknown } | undefined) =>
  perfil?.condiciones === CONDICIONES_VIGENTES;
