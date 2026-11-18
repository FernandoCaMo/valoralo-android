# Google Play: ficha, formularios y pasos (Fase 7)

Textos aprobados el 17/11/2026. Gráficos en esta carpeta (`icono-512.png`, `grafico-destacado-1024x500.png`),
generados con `tools/play/graficos.py` a partir del icono de la app.

## Ficha de la tienda

**Nombre:** Valoralo · **Paquete:** `io.github.fernandocamo.valoralo` · **Categoría:** Educación · **Contacto:** valoralo.app@gmail.com
**Política de privacidad:** https://valoralo-dc735.web.app/privacidad.html

**Descripción corta** (≤ 80 caracteres):

> Opiniones de colegios e institutos públicos de Andalucía, por quien los conoce.

**Descripción larga:**

> Valoralo te ayuda a conocer los centros educativos públicos de Andalucía a través de la experiencia de quienes los viven cada día: estudiantes, familias y docentes.
>
> **Busca tu centro.** Más de 3.700 colegios, institutos, escuelas de idiomas, conservatorios, centros de adultos y escuelas de arte, con su dirección, sus enseñanzas y sus departamentos. Filtra por provincia, municipio, tipo de centro o enseñanzas.
>
> **Lee y escribe valoraciones.** Puntúa el centro en general, las aulas e instalaciones y el profesorado por departamento, nunca a personas concretas. Filtra las opiniones por quién las escribe: docentes, estudiantes u otras personas.
>
> **Docentes verificados.** El profesorado puede verificar su condición con un documento oficial, y sus reseñas muestran una insignia.
>
> **Compara y guarda.** Compara hasta tres centros, lado a lado, y guarda tus favoritos.
>
> **Un espacio respetuoso.** Las reseñas pasan un filtro automático y una moderación humana. No se permiten nombres de personas, insultos ni datos personales, y puedes denunciar cualquier reseña o bloquear a su autor.
>
> Necesitas tener al menos 14 años para crear una cuenta. Consultar centros y reseñas no requiere cuenta.
>
> Valoralo es un proyecto independiente: no está vinculado a la Junta de Andalucía ni a ningún centro. Los datos de los centros proceden del portal de datos abiertos de la Junta de Andalucía (licencia CC BY 4.0).

**Capturas** (las haces en el móvil, entre 2 y 8, recomendadas 4–6): Inicio con el carrusel, Buscar con filtros,
detalle de un centro, comparador y una reseña.

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
3. **Huellas en Firebase**: Play Console → Integridad de la app → Firma de apps: copiar SHA-1 y SHA-256 de **todas** las claves de firma que muestre (con la firma híbrida preparada para la computación cuántica son tres: la clásica para Android 16 o anterior, la clásica de la firma híbrida y la poscuántica; también las de "claves de firma anteriores" si aparecen) y de la **clave de subida**. Comprobación: en la app instalada desde Play, Mi cuenta → Acerca de → Firma de esta instalación muestra la huella que debe estar en Firebase. Añadirlas en Firebase → Configuración del proyecto → app Android `io.github.fernandocamo.valoralo` → Huellas digitales (junto con las de debug). Después, App Check → app Android → Play Integrity con el SHA-256 de la clave de firma. Sin el SHA-1 de la clave de firma, el inicio de sesión con Google falla en la versión de Play.
4. Contenido de la app y ficha (tablas de arriba).
5. **Prueba cerrada**: lista de correos o Grupo de Google con 15–16 testers (mínimo 12), compartir el enlace de participación; cada uno acepta e instala desde Play. **12 testers durante 14 días seguidos** (cuentas personales creadas después del 13/11/2023); si alguien abandona y vuelve, su plazo empieza de cero.
6. Panel de control → **Solicitar acceso a producción** (preguntas sobre la prueba, la app y su preparación). Google suele responder en 7 días o menos.
7. Publicar en producción y avisar para activar App Check obligatorio (funciones + Firestore + Storage).

Requisitos comprobados en noviembre de 2026: [prueba cerrada](https://support.google.com/googleplay/android-developer/answer/14151465?hl=es),
[nivel de API (Android 16 / API 36)](https://support.google.com/googleplay/android-developer/answer/11926878),
[contenido generado por usuarios](https://support.google.com/googleplay/android-developer/answer/9876937).
