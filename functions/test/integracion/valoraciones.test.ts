/**
 * End-to-end tests against the Auth + Firestore + Functions emulators:
 *   npm run test:integracion
 */
import { execFileSync } from "node:child_process";
import { existsSync } from "node:fs";
import { join } from "node:path";
import { beforeAll, describe, expect, test } from "vitest";
import { crearCentro, crearUsuario, db, entradaCatalogo, esperar, llamar, stats, Usuario } from "./ayuda";

const CODIGO = "21000693";
const ruta = (uid: string) => `centros/${CODIGO}/valoraciones/${uid}`;
const formulario = (texto = "Buen ambiente en el centro.", extra: Record<string, unknown> = {}) => ({
  codigo: CODIGO,
  centro: { nota: 4 },
  instalaciones: { nota: 3 },
  profesorado: [{ departamento: "Matemáticas", nota: 5 }],
  texto,
  ...extra,
});

let estudiante: Usuario;
let docente: Usuario;

beforeAll(async () => {
  await crearCentro(CODIGO);
  estudiante = await crearUsuario("ESTUDIANTE");
  docente = await crearUsuario("DOCENTE", true, "Profe Verificada");
});

describe("publicarValoracion", () => {
  test("sin sesión no se puede", async () => {
    const r = await llamar("publicarValoracion", formulario());
    expect(r.error?.status).toBe("UNAUTHENTICATED");
  });

  test("sin aceptar las condiciones vigentes no se puede publicar", async () => {
    const u = await crearUsuario("OTRO");
    await db.doc(`usuarios/${u.uid}`).update({ condiciones: "2020-01" });
    const r = await llamar("publicarValoracion", formulario(), u);
    expect(r.error?.status).toBe("FAILED_PRECONDITION");
    expect(r.error?.details).toMatchObject({ motivo: "CONDICIONES" });
    expect((await db.doc(ruta(u.uid)).get()).exists).toBe(false);
  });

  test("publica y recalcula las notas del centro y del catálogo", async () => {
    const r = await llamar("publicarValoracion", formulario(), estudiante);
    expect(r.result).toEqual({ estado: "PUBLICADA", fragmentos: [], origen: null });
    const guardada = (await db.doc(ruta(estudiante.uid)).get()).data()!;
    expect(guardada).toMatchObject({ uid: estudiante.uid, rolAutor: "ESTUDIANTE", estado: "PUBLICADA", editada: false });

    const s = await esperar(() => stats(CODIGO), (s) => s?.total?.n === 1);
    expect(s.total).toMatchObject({ n: 1, centro: 4, instalaciones: 3, profesorado: 5, global: 4 });
    expect(s.estudiante.n).toBe(1);
    expect(await esperar(() => entradaCatalogo(CODIGO), (c) => c?.resenas === 1)).toMatchObject({ nota: 4, resenas: 1 });
  });

  test("un docente verificado cuenta como DOCENTE", async () => {
    const r = await llamar("publicarValoracion", formulario("Claustro implicado.", { centro: { nota: 2 }, instalaciones: { nota: 2 }, profesorado: [] }), docente);
    expect(r.result?.estado).toBe("PUBLICADA");
    const s = await esperar(() => stats(CODIGO), (s) => s?.total?.n === 2);
    expect(s.docente).toMatchObject({ n: 1, centro: 2, global: 2, profesorado: null });
    expect(s.total.global).toBeCloseTo((4 + 2) / 2);
  });

  test("texto bloqueado: no se guarda y devuelve el fragmento", async () => {
    const u = await crearUsuario();
    const r = await llamar("publicarValoracion", formulario("La profe Ana es genial"), u);
    expect(r.error?.status).toBe("INVALID_ARGUMENT");
    expect(r.error?.details).toMatchObject({ motivo: "FILTRO", fragmentos: [{ texto: "Ana", motivo: "NOMBRE_PROPIO" }] });
    expect((await db.doc(ruta(u.uid)).get()).exists).toBe(false);
  });

  test("texto dudoso: queda en revisión fuera de las notas; al corregirlo se publica", async () => {
    const u = await crearUsuario("OTRO");
    const r = await llamar("publicarValoracion", formulario("juan explica fatal"), u);
    expect(r.result).toMatchObject({ estado: "EN_REVISION", origen: "FILTRO", fragmentos: [{ texto: "juan", motivo: "NOMBRE_DUDOSO" }] });
    await new Promise((res) => setTimeout(res, 1500));
    expect((await stats(CODIGO)).total.n).toBe(2); // not counted

    const corregida = await llamar("publicarValoracion", formulario("Explican fatal en ese departamento"), u);
    expect(corregida.result?.estado).toBe("PUBLICADA");
    expect((await esperar(() => stats(CODIGO), (s) => s?.total?.n === 3)).otro.n).toBe(1);
  });

  test("al editar se mantienen la fecha y el rol original aunque el perfil cambie", async () => {
    const antes = (await db.doc(ruta(estudiante.uid)).get()).data()!;
    await db.doc(`usuarios/${estudiante.uid}`).update({ rol: "OTRO" });
    const r = await llamar("publicarValoracion", formulario("Mejor de lo que pensaba.", { centro: { nota: 5 } }), estudiante);
    expect(r.result?.estado).toBe("PUBLICADA");
    const despues = (await db.doc(ruta(estudiante.uid)).get()).data()!;
    expect(despues.fecha.toMillis()).toBe(antes.fecha.toMillis());
    expect(despues).toMatchObject({ rolAutor: "ESTUDIANTE", editada: true, centro: { nota: 5 } });
  });

  test("datos no válidos", async () => {
    const r = await llamar("publicarValoracion", formulario("ok", { centro: { nota: 9 } }), estudiante);
    expect(r.error?.status).toBe("INVALID_ARGUMENT");
    const r2 = await llamar("publicarValoracion", formulario("ok", { profesorado: [{ departamento: "Química", nota: 3 }] }), estudiante);
    expect(r2.error?.message).toMatch(/no es de este centro/);
  });
});

describe("denunciarValoracion", () => {
  test("3 denuncias de usuarios distintos la pasan a revisión y salen de las notas", async () => {
    const autor = await crearUsuario("ESTUDIANTE", false, "Autor Denunciado");
    await llamar("publicarValoracion", formulario("Normal.", { centro: { nota: 1 }, instalaciones: { nota: 1 }, profesorado: [] }), autor);
    const conAutor = await esperar(() => stats(CODIGO), (s) => s?.total?.n === 4);
    expect(conAutor.total.n).toBe(4);

    const propia = await llamar("denunciarValoracion", { codigo: CODIGO, autorUid: autor.uid, motivo: "INSULTOS" }, autor);
    expect(propia.error?.status).toBe("FAILED_PRECONDITION");
    const sinMotivo = await llamar("denunciarValoracion", { codigo: CODIGO, autorUid: autor.uid, motivo: "PORQUE_SI" }, estudiante);
    expect(sinMotivo.error?.status).toBe("INVALID_ARGUMENT");

    const denunciantes = [await crearUsuario(), await crearUsuario(), await crearUsuario()];
    expect((await llamar("denunciarValoracion", { codigo: CODIGO, autorUid: autor.uid, motivo: "INSULTOS" }, denunciantes[0])).result).toEqual({ denuncias: 1 });
    const repetida = await llamar("denunciarValoracion", { codigo: CODIGO, autorUid: autor.uid, motivo: "OTRO" }, denunciantes[0]);
    expect(repetida.error?.status).toBe("ALREADY_EXISTS");
    await llamar("denunciarValoracion", { codigo: CODIGO, autorUid: autor.uid, motivo: "NOMBRE_PROPIO" }, denunciantes[1]);
    expect((await db.doc(ruta(autor.uid)).get()).get("estado")).toBe("PUBLICADA");
    await llamar("denunciarValoracion", { codigo: CODIGO, autorUid: autor.uid, motivo: "OTRO" }, denunciantes[2]);

    const v = (await db.doc(ruta(autor.uid)).get()).data()!;
    expect(v).toMatchObject({ estado: "EN_REVISION", revision: { origen: "DENUNCIAS" } });
    expect((await esperar(() => stats(CODIGO), (s) => s?.total?.n === 3)).total.n).toBe(3);

    // Editing does not republish a review that is under review because of reports.
    const editada = await llamar("publicarValoracion", formulario("Texto nuevo."), autor);
    expect(editada.result?.estado).toBe("EN_REVISION");
    denunciada = autor;
  });

  test.skipIf(!existsSync(PYTHON))("el script de moderación la aprueba, resuelve las denuncias y vuelve a contar", async () => {
    const listado = moderar("listar");
    expect(listado).toContain(denunciada.uid);
    expect(listado).toContain("DENUNCIAS");

    moderar("aprobar", CODIGO, denunciada.uid);
    expect((await db.doc(ruta(denunciada.uid)).get()).get("estado")).toBe("PUBLICADA");
    expect((await esperar(() => stats(CODIGO), (s) => s?.total?.n === 4)).total.n).toBe(4);

    // Old reports are resolved: a new one alone does not send it back to review.
    const otro = await crearUsuario();
    expect((await llamar("denunciarValoracion", { codigo: CODIGO, autorUid: denunciada.uid, motivo: "OTRO" }, otro)).result)
      .toEqual({ denuncias: 1 });
    expect((await db.doc(ruta(denunciada.uid)).get()).get("estado")).toBe("PUBLICADA");
  });
});

let denunciada: Usuario;
const PYTHON = join(__dirname, "..", "..", "..", "tools", "import", ".venv", "Scripts", "python.exe");
function moderar(...args: string[]): string {
  const script = join(__dirname, "..", "..", "..", "tools", "moderacion", "moderar.py");
  return execFileSync(PYTHON, [script, ...args, "--emulador", process.env.FIRESTORE_EMULATOR_HOST ?? "127.0.0.1:8080"], {
    encoding: "utf-8",
    env: { ...process.env, PYTHONIOENCODING: "utf-8" },
  });
}

describe("eliminar cuenta", () => {
  test("al borrar usuarios/{uid} se borran sus reseñas y se recalculan las notas", async () => {
    const u = await crearUsuario("OTRO");
    await llamar("publicarValoracion", formulario("Se va a borrar.", { centro: { nota: 5 }, instalaciones: { nota: 5 }, profesorado: [] }), u);
    const n = (await esperar(() => stats(CODIGO), (s) => s?.otro?.n === 2)).total.n;

    await db.doc(`usuarios/${u.uid}`).delete();
    await esperar(async () => (await db.doc(ruta(u.uid)).get()).exists, (existe) => !existe);
    const s = await esperar(() => stats(CODIGO), (s) => s?.total?.n === n - 1);
    expect(s.otro.n).toBe(1);
    expect((await esperar(() => entradaCatalogo(CODIGO), (c) => c?.resenas === n - 1))?.resenas).toBe(n - 1);
  });
});
