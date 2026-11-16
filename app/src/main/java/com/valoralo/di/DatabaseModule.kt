package com.valoralo.di

import android.content.Context
import androidx.room.Room
import com.valoralo.data.local.room.CatalogoDao
import com.valoralo.data.local.room.ValoraloDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ValoraloDatabase =
        Room.databaseBuilder(context, ValoraloDatabase::class.java, "valoralo.db")
            // Only a cache of data that lives in Firestore: safe to drop on schema changes.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideCatalogoDao(db: ValoraloDatabase): CatalogoDao = db.catalogoDao()
}
