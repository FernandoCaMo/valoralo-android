package com.valoralo.data.repository

import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.Valoracion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CentroRepository {
    /** Catalogue for Inicio (summarised centros with their averages). */
    fun observarCentros(): Flow<List<Centro>>

    /** Non-null while the catalogue is being downloaded. */
    val progresoDescarga: StateFlow<ProgresoDescarga?>

    /** Full centro, live. */
    fun observarCentro(codigo: String): Flow<Centro?>
    fun observarValoraciones(codigo: String): Flow<List<Valoracion>>
}
