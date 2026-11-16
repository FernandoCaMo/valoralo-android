Valoralo — Visión y requisitos
Idea

App Android para valorar los centros educativos públicos de España (no universitarios). El MVP empieza solo con Andalucía. Se valora en tres dimensiones:

Centro en general (gestión, ambiente, convivencia, comunicación)
Aulas / instalaciones (estado, equipamiento, climatización, accesibilidad, limpieza)
Profesorado por departamento (p. ej. Matemáticas, Lengua, Informática…). Nunca por persona.
Usuarios y roles
Rol	Verificación	Qué busca
Docente	Obligatoria (ver doc 02, "Verificación de docentes")	Elegir centro (concurso de traslados, interinidades): ambiente, equipo directivo, instalaciones, claustro.
Estudiante	No	Qué enseñanzas imparte cada centro y opiniones sobre instalaciones y profesorado.
Otro (familia, exalumno…)	No	Igual que estudiante.
Leer reseñas: cualquiera (sin cuenta).
Escribir reseñas: con cuenta. Un docente sin verificar no puede publicar como docente: hasta que se verifique, su reseña sale como "Otro".
Filtro de reseñas por rol: Todos · Docentes · Estudiantes · Otros. Las notas medias se recalculan según el filtro (p. ej. "4,2 según docentes / 3,6 según estudiantes").
En cada reseña de docente verificado se muestra la insignia "Docente verificado".
Pantallas (según los bocetos)

1. Inicio / listado

Cabecera "Valoralo" + botón Filtros (provincia andaluza, tipo de centro, enseñanzas).
Barra de búsqueda "Buscar centro educativo…".
Tarjetas: imagen, nombre, localidad, nota media ★, nº de reseñas, Ver más y Comparar.
Barra inferior: Inicio · Buscar · Favoritos · Mi cuenta.

2. Detalle del centro

Volver, nombre, localidad, puntuación global y nº de reseñas.
Información: contacto y dirección.
Oferta académica: ESO, Bachillerato, Ciclos Formativos…
Notas por dimensión (centro / instalaciones / profesorado) y desglose por departamento.
Reseñas con filtro por rol (chips: Todos · Docentes · Estudiantes · Otros).
Botón + Añadir valoración (solo usuarios registrados).

Pantallas por diseñar: formulario de valoración (3 bloques + departamentos), verificación de docente, comparador, favoritos, mi cuenta / login.

Requisitos funcionales (MVP)
Catálogo de centros públicos de Andalucía con datos oficiales y enseñanzas.
Búsqueda por nombre; filtros por provincia, municipio, tipo y enseñanza.
Ficha de centro con medias por dimensión, por departamento y por rol.
Registro/login (email y Google) con elección de rol.
Verificación de docentes.
Crear, editar y borrar la propia valoración (1 por usuario y centro).
Filtro de reseñas por rol.
Favoritos y comparador (2–3 centros).
Denunciar reseñas + moderación básica.
Fuera del MVP

Resto de comunidades autónomas, mapa, notificaciones, respuestas del centro, iOS/web.

⚠️ Nota legal (RGPD / LOPDGDD)
Profesorado valorado por departamento, nunca por persona. Prohibido escribir nombres propios en los textos (filtro automático + moderación).
Hace falta política de privacidad, condiciones de uso, edad mínima (menores de 14 necesitan consentimiento parental en España) y un sistema de denuncia y retirada.
De la verificación de un docente solo se guarda el hash del CSV, la fecha y el resultado; nunca el correo ni el documento (el PDF se borra tras la revisión). El nombre declarado nunca se muestra. Ver doc 04.