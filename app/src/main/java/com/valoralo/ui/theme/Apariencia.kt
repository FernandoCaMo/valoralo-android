package com.valoralo.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Light, dark or follow the system (1.0.8, Mi cuenta → Tema). */
enum class Tema(val etiqueta: String) {
    SISTEMA("Según el sistema"),
    CLARO("Claro"),
    OSCURO("Oscuro"),
}

/**
 * The chosen theme, kept in the device's preferences (a per-device convenience: nothing goes to the server).
 * Read once in MainActivity before the first frame, so there is no flash of the wrong theme.
 */
object Apariencia {
    private const val ARCHIVO = "apariencia"
    private const val CLAVE = "tema"
    private val estado = MutableStateFlow(Tema.SISTEMA)
    val tema: StateFlow<Tema> = estado.asStateFlow()

    fun cargar(context: Context) {
        val guardado = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).getString(CLAVE, null)
        estado.value = Tema.entries.firstOrNull { it.name == guardado } ?: Tema.SISTEMA
    }

    fun elegir(context: Context, tema: Tema) {
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit().putString(CLAVE, tema.name).apply()
        estado.value = tema
    }

    /** null = follow the system. */
    fun oscuro(tema: Tema, sistemaOscuro: Boolean): Boolean = when (tema) {
        Tema.SISTEMA -> sistemaOscuro
        Tema.CLARO -> false
        Tema.OSCURO -> true
    }
}
