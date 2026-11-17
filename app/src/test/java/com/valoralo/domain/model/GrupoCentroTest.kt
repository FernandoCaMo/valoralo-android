package com.valoralo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GrupoCentroTest {

    @Test
    fun `todos los tipos del catalogo tienen grupo`() {
        // Types present in the Andalucía catalogue (curso 2025/26).
        val esperado = mapOf(
            "CEIP" to GrupoCentro.INFANTIL_PRIMARIA, "CPR" to GrupoCentro.INFANTIL_PRIMARIA, "CEPR" to GrupoCentro.INFANTIL_PRIMARIA,
            "IES" to GrupoCentro.SECUNDARIA, "SES" to GrupoCentro.SECUNDARIA,
            "CIFP" to GrupoCentro.FP,
            "SEP" to GrupoCentro.ADULTOS, "CEPER" to GrupoCentro.ADULTOS, "IPEP" to GrupoCentro.ADULTOS, "SIPEP" to GrupoCentro.ADULTOS,
            "EOI" to GrupoCentro.IDIOMAS,
            "CEM" to GrupoCentro.MUSICA_DANZA, "CPM" to GrupoCentro.MUSICA_DANZA, "CPD" to GrupoCentro.MUSICA_DANZA,
            "CSM" to GrupoCentro.MUSICA_DANZA, "CSD" to GrupoCentro.MUSICA_DANZA,
            "EA" to GrupoCentro.ARTE, "EASD" to GrupoCentro.ARTE, "ESAD" to GrupoCentro.ARTE,
            "CEEE" to GrupoCentro.EDUCACION_ESPECIAL,
        )
        esperado.forEach { (tipo, grupo) -> assertEquals(tipo, grupo, GrupoCentro.de(tipo)) }
    }

    @Test
    fun `un tipo desconocido usa la ilustracion generica`() {
        assertEquals(GrupoCentro.OTRO, GrupoCentro.de("XYZ"))
        assertEquals(GrupoCentro.OTRO, GrupoCentro.de(null))
        assertEquals(GrupoCentro.SECUNDARIA, GrupoCentro.de(" ies "))
    }
}
