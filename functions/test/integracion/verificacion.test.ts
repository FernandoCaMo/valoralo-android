/**
 * Teacher verification end to end (doc 04) against the Auth, Firestore, Storage and
 * Functions emulators:  npm run test:integracion
 */
import { getAuth } from "firebase-admin/auth";
import { beforeAll, describe, expect, test } from "vitest";
import { caducarPendientes } from "../../src/verificacion/operaciones";
import { hashCsv } from "../../src/verificacion/logica";
import { bucket, crearAdmin, crearCentro, crearUsuario, db, esperar, llamar, stats, Usuario } from "./ayuda";

const CENTRO_A = "21000801";
const CENTRO_B = "21000802";
const nota = (n: number) => ({ centro: { nota: n }, instalaciones: { nota: n }, profesorado: [], texto: "" });
const solicitud = (csv: string, extra: Record<string, unknown> = {}) => ({ nombre: "Lucía Ruiz Pérez", csv, ...extra });
const verificacion = async (uid: string) => (await db.doc(`verificaciones/${uid}`).get()).data();
const privado = (uid: string) => db.doc(`verificaciones/${uid}/privado/solicitud`).get();

let admin: Usuario;
let csvN = 0;
const csvNuevo = () => `VH5DPNPY5AXNHX${Date.now()}${++csvN}`.slice(0, 40);

beforeAll(async () => {
  await crearCentro(CENTRO_A);
  await crearCentro(CENTRO_B);
  admin = await crearAdmin();
});

describe("solicitarVerificacion", () => {
  test("crea la solicitud: el usuario ve el estado y el hash; el nombre y el CSV quedan aparte", async () => {
    const u = await crearUsuario("OTRO");
    const csv = csvNuevo();
    expect((await llamar("solicitarVerificacion", solicitud(csv), u)).result).toEqual({ estado: "PENDIENTE" });
    const v = await verificacion(u.uid);
    expect(v).toMatchObject({ estado: "PENDIENTE", csvHash: hashCsv(csv), tienePdf: false, motivoRechazo: null });
    expect(JSON.stringify(v)).not.toContain(csv);
    expect((await privado(u.uid)).data()).toEqual({ nombreDeclarado: "Lucía Ruiz Pérez", csv, pdfPath: null });

    const otra = await llamar("solicitarVerificacion", solicitud(csvNuevo()), u);
    expect(otra.error?.status).toBe("ALREADY_EXISTS");
  });

  test("el mismo CSV pendiente en otra cuenta no se acepta", async () => {
    const csv = csvNuevo();
    await llamar("solicitarVerificacion", solicitud(csv), await crearUsuario());
    const r = await llamar("solicitarVerificacion", solicitud(csv), await crearUsuario());
    expect(r.error?.status).toBe("FAILED_PRECONDITION");
  });

  test("datos no válidos y PDF inexistente", async () => {
    const u = await crearUsuario();
    expect((await llamar("solicitarVerificacion", solicitud("123"), u)).error?.status).toBe("INVALID_ARGUMENT");
    expect((await llamar("solicitarVerificacion", { nombre: "Lucía", csv: csvNuevo() }, u)).error?.status).toBe("INVALID_ARGUMENT");
    const sinPdf = await llamar("solicitarVerificacion", solicitud(csvNuevo(), { pdfPath: `verificaciones/${u.uid}/no-existe.pdf` }), u);
    expect(sinPdf.error?.status).toBe("INVALID_ARGUMENT");
    expect((await llamar("solicitarVerificacion", solicitud(csvNuevo()))).error?.status).toBe("UNAUTHENTICATED");
  });
});

describe("resolverVerificacion", () => {
  test("solo administradores", async () => {
    const u = await crearUsuario();
    await llamar("solicitarVerificacion", solicitud(csvNuevo()), u);
    const r = await llamar("resolverVerificacion", { uid: u.uid, decision: "APROBAR" }, await crearUsuario());
    expect(r.error?.status).toBe("PERMISSION_DENIED");
    expect((await verificacion(u.uid))?.estado).toBe("PENDIENTE");
  });

  test("aprobar: DOCENTE verificado; sus reseñas 'Otro' pasan a DOCENTE y las de 'Estudiante' no", async () => {
    const u = await crearUsuario("ESTUDIANTE");
    await llamar("publicarValoracion", { codigo: CENTRO_A, ...nota(3) }, u);
    await db.doc(`usuarios/${u.uid}`).update({ rol: "OTRO" });
    await llamar("publicarValoracion", { codigo: CENTRO_B, ...nota(5) }, u);
    await esperar(() => stats(CENTRO_B), (s) => s?.otro?.n === 1);

    const csv = csvNuevo();
    await llamar("solicitarVerificacion", solicitud(csv), u);
    const r = await llamar("resolverVerificacion", { uid: u.uid, decision: "APROBAR", centroActualCodigo: CENTRO_B }, admin);
    expect(r.result).toEqual({ estado: "APROBADA" });

    expect((await db.doc(`usuarios/${u.uid}`).get()).data()).toMatchObject({ rol: "DOCENTE", verificado: true, centroActualCodigo: CENTRO_B });
    expect((await getAuth().getUser(u.uid)).customClaims).toMatchObject({ rol: "DOCENTE", verificado: true });
    expect(await verificacion(u.uid)).toMatchObject({ estado: "APROBADA", revisorUid: admin.uid });
    expect((await privado(u.uid)).exists).toBe(false);
    expect((await db.doc(`csvUsados/${hashCsv(csv)}`).get()).data()).toMatchObject({ uid: u.uid, resultado: "APROBADA" });

    const a = (await db.doc(`centros/${CENTRO_A}/valoraciones/${u.uid}`).get()).data();
    const b = (await db.doc(`centros/${CENTRO_B}/valoraciones/${u.uid}`).get()).data();
    expect(a).toMatchObject({ rolAutor: "ESTUDIANTE", verificado: false });
    expect(b).toMatchObject({ rolAutor: "DOCENTE", verificado: true });
    const s = await esperar(() => stats(CENTRO_B), (s) => s?.docente?.n === 1);
    expect(s.otro.n).toBe(0);

    // Already verified: can't request again; the approved CSV can't be reused by anyone.
    expect((await llamar("solicitarVerificacion", solicitud(csvNuevo()), u)).error?.status).toBe("FAILED_PRECONDITION");
    expect((await llamar("solicitarVerificacion", solicitud(csv), await crearUsuario())).error?.status).toBe("FAILED_PRECONDITION");
    // A second resolution of the same request is refused.
    expect((await llamar("resolverVerificacion", { uid: u.uid, decision: "RECHAZAR", motivo: "x x x" }, admin)).error?.status).toBe("FAILED_PRECONDITION");
  });

  test("rechazar: motivo visible, PDF borrado, CSV bloqueado; se puede pedir otra vez con otro documento", async () => {
    const u = await crearUsuario("OTRO");
    const pdf = `verificaciones/${u.uid}/hoja.pdf`;
    await bucket().file(pdf).save(Buffer.from("%PDF-1.4"), { contentType: "application/pdf" });
    const csv = csvNuevo();
    expect((await llamar("solicitarVerificacion", solicitud(csv, { pdfPath: pdf }), u)).result).toEqual({ estado: "PENDIENTE" });
    expect((await privado(u.uid)).get("pdfPath")).toBe(pdf);

    const sinMotivo = await llamar("resolverVerificacion", { uid: u.uid, decision: "RECHAZAR" }, admin);
    expect(sinMotivo.error?.status).toBe("INVALID_ARGUMENT");
    await llamar("resolverVerificacion", { uid: u.uid, decision: "RECHAZAR", motivo: "El nombre no coincide con el documento." }, admin);

    expect(await verificacion(u.uid)).toMatchObject({ estado: "RECHAZADA", motivoRechazo: "El nombre no coincide con el documento." });
    expect((await privado(u.uid)).exists).toBe(false);
    expect((await bucket().file(pdf).exists())[0]).toBe(false);
    expect((await db.doc(`usuarios/${u.uid}`).get()).get("rol")).toBe("OTRO");

    expect((await llamar("solicitarVerificacion", solicitud(csv), u)).error?.status).toBe("FAILED_PRECONDITION");
    expect((await llamar("solicitarVerificacion", solicitud(csvNuevo()), u)).result).toEqual({ estado: "PENDIENTE" });
  });

  test("como máximo 3 solicitudes en 30 días", async () => {
    const u = await crearUsuario();
    for (let i = 0; i < 3; i++) {
      expect((await llamar("solicitarVerificacion", solicitud(csvNuevo()), u)).result).toEqual({ estado: "PENDIENTE" });
      await llamar("resolverVerificacion", { uid: u.uid, decision: "RECHAZAR", motivo: "Documento ilegible." }, admin);
    }
    const cuarta = await llamar("solicitarVerificacion", solicitud(csvNuevo()), u);
    expect(cuarta.error?.status).toBe("RESOURCE_EXHAUSTED");
  });
});

describe("eliminar cuenta con verificación", () => {
  test("se borran la solicitud, sus datos privados y el PDF; la huella del CSV queda sin uid", async () => {
    const u = await crearUsuario("OTRO");
    // A rejected document (its hash stays in csvUsados) and then a pending request with a PDF.
    const rechazado = csvNuevo();
    await llamar("solicitarVerificacion", solicitud(rechazado), u);
    await llamar("resolverVerificacion", { uid: u.uid, decision: "RECHAZAR", motivo: "Documento ilegible." }, admin);
    expect((await db.doc(`csvUsados/${hashCsv(rechazado)}`).get()).get("uid")).toBe(u.uid);
    const pdf = `verificaciones/${u.uid}/hoja.pdf`;
    await bucket().file(pdf).save(Buffer.from("%PDF-1.4"), { contentType: "application/pdf" });
    expect((await llamar("solicitarVerificacion", solicitud(csvNuevo(), { pdfPath: pdf }), u)).result).toEqual({ estado: "PENDIENTE" });

    await db.doc(`usuarios/${u.uid}`).delete();

    await esperar(() => verificacion(u.uid), (v) => v === undefined);
    expect((await privado(u.uid)).exists).toBe(false);
    await esperar(async () => (await bucket().file(pdf).exists())[0], (e) => e === false);
    const huella = await esperar(async () => (await db.doc(`csvUsados/${hashCsv(rechazado)}`).get()).data(), (d) => d !== undefined && !("uid" in d));
    expect(huella).toMatchObject({ resultado: "RECHAZADA" });
  });
});

describe("caducarVerificaciones", () => {
  test("las pendientes de más de 30 días caducan y se borran sus datos y su PDF", async () => {
    const u = await crearUsuario();
    const pdf = `verificaciones/${u.uid}/hoja.pdf`;
    await bucket().file(pdf).save(Buffer.from("%PDF-1.4"), { contentType: "application/pdf" });
    await llamar("solicitarVerificacion", solicitud(csvNuevo(), { pdfPath: pdf }), u);

    expect(await caducarPendientes(db, bucket(), new Date(Date.now() + 29 * 86400000))).toBe(0);
    expect((await verificacion(u.uid))?.estado).toBe("PENDIENTE");

    const caducadas = await caducarPendientes(db, bucket(), new Date(Date.now() + 31 * 86400000));
    expect(caducadas).toBeGreaterThanOrEqual(1);
    expect((await verificacion(u.uid))?.estado).toBe("CADUCADA");
    expect((await privado(u.uid)).exists).toBe(false);
    expect((await bucket().file(pdf).exists())[0]).toBe(false);
  });
});
