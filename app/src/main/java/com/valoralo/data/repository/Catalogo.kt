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
const val FORMATO_CACHE_CATALOGO = 2 // 2: summarised centros from catalogo/{provincia}-{parte}

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

/** `config/catalogo`: version and the ids of the catalogo/{provincia}-{parte} documents. */
data class InfoCatalogo(val version: String?, val partes: List<String>)

/** Progress of the catalogue download shown on Inicio ("Descargando centros… 3 de 8"). */
data class ProgresoDescarga(val hechas: Int, val total: Int)

/** Firestore. */
interface CentrosRemoto {
    /** `config/catalogo`, read from the server (1 read). */
    suspend fun infoCatalogo(): InfoCatalogo

    /**
     * Downloads the summarised catalogue (1 read per part). With no parts listed it falls
     * back to the whole centros collection.
     */
    suspend fun descargarCatalogo(
        partes: List<String>,
        alProgresar: (hechas: Int, total: Int) -> Unit,
    ): List<Centro>
    /** Live centro (the detail always shows live averages). */
    fun observarCentro(codigo: String): Flow<Centro?>
    fun observarValoraciones(codigo: String): Flow<List<Valoracion>>
}
