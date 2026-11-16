package com.valoralo.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CentroEntity::class, CatalogoMetaEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ValoraloDatabase : RoomDatabase() {
    abstract fun catalogoDao(): CatalogoDao
}
