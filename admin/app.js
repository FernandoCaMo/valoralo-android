// Teacher verification panel (doc 04). Hosted on the valoralo-admin Hosting site.
// Access: Google sign-in + custom claim admin = true. Firestore/Storage rules and the
// resolverVerificacion function enforce the same check on the server.
import { initializeApp } from "https://www.gstatic.com/firebasejs/12.19.0/firebase-app.js";
import {
  connectAuthEmulator, getAuth, GoogleAuthProvider, onAuthStateChanged, signInWithPopup, signOut,
} from "https://www.gstatic.com/firebasejs/12.19.0/firebase-auth.js";
import {
  collection, connectFirestoreEmulator, doc, getDoc, getDocs, getFirestore, query, where,
} from "https://www.gstatic.com/firebasejs/12.19.0/firebase-firestore.js";
import { connectFunctionsEmulator, getFunctions, httpsCallable } from "https://www.gstatic.com/firebasejs/12.19.0/firebase-functions.js";
import { connectStorageEmulator, getDownloadURL, getStorage, ref } from "https://www.gstatic.com/firebasejs/12.19.0/firebase-storage.js";
import {
  comprobaciones, diasDesde, esAdmin, formatearFecha, URL_VERIFICACION_JUNTA, validarCentro, validarMotivo,
} from "./logica.js";

const $ = (id) => document.getElementById(id);
const mostrar = (id) => ["cargando", "acceso", "sin-acceso", "panel"].forEach((s) => { $(s).hidden = s !== id; });
const error = (texto) => { $("error").textContent = texto ?? ""; $("error").hidden = !texto; };

// Config of the project's web app, served by Firebase Hosting itself.
const config = await (await fetch("/__/firebase/init.json")).json();
const app = initializeApp(config);
const auth = getAuth(app);
const db = getFirestore(app);
const storage = getStorage(app);
const resolver = httpsCallable(getFunctions(app, "europe-southwest1"), "resolverVerificacion");

if (["localhost", "127.0.0.1"].includes(location.hostname)) {
  connectAuthEmulator(auth, "http://127.0.0.1:9099", { disableWarnings: true });
  connectFirestoreEmulator(db, "127.0.0.1", 8080);
  connectStorageEmulator(storage, "127.0.0.1", 9199);
  connectFunctionsEmulator(getFunctions(app, "europe-southwest1"), "127.0.0.1", 5001);
}

$("enlace-junta").href = URL_VERIFICACION_JUNTA;
$("entrar").addEventListener("click", () => signInWithPopup(auth, new GoogleAuthProvider()).catch((e) => error(e.message)));
$("salir").addEventListener("click", () => signOut(auth));
$("recargar").addEventListener("click", () => cargar());

onAuthStateChanged(auth, async (usuario) => {
  error(null);
  $("usuario").hidden = !usuario;
  $("correo").textContent = usuario?.email ?? "";
  if (!usuario) return mostrar("acceso");
  const { claims } = await usuario.getIdTokenResult(true);
  if (!esAdmin(claims)) return mostrar("sin-acceso");
  mostrar("panel");
  await cargar();
});

const aFecha = (t) => (t && typeof t.toDate === "function" ? t.toDate() : null);

async function cargar() {
  error(null);
  $("lista").replaceChildren();
  try {
    const pendientes = await getDocs(query(collection(db, "verificaciones"), where("estado", "==", "PENDIENTE")));
    const docs = pendientes.docs.sort((a, b) => (aFecha(a.get("fechaSolicitud")) ?? 0) - (aFecha(b.get("fechaSolicitud")) ?? 0));
    $("total").textContent = `(${docs.length})`;
    $("vacio").hidden = docs.length > 0;
    for (const v of docs) $("lista").append(await tarjeta(v));
  } catch (e) {
    error(`No se han podido cargar las solicitudes: ${e.message}`);
  }
}

async function tarjeta(v) {
  const uid = v.id;
  const [privado, perfil] = await Promise.all([
    getDoc(doc(db, `verificaciones/${uid}/privado/solicitud`)),
    getDoc(doc(db, `usuarios/${uid}`)),
  ]);
  const nodo = $("plantilla-solicitud").content.firstElementChild.cloneNode(true);
  const en = (clase) => nodo.querySelector(`.${clase}`);
  const fechaSolicitud = aFecha(v.get("fechaSolicitud"));
  const fechaAlta = aFecha(perfil.get("fechaAlta"));

  // Everything coming from users is set as text, never as HTML.
  en("alias").textContent = perfil.get("alias") ?? "(sin perfil)";
  en("antiguedad").textContent = fechaSolicitud ? `hace ${diasDesde(fechaSolicitud)} días` : "";
  en("nombre").textContent = privado.get("nombreDeclarado") ?? "—";
  en("csv").textContent = privado.get("csv") ?? "—";
  en("alta").textContent = formatearFecha(fechaAlta);
  en("fecha").textContent = formatearFecha(fechaSolicitud);
  en("uid").textContent = uid;
  for (const texto of comprobaciones(fechaAlta)) {
    const li = document.createElement("li");
    li.textContent = texto;
    en("comprobaciones").append(li);
  }
  en("copiar").addEventListener("click", () => navigator.clipboard.writeText(privado.get("csv") ?? ""));

  const pdfPath = privado.get("pdfPath");
  if (pdfPath) {
    const enlace = document.createElement("a");
    enlace.textContent = "Abrir el PDF";
    enlace.target = "_blank";
    enlace.rel = "noopener noreferrer";
    enlace.href = "#";
    enlace.addEventListener("click", async (e) => {
      e.preventDefault();
      window.open(await getDownloadURL(ref(storage, pdfPath)), "_blank", "noopener");
    });
    en("pdf").append(enlace);
  } else {
    en("pdf").textContent = "No adjuntó PDF";
  }

  const resultado = en("resultado");
  const decidir = async (datos, boton) => {
    resultado.hidden = true;
    nodo.querySelectorAll("button").forEach((b) => { b.disabled = true; });
    try {
      await resolver({ uid, ...datos });
      nodo.remove();
      const quedan = $("lista").children.length;
      $("total").textContent = `(${quedan})`;
      $("vacio").hidden = quedan > 0;
    } catch (e) {
      resultado.textContent = e.message;
      resultado.hidden = false;
      nodo.querySelectorAll("button").forEach((b) => { b.disabled = false; });
      boton.focus();
    }
  };
  en("aprobar").addEventListener("click", (e) => {
    const centro = en("centro").value.trim();
    const problema = validarCentro(centro);
    if (problema) { resultado.textContent = problema; resultado.hidden = false; return; }
    if (!confirm(`¿Aprobar a ${perfil.get("alias") ?? uid} como docente verificado?`)) return;
    decidir({ decision: "APROBAR", centroActualCodigo: centro || null }, e.currentTarget);
  });
  en("rechazar").addEventListener("click", (e) => {
    const motivo = en("motivo").value;
    const problema = validarMotivo(motivo);
    if (problema) { resultado.textContent = problema; resultado.hidden = false; return; }
    decidir({ decision: "RECHAZAR", motivo: motivo.trim() }, e.currentTarget);
  });
  return nodo;
}
