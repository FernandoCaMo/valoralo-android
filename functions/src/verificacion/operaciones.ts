/**
 * Teacher verification (doc 04) against Firestore, Auth and Storage.
 *
 * verificaciones/{uid}                    status, readable by the user and administrators:
 *   estado PENDIENTE | APROBADA | RECHAZADA | CADUCADA, fechaSolicitud, fechaRevision,
 *   revisorUid, motivoRechazo, csvHash, tienePdf, solicitudes [dates of the last 30 days]
 * verificaciones/{uid}/privado/solicitud  only administrators, only while PENDIENTE:
 *   nombreDeclarado, csv (plain, to check it on the Junta's website), pdfPath
 * csvUsados/{hash}                        { uid, fecha, resultado }: approved AND rejected
 *   documents can't be used again.
 */
import type { Auth } from "firebase-admin/auth";
import { Firestore, Timestamp } from "firebase-admin/firestore";
import {
  caducaAntesDe, hashCsv, MAX_SOLICITUDES, normalizarCsv, solicitudesRecientes, validarNombre, validarPdf,
  validarResolucion,
} from "./logica";

export type CodigoError = "failed-precondition" | "already-exists" | "resource-exhausted" | "not-found" | "invalid-argument";
export class ErrorOperacion extends Error {
  constructor(public readonly codigo: CodigoError, mensaje: string) {
    super(mensaje);
  }
}

/** Minimal Storage surface used here (a google-cloud/storage Bucket satisfies it). */
export interface Almacen {
  file(ruta: string): { exists(): Promise<[boolean]>; delete(opciones?: { ignoreNotFound?: boolean }): Promise<unknown> };
}

const fechas = (valor: unknown): Date[] =>
  (Array.isArray(valor) ? valor : []).map((v) => (v instanceof Timestamp ? v.toDate() : new Date(NaN))).filter((d) => !isNaN(d.getTime()));

export async function solicitar(db: Firestore, almacen: Almacen, uid: string, datos: unknown, ahora: Date) {
  const d = (typeof datos === "object" && datos !== null ? datos : {}) as Record<string, unknown>;
  const nombre = validarNombre(d.nombre);
  const csv = normalizarCsv(d.csv);
  const pdf = validarPdf(uid, d.pdfPath);
  const hash = hashCsv(csv);

  const perfil = await db.doc(`usuarios/${uid}`).get();
  if (!perfil.exists) throw new ErrorOperacion("failed-precondition", "Completa tu perfil antes de verificarte.");
  if (perfil.get("rol") === "DOCENTE" && perfil.get("verificado") === true) {
    throw new ErrorOperacion("failed-precondition", "Ya estás verificado como docente.");
  }
  if (pdf && !(await almacen.file(pdf).exists())[0]) {
    throw new ErrorOperacion("invalid-argument", "No encontramos el PDF. Vuelve a adjuntarlo.");
  }

  const ref = db.doc(`verificaciones/${uid}`);
  await db.runTransaction(async (tx) => {
    const actual = await tx.get(ref);
    if (actual.get("estado") === "PENDIENTE") {
      throw new ErrorOperacion("already-exists", "Ya tienes una solicitud pendiente de revisión.");
    }
    const recientes = fechas(actual.get("solicitudes")).filter((f) => solicitudesRecientes([f], ahora) === 1);
    if (recientes.length >= MAX_SOLICITUDES) {
      throw new ErrorOperacion("resource-exhausted", `Has hecho ${MAX_SOLICITUDES} solicitudes en los últimos 30 días. Vuelve a intentarlo más adelante.`);
    }
    const usado = await tx.get(db.doc(`csvUsados/${hash}`));
    const pendienteConElMismo = await tx.get(
      db.collection("verificaciones").where("csvHash", "==", hash).where("estado", "==", "PENDIENTE").limit(1),
    );
    if (usado.exists || !pendienteConElMismo.empty) {
      throw new ErrorOperacion("failed-precondition", "Este documento ya se ha usado en otra solicitud. Descarga uno nuevo del Portal del docente.");
    }
    tx.set(ref, {
      estado: "PENDIENTE",
      fechaSolicitud: Timestamp.fromDate(ahora),
      fechaRevision: null,
      revisorUid: null,
      motivoRechazo: null,
      csvHash: hash,
      tienePdf: pdf !== null,
      solicitudes: [...recientes, ahora].map((f) => Timestamp.fromDate(f)),
    });
    tx.set(ref.collection("privado").doc("solicitud"), { nombreDeclarado: nombre, csv, pdfPath: pdf });
  });
  return { estado: "PENDIENTE" as const };
}

export async function resolver(db: Firestore, auth: Auth, almacen: Almacen, revisorUid: string, datos: unknown, ahora: Date) {
  const r = validarResolucion(datos);
  const ref = db.doc(`verificaciones/${r.uid}`);
  const privadoRef = ref.collection("privado").doc("solicitud");
  const aprobada = r.decision === "APROBAR";

  const pdfPath = await db.runTransaction(async (tx) => {
    const v = await tx.get(ref);
    if (v.get("estado") !== "PENDIENTE") throw new ErrorOperacion("failed-precondition", "Esa solicitud ya no está pendiente.");
    const privado = await tx.get(privadoRef);
    const perfilRef = db.doc(`usuarios/${r.uid}`);
    const perfil = await tx.get(perfilRef);
    if (aprobada && !perfil.exists) throw new ErrorOperacion("not-found", "El usuario ya no existe.");

    if (aprobada) {
      tx.update(perfilRef, {
        rol: "DOCENTE",
        verificado: true,
        ...(r.centroActualCodigo ? { centroActualCodigo: r.centroActualCodigo } : {}),
      });
    }
    tx.set(db.doc(`csvUsados/${v.get("csvHash")}`), {
      uid: r.uid,
      fecha: Timestamp.fromDate(ahora),
      resultado: aprobada ? "APROBADA" : "RECHAZADA",
    });
    tx.update(ref, {
      estado: aprobada ? "APROBADA" : "RECHAZADA",
      fechaRevision: Timestamp.fromDate(ahora),
      revisorUid,
      motivoRechazo: r.motivo,
    });
    // Only hash, date and result remain (doc 04): name, plain CSV and PDF path go away.
    tx.delete(privadoRef);
    return (privado.get("pdfPath") as string | null | undefined) ?? null;
  });

  if (pdfPath) await almacen.file(pdfPath).delete({ ignoreNotFound: true });

  if (aprobada) {
    const usuario = await auth.getUser(r.uid);
    await auth.setCustomUserClaims(r.uid, { ...(usuario.customClaims ?? {}), rol: "DOCENTE", verificado: true });
    // Reviews published as "Otro" while pending become DOCENTE; those written as
    // ESTUDIANTE stay as they are (docs/decisiones.md). The trigger recalculates the stats.
    const suyas = await db.collectionGroup("valoraciones").where("uid", "==", r.uid).get();
    await Promise.all(suyas.docs
      .filter((d) => d.get("rolAutor") === "OTRO")
      .map((d) => d.ref.update({ rolAutor: "DOCENTE", verificado: true })));
  }
  return { estado: aprobada ? "APROBADA" : "RECHAZADA" };
}

/** Pending requests older than 30 days expire: status CADUCADA, private data and PDF deleted. */
export async function caducarPendientes(db: Firestore, almacen: Almacen, ahora: Date): Promise<number> {
  const limite = caducaAntesDe(ahora);
  const pendientes = await db.collection("verificaciones").where("estado", "==", "PENDIENTE").get();
  let caducadas = 0;
  for (const v of pendientes.docs) {
    const fecha = v.get("fechaSolicitud");
    if (!(fecha instanceof Timestamp) || fecha.toDate() >= limite) continue;
    const privadoRef = v.ref.collection("privado").doc("solicitud");
    const pdfPath = (await privadoRef.get()).get("pdfPath") as string | null | undefined;
    await db.batch()
      .update(v.ref, { estado: "CADUCADA", fechaRevision: Timestamp.fromDate(ahora) })
      .delete(privadoRef)
      .commit();
    if (pdfPath) await almacen.file(pdfPath).delete({ ignoreNotFound: true });
    caducadas += 1;
  }
  return caducadas;
}
