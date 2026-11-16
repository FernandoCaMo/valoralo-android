package com.valoralo.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class PoliticaCacheTest {

    private val ahora = Instant.parse("2026-11-16T20:00:00Z")
    private fun meta(version: String? = "v1", haceHoras: Long = 1, formato: Int = FORMATO_CACHE_CATALOGO) =
        MetaCatalogo(version = version, descargadoEn = ahora.minus(Duration.ofHours(haceHoras)), formato = formato)

    @Test
    fun `sin cache hay que descargar`() {
        assertTrue(necesitaDescarga(local = null, versionRemota = "v1", ahora = ahora))
        assertTrue(necesitaDescarga(local = null, versionRemota = null, ahora = ahora))
    }

    @Test
    fun `misma version y menos de 24 horas no descarga`() {
        assertFalse(necesitaDescarga(meta(haceHoras = 23), "v1", ahora))
    }

    @Test
    fun `version distinta descarga`() {
        assertTrue(necesitaDescarga(meta(), "v2", ahora))
    }

    @Test
    fun `mas de 24 horas descarga aunque la version sea la misma`() {
        assertTrue(necesitaDescarga(meta(haceHoras = 24), "v1", ahora))
        assertTrue(necesitaDescarga(meta(haceHoras = 30), "v1", ahora))
    }

    @Test
    fun `sin version remota solo cuenta la edad`() {
        assertFalse(necesitaDescarga(meta(haceHoras = 2), null, ahora))
        assertTrue(necesitaDescarga(meta(haceHoras = 25), null, ahora))
    }

    @Test
    fun `un formato de cache antiguo obliga a descargar`() {
        assertTrue(necesitaDescarga(meta(formato = FORMATO_CACHE_CATALOGO - 1), "v1", ahora))
    }
}
