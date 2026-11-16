package com.valoralo.data.valoraciones

import com.valoralo.domain.model.Fragmento
import org.junit.Assert.assertEquals
import org.junit.Test

class ErroresPublicacionTest {

    @Test
    fun `texto bloqueado trae los fragmentos`() {
        val e = errorDeFuncion(
            codigo = "INVALID_ARGUMENT",
            mensaje = "El texto contiene nombres propios, insultos o datos personales.",
            detalles = mapOf(
                "motivo" to "FILTRO",
                "fragmentos" to listOf(mapOf("inicio" to 9, "fin" to 12, "texto" to "Ana", "motivo" to "NOMBRE_PROPIO")),
            ),
        )
        assertEquals(TipoErrorPublicacion.TEXTO_NO_PERMITIDO, e.tipo)
        assertEquals(listOf(Fragmento(9, 12, "Ana", "NOMBRE_PROPIO")), e.fragmentos)
    }

    @Test
    fun `otros errores de la funcion`() {
        assertEquals(TipoErrorPublicacion.DATOS_NO_VALIDOS, errorDeFuncion("INVALID_ARGUMENT", "Las notas van de 1 a 5.", mapOf("campo" to "centro.nota")).tipo)
        assertEquals("Las notas van de 1 a 5.", errorDeFuncion("INVALID_ARGUMENT", "Las notas van de 1 a 5.", null).message)
        assertEquals(TipoErrorPublicacion.SIN_SESION, errorDeFuncion("UNAUTHENTICATED", "x", null).tipo)
        assertEquals(TipoErrorPublicacion.YA_DENUNCIADA, errorDeFuncion("ALREADY_EXISTS", "Ya has denunciado esta reseña.", null).tipo)
        assertEquals(TipoErrorPublicacion.SIN_CONEXION, errorDeFuncion("UNAVAILABLE", null, null).tipo)
        assertEquals(TipoErrorPublicacion.OTRO, errorDeFuncion("INTERNAL", "INTERNAL", null).tipo)
        assertEquals(TipoErrorPublicacion.OTRO.mensaje, errorDeFuncion("INTERNAL", "INTERNAL", null).message)
    }
}
