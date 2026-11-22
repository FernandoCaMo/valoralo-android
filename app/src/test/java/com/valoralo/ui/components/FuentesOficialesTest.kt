package com.valoralo.ui.components

import com.valoralo.ui.detail.avisarAlCopiar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuentesOficialesTest {

    /** Same three sources, names and order as the Play listing (docs/play/ficha-y-formularios.md). */
    @Test
    fun lasMismasFuentesQueLaFichaDePlay() {
        assertEquals(
            listOf(
                "Directorio de centros docentes no universitarios de Andalucía" to
                    "https://www.juntadeandalucia.es/datosabiertos/portal/dataset/directorio-de-centros-docentes-de-andalucia",
                "Plantillas orgánicas de centros docentes públicos" to
                    "https://www.juntadeandalucia.es/datosabiertos/portal/dataset/plantillas-organicas-de-centros-docentes-publicos",
                "Consulta oficial de centros de la Consejería de Educación" to
                    "https://www.juntadeandalucia.es/educacion/vscripts/centros/index.asp",
            ),
            FUENTES_OFICIALES.map { it.nombre to it.url },
        )
    }

    @Test
    fun elAvisoEsElDeLaFicha() {
        assertTrue(AVISO_INDEPENDENCIA.startsWith("Valoralo es una aplicación independiente. No representa a la Junta de Andalucía"))
        assertTrue(AVISO_INDEPENDENCIA_CENTRO.contains("no representa a la Junta de Andalucía"))
    }

    /** Android 13+ shows its own "copied" confirmation. */
    @Test
    fun avisoDeDireccionCopiadaSoloHastaAndroid12() {
        assertTrue(avisarAlCopiar(26))
        assertTrue(avisarAlCopiar(32))
        assertFalse(avisarAlCopiar(33))
        assertFalse(avisarAlCopiar(36))
    }
}
