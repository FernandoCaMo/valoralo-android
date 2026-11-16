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