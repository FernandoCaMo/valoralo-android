/**
 * Moderation (Fase 6) against the emulators:  npm run test:integracion
 */
import { execFileSync } from "node:child_process";
import { existsSync } from "node:fs";
import { join } from "node:path";
import { beforeAll, describe, expect, test } from "vitest";
import { crearAdmin, crearCentro, crearUsuario, db, esperar, llamar, stats, Usuario } from "./ayuda";

const CODIGO = "21000901";
const ruta = (uid: string) => `centros/${CODIGO}/valoraciones/${uid}`;
const formulario = (texto: string, n = 4) => ({ codigo: CODIGO, centro: { nota: n }, instalaciones: { nota: n }, profesorado: [], texto });
const leer = async (uid: string) => (await db.doc(ruta(uid)).get()).data()!;

let admin: Usuario;
beforeAll(async () => {
  await crearCentro(CODIGO);
  admin = await crearAdmin();
});

describe("moderarValoracion", () => {
  test("solo administradores", async () => {
    const autor = await crearUsuario();
    await llamar("publicarValoracion", formulario("juan explica fatal"), autor);
    const r = await llamar("moderarValoracion", { codigo: CODIGO, uid: autor.uid, accion: "APROBAR" }, await crearUsuario());
    expect(r.error?.status).toBe("PERMISSION_DENIED");
  });

  test("ocultar con motivo; al editarla vuelve a revisión aunque pase el filtro; solo un moderador la publica", async () => {
    const autor = await crearUsuario("OTRO");
    expect((await llamar("publicarValoracion", formulario("juan explica fatal"), autor)).result?.estado).toBe("EN_REVISION");

    const sinMotivo = await llamar("moderarValoracion", { codigo: CODIGO, uid: autor.uid, accion: "OCULTAR" }, admin);
    expect(sinMotivo.error?.status).toBe("FAILED_PRECONDITION");
    const oculta = await llamar("moderarValoracion",
      { codigo: CODIGO, uid: autor.uid, accion: "OCULTAR", motivo: "NOMBRE_PERSONA", nota: "Nombra a un profesor." }, admin);
    expect(oculta.result).toMatchObject({ estado: "OCULTA" });
    expect((await leer(autor.uid)).moderacion).toMatchObject({
      decision: "OCULTADA", motivo: "NOMBRE_PERSONA", motivoTexto: "Contiene el nombre de una persona",
      nota: "Nombra a un profesor.", moderadorUid: admin.uid,
    });

    // Clean text: back to review (MODERACION), not published.
    expect((await llamar("publicarValoracion", formulario("Explican fatal en ese departamento."), autor)).result?.estado).toBe("EN_REVISION");
    expect((await leer(autor.uid)).revision).toMatchObject({ origen: "MODERACION" });
    // Doubtful, then clean again: still waiting for a moderator.
    await llamar("publicarValoracion", formulario("juan explica fatal"), autor);
    expect((await llamar("publicarValoracion", formulario("Explican fatal."), autor)).result?.estado).toBe("EN_REVISION");
    expect((await leer(autor.uid)).revision.origen).toBe("MODERACION");
    await new Promise((r) => setTimeout(r, 1000));
    expect((await stats(CODIGO))?.total?.n ?? 0).toBe(0);

    const aprobada = await llamar("moderarValoracion", { codigo: CODIGO, uid: autor.uid, accion: "APROBAR" }, admin);
    expect(aprobada.result).toMatchObject({ estado: "PUBLICADA" });
    expect((await leer(autor.uid)).revision).toBeNull();
    expect((await esperar(() => stats(CODIGO), (s) => s?.total?.n === 1)).total.n).toBe(1);
  });

  test("descartar las denuncias de una publicada: sigue publicada y las denuncias se resuelven", async () => {
    const autor = await crearUsuario();
    await llamar("publicarValoracion", formulario("Buen centro.", 5), autor);
    const [d1, d2] = [await crearUsuario(), await crearUsuario()];
    await llamar("denunciarValoracion", { codigo: CODIGO, autorUid: autor.uid, motivo: "INSULTOS" }, d1);
    await llamar("denunciarValoracion", { codigo: CODIGO, autorUid: autor.uid, motivo: "OTRO" }, d2);

    const r = await llamar("moderarValoracion", { codigo: CODIGO, uid: autor.uid, accion: "DESCARTAR_DENUNCIAS", nota: "Sin problema." }, admin);
    expect(r.result).toEqual({ estado: "PUBLICADA", denunciasResueltas: 2 });
    expect((await leer(autor.uid))).toMatchObject({ estado: "PUBLICADA", moderacion: { decision: "DENUNCIAS_DESCARTADAS" } });
    const denuncias = await db.collection("denuncias").where("valoracionUid", "==", autor.uid).get();
    expect(denuncias.docs.map((d) => d.data())).toEqual(expect.arrayContaining([
      expect.objectContaining({ estado: "RESUELTA", decision: "DESCARTADA", moderadorUid: admin.uid }),
    ]));
    expect(denuncias.docs.every((d) => d.get("estado") === "RESUELTA")).toBe(true);
    // A new report starts counting from 1.
    expect((await llamar("denunciarValoracion", { codigo: CODIGO, autorUid: autor.uid, motivo: "OTRO" }, await crearUsuario())).result)
      .toEqual({ denuncias: 1 });
  });

  const PYTHON = join(__dirname, "..", "..", "..", "tools", "import", ".venv", "Scripts", "python.exe");
  test.skipIf(!existsSync(PYTHON))("el script deja la misma constancia que el panel", async () => {
    const [a, b] = [await crearUsuario(), await crearUsuario()];
    await llamar("publicarValoracion", formulario("juan explica fatal"), a);
    await llamar("publicarValoracion", formulario("pedro explica fatal"), b);

    await llamar("moderarValoracion", { codigo: CODIGO, uid: a.uid, accion: "OCULTAR", motivo: "INSULTOS", nota: "n" }, admin);
    execFileSync(PYTHON, [
      join(__dirname, "..", "..", "..", "tools", "moderacion", "moderar.py"),
      "ocultar", CODIGO, b.uid, "--motivo", "INSULTOS", "--nota", "n",
      "--emulador", process.env.FIRESTORE_EMULATOR_HOST ?? "127.0.0.1:8080",
    ], { encoding: "utf-8", env: { ...process.env, PYTHONIOENCODING: "utf-8" } });

    const [va, vb] = [await leer(a.uid), await leer(b.uid)];
    const sinVariables = (m: Record<string, unknown>) => ({ ...m, fecha: "x", moderadorUid: "x" });
    expect(vb.estado).toBe(va.estado);
    expect(sinVariables(vb.moderacion)).toEqual(sinVariables(va.moderacion));
    expect(Object.keys(vb.moderacion).sort()).toEqual(Object.keys(va.moderacion).sort());
    expect(vb.moderacion.moderadorUid).toBe("script-moderar");
  });
});

describe("eliminar cuenta", () => {
  test("al borrar usuarios/{uid} también se borran sus favoritos", async () => {
    const u = await crearUsuario();
    await db.doc(`usuarios/${u.uid}/favoritos/${CODIGO}`).set({ codigo: CODIGO, fecha: new Date() });
    await db.doc(`usuarios/${u.uid}`).delete();
    await esperar(async () => (await db.collection(`usuarios/${u.uid}/favoritos`).get()).size, (n) => n === 0);
  });
});
