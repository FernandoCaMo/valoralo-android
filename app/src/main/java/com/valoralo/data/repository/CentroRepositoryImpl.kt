package com.valoralo.data.repository

import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.Valoracion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inicio reads the catalogue from the local cache and refreshes it in the background
 * only when [necesitaDescarga] says so (1 read of config/catalogo per launch instead of
 * the whole collection). The detail and its reviews are always live from Firestore.
 */
@Singleton
class CentroRepositoryImpl(
    private val local: CatalogoLocal,
    private val remoto: CentrosRemoto,
    private val reloj: () -> Instant,
) : CentroRepository {

    @Inject
    constructor(local: CatalogoLocal, remoto: CentrosRemoto) : this(local, remoto, Instant::now)

    override fun observarCentros(): Flow<List<Centro>> = channelFlow {
        if (local.meta() == null) {
            // First launch: nothing to show until the download works (errors reach the UI).
            descargar(remoto.versionCatalogo())
        } else {
            // Show the cache straight away and refresh it if needed; offline is fine.
            launch {
                try {
                    val version = remoto.versionCatalogo()
                    if (necesitaDescarga(local.meta(), version, reloj())) descargar(version)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Keep showing the cached catalogue.
                }
            }
        }
        local.observar().collect { send(it) }
    }

    private suspend fun descargar(version: String?) {
        val centros = remoto.descargarCentros()
        local.guardar(centros, MetaCatalogo(version, reloj(), FORMATO_CACHE_CATALOGO))
    }

    override fun observarCentro(codigo: String): Flow<Centro?> = remoto.observarCentro(codigo)

    override fun observarValoraciones(codigo: String): Flow<List<Valoracion>> =
        remoto.observarValoraciones(codigo)
}
