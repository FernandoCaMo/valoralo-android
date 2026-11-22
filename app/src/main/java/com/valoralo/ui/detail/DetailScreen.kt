package com.valoralo.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import com.valoralo.ui.components.FotoCentro
import com.valoralo.ui.components.EventoAccion
import com.valoralo.domain.usecase.EstadoLike
import com.valoralo.domain.usecase.OrdenResenas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextOverflow
import com.valoralo.domain.usecase.MIN_RESENAS_FIABLE
import com.valoralo.domain.usecase.textoResenas
import com.valoralo.ui.components.BotonComparar
import com.valoralo.ui.components.EstadoVacio
import kotlinx.coroutines.launch
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import com.valoralo.data.valoraciones.MotivoDenuncia
import com.valoralo.domain.model.EstadoValoracion
import com.valoralo.domain.model.Valoracion
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import com.valoralo.ui.components.AccionesCentroViewModel
import com.valoralo.ui.components.BotonFavorito
import com.valoralo.ui.components.AVISO_INDEPENDENCIA_CENTRO
import com.valoralo.ui.components.FUENTE_DATOS
import com.valoralo.ui.components.URL_DATASET_CENTROS
import com.valoralo.ui.components.ubicacion

@Composable
fun DetailScreen(
    onVolver: () -> Unit,
    onAnadirValoracion: (Centro) -> Unit,
    onIniciarSesion: (mensaje: String) -> Unit,
    onAviso: (String) -> Unit,
    acciones: AccionesCentroViewModel,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val aviso by viewModel.aviso.collectAsStateWithLifecycle()
    val favoritos by acciones.favoritos.collectAsStateWithLifecycle()
    val seleccion by acciones.seleccion.collectAsStateWithLifecycle()
    LaunchedEffect(aviso) {
        aviso?.let { onAviso(it); viewModel.avisoMostrado() }
    }
    LaunchedEffect(viewModel) {
        viewModel.likes.eventos.collect { e ->
            when (e) {
                is EventoAccion.Aviso -> onAviso(e.texto)
                is EventoAccion.IniciarSesion -> onIniciarSesion(e.mensaje)
            }
        }
    }
    DetailContent(
        state, onVolver, onAnadirValoracion, viewModel::onFiltroRolChange,
        onDenunciar = { autor, motivo -> viewModel.denunciar(autor, motivo) },
        onIniciarSesion = { onIniciarSesion("Inicia sesión para denunciar o bloquear.") },
        onOrdenChange = viewModel::onOrdenChange,
        onLike = viewModel::alternarLike,
        onBloquear = viewModel::bloquear,
        onAviso = onAviso,
        accionesBarra = { centro ->
            BotonFavorito(centro.codigo in favoritos.orEmpty(), centro.nombre) { acciones.alternarFavorito(centro.codigo) }
        },
        // 1.0.8: "Comparar" with text (not the arrows icon), next to the centro's mark.
        botonComparar = { centro ->
            BotonComparar(centro.codigo in seleccion, { acciones.alternarComparar(centro.codigo) })
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailContent(
    state: DetailUiState,
    onVolver: () -> Unit,
    onAnadirValoracion: (Centro) -> Unit,
    onFiltroRolChange: (FiltroRol) -> Unit,
    onDenunciar: (autorUid: String, MotivoDenuncia) -> Unit,
    onIniciarSesion: () -> Unit,
    onOrdenChange: (OrdenResenas) -> Unit,
    onLike: (Valoracion) -> Unit,
    onBloquear: (Valoracion) -> Unit,
    onAviso: (String) -> Unit,
    accionesBarra: @Composable (Centro) -> Unit,
    botonComparar: @Composable (Centro) -> Unit,
) {
    val lista = rememberLazyListState()
    // 1.0.8: once the header scrolls away, the bar shows which centro this is.
    val cabeceraFuera by remember { derivedStateOf { lista.firstVisibleItemIndex > 0 } }
    // 1.0.8: the button shrinks to its icon while scrolling down and grows back when scrolling up or at the top.
    val fabExpandido by remember { derivedStateOf { lista.firstVisibleItemIndex == 0 || !lista.lastScrolledForward } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (cabeceraFuera && state is DetailUiState.Listo) {
                        Text(state.centro.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        LogoValoralo()
                    }
                },
                windowInsets = WindowInsets(0),
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = { if (state is DetailUiState.Listo) accionesBarra(state.centro) },
            )
        },
        floatingActionButton = {
            if (state is DetailUiState.Listo) {
                val propia = state.miValoracion != null
                val etiqueta = if (propia) "Editar mi valoración" else "Añadir valoración"
                ExtendedFloatingActionButton(
                    onClick = { onAnadirValoracion(state.centro) },
                    icon = { Icon(if (propia) Icons.Outlined.Edit else Icons.Filled.Add, contentDescription = null) },
                    text = { Text(etiqueta) },
                    expanded = fabExpandido,
                    // One accessibility node with name, role and action: the tree showed this button without
                    // text (QA 1.0.5, finding 15), so TalkBack doesn't depend on merging the inner Text.
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = etiqueta
                        role = Role.Button
                        onClick(label = etiqueta) { onAnadirValoracion(state.centro); true }
                    },
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
                EstadoVacio(Icons.Outlined.SearchOff, "No hemos encontrado este centro", "Puede que ya no figure en el directorio oficial.", "Volver", onVolver)
            }
            DetailUiState.SinConexion -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                EstadoVacio(Icons.Outlined.CloudOff, "Sin conexión", "Este centro se cargará solo en cuanto vuelvas a tener red.")
            }
            DetailUiState.Error -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                EstadoVacio(Icons.Outlined.ErrorOutline, "No se ha podido cargar el centro", "Comprueba tu conexión e inténtalo de nuevo.", "Volver", onVolver)
            }
            is DetailUiState.Listo -> DetailLista(
                state, lista, onFiltroRolChange, padding, onDenunciar, onIniciarSesion, onOrdenChange, onLike, onBloquear, onAviso,
                onAnadirValoracion = { onAnadirValoracion(state.centro) }, botonComparar = { botonComparar(state.centro) },
            )
        }
    }
}

@Composable
private fun DetailLista(
    state: DetailUiState.Listo,
    lista: LazyListState,
    onFiltroRolChange: (FiltroRol) -> Unit,
    padding: PaddingValues,
    onDenunciar: (autorUid: String, MotivoDenuncia) -> Unit,
    onIniciarSesion: () -> Unit,
    onOrdenChange: (OrdenResenas) -> Unit,
    onLike: (Valoracion) -> Unit,
    onBloquear: (Valoracion) -> Unit,
    onAviso: (String) -> Unit,
    onAnadirValoracion: () -> Unit,
    botonComparar: @Composable () -> Unit,
) {
    val centro = state.centro
    val scope = rememberCoroutineScope()
    // Only the author's uid, saveable: the dialogs survive a rotation (QA 1.0.5, finding 9).
    var denunciandoUid by rememberSaveable { mutableStateOf<String?>(null) }
    var bloqueandoUid by rememberSaveable { mutableStateOf<String?>(null) }
    val denunciando = denunciandoUid?.let { uid -> state.resenas.firstOrNull { it.uid == uid } }
    val bloqueando = bloqueandoUid?.let { uid -> state.resenas.firstOrNull { it.uid == uid } }
    fun cerrarDialogos() { denunciandoUid = null; bloqueandoUid = null }
    bloqueando?.let { v ->
        AlertDialog(
            onDismissRequest = ::cerrarDialogos,
            title = { Text("¿Bloquear a ${v.alias}?") },
            text = { Text("Dejarás de ver sus reseñas en Valoralo. Puedes desbloquearlo cuando quieras en Mi cuenta.") },
            confirmButton = { TextButton(onClick = { onBloquear(v); cerrarDialogos() }) { Text("Bloquear") } },
            dismissButton = { TextButton(onClick = ::cerrarDialogos) { Text("Cancelar") } },
        )
    }
    denunciando?.let { v ->
        DialogoDenuncia(
            onDenunciar = { motivo -> onDenunciar(v.uid, motivo); cerrarDialogos() },
            onCancelar = ::cerrarDialogos,
        )
    }
    // 1.0.8 order: what people look for first (mark, notes) before the data of the centro.
    val indiceResenas = indiceDeResenas(hayDepartamentos = state.departamentos.isNotEmpty(), hayPropia = state.miValoracion != null)
    LazyColumn(
        state = lista,
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "cabecera") {
            Cabecera(centro, botonComparar, onVerResenas = { scope.launch { lista.animateScrollToItem(indiceResenas) } })
        }
        item(key = "valoraciones") {
            Titulo("Valoraciones")
            Spacer(Modifier.height(8.dp))
            ChipsRol(state.filtroRol, onFiltroRolChange)
        }
        item(key = "notas") { NotasPorDimension(state.notas, state.filtroRol) }
        item(key = "info") { Informacion(centro, onAviso) }
        item(key = "oferta") { OfertaAcademica(centro) }
        if (state.departamentos.isNotEmpty()) {
            item(key = "departamentos") { NotasPorDepartamento(state.departamentos) }
        }
        state.miValoracion?.let { mia ->
            item(key = "tu-valoracion") { Titulo("Tu valoración") }
            item(key = "mia") {
                TarjetaResena(
                    mia,
                    etiquetaEstado = mia.etiquetaParaElAutor(),
                    like = state.likes[mia.uid] ?: EstadoLike(false, mia.nLikes),
                )
            }
        }
        item(key = "resenas") {
            Titulo("Reseñas")
            if (state.resenas.size > 1) {
                Spacer(Modifier.height(8.dp))
                SelectorOrden(state.orden, onOrdenChange)
            }
        }
        if (state.resenas.isEmpty()) {
            item(key = "sin-resenas") {
                val invitar = state.filtroRol == FiltroRol.TODOS && state.miValoracion == null
                EstadoVacio(
                    Icons.Outlined.RateReview,
                    textoSinResenas(state.filtroRol, tengoPropia = state.miValoracion != null),
                    if (invitar) "Tu opinión ayuda a otras familias, estudiantes y docentes." else null,
                    accion = if (invitar) "Sé el primero en valorar" else null,
                    onAccion = if (invitar) onAnadirValoracion else null,
                )
            }
        } else {
            items(state.resenas, key = { it.uid }) { v ->
                TarjetaResena(
                    v,
                    onDenunciar = { if (state.uidActual == null) onIniciarSesion() else denunciandoUid = v.uid },
                    onBloquear = { if (state.uidActual == null) onIniciarSesion() else bloqueandoUid = v.uid },
                    like = state.likes[v.uid] ?: EstadoLike(false, v.nLikes),
                    onLike = { onLike(v) },
                )
            }
        }
    }
}

/** Index of the "Reseñas" item, for "Ver reseñas" (same order as the LazyColumn above). */
internal fun indiceDeResenas(hayDepartamentos: Boolean, hayPropia: Boolean): Int =
    5 + (if (hayDepartamentos) 1 else 0) + (if (hayPropia) 2 else 0)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Cabecera(centro: Centro, botonComparar: @Composable () -> Unit, onVerResenas: () -> Unit) {
    Column {
        // 1.0.8: a slimmer illustration (was 16:9, a third of the screen) so the mark is visible at once.
        FotoCentro(centro.fotoUrl, centro.tipo, Modifier.fillMaxWidth().aspectRatio(3f))
        Spacer(Modifier.height(12.dp))
        Text(centro.nombre, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(
            "${centro.tipo} · ${centro.ubicacion()}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        NotaMedia(
            centro.stats.total.global,
            centro.stats.total.n,
            style = MaterialTheme.typography.headlineMedium,
            iconSize = 30.dp,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            if (centro.stats.total.n > 0) {
                FilledTonalButton(onClick = onVerResenas) {
                    Text(if (centro.stats.total.n == 1) "Ver la reseña" else "Ver las ${centro.stats.total.n} reseñas")
                }
            }
            botonComparar()
        }
    }
}

@Composable
private fun Informacion(centro: Centro, onAviso: (String) -> Unit) {
    val uriHandler = LocalUriHandler.current
    fun abrir(uri: String) = runCatching { uriHandler.openUri(uri) }

    Seccion("Información") {
        // 1.0.7: the address is a button with options (directions, copy, share).
        DireccionConOpciones(centro, "${centro.direccion}\n${centro.cp} ${centro.ubicacion()}".trimEnd(), onAviso)
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
        // Play (misleading claims policy, 21/11/2026): link to the official source and say we are independent.
        Text(
            "$FUENTE_DATOS · Ver la fuente oficial",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable(onClickLabel = "abrir la fuente oficial", role = Role.Button) { abrir(URL_DATASET_CENTROS) }
                .padding(vertical = 6.dp),
        )
        Text(
            AVISO_INDEPENDENCIA_CENTRO,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FilaInfo(icono: ImageVector, texto: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
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
            val pocas = stats.n < MIN_RESENAS_FIABLE
            BarraNota(
                nombre, stats.media, detalle = "(${stats.n})", atenuada = pocas,
                descripcion = "$nombre: ${formatearNota(stats.media)} de 5, ${textoResenas(stats.n)}" + if (pocas) ", pocas reseñas" else "",
            )
        }
        if (departamentos.any { it.second.n < MIN_RESENAS_FIABLE }) {
            Text(
                "En gris, departamentos con menos de $MIN_RESENAS_FIABLE reseñas: su nota aún es poco fiable.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun BarraNota(etiqueta: String, nota: Double?, detalle: String? = null, atenuada: Boolean = false, descripcion: String? = null) {
    val colorTexto = if (atenuada) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    val semantica = if (descripcion != null) Modifier.clearAndSetSemantics { contentDescription = descripcion } else Modifier.semantics(mergeDescendants = true) {}
    Column(Modifier.fillMaxWidth().then(semantica)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = colorTexto, modifier = Modifier.weight(1f))
            Text(formatearNota(nota), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colorTexto)
            if (detalle != null) {
                Spacer(Modifier.width(4.dp))
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // 1.0.8: one colour on a neutral track (the expressive two-tone track looked like a second value).
        LinearProgressIndicator(
            progress = { ((nota ?: 0.0) / 5.0).toFloat() },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            color = if (atenuada) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            gapSize = 0.dp,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorOrden(orden: OrdenResenas, onChange: (OrdenResenas) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OrdenResenas.entries.forEach { o ->
            FilterChip(selected = o == orden, onClick = { onChange(o) }, label = { Text(o.etiqueta) })
        }
    }
}

private fun FiltroRol.etiqueta(): String = when (this) {
    FiltroRol.TODOS -> "Todos"
    FiltroRol.DOCENTES -> "Docentes"
    FiltroRol.ESTUDIANTES -> "Estudiantes"
    FiltroRol.OTROS -> "Otros"
}

private fun Valoracion.etiquetaParaElAutor(): String? = when (estado) {
    EstadoValoracion.PUBLICADA -> null
    EstadoValoracion.EN_REVISION -> "En revisión: aún no la ven los demás"
    EstadoValoracion.OCULTA -> "Oculta por moderación" + (motivoModeracion?.let { ": $it" } ?: "")
}

@Composable
private fun DialogoDenuncia(onDenunciar: (MotivoDenuncia) -> Unit, onCancelar: () -> Unit) {
    var motivo by rememberSaveable { mutableStateOf<MotivoDenuncia?>(null) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Denunciar reseña") },
        text = {
            Column(Modifier.selectableGroup()) {
                Text("¿Qué problema tiene?", style = MaterialTheme.typography.bodyMedium)
                MotivoDenuncia.entries.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(selected = motivo == m, role = Role.RadioButton, onClick = { motivo = m })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = motivo == m, onClick = null)
                        Text(m.etiqueta, Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { motivo?.let(onDenunciar) }, enabled = motivo != null) { Text("Denunciar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

/** Under "Reseñas" (the user's own review is shown apart, under "Tu valoración"). */
internal fun textoSinResenas(filtro: FiltroRol, tengoPropia: Boolean): String = when {
    !tengoPropia -> "Aún no hay reseñas${filtro.deQuien()}."
    filtro == FiltroRol.TODOS -> "Aún no hay reseñas de otras personas."
    else -> "Aún no hay otras reseñas${filtro.deQuien()}."
}

/** "Aún no hay reseñas{deQuien()}." */
private fun FiltroRol.deQuien(): String = when (this) {
    FiltroRol.TODOS -> ""
    FiltroRol.DOCENTES -> " de docentes"
    FiltroRol.ESTUDIANTES -> " de estudiantes"
    FiltroRol.OTROS -> " de otros usuarios"
}
