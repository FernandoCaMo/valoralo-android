package com.valoralo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.valoralo.domain.usecase.FiltroCentros

/** Filters apply immediately; the sheet only collects them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltrosSheet(
    filtro: FiltroCentros,
    opciones: OpcionesFiltro,
    onFiltroChange: (FiltroCentros) -> Unit,
    onLimpiar: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filtros", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onLimpiar, enabled = filtro.hayActivos) { Text("Limpiar") }
            }

            GrupoChips("Provincia", opciones.provincias, filtro.provincia) {
                onFiltroChange(filtro.conProvincia(it))
            }
            if (opciones.municipios.isNotEmpty()) {
                GrupoChips("Municipio", opciones.municipios, filtro.municipio) {
                    onFiltroChange(filtro.copy(municipio = it))
                }
            }
            GrupoChips("Tipo de centro", opciones.tipos, filtro.tipo) {
                onFiltroChange(filtro.copy(tipo = it))
            }
            GrupoChips("Enseñanzas", opciones.niveles, filtro.nivel) {
                onFiltroChange(filtro.copy(nivel = it))
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Ver resultados") }
        }
    }
}

/** Single-choice chip group; tapping the selected chip clears it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GrupoChips(
    titulo: String,
    opciones: List<String>,
    seleccionada: String?,
    onSeleccion: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opciones.forEach { opcion ->
                val activa = opcion == seleccionada
                FilterChip(
                    selected = activa,
                    onClick = { onSeleccion(if (activa) null else opcion) },
                    label = { Text(opcion) },
                )
            }
        }
    }
}
