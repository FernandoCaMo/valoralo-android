package com.valoralo.data.repository

import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.Valoracion
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant

/**
 * Bump when the cached JSON format changes incompatibly: the app then re-downloads
 * the catalogue instead of reading entries it can no longer decode.
 */
const val FORMATO_CACHE_CATALOGO = 1

/** The cached catalogue (centros with their stats) expires after this time. */
val EDAD_MAXIMA_CATALOGO: Duration = Duration.ofHours(24)

data class MetaCatalogo(
    /** `config/catalogo.version` when it was downloaded (null if it did not exist). */
    val version: String?,
    val descargadoEn: Instant,
    val formato: Int,
)

/**
 * Re-download when there is no cache, the cache format is old, the catalogue version
 * changed (a new import) or the cache is 24 h old (so the averages on Inicio stay fresh).
 * The version does not change with reviews.
 */
fun necesitaDescarga(local: MetaCatalogo?, versionRemota: String?, ahora: Instant): Boolean {
    if (local == null || local.formato != FORMATO_CACHE_CATALOGO) return true
    if (versionRemota != null && versionRemota != local.version) return true
    return Duration.between(local.descargadoEn, ahora) >= EDAD_MAXIMA_CATALOGO
}

/** Local copy of the catalogue (Room). */
interface CatalogoLocal {
    /** Cached centros sorted by nombre (empty until a catalogue has been saved). */
    fun observar(): Flow<List<Centro>>
    suspend fun meta(): MetaCatalogo?
    /** Replaces the whole catalogue atomically. */
    suspend fun guardar(centros: List<Centro>, meta: MetaCatalogo)
}

/** Firestore. */
interface CentrosRemoto {
    /** `config/catalogo.version`, read from the server. */
    suspend fun versionCatalogo(): String?
    suspend fun descargarCentros(): List<Centro>
    /** Live centro (the detail always shows live averages). */
    fun observarCentro(codigo: String): Flow<Centro?>
    fun observarValoraciones(codigo: String): Flow<List<Valoracion>>
}
