package com.valoralo.ui.review

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoralo.domain.model.EstadoValoracion
import com.valoralo.domain.model.Fragmento
import com.valoralo.domain.model.OrigenRevision
import com.valoralo.domain.usecase.FormularioValoracion
import com.valoralo.domain.usecase.LIMITE_TEXTO_VALORACION
import com.valoralo.domain.usecase.OtroDepartamento
import com.valoralo.ui.components.SelectorEstrellas

private val SUBNOTAS_CENTRO = listOf("gestion" to "Gestión", "convivencia" to "Convivencia", "comunicacion" to "Comunicación")
private val SUBNOTAS_INSTALACIONES = listOf(
    "aulas" to "Aulas", "equipamiento" to "Equipamiento", "limpieza" to "Limpieza", "accesibilidad" to "Accesibilidad",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onVolver: () -> Unit,
    onTerminado: (mensaje: String) -> Unit,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    var confirmarBorrado by rememberSaveable { mutableStateOf(false) }
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(s.terminado) { s.terminado?.let(onTerminado) }
    // Leaving with unsaved changes (back arrow or system back) asks first.
    val salir = { if (s.hayCambios) confirmarSalida = true else onVolver() }
    BackHandler(enabled = s.hayCambios) { confirmarSalida = true }
    // When the filter marks something, scroll to the warning (it is below the text field).
    val avisoFiltro = remember { BringIntoViewRequester() }
    LaunchedEffect(s.fragmentos, s.error) {
        if (s.fragmentos.isNotEmpty() || s.error != null) avisoFiltro.bringIntoView()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (s.existente != null) "Editar mi valoración" else "Valorar el centro") },
                navigationIcon = {
                    IconButton(onClick = salir) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                windowInsets = WindowInsets(0),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (s.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val f = s.formulario
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            s.centro?.let { Text(it.nombre, style = MaterialTheme.typography.titleLarge) }
            AvisoEstado(s.existente?.estado, s.existente?.revision?.origen, s.existente?.motivoModeracion)

            Bloque("Centro en general", obligatorio = true) {
                SelectorEstrellas(f.notaCentro, { n -> viewModel.cambiar { it.copy(notaCentro = n) } }, "Centro en general")
                Subnotas(SUBNOTAS_CENTRO, f.subnotasCentro) { clave, n ->
                    viewModel.cambiar { it.copy(subnotasCentro = it.subnotasCentro.conNota(clave, n)) }
                }
            }
            Bloque("Aulas e instalaciones", obligatorio = true) {
                SelectorEstrellas(f.notaInstalaciones, { n -> viewModel.cambiar { it.copy(notaInstalaciones = n) } }, "Aulas e instalaciones")
                Subnotas(SUBNOTAS_INSTALACIONES, f.subnotasInstalaciones) { clave, n ->
                    viewModel.cambiar { it.copy(subnotasInstalaciones = it.subnotasInstalaciones.conNota(clave, n)) }
                }
            }
            Bloque("Profesorado por departamento", obligatorio = false) {
                Text(
                    "Valora solo los departamentos que conozcas. Se valora el departamento, nunca a una persona.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                s.centro?.departamentos?.forEach { d ->
                    FilaDepartamento(d, f.departamentos[d], onNota = { n -> viewModel.cambiar { it.copy(departamentos = it.departamentos + (d to n)) } },
                        onQuitar = { viewModel.cambiar { it.copy(departamentos = it.departamentos - d) } })
                }
                f.otros.forEachIndexed { i, otro ->
                    OtroDepartamentoFila(otro, onCambio = { viewModel.cambiarOtro(i, it) }, onQuitar = { viewModel.quitarOtro(i) })
                }
                TextButton(onClick = viewModel::anadirOtro) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Otro departamento")
                }
            }
            Bloque("Tu opinión", obligatorio = false) {
                val colorMarca = MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                OutlinedTextField(
                    value = f.texto,
                    onValueChange = { if (it.length <= LIMITE_TEXTO_VALORACION + 50) viewModel.onTexto(it) },
                    modifier = Modifier.fillMaxWidth(),
                    // The marked fragments are also highlighted inside the field until the text changes.
                    visualTransformation = remember(s.fragmentos, colorMarca) { ResaltarFragmentos(s.fragmentos, colorMarca) },
                    minLines = 4,
                    isError = f.texto.trim().length > LIMITE_TEXTO_VALORACION,
                    supportingText = {
                        val sobran = f.texto.trim().length - LIMITE_TEXTO_VALORACION
                        Row(Modifier.fillMaxWidth()) {
                            // Too long: say so, instead of turning the names reminder red (QA 1.0.5, finding 10).
                            Text(
                                if (sobran > 0) textoSobran(sobran) else "Sin nombres de personas, insultos ni datos personales.",
                                Modifier.weight(1f),
                            )
                            Text("${f.texto.trim().length}/$LIMITE_TEXTO_VALORACION")
                        }
                    },
                )
            }

            Column(Modifier.bringIntoViewRequester(avisoFiltro), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (s.fragmentos.isNotEmpty()) Fragmentos(f.texto, s.fragmentos)
                s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }

            Button(onClick = viewModel::publicar, enabled = !s.enviando, modifier = Modifier.fillMaxWidth()) {
                Text(if (s.enviando) "Enviando…" else if (s.existente != null) "Guardar cambios" else "Publicar")
            }
            if (s.existente != null) {
                OutlinedButton(onClick = { confirmarBorrado = true }, enabled = !s.enviando, modifier = Modifier.fillMaxWidth()) {
                    Text("Borrar mi valoración", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (s.enRevision && !s.cargando && s.fragmentos.isNotEmpty() && s.terminado == null) {
        AlertDialog(
            onDismissRequest = viewModel::revisionVista,
            title = { Text("Tu reseña está en revisión") },
            text = {
                Text(
                    "Hemos marcado algo que podría ser un nombre de una persona o un insulto. Un moderador la " +
                        "revisará antes de publicarla. Si lo corriges y vuelves a guardar, se publicará directamente.",
                )
            },
            confirmButton = { TextButton(onClick = viewModel::revisionVista) { Text("Entendido") } },
        )
    }
    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Descartar los cambios?") },
            text = { Text("Si sales ahora, se perderán las notas y el texto que no has guardado.") },
            confirmButton = {
                TextButton(onClick = { confirmarSalida = false; onVolver() }) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Seguir editando") } },
        )
    }
    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Borrar tu valoración?") },
            text = { Text("Se borrarán tus notas y tu texto de este centro.") },
            confirmButton = {
                TextButton(onClick = { confirmarBorrado = false; viewModel.borrar() }) {
                    Text("Borrar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } },
        )
    }
}

/** "Te sobra 1 carácter." / "Te sobran 50 caracteres." */
internal fun textoSobran(n: Int): String = if (n == 1) "Te sobra 1 carácter." else "Te sobran $n caracteres."

private fun Map<String, Int>.conNota(clave: String, nota: Int?): Map<String, Int> =
    if (nota == null || this[clave] == nota) this - clave else this + (clave to nota)

@Composable
private fun AvisoEstado(estado: EstadoValoracion?, origen: OrigenRevision?, motivo: String?) {
    val texto = when (estado) {
        EstadoValoracion.EN_REVISION -> when (origen) {
            OrigenRevision.DENUNCIAS -> "Tu reseña está en revisión porque varias personas la han denunciado. Un moderador decidirá si se publica."
            OrigenRevision.MODERACION -> "Tu reseña está en revisión: un moderador la ocultó y tiene que revisar tus cambios antes de publicarla."
            else -> "Tu reseña está en revisión. Corrige lo marcado y se publicará al guardar, o espera a que la revise un moderador."
        }
        EstadoValoracion.OCULTA -> "Un moderador ha ocultado tu reseña" + (motivo?.let { " (motivo: $it)" } ?: "") +
            ". Si la editas, volverá a revisión y un moderador decidirá si se publica."
        else -> return
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Text(texto, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun Bloque(titulo: String, obligatorio: Boolean, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            if (obligatorio) titulo else "$titulo (opcional)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        contenido()
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun Subnotas(opciones: List<Pair<String, String>>, valores: Map<String, Int>, onNota: (String, Int?) -> Unit) {
    var abierto by rememberSaveable { mutableStateOf(valores.isNotEmpty()) }
    TextButton(onClick = { abierto = !abierto }) { Text(if (abierto) "Ocultar detalles" else "Detallar (opcional)") }
    if (abierto) {
        opciones.forEach { (clave, etiqueta) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(etiqueta, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                SelectorEstrellas(valores[clave], { onNota(clave, it) }, etiqueta, tamano = 22.dp)
            }
        }
    }
}

@Composable
private fun FilaDepartamento(nombre: String, nota: Int?, onNota: (Int) -> Unit, onQuitar: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(nombre, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            if (nota != null) {
                IconButton(onClick = onQuitar) { Icon(Icons.Filled.Close, contentDescription = "Quitar la nota de $nombre") }
            }
        }
        SelectorEstrellas(nota, onNota, nombre, tamano = 22.dp)
    }
}

@Composable
private fun OtroDepartamentoFila(otro: OtroDepartamento, onCambio: (OtroDepartamento) -> Unit, onQuitar: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = otro.nombre, onValueChange = { onCambio(otro.copy(nombre = it.take(40))) },
                label = { Text("Otro departamento") }, singleLine = true, modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onQuitar) { Icon(Icons.Filled.Close, contentDescription = "Quitar este departamento") }
        }
        SelectorEstrellas(otro.nota, { onCambio(otro.copy(nota = it)) }, otro.nombre.ifBlank { "Otro departamento" }, tamano = 22.dp)
    }
}

/** Background on the marked fragments inside the text field (same positions: no offset change). */
private class ResaltarFragmentos(private val fragmentos: List<Fragmento>, private val color: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val validos = fragmentos.filter { it.inicio >= 0 && it.fin <= text.length && it.inicio < it.fin }
        if (validos.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val marcado = buildAnnotatedString {
            append(text)
            validos.forEach { addStyle(SpanStyle(background = color, fontWeight = FontWeight.Bold), it.inicio, it.fin) }
        }
        return TransformedText(marcado, OffsetMapping.Identity)
    }

    override fun equals(other: Any?) = other is ResaltarFragmentos && other.fragmentos == fragmentos && other.color == color
    override fun hashCode() = fragmentos.hashCode() * 31 + color.hashCode()
}

/** Shows the text with the marked fragments highlighted, and the reason for each. */
@Composable
private fun Fragmentos(texto: String, fragmentos: List<Fragmento>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Revisa lo marcado:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
            val enTexto = fragmentos.filter { it.inicio >= 0 && it.fin <= texto.length }.sortedBy { it.inicio }
            if (enTexto.isNotEmpty()) {
                val resaltado = buildAnnotatedString {
                    var i = 0
                    for (fr in enTexto) {
                        if (fr.inicio < i) continue
                        append(texto.substring(i, fr.inicio))
                        withStyle(SpanStyle(background = MaterialTheme.colorScheme.error.copy(alpha = 0.25f), fontWeight = FontWeight.Bold)) {
                            append(texto.substring(fr.inicio, fr.fin))
                        }
                        i = fr.fin
                    }
                    append(texto.substring(i))
                }
                Text(resaltado, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            fragmentos.forEach {
                Text("• «${it.texto}»: ${it.explicacion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}
