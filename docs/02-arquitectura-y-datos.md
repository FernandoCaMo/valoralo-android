Valoralo — Arquitectura y modelo de datos
Stack (decidido)
Kotlin + Jetpack Compose (Material 3), MVVM + capas (ui / domain / data).
Hilt, Navigation Compose, Coil, Kotlin Coroutines/Flow.
Firebase: Authentication (email + Google), Cloud Firestore, Cloud Functions (TypeScript), Cloud Storage (PDF de verificación temporal), Hosting (panel admin), App Check, Crashlytics.
Room como caché local del catálogo y de favoritos.
minSdk 26. Repositorio: https://github.com/FernandoCaMo/valoralo-android
Fuente de datos (Andalucía)
Consejería de Desarrollo Educativo y FP — Consulta de centros: https://www.juntadeandalucia.es/educacion/vscripts/centros/index.asp
Portal de datos abiertos de la Junta: https://www.juntadeandalucia.es/datosabiertos/portal.html (y su réplica en datos.gob.es).
Dataset elegido: "Directorio de centros docentes no universitarios de Andalucía" (https://www.juntadeandalucia.es/datosabiertos/portal/dataset/directorio-de-centros-docentes-de-andalucia). CSV anual (UTF-8, separador ";"), licencia CC BY 4.0. Trae identificación, dirección, contacto, coordenadas y las enseñanzas como columnas S/N por nivel y modalidad. No trae ciclos concretos (p. ej. DAM), departamentos ni web.
Atribución obligatoria (CC BY 4.0): "Fuente: Junta de Andalucía, CC BY 4.0" en el detalle de cada centro y en la pantalla "Acerca de".
Tipos de centro públicos que se importan: CEIP, CEPR, CPR, IES, SES, CIFP, CEEE, CEPER, SEP, IPEP (y sus secciones), EOI, conservatorios, escuelas de arte y ESAD. Se excluyen: escuelas infantiles de 0 a 3 años, escuelas municipales de música y danza, centros de convenio e Instituto Andaluz del Deporte.
Respaldo: Registro Estatal de Centros Docentes (https://www.educacion.gob.es/centros/), filtrando por Andalucía.
Clave única: código de centro (8 dígitos).
Un script de importación (Python, en tools/import/) carga los datos en Firestore. La app no hace scraping.
Verificación de docentes

Se hace con un documento oficial con CSV (hoja de servicios del Portal del docente) revisado por un administrador. Todo el detalle está en doc 04. El rol y verificado solo los escribe el backend (custom claims + reglas de Firestore).

Registro
Edad mínima: 14 años (se pide la fecha de nacimiento al registrarse; si es menor, no se crea la cuenta).
Rol elegido al registrarse: ESTUDIANTE u OTRO. DOCENTE solo tras la verificación.
Modelo de datos (Firestore)
centros/{codigo}
  nombre, tipo (IES, CEIP, CEPR, CIFP, EOI, CPM…), titularidad: "PUBLICA",
  direccion, cp, localidad, municipio, codMunicipio (INE), provincia, comunidad: "Andalucía",
  telefono, email, web?, geo (lat,lng), fotoUrl?,
  ensenanzas: [{ nivel, nombre }],     // solo nivel y modalidad (MVP), p. ej.
                                       // { "Bachillerato", "Bachillerato semipresencial / a distancia" },
                                       // { "FP Grado Superior", "FP Grado Superior" }; sin ciclos concretos
  departamentos: [String],             // de la plantilla orgánica (tools/import/especialidades_departamentos.csv)
                                       // o, si el centro no tiene plantilla, la lista estándar por tipo
                                       // (tools/import/departamentos_estandar.csv)
  departamentosFuente: "plantilla" | "estandar",
  departamentosCurso?,                 // curso de la plantilla, p. ej. "2025/2026"
  fuente: { dataset, curso },          // p. ej. { "directorio-de-centros-docentes-de-andalucia", "2024/2025" }
  stats: {                             // lo escribe solo una Cloud Function
    total:      { n, global, centro, instalaciones, profesorado,
                  porDepartamento: { "Matemáticas": { n, media }, ... } },
    docente:    { ...igual },
    estudiante: { ...igual },
    otro:       { ...igual }
  }

Cálculo de las notas (lo hace onValoracionWrite; solo cuentan las reseñas PUBLICADA):
  Por reseña:
    profesorado = media de las notas de los departamentos que valora; si no valora ninguno, no tiene nota de profesorado.
    global      = media de sus dimensiones (centro, instalaciones y, si la tiene, profesorado), todas con el mismo peso.
  Por centro y grupo de rol:
    cada media (global, centro, instalaciones, profesorado y cada departamento) se calcula aparte,
    con las reseñas del grupo que tienen ese dato. n = nº de reseñas del grupo.
  Se guardan sin redondear; la app las muestra con un decimal.

usuarios/{uid}
  alias, rol: "DOCENTE" | "ESTUDIANTE" | "OTRO", verificado: Boolean,
  fechaAlta, mayorDe14: true, centroActualCodigo?   // rol/verificado: solo backend
  // Reglas: el usuario crea su propio documento con exactamente alias, rol (ESTUDIANTE u OTRO),
  // verificado: false, mayorDe14: true y fechaAlta = hora del servidor; después solo puede cambiar
  // el alias (3–30 caracteres), el rol entre ESTUDIANTE y OTRO (nunca a DOCENTE ni desde DOCENTE)
  // y borrar el documento (eliminar cuenta). Solo lee el suyo. Las reseñas ya escritas mantienen su rolAutor.
  // La fecha de nacimiento se pide para comprobar la edad, pero no se guarda.

centros/{codigo}/valoraciones/{uid}      // id = uid → 1 por usuario y centro
  uid, alias, rolAutor, verificado, fecha (alta), actualizada, editada,
  centro:        { nota 1–5, gestion?, convivencia?, comunicacion? },
  instalaciones: { nota 1–5, aulas?, equipamiento?, limpieza?, accesibilidad? },
  profesorado:   [{ departamento, nota 1–5, otro?: true }],   // otro = "Otro departamento" (texto libre, filtrado)
  texto (opcional, ≤ 1.000 caracteres), estado: "PUBLICADA" | "EN_REVISION" | "OCULTA",
  revision?: { origen: "FILTRO" | "DENUNCIAS" | "MODERACION", fragmentos: [{ inicio, fin, texto, motivo }] },
  moderacion?: { decision, fecha, nota }
  // La escribe solo la función publicarValoracion (valida, filtra y toma rolAutor del perfil; al
  // editar mantiene fecha y rolAutor). El autor puede leer la suya en cualquier estado y borrarla;
  // los demás solo leen las PUBLICADA.

centros/{codigo}.catalogoDoc            // documento catalogo/{provincia}-{parte} del centro (lo escribe el script)

config/catalogo     { version, partes: ["almeria-1", ...] }   // la escribe el script de importación; solo cambia al importar
catalogo/{provincia}-{parte}                  // catálogo resumido para Inicio y los filtros (lectura pública)
  provincia, parte, partes,
  centros: [{ codigo, nombre, tipo, localidad, municipio, niveles: [String], nota (stats.total.global), resenas (stats.total.n) }]
  // Una parte por provincia mientras quepa con margen bajo 1 MiB (Sevilla, la mayor, ~200 KiB); si no, se divide.
  // alEscribirValoracion actualiza la nota y el nº de reseñas del centro en su documento de catálogo.
config/departamentos { porTipo: { "IES": [...], ... } }   // listas estándar por tipo

destacadas/actual   // reseñas destacadas de Inicio (lectura pública; solo backend). Abrir Inicio = 1 lectura.
  { resenas: [{ clave (huella de centro + autor, sin uid), codigo, nombre, municipio, tipo, alias, rolAutor,
      verificado, nota (de esa reseña), extracto (~200 caracteres), fecha }],
    minimoParaMostrar, generada }
  // La calcula calcularDestacadas (cada día, 05:00, europe-west1) o recalcularDestacadas (botón del panel).
  // alEscribirValoracion y denunciarValoracion retiran al momento una destacada que se oculta, se edita,
  // se borra o se denuncia.
config/destacadas   // números de la selección; si falta un campo o no es válido, se usa el valor por defecto
  { minCaracteres: 120, maxDias: 180, maxResenas: 12, maxFraccionAltas: 0.333, umbralAlta: 4.5,
    umbralBaja: 3.5, minimoParaMostrar: 3, longitudExtracto: 200 }
  // Se cambia desde la consola de Firebase, sin desplegar; el cambio vale desde el siguiente cálculo.

Pestañas: Inicio (logo, carrusel de destacadas, "Buscar un centro" y, con sesión, 3 favoritos) ·
Buscar (lista con buscador y filtros) · Favoritos · Mi cuenta.
Filtros de Buscar, todos de selección múltiple y en secciones plegables: Provincia → Ciudad (municipio)
→ Localidad → Tipo de centro → Enseñanzas. Ciudad y Localidad solo ofrecen lo que encaja con lo
elegido arriba y tienen buscador.

Caché del catálogo (Room): Buscar lee los centros (con sus notas) de una caché local. En cada arranque
se lee config/catalogo (1 lectura); el catálogo se vuelve a descargar (los documentos catalogo/*, ~8
lecturas, en paralelo y con "Descargando centros…" y su progreso) si cambia la versión, si la caché
tiene más de 24 horas o si cambia el formato de la caché. Sin red se muestra la caché. El detalle del
centro y sus reseñas se leen siempre en directo de Firestore.

usuarios/{uid}/favoritos/{codigo} { codigo, fecha }   // solo el dueño; máx. 100 (lo controla la app)
// valoraciones: moderacion { decision: APROBADA|OCULTADA|DENUNCIAS_DESCARTADAS, motivo?, motivoTexto? (lo ve el autor),
//   nota? (interna), fecha, moderadorUid }; la escriben moderarValoracion o tools/moderacion/moderar.py.
// stats.{grupo}.nProfesorado: reseñas del grupo que valoran algún departamento.
denuncias/{codigo}_{autorUid}_{denuncianteUid}   // una por usuario y reseña; solo backend
  { centro, valoracionUid, denuncianteUid, motivo: INSULTOS | NOMBRE_PROPIO | DATOS_PERSONALES | FALSA_O_SPAM | OTRO,
    fecha, estado: "PENDIENTE" | "RESUELTA", decision?, resuelta? }

verificaciones/{uid}                     // lo escribe solo el backend; lo leen el propio usuario y los administradores
  estado: "PENDIENTE"|"APROBADA"|"RECHAZADA"|"CADUCADA", fechaSolicitud, fechaRevision, revisorUid,
  motivoRechazo?, csvHash, tienePdf, solicitudes: [fechas de los últimos 30 días]
verificaciones/{uid}/privado/solicitud   // solo administradores; solo mientras está PENDIENTE
  nombreDeclarado, csv (en claro, para comprobarlo en la web de la Junta), pdfPath?
csvUsados/{csvHash} { uid, fecha, resultado: "APROBADA"|"RECHAZADA" }   // evita reutilizar un documento
// Detalle del flujo, límites y caducidad: doc 04.

Cloud Functions (functions/, TypeScript, región europe-southwest1)

Implementadas (Fase 5):
publicarValoracion (callable, App Check) → valida el formulario, pasa el filtro de texto (también a "Otro departamento"): BLOQUEADO no se guarda y devuelve los fragmentos; DUDOSO se guarda EN_REVISION con los fragmentos; si el autor edita una reseña en revisión por el filtro y ahora pasa, se publica. Las que están en revisión por denuncias u ocultas por moderación vuelven a revisión al editarlas.
denunciarValoracion (callable, App Check) → una denuncia por usuario y reseña (nunca la propia); con 3 denuncias PENDIENTES pasa la reseña a EN_REVISION.
alEscribirValoracion (trigger) → recalcula stats y la nota y nº de reseñas del centro en su catalogo/{provincia}-{parte}.
alBorrarUsuario (trigger al borrar usuarios/{uid}, que hace la app al eliminar la cuenta) → borra sus valoraciones (cada borrado recalcula su centro) y sus denuncias.
Moderación hasta la Fase 6: tools/moderacion/moderar.py (listar, aprobar, rechazar; resuelve las denuncias pendientes).

Filtro de texto (functions/src/filtro): sin servicios externos. Normaliza (minúsculas, sin tildes, "leet", letras repetidas, insultos deletreados); listas propias de insultos (bloquean) e insultos ambiguos (dudosos tras "un", "eres"…); nombres y apellidos del INE (tools/filtro): nombre tras un tratamiento ("la profe Ana") o nombre y apellido seguidos → bloqueado; nombre de pila en mayúscula → bloqueado; nombre ambiguo o apellido en mayúscula a mitad de frase, o nombre frecuente en minúscula → dudoso; correos, teléfonos y enlaces → bloqueado. Excepciones: palabras del nombre del centro, localidad, municipio, provincia, departamentos, asignaturas y términos educativos.

Plan original:

onValoracionWrite → recalcula centros/{codigo}.stats (total y por rol; cada grupo con su desglose por departamento) y actualiza nota y resenas del centro en su documento catalogo/{provincia}-{parte}.
solicitarVerificacion → crea la solicitud y comprueba que el CSV no se ha usado.
resolverVerificacion (solo administradores) → aprueba o rechaza, pone los custom claims, borra el PDF y actualiza el rolAutor de las reseñas previas del usuario: solo las que tenían rolAutor "OTRO" pasan a "DOCENTE"; las escritas como "ESTUDIANTE" no se tocan.
onDenuncia → al llegar a N denuncias pasa la reseña a EN_REVISION.
Filtro de texto (nombres propios / insultos) antes de publicar.

Consulta "filtrar por rol": valoraciones.where("rolAutor","==",X).orderBy("fecha","desc") (necesita un índice compuesto).

Rutas de navegación

home · search · centro/{codigo} · centro/{codigo}/valorar · compare?ids=a,b,c · favoritos · cuenta · cuenta/verificar-docente · cuenta/acerca-de · login · registro · registro/perfil (completar el perfil tras el primer acceso con Google)

Búsqueda: por nombre, localidad o municipio (sin distinguir mayúsculas ni tildes).

Estructura del repo
valoralo-android/
 ├─ app/src/main/java/com/valoralo/
 │   ├─ ui/ (theme, components, home, detail, review, compare, favorites, account, verify)
 │   ├─ domain/ (model, usecase)
 │   ├─ data/ (remote/firestore, local/room, repository)
 │   └─ di/
 ├─ functions/          // Cloud Functions (TypeScript)
 ├─ admin/              // panel web de verificación (Firebase Hosting)
 ├─ firestore.rules, storage.rules, firestore.indexes.json
 ├─ tools/import/       // script de importación de centros
 ├─ docs/ (bocetos/, decisiones.md)
 └─ CLAUDE.md