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

describe("todo lo demás está cerrado", () => {
  test("usuarios: ni el propio usuario puede escribir su rol o verificado", async () => {
    await assertFails(updateDoc(doc(usuario("autorA"), "usuarios/autorA"), { rol: "DOCENTE" }));
    await assertFails(updateDoc(doc(usuario("autorA"), "usuarios/autorA"), { verificado: true }));
    await assertFails(setDoc(doc(usuario("autorZ"), "usuarios/autorZ"), { rol: "DOCENTE", verificado: true }));
  });

  test("usuarios, verificaciones y colecciones desconocidas no se leen", async () => {
    await assertFails(getDoc(doc(usuario("autorA"), "usuarios/autorA")));
    await assertFails(getDoc(doc(usuario("autorA"), "verificaciones/autorA")));
    await assertFails(getDoc(doc(anonimo(), "loquesea/x")));
    await assertFails(setDoc(doc(usuario(), "denuncias/d1"), { motivo: "x" }));
  });
});
