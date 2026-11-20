/**
 * Featured reviews (Inicio) against the emulators:  npm run test:integracion
 */
import { Timestamp } from "firebase-admin/firestore";
import { beforeAll, describe, expect, test } from "vitest";
import { claveDestacada } from "../../src/destacadas";
import { crearAdmin, crearCentro, crearUsuario, db, esperar, llamar, Usuario } from "./ayuda";

const CENTROS = ["21000801", "21000802", "21000803", "21000804"];
const TEXTO = "El centro tiene buen ambiente, las aulas están cuidadas y el departamento de matemáticas explica con paciencia y mucha claridad.";

let admin: Usuario;
const destacadas = async () => ((await db.doc("destacadas/actual").get()).get("resenas") ?? []) as Record<string, unknown>[];
const claves = async () => (await destacadas()).map((r) => r.clave);

async function sembrar(codigo: string, uid: string, nota: number, texto = TEXTO) {
  const ahora = Timestamp.now();
  await db.doc(`centros/${codigo}/valoraciones/${uid}`).set({
    uid, alias: `Autor ${uid.slice(0, 4)}`, rolAutor: "DOCENTE", verificado: true, estado: "PUBLICADA", revision: null,
    centro: { nota }, instalaciones: { nota }, profesorado: [], texto, fecha: ahora, actualizada: ahora,
  });
}

beforeAll(async () => {
  for (const c of CENTROS) await crearCentro(c);
  admin = await crearAdmin();
  await db.doc("config/destacadas").delete();
});

describe("destacadas", () => {
  test("solo administradores pueden recalcular", async () => {
    expect((await llamar("recalcularDestacadas", {}, await crearUsuario())).error?.status).toBe("PERMISSION_DENIED");
  });

  test("recalcular: guarda las que cumplen, sin uid; ocultar, editar o denunciar la retira al momento", async () => {
    const uids = ["dA1", "dB2", "dC3", "dD4"].map((u) => `${u}${Date.now()}`);
    await sembrar(CENTROS[0], uids[0], 4);
    await sembrar(CENTROS[1], uids[1], 3);
    await sembrar(CENTROS[2], uids[2], 4);
    await sembrar(CENTROS[3], uids[3], 4, "Texto corto.");

    const r = await llamar<{ n: number; config: { minCaracteres: number } }>("recalcularDestacadas", {}, admin);
    expect(r.result?.config.minCaracteres).toBe(120);
    const doc = (await db.doc("destacadas/actual").get()).data()!;
    expect(doc.minimoParaMostrar).toBe(3);
    const mias = (doc.resenas as Record<string, unknown>[]).filter((x) => CENTROS.includes(x.codigo as string));
    expect(mias.map((x) => x.codigo).sort()).toEqual(CENTROS.slice(0, 3));
    expect(mias[0]).toMatchObject({ nombre: "IES Delgado Hernández", rolAutor: "DOCENTE", verificado: true });
    // Only the review id (author uid, already public as the document id); no email or name.
    const primera = mias.find((x) => x.codigo === CENTROS[0])!;
    expect(primera.autorUid).toBe(uids[0]);
    expect(Object.keys(primera).sort()).toEqual(
      ["alias", "autorUid", "clave", "codigo", "extracto", "fecha", "municipio", "nLikes", "nombre", "nota", "rolAutor", "tipo", "verificado"],
    );

    // Hidden by a moderator.
    await llamar("moderarValoracion", { codigo: CENTROS[0], uid: uids[0], accion: "OCULTAR", motivo: "OTRO" }, admin);
    await esperar(claves, (c) => !c.includes(claveDestacada(CENTROS[0], uids[0])));
    // Reported: one report is not enough (QA 1.0.5, finding 14); two from different users take it out.
    await llamar("denunciarValoracion", { codigo: CENTROS[1], autorUid: uids[1], motivo: "OTRO" }, await crearUsuario());
    expect(await claves()).toContain(claveDestacada(CENTROS[1], uids[1]));
    await llamar("denunciarValoracion", { codigo: CENTROS[1], autorUid: uids[1], motivo: "OTRO" }, await crearUsuario());
    expect(await claves()).not.toContain(claveDestacada(CENTROS[1], uids[1]));
    // Deleted by its author.
    await db.doc(`centros/${CENTROS[2]}/valoraciones/${uids[2]}`).delete();
    await esperar(claves, (c) => !c.includes(claveDestacada(CENTROS[2], uids[2])));
  });

  test("los números se leen de config/destacadas", async () => {
    const uid = `dE5${Date.now()}`;
    await sembrar(CENTROS[3], uid, 3);
    await db.doc("config/destacadas").set({ minCaracteres: 900 });
    const r = await llamar<{ config: { minCaracteres: number } }>("recalcularDestacadas", {}, admin);
    expect(r.result?.config.minCaracteres).toBe(900);
    expect(await destacadas()).toEqual([]);
    await db.doc("config/destacadas").delete();
  });
});
