package com.valoralo.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.RadioButton
import androidx.compose.ui.semantics.Role
import com.valoralo.ui.theme.Apariencia
import com.valoralo.ui.theme.Tema
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.valoralo.domain.usecase.ALIAS_MAX
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valoralo.domain.model.Rol
import com.valoralo.domain.model.Sesion
import com.valoralo.domain.model.Usuario
import com.valoralo.ui.components.URL_PRIVACIDAD
import com.valoralo.ui.components.URL_CONDICIONES
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.runtime.LaunchedEffect

@Composable
fun AccountScreen(
    onIniciarSesion: () -> Unit,
    onCrearCuenta: () -> Unit,
    onCompletarPerfil: () -> Unit,
    onAcercaDe: () -> Unit,
    onVerificar: () -> Unit,
    onAviso: (String) -> Unit,
    viewModel: MiCuentaViewModel = hiltViewModel(),
) {
    val sesion by viewModel.sesion.collectAsStateWithLifecycle()
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val abrirPrivacidad = { runCatching { uriHandler.openUri(URL_PRIVACIDAD) }; Unit }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        when (val s = sesion) {
            Sesion.Cargando -> CircularProgressIndicator(Modifier.padding(32.dp).align(Alignment.CenterHorizontally))
            Sesion.SinSesion -> SinSesion(onIniciarSesion, onCrearCuenta)
            is Sesion.Iniciada -> if (s.perfil == null) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Te falta completar tu perfil para usar la cuenta.")
                    Button(onClick = onCompletarPerfil) { Text("Completar perfil") }
                }
            } else {
                ConSesion(s, s.perfil, viewModel, onVerificar)
            }
        }

        HorizontalDivider(Modifier.padding(top = 8.dp))
        // 1.0.8: light, dark or system (a per-device preference).
        val context = LocalContext.current
        val tema by Apariencia.tema.collectAsStateWithLifecycle()
        var elegirTema by rememberSaveable { mutableStateOf(false) }
        Opcion(Icons.Outlined.Palette, "Tema", tema.etiqueta) { elegirTema = true }
        Opcion(Icons.Outlined.Gavel, "Condiciones de uso") { runCatching { uriHandler.openUri(URL_CONDICIONES) } }
        Opcion(Icons.Outlined.Policy, "Política de privacidad", onClick = abrirPrivacidad)
        Opcion(Icons.Outlined.Info, "Acerca de", "Versión, información legal y contacto", onClick = onAcercaDe)
        val conPerfil = (sesion as? Sesion.Iniciada)?.takeIf { it.perfil != null }
        if (sesion is Sesion.Iniciada) {
            Opcion(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", flecha = false) { viewModel.cerrarSesion(context) }
        }
        // 1.0.8: the destructive action last, apart from the rest.
        if (conPerfil != null) {
            var eliminar by rememberSaveable { mutableStateOf(false) }
            HorizontalDivider(Modifier.padding(top = 8.dp))
            Opcion(Icons.Outlined.DeleteForever, "Eliminar cuenta", "Se borra todo: perfil, reseñas, likes, favoritos…", flecha = false, peligro = true) { eliminar = true }
            if (eliminar) DialogoEliminar(conPerfil.conGoogle, viewModel) { eliminar = false }
        }
        Spacer(Modifier.height(16.dp))
        if (elegirTema) {
            DialogoTema(tema, onElegir = { Apariencia.elegir(context, it); elegirTema = false }, onCerrar = { elegirTema = false })
        }
    }

    // Errors need attention (dialog); confirmations ("Alias actualizado.") are a short snackbar.
    estado.error?.let { MensajeDialogo(it, viewModel::mensajesMostrados) }
    LaunchedEffect(estado.aviso) {
        estado.aviso?.let { onAviso(it); viewModel.avisoMostrado() }
    }
}

@Composable
private fun SinSesion(onIniciarSesion: () -> Unit, onCrearCuenta: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        Text("Mi cuenta", style = MaterialTheme.typography.titleLarge)
        Text(
            "Puedes leer reseñas sin cuenta. Para escribirlas, inicia sesión o crea una cuenta (a partir de 14 años).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onIniciarSesion, modifier = Modifier.fillMaxWidth()) { Text("Iniciar sesión") }
        OutlinedButton(onClick = onCrearCuenta, modifier = Modifier.fillMaxWidth()) { Text("Crear una cuenta") }
    }
}

@Composable
private fun ConSesion(sesion: Sesion.Iniciada, perfil: Usuario, viewModel: MiCuentaViewModel, onVerificar: () -> Unit) {
    var editarAlias by rememberSaveable { mutableStateOf(false) }
    var cambiarRol by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(perfil.alias, style = MaterialTheme.typography.headlineSmall)
        Text(
            when {
                perfil.rol == Rol.DOCENTE && perfil.verificado -> "Docente verificado"
                perfil.rol == Rol.ESTUDIANTE -> "Estudiante"
                else -> "Otro"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        sesion.correo?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Opcion(Icons.Outlined.Edit, "Cambiar alias", perfil.alias) { editarAlias = true }
    if (perfil.rol != Rol.DOCENTE) {
        Opcion(Icons.Outlined.Badge, "Cambiar rol", if (perfil.rol == Rol.ESTUDIANTE) "Estudiante" else "Otro") {
            cambiarRol = true
        }
    }
    if (!(perfil.rol == Rol.DOCENTE && perfil.verificado)) {
        Opcion(Icons.Outlined.School, "Soy docente", "Verifícate con la hoja de servicios del Portal del docente", onClick = onVerificar)
    }
    val bloqueadosVm: BloqueadosViewModel = hiltViewModel()
    val bloqueados by bloqueadosVm.bloqueados.collectAsStateWithLifecycle()
    var verBloqueados by rememberSaveable { mutableStateOf(false) }
    Opcion(
        Icons.Outlined.Block,
        "Autores bloqueados",
        when (bloqueados.size) { 0 -> "Ninguno"; 1 -> "1 autor"; else -> "${bloqueados.size} autores" },
    ) { verBloqueados = true }

    if (verBloqueados) DialogoBloqueados(bloqueados, bloqueadosVm::desbloquear) { verBloqueados = false }
    if (editarAlias) DialogoAlias(perfil.alias, viewModel) { editarAlias = false }
    if (cambiarRol) DialogoRol(perfil.rol, viewModel) { cambiarRol = false }
}

@Composable
private fun DialogoTema(actual: Tema, onElegir: (Tema) -> Unit, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Tema") },
        text = {
            Column(Modifier.selectableGroup()) {
                Tema.entries.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(selected = t == actual, role = Role.RadioButton, onClick = { onElegir(t) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = t == actual, onClick = null)
                        Text(t.etiqueta, Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
    )
}

@Composable
private fun DialogoBloqueados(bloqueados: Map<String, String>, onDesbloquear: (String) -> Unit, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Autores bloqueados") },
        text = {
            if (bloqueados.isEmpty()) {
                Text("No has bloqueado a nadie. Desde una reseña puedes bloquear a su autor para dejar de ver sus reseñas.")
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    bloqueados.entries.sortedBy { it.value.lowercase() }.forEach { (uid, alias) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(alias.ifBlank { "Autor sin alias" }, Modifier.weight(1f))
                            TextButton(onClick = { onDesbloquear(uid) }) { Text("Desbloquear") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
    )
}

@Composable
private fun DialogoAlias(actual: String, viewModel: MiCuentaViewModel, onCerrar: () -> Unit) {
    var alias by rememberSaveable { mutableStateOf(actual) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Cambiar alias") },
        text = {
            OutlinedTextField(
                // Never more than the limit, also when pasting (QA 1.0.5, finding 4).
                value = alias, onValueChange = { alias = it.take(ALIAS_MAX); error = null },
                label = { Text("Alias") }, singleLine = true,
                isError = error != null,
                supportingText = {
                    Row {
                        Text(
                            // Finding 3: published reviews keep the alias they were written with.
                            error ?: "Es el nombre que verán los demás. Se aplicará a tus reseñas cuando las publiques o edites.",
                            Modifier.weight(1f),
                        )
                        Text("${alias.length}/$ALIAS_MAX", Modifier.padding(start = 8.dp))
                    }
                },
            )
        },
        confirmButton = {
            TextButton(onClick = {
                error = viewModel.cambiarAlias(alias)
                if (error == null) onCerrar()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

@Composable
private fun DialogoRol(actual: Rol, viewModel: MiCuentaViewModel, onCerrar: () -> Unit) {
    var elegido by rememberSaveable { mutableStateOf(actual) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Cambiar rol") },
        text = {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(Rol.ESTUDIANTE to "Estudiante", Rol.OTRO to "Otro (familia, exalumnado…)").forEach { (rol, texto) ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(selected = elegido == rol, role = Role.RadioButton, onClick = { elegido = rol })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = elegido == rol, onClick = null)
                        Text(texto, Modifier.padding(start = 8.dp))
                    }
                }
                Text(
                    "Tus reseñas ya publicadas mantienen el rol con el que las escribiste. " +
                        "Si eres docente, verifícate desde \"Soy docente\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (elegido != actual) viewModel.cambiarRol(elegido)
                onCerrar()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

/** Everything deleted with the account (functions/src alBorrarUsuario). Keep in sync with web/eliminar-cuenta.html. */
private val LO_QUE_SE_BORRA = listOf(
    "Tu acceso y tu perfil (alias, rol y fecha de alta).",
    "Tus valoraciones, con los likes que recibieron; dejan de contar en las notas.",
    "Los likes que has dado (se descuentan de las reseñas).",
    "Tus favoritos.",
    "Tus denuncias.",
    "Los autores que has bloqueado.",
    "Tu solicitud de verificación docente, si la hiciste: nombre, código CSV y PDF.",
)

@Composable
private fun DialogoEliminar(conGoogle: Boolean, viewModel: MiCuentaViewModel, onCerrar: () -> Unit) {
    val context = LocalContext.current
    var contrasena by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("¿Eliminar tu cuenta?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Se borrará todo lo tuyo en Valoralo, al momento y sin posibilidad de deshacerlo:")
                // Same list as web/eliminar-cuenta.html (what alBorrarUsuario deletes).
                Text(
                    LO_QUE_SE_BORRA.joinToString("\n") { "• $it" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (conGoogle) {
                    Text("Para confirmar que eres tú, te pediremos que elijas tu cuenta de Google.")
                } else {
                    OutlinedTextField(
                        value = contrasena, onValueChange = { contrasena = it },
                        label = { Text("Tu contraseña") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (conGoogle) viewModel.eliminarConGoogle(context) else viewModel.eliminarConContrasena(contrasena)
                    onCerrar()
                },
                enabled = conGoogle || contrasena.isNotEmpty(),
            ) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

@Composable
private fun MensajeDialogo(texto: String, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Entendido") } },
    )
}

@Composable
private fun Opcion(
    icono: ImageVector,
    titulo: String,
    detalle: String? = null,
    flecha: Boolean = true,
    peligro: Boolean = false,
    habilitada: Boolean = true,
    onClick: () -> Unit,
) {
    val color = when {
        !habilitada -> MaterialTheme.colorScheme.onSurfaceVariant
        peligro -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    ListItem(
        headlineContent = { Text(titulo, color = color) },
        supportingContent = detalle?.let { { Text(it) } },
        leadingContent = { Icon(icono, contentDescription = null, tint = color) },
        trailingContent = if (flecha && habilitada) {
            { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
        } else null,
        colors = ListItemDefaults.colors(),
        modifier = Modifier.fillMaxWidth().clickable(enabled = habilitada, onClick = onClick),
    )
}
