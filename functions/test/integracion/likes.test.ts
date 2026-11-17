/**
 * Likes against the emulators:  npm run test:integracion
 * The app writes the likes (checked by the rules, see rules-tests/); here they are written
 * with the Admin SDK and we check what the functions do with them.
 */
import { Timestamp } from "firebase-admin/firestore";
import { beforeAll, describe, expect, test } from "vitest";
import { crearAdmin, crearCentro, crearUsuario, db, esperar, llamar, Usuario } from "./ayuda";

const CODIGO = "21000701";
const CODIGO2 = "21000702";
// Own centros for the featured test (one review per centro is featured).
const CODIGO3 = "21000703";
const CODIGO4 = "21000704";
const TEXTO = "Muy buen centro: el profesorado se implica, las instalaciones están cuidadas y hay buen ambiente entre el alumnado y las familias.";
const formulario = (codigo = CODIGO, texto = TEXTO) => ({ codigo, centro: { nota: 4 }, instalaciones: { nota: 3 }, profesorado: [], texto });
const ruta = (uid: string, codigo = CODIGO) => `centros/${codigo}/valoraciones/${uid}`;
const nLikes = async (uid: string, codigo = CODIGO) => ((await db.doc(ruta(uid, codigo)).get()).get("nLikes") ?? null) as number | null;

async function like(autor: string, lector: string, codigo = CODIGO, fecha = Timestamp.now()) {
  await db.doc(`${ruta(autor, codigo)}/likes/${lector}`).set({ uid: lector, codigo, fecha });
}
const quitar = (autor: string, lector: string, codigo = CODIGO) => db.doc(`${ruta(autor, codigo)}/likes/${lector}`).delete();

let admin: Usuario;
beforeAll(async () => {
  await crearCentro(CODIGO);
  await crearCentro(CODIGO2);
  await crearCentro(CODIGO3);
  await crearCentro(CODIGO4);
  admin = await crearAdmin();
});

describe("contador nLikes", () => {
  test("empieza en 0, sube y baja con los likes, también con varios a la vez", async () => {
    const autor = await crearUsuario();
    await llamar("publicarValoracion", formulario(), autor);
    expect(await nLikes(autor.uid)).toBe(0);

    await Promise.all(["l1", "l2", "l3", "l4", "l5"].map((l) => like(autor.uid, l)));
    await esperar(() => nLikes(autor.uid), (n) => n === 5);
    await quitar(autor.uid, "l2");
    await esperar(() => nLikes(autor.uid), (n) => n === 4);
  });

  test("un evento repetido no descuadra el contador (vuelve a contar)", async () => {
    const autor = await crearUsuario();
    await llamar("publicarValoracion", formulario(), autor);
    await like(autor.uid, "r1");
    await esperar(() => nLikes(autor.uid), (n) => n === 1);
    // Rewriting the same like fires the trigger again: still 1.
    await like(autor.uid, "r1");
    await new Promise((r) => setTimeout(r, 1500));
    expect(await nLikes(autor.uid)).toBe(1);
  });

  test("editar la reseña no borra sus likes", async () => {
    const autor = await crearUsuario();
    await llamar("publicarValoracion", formulario(), autor);
    await like(autor.uid, "e1");
    await like(autor.uid, "e2");
    await esperar(() => nLikes(autor.uid), (n) => n === 2);
    await llamar("publicarValoracion", formulario(CODIGO, `${TEXTO} Lo recomiendo.`), autor);
    expect(await nLikes(autor.uid)).toBe(2);
  });

  test("una reseña oculta conserva sus likes", async () => {
    const autor = await crearUsuario();
    await llamar("publicarValoracion", formulario(), autor);
    await like(autor.uid, "o1");
    await esperar(() => nLikes(autor.uid), (n) => n === 1);
    await llamar("moderarValoracion", { codigo: CODIGO, uid: autor.uid, accion: "OCULTAR", motivo: "OTRO" }, admin);
    expect((await db.doc(ruta(autor.uid)).get()).get("estado")).toBe("OCULTA");
    expect(await nLikes(autor.uid)).toBe(1);
    expect((await db.collection(`${ruta(autor.uid)}/likes`).get()).size).toBe(1);
  });

  test("borrar una reseña borra sus likes", async () => {
    const autor = await crearUsuario();
    await llamar("publicarValoracion", formulario(), autor);
    await like(autor.uid, "b1");
    await esperar(() => nLikes(autor.uid), (n) => n === 1);
    await db.doc(ruta(autor.uid)).delete();
    await esperar(async () => (await db.collection(`${ruta(autor.uid)}/likes`).get()).size, (n) => n === 0);
  });

  test("borrar una cuenta borra sus likes (y se descuentan), su contador por hora y sus bloqueos", async () => {
    const autor = await crearUsuario();
    const lector = await crearUsuario();
    await llamar("publicarValoracion", formulario(), autor);
    await like(autor.uid, lector.uid);
    await like(autor.uid, "otro");
    await db.doc(`usuarios/${lector.uid}/limites/likes`).set({ inicio: Timestamp.now(), n: 1 });
    await db.doc(`usuarios/${lector.uid}/bloqueados/${autor.uid}`).set({ fecha: Timestamp.now() });
    await esperar(() => nLikes(autor.uid), (n) => n === 2);

    await db.doc(`usuarios/${lector.uid}`).delete();
    await esperar(() => nLikes(autor.uid), (n) => n === 1);
    expect((await db.doc(`${ruta(autor.uid)}/likes/${lector.uid}`).get()).exists).toBe(false);
    await esperar(async () => (await db.doc(`usuarios/${lector.uid}/limites/likes`).get()).exists, (e) => e === false);
    await esperar(async () => (await db.collection(`usuarios/${lector.uid}/bloqueados`).get()).size, (n) => n === 0);
  });
});

describe("destacadas por likes", () => {
  test("ordenadas por likes de los últimos 7 días; las antiguas no cuentan; guardan autorUid y nLikes", async () => {
    // Old enough not to be "new" (those get reserved places), recent enough for the 180 days.
    const hace10 = Timestamp.fromMillis(Date.now() - 10 * 86400000);
    const sembrar = async (codigo: string, uid: string) => db.doc(ruta(uid, codigo)).set({
      uid, alias: `A ${uid}`, rolAutor: "ESTUDIANTE", verificado: false, estado: "PUBLICADA", revision: null,
      centro: { nota: 3 }, instalaciones: { nota: 3 }, profesorado: [], texto: TEXTO, fecha: hace10, actualizada: hace10, nLikes: 0,
    });
    const popular = `pop${Date.now()}`;
    const antigua = `ant${Date.now()}`;
    await sembrar(CODIGO3, popular);
    await sembrar(CODIGO4, antigua);
    for (const l of ["p1", "p2", "p3"]) await like(popular, l, CODIGO3);
    // Many likes, but older than 7 days.
    const hace8 = Timestamp.fromMillis(Date.now() - 8 * 86400000);
    for (const l of ["a1", "a2", "a3", "a4", "a5"]) await like(antigua, l, CODIGO4, hace8);
    await esperar(() => nLikes(popular, CODIGO3), (n) => n === 3);

    await db.doc("config/destacadas").delete();
    await llamar("recalcularDestacadas", {}, admin);
    const resenas = ((await db.doc("destacadas/actual").get()).get("resenas") ?? []) as Record<string, unknown>[];
    const i = resenas.findIndex((r) => r.autorUid === popular);
    const j = resenas.findIndex((r) => r.autorUid === antigua);
    expect(i).toBeGreaterThanOrEqual(0);
    expect(j).toBeGreaterThan(i);
    expect(resenas[i]).toMatchObject({ codigo: CODIGO3, nLikes: 3 });
  });
});
