package com.valoralo.data.repository.fake

import com.valoralo.data.repository.CentroRepository
import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.Valoracion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory catalogue with sample data (Phase 2). Replaced by Firestore in Phase 1/5. */
@Singleton
class FakeCentroRepository @Inject constructor() : CentroRepository {

    private val centros: List<Centro> = DatosPrueba.centros.map { centro ->
        centro.copy(stats = calcularStatsDePrueba(DatosPrueba.valoraciones[centro.codigo].orEmpty()))
    }

    override fun observarCentros(): Flow<List<Centro>> = flowOf(centros)

    override fun observarCentro(codigo: String): Flow<Centro?> =
        flowOf(centros.firstOrNull { it.codigo == codigo })

    override fun observarValoraciones(codigo: String): Flow<List<Valoracion>> =
        flowOf(DatosPrueba.valoraciones[codigo].orEmpty())
}
