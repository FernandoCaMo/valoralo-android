package com.valoralo.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.valoralo.data.repository.CentroRepository
import com.valoralo.domain.model.Centro
import com.valoralo.ui.components.AccionesCentroViewModel
import com.valoralo.ui.components.PantallaPendiente
import com.valoralo.ui.components.TarjetaCentro
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The catalogue (local cache) to draw the favourite cards without extra reads. */
@HiltViewModel
class FavoritosViewModel @Inject constructor(repo: CentroRepository) : ViewModel() {
    val centros: StateFlow<List<Centro>?> = repo.observarCentros()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** The favourite centros in the order of [codigos] (most recently added first). */
fun centrosEnOrden(centros: List<Centro>, codigos: Collection<String>): List<Centro> {
    val porCodigo = centros.associateBy { it.codigo }
    return codigos.mapNotNull { porCodigo[it] }
}

@Composable
fun FavoritesScreen(
    acciones: AccionesCentroViewModel,
    onVerCentro: (String) -> Unit,
    onIniciarSesion: () -> Unit,
    viewModel: FavoritosViewModel = hiltViewModel(),
) {
    val favoritos by acciones.favoritos.collectAsStateWithLifecycle()
    val seleccion by acciones.seleccion.collectAsStateWithLifecycle()
    val centros by viewModel.centros.collectAsStateWithLifecycle()

    val codigos = favoritos
    when {
        codigos == null -> Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            Text("Favoritos", style = MaterialTheme.typography.titleLarge)
            Text("Inicia sesión para guardar centros y tenerlos siempre a mano.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onIniciarSesion) { Text("Iniciar sesión") }
        }
        centros == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        else -> {
            val guardados = centrosEnOrden(centros.orEmpty(), codigos)
            if (guardados.isEmpty()) {
                PantallaPendiente(
                    icono = Icons.Outlined.FavoriteBorder,
                    titulo = "Aún no tienes favoritos",
                    texto = "Pulsa el corazón de un centro para guardarlo aquí.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            "Favoritos (${guardados.size})",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    items(guardados, key = { it.codigo }) { centro ->
                        TarjetaCentro(
                            centro = centro,
                            esFavorito = true,
                            enComparador = centro.codigo in seleccion,
                            onVerMas = { onVerCentro(centro.codigo) },
                            onFavorito = { acciones.alternarFavorito(centro.codigo) },
                            onComparar = { acciones.alternarComparar(centro.codigo) },
                        )
                    }
                }
            }
        }
    }
}
