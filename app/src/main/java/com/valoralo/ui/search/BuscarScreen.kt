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
import com.valoralo.ui.components.EstadoVacio
import com.valoralo.domain.usecase.OrdenCentros
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.semantics.contentDescription
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
        onOrdenChange = viewModel::onOrdenChange,
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
    onOrdenChange: (OrdenCentros) -> Unit,
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
            state.error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EstadoVacio(
                    Icons.Outlined.CloudOff,
                    "No se han podido cargar los centros",
                    "Comprueba tu conexión. Se cargarán solos en cuanto vuelvas a tener red.",
                )
            }
            state.centros.isEmpty() -> SinResultados(
                hayTexto = state.texto.isNotBlank(),
                hayFiltros = state.filtro.hayActivos,
                onBorrarTexto = { onTextoChange("") },
                onLimpiarFiltros = onLimpiarFiltros,
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 1.0.8: how many results and how they are ordered.
                item(key = "resultados") { FilaResultados(state.centros.size, state.orden, onOrdenChange) }
                items(state.centros, key = { it.codigo }) { centro ->
                    TarjetaCentro(
                        centro = centro,
                        esFavorito = centro.codigo in favoritos,
                        enComparador = centro.codigo in seleccion,
                        onAbrir = { onVerCentro(centro.codigo) },
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
        // Same 16 dp gutter as the search field below (the badge sits over the button's corner).
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
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
private fun SinResultados(hayTexto: Boolean, hayFiltros: Boolean, onBorrarTexto: () -> Unit, onLimpiarFiltros: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EstadoVacio(
            Icons.Outlined.SearchOff,
            "No hay centros que coincidan",
            when {
                hayFiltros && hayTexto -> "Prueba con otro nombre o quita algún filtro."
                hayFiltros -> "Prueba quitando algún filtro."
                else -> "Prueba con otro nombre, o con el municipio o la localidad."
            },
            accion = if (hayFiltros) "Quitar filtros" else if (hayTexto) "Borrar búsqueda" else null,
            onAccion = if (hayFiltros) onLimpiarFiltros else onBorrarTexto,
        )
    }
}

/** "42 centros" and the order menu (1.0.8). */
@Composable
internal fun FilaResultados(n: Int, orden: OrdenCentros, onOrdenChange: (OrdenCentros) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            textoCentros(n),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Box {
            TextButton(onClick = { abierto = true }, modifier = Modifier.semantics { contentDescription = "Ordenar por: ${orden.etiqueta}" }) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(orden.etiqueta)
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                OrdenCentros.entries.forEach { o ->
                    DropdownMenuItem(
                        text = { Text(o.etiqueta) },
                        onClick = { onOrdenChange(o); abierto = false },
                        leadingIcon = if (o == orden) { { Icon(Icons.Filled.Check, contentDescription = null) } } else null,
                    )
                }
            }
        }
    }
}

internal fun textoCentros(n: Int): String = when (n) {
    1 -> "1 centro"
    else -> "${"%,d".format(java.util.Locale.forLanguageTag("es-ES"), n)} centros"
}
