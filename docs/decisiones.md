# Valoralo — Registro de decisiones

La carpeta `docs/` es la fuente de verdad de la especificación. Cualquier cambio se hace aquí y se anota en este registro.

## 16/11/2026

- **Idioma del código.** Dominio y datos en español (`Centro`, `Valoracion`, `rolAutor`, `ensenanzas`), coherente con el modelo de Firestore del doc 02. Los sufijos y términos técnicos van en inglés (`ViewModel`, `Repository`, `Screen`, `UiState`). La UI, en español.
- **Cambio de rol al verificar a un docente.** Al aprobar la verificación, solo las reseñas previas con `rolAutor = "OTRO"` pasan a `"DOCENTE"`. Las escritas como `"ESTUDIANTE"` se quedan igual. Actualizado en el doc 02 (`resolverVerificacion`).
- **Verificación y datos personales.** Corregido el doc 01: de la verificación solo se guarda el hash del CSV, la fecha y el resultado; nunca el correo (ver doc 04).
- **Ficha de centro.** Se sigue el doc 01 aunque el boceto no lo muestre: notas por dimensión (centro / instalaciones / profesorado), desglose por departamento y chips de rol que recalculan las medias y filtran las reseñas.
- **Pestaña Buscar.** Lleva a Inicio con el buscador enfocado. Favoritos y Mi cuenta quedan como pantallas vacías hasta sus fases.
- **Datos de prueba.** Solo centros de Andalucía (los bocetos muestran Madrid; es solo ilustrativo).
- **Desglose por departamento por rol.** `porDepartamento` va dentro de cada grupo de `stats` (`total`, `docente`, `estudiante`, `otro`), de modo que el filtro de rol también recalcula el desglose por departamento. Actualizado en el doc 02.
- **Cálculo de las notas.** La nota de profesorado de una reseña es la media de los departamentos que valora; si no valora ninguno, no cuenta. La global de una reseña es la media de sus dimensiones, todas con el mismo peso. Cada media del centro se calcula aparte, con las reseñas que tienen ese dato. Se calcula sin redondear y se muestra con un decimal. Detalle en el doc 02.
- **Build.** Se compila con el JDK 21 de Android Studio (`jbr`). El plugin de Google Services solo se aplica si existe `app/google-services.json`, que no se sube al repo.
