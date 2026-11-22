package com.valoralo.domain.usecase

import com.valoralo.domain.model.Centro
import java.net.URLEncoder
import java.util.Locale

/**
 * Links and texts for the address options in the centro detail (1.0.7): "Cómo llegar en Google Maps",
 * "Copiar dirección", "Compartir dirección". Pure, so it can be tested on the JVM.
 *
 * With coordinates the links point at the exact spot; without them Maps searches by
 * "name, address" (some centros have no coordinates in the dataset).
 */
data class EnlacesDireccion(
    /** "Avenida de Monte Claro, s/n, 41020 Sevilla" — what is copied and shared. */
    val direccionCompleta: String,
    /** Google Maps (app if installed, browser otherwise): route preview to the centro. */
    val rutaGoogleMaps: String,
    /** Any map app: the centro's spot with its name as the label. */
    val geo: String,
    /** Link to the place on Google Maps, for sharing. */
    val enlaceMaps: String,
    /** Text for Android's share sheet: name, address and the Maps link. */
    val textoCompartir: String,
)

fun enlacesDireccion(centro: Centro): EnlacesDireccion {
    val lugar = centro.lugar()
    val direccionCompleta = listOf(centro.direccion.trim(), "${centro.cp} $lugar".trim())
        .filter { it.isNotBlank() }
        .joinToString(", ")
    val coordenadas = if (centro.lat != null && centro.lng != null) {
        String.format(Locale.ROOT, "%.6f,%.6f", centro.lat, centro.lng)
    } else {
        null
    }
    // Without coordinates, the name helps Maps find the right building at that address.
    val busqueda = coordenadas ?: listOf(centro.nombre, direccionCompleta).filter { it.isNotBlank() }.joinToString(", ")
    val enlaceMaps = "https://www.google.com/maps/search/?api=1&query=${cod(busqueda)}"
    return EnlacesDireccion(
        direccionCompleta = direccionCompleta,
        rutaGoogleMaps = "https://www.google.com/maps/dir/?api=1&destination=${cod(busqueda)}",
        geo = if (coordenadas != null) "geo:0,0?q=$coordenadas(${cod(centro.nombre)})" else "geo:0,0?q=${cod(busqueda)}",
        enlaceMaps = enlaceMaps,
        textoCompartir = listOf(centro.nombre, direccionCompleta, enlaceMaps).filter { it.isNotBlank() }.joinToString("\n"),
    )
}

/** "Sevilla", "Aguadulce, Roquetas de Mar (Almería)" — same as the detail shows. */
private fun Centro.lugar(): String {
    val lugar = if (localidad.isNotBlank() && localidad != municipio) "$localidad, $municipio" else municipio
    return if (lugar.isBlank() || lugar == provincia || provincia.isBlank()) lugar else "$lugar ($provincia)"
}

/** URL encoding with %20 for spaces (Maps URLs and geo: URIs). */
private fun cod(texto: String): String = URLEncoder.encode(texto, "UTF-8").replace("+", "%20")
