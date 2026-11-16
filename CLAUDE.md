CLAUDE.md — Valoralo (Android)


Qué es

App Android para valorar centros educativos públicos de España (no universitarios). MVP: solo Andalucía. Se valora: centro en general, aulas/instalaciones y profesorado por departamento (nunca por persona). Roles: DOCENTE (verificado con documento oficial con CSV), ESTUDIANTE, OTRO. Las reseñas se filtran por rol. Edad mínima: 14 años.

Especificación completa en el Project "Valoralo" de Claude: docs 01 (requisitos), 02 (arquitectura y datos), 03 (plan), 04 (verificación de docentes).

Stack
Kotlin, Jetpack Compose (Material 3), MVVM, Hilt, Navigation Compose, Coroutines/Flow, Coil, Room.
Firebase: Auth (email + Google), Firestore, Cloud Functions (TypeScript, en functions/), Storage, Hosting (panel admin en admin/), App Check, Crashlytics.
minSdk 26. Paquete com.valoralo.
Estructura
app/src/main/java/com/valoralo/{ui,domain,data,di}
functions/ Cloud Functions · admin/ panel de verificación · firestore.rules · storage.rules · firestore.indexes.json
tools/import/ script de importación de centros (Python)
docs/bocetos/ bocetos de Inicio y Detalle
Reglas del proyecto
Idioma de la UI: español. Código e identificadores en inglés o español, pero de forma coherente (decidir en la Fase 0 y no mezclar).
rol, verificado y stats solo los escribe el backend (Cloud Functions / custom claims). Las reglas de Firestore deben impedir que el cliente los escriba.
Una valoración por usuario y centro: documento centros/{codigo}/valoraciones/{uid}.
Nunca guardar ni mostrar nombres de profesores. Filtrar nombres propios en los textos.
Verificación de docentes: guardar solo el hash del CSV, la fecha y el resultado. El PDF se borra tras la revisión. El nombre declarado nunca se muestra.
Registro: bloquear a menores de 14 años.
No subir google-services.json de producción ni secretos al repo.
TDD para la lógica de dominio y de las Functions (agregados, permisos, verificación).
Comandos
Compilar: ./gradlew assembleDebug
Tests: ./gradlew test
Functions: cd functions && npm test · emuladores: firebase emulators:start