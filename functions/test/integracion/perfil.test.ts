/**
 * QA 1.0.5: alias validated on the server (findings 2 and 13) and hourly limit on reports (finding 14).
 */
import { beforeAll, describe, expect, test } from "vitest";
import { crearCentro, crearUsuario, db, llamar, PROYECTO, Usuario } from "./ayuda";

const AUTH = `http://${process.env.FIREBASE_AUTH_EMULATOR_HOST ?? "127.0.0.1:9099"}`;
const CODIGO = "21009901";

/** An Auth account without usuarios/{uid}, as right after signing up. */
async function cuentaSinPerfil(): Promise<Usuario> {
  const correo = `p${Date.now()}_${Math.random().toString(36).slice(2)}@example.com`;
  const r = await fetch(`${AUTH}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=demo`, {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: correo, password: "valoralo2026", returnSecureToken: true }),
  });
  const d = (await r.json()) as { localId: string; idToken: string };
  return { uid: d.localId, token: d.idToken, correo };
}

const datosPerfil = (cambios: Record<string, unknown> = {}) => ({
  alias: "Familia de 5º B", rol: "OTRO", mayorDe14: true, condiciones: "2026-11", ...cambios,
});

beforeAll(async () => {
  expect(PROYECTO).toBe("demo-valoralo");
  await crearCentro(CODIGO);
});

describe("crearPerfil", () => {
  test("crea el perfil con el alias normalizado y los campos del modelo", async () => {
    const u = await cuentaSinPerfil();
    const r = await llamar("crearPerfil", datosPerfil({ alias: "  Familia   de 5º B " }), u);
    expect(r.result).toEqual({ alias: "Familia de 5º B" });
    const p = (await db.doc(`usuarios/${u.uid}`).get()).data()!;
    expect(p).toMatchObject({ alias: "Familia de 5º B", rol: "OTRO", verificado: false, mayorDe14: true, condiciones: "2026-11" });
    expect(Object.keys(p).sort()).toEqual(["alias", "condiciones", "fechaAlta", "mayorDe14", "rol", "verificado"]);
    // A second call doesn't overwrite it.
    expect((await llamar("crearPerfil", datosPerfil({ alias: "Otro alias" }), u)).error?.status).toBe("ALREADY_EXISTS");
  });

  test.each([
    ["yo@correo.es", "solo puede llevar"], ["Docente verificado", "puede confundirse"], ["Moderador", "puede confundirse"],
    ["Valoralo oficial", "puede confundirse"], ["612345678", "teléfonos"], ["miweb.com", "enlaces"], ["Gilipollas", "insultos"],
  ])("rechaza el alias %s", async (alias, mensaje) => {
    const u = await cuentaSinPerfil();
    const r = await llamar("crearPerfil", datosPerfil({ alias }), u);
    expect(r.error?.status).toBe("INVALID_ARGUMENT");
    expect(r.error?.message).toContain(mensaje);
    expect(r.error?.details).toMatchObject({ paraUsuario: true, campo: "alias" });
    expect((await db.doc(`usuarios/${u.uid}`).get()).exists).toBe(false);
  });

  test("solo ESTUDIANTE u OTRO, mayores de 14 y con las condiciones vigentes", async () => {
    const u = await cuentaSinPerfil();
    expect((await llamar("crearPerfil", datosPerfil({ rol: "DOCENTE" }), u)).error?.status).toBe("INVALID_ARGUMENT");
    expect((await llamar("crearPerfil", datosPerfil({ mayorDe14: false }), u)).error?.status).toBe("FAILED_PRECONDITION");
    expect((await llamar("crearPerfil", datosPerfil({ condiciones: "2020-01" }), u)).error?.details).toMatchObject({ motivo: "CONDICIONES" });
    expect((await llamar("crearPerfil", datosPerfil())).error?.status).toBe("UNAUTHENTICATED");
  });
});

describe("cambiarAlias", () => {
  test("cambia un alias válido y rechaza los que no cumplen", async () => {
    const u = await crearUsuario("ESTUDIANTE", false, "Estudiante de 4º ESO");
    expect((await llamar("cambiarAlias", { alias: "Exalumna 2024" }, u)).result).toEqual({ alias: "Exalumna 2024" });
    expect((await db.doc(`usuarios/${u.uid}`).get()).get("alias")).toBe("Exalumna 2024");
    for (const alias of ["Docente verificado", "yo@correo.es", "Admin", "g1l1poll4s"]) {
      expect((await llamar("cambiarAlias", { alias }, u)).error?.status).toBe("INVALID_ARGUMENT");
    }
    expect((await db.doc(`usuarios/${u.uid}`).get()).get("alias")).toBe("Exalumna 2024");
  });

  test("sin perfil, pide completarlo", async () => {
    expect((await llamar("cambiarAlias", { alias: "Familia nueva" }, await cuentaSinPerfil())).error?.status).toBe("FAILED_PRECONDITION");
  });
});

describe("publicarValoracion con un alias antiguo que no cumple", () => {
  test("no se publica y se pide cambiar el alias", async () => {
    const u = await crearUsuario("ESTUDIANTE", false, "Docente verificado");
    const r = await llamar("publicarValoracion", { codigo: CODIGO, centro: { nota: 4 }, instalaciones: { nota: 4 }, profesorado: [], texto: "" }, u);
    expect(r.error?.status).toBe("FAILED_PRECONDITION");
    expect(r.error?.details).toMatchObject({ motivo: "ALIAS" });
  });
});

describe("límite de denuncias por hora (config/denuncias)", () => {
  test("con maxPorHora = 3, la cuarta denuncia seguida se rechaza y no se guarda", async () => {
    await db.doc("config/denuncias").set({ maxPorHora: 3 });
    try {
      const autores = await Promise.all([1, 2, 3, 4].map(() => crearUsuario()));
      for (const a of autores) {
        await llamar("publicarValoracion", { codigo: CODIGO, centro: { nota: 3 }, instalaciones: { nota: 3 }, profesorado: [], texto: "" }, a);
      }
      const denunciante = await crearUsuario();
      const denunciar = (a: Usuario) => llamar("denunciarValoracion", { codigo: CODIGO, autorUid: a.uid, motivo: "OTRO" }, denunciante);
      for (const a of autores.slice(0, 3)) expect((await denunciar(a)).result).toEqual({ denuncias: 1 });
      // A repeated report is answered as before and doesn't use up the counter.
      expect((await denunciar(autores[0])).error?.status).toBe("ALREADY_EXISTS");
      const cuarta = await denunciar(autores[3]);
      expect(cuarta.error?.status).toBe("RESOURCE_EXHAUSTED");
      expect(cuarta.error?.message).toContain("muchas denuncias");
      expect((await db.doc(`denuncias/${CODIGO}_${autores[3].uid}_${denunciante.uid}`).get()).exists).toBe(false);
      expect((await db.doc(`usuarios/${denunciante.uid}/limites/denuncias`).get()).get("n")).toBe(3);
    } finally {
      await db.doc("config/denuncias").delete();
    }
  });
});
