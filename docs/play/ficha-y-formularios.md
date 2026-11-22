# Google Play: ficha, formularios y pasos (Fase 7)

Textos aprobados el 17/11/2026. Gráficos en esta carpeta (`icono-512.png`, `grafico-destacado-1024x500.png`),
generados con `tools/play/graficos.py` a partir del icono de la app.

## Ficha de la tienda

**Nombre:** Valoralo · **Paquete:** `io.github.fernandocamo.valoralo` · **Categoría:** Educación · **Contacto:** valoralo.app@gmail.com
**Política de privacidad:** https://valoralo-dc735.web.app/privacidad.html

**Descripción corta** (≤ 80 caracteres):

> Opiniones de colegios e institutos públicos de Andalucía, por quien los conoce.

**Descripción larga** (vigente desde el 21/11/2026, tras el rechazo de la prueba cerrada por la política de
declaraciones engañosas; texto tal como está en Play Console):

```text
AVISO: Valoralo es una aplicación independiente. No representa a la Junta de Andalucía ni a ningún organismo público o centro educativo, y no está afiliada a ellos ni autorizada por ellos. Las opiniones publicadas son de sus usuarios.

Valoralo te ayuda a conocer los centros educativos públicos de Andalucía a través de la experiencia de quienes los viven cada día: estudiantes, familias y docentes.

BUSCA TU CENTRO
Más de 3.700 colegios, institutos, escuelas de idiomas, conservatorios, centros de adultos y escuelas de arte, con su dirección, contacto, enseñanzas y departamentos. Filtra por provincia, municipio, tipo de centro o enseñanzas.

LEE Y ESCRIBE VALORACIONES
Puntúa el centro en general, las aulas e instalaciones y el profesorado por departamento, nunca a personas concretas. Filtra las opiniones según quién las escribe: docentes, estudiantes u otras personas. Marca con un corazón las reseñas que te resulten útiles.

DOCENTES VERIFICADOS
El profesorado puede verificar su condición con un documento oficial, y sus reseñas muestran una insignia.

COMPARA Y GUARDA
Compara hasta tres centros lado a lado y guarda tus favoritos.

UN ESPACIO RESPETUOSO
Las reseñas pasan un filtro automático y moderación humana. No se permiten nombres de personas, insultos ni datos personales, y puedes denunciar cualquier reseña o bloquear a su autor.

Necesitas tener al menos 14 años para crear una cuenta. Consultar centros y reseñas no requiere cuenta.

FUENTES DE LOS DATOS OFICIALES
Los datos de los centros (nombre, dirección, contacto, enseñanzas y departamentos) proceden del portal de datos abiertos de la Junta de Andalucía, con licencia CC BY 4.0, y pueden no estar actualizados. Consulta siempre la información oficial:
- Directorio de centros docentes no universitarios de Andalucía: https://www.juntadeandalucia.es/datosabiertos/portal/dataset/directorio-de-centros-docentes-de-andalucia
- Plantillas orgánicas de centros docentes públicos: https://www.juntadeandalucia.es/datosabiertos/portal/dataset/plantillas-organicas-de-centros-docentes-publicos
- Consulta oficial de centros de la Consejería de Educación: https://www.juntadeandalucia.es/educacion/vscripts/centros/index.asp
```

**El aviso y las tres fuentes deben coincidir con la app** (`ui/components/Fuente.kt`: `AVISO_INDEPENDENCIA`,
`FUENTES_OFICIALES`; test `FuentesOficialesTest`). Desde la 1.0.7 la app muestra el aviso al principio de Acerca de, la
sección «Fuentes de los datos oficiales» con los mismos tres enlaces, y en el detalle de cada centro el enlace a la
fuente oficial y una línea de independencia. Enlaces comprobados el 22/11/2026: los tres responden; el tercero redirige
a `educaweb.juntadeandalucia.es/centros/index.asp` (buscador oficial de centros).

**Capturas** (1080×1920, 9:16) en `capturas/`, **fuera del repositorio** (`.gitignore`): `01-inicio`,
`02-buscar-filtros`, `03-detalle-centro`, `04-valorar-centro` y `05-comparador`. Hechas el 19/11/2026 en el emulador
(Pixel 5 con `wm size 1080x1920`) con la versión debug contra los emuladores de Firebase. **Siempre con centros y
reseñas ficticios** (ver decisiones.md): IES Monte Claro, IES Arroyo Sereno, CEIP Valle Claro, CEIP Las Amapolas y
CEIP Colina Azul, con reseñas de ejemplo firmadas solo con el rol. La de filtros muestra el catálogo real de Sevilla,
sin reseñas. Barra de estado en modo demo; los iconos de notificaciones del sistema se taparon con el color de fondo.

## Contenido de la app (Play Console → Política)

| Apartado | Respuesta |
|---|---|
| Política de privacidad | https://valoralo-dc735.web.app/privacidad.html |
| Acceso a la app | Algunas funciones requieren cuenta: dar a Google una cuenta de prueba (correo y contraseña) creada para los revisores |
| Anuncios | No |
| ID de publicidad | No (el permiso `AD_ID` se quita en el manifiesto) |
| Clasificación de contenido (IARC) | Categoría Referencia/Educación; contenido generado por usuarios con interacción; con moderación y denuncias; sin compras, sin ubicación |
| Público objetivo | 13-15, 16-17 y 18+ (no marcar menores de 13: activaría el programa para familias) |
| App gubernamental | No |
| Eliminación de cuenta | https://valoralo-dc735.web.app/eliminar-cuenta.html (y en la app: Mi cuenta → Eliminar cuenta) |

## Formulario de seguridad de los datos

- ¿Recoge o comparte datos? **Recoge.** ¿Cifrados en tránsito? **Sí.** ¿El usuario puede pedir que se eliminen? **Sí** (app y web).
- **Compartidos con terceros: ninguno** (Firebase trata los datos por encargo nuestro; Play no lo cuenta como compartir).
- "Opcional" = se puede usar la app sin dar ese dato (sin cuenta se puede consultar todo).

| Tipo de dato | Para qué | ¿Obligatorio? |
|---|---|---|
| Información personal → Correo electrónico | Gestión de la cuenta | Opcional |
| Información personal → Nombre (alias visible; nombre de la cuenta de Google; nombre declarado al verificarse como docente) | Funcionalidad, gestión de la cuenta, prevención de fraude | Opcional |
| Información personal → ID de usuario | Funcionalidad, gestión de la cuenta, seguridad | Opcional |
| Información personal → Otra información (confirmación de tener 14 años o más) | Cumplimiento | Opcional |
| Archivos y documentos (documento de verificación docente; se borra tras la revisión) | Prevención de fraude | Opcional |
| Actividad en apps → Otro contenido generado por usuarios (reseñas y notas) | Funcionalidad, moderación | Opcional |
| Actividad en apps → Otras acciones (likes, favoritos, denuncias, bloqueos) | Funcionalidad, seguridad | Opcional |
| Información y rendimiento de la app → Registros de fallos y Diagnóstico (Crashlytics; no vinculados a la cuenta) | Análisis | Se recogen siempre (no se pueden desactivar en la app) |
| Dispositivo u otros IDs (identificador de instalación de Crashlytics) | Análisis | Se recoge siempre |

La dirección IP la reciben Firebase Auth y Functions por seguridad; no la usamos para saber la ubicación.

## Pasos en Play Console

1. Crear la app: nombre Valoralo, español (España), app, gratuita. Aceptar la **firma de apps de Google Play**.
2. **Prueba interna**: subir el AAB (`app/build/outputs/bundle/release/app-release.aab`), añadirte como tester e instalar desde Play.
3. **Huellas en Firebase**: Play Console → Integridad de la app → Firma de apps: copiar SHA-1 y SHA-256 de **todas** las claves de firma que muestre (con la firma híbrida preparada para la computación cuántica son tres: la clásica para Android 16 o anterior, la clásica de la firma híbrida y la poscuántica; también las de "claves de firma anteriores" si aparecen) y de la **clave de subida**. En Valoralo son: clásica `1D:9A:6D:CE:E9:0E:5D:4D:05:F3:06:CA:05:DC:E9:90:70:42:7C:A8`, poscuántica `A8:47:E3:62:9F:55:D0:EA:56:DA:A4:6C:A5:B5:E0:F8:8E:24:CF:C0` y anterior `26:FF:D8:13:5B:5F:08:AD:75:35:F2:88:64:91:8F:A2:6E:A7:A9:79` (la que ven los móviles con Android 16 o anterior; sin ella, "Continuar con Google" falla con el código 16). Si vuelve a fallar, Crashlytics recibe la huella de la instalación (`firma_sha1`) para compararla. Añadirlas en Firebase → Configuración del proyecto → app Android `io.github.fernandocamo.valoralo` → Huellas digitales (junto con las de debug). Después, App Check → app Android → Play Integrity con el SHA-256 de la clave de firma. Sin el SHA-1 de la clave de firma, el inicio de sesión con Google falla en la versión de Play.
4. Contenido de la app y ficha (tablas de arriba).
5. **Prueba cerrada**: lista de correos o Grupo de Google con 15–16 testers (mínimo 12), compartir el enlace de participación; cada uno acepta e instala desde Play. **12 testers durante 14 días seguidos** (cuentas personales creadas después del 13/11/2023); si alguien abandona y vuelve, su plazo empieza de cero.
6. Panel de control → **Solicitar acceso a producción** (preguntas sobre la prueba, la app y su preparación). Google suele responder en 7 días o menos.
7. Publicar en producción y avisar para activar App Check obligatorio (funciones + Firestore + Storage).

Requisitos comprobados en noviembre de 2026: [prueba cerrada](https://support.google.com/googleplay/android-developer/answer/14151465?hl=es),
[nivel de API (Android 16 / API 36)](https://support.google.com/googleplay/android-developer/answer/11926878),
[contenido generado por usuarios](https://support.google.com/googleplay/android-developer/answer/9876937).
