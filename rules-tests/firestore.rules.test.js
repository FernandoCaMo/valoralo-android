// Tests of firestore.rules. Run from rules-tests/:  npm test
// (starts the Firestore emulator, runs the tests and stops it).
import { after, before, beforeEach, describe, test } from "node:test";
import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  updateDoc,
  deleteDoc,
  serverTimestamp,
  where,
} from "firebase/firestore";

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-valoralo",
    firestore: {
      rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8"),
      host: "127.0.0.1",
      port: 8080,
    },
  });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  // Seed data bypassing the rules (as the backend would).
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "centros/21000693"), {
      nombre: "IES Delgado Hernández",
      stats: { total: { n: 0 } },
    });
    await setDoc(doc(db, "centros/21000693/valoraciones/autorA"), {
      uid: "autorA", rolAutor: "ESTUDIANTE", verificado: false, estado: "PUBLICADA",
    });
    await setDoc(doc(db, "centros/21000693/valoraciones/autorB"), {
      uid: "autorB", rolAutor: "OTRO", verificado: false, estado: "EN_REVISION",
    });
    await setDoc(doc(db, "config/departamentos"), { IES: ["Matemáticas"] });
    await setDoc(doc(db, "usuarios/autorA"), { alias: "A", rol: "ESTUDIANTE", verificado: false });
    await setDoc(doc(db, "verificaciones/autorA"), { estado: "PENDIENTE" });
  });
});

const anonimo = () => env.unauthenticatedContext().firestore();
const usuario = (uid = "autorA") => env.authenticatedContext(uid).firestore();

describe("centros", () => {
  test("cualquiera puede leer un centro, también sin cuenta", async () => {
    await assertSucceeds(getDoc(doc(anonimo(), "centros/21000693")));
    await assertSucceeds(getDocs(collection(anonimo(), "centros")));
  });

  test("nadie puede crear, editar ni borrar centros desde el cliente", async () => {
    const db = usuario();
    await assertFails(setDoc(doc(db, "centros/99999999"), { nombre: "Falso" }));
    await assertFails(updateDoc(doc(db, "centros/21000693"), { nombre: "Cambiado" }));
    await assertFails(deleteDoc(doc(db, "centros/21000693")));
  });

  test("nadie puede escribir stats desde el cliente", async () => {
    await assertFails(updateDoc(doc(usuario(), "centros/21000693"), { "stats.total.n": 999 }));
    await assertFails(updateDoc(doc(anonimo(), "centros/21000693"), { "stats.total.n": 999 }));
  });
});

describe("valoraciones", () => {
  test("se leen las publicadas", async () => {
    await assertSucceeds(getDoc(doc(anonimo(), "centros/21000693/valoraciones/autorA")));
    const publicadas = query(
      collection(anonimo(), "centros/21000693/valoraciones"),
      where("estado", "==", "PUBLICADA"),
    );
    await assertSucceeds(getDocs(publicadas));
  });

  test("no se leen las que no están publicadas, ni siquiera el autor", async () => {
    await assertFails(getDoc(doc(anonimo(), "centros/21000693/valoraciones/autorB")));
    await assertFails(getDoc(doc(usuario("autorB"), "centros/21000693/valoraciones/autorB")));
  });

  test("una consulta sin filtrar por estado se rechaza", async () => {
    await assertFails(getDocs(collection(anonimo(), "centros/21000693/valoraciones")));
  });

  test("por ahora el cliente no puede escribir valoraciones", async () => {
    const ref = doc(usuario("autorC"), "centros/21000693/valoraciones/autorC");
    await assertFails(setDoc(ref, { uid: "autorC", estado: "PUBLICADA", rolAutor: "OTRO" }));
  });
});

describe("catalogo", () => {
  test("lectura pública y sin escritura", async () => {
    await assertSucceeds(getDoc(doc(anonimo(), "catalogo/huelva-1")));
    await assertFails(setDoc(doc(usuario(), "catalogo/huelva-1"), { centros: [] }));
  });
});

describe("config", () => {
  test("lectura pública y sin escritura", async () => {
    await assertSucceeds(getDoc(doc(anonimo(), "config/departamentos")));
    await assertFails(setDoc(doc(usuario(), "config/departamentos"), { IES: [] }));
  });
});

describe("usuarios", () => {
  const perfil = (cambios = {}) => ({
    alias: "Lucía R.", rol: "ESTUDIANTE", verificado: false, mayorDe14: true,
    fechaAlta: serverTimestamp(), ...cambios,
  });

  test("cada usuario crea su propio documento como ESTUDIANTE u OTRO", async () => {
    await assertSucceeds(setDoc(doc(usuario("nuevo1"), "usuarios/nuevo1"), perfil()));
    await assertSucceeds(setDoc(doc(usuario("nuevo2"), "usuarios/nuevo2"), perfil({ rol: "OTRO" })));
  });

  test("no puede crear el documento de otro ni sin sesión", async () => {
    await assertFails(setDoc(doc(usuario("nuevo1"), "usuarios/otro"), perfil()));
    await assertFails(setDoc(doc(anonimo(), "usuarios/nuevo1"), perfil()));
  });

  test("al crearlo no puede ser DOCENTE, ni verificado, ni menor de 14", async () => {
    const db = usuario("nuevo1");
    await assertFails(setDoc(doc(db, "usuarios/nuevo1"), perfil({ rol: "DOCENTE" })));
    await assertFails(setDoc(doc(db, "usuarios/nuevo1"), perfil({ rol: "ADMIN" })));
    await assertFails(setDoc(doc(db, "usuarios/nuevo1"), perfil({ verificado: true })));
    await assertFails(setDoc(doc(db, "usuarios/nuevo1"), perfil({ mayorDe14: false })));
  });

  test("al crearlo los campos deben ser exactamente los del modelo", async () => {
    const db = usuario("nuevo1");
    await assertFails(setDoc(doc(db, "usuarios/nuevo1"), perfil({ fechaNacimiento: "2000-01-01" })));
    const { mayorDe14, ...sinMayorDe14 } = perfil();
    await assertFails(setDoc(doc(db, "usuarios/nuevo1"), sinMayorDe14));
    await assertFails(setDoc(doc(db, "usuarios/nuevo1"), perfil({ fechaAlta: new Date(2020, 0, 1) })));
  });

  test("alias de 3 a 30 caracteres", async () => {
    const db = usuario("nuevo1");
    await assertFails(setDoc(doc(db, "usuarios/nuevo1"), perfil({ alias: "Lu" })));
    await assertFails(setDoc(doc(db, "usuarios/nuevo1"), perfil({ alias: "x".repeat(31) })));
    await assertFails(setDoc(doc(db, "usuarios/nuevo1"), perfil({ alias: 42 })));
  });

  test("lee su propio documento pero no el de otros", async () => {
    await assertSucceeds(getDoc(doc(usuario("autorA"), "usuarios/autorA")));
    await assertFails(getDoc(doc(usuario("autorB"), "usuarios/autorA")));
    await assertFails(getDoc(doc(anonimo(), "usuarios/autorA")));
  });

  test("solo puede cambiar su alias", async () => {
    const ref = doc(usuario("autorA"), "usuarios/autorA");
    await assertSucceeds(updateDoc(ref, { alias: "Nuevo alias" }));
    await assertFails(updateDoc(ref, { alias: "No" }));
    await assertFails(updateDoc(doc(usuario("autorB"), "usuarios/autorA"), { alias: "Intruso" }));
  });

  test("nunca puede pasar a DOCENTE ni tocar verificado", async () => {
    const ref = doc(usuario("autorA"), "usuarios/autorA");
    await assertFails(updateDoc(ref, { rol: "DOCENTE" }));
    await assertFails(updateDoc(ref, { rol: "OTRO" }));
    await assertFails(updateDoc(ref, { verificado: true }));
    await assertFails(updateDoc(ref, { alias: "Cambio", verificado: true }));
    await assertFails(setDoc(ref, { alias: "A", rol: "DOCENTE", verificado: true, mayorDe14: true, fechaAlta: serverTimestamp() }));
  });

  test("puede borrar su propio documento (eliminar cuenta), no el de otros", async () => {
    await assertFails(deleteDoc(doc(usuario("autorB"), "usuarios/autorA")));
    await assertSucceeds(deleteDoc(doc(usuario("autorA"), "usuarios/autorA")));
  });
});

describe("todo lo demás está cerrado", () => {
  test("verificaciones y colecciones desconocidas no se leen ni se escriben", async () => {
    await assertFails(getDoc(doc(usuario("autorA"), "verificaciones/autorA")));
    await assertFails(setDoc(doc(usuario("autorA"), "verificaciones/autorA"), { estado: "APROBADA" }));
    await assertFails(getDoc(doc(usuario("autorA"), "usuarios/autorA/favoritos/21000693")));
    await assertFails(getDoc(doc(anonimo(), "loquesea/x")));
    await assertFails(setDoc(doc(usuario(), "denuncias/d1"), { motivo: "x" }));
  });
});
