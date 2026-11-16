// Tests of storage.rules (PDF of the teacher verification). Run from rules-tests/: npm test
import { after, before, beforeEach, describe, test } from "node:test";
import { readFileSync } from "node:fs";
import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import { deleteObject, getBytes, ref, uploadBytes } from "firebase/storage";

let env;
const PDF = new Uint8Array([0x25, 0x50, 0x44, 0x46, 0x2d]); // "%PDF-"
const pdf = { contentType: "application/pdf" };

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-valoralo",
    storage: {
      rules: readFileSync(new URL("../storage.rules", import.meta.url), "utf8"),
      host: "127.0.0.1",
      port: 9199,
    },
  });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearStorage();
  await env.withSecurityRulesDisabled(async (ctx) => {
    await uploadBytes(ref(ctx.storage(), "verificaciones/autorA/hoja.pdf"), PDF, pdf);
  });
});

const usuario = (uid) => env.authenticatedContext(uid).storage();
const admin = () => env.authenticatedContext("admin1", { admin: true }).storage();

describe("PDF de verificación", () => {
  test("el usuario sube su propio PDF", async () => {
    await assertSucceeds(uploadBytes(ref(usuario("autorB"), "verificaciones/autorB/hoja-servicios.pdf"), PDF, pdf));
  });

  test("no puede subirlo a la carpeta de otro ni sin sesión", async () => {
    await assertFails(uploadBytes(ref(usuario("autorB"), "verificaciones/autorA/x.pdf"), PDF, pdf));
    await assertFails(uploadBytes(ref(env.unauthenticatedContext().storage(), "verificaciones/autorB/x.pdf"), PDF, pdf));
  });

  test("solo PDF, de menos de 5 MB y con un nombre sencillo", async () => {
    const r = (nombre) => ref(usuario("autorB"), `verificaciones/autorB/${nombre}`);
    await assertFails(uploadBytes(r("foto.jpg"), PDF, { contentType: "image/jpeg" }));
    await assertFails(uploadBytes(r("hoja.pdf"), PDF, { contentType: "text/html" }));
    await assertFails(uploadBytes(r("hoja.pdf"), new Uint8Array(5 * 1024 * 1024), pdf));
    await assertFails(uploadBytes(r("../../otro.pdf"), PDF, pdf));
  });

  test("no puede sobrescribir un PDF ya subido", async () => {
    await assertFails(uploadBytes(ref(usuario("autorA"), "verificaciones/autorA/hoja.pdf"), PDF, pdf));
  });

  test("solo los administradores lo leen y lo borran; el propio usuario tampoco lo lee", async () => {
    await assertFails(getBytes(ref(usuario("autorA"), "verificaciones/autorA/hoja.pdf")));
    await assertFails(deleteObject(ref(usuario("autorA"), "verificaciones/autorA/hoja.pdf")));
    await assertSucceeds(getBytes(ref(admin(), "verificaciones/autorA/hoja.pdf")));
    await assertSucceeds(deleteObject(ref(admin(), "verificaciones/autorA/hoja.pdf")));
  });

  test("el resto del bucket está cerrado", async () => {
    await assertFails(uploadBytes(ref(usuario("autorA"), "otra/cosa.pdf"), PDF, pdf));
    await assertFails(getBytes(ref(admin(), "otra/cosa.pdf")));
  });
});
