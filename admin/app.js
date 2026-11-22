// Administration panel: teacher verification (doc 04) and moderation of reviews (Fase 6).
// Hosted on the valoralo-admin Hosting site. Access: Google sign-in + custom claim admin = true;
// the Firestore/Storage rules and the functions enforce the same check on the server.
import { initializeApp } from "https://www.gstatic.com/firebasejs/12.19.0/firebase-app.js";
import { initializeAppCheck, ReCaptchaEnterpriseProvider } from "https://www.gstatic.com/firebasejs/12.19.0/firebase-app-check.js";
import {
  connectAuthEmulator, getAuth, GoogleAuthProvider, onAuthStateChanged, signInWithPopup, signOut,
} from "https://www.gstatic.com/firebasejs/12.19.0/firebase-auth.js";
import {
  collection, collectionGroup, connectFirestoreEmulator, doc, getDoc, getDocs, getFirestore, query, where,
} from "https://www.gstatic.com/firebasejs/12.19.0/firebase-firestore.js";
import { connectFunctionsEmulator, getFunctions, httpsCallable } from "https://www.gstatic.com/firebasejs/12.19.0/firebase-functions.js";
import { connectStorageEmulator, getDownloadURL, getStorage, ref } from "https://www.gstatic.com/firebasejs/12.19.0/firebase-storage.js";
import {
  agruparDenuncias, CLAVE_RECAPTCHA, comprobaciones, usarAppCheck, textoAntiguedad, esAdmin, formatearFecha, MOTIVO_DENUNCIA, MOTIVOS_OCULTAR, ORIGEN_REVISION,
  segmentos, textoDestacadas, URL_VERIFICACION_JUNTA, validarCentro, validarMotivo, validarNota,
} from "./logica.js";

const $ = (id) => document.getElementById(id);
const mostrar = (id) => ["cargando", "acceso", "sin-acceso", "panel"].forEach((s) => { $(s).hidden = s !== id; });
const error = (texto) => { $("error").textContent = texto ?? ""; $("error").hidden = !texto; };
const aFecha = (t) => (t && typeof t.toDate === "function" ? t.toDate() : null);

// Config of the project's web app, served by Firebase Hosting itself.
const config = await (await fetch("/__/firebase/init.json")).json();
const app = initializeApp(config);
// App Check (reCAPTCHA Enterprise) before any other Firebase call, so every request carries its token.
if (usarAppCheck(CLAVE_RECAPTCHA, location.hostname)) {
  initializeAppCheck(app, { provider: new ReCaptchaEnterpriseProvider(CLAVE_RECAPTCHA), isTokenAutoRefreshEnabled: true });
}
const auth = getAuth(app);
const db = getFirestore(app);
const storage = getStorage(app);
const funciones = getFunctions(app, "europe-southwest1");
const resolverVerificacion = httpsCallable(funciones, "resolverVerificacion");
const moderarValoracion = httpsCallable(funciones, "moderarValoracion");
const recalcularDestacadas = httpsCallable(funciones, "recalcularDestacadas");

if (["localhost", "127.0.0.1"].includes(location.hostname)) {
  connectAuthEmulator(auth, "http://127.0.0.1:9099", { disableWarnings: true });
  connectFirestoreEmulator(db, "127.0.0.1", 8080);
  connectStorageEmulator(storage, "127.0.0.1", 9199);
  connectFunctionsEmulator(funciones, "127.0.0.1", 5001);
}

// ---------- Session and tabs ----------

$("enlace-junta").href = URL_VERIFICACION_JUNTA;
$("entrar").addEventListener("click", () => signInWithPopup(auth, new GoogleAuthProvider()).catch((e) => error(e.message)));
$("salir").addEventListener("click", () => signOut(auth));
$("recargar").addEventListener("click", () => cargarTodo());
$("recalcular-destacadas").addEventListener("click", async () => {
  const boton = $("recalcular-destacadas");
  const salida = $("resultado-destacadas");
  boton.disabled = true;
  salida.hidden = false;
  salida.textContent = "Recalculando…";
  try {
    const { data } = await recalcularDestacadas({});
    salida.textContent = textoDestacadas(data.n, data.config);
  } catch (e) {
    salida.textContent = `No se han podido recalcular: ${e.message}`;
  } finally {
    boton.disabled = false;
  }
});

for (const boton of document.querySelectorAll("[data-pestana]")) {
  boton.addEventListener("click", () => {
    for (const b of document.querySelectorAll("[data-pestana]")) {
      const activa = b === boton;
      b.setAttribute("aria-selected", String(activa));
      $(`pestana-${b.dataset.pestana}`).hidden = !activa;
    }
  });
}

onAuthStateChanged(auth, async (usuario) => {
  error(null);
  $("usuario").hidden = !usuario;
  $("correo").textContent = usuario?.email ?? "";
  if (!usuario) return mostrar("acceso");
  const { claims } = await usuario.getIdTokenResult(true);
  if (!esAdmin(claims)) return mostrar("sin-acceso");
  mostrar("panel");
  await cargarTodo();
});

async function cargarTodo() {
  error(null);
  await Promise.all([cargarVerificaciones(), cargarRevision(), cargarDenuncias()]);
}

/** Fills a tab's list and its counter. */
function pintarLista(pestana, nodos) {
  const panel = $(`pestana-${pestana}`);
  panel.querySelector(".lista").replaceChildren(...nodos);
  panel.querySelector(".vacio").hidden = nodos.length > 0;
  $(`n-${pestana}`).textContent = `(${nodos.length})`;
}

function quitarDeLista(pestana, nodo) {
  nodo.remove();
  const quedan = $(`pestana-${pestana}`).querySelector(".lista").children.length;
  $(`pestana-${pestana}`).querySelector(".vacio").hidden = quedan > 0;
  $(`n-${pestana}`).textContent = `(${quedan})`;
}

/** Runs a callable from a card: disables its buttons, shows the error inside the card. */
async function ejecutar(nodo, pestana, llamada) {
  const resultado = nodo.querySelector(".resultado");
  resultado.hidden = true;
  nodo.querySelectorAll("button").forEach((b) => { b.disabled = true; });
  try {
    await llamada();
    quitarDeLista(pestana, nodo);
  } catch (e) {
    resultado.textContent = e.message;
    resultado.hidden = false;
    nodo.querySelectorAll("button").forEach((b) => { b.disabled = false; });
  }
}

const avisoEnTarjeta = (nodo, texto) => {
  const r = nodo.querySelector(".resultado");
  r.textContent = texto;
  r.hidden = false;
};

// ---------- Teacher verification ----------

async function cargarVerificaciones() {
  try {
    const pendientes = await getDocs(query(collection(db, "verificaciones"), where("estado", "==", "PENDIENTE")));
    const docs = pendientes.docs.sort((a, b) => (aFecha(a.get("fechaSolicitud")) ?? 0) - (aFecha(b.get("fechaSolicitud")) ?? 0));
    pintarLista("verificaciones", await Promise.all(docs.map(tarjetaSolicitud)));
  } catch (e) {
    error(`No se han podido cargar las solicitudes: ${e.message}`);
  }
}

async function tarjetaSolicitud(v) {
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
  en("antiguedad").textContent = fechaSolicitud ? textoAntiguedad(fechaSolicitud) : "";
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
    enlace.href = "#";
    enlace.addEventListener("click", async (e) => {
      e.preventDefault();
      window.open(await getDownloadURL(ref(storage, pdfPath)), "_blank", "noopener");
    });
    en("pdf").append(enlace);
  } else {
    en("pdf").textContent = "No adjuntó PDF";
  }

  en("aprobar").addEventListener("click", () => {
    const centro = en("centro").value.trim();
    const problema = validarCentro(centro);
    if (problema) return avisoEnTarjeta(nodo, problema);
    if (!confirm(`¿Aprobar a ${perfil.get("alias") ?? uid} como docente verificado?`)) return;
    ejecutar(nodo, "verificaciones", () => resolverVerificacion({ uid, decision: "APROBAR", centroActualCodigo: centro || null }));
  });
  en("rechazar").addEventListener("click", () => {
    const motivo = en("motivo").value;
    const problema = validarMotivo(motivo);
    if (problema) return avisoEnTarjeta(nodo, problema);
    ejecutar(nodo, "verificaciones", () => resolverVerificacion({ uid, decision: "RECHAZAR", motivo: motivo.trim() }));
  });
  return nodo;
}

// ---------- Moderation of reviews ----------

const nombresCentro = new Map();
async function nombreCentro(codigo) {
  if (!nombresCentro.has(codigo)) nombresCentro.set(codigo, (await getDoc(doc(db, `centros/${codigo}`))).get("nombre") ?? codigo);
  return nombresCentro.get(codigo);
}

async function denunciasPendientes(codigo, uid) {
  const q = query(collection(db, "denuncias"),
    where("centro", "==", codigo), where("valoracionUid", "==", uid), where("estado", "==", "PENDIENTE"));
  return (await getDocs(q)).docs.map((d) => d.data());
}

async function cargarRevision() {
  try {
    const enRevision = await getDocs(query(collectionGroup(db, "valoraciones"), where("estado", "==", "EN_REVISION")));
    const nodos = await Promise.all(enRevision.docs.map(async (v) => {
      const codigo = v.ref.parent.parent.id;
      return tarjetaResena(codigo, v.id, v.data(), await denunciasPendientes(codigo, v.id), "revision");
    }));
    pintarLista("revision", nodos);
  } catch (e) {
    error(`No se han podido cargar las reseñas en revisión: ${e.message}`);
  }
}

async function cargarDenuncias() {
  try {
    const pendientes = (await getDocs(query(collection(db, "denuncias"), where("estado", "==", "PENDIENTE")))).docs.map((d) => d.data());
    const nodos = [];
    for (const g of agruparDenuncias(pendientes)) {
      const v = await getDoc(doc(db, `centros/${g.centro}/valoraciones/${g.uid}`));
      // Reviews already under review appear in the other tab.
      if (v.exists() && v.get("estado") === "PUBLICADA") {
        nodos.push(await tarjetaResena(g.centro, g.uid, v.data(), pendientes.filter((d) => d.centro === g.centro && d.valoracionUid === g.uid), "denuncias"));
      }
    }
    pintarLista("denuncias", nodos);
  } catch (e) {
    error(`No se han podido cargar las denuncias: ${e.message}`);
  }
}

async function tarjetaResena(codigo, uid, v, denuncias, pestana) {
  const nodo = $("plantilla-resena").content.firstElementChild.cloneNode(true);
  const en = (clase) => nodo.querySelector(`.${clase}`);
  const fecha = aFecha(v.actualizada) ?? aFecha(v.fecha);

  en("centro-nombre").textContent = `${await nombreCentro(codigo)} (${codigo})`;
  en("antiguedad").textContent = fecha ? formatearFecha(fecha) : "";
  en("autor").textContent = `${v.alias ?? "—"} · ${v.rolAutor ?? "—"}`;
  en("uid").textContent = uid;
  en("origen").textContent = pestana === "denuncias"
    ? "Publicada con denuncias pendientes"
    : ORIGEN_REVISION[v.revision?.origen] ?? (v.revision?.origen ?? "—");
  const departamentos = (v.profesorado ?? []).map((p) => `${p.departamento}${p.otro ? " (otro)" : ""} ${p.nota}`).join(", ");
  en("notas").textContent = `centro ${v.centro?.nota ?? "—"} · instalaciones ${v.instalaciones?.nota ?? "—"}${departamentos ? ` · ${departamentos}` : ""}`;
  const porMotivo = agruparDenuncias(denuncias)[0]?.motivos ?? {};
  en("denuncias").textContent = denuncias.length === 0 ? "Ninguna pendiente"
    : `${denuncias.length}: ` + Object.entries(porMotivo).map(([m, n]) => `${MOTIVO_DENUNCIA[m] ?? m} (${n})`).join(", ");

  // Review text with the filter's fragments highlighted (text nodes only, never HTML).
  const fragmentos = v.revision?.fragmentos ?? [];
  if (!v.texto) en("texto").textContent = "(sin texto)";
  for (const s of segmentos(v.texto ?? "", fragmentos)) {
    if (s.marcado) {
      const mark = document.createElement("mark");
      mark.textContent = s.texto;
      mark.title = s.motivo ?? "";
      en("texto").append(mark);
    } else {
      en("texto").append(document.createTextNode(s.texto));
    }
  }
  const fuera = fragmentos.filter((f) => !(f.inicio >= 0));
  en("otros-marcados").textContent = fuera.length ? `Marcado en "Otro departamento": ${fuera.map((f) => f.texto).join(", ")}` : "";

  for (const [clave, texto] of Object.entries(MOTIVOS_OCULTAR)) {
    const opcion = document.createElement("option");
    opcion.value = clave;
    opcion.textContent = texto;
    en("motivo-ocultar").append(opcion);
  }
  // Allowed actions by state (same rules as moderarValoracion).
  en("aprobar").hidden = pestana !== "revision";
  en("descartar").hidden = pestana !== "denuncias";

  const nota = () => en("nota-interna").value.trim() || null;
  const conNota = (accion, extra = {}) => {
    const problema = validarNota(nota());
    if (problema) return avisoEnTarjeta(nodo, problema);
    ejecutar(nodo, pestana, () => moderarValoracion({ codigo, uid, accion, nota: nota(), ...extra }));
  };
  en("aprobar").addEventListener("click", () => conNota("APROBAR"));
  en("descartar").addEventListener("click", () => conNota("DESCARTAR_DENUNCIAS"));
  en("ocultar").addEventListener("click", () => {
    const motivo = en("motivo-ocultar").value;
    if (!motivo) return avisoEnTarjeta(nodo, "Elige el motivo para ocultarla; el autor lo verá.");
    conNota("OCULTAR", { motivo });
  });
  return nodo;
}
