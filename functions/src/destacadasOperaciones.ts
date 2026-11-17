/**
 * Reads Firestore and writes destacadas/actual (one read for Inicio).
 * Each entry carries an opaque key (hash of centro + author) so a featured review can be
 * withdrawn at once when it is hidden, edited, reported or deleted, without publishing the uid.
 */
import { FieldValue, Firestore, Timestamp } from "firebase-admin/firestore";
import { Candidata, claveDestacada, ConfigDestacadas, leerConfig, notaDeResena, seleccionar } from "./destacadas";

export const RUTA_DESTACADAS = "destacadas/actual";
export const RUTA_CONFIG = "config/destacadas";

const aFecha = (t: unknown) => (t instanceof Timestamp ? t.toDate() : new Date(0));

export async function generarDestacadas(db: Firestore, ahora: Date): Promise<{ n: number; config: ConfigDestacadas }> {
  const config = leerConfig((await db.doc(RUTA_CONFIG).get()).data());
  const desde = new Date(ahora.getTime() - config.maxDias * 86400000);

  // `actualizada` is set on creation and on every edit, so it is ≥ fecha.
  const [publicadas, denuncias] = await Promise.all([
    db.collectionGroup("valoraciones").where("estado", "==", "PUBLICADA").where("actualizada", ">=", Timestamp.fromDate(desde)).get(),
    db.collection("denuncias").where("estado", "==", "PENDIENTE").get(),
  ]);
  const denunciadas = new Set(denuncias.docs.map((d) => `${d.get("centro")}/${d.get("valoracionUid")}`));

  const candidatas: Candidata[] = publicadas.docs.flatMap((d) => {
    const v = d.data();
    const codigo = d.ref.parent.parent?.id;
    if (!codigo || typeof v.texto !== "string" || !v.centro || !v.instalaciones) return [];
    return [{
      codigo, uid: d.id, alias: v.alias ?? "", rolAutor: v.rolAutor ?? "OTRO", verificado: v.verificado === true,
      nota: notaDeResena(v as Parameters<typeof notaDeResena>[0]), texto: v.texto,
      fecha: aFecha(v.fecha), actualizada: aFecha(v.actualizada), estado: v.estado,
      denunciasPendientes: denunciadas.has(`${codigo}/${d.id}`),
    }];
  });
  const elegidas = seleccionar(candidatas, config, ahora);

  const centros = elegidas.length ? await db.getAll(...elegidas.map((e) => db.doc(`centros/${e.codigo}`))) : [];
  const resenas = elegidas.map((e, i) => ({
    clave: e.clave,
    codigo: e.codigo,
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
