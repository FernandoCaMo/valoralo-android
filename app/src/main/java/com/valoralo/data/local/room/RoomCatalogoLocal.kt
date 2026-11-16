package com.valoralo.data.local.room

import com.valoralo.data.repository.CatalogoLocal
import com.valoralo.data.repository.MetaCatalogo
import com.valoralo.domain.model.Centro
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCatalogoLocal @Inject constructor(
    private val dao: CatalogoDao,
) : CatalogoLocal {

    // Entries that cannot be decoded are skipped; a format bump forces a re-download.
    override fun observar(): Flow<List<Centro>> =
        dao.observarCentros().map { lista -> lista.mapNotNull { CentroCodec.decodificar(it.json) } }

    override suspend fun meta(): MetaCatalogo? = dao.meta()?.let {
        MetaCatalogo(it.version, Instant.ofEpochMilli(it.descargadoEnMillis), it.formato)
    }

    override suspend fun guardar(centros: List<Centro>, meta: MetaCatalogo) {
        dao.reemplazar(
            centros.map { CentroEntity(it.codigo, it.nombre, CentroCodec.codificar(it)) },
            CatalogoMetaEntity(
                version = meta.version,
                descargadoEnMillis = meta.descargadoEn.toEpochMilli(),
                formato = meta.formato,
            ),
        )
    }
}
