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
  departamentos: [String],             // [] al importar (el dataset no los trae)
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

centros/{codigo}/valoraciones/{uid}      // id = uid → 1 por usuario y centro
  uid, alias, rolAutor, verificado, fecha, editada,
  centro:        { nota 1–5, gestion?, convivencia?, comunicacion? },
  instalaciones: { nota 1–5, aulas?, equipamiento?, limpieza?, accesibilidad? },
  profesorado:   [{ departamento, nota 1–5 }],
  texto, estado: "PUBLICADA" | "EN_REVISION" | "OCULTA"

usuarios/{uid}/favoritos/{codigo}
denuncias/{id}  { centro, valoracionUid, autorUid, motivo, fecha, estado }

verificaciones/{uid}                     // solo backend y administradores
  nombreDeclarado, csvHash, pdfPath?, estado: "PENDIENTE"|"APROBADA"|"RECHAZADA",
  motivoRechazo?, fechaSolicitud, fechaRevision, revisorUid
csvUsados/{csvHash} { uid, fecha }       // evita reutilizar un documento

Cloud Functions

onValoracionWrite → recalcula centros/{codigo}.stats (total y por rol; cada grupo con su desglose por departamento).
solicitarVerificacion → crea la solicitud y comprueba que el CSV no se ha usado.
resolverVerificacion (solo administradores) → aprueba o rechaza, pone los custom claims, borra el PDF y actualiza el rolAutor de las reseñas previas del usuario: solo las que tenían rolAutor "OTRO" pasan a "DOCENTE"; las escritas como "ESTUDIANTE" no se tocan.
onDenuncia → al llegar a N denuncias pasa la reseña a EN_REVISION.
Filtro de texto (nombres propios / insultos) antes de publicar.

Consulta "filtrar por rol": valoraciones.where("rolAutor","==",X).orderBy("fecha","desc") (necesita un índice compuesto).

Rutas de navegación

home · search · centro/{codigo} · centro/{codigo}/valorar · compare?ids=a,b,c · favoritos · cuenta · cuenta/verificar-docente · cuenta/acerca-de · login · registro

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