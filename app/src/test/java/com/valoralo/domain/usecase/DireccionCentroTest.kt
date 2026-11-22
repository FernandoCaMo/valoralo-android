package com.valoralo.domain.usecase

import com.valoralo.domain.model.Centro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DireccionCentroTest {

    private val conCoordenadas = Centro(
        codigo = "41999901", nombre = "IES Monte Claro", tipo = "IES", direccion = "Avenida de Monte Claro, s/n",
        cp = "41020", localidad = "Sevilla", municipio = "Sevilla", provincia = "Sevilla",
        lat = 37.3891234, lng = -5.9845678,
    )
    private val sinCoordenadas = conCoordenadas.copy(
        nombre = "CEIP Valle Claro", direccion = "Plaza del Valle, 1", cp = "04740",
        localidad = "Aguadulce", municipio = "Roquetas de Mar", provincia = "Almería", lat = null, lng = null,
    )

    @Test
    fun direccionCompleta() {
        assertEquals("Avenida de Monte Claro, s/n, 41020 Sevilla", enlacesDireccion(conCoordenadas).direccionCompleta)
        assertEquals("Plaza del Valle, 1, 04740 Aguadulce, Roquetas de Mar (Almería)", enlacesDireccion(sinCoordenadas).direccionCompleta)
    }

    @Test
    fun conCoordenadasLosEnlacesVanAlPuntoExacto() {
        val e = enlacesDireccion(conCoordenadas)
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=37.389123%2C-5.984568", e.rutaGoogleMaps)
        assertEquals("https://www.google.com/maps/search/?api=1&query=37.389123%2C-5.984568", e.enlaceMaps)
        // Other map apps: the spot with the centro's name as the label.
        assertEquals("geo:0,0?q=37.389123,-5.984568(IES%20Monte%20Claro)", e.geo)
    }

    @Test
    fun sinCoordenadasBuscaPorNombreYDireccion() {
        val e = enlacesDireccion(sinCoordenadas)
        val busqueda = "CEIP%20Valle%20Claro%2C%20Plaza%20del%20Valle%2C%201%2C%2004740%20Aguadulce%2C%20Roquetas%20de%20Mar%20%28Almer%C3%ADa%29"
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=$busqueda", e.rutaGoogleMaps)
        assertEquals("https://www.google.com/maps/search/?api=1&query=$busqueda", e.enlaceMaps)
        assertEquals("geo:0,0?q=$busqueda", e.geo)
        assertFalse(e.rutaGoogleMaps.contains(" "))
    }

    @Test
    fun textoParaCompartir() {
        val e = enlacesDireccion(conCoordenadas)
        assertEquals(
            "IES Monte Claro\nAvenida de Monte Claro, s/n, 41020 Sevilla\nhttps://www.google.com/maps/search/?api=1&query=37.389123%2C-5.984568",
            e.textoCompartir,
        )
    }

    @Test
    fun conDatosIncompletosNoQuedanComasSueltas() {
        val e = enlacesDireccion(conCoordenadas.copy(direccion = "  ", cp = "", lat = null, lng = null))
        assertEquals("Sevilla", e.direccionCompleta)
        assertTrue(e.enlaceMaps.endsWith("IES%20Monte%20Claro%2C%20Sevilla"))
    }
}
