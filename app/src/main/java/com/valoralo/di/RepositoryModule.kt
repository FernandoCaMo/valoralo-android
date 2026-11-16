package com.valoralo.di

import com.valoralo.data.local.room.RoomCatalogoLocal
import com.valoralo.data.remote.firestore.FirestoreCentrosRemoto
import com.valoralo.data.repository.CatalogoLocal
import com.valoralo.data.repository.CentroRepository
import com.valoralo.data.repository.CentroRepositoryImpl
import com.valoralo.data.repository.CentrosRemoto
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // The sample data (FakeCentroRepository) is kept for tests and previews.
    @Binds
    abstract fun bindCentroRepository(impl: CentroRepositoryImpl): CentroRepository

    @Binds
    abstract fun bindCatalogoLocal(impl: RoomCatalogoLocal): CatalogoLocal

    @Binds
    abstract fun bindCentrosRemoto(impl: FirestoreCentrosRemoto): CentrosRemoto
}
