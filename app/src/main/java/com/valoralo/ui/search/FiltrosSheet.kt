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
import com.valoralo.domain.usecase.OpcionesFiltro
import com.valoralo.domain.usecase.alternar
import com.valoralo.domain.usecase.buscar
import com.valoralo.domain.usecase.resumenSeleccion
import com.valoralo.domain.usecase.textoVerCentros

/** Most chips drawn at once in a searchable section; the search narrows the rest. */
private const val MAX_CHIPS_VISIBLES = 40

private enum class Seccion(val titulo: String) {
    PROVINCIA("Provincia"),
    CIUDAD("Ciudad"),
    LOCALIDAD("Localidad"),
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
                    "${resumenSeleccion(provinciasDeducidas)} (por la ciudad)"
                } else null
                SeccionPlegable(
                    Seccion.PROVINCIA, resumenSeleccion(filtro.provincias) ?: deducida, abierta == Seccion.PROVINCIA,
                    { alternarSeccion(Seccion.PROVINCIA) },
                ) {
                    Chips(opciones.provincias, filtro.provincias) { onFiltroChange(filtro.copy(provincias = filtro.provincias.alternar(it))) }
                }
                SeccionPlegable(
                    Seccion.CIUDAD, resumenSeleccion(filtro.municipios), abierta == Seccion.CIUDAD,
                    { alternarSeccion(Seccion.CIUDAD) },
                ) {
                    ChipsConBuscador("Buscar ciudad", opciones.municipios, filtro.municipios) {
                        onFiltroChange(filtro.copy(municipios = filtro.municipios.alternar(it)))
                    }
                }
                SeccionPlegable(
                    Seccion.LOCALIDAD, resumenSeleccion(filtro.localidades), abierta == Seccion.LOCALIDAD,
                    { alternarSeccion(Seccion.LOCALIDAD) },
                ) {
                    ChipsConBuscador("Buscar localidad", opciones.localidades, filtro.localidades) {
                        onFiltroChange(filtro.copy(localidades = filtro.localidades.alternar(it)))
                    }
                }
                SeccionPlegable(
                    Seccion.TIPO, resumenSeleccion(filtro.tipos), abierta == Seccion.TIPO,
                    { alternarSeccion(Seccion.TIPO) },
                ) {
                    Chips(opciones.tipos, filtro.tipos) { onFiltroChange(filtro.copy(tipos = filtro.tipos.alternar(it))) }
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
            if (resumen != null) {
                Text(
                    " · $resumen",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                Icon(if (expandida) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            }
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

/** Long lists (ciudades, localidades): the chosen ones first, then what matches the search. */
@Composable
private fun ChipsConBuscador(etiqueta: String, opciones: List<String>, elegidas: Set<String>, onAlternar: (String) -> Unit) {
    var texto by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(etiqueta) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
        )
        val coinciden = opciones.buscar(texto).filterNot { it in elegidas }
        val visibles = elegidas.filter { it in opciones } + coinciden.take(MAX_CHIPS_VISIBLES)
        if (visibles.isEmpty()) {
            Text("No hay ninguna con ese nombre.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Chips(visibles, elegidas, onAlternar)
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
