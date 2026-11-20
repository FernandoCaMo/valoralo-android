/**
 * Reads Firestore and writes destacadas/actual (one read for Inicio).
 * Each entry carries an opaque key (hash of centro + author) so a featured review can be
 * withdrawn at once when it is hidden, edited, reported or deleted, without publishing the uid.
 */
import { FieldValue, Firestore, Timestamp } from "firebase-admin/firestore";
import { Candidata, claveDestacada, ConfigDestacadas, leerConfig, notaDeResena, seleccionar } from "./destacadas";
import { leerConfigDenuncias, RUTA_CONFIG_DENUNCIAS } from "./denuncias";

export const RUTA_DESTACADAS = "destacadas/actual";
export const RUTA_CONFIG = "config/destacadas";

const aFecha = (t: unknown) => (t instanceof Timestamp ? t.toDate() : new Date(0));

/** "2026-11-17" in Spain's time zone, to tell today's likes. */
const DIA_MADRID = new Intl.DateTimeFormat("en-CA", { timeZone: "Europe/Madrid", year: "numeric", month: "2-digit", day: "2-digit" });
export const diaEnMadrid = (d: Date) => DIA_MADRID.format(d);

export async function generarDestacadas(db: Firestore, ahora: Date): Promise<{ n: number; config: ConfigDestacadas }> {
  const config = leerConfig((await db.doc(RUTA_CONFIG).get()).data());
  const { paraRetirarDestacada } = leerConfigDenuncias((await db.doc(RUTA_CONFIG_DENUNCIAS).get()).data());
  const desde = new Date(ahora.getTime() - config.maxDias * 86400000);

  const desdeLikes = new Date(ahora.getTime() - config.diasLikes * 86400000);

  // `actualizada` is set on creation and on every edit, so it is ≥ fecha.
  const [publicadas, denuncias, likes] = await Promise.all([
    db.collectionGroup("valoraciones").where("estado", "==", "PUBLICADA").where("actualizada", ">=", Timestamp.fromDate(desde)).get(),
    db.collection("denuncias").where("estado", "==", "PENDIENTE").get(),
    db.collectionGroup("likes").where("fecha", ">=", Timestamp.fromDate(desdeLikes)).get(),
  ]);
  // Pending reports per review; it is left out with config/denuncias.paraRetirarDestacada or more.
  const denunciadas = new Map<string, number>();
  for (const d of denuncias.docs) {
    const clave = `${d.get("centro")}/${d.get("valoracionUid")}`;
    denunciadas.set(clave, (denunciadas.get(clave) ?? 0) + 1);
  }

  // Likes in the window and today (Europe/Madrid) per review: centros/{codigo}/valoraciones/{autor}/likes/{uid}.
  const hoy = diaEnMadrid(ahora);
  const enVentana = new Map<string, number>();
  const deHoy = new Map<string, number>();
  for (const l of likes.docs) {
    const resena = l.ref.parent.parent;
    const codigo = resena?.parent.parent?.id;
    if (!resena || !codigo) continue;
    const clave = `${codigo}/${resena.id}`;
    enVentana.set(clave, (enVentana.get(clave) ?? 0) + 1);
    if (diaEnMadrid(aFecha(l.get("fecha"))) === hoy) deHoy.set(clave, (deHoy.get(clave) ?? 0) + 1);
  }

  const candidatas: Candidata[] = publicadas.docs.flatMap((d) => {
    const v = d.data();
    const codigo = d.ref.parent.parent?.id;
    if (!codigo || typeof v.texto !== "string" || !v.centro || !v.instalaciones) return [];
    return [{
      codigo, uid: d.id, alias: v.alias ?? "", rolAutor: v.rolAutor ?? "OTRO", verificado: v.verificado === true,
      nota: notaDeResena(v as Parameters<typeof notaDeResena>[0]), texto: v.texto,
      fecha: aFecha(v.fecha), actualizada: aFecha(v.actualizada), estado: v.estado,
      denunciasPendientes: (denunciadas.get(`${codigo}/${d.id}`) ?? 0) >= paraRetirarDestacada,
      nLikes: typeof v.nLikes === "number" ? v.nLikes : 0,
      likesVentana: enVentana.get(`${codigo}/${d.id}`) ?? 0,
      likesHoy: deHoy.get(`${codigo}/${d.id}`) ?? 0,
    }];
  });
  const elegidas = seleccionar(candidatas, config, ahora);

  const centros = elegidas.length ? await db.getAll(...elegidas.map((e) => db.doc(`centros/${e.codigo}`))) : [];
  const resenas = elegidas.map((e, i) => ({
    clave: e.clave,
    codigo: e.codigo,
    autorUid: e.autorUid,
    nLikes: e.nLikes,
    nombre: centros[i].get("nombre") ?? e.codigo,
    municipio: centros[i].get("municipio") ?? null,
    tipo: centros[i].get("tipo") ?? null,
    alias: e.alias,
    rolAutor: e.rolAutor,
    verificado: e.verificado,
    nota: Math.round(e.nota * 10) / 10,
    extracto: e.extracto,
    fecha: Timestamp.fromDate(e.fecha),
  }));
  await db.doc(RUTA_DESTACADAS).set({
    resenas,
    minimoParaMostrar: config.minimoParaMostrar,
    generada: FieldValue.serverTimestamp(),
  });
  return { n: resenas.length, config };
}

/** Removes the review from destacadas/actual if it is there. Returns whether it was. */
export async function retirarDestacada(db: Firestore, codigo: string, uid: string): Promise<boolean> {
  const clave = claveDestacada(codigo, uid);
  const ref = db.doc(RUTA_DESTACADAS);
  return db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const resenas = (snap.get("resenas") ?? []) as { clave: string }[];
    if (!resenas.some((r) => r.clave === clave)) return false;
    tx.update(ref, { resenas: resenas.filter((r) => r.clave !== clave) });
    return true;
  });
}
