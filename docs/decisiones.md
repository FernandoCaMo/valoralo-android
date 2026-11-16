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
- **Versiones.** BOM de Firebase 35.2.0 y plugin de Google Services 4.6.0. Esa BOM obliga a Kotlin ≥ 2.3 (`firebase-auth` 25 trae metadatos de Kotlin 2.4): Kotlin 2.3.21 y KSP 2.3.14. Hilt se queda en 2.57.2 porque 2.60 exige AGP 9.
- **Fuente de datos de centros.** "Directorio de centros docentes no universitarios de Andalucía" (portal de datos abiertos de la Junta, CC BY 4.0), curso 2024/2025. Detalle y enlace en el doc 02.
- **Tipos de centro importados.** Solo públicos: CEIP, CEPR, CPR, IES, SES, CIFP, CEEE, CEPER, SEP, IPEP (y secciones), EOI, conservatorios, escuelas de arte y ESAD. Los CEIP se incluyen aunque su alumnado tenga menos de 14 años, porque docentes y familias también los valoran. Se excluyen las escuelas infantiles de 0 a 3 años, las escuelas municipales de música y danza, los centros de convenio y el Instituto Andaluz del Deporte.
- **Departamentos.** Se deducen de las "Plantillas orgánicas de centros docentes públicos" (Junta, CC BY 4.0) con una tabla versionada especialidad → departamento (`tools/import/especialidades_departamentos.csv`; las variantes bilingües cuentan como su especialidad). Si el centro no tiene plantilla, se usa la lista estándar por tipo (`tools/import/departamentos_estandar.csv`, también en `config/departamentos`). Cada centro guarda `departamentosFuente` ("plantilla" o "estandar") y `departamentosCurso`. Como la lista es aproximada, el formulario de valoración (Fase 5) tendrá la opción "Otro departamento".
- **Firestore.** Reglas desplegadas en `valoralo-dc735`: lectura pública de centros, valoraciones publicadas y config; ninguna escritura del cliente; el resto cerrado. Tests en `rules-tests/`. La app lee de Firestore; los datos de prueba quedan para los tests. Las valoraciones se ordenan por fecha en la app, así que de momento no hace falta ningún índice compuesto.
- **Campos nuevos del centro.** `localidad`, `codMunicipio` (INE) y `fuente: { dataset, curso }`. La búsqueda incluye la localidad.
- **Atribución.** "Fuente: Junta de Andalucía, CC BY 4.0" en el detalle de cada centro y en la nueva pantalla "Acerca de" (`cuenta/acerca-de`).
- **Enseñanzas.** En el MVP, solo por nivel y modalidad (el dataset no trae los ciclos concretos). Corregido el ejemplo del doc 02.
- **Carga.** Primero `--dry-run` y emulador; después Firestore real, empezando por Huelva. Las credenciales se leen de `GOOGLE_APPLICATION_CREDENTIALS` o de `--credenciales`, nunca de una ruta fija ni de un archivo dentro del repo.
