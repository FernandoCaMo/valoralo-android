package com.valoralo.data.local.room

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** One cached centro; the full model is stored as JSON (see [CentroCodec]). */
@Entity(tableName = "centros")
data class CentroEntity(
    @PrimaryKey val codigo: String,
    val nombre: String,
    val json: String,
)

/** Single-row table with the state of the cached catalogue. */
@Entity(tableName = "catalogo_meta")
data class CatalogoMetaEntity(
    @PrimaryKey val id: Int = 0,
    val version: String?,
    val descargadoEnMillis: Long,
    val formato: Int,
)

@Dao
interface CatalogoDao {

    @Query("SELECT * FROM centros ORDER BY nombre COLLATE NOCASE")
    fun observarCentros(): Flow<List<CentroEntity>>

    @Query("SELECT * FROM catalogo_meta WHERE id = 0")
    suspend fun meta(): CatalogoMetaEntity?

    @Query("DELETE FROM centros")
    suspend fun borrarCentros()

    @Insert
    suspend fun insertarCentros(centros: List<CentroEntity>)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun guardarMeta(meta: CatalogoMetaEntity)

    @Transaction
    suspend fun reemplazar(centros: List<CentroEntity>, meta: CatalogoMetaEntity) {
        borrarCentros()
        insertarCentros(centros)
        guardarMeta(meta)
    }
}
