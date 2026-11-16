package com.valoralo.data.local.room

import com.valoralo.domain.model.Centro
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Centro <-> JSON for the Room cache. */
object CentroCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun codificar(centro: Centro): String = json.encodeToString(Centro.serializer(), centro)

    /** Null if the entry cannot be decoded (e.g. corrupted). */
    fun decodificar(texto: String): Centro? = try {
        json.decodeFromString(Centro.serializer(), texto)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
