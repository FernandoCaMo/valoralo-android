package com.valoralo.ui.components

import com.valoralo.domain.model.Centro

/**
 * "Dos Hermanas (Sevilla)", or just "Sevilla" when municipio and provincia match.
 * When the localidad differs from the municipio: "Aguadulce, Roquetas de Mar (Almería)".
 */
fun Centro.ubicacion(): String {
    val lugar = if (localidad.isNotBlank() && localidad != municipio) "$localidad, $municipio" else municipio
    return if (lugar == provincia) lugar else "$lugar ($provincia)"
}
