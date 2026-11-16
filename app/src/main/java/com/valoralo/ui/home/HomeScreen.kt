package com.valoralo.ui.home

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
import com.valoralo.domain.model.Centro
import com.valoralo.domain.usecase.FiltroCentros
import com.valoralo.ui.components.FotoCentro
import com.valoralo.ui.components.LogoValoralo
import com.valoralo.ui.components.NotaMedia

@Composable
fun HomeScreen(
    onVerCentro: (codigo: String) -> Unit,
    onComparar: (Centro) -> Unit,
    enfocarBuscador: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        enfocarBuscador = enfocarBuscador,
        onTextoChange = viewModel::onTextoChange,
        onFiltroChange = viewModel::onFiltroChange,
        onLimpiarFiltros = viewModel::limpiarFiltros,
        onVerCentro = onVerCentro,
        onComparar = onComparar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    enfocarBuscador: Boolean,
    onTextoChange: (String) -> Unit,
    onFiltroChange: (FiltroCentros) -> Unit,
    onLimpiarFiltros: () -> Unit,
    onVerCentro: (String) -> Unit,
    onComparar: (Centro) -> Unit,
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
            enfocar = enfocarBuscador,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))

        when {
            state.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
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
                        onVerMas = { onVerCentro(centro.codigo) },
                        onComparar = { onComparar(centro) },
                    )
                }
            }
        }
    }

    if (mostrarFiltros) {
        FiltrosSheet(
            filtro = state.filtro,
            opciones = state.opciones,
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
        LogoValoralo(Modifier.weight(1f))
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
    enfocar: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(enfocar) { if (enfocar) focusRequester.requestFocus() }

    OutlinedTextField(
        value = texto,
        onValueChange = onTextoChange,
        modifier = modifier.fillMaxWidth().focusRequester(focusRequester),
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

@Composable
private fun TarjetaCentro(
    centro: Centro,
    onVerMas: () -> Unit,
    onComparar: () -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onVerMas)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FotoCentro(centro.fotoUrl, Modifier.size(96.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    centro.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    centro.localidad(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                NotaMedia(centro.stats.total.global, centro.stats.total.n)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onVerMas, contentPadding = ButtonDefaults.TextButtonContentPadding) {
                        Text("Ver más")
                    }
                    OutlinedButton(onClick = onComparar, contentPadding = ButtonDefaults.TextButtonContentPadding) {
                        Text("Comparar")
                    }
                }
            }
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

/** "Dos Hermanas (Sevilla)" or just "Sevilla" when municipio and provincia match. */
fun Centro.localidad(): String =
    if (municipio == provincia) municipio else "$municipio ($provincia)"
