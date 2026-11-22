package com.valoralo.ui.compare

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.valoralo.domain.usecase.MIN_RESENAS_FIABLE
import com.valoralo.domain.usecase.textoResenas
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.valoralo.data.repository.CentroRepository
import com.valoralo.domain.model.Centro
import com.valoralo.domain.usecase.Celda
import com.valoralo.domain.usecase.Comparacion
import com.valoralo.domain.usecase.FiltroRol
import com.valoralo.domain.usecase.MAX_CENTROS_COMPARADOR
import com.valoralo.domain.usecase.MIN_RESENAS_PARA_DESTACAR
import com.valoralo.domain.usecase.comparar
import com.valoralo.domain.usecase.formatearNota
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

sealed interface CompareUiState {
    data object Cargando : CompareUiState
    data object Error : CompareUiState
    data class Listo(val centros: List<Centro>, val filtro: FiltroRol, val comparacion: Comparacion) : CompareUiState
}

@HiltViewModel
class CompareViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repo: CentroRepository,
) : ViewModel() {

    private val codigos: List<String> = savedStateHandle.get<String>(ARG_IDS).orEmpty()
        .split(",").filter { it.matches(Regex("\\d{8}")) }.distinct().take(MAX_CENTROS_COMPARADOR)
    private val filtro = MutableStateFlow(FiltroRol.TODOS)

    // Full centros, live (2–3 reads): stats by role, departments and teachings.
    val estado: StateFlow<CompareUiState> = combine(
        if (codigos.isEmpty()) flowOf(emptyList())
        else combine(codigos.map { repo.observarCentro(it) }) { it.toList().filterNotNull() },
        filtro,
    ) { centros, f -> CompareUiState.Listo(centros, f, comparar(centros, f)) as CompareUiState }
        .catch { emit(CompareUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompareUiState.Cargando)

    fun onFiltro(f: FiltroRol) = filtro.update { f }

    companion object {
        const val ARG_IDS = "ids"
    }
}

private const val ANCHO_ETIQUETA = 1.3f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    onVolver: () -> Unit,
    onVerCentro: (String) -> Unit,
    viewModel: CompareViewModel = hiltViewModel(),
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comparar centros") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                windowInsets = WindowInsets(0),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        when (val e = s) {
            CompareUiState.Cargando -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            CompareUiState.Error -> Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), Alignment.Center) {
                Text("No se han podido cargar los centros. Comprueba tu conexión.")
            }
            is CompareUiState.Listo -> if (e.centros.size < 2) {
                Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), Alignment.Center) {
                    Text("Elige al menos 2 centros con el botón \"Comparar\".", textAlign = TextAlign.Center)
                }
            } else {
                Tabla(e, viewModel::onFiltro, onVerCentro, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun Tabla(s: CompareUiState.Listo, onFiltro: (FiltroRol) -> Unit, onVerCentro: (String) -> Unit, modifier: Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        item {
            // 1.0.8: each centro's number of reviews (for the chosen role) once, in its header, not in every cell.
            val resenas = s.comparacion.notas.firstOrNull()?.celdas?.map { it.n }.orEmpty()
            Row(verticalAlignment = Alignment.Bottom) {
                Spacer(ANCHO_ETIQUETA)
                s.centros.forEachIndexed { i, c ->
                    Column(
                        Modifier.weight(1f).clickable(onClickLabel = "ver el centro") { onVerCentro(c.codigo) }.padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            c.nombre,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            textoResenas(resenas.getOrElse(i) { 0 }),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FiltroRol.entries.forEach { f ->
                    FilterChip(selected = f == s.filtro, onClick = { onFiltro(f) }, label = { Text(f.etiqueta()) })
                }
            }
        }
        item { Seccion("Notas") }
        items(s.comparacion.notas) { FilaNotas(it.etiqueta, it.celdas) }
        item {
            Text(
                "La mejor nota de cada fila se destaca en negrita solo si todos los centros tienen al menos $MIN_RESENAS_PARA_DESTACAR reseñas. En gris, notas con menos de $MIN_RESENAS_FIABLE reseñas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        item { Seccion("Enseñanzas") }
        items(s.comparacion.ensenanzas) { fila ->
            FilaBase(fila.nivel) {
                fila.tiene.forEach { tiene ->
                    Text(
                        if (tiene) "✓" else "—",
                        Modifier.weight(1f).clearAndSetSemantics { contentDescription = if (tiene) "Sí" else "No" },
                        textAlign = TextAlign.Center,
                        color = if (tiene) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (s.comparacion.departamentos.isNotEmpty()) {
            item { Seccion("Profesorado por departamento") }
            items(s.comparacion.departamentos) { FilaNotas(it.etiqueta, it.celdas) }
            item {
                Text(
                    "«—»: el centro no tiene ese departamento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String) {
    Text(
        titulo,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).semantics { heading() },
    )
    HorizontalDivider()
}

@Composable
private fun FilaBase(etiqueta: String, celdas: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, Modifier.weight(ANCHO_ETIQUETA), style = MaterialTheme.typography.bodyMedium)
        celdas()
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun FilaNotas(etiqueta: String, celdas: List<Celda>) {
    FilaBase(etiqueta) {
        celdas.forEach { c ->
            val texto = when {
                !c.disponible -> "—"
                c.nota == null -> "Sin datos"
                else -> formatearNota(c.nota)
            }
            val pocas = c.nota != null && c.n < MIN_RESENAS_FIABLE
            val descripcion = when {
                !c.disponible -> "No tiene este departamento"
                c.nota == null -> "Sin valoraciones"
                else -> "${formatearNota(c.nota)} de 5, ${textoResenas(c.n)}" + (if (pocas) ", pocas reseñas" else "") + if (c.mejor) ", la mejor" else ""
            }
            Text(
                texto,
                Modifier.weight(1f).clearAndSetSemantics { contentDescription = descripcion },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (c.mejor) FontWeight.Bold else FontWeight.Normal,
                color = if (c.nota == null || pocas) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Spacer(peso: Float) {
    Box(Modifier.weight(peso))
}

private fun FiltroRol.etiqueta() = when (this) {
    FiltroRol.TODOS -> "Todos"
    FiltroRol.DOCENTES -> "Docentes"
    FiltroRol.ESTUDIANTES -> "Estudiantes"
    FiltroRol.OTROS -> "Otros"
}
