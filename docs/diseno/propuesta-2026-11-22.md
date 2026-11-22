# Auditoría de diseño y propuesta de mejoras — Valoralo 1.0.7 — 22/11/2026

Revisión de todas las pantallas de la app y del panel web, en modo claro y oscuro, con los **centros y reseñas ficticios**
de los emuladores de Firebase (versión debug 1.0.7, Pixel 5, Android 16). **No se ha cambiado código ni desplegado nada.**

- **Capturas:** `docs/diseno/capturas/` (70 JPG, 540 px de ancho, unos 3,9 MB). Se citan como `[NN-nombre]`; casi todas
  existen en `-claro` y `-oscuro`.
- **Referencias:** los bocetos de `docs/bocetos/` (listado de centros y detalle) y las guías de Material 3
  (jerarquía, tamaño táctil mínimo de 48 dp, contraste 4,5:1 en texto y 3:1 en elementos gráficos, estados vacíos).
- **Esfuerzo:** **P** (pequeño: horas, un archivo o dos), **M** (medio: uno o dos días), **G** (grande: varios días,
  backend o diseño nuevo). **Riesgo:** de romper algo existente. **Impacto:** para el usuario.
- **Nota:** la búsqueda «Valle» del comparador eligió el CEIP Adriano del Valle (real, **sin reseñas**) en lugar del
  ficticio CEIP Valle Claro; no hay opiniones inventadas sobre él en ninguna captura.

## 1. Resumen

La app es **sólida y coherente**: Material 3 bien aplicado, modo oscuro completo y correcto en todas las pantallas
(incluido el panel), textos claros y en español, estrellas con zona táctil de 48 dp, y la estructura de los bocetos se
respeta (barra de búsqueda + filtros + tarjetas con «Ver más» y «Comparar»; detalle con información, oferta académica
y reseñas). Lo que más se nota al usarla por primera vez:

1. **Lo importante del centro aparece tarde.** En el detalle, la ilustración ocupa un tercio de la pantalla y las notas
   empiezan después de Información y Oferta académica: hay que desplazarse tres pantallas para leer la primera reseña.
2. **Buscar no ayuda a decidir.** No se puede ordenar (mejor valorados, más reseñas), no se ve cuántos resultados hay y
   la lista empieza por centros de educación especial por orden alfabético.
3. **Estados vacíos y de error «secos»:** texto solo, sin icono ni acción (sin conexión, sin resultados, sin reseñas).
4. **Algunos detalles de jerarquía y accesibilidad:** «Eliminar cuenta» en medio de la lista de Mi cuenta, barras de
   notas de dos colores que no se explican, botones que se parten con letra grande, horizontal casi inutilizable.

## 2. Auditoría por pantalla

### Inicio `[01-inicio] [27-inicio-con-favoritos] [00-primer-arranque-carga]`
- ✅ El carrusel es lo primero que se ve y cumple la idea del producto (opiniones reales). Borde por rol bien visible.
- ⚠️ Sin favoritos, debajo del botón «Buscar un centro» queda **media pantalla vacía** `[01]`; no hay nada que invite a
  explorar (por ejemplo, centros mejor valorados o con reseñas recientes).
- ⚠️ El extracto del carrusel se corta en 3 líneas con «…» y el resto de la tarjeta queda en blanco: la altura fija
  desperdicia espacio `[01]`.
- ⚠️ Primer arranque: solo el logo sobre azul durante la descarga del catálogo `[00]` (unos 6 s en debug); no se dice
  qué está pasando.
- ℹ️ La cabecera «Valoralo» con el icono coincide con el boceto.

### Buscar y filtros `[02-buscar] [03-filtros] [04-buscar-sin-resultados] [08-buscar-bandeja-comparar]`
- ✅ Igual que el boceto. Hoja de filtros clara, con secciones plegables y «Ver 762 centros» (cuenta en vivo).
- ⚠️ **Sin orden** y sin número de resultados visible en la lista; el orden alfabético pone los CEEE primero.
- ⚠️ Tarjeta: «– ★ (sin reseñas)» con un guion suelto se lee raro; «Ver más» duplica pulsar la tarjeta.
- ⚠️ Sin resultados: solo una frase `[04]`; no ofrece «Quitar filtros» si no hay filtros, ni sugerencias.
- ✅ Bandeja «3 centros para comparar · Vaciar · Comparar» clara `[08]`.

### Detalle del centro `[05-detalle-arriba] [06-detalle-notas] [07-detalle-resenas] [21-detalle-tu-valoracion] [30-detalle-sin-resenas] [31-detalle-sin-conexion]`
- ⚠️ **Jerarquía:** la ilustración (genérica por tipo) ocupa ~35 % de la primera pantalla; la nota global es pequeña
  frente al nombre; Información y Oferta académica van antes que las notas `[05] [06]`. El boceto ponía la puntuación
  arriba y la información en una columna compacta.
- ⚠️ Barra superior: muestra «Valoralo» en vez del nombre del centro; al desplazar se pierde de qué centro se trata.
- ⚠️ **Barras de nota de dos colores** (azul + tramo verde/claro al final) `[06]`: es el «track» del indicador de
  progreso de Material 3 expresivo; parece una segunda serie de datos y no se explica.
- ⚠️ «Profesorado por departamento» lista hasta 18 filas con barra `[07]`: muy largo antes de llegar a las reseñas.
- ⚠️ El botón flotante tapa contenido a media lista (la tercera barra de notas, el like de «Tu valoración») `[06] [21]`.
- ⚠️ Sin reseñas: «Aún no hay valoraciones.» `[30]`, sin invitar a escribir la primera.
- ⚠️ Sin conexión: solo texto centrado, sin icono ni «Reintentar» `[31]`.
- ✅ Dirección como botón, fuente oficial y aviso de independencia bien colocados (1.0.7).

### Formulario de valoración `[22-formulario-arriba] [23-formulario-detalles] [24-formulario-texto-bloqueado] [25-formulario-descartar]`
- ✅ Estrellas grandes (48 dp), «Detallar (opcional)» desplegable, aviso de descartar cambios, resaltado del fragmento
  dentro del campo y en el recuadro «Revisa lo marcado» `[24]`.
- ⚠️ Formulario muy largo para un IES (18 departamentos con 5 estrellas cada uno): el texto, que es lo más valioso,
  está al final. No hay indicación de progreso ni resumen antes de publicar.
- ⚠️ En oscuro, el recuadro de error es rojo intenso a pantalla casi completa `[24-oscuro]`; pesa mucho visualmente.

### Comparador `[09-comparador] [10-comparador-abajo]`
- ✅ Tabla clara, filtro por rol, leyenda de qué significa el destacado.
- ⚠️ Cabeceras de columna con el nombre en 3 líneas, color primario y tamaño pequeño `[09]`; no fijas al desplazar.
- ⚠️ «sin val.» abreviado y «‹—›: el centro no tiene ese departamento» `[10]` cuestan de entender.
- ⚠️ Las notas no se distinguen visualmente (no hay barra ni color); el destacado de la mejor solo aparece con ≥ 3 reseñas.

### Favoritos `[11-favoritos-sin-sesion] [26-favoritos]`
- ✅ Estado sin sesión correcto con acción.
- ⚠️ Con favoritos, la lista es igual que Buscar; no muestra novedades (reseñas nuevas desde la última visita).

### Mi cuenta `[12-cuenta-sin-sesion] [15-mi-cuenta] [16-mi-cuenta-abajo]`
- ⚠️ **«Eliminar cuenta» en medio de la lista**, entre «Autores bloqueados» y «Condiciones de uso» `[15]`; lo habitual
  (y más seguro) es al final, separado. «Cerrar sesión» queda debajo, fuera de la primera pantalla.
- ⚠️ No hay acceso a «Mis valoraciones» (las reseñas propias, su estado y sus likes): hay que recordar en qué centros
  se escribió.
- ✅ Sin sesión: explicación breve y dos botones claros `[12]`.

### Inicio de sesión y registro `[13-login] [14-registro]`
- ✅ Limpios. Google primero, separador «o con tu correo», enlaces de recuperación.
- ⚠️ Registro: el formulario completo en una pantalla larga; la casilla de condiciones y el botón quedan abajo.

### Verificación docente `[17-verificacion] [18-verificacion-abajo]`
- ⚠️ Dos bloques de texto largos antes del formulario; «Enviar solicitud» queda fuera de la primera pantalla.
  Mejor: un resumen en 3 pasos con iconos y el detalle de privacidad plegable.

### Acerca de `[19-acerca-de] [20-acerca-de-abajo]`
- ✅ Aviso de independencia arriba y fuentes oficiales (1.0.7). Bien.
- ℹ️ Muchos `TextButton` seguidos con mucho espacio vertical; podrían ir en una lista con iconos como Mi cuenta.

### Panel web de administración `[34-panel-entrada] [35-panel-sin-acceso] [36-panel-verificaciones] [37-panel-verificacion-acciones] [38-panel-revision] [39-panel-revision-acciones]`
- ✅ Usable en móvil, modo oscuro, checklist de verificación en amarillo, fragmento resaltado en la reseña retenida.
- ⚠️ «Cerrar sesión» visible **antes de iniciar sesión** `[34]`.
- ⚠️ «hace 0 días» en lugar de «hoy» `[36]`.
- ⚠️ Pestañas y acciones se mezclan: «Actualizar» y «Recalcular destacadas ahora» tienen el mismo estilo que las pestañas `[36]`.
- ⚠️ El uid completo ocupa espacio sin utilidad para el moderador `[37] [39]`.

### Transversal: accesibilidad, letra grande y horizontal `[32-inicio-letra-150] [33-cuenta-letra-150] [28-inicio-horizontal] [29-buscar-horizontal]`
- ✅ Contraste de textos y bordes correcto (medido en la 1.0.5); TalkBack: botones principales con nombre (1.0.6–1.0.7).
- ⚠️ Con letra al 150 %, «Comparar» se parte en «Compara / r» en la tarjeta `[32]`; el extracto del carrusel baja a
  2 líneas.
- ⚠️ Filas de información del detalle (teléfono, correo) de ~36 dp de alto: por debajo de 48 dp.
- ⚠️ Horizontal: la barra inferior y la cabecera dejan menos de la mitad útil `[28]` (ya anotado en la QA, hallazgo 8).
- ⚠️ Estrellas amarillas sobre fondo claro: 1,96:1 (QA, hallazgo 17), pendiente.

## 3. Ideas de funcionalidad (qué echaría en falta alguien nuevo)

| Idea | Estudiante | Familia | Docente | Valoración |
|---|:-:|:-:|:-:|---|
| **Ordenar resultados** (mejor valorados, más reseñas, nombre) | ✔ | ✔✔ | ✔ | Imprescindible; barato (el catálogo ya trae nota y nº de reseñas). |
| **Compartir un centro** (enlace) | ✔ | ✔✔ | ✔ | Barato; muy útil entre familias. Requiere un enlace web que abra la app o una página pública mínima. |
| **Onboarding breve** (3 pantallas: qué es, cómo se valora sin nombres, verificación docente) | ✔ | ✔ | ✔✔ | Ayuda a entender las reglas antes de escribir; reduce textos bloqueados. |
| **Tema claro/oscuro/sistema en la app** | ✔ | ✔ | ✔ | Barato; el modo oscuro ya funciona. |
| **Mis valoraciones** en Mi cuenta | ✔✔ | ✔ | ✔✔ | Saber dónde has escrito, estado (en revisión, oculta) y likes. |
| **Centros cerca de mí / mapa** | ✔ | ✔✔ | ✔ | Muy pedido por familias, pero **exige permiso de ubicación** (cambia el formulario de datos) o elegir el punto en un mapa. Las coordenadas ya están. Grande. |
| **Resumen de puntos fuertes y débiles** | ✔ | ✔✔ | ✔ | Versión barata: destacar la mejor y la peor dimensión y departamentos (ya calculado). Versión con texto automático: necesita un modelo de lenguaje y moderación; riesgo de contenido. |
| **Avisos cuando un favorito recibe reseñas** | ✔ | ✔✔ | ✔ | Requiere notificaciones push (permiso en Android 13+, FCM, cambia el formulario de datos) o, más simple, un indicador «nuevas» dentro de la app. |
| **Respuesta del centro / del docente** | | ✔ | ✔✔ | Interesante, pero requiere verificar cuentas de centro. Para más adelante. |
| **Filtro «tiene reseñas»** y **enseñanza concreta** (p. ej. un ciclo de FP) | ✔✔ | ✔ | | El dataset solo trae niveles; los ciclos concretos no están. |

## 4. Propuesta ordenada

Cada mejora: **qué cambia · por qué · captura actual · esfuerzo · riesgo · impacto**.

### A. Rápidas y de mucho impacto (1.0.8)

| # | Qué cambia | Por qué | Captura | Esf. | Riesgo | Impacto |
|---|---|---|---|:-:|:-:|:-:|
| A1 | **Ordenar en Buscar:** selector «Relevancia · Mejor valorados · Más reseñas · Nombre» (chips o menú) y número de resultados («42 centros»). Desempate: con menos de 3 reseñas, después. | Es la pregunta principal de una familia; hoy el orden alfabético empieza por CEEE. Datos ya en el catálogo. | `02-buscar` | P | Bajo | Alto |
| A2 | **Detalle: nota arriba y ilustración más baja.** Ilustración a 16:6 (o miniatura junto al nombre), nota global grande con nº de reseñas y botón «Ver reseñas» que salta a la lista; Información y Oferta académica después de las notas. | Lo que se busca al abrir un centro es la nota y las opiniones; hoy hay que desplazarse 3 pantallas. Se acerca al boceto. | `05-detalle-arriba`, `06-detalle-notas` | M | Bajo | Alto |
| A3 | **Nombre del centro en la barra superior** al desplazar (barra que muestra el título cuando el nombre sale de la pantalla). | Orientación: en la lista de reseñas no se sabe qué centro es. | `07-detalle-resenas` | P | Bajo | Medio |
| A4 | **Barras de nota de un solo color** (sin el tramo de «track» de otro color) y nota numérica alineada. | El tramo verde parece otro dato. | `06-detalle-notas` | P | Bajo | Medio |
| A5 | **Estados vacíos y de error con icono, texto y acción:** sin conexión → «Reintentar»; sin resultados → «Quitar filtros» / «Borrar búsqueda»; sin reseñas → «Sé el primero en valorar» con el botón. | Hoy son frases sueltas; Material 3 recomienda explicar y ofrecer la salida. | `04`, `30`, `31` | P | Bajo | Medio |
| A6 | **Mi cuenta: «Eliminar cuenta» al final**, separado y después de «Cerrar sesión». | Acción destructiva en medio de la lista. | `15-mi-cuenta` | P | Bajo | Medio |
| A7 | **Tarjeta de centro:** «Sin reseñas» sin guion ni estrella vacía; «Ver más» fuera (la tarjeta entera abre el detalle) y «Comparar» como icono con texto que no se parta. | Limpieza visual; arregla «Compara / r» con letra grande. | `02-buscar`, `32-inicio-letra-150` | P | Bajo | Medio |
| A8 | **Filas de información del detalle a 48 dp** (teléfono, correo, web, dirección). | Tamaño táctil mínimo. | `05-detalle-arriba` | P | Bajo | Medio |
| A9 | **Panel:** ocultar «Cerrar sesión» sin sesión; «hoy»/«ayer»; acciones separadas de las pestañas; uid plegado. | Pulido de la herramienta de moderación. | `34`, `36`, `39` | P | Bajo | Bajo |
| A10 | **Tema claro/oscuro/sistema** en Mi cuenta. | Barato y pedido a menudo; el modo oscuro ya está bien hecho. | `15-mi-cuenta` | P | Bajo | Medio |

### B. Para la siguiente versión (1.0.9–1.1)

| # | Qué cambia | Por qué | Captura | Esf. | Riesgo | Impacto |
|---|---|---|---|:-:|:-:|:-:|
| B1 | **Inicio con contenido:** debajo del carrusel, «Mejor valorados en tu provincia» y «Reseñas recientes» (del catálogo y `destacadas`), también sin favoritos. | Media pantalla vacía en la primera visita. | `01-inicio` | M | Bajo | Alto |
| B2 | **Compartir un centro** (menú de compartir con nombre, nota y enlace). Primero, enlace a una página web pública mínima del centro (Hosting); después, App Links para abrir la app. | Las familias se pasan centros por WhatsApp. | `05-detalle-arriba` | M | Medio (página pública indexable: revisar privacidad y moderación) | Alto |
| B3 | **Mis valoraciones** en Mi cuenta: lista de reseñas propias con estado y likes. | Hoy no hay forma de encontrarlas. | `16-mi-cuenta-abajo` | M | Bajo | Medio |
| B4 | **Onboarding de 3 pantallas** en la primera apertura (saltable): qué es Valoralo, «se valora el departamento, nunca a una persona», verificación docente. Durante la descarga del catálogo, con progreso. | Explica las reglas antes de escribir y aprovecha la espera del primer arranque. | `00-primer-arranque-carga` | M | Bajo | Medio |
| B5 | **Profesorado por departamento plegado** (los 5 con más reseñas y «Ver los 18») y **resumen de puntos fuertes y débiles** sin IA: mejor y peor dimensión y departamento, con su nota. | El detalle de un IES es larguísimo; el resumen responde rápido a «¿qué tal es?». | `07-detalle-resenas` | M | Bajo | Alto |
| B6 | **Comparador:** cabeceras fijas con nombre corto, «Sin datos» en vez de «sin val.», barra o color en cada nota. | Legibilidad de la tabla. | `09`, `10` | M | Bajo | Medio |
| B7 | **Formulario más corto:** departamentos plegados por defecto (solo los que el usuario elija), el texto antes, y un resumen antes de publicar. | Hoy un IES tiene 90 estrellas antes del texto. | `22-formulario-arriba` | M | Medio (cambia el flujo) | Medio |
| B8 | **Verificación docente en 3 pasos** con iconos y la privacidad plegable. | Muro de texto. | `17-verificacion` | P | Bajo | Bajo |
| B9 | **Estrellas con más contraste** en claro y número junto a las estrellas de cada reseña (QA 17). | 1,96:1 hoy. | `21-detalle-tu-valoracion` | P | Bajo | Medio |

### C. Para más adelante

| # | Qué cambia | Por qué | Esf. | Riesgo | Impacto |
|---|---|---|:-:|:-:|:-:|
| C1 | **Mapa y «cerca de mí».** Mapa con los centros filtrados; «cerca de mí» con ubicación aproximada y solo mientras se usa. | Muy útil para familias. Requiere permiso de ubicación, actualizar el formulario de seguridad de los datos y la política de privacidad, y una librería de mapas (o Google Maps SDK con clave). Alternativa sin permiso: elegir municipio o punto en el mapa. | G | Medio-alto | Alto |
| C2 | **Avisos de reseñas nuevas en favoritos.** Primero un indicador «nuevas» dentro de la app (sin permisos); después notificaciones push opcionales (FCM, permiso de notificaciones, Functions que agrupen avisos). | Retención. Push cambia el formulario de datos y la política. | G | Medio | Medio |
| C3 | **Horizontal y tabletas:** `NavigationRail` y detalle en dos columnas (QA 8). | Hoy casi inutilizable en horizontal. | M | Medio | Bajo |
| C4 | **Letra muy grande** (200 %): revisión completa de filas e insignias (QA 16). | Accesibilidad. | M | Bajo | Medio |
| C5 | **Resumen automático de opiniones con IA.** | Útil, pero exige moderación del resultado, coste por centro y explicar que es automático. | G | Alto | Medio |
| C6 | **Respuestas de los centros** con cuentas de centro verificadas. | Equilibrio entre opiniones y centros. | G | Medio | Medio |

## 5. Recomendación

Aprobar el grupo **A** completo para la **1.0.8**: son cambios de interfaz sin backend (salvo A9, que es del panel),
con poco riesgo, y resuelven lo que más se nota (ordenar, ver la nota del centro de un vistazo, estados vacíos). De B,
empezaría por **B1, B5 y B2** (contenido en Inicio, resumen del centro, compartir). C1 y C2 necesitan decidir antes los
permisos y el formulario de seguridad de los datos.
