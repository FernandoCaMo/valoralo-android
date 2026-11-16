package com.valoralo.data.repository

import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.Valoracion
import kotlinx.coroutines.flow.Flow

interface CentroRepository {
    fun observarCentros(): Flow<List<Centro>>
    fun observarCentro(codigo: String): Flow<Centro?>
    fun observarValoraciones(codigo: String): Flow<List<Valoracion>>
}
