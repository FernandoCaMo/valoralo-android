package com.valoralo.ui.components

/** Attribution required by the CC BY 4.0 licence of the centres catalogue. */
const val FUENTE_DATOS = "Fuente: Junta de Andalucía, CC BY 4.0"

/**
 * Official sources, the same three (same names and order) as the Play listing's "Fuentes de los datos
 * oficiales" (Play's misleading claims policy, rejection of 21/11/2026; docs/play/ficha-y-formularios.md).
 */
const val URL_DATASET_CENTROS =
    "https://www.juntadeandalucia.es/datosabiertos/portal/dataset/directorio-de-centros-docentes-de-andalucia"
const val URL_DATASET_PLANTILLAS =
    "https://www.juntadeandalucia.es/datosabiertos/portal/dataset/plantillas-organicas-de-centros-docentes-publicos"
const val URL_CONSULTA_CENTROS = "https://www.juntadeandalucia.es/educacion/vscripts/centros/index.asp"

data class FuenteOficial(val nombre: String, val url: String)

val FUENTES_OFICIALES = listOf(
    FuenteOficial("Directorio de centros docentes no universitarios de Andalucía", URL_DATASET_CENTROS),
    FuenteOficial("Plantillas orgánicas de centros docentes públicos", URL_DATASET_PLANTILLAS),
    FuenteOficial("Consulta oficial de centros de la Consejería de Educación", URL_CONSULTA_CENTROS),
)

/** Same wording as the notice at the top of the Play listing. */
const val AVISO_INDEPENDENCIA =
    "Valoralo es una aplicación independiente. No representa a la Junta de Andalucía ni a ningún organismo " +
        "público o centro educativo, y no está afiliada a ellos ni autorizada por ellos. Las opiniones publicadas son de sus usuarios."

/** Short version under the data of each centro. */
const val AVISO_INDEPENDENCIA_CENTRO =
    "Valoralo es independiente: no representa a la Junta de Andalucía ni a este centro. Los datos pueden no estar actualizados."

const val URL_LICENCIA_CC_BY = "https://creativecommons.org/licenses/by/4.0/deed.es"

/** Provisional privacy policy page (Firebase Hosting, web/privacidad.html) until Phase 7. */
const val URL_PRIVACIDAD = "https://valoralo-dc735.web.app/privacidad.html"
const val URL_CONDICIONES = "https://valoralo-dc735.web.app/condiciones.html"
const val URL_ELIMINAR_CUENTA = "https://valoralo-dc735.web.app/eliminar-cuenta.html"
const val CORREO_CONTACTO = "valoralo.app@gmail.com"
