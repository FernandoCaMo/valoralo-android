Valoralo — Plan de trabajo y próxima sesión
Decisiones tomadas (13/11/2026)
Backend: Firebase (Auth + Firestore + Cloud Functions).
Profesorado: se valora por departamento.
Ámbito inicial: Andalucía.
Docentes: verificación obligatoria con documento oficial con CSV (hoja de servicios del Portal del docente) y revisión manual. Nada de verificar por dominio de correo. Detalle en el doc 04.
Filtro de reseñas por rol: Docentes · Estudiantes · Otros.
Edad mínima: 14 años.
Repositorio: https://github.com/FernandoCaMo/valoralo-android
Pendiente
Dar acceso a Claude al repositorio (ver checklist).
Dataset descargable de centros de Andalucía (portal de datos abiertos de la Junta) y si incluye enseñanzas y departamentos.
Probar el flujo de verificación con una hoja de servicios real (comprobar qué datos muestra la web de verificación de la Junta).
Hoja de ruta
Fase	Contenido
0. Preparación	Proyecto Android Studio (Empty Compose Activity, paquete com.valoralo), proyecto Firebase + google-services.json, tema Material 3, CLAUDE.md, primer commit.
1. Datos	Dataset de Andalucía → script tools/import → Firestore. Empezar con una provincia.
2. Catálogo	Home (lista + búsqueda + filtros) y detalle de centro, primero con datos de prueba.
3. Cuentas	Registro con fecha de nacimiento (≥14), login email/Google, Mi cuenta.
4. Verificación docente	Pantalla de solicitud, Functions, reglas, panel admin sencillo.
5. Valoraciones	Formulario de 3 bloques + departamentos, agregados por rol, filtro de reseñas.
6. Extras MVP	Favoritos, comparador, denuncias, moderación.
7. Publicación	Privacidad, condiciones, pruebas, Google Play.
Checklist al llegar a casa
[ ] Acceso de Claude al repo: en GitHub → Settings → Applications → Claude → Configure, y añade valoralo-android a los repositorios permitidos (o elige "All repositories"). Si la app de Claude no aparece, conéctala desde claude.ai → Ajustes → Conectores → GitHub.
[ ] Android Studio actualizado y emulador funcionando.
[ ] Crear el proyecto "valoralo" en la consola de Firebase y activar Auth (email + Google) y Firestore.
[ ] Clonar el repo, copiar CLAUDE.md (doc claude/CLAUDE-repo.md) a la raíz y los bocetos a docs/bocetos/.
[ ] Abrir Claude Code en la carpeta del repo.
Prompt para empezar

Lee CLAUDE.md y los documentos 01–04 del proyecto Valoralo. Crea la Fase 0 (proyecto Android Kotlin + Compose con la estructura del doc 02, dependencias de Hilt, Navigation, Firebase y Room) y la Fase 2 con datos de prueba de 5 centros andaluces: pantallas de Inicio y Detalle siguiendo los bocetos de docs/bocetos.