package com.valoralo.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.valoralo.domain.usecase.FiltroCentros
import com.valoralo.domain.usecase.Lugar
import com.valoralo.domain.usecase.OpcionGrupo
import com.valoralo.domain.usecase.OpcionesFiltro
import com.valoralo.domain.usecase.agruparTipos
import androidx.compose.foundation.layout.heightIn
import com.valoralo.domain.usecase.alternar
import com.valoralo.domain.usecase.resumenSeleccion
import com.valoralo.domain.usecase.sugerir
import com.valoralo.domain.usecase.textoVerCentros

/** Most places drawn at once; the search narrows the rest. */
private const val MAX_CHIPS_VISIBLES = 40

private enum class Seccion(val titulo: String) {
    PROVINCIA("Provincia"),
    LUGAR("Municipio o localidad"),
    TIPO("Tipo de centro"),
    ENSENANZAS("Enseñanzas"),
}

/** Filters apply immediately (the count on the button is live); the sheet only collects them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltrosSheet(
    filtro: FiltroCentros,
    opciones: OpcionesFiltro,
    provinciasDeducidas: Set<String>,
    resultados: Int,
    onFiltroChange: (FiltroCentros) -> Unit,
    onLimpiar: () -> Unit,
    onDismiss: () -> Unit,
) {
    var abierta by rememberSaveable { mutableStateOf<Seccion?>(Seccion.PROVINCIA) }
    fun alternarSeccion(s: Seccion) { abierta = if (abierta == s) null else s }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column {
            Text(
                "Filtros",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp).semantics { heading() },
            )
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                val deducida = if (filtro.provincias.isEmpty() && provinciasDeducidas.isNotEmpty()) {
                    "${resumenSeleccion(provinciasDeducidas)} (por el municipio)"
                } else null
                SeccionPlegable(
                    Seccion.PROVINCIA, resumenSeleccion(filtro.provincias) ?: deducida, abierta == Seccion.PROVINCIA,
                    { alternarSeccion(Seccion.PROVINCIA) },
                ) {
                    Chips(opciones.provincias, filtro.provincias) { onFiltroChange(filtro.copy(provincias = filtro.provincias.alternar(it))) }
                }
                SeccionPlegable(
                    Seccion.LUGAR, resumenSeleccion(filtro.lugares.map { it.nombre }.toSet()), abierta == Seccion.LUGAR,
                    { alternarSeccion(Seccion.LUGAR) },
                ) {
                    BuscadorLugares(opciones.lugares, filtro.lugares) {
                        onFiltroChange(filtro.copy(lugares = filtro.lugares.alternar(it)))
                    }
                }
                // 1.0.8: groups with a readable name ("Colegios", "Institutos"…), the acronyms in small print.
                val grupos = agruparTipos(opciones.tipos)
                val gruposElegidos = grupos.filter { g -> g.siglas.any { it in filtro.tipos } }
                SeccionPlegable(
                    Seccion.TIPO, resumenSeleccion(gruposElegidos.map { it.nombre }.toSet()), abierta == Seccion.TIPO,
                    { alternarSeccion(Seccion.TIPO) },
                ) {
                    ChipsGrupos(grupos, filtro.tipos) { g ->
                        val todas = g.siglas.all { it in filtro.tipos }
                        onFiltroChange(filtro.copy(tipos = if (todas) filtro.tipos - g.siglas.toSet() else filtro.tipos + g.siglas))
                    }
                }
                SeccionPlegable(
                    Seccion.ENSENANZAS, resumenSeleccion(filtro.niveles), abierta == Seccion.ENSENANZAS,
                    { alternarSeccion(Seccion.ENSENANZAS) },
                ) {
                    Chips(opciones.niveles, filtro.niveles) { onFiltroChange(filtro.copy(niveles = filtro.niveles.alternar(it))) }
                }
            }

            // Fixed at the bottom, outside the scroll.
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = onLimpiar, enabled = filtro.hayActivos, modifier = Modifier.weight(1f)) { Text("Limpiar") }
                    Button(onClick = onDismiss, modifier = Modifier.weight(2f)) {
                        Text(textoVerCentros(resultados))
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionPlegable(
    seccion: Seccion,
    resumen: String?,
    expandida: Boolean,
    onAlternar: () -> Unit,
    contenido: @Composable () -> Unit,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = if (expandida) "Cerrar" else "Abrir", onClick = onAlternar)
                .padding(vertical = 14.dp)
                .semantics(mergeDescendants = true) {
                    heading()
                    stateDescription = if (expandida) "Abierta" else "Cerrada"
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(seccion.titulo, style = MaterialTheme.typography.titleMedium)
            // Only one weighted child: the summary takes the rest of the row, so the arrow is always at the end
            // (with two weights the arrow ended up in the middle when there was a summary).
            if (resumen != null) {
                Text(
                    " · $resumen",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            Icon(if (expandida) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
        }
        AnimatedVisibility(expandida) {
            Column(Modifier.padding(bottom = 12.dp)) { contenido() }
        }
        HorizontalDivider()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(opciones: List<String>, elegidas: Set<String>, onAlternar: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEach { opcion ->
            val activa = opcion in elegidas
            FilterChip(
                selected = activa,
                onClick = { onAlternar(opcion) },
                label = { Text(opcion) },
                leadingIcon = if (activa) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(0.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}

/** Type groups: the name, and the acronyms below in small print (CEIP · CEPR · CPR). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsGrupos(grupos: List<OpcionGrupo>, elegidos: Set<String>, onAlternar: (OpcionGrupo) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        grupos.forEach { g ->
            val activa = g.siglas.any { it in elegidos }
            FilterChip(
                selected = activa,
                onClick = { onAlternar(g) },
                label = {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(g.nombre)
                        if (g.grupo != null) {
                            Text(
                                g.siglas.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                leadingIcon = if (activa) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else null,
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

/**
 * One search box for municipios and localidades together, each with its context
 * ("Gines · Sevilla", "Aguadulce · Roquetas de Mar, Almería"). The chosen ones first.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuscadorLugares(opciones: List<Lugar>, elegidos: Set<Lugar>, onAlternar: (Lugar) -> Unit) {
    var texto by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Escribe tu pueblo o ciudad") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
        )
        val coinciden = opciones.sugerir(texto).filterNot { it in elegidos }
        val visibles = elegidos.toList() + coinciden.take(MAX_CHIPS_VISIBLES)
        if (visibles.isEmpty()) {
            Text("No hay ningún municipio ni localidad con ese nombre.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                visibles.forEach { lugar ->
                    val activo = lugar in elegidos
                    FilterChip(
                        selected = activo,
                        onClick = { onAlternar(lugar) },
                        label = { Text(lugar.etiqueta) },
                        leadingIcon = if (activo) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else null,
                    )
                }
            }
        }
        val ocultas = coinciden.size - MAX_CHIPS_VISIBLES
        if (ocultas > 0) {
            Text(
                "Y $ocultas más: escribe para encontrarlas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
