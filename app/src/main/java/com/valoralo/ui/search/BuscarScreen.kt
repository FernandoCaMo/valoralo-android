package com.valoralo.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoralo.data.repository.ProgresoDescarga
import com.valoralo.domain.model.Centro
import com.valoralo.domain.usecase.FiltroCentros
import com.valoralo.ui.components.AccionesCentroViewModel
import com.valoralo.ui.components.TarjetaCentro
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

/** Buscar: the catalogue list with the search box and the filters. */
@Composable
fun BuscarScreen(
    onVerCentro: (codigo: String) -> Unit,
    acciones: AccionesCentroViewModel,
    viewModel: BuscarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favoritos by acciones.favoritos.collectAsStateWithLifecycle()
    val seleccion by acciones.seleccion.collectAsStateWithLifecycle()
    BuscarContent(
        state = state,
        onTextoChange = viewModel::onTextoChange,
        onFiltroChange = viewModel::onFiltroChange,
        onLimpiarFiltros = viewModel::limpiarFiltros,
        onVerCentro = onVerCentro,
        favoritos = favoritos.orEmpty(),
        seleccion = seleccion,
        onFavorito = acciones::alternarFavorito,
        onComparar = acciones::alternarComparar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuscarContent(
    state: BuscarUiState,
    onTextoChange: (String) -> Unit,
    onFiltroChange: (FiltroCentros) -> Unit,
    onLimpiarFiltros: () -> Unit,
    onVerCentro: (String) -> Unit,
    favoritos: Set<String>,
    seleccion: List<String>,
    onFavorito: (String) -> Unit,
    onComparar: (String) -> Unit,
) {
    var mostrarFiltros by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Cabecera(
            filtrosActivos = state.filtro.activos,
            onFiltros = { mostrarFiltros = true },
        )
        Buscador(
            texto = state.texto,
            onTextoChange = onTextoChange,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))

        when {
            state.cargando -> Descargando(state.progreso)
            state.error -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No se han podido cargar los centros. Comprueba tu conexión e inténtalo de nuevo.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
            state.centros.isEmpty() -> SinResultados(
                hayFiltros = state.filtro.hayActivos,
                onLimpiarFiltros = onLimpiarFiltros,
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.centros, key = { it.codigo }) { centro ->
                    TarjetaCentro(
                        centro = centro,
                        esFavorito = centro.codigo in favoritos,
                        enComparador = centro.codigo in seleccion,
                        onVerMas = { onVerCentro(centro.codigo) },
                        onFavorito = { onFavorito(centro.codigo) },
                        onComparar = { onComparar(centro.codigo) },
                    )
                }
            }
        }
    }

    if (mostrarFiltros) {
        FiltrosSheet(
            filtro = state.filtro,
            opciones = state.opciones,
            provinciasDeducidas = state.provinciasDeducidas,
            resultados = state.centros.size,
            onFiltroChange = onFiltroChange,
            onLimpiar = onLimpiarFiltros,
            onDismiss = { mostrarFiltros = false },
        )
    }
}

@Composable
private fun Cabecera(filtrosActivos: Int, onFiltros: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Buscar centros",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        BadgedBox(
            badge = { if (filtrosActivos > 0) Badge { Text("$filtrosActivos") } },
        ) {
            OutlinedButton(onClick = onFiltros) {
                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Filtros")
            }
        }
    }
}

@Composable
private fun Buscador(
    texto: String,
    onTextoChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = texto,
        onValueChange = onTextoChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Buscar centro educativo…") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = { onTextoChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
    )
}

/** First launch: the catalogue is being downloaded (one document per provincia). */
@Composable
private fun Descargando(progreso: ProgresoDescarga?) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        if (progreso == null || progreso.total == 0) {
            CircularProgressIndicator()
        } else {
            LinearProgressIndicator(
                progress = { progreso.hechas.toFloat() / progreso.total },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text("Descargando centros…", style = MaterialTheme.typography.titleMedium)
        if (progreso != null && progreso.total > 0) {
            Text(
                "${progreso.hechas} de ${progreso.total}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SinResultados(hayFiltros: Boolean, onLimpiarFiltros: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "No hay centros que coincidan con tu búsqueda.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (hayFiltros) {
            TextButton(onClick = onLimpiarFiltros) { Text("Quitar filtros") }
        }
    }
}
