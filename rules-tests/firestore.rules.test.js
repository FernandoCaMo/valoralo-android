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
  collectionGroup,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  updateDoc,
  deleteDoc,
  serverTimestamp,
  where,
  writeBatch,
  increment,
  Timestamp,
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
    await setDoc(doc(db, "usuarios/autorA"), { alias: "Autor A", rol: "ESTUDIANTE", verificado: false });
    await setDoc(doc(db, "verificaciones/autorA"), { estado: "PENDIENTE" });
  });
});

const anonimo = () => env.unauthenticatedContext().firestore();
const usuario = (uid = "autorA") => env.authenticatedContext(uid).firestore();
const adminCtx = () => env.authenticatedContext("admin1", { admin: true }).firestore();

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

  test("las que están en revisión solo las lee su autor", async () => {
    await assertFails(getDoc(doc(anonimo(), "centros/21000693/valoraciones/autorB")));
    await assertFails(getDoc(doc(usuario("autorA"), "centros/21000693/valoraciones/autorB")));
    await assertSucceeds(getDoc(doc(usuario("autorB"), "centros/21000693/valoraciones/autorB")));
  });

  test("una consulta sin filtrar por estado se rechaza", async () => {
    await assertFails(getDocs(collection(anonimo(), "centros/21000693/valoraciones")));
  });

  test("el cliente no crea ni edita valoraciones (lo hace publicarValoracion)", async () => {
    const nueva = doc(usuario("autorC"), "centros/21000693/valoraciones/autorC");
    await assertFails(setDoc(nueva, { uid: "autorC", estado: "PUBLICADA", rolAutor: "DOCENTE" }));
    const propia = doc(usuario("autorA"), "centros/21000693/valoraciones/autorA");
    await assertFails(updateDoc(propia, { rolAutor: "DOCENTE" }));
    await assertFails(updateDoc(doc(usuario("autorB"), "centros/21000693/valoraciones/autorB"), { estado: "PUBLICADA" }));
  });

  test("los administradores leen las de cualquier estado de todos los centros (panel)", async () => {
    const admin = env.authenticatedContext("admin1", { admin: true }).firestore();
    await assertSucceeds(getDoc(doc(admin, "centros/21000693/valoraciones/autorB")));
    await assertSucceeds(getDocs(query(collectionGroup(admin, "valoraciones"), where("estado", "==", "EN_REVISION"))));
    await assertFails(getDocs(query(collectionGroup(usuario("autorA"), "valoraciones"), where("estado", "==", "EN_REVISION"))));
    await assertFails(updateDoc(doc(admin, "centros/21000693/valoraciones/autorB"), { estado: "PUBLICADA" }));
  });

  test("el autor puede borrar la suya y nadie más", async () => {
    await assertFails(deleteDoc(doc(usuario("autorB"), "centros/21000693/valoraciones/autorA")));
    await assertFails(deleteDoc(doc(anonimo(), "centros/21000693/valoraciones/autorA")));
    await assertSucceeds(deleteDoc(doc(usuario("autorA"), "centros/21000693/valoraciones/autorA")));
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
    await assertFails(setDoc(doc(adminCtx(), "config/destacadas"), { minCaracteres: 10 }));
  });
});

describe("destacadas", () => {
  test("lectura pública; solo las escribe el backend", async () => {
    await assertSucceeds(getDoc(doc(anonimo(), "destacadas/actual")));
    await assertFails(setDoc(doc(usuario(), "destacadas/actual"), { resenas: [] }));
    await assertFails(setDoc(doc(adminCtx(), "destacadas/actual"), { resenas: [] }));
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

  test("puede cambiar entre ESTUDIANTE y OTRO", async () => {
    const ref = doc(usuario("autorA"), "usuarios/autorA");
    await assertSucceeds(updateDoc(ref, { rol: "OTRO" }));
    await assertSucceeds(updateDoc(ref, { rol: "ESTUDIANTE", alias: "Otro alias" }));
  });

  test("un docente verificado no cambia su rol desde la app", async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "usuarios/docente1"),
        { alias: "Profe", rol: "DOCENTE", verificado: true, mayorDe14: true, fechaAlta: new Date() });
    });
    const ref = doc(usuario("docente1"), "usuarios/docente1");
    await assertFails(updateDoc(ref, { rol: "OTRO" }));
    await assertSucceeds(updateDoc(ref, { alias: "Profe Nuevo" }));
  });

  test("nunca puede pasar a DOCENTE ni tocar verificado", async () => {
    const ref = doc(usuario("autorA"), "usuarios/autorA");
    await assertFails(updateDoc(ref, { rol: "DOCENTE" }));
    await assertFails(updateDoc(ref, { rol: "ADMIN" }));
    await assertFails(updateDoc(ref, { verificado: true }));
    await assertFails(updateDoc(ref, { alias: "Cambio", verificado: true }));
    await assertFails(setDoc(ref, { alias: "A", rol: "DOCENTE", verificado: true, mayorDe14: true, fechaAlta: serverTimestamp() }));
  });

  test("puede borrar su propio documento (eliminar cuenta), no el de otros", async () => {
    await assertFails(deleteDoc(doc(usuario("autorB"), "usuarios/autorA")));
    await assertSucceeds(deleteDoc(doc(usuario("autorA"), "usuarios/autorA")));
  });
});

describe("verificaciones de docentes", () => {
  const admin = () => env.authenticatedContext("admin1", { admin: true }).firestore();

  beforeEach(async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await setDoc(doc(db, "verificaciones/autorA"), { estado: "PENDIENTE", csvHash: "abc" });
      await setDoc(doc(db, "verificaciones/autorA/privado/solicitud"), { nombreDeclarado: "Lucía Ruiz", csv: "VH5DPX" });
      await setDoc(doc(db, "csvUsados/abc"), { uid: "autorA", resultado: "RECHAZADA" });
    });
  });

  test("el usuario lee su propia solicitud y nadie más (salvo administradores)", async () => {
    await assertSucceeds(getDoc(doc(usuario("autorA"), "verificaciones/autorA")));
    await assertFails(getDoc(doc(usuario("autorB"), "verificaciones/autorA")));
    await assertFails(getDoc(doc(anonimo(), "verificaciones/autorA")));
    await assertSucceeds(getDoc(doc(admin(), "verificaciones/autorA")));
  });

  test("el nombre declarado y el CSV en claro solo los leen administradores, ni siquiera el propio usuario", async () => {
    await assertFails(getDoc(doc(usuario("autorA"), "verificaciones/autorA/privado/solicitud")));
    await assertSucceeds(getDoc(doc(admin(), "verificaciones/autorA/privado/solicitud")));
  });

  test("una marca admin falsa o ausente no da acceso", async () => {
    const noAdmin = env.authenticatedContext("x", { admin: "true" }).firestore();
    await assertFails(getDoc(doc(noAdmin, "verificaciones/autorA/privado/solicitud")));
  });

  test("nadie escribe verificaciones desde el cliente, tampoco un administrador", async () => {
    await assertFails(setDoc(doc(usuario("autorA"), "verificaciones/autorA"), { estado: "APROBADA" }));
    await assertFails(updateDoc(doc(admin(), "verificaciones/autorA"), { estado: "APROBADA" }));
    await assertFails(setDoc(doc(usuario("autorA"), "verificaciones/autorA/privado/solicitud"), { csv: "X" }));
  });

  test("csvUsados está cerrado", async () => {
    await assertFails(getDoc(doc(usuario("autorA"), "csvUsados/abc")));
    await assertFails(getDoc(doc(admin(), "csvUsados/abc")));
    await assertFails(setDoc(doc(usuario("autorA"), "csvUsados/xyz"), { uid: "autorA" }));
  });

  test("los administradores leen perfiles para revisar (alias, fecha de alta)", async () => {
    await assertSucceeds(getDoc(doc(admin(), "usuarios/autorA")));
    await assertFails(updateDoc(doc(admin(), "usuarios/autorA"), { rol: "DOCENTE" }));
  });
});

describe("favoritos", () => {
  const fav = (uid, codigo) => doc(usuario(uid), `usuarios/${uid}/favoritos/${codigo}`);

  test("cada usuario guarda, lee y quita los suyos", async () => {
    await assertSucceeds(setDoc(fav("autorA", "21000693"), { codigo: "21000693", fecha: serverTimestamp() }));
    await assertSucceeds(getDoc(fav("autorA", "21000693")));
    await assertSucceeds(getDocs(collection(usuario("autorA"), "usuarios/autorA/favoritos")));
    await assertSucceeds(deleteDoc(fav("autorA", "21000693")));
  });

  test("nadie lee ni toca los favoritos de otro, ni sin sesión", async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "usuarios/autorA/favoritos/21000693"), { codigo: "21000693", fecha: new Date() });
    });
    await assertFails(getDoc(doc(usuario("autorB"), "usuarios/autorA/favoritos/21000693")));
    await assertFails(getDocs(collection(usuario("autorB"), "usuarios/autorA/favoritos")));
    await assertFails(deleteDoc(doc(usuario("autorB"), "usuarios/autorA/favoritos/21000693")));
    await assertFails(setDoc(doc(anonimo(), "usuarios/autorA/favoritos/21000001"), { codigo: "21000001", fecha: serverTimestamp() }));
  });

  test("el documento es exactamente { codigo, fecha } con el código como id", async () => {
    await assertFails(setDoc(fav("autorA", "21000693"), { codigo: "21000001", fecha: serverTimestamp() }));
    await assertFails(setDoc(fav("autorA", "123"), { codigo: "123", fecha: serverTimestamp() }));
    await assertFails(setDoc(fav("autorA", "21000693"), { codigo: "21000693", fecha: serverTimestamp(), nota: 5 }));
    await assertFails(setDoc(fav("autorA", "21000693"), { codigo: "21000693", fecha: new Date(2020, 0, 1) }));
  });
});

describe("todo lo demás está cerrado", () => {
  test("colecciones desconocidas no se leen ni se escriben", async () => {
    await assertFails(getDoc(doc(anonimo(), "loquesea/x")));
  });

  test("denuncias: solo las escriben las funciones; las leen solo administradores", async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "denuncias/21000693_autorA_autorB"), { motivo: "INSULTOS", estado: "PENDIENTE" });
    });
    const admin = env.authenticatedContext("admin1", { admin: true }).firestore();
    await assertFails(setDoc(doc(usuario("autorB"), "denuncias/21000693_autorA_autorC"), { motivo: "INSULTOS" }));
    await assertFails(getDoc(doc(usuario("autorB"), "denuncias/21000693_autorA_autorB")));
    await assertFails(getDocs(collection(usuario("autorA"), "denuncias")));
    await assertSucceeds(getDocs(query(collection(admin, "denuncias"), where("estado", "==", "PENDIENTE"))));
    await assertFails(updateDoc(doc(admin, "denuncias/21000693_autorA_autorB"), { estado: "RESUELTA" }));
  });
});

describe("likes", () => {
  const RESENA = "centros/21000693/valoraciones/autorA";
  const like = (db, uid, resena = RESENA, extra = {}) => doc(db, `${resena}/likes/${uid}`);
  const datosLike = (uid, extra = {}) => ({ uid, codigo: "21000693", fecha: serverTimestamp(), ...extra });
  const limite = (db, uid) => doc(db, `usuarios/${uid}/limites/likes`);

  /** Like as the app does it: the like and the hourly counter in one batch. */
  async function darLike(uid, { resena = RESENA, contador = "nuevo", datos } = {}) {
    const db = usuario(uid);
    const lote = writeBatch(db);
    lote.set(like(db, uid, resena), datos ?? datosLike(uid));
    if (contador === "nuevo") lote.set(limite(db, uid), { inicio: serverTimestamp(), n: 1 });
    if (contador === "mas1") lote.update(limite(db, uid), { n: increment(1) });
    return lote.commit();
  }

  async function sembrarLimite(uid, n, haceMs) {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), `usuarios/${uid}/limites/likes`), {
        inicio: Timestamp.fromMillis(Date.now() - haceMs), n,
      });
    });
  }

  test("un usuario con cuenta da like a una reseña publicada de otro", async () => {
    await assertSucceeds(darLike("lector1"));
    await assertSucceeds(getDoc(like(usuario("lector1"), "lector1")));
  });

  test("sin sesión no se puede", async () => {
    await assertFails(setDoc(like(anonimo(), "lector1"), datosLike("lector1")));
  });

  test("no se puede dar like a la reseña propia", async () => {
    await assertFails(darLike("autorA"));
  });

  test("un like por usuario y reseña", async () => {
    await assertSucceeds(darLike("lector1"));
    await assertFails(darLike("lector1", { contador: "mas1" }));
  });

  test("solo en reseñas publicadas", async () => {
    await assertFails(darLike("lector1", { resena: "centros/21000693/valoraciones/autorB" }));
  });

  test("no a nombre de otro, sin campos de más y con la hora del servidor", async () => {
    await assertFails(darLike("lector1", { datos: datosLike("lector2") }));
    await assertFails(darLike("lector1", { datos: datosLike("lector1", { nLikes: 5 }) }));
    await assertFails(darLike("lector1", { datos: datosLike("lector1", { codigo: "99999999" }) }));
    await assertFails(darLike("lector1", { datos: datosLike("lector1", { fecha: Timestamp.fromMillis(0) }) }));
  });

  test("el like tiene que ir con su contador por hora", async () => {
    await assertFails(setDoc(like(usuario("lector1"), "lector1"), datosLike("lector1")));
  });

  test("como mucho 60 por hora; pasada la hora el contador vuelve a empezar", async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "centros/21000693/valoraciones/autorC"), { uid: "autorC", estado: "PUBLICADA" });
    });
    await sembrarLimite("lector1", 59, 10 * 60 * 1000);
    await assertSucceeds(darLike("lector1", { contador: "mas1" }));
    // 61st within the hour: rejected, also restarting the counter early.
    await assertFails(darLike("lector1", { resena: "centros/21000693/valoraciones/autorC", contador: "mas1" }));
    await assertFails(darLike("lector1", { resena: "centros/21000693/valoraciones/autorC", contador: "nuevo" }));
    // An hour later it starts again at 1.
    await sembrarLimite("lector1", 60, 61 * 60 * 1000);
    await assertSucceeds(darLike("lector1", { resena: "centros/21000693/valoraciones/autorC", contador: "nuevo" }));
  });

  test("el límite se lee de config/likes", async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "config/likes"), { maxPorHora: 1 });
    });
    await sembrarLimite("lector1", 1, 60 * 1000);
    await assertFails(darLike("lector1", { contador: "mas1" }));
  });

  test("el contador no se puede borrar ni reiniciar antes de tiempo", async () => {
    await sembrarLimite("lector1", 30, 60 * 1000);
    await assertFails(deleteDoc(limite(usuario("lector1"), "lector1")));
    await assertFails(setDoc(limite(usuario("lector1"), "lector1"), { inicio: serverTimestamp(), n: 1 }));
    await assertFails(getDoc(limite(usuario("lector2"), "lector1")));
  });

  test("el contador nLikes de la reseña está protegido", async () => {
    await assertFails(updateDoc(doc(usuario("lector1"), RESENA), { nLikes: 100 }));
    await assertFails(updateDoc(doc(usuario("autorA"), RESENA), { nLikes: 100 }));
  });

  test("solo el dueño quita o lee su like, también si la reseña ya no está publicada", async () => {
    await assertSucceeds(darLike("lector1"));
    await assertFails(deleteDoc(like(usuario("lector2"), "lector1")));
    await assertFails(getDoc(like(usuario("lector2"), "lector1")));
    await assertFails(getDoc(like(usuario("autorA"), "lector1")));
    await env.withSecurityRulesDisabled(async (ctx) => {
      await updateDoc(doc(ctx.firestore(), RESENA), { estado: "OCULTA" });
    });
    await assertSucceeds(deleteDoc(like(usuario("lector1"), "lector1")));
  });

  test("consulta de mis likes en un centro: solo los propios", async () => {
    await assertSucceeds(darLike("lector1"));
    const db = usuario("lector1");
    await assertSucceeds(getDocs(query(collectionGroup(db, "likes"), where("uid", "==", "lector1"), where("codigo", "==", "21000693"))));
    await assertFails(getDocs(query(collectionGroup(db, "likes"), where("codigo", "==", "21000693"))));
  });
});