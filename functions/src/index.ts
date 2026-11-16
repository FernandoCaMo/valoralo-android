/**
 * Valoralo Cloud Functions (Fase 5). Region europe-southwest1 (Madrid), same as Firestore.
 *
 *  publicarValoracion   callable: validates, filters and writes the user's review.
 *  denunciarValoracion  callable: one report per user and review; 3 reports → EN_REVISION.
 *  alEscribirValoracion trigger: recalculates centros/{codigo}.stats and the catalogue entry.
 *  alBorrarUsuario      trigger: when usuarios/{uid} is deleted (account deletion), deletes
 *                       the user's reviews (each deletion recalculates its centro) and reports.
 *  solicitarVerificacion  callable: teacher verification request (doc 04).
 *  resolverVerificacion   callable, administrators only: approve or reject.
 *  caducarVerificaciones  daily: pending requests older than 30 days expire.
 */
import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getStorage } from "firebase-admin/storage";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { ErrorVerificacion } from "./verificacion/logica";
import { caducarPendientes, ErrorOperacion, resolver, solicitar } from "./verificacion/operaciones";
import { FieldValue, getFirestore, Timestamp } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import { setGlobalOptions } from "firebase-functions/v2";
import { onDocumentDeleted, onDocumentWritten } from "firebase-functions/v2/firestore";
import { CallableRequest, HttpsError, onCall } from "firebase-functions/v2/https";
import { defineBoolean } from "firebase-functions/params";
import { calcularStats, Rol, ValoracionParaStats } from "./calculo";
import { construirValoracion, ErrorFiltro, PerfilAutor, ValoracionGuardada } from "./publicacion";
import { ErrorValidacion, validarFormulario } from "./validacion";

initializeApp();
setGlobalOptions({ region: "europe-southwest1", maxInstances: 10 });

/**
 * App Check (Play Integrity, debug token in development). Read from functions/.env so it can
 * be switched on once the app is registered in App Check without changing code.
 */
const APP_CHECK_OBLIGATORIO = defineBoolean("APP_CHECK_OBLIGATORIO", { default: true });
export const UMBRAL_DENUNCIAS = 3;
const MOTIVOS_DENUNCIA = ["INSULTOS", "NOMBRE_PROPIO", "DATOS_PERSONALES", "FALSA_O_SPAM", "OTRO"];

function comprobarLlamada(request: CallableRequest): string {
  if (APP_CHECK_OBLIGATORIO.value() && !request.app) {
    throw new HttpsError("failed-precondition", "Esta versión de la app no está verificada.");
  }
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Inicia sesión para continuar.");
  return uid;
}

const db = () => getFirestore();

// ---------- publicarValoracion ----------

export const publicarValoracion = onCall(async (request) => {
  const uid = comprobarLlamada(request);
  const codigo = (request.data as { codigo?: unknown } | null)?.codigo;
  if (typeof codigo !== "string" || !/^\d{8}$/.test(codigo)) {
    throw new HttpsError("invalid-argument", "Código de centro no válido.");
  }
  const centroSnap = await db().doc(`centros/${codigo}`).get();
  if (!centroSnap.exists) throw new HttpsError("not-found", "No existe ese centro.");
  const centro = centroSnap.data()!;
  const departamentos: string[] = centro.departamentos ?? [];

  let formulario;
  try {
    formulario = validarFormulario(request.data, departamentos);
  } catch (e) {
    if (e instanceof ErrorValidacion) throw new HttpsError("invalid-argument", e.message, { campo: e.campo });
    throw e;
  }

  const perfilSnap = await db().doc(`usuarios/${uid}`).get();
  if (!perfilSnap.exists) throw new HttpsError("failed-precondition", "Completa tu perfil antes de valorar.");
  const p = perfilSnap.data()!;
  const perfil: PerfilAutor = { uid, alias: p.alias, rol: p.rol as Rol, verificado: p.verificado === true };

  const contexto = {
    permitidas: [centro.nombre, centro.localidad, centro.municipio, centro.provincia, ...departamentos]
      .filter((s): s is string => typeof s === "string"),
  };
  const ref = db().doc(`centros/${codigo}/valoraciones/${uid}`);
  return db().runTransaction(async (tx) => {
    const previaSnap = await tx.get(ref);
    const previa = previaSnap.exists ? aGuardada(previaSnap.data()!) : null;
    let valoracion;
    try {
      valoracion = construirValoracion(formulario, perfil, previa, new Date(), contexto);
    } catch (e) {
      if (e instanceof ErrorFiltro) {
        throw new HttpsError("invalid-argument", e.message, { motivo: "FILTRO", fragmentos: e.fragmentos });
      }
      throw e;
    }
    tx.set(ref, {
      ...valoracion,
      fecha: Timestamp.fromDate(valoracion.fecha),
      actualizada: Timestamp.fromDate(valoracion.actualizada),
    });
    return { estado: valoracion.estado, fragmentos: valoracion.revision?.fragmentos ?? [] };
  });
});

function aGuardada(d: FirebaseFirestore.DocumentData): ValoracionGuardada {
  return {
    uid: d.uid,
    alias: d.alias,
    rolAutor: d.rolAutor,
    verificado: d.verificado === true,
    fecha: d.fecha instanceof Timestamp ? d.fecha.toDate() : new Date(),
    estado: d.estado,
    revision: d.revision ?? null,
  };
}

// ---------- denunciarValoracion ----------

export const denunciarValoracion = onCall(async (request) => {
  const uid = comprobarLlamada(request);
  const { codigo, autorUid, motivo } = (request.data ?? {}) as Record<string, unknown>;
  if (typeof codigo !== "string" || !/^\d{8}$/.test(codigo) || typeof autorUid !== "string" || autorUid.length === 0 || autorUid.includes("/")) {
    throw new HttpsError("invalid-argument", "Reseña no válida.");
  }
  if (typeof motivo !== "string" || !MOTIVOS_DENUNCIA.includes(motivo)) {
    throw new HttpsError("invalid-argument", "Elige un motivo.");
  }
  if (autorUid === uid) throw new HttpsError("failed-precondition", "No puedes denunciar tu propia reseña.");

  const valoracionRef = db().doc(`centros/${codigo}/valoraciones/${autorUid}`);
  const valoracion = await valoracionRef.get();
  if (!valoracion.exists || valoracion.get("estado") !== "PUBLICADA") {
    throw new HttpsError("not-found", "Esa reseña ya no está publicada.");
  }

  try {
    await db().doc(`denuncias/${codigo}_${autorUid}_${uid}`).create({
      centro: codigo,
      valoracionUid: autorUid,
      denuncianteUid: uid,
      motivo,
      fecha: FieldValue.serverTimestamp(),
      estado: "PENDIENTE",
    });
  } catch (e) {
    if ((e as { code?: number }).code === 6) throw new HttpsError("already-exists", "Ya has denunciado esta reseña.");
    throw e;
  }

  // Only pending reports count: after a moderator approves a review, its reports are resolved.
  const total = (await db().collection("denuncias")
    .where("centro", "==", codigo).where("valoracionUid", "==", autorUid).where("estado", "==", "PENDIENTE")
    .count().get()).data().count;
  if (total >= UMBRAL_DENUNCIAS) {
    await db().runTransaction(async (tx) => {
      const actual = await tx.get(valoracionRef);
      if (actual.exists && actual.get("estado") === "PUBLICADA") {
        tx.update(valoracionRef, { estado: "EN_REVISION", revision: { origen: "DENUNCIAS", fragmentos: [] } });
      }
    });
    logger.info("Reseña a revisión por denuncias", { codigo, autorUid, total });
  }
  return { denuncias: total };
});

// ---------- Recalcular notas ----------

export async function recalcularCentro(codigo: string): Promise<void> {
  const centroRef = db().doc(`centros/${codigo}`);
  const centro = await centroRef.get();
  if (!centro.exists) return;
  const publicadas = await centroRef.collection("valoraciones").where("estado", "==", "PUBLICADA").get();
  const stats = calcularStats(publicadas.docs.map((d) => d.data() as ValoracionParaStats));
  await centroRef.update({ stats });

  const parte = await parteDelCatalogo(codigo, centro.get("catalogoDoc"), centro.get("provincia"));
  if (!parte) {
    logger.warn("Centro sin entrada en el catálogo", { codigo });
    return;
  }
  await db().runTransaction(async (tx) => {
    const snap = await tx.get(parte);
    const centros = (snap.get("centros") ?? []) as Record<string, unknown>[];
    const i = centros.findIndex((c) => c.codigo === codigo);
    if (i < 0) return;
    centros[i] = { ...centros[i], nota: stats.total.global, resenas: stats.total.n };
    tx.update(parte, { centros });
  });
}

async function parteDelCatalogo(codigo: string, catalogoDoc: unknown, provincia: unknown) {
  if (typeof catalogoDoc === "string") return db().doc(`catalogo/${catalogoDoc}`);
  // Fallback for centros imported before catalogoDoc existed.
  const partes = await db().collection("catalogo").where("provincia", "==", provincia).get();
  const encontrada = partes.docs.find((d) => ((d.get("centros") ?? []) as { codigo: string }[]).some((c) => c.codigo === codigo));
  return encontrada?.ref ?? null;
}

export const alEscribirValoracion = onDocumentWritten("centros/{codigo}/valoraciones/{uid}", async (event) => {
  const antes = event.data?.before.data();
  const despues = event.data?.after.data();
  // Only what changes the averages matters (e.g. not an alias-only change).
  const relevante = (d?: FirebaseFirestore.DocumentData) => d && JSON.stringify([d.estado, d.rolAutor, d.centro, d.instalaciones, d.profesorado]);
  if (relevante(antes) === relevante(despues)) return;
  await recalcularCentro(event.params.codigo);
});

// ---------- Verificación de docentes (doc 04) ----------

function aHttpsError(e: unknown): never {
  if (e instanceof ErrorVerificacion) throw new HttpsError("invalid-argument", e.message);
  if (e instanceof ErrorOperacion) throw new HttpsError(e.codigo, e.message);
  throw e;
}

export const solicitarVerificacion = onCall(async (request) => {
  const uid = comprobarLlamada(request);
  try {
    return await solicitar(db(), getStorage().bucket(), uid, request.data, new Date());
  } catch (e) {
    aHttpsError(e);
  }
});

/** Administrators only (custom claim admin = true, set with tools/admin/hacer_admin.py). */
export const resolverVerificacion = onCall(async (request) => {
  const uid = comprobarLlamada(request);
  if (request.auth?.token.admin !== true) throw new HttpsError("permission-denied", "Sin acceso.");
  try {
    return await resolver(db(), getAuth(), getStorage().bucket(), uid, request.data, new Date());
  } catch (e) {
    aHttpsError(e);
  }
});

export const caducarVerificaciones = onSchedule({ schedule: "every day 04:00", timeZone: "Europe/Madrid" }, async () => {
  const n = await caducarPendientes(db(), getStorage().bucket(), new Date());
  logger.info("Solicitudes de verificación caducadas", { n });
});

// ---------- Eliminar cuenta ----------

export const alBorrarUsuario = onDocumentDeleted("usuarios/{uid}", async (event) => {
  const uid = event.params.uid;
  const valoraciones = await db().collectionGroup("valoraciones").where("uid", "==", uid).get();
  const denuncias = await db().collection("denuncias").where("denuncianteUid", "==", uid).get();
  const lote = db().bulkWriter();
  valoraciones.docs.forEach((d) => lote.delete(d.ref));
  denuncias.docs.forEach((d) => lote.delete(d.ref));
  await lote.close();
  logger.info("Cuenta eliminada: datos borrados", { uid, valoraciones: valoraciones.size, denuncias: denuncias.size });
});
