package com.valoralo.data.remote.firestore

import com.google.firebase.firestore.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoordenadasTest {

    @Test
    fun geoPointDelImportador() {
        assertEquals(37.38 to -5.98, coordenadas(GeoPoint(37.38, -5.98)))
    }

    @Test
    fun mapaConLatLngOLatitudeLongitude() {
        assertEquals(37.38 to -5.98, coordenadas(mapOf("lat" to 37.38, "lng" to -5.98)))
        assertEquals(37.0 to -5.0, coordenadas(mapOf("latitude" to 37L, "longitude" to -5L)))
    }

    @Test
    fun sinCoordenadasOFueraDeRango() {
        assertNull(coordenadas(null))
        assertNull(coordenadas("37,-5"))
        assertNull(coordenadas(mapOf("lat" to 37.0)))
        assertNull(coordenadas(mapOf("lat" to 137.0, "lng" to -5.0)))
        assertNull(coordenadas(GeoPoint(0.0, 0.0)))
    }

    @Test
    fun elCentroLlevaSusCoordenadas() {
        val c = mapOf<String, Any?>("nombre" to "IES Monte Claro", "geo" to GeoPoint(37.38, -5.98)).aCentro("41999901")
        assertEquals(37.38, c.lat!!, 0.0)
        assertEquals(-5.98, c.lng!!, 0.0)
        assertNull(mapOf<String, Any?>("nombre" to "Sin geo").aCentro("1").lat)
    }
}
