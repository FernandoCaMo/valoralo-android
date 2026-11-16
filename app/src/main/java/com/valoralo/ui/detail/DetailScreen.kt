package com.valoralo.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.StatsDepartamento
import com.valoralo.domain.model.StatsGrupo
import com.valoralo.domain.usecase.FiltroRol
import com.valoralo.domain.usecase.formatearNota
import com.valoralo.ui.components.LogoValoralo
import com.valoralo.ui.components.NotaMedia
import com.valoralo.ui.components.FUENTE_DATOS
import com.valoralo.ui.home.ubicacion

@Composable
fun DetailScreen(
    onVolver: () -> Unit,
    onAnadirValoracion: (Centro) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DetailContent(state, onVolver, onAnadirValoracion, viewModel::onFiltroRolChange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailContent(
    state: DetailUiState,
    onVolver: () -> Unit,
    onAnadirValoracion: (Centro) -> Unit,
    onFiltroRolChange: (FiltroRol) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { LogoValoralo() },
                windowInsets = WindowInsets(0),
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            if (state is DetailUiState.Listo) {
                ExtendedFloatingActionButton(
                    onClick = { onAnadirValoracion(state.centro) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Añadir valoración") },
                )
            }
        },
        // The app-level Scaffold already handles system bars and the bottom bar.
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        when (state) {
            DetailUiState.Cargando -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            DetailUiState.NoEncontrado -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("No hemos encontrado este centro.")
            }
            is DetailUiState.Listo -> DetailLista(state, onFiltroRolChange, padding)
        }
    }
}

@Composable
private fun DetailLista(
    state: DetailUiState.Listo,
    onFiltroRolChange: (FiltroRol) -> Unit,
    padding: PaddingValues,
) {
    val centro = state.centro
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Cabecera(centro) }
        item { Informacion(centro) }
        item { OfertaAcademica(centro) }
        item {
            Titulo("Valoraciones")
            Spacer(Modifier.height(8.dp))
            ChipsRol(state.filtroRol, onFiltroRolChange)
        }
        item { NotasPorDimension(state.notas, state.filtroRol) }
        if (state.departamentos.isNotEmpty()) {
            item { NotasPorDepartamento(state.departamentos) }
        }
        item { Titulo("Reseñas") }
        if (state.resenas.isEmpty()) {
            item {
                Text(
                    "Aún no hay reseñas${state.filtroRol.deQuien()}.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.resenas, key = { it.uid }) { TarjetaResena(it) }
        }
    }
}

@Composable
private fun Cabecera(centro: Centro) {
    Column {
        Text(centro.nombre, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(
            "${centro.tipo} · ${centro.ubicacion()}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        NotaMedia(
            centro.stats.total.global,
            centro.stats.total.n,
            style = MaterialTheme.typography.headlineSmall,
            iconSize = 26.dp,
        )
    }
}

@Composable
private fun Informacion(centro: Centro) {
    val uriHandler = LocalUriHandler.current
    fun abrir(uri: String) = runCatching { uriHandler.openUri(uri) }

    Seccion("Información") {
        FilaInfo(Icons.Outlined.Place, "${centro.direccion}\n${centro.cp} ${centro.ubicacion()}".trimEnd())
        centro.telefono?.let { tel ->
            FilaInfo(Icons.Outlined.Phone, tel, onClick = { abrir("tel:${tel.replace(" ", "")}") })
        }
        centro.email?.let { email ->
            FilaInfo(Icons.Outlined.Email, email, onClick = { abrir("mailto:$email") })
        }
        centro.web?.let { web ->
            FilaInfo(Icons.Outlined.Language, web.removePrefix("https://"), onClick = { abrir(web) })
        }
        FilaInfo(Icons.Outlined.Tag, "Código de centro: ${centro.codigo}")
        Text(
            FUENTE_DATOS,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun FilaInfo(icono: ImageVector, texto: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun OfertaAcademica(centro: Centro) {
    Seccion("Oferta académica") {
        centro.ensenanzas.groupBy { it.nivel }.forEach { (nivel, ensenanzas) ->
            Text(nivel, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
            ensenanzas.forEach {
                Text("• ${it.nombre}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun ChipsRol(seleccionado: FiltroRol, onChange: (FiltroRol) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FiltroRol.entries.forEach { filtro ->
            FilterChip(
                selected = filtro == seleccionado,
                onClick = { onChange(filtro) },
                label = { Text(filtro.etiqueta()) },
            )
        }
    }
}

@Composable
private fun NotasPorDimension(notas: StatsGrupo, filtro: FiltroRol) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (notas.n == 0) {
                Text("Aún no hay valoraciones${filtro.deQuien()}.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                NotaMedia(notas.global, notas.n)
                if (filtro != FiltroRol.TODOS) {
                    Text(
                        " según ${filtro.etiqueta().lowercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            BarraNota("Centro en general", notas.centro)
            BarraNota("Aulas e instalaciones", notas.instalaciones)
            BarraNota("Profesorado", notas.profesorado)
        }
    }
}

@Composable
private fun NotasPorDepartamento(departamentos: List<Pair<String, StatsDepartamento>>) {
    Seccion("Profesorado por departamento") {
        departamentos.forEach { (nombre, stats) ->
            BarraNota(nombre, stats.media, detalle = "(${stats.n})")
        }
    }
}

@Composable
private fun BarraNota(etiqueta: String, nota: Double?, detalle: String? = null) {
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(etiqueta, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(formatearNota(nota), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            if (detalle != null) {
                Spacer(Modifier.width(4.dp))
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LinearProgressIndicator(
            progress = { ((nota ?: 0.0) / 5.0).toFloat() },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Titulo(titulo)
            Spacer(Modifier.height(4.dp))
            contenido()
        }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

private fun FiltroRol.etiqueta(): String = when (this) {
    FiltroRol.TODOS -> "Todos"
    FiltroRol.DOCENTES -> "Docentes"
    FiltroRol.ESTUDIANTES -> "Estudiantes"
    FiltroRol.OTROS -> "Otros"
}

/** "Aún no hay reseñas{deQuien()}." */
private fun FiltroRol.deQuien(): String = when (this) {
    FiltroRol.TODOS -> ""
    FiltroRol.DOCENTES -> " de docentes"
    FiltroRol.ESTUDIANTES -> " de estudiantes"
    FiltroRol.OTROS -> " de otros usuarios"
}
