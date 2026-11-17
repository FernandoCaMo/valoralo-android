package com.valoralo.ui.home

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.valoralo.domain.model.Destacada
import com.valoralo.domain.model.Rol
import com.valoralo.domain.usecase.formatearNota
import com.valoralo.ui.components.AccionesCentroViewModel
import com.valoralo.data.likes.ClaveResena
import com.valoralo.domain.usecase.EstadoLike
import com.valoralo.ui.components.BotonLike
import com.valoralo.ui.components.EventoAccion
import com.valoralo.ui.components.IlustracionCentro
import com.valoralo.ui.components.LogoValoralo
import com.valoralo.ui.components.TarjetaCentro
import com.valoralo.ui.favorites.centrosEnOrden
import com.valoralo.ui.theme.Estrella
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val FAVORITOS_EN_INICIO = 3
private val FORMATO_FECHA = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-ES"))

@Composable
fun InicioScreen(
    acciones: AccionesCentroViewModel,
    onVerCentro: (String) -> Unit,
    onBuscar: () -> Unit,
    onVerFavoritos: () -> Unit,
    onIniciarSesion: (mensaje: String) -> Unit,
    onAviso: (String) -> Unit,
    viewModel: InicioViewModel = hiltViewModel(),
) {
    val destacadas by viewModel.destacadas.collectAsStateWithLifecycle()
    val corazones by viewModel.corazones.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.likes.eventos.collect { e ->
            when (e) {
                is EventoAccion.Aviso -> onAviso(e.texto)
                is EventoAccion.IniciarSesion -> onIniciarSesion(e.mensaje)
            }
        }
    }
    val catalogo by viewModel.catalogo.collectAsStateWithLifecycle()
    val favoritos by acciones.favoritos.collectAsStateWithLifecycle()
    val seleccion by acciones.seleccion.collectAsStateWithLifecycle()
    val mios = favoritos?.let { centrosEnOrden(catalogo, it).take(FAVORITOS_EN_INICIO) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { LogoValoralo(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) }
        item {
            val d = destacadas
            when {
                d == null -> Box(Modifier.fillMaxWidth().height(ALTO_TARJETA), Alignment.Center) { CircularProgressIndicator() }
                d.mostrarCarrusel -> Carrusel(d.resenas, corazones, viewModel::alternarLike, onVerCentro)
                else -> Bienvenida()
            }
        }
        item {
            Button(onClick = onBuscar, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Buscar un centro")
            }
        }
        // With a session: the first three favourites (most recently added) and "Ver todos".
        if (mios != null) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Tus favoritos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
                    if (mios.isNotEmpty()) TextButton(onClick = onVerFavoritos) { Text("Ver todos") }
                }
            }
            if (mios.isEmpty()) {
                item {
                    Text(
                        "Pulsa el marcador de un centro para tenerlo aquí.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            items(mios, key = { "fav-${it.codigo}" }) { centro ->
                TarjetaCentro(
                    centro = centro,
                    esFavorito = true,
                    enComparador = centro.codigo in seleccion,
                    onVerMas = { onVerCentro(centro.codigo) },
                    onFavorito = { acciones.alternarFavorito(centro.codigo) },
                    onComparar = { acciones.alternarComparar(centro.codigo) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

private val ALTO_TARJETA = 236.dp

@Composable
private fun Bienvenida() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Te damos la bienvenida",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Aquí verás reseñas destacadas de centros educativos de Andalucía en cuanto haya suficientes. " +
                "Mientras tanto, busca tu centro y cuenta qué tal es.",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** Accessibility settings that turn the automatic rotation off. */
private fun animacionesDesactivadas(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

private fun lectorDePantalla(context: Context): Boolean =
    (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)?.isTouchExplorationEnabled == true

@Composable
private fun Carrusel(
    resenas: List<Destacada>,
    corazones: Map<ClaveResena, LikeDestacada>,
    onLike: (Destacada) -> Unit,
    onVerCentro: (String) -> Unit,
) {
    val n = resenas.size
    val pager = rememberPagerState(initialPage = paginaInicial(n)) { paginasVirtuales(n) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val ciclo = LocalLifecycleOwner.current.lifecycle
    // Not saveable: leaving Inicio and coming back lets it turn again.
    var tocado by remember { mutableStateOf(false) }
    val arrastrando by pager.interactionSource.collectIsDraggedAsState()
    if (arrastrando) tocado = true

    val gira = debeRotarSolo(n, tocado, animacionesDesactivadas(context), lectorDePantalla(context))
    LaunchedEffect(gira) {
        if (!gira) return@LaunchedEffect
        // Only while the screen is visible.
        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(MS_POR_RESENA)
                pager.animateScrollToPage(pager.currentPage + 1, animationSpec = tween(900, easing = FastOutSlowInEasing))
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.pointerInput(Unit) {
                // Any touch (tap or drag) stops the automatic rotation for this visit.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    tocado = true
                }
            },
        ) { pagina ->
            val r = resenas[resenaDePagina(pagina, n)]
            TarjetaDestacada(r, r.clave()?.let { corazones[it] }, { onLike(r) }) { onVerCentro(it) }
        }
        Controles(pager, n,
            onAnterior = { tocado = true; scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
            onSiguiente = { tocado = true; scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
        )
    }
}

@Composable
private fun Controles(pager: PagerState, n: Int, onAnterior: () -> Unit, onSiguiente: () -> Unit) {
    val actual = resenaDePagina(pager.currentPage, n)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(onClick = onAnterior) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Reseña anterior") }
        Row(
            Modifier.semantics(mergeDescendants = true) { contentDescription = "Reseña ${actual + 1} de $n" },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(n) { i ->
                Box(
                    Modifier.size(if (i == actual) 9.dp else 7.dp).clip(CircleShape).background(
                        if (i == actual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
            }
        }
        IconButton(onClick = onSiguiente) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Reseña siguiente") }
    }
}

@Composable
private fun TarjetaDestacada(r: Destacada, corazon: LikeDestacada?, onLike: () -> Unit, onVerCentro: (String) -> Unit) {
    val rol = when (r.rol) {
        Rol.DOCENTE -> if (r.verificado) "Docente verificado" else "Docente"
        Rol.ESTUDIANTE -> "Estudiante"
        Rol.OTRO -> "Otro"
    }
    val fecha = r.fecha?.format(FORMATO_FECHA)
    ElevatedCard(
        onClick = { onVerCentro(r.codigo) },
        modifier = Modifier.fillMaxWidth().height(ALTO_TARJETA).semantics(mergeDescendants = true) {
            contentDescription = "${r.nombreCentro}. Nota ${formatearNota(r.nota)} de 5. «${r.extracto}». $rol${fecha?.let { ", $it" }.orEmpty()}. Abrir el centro."
        },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IlustracionCentro(r.tipo, Modifier.size(44.dp).clip(MaterialTheme.shapes.small))
                Column(Modifier.weight(1f)) {
                    Text(r.nombreCentro, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    r.municipio?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatearNota(r.nota), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Estrella, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                "«${r.extracto}»",
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (r.rol == Rol.DOCENTE && r.verificado) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                Text(rol, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (fecha != null) {
                    Text(
                        "· $fecha",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
                Spacer(Modifier.weight(1f))
                // Old data without the review id: only the number.
                val estado = corazon?.estado ?: EstadoLike(dado = false, n = r.nLikes)
                BotonLike(estado, if (corazon == null || corazon.propia) null else onLike)
            }
        }
    }
}
