Valoralo — Verificación de docentes

Decisión (13/11/2026): no se verifica por el dominio del correo. Se verifica con un documento oficial que tenga Código Seguro de Verificación (CSV).

Documento que se pide

Hoja de servicios del Portal del docente (Junta de Andalucía). La Junta la expide de forma automatizada a cualquier docente de centros públicos, en el momento y gratis. Alternativas aceptadas: credencial de nombramiento o toma de posesión, o adjudicación de destino, siempre que tengan CSV. Los documentos de la Junta indican que su autenticidad se comprueba en https://ws050.juntadeandalucia.es/verificarFirma introduciendo el código de verificación.

Flujo
En "Mi cuenta → Soy docente", el usuario escribe su nombre completo (privado, nunca se muestra) y el código de verificación (CSV) del documento. Opcionalmente sube el PDF para que el revisor lo vea, pero no es obligatorio.
La solicitud queda en verificaciones/{uid} con estado PENDIENTE.
Un administrador (al principio, tú) abre el panel y comprueba el CSV en la web de la Junta:
que el documento existe y es auténtico;
que el nombre coincide con el que ha escrito el usuario;
que el documento es reciente: expedido después de crear la cuenta en Valoralo. Esto impide reutilizar documentos antiguos o de otra persona.
Si todo cuadra, una Cloud Function pone el custom claim rol = DOCENTE, verificado = true y opcionalmente centroActual.
Se borra el PDF (si lo había) y se guarda solo: hash del CSV, fecha y resultado. El hash impide usar el mismo documento en dos cuentas.

Implementación (Fase 4)
Datos: verificaciones/{uid} (estado PENDIENTE | APROBADA | RECHAZADA | CADUCADA, fechas, motivoRechazo, csvHash, tienePdf, solicitudes de los últimos 30 días) lo lee el propio usuario y los administradores. verificaciones/{uid}/privado/solicitud (nombre declarado, CSV en claro y ruta del PDF) solo lo leen los administradores y solo existe mientras la solicitud está PENDIENTE: al resolverla o caducar se borra y queda el hash. csvUsados/{hash} guarda los CSV aprobados y también los rechazados: ninguno se puede volver a usar. Mientras hay una solicitud pendiente con un CSV, ninguna otra cuenta puede usarlo.
Funciones (europe-southwest1): solicitarVerificacion (usuario; App Check preparado), resolverVerificacion (solo administradores), caducarVerificaciones (diaria, 04:00 Madrid).
Límites: una solicitud pendiente a la vez; como máximo 3 solicitudes por usuario en 30 días; las pendientes caducan a los 30 días y se borran sus datos y el PDF. Tras un rechazo se puede pedir de nuevo con otro documento.
PDF: opcional, en Cloud Storage (verificaciones/{uid}/…pdf, < 5 MB, sin sobrescribir); solo lo leen los administradores y se borra al resolver o caducar.
Al aprobar: custom claims rol = DOCENTE y verificado = true, usuarios/{uid} con rol DOCENTE, verificado true y centroActualCodigo opcional; sus reseñas con rolAutor OTRO pasan a DOCENTE verificado (las de ESTUDIANTE no cambian) y se recalculan las notas.
Administradores: custom claim admin = true (tools/admin/hacer_admin.py). Panel en el sitio de Hosting valoralo-admin (admin/): acceso con Google y la marca admin; los demás ven "Sin acceso".
Por qué así
No depende de dominios de correo, que comparten alumnado y profesorado.
Un documento con CSV no se puede falsificar: el original se consulta en la sede de la Junta.
La regla "expedido después del registro" obliga a tener acceso real al Portal del docente.
Guardamos el mínimo de datos (RGPD): nada de DNI ni del documento una vez revisado.
Límites y mejoras futuras
En el MVP la revisión es manual. La Junta tiene servicios de recuperación de documentos por CSV, pero son para integración interna de la Administración, no una API pública. Para automatizarlo habría que solicitarlo o comprobar si existe acceso para terceros.
Revalidación anual (los interinos cambian de situación): pedir un documento nuevo cada curso.
Al ampliar a otras comunidades, cada consejería tiene su propia sede de verificación por CSV; habrá que mantener la lista.
Mientras el docente está pendiente de verificación, sus reseñas cuentan como "Otro".
Pantallas
cuenta/verificar-docente: explicación + nombre + CSV + PDF opcional + estado (Pendiente / Verificado / Rechazado con motivo).
Panel de administración (fase posterior): lista de pendientes, enlace a la web de verificación, botones Aprobar / Rechazar. Para el MVP puede ser una página web sencilla con Firebase Hosting y acceso solo para administradores.