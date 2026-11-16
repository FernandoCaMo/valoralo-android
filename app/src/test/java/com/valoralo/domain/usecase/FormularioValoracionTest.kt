package com.valoralo.domain.usecase

import com.valoralo.domain.model.NotaCentro
import com.valoralo.domain.model.NotaDepartamento
import com.valoralo.domain.model.NotaInstalaciones
import com.valoralo.domain.model.Rol
import com.valoralo.domain.model.Valoracion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FormularioValoracionTest {

    private val departamentos = listOf("Matemáticas", "Inglés")
    private val valido = FormularioValoracion(notaCentro = 4, notaInstalaciones = 3)

    @Test
    fun `basta con las dos notas obligatorias`() {
        assertNull(valido.error(departamentos))
    }

    @Test
    fun `faltan notas obligatorias`() {
        assertEquals(ErrorFormulario.FALTA_NOTA_CENTRO, valido.copy(notaCentro = null).error(departamentos))
        assertEquals(ErrorFormulario.FALTA_NOTA_INSTALACIONES, valido.copy(notaInstalaciones = null).error(departamentos))
    }

    @Test
    fun `texto opcional de hasta 1000 caracteres`() {
        assertNull(valido.copy(texto = "x".repeat(1000)).error(departamentos))
        assertEquals(ErrorFormulario.TEXTO_LARGO, valido.copy(texto = "x".repeat(1001)).error(departamentos))
        assertNull(valido.copy(texto = "   ").error(departamentos))
    }

    @Test
    fun `otro departamento de 3 a 40 letras y sin repetir`() {
        assertNull(valido.copy(otros = listOf(OtroDepartamento("Robótica", 4))).error(departamentos))
        assertEquals(ErrorFormulario.OTRO_DEPARTAMENTO_NO_VALIDO, valido.copy(otros = listOf(OtroDepartamento("AB", 4))).error(departamentos))
        assertEquals(ErrorFormulario.OTRO_DEPARTAMENTO_NO_VALIDO, valido.copy(otros = listOf(OtroDepartamento("Dpto. 3", 4))).error(departamentos))
        assertEquals(ErrorFormulario.OTRO_DEPARTAMENTO_SIN_NOTA, valido.copy(otros = listOf(OtroDepartamento("Robótica", null))).error(departamentos))
        assertEquals(
            ErrorFormulario.DEPARTAMENTO_REPETIDO,
            valido.copy(otros = listOf(OtroDepartamento("matematicas", 3))).error(departamentos),
        )
    }

    @Test
    fun `se envia como lo espera publicarValoracion`() {
        val f = valido.copy(
            subnotasCentro = mapOf("gestion" to 5),
            departamentos = mapOf("Matemáticas" to 5, "Inglés" to null),
            otros = listOf(OtroDepartamento("  Robótica   educativa ", 4)),
            texto = "  Muy bien.  ",
        )
        assertEquals(
            mapOf(
                "codigo" to "21000693",
                "centro" to mapOf("nota" to 4, "gestion" to 5),
                "instalaciones" to mapOf("nota" to 3),
                "profesorado" to listOf(
                    mapOf("departamento" to "Matemáticas", "nota" to 5),
                    mapOf("departamento" to "Robótica educativa", "nota" to 4, "otro" to true),
                ),
                "texto" to "Muy bien.",
            ),
            f.paraEnviar("21000693"),
        )
    }

    @Test
    fun `se rellena desde mi valoracion para editarla`() {
        val mia = Valoracion(
            uid = "u", alias = "a", rolAutor = Rol.OTRO, verificado = false, fecha = LocalDate.of(2026, 1, 1),
            centro = NotaCentro(4, gestion = 3), instalaciones = NotaInstalaciones(2, limpieza = 1),
            profesorado = listOf(NotaDepartamento("Matemáticas", 5), NotaDepartamento("Robótica", 3)),
            texto = "Hola",
        )
        val f = FormularioValoracion.desde(mia, departamentos)
        assertEquals(4, f.notaCentro)
        assertEquals(mapOf("gestion" to 3), f.subnotasCentro)
        assertEquals(mapOf("limpieza" to 1), f.subnotasInstalaciones)
        assertEquals(mapOf("Matemáticas" to 5), f.departamentos)
        assertEquals(listOf(OtroDepartamento("Robótica", 3)), f.otros)
        assertEquals("Hola", f.texto)
    }
}
