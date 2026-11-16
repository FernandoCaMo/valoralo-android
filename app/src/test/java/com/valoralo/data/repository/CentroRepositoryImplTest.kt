package com.valoralo.data.repository

import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.Valoracion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.time.Duration
import java.time.Instant

class CentroRepositoryImplTest {

    private val ahora = Instant.parse("2026-11-16T20:00:00Z")
    private fun centro(codigo: String, nombre: String = "IES $codigo") =
        Centro(codigo = codigo, nombre = nombre, tipo = "IES", direccion = "", cp = "", municipio = "", provincia = "Huelva")

    private class FakeLocal(var meta: MetaCatalogo? = null, centros: List<Centro> = emptyList()) : CatalogoLocal {
        private val datos = MutableStateFlow(if (meta == null) null else centros)
        var guardados = 0
        override fun observar(): Flow<List<Centro>> = datos.filterNotNull()
        override suspend fun meta(): MetaCatalogo? = meta
        override suspend fun guardar(centros: List<Centro>, meta: MetaCatalogo) {
            guardados++
            this.meta = meta
            datos.value = centros
        }
    }

    private class FakeRemoto(
        var version: String? = "v1",
        var centros: List<Centro> = emptyList(),
        var falla: Boolean = false,
    ) : CentrosRemoto {
        var descargas = 0
        val partesPedidas = mutableListOf<List<String>>()
        override suspend fun infoCatalogo(): InfoCatalogo {
            if (falla) throw IOException("sin red")
            return InfoCatalogo(version, partes = listOf("huelva-1", "sevilla-1"))
        }
        override suspend fun descargarCatalogo(
            partes: List<String>,
            alProgresar: (hechas: Int, total: Int) -> Unit,
        ): List<Centro> {
            if (falla) throw IOException("sin red")
            descargas++
            partesPedidas += partes
            partes.indices.forEach { alProgresar(it + 1, partes.size) }
            return centros
        }
        override fun observarCentro(codigo: String): Flow<Centro?> = emptyFlow()
        override fun observarValoraciones(codigo: String): Flow<List<Valoracion>> = emptyFlow()
    }

    private fun repo(local: CatalogoLocal, remoto: CentrosRemoto) =
        CentroRepositoryImpl(local, remoto, reloj = { ahora })

    private fun metaVigente(version: String = "v1") =
        MetaCatalogo(version, ahora.minus(Duration.ofHours(1)), FORMATO_CACHE_CATALOGO)

    @Test
    fun `primera vez descarga, guarda y emite`() = runTest {
        val local = FakeLocal()
        val remoto = FakeRemoto(version = "v1", centros = listOf(centro("1"), centro("2")))
        val emitidos = repo(local, remoto).observarCentros().first()
        assertEquals(listOf("1", "2"), emitidos.map { it.codigo })
        assertEquals(1, remoto.descargas)
        assertEquals(MetaCatalogo("v1", ahora, FORMATO_CACHE_CATALOGO), local.meta)
    }

    @Test
    fun `descarga las partes de config_catalogo e informa del progreso`() = runTest {
        val remoto = FakeRemoto(centros = listOf(centro("1")))
        val repositorio = repo(FakeLocal(), remoto)
        val progresos = mutableListOf<ProgresoDescarga?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repositorio.progresoDescarga.collect { progresos += it }
        }
        repositorio.observarCentros().first()
        assertEquals(listOf(listOf("huelva-1", "sevilla-1")), remoto.partesPedidas)
        assertEquals(
            listOf(null, ProgresoDescarga(0, 2), ProgresoDescarga(1, 2), ProgresoDescarga(2, 2), null),
            progresos,
        )
    }

    @Test
    fun `con cache vigente no descarga el catalogo`() = runTest {
        val local = FakeLocal(metaVigente(), listOf(centro("1")))
        val remoto = FakeRemoto(version = "v1", centros = listOf(centro("9")))
        val emitidos = repo(local, remoto).observarCentros().first()
        assertEquals(listOf("1"), emitidos.map { it.codigo })
        assertEquals(0, remoto.descargas)
    }

    @Test
    fun `si cambia la version descarga y emite el catalogo nuevo`() = runTest {
        val local = FakeLocal(metaVigente("v1"), listOf(centro("1")))
        val remoto = FakeRemoto(version = "v2", centros = listOf(centro("1"), centro("2")))
        val nuevos = repo(local, remoto).observarCentros().map { it.map(Centro::codigo) }
            .first { it == listOf("1", "2") }
        assertEquals(listOf("1", "2"), nuevos)
        assertEquals(1, remoto.descargas)
        assertEquals("v2", local.meta?.version)
    }

    @Test
    fun `si la cache tiene mas de 24 horas descarga`() = runTest {
        val vieja = MetaCatalogo("v1", ahora.minus(Duration.ofHours(25)), FORMATO_CACHE_CATALOGO)
        val local = FakeLocal(vieja, listOf(centro("1")))
        val remoto = FakeRemoto(version = "v1", centros = listOf(centro("1", nombre = "IES con nota nueva")))
        repo(local, remoto).observarCentros().map { it.single().nombre }.first { it == "IES con nota nueva" }
        assertEquals(1, remoto.descargas)
    }

    @Test
    fun `sin red y con cache emite la cache`() = runTest {
        val vieja = MetaCatalogo("v1", ahora.minus(Duration.ofHours(48)), FORMATO_CACHE_CATALOGO)
        val local = FakeLocal(vieja, listOf(centro("1")))
        val emitidos = repo(local, FakeRemoto(falla = true)).observarCentros().first()
        assertEquals(listOf("1"), emitidos.map { it.codigo })
        assertEquals(0, local.guardados)
    }

    @Test
    fun `sin red y sin cache falla`() = runTest {
        try {
            repo(FakeLocal(), FakeRemoto(falla = true)).observarCentros().first()
            fail("Debería fallar")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("sin red"))
        }
    }
}
