/** Helpers for the integration tests (run inside `firebase emulators:exec`). */
import { getApps, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

export const PROYECTO = "demo-valoralo";
const REGION = "europe-southwest1";
const FUNCTIONS = `http://127.0.0.1:5001/${PROYECTO}/${REGION}`;
const AUTH = `http://${process.env.FIREBASE_AUTH_EMULATOR_HOST ?? "127.0.0.1:9099"}`;

if (getApps().length === 0) initializeApp({ projectId: PROYECTO });
export const db = getFirestore();

export interface Usuario {
  uid: string;
  token: string;
}

let contador = 0;
export async function crearUsuario(rol = "ESTUDIANTE", verificado = false, alias = "Usuaria Prueba"): Promise<Usuario> {
  contador += 1;
  const respuesta = await fetch(`${AUTH}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=demo`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: `u${Date.now()}_${contador}@example.com`, password: "valoralo2026", returnSecureToken: true }),
  });
  const datos = (await respuesta.json()) as { localId: string; idToken: string };
  await db.doc(`usuarios/${datos.localId}`).set({ alias, rol, verificado, mayorDe14: true, fechaAlta: new Date() });
  return { uid: datos.localId, token: datos.idToken };
}

export interface Respuesta<T = Record<string, unknown>> {
  result?: T;
  error?: { status: string; message: string; details?: Record<string, unknown> };
}

export async function llamar<T = Record<string, unknown>>(funcion: string, datos: unknown, usuario?: Usuario): Promise<Respuesta<T>> {
  const respuesta = await fetch(`${FUNCTIONS}/${funcion}`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...(usuario ? { Authorization: `Bearer ${usuario.token}` } : {}) },
    body: JSON.stringify({ data: datos }),
  });
  return (await respuesta.json()) as Respuesta<T>;
}

/** Waits until a condition holds (triggers run asynchronously). */
export async function esperar<T>(leer: () => Promise<T>, cumple: (v: T) => boolean, ms = 15000): Promise<T> {
  const limite = Date.now() + ms;
  let ultimo = await leer();
  while (!cumple(ultimo)) {
    if (Date.now() > limite) throw new Error(`Tiempo agotado; último valor: ${JSON.stringify(ultimo)}`);
    await new Promise((r) => setTimeout(r, 250));
    ultimo = await leer();
  }
  return ultimo;
}

export async function crearCentro(codigo: string): Promise<void> {
  await db.doc(`centros/${codigo}`).set({
    codigo, nombre: "IES Delgado Hernández", tipo: "IES", localidad: "Bollullos Par del Condado",
    municipio: "Bollullos Par del Condado", provincia: "Huelva", catalogoDoc: "huelva-1",
    departamentos: ["Matemáticas", "Inglés", "Informática y Comunicaciones"],
    stats: {},
  });
  await db.doc("catalogo/huelva-1").set({
    provincia: "Huelva", parte: 1, partes: 1,
    centros: [
      { codigo: "21000001", nombre: "CEIP Otro", nota: null, resenas: 0 },
      { codigo, nombre: "IES Delgado Hernández", nota: null, resenas: 0 },
    ],
  });
}

export const stats = async (codigo: string) => (await db.doc(`centros/${codigo}`).get()).get("stats");
export const entradaCatalogo = async (codigo: string) =>
  ((await db.doc("catalogo/huelva-1").get()).get("centros") as { codigo: string; nota: number | null; resenas: number }[])
    .find((c) => c.codigo === codigo);
