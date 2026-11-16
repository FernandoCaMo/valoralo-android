package com.valoralo.di

import com.valoralo.data.remote.firestore.FirestoreCentroRepository
import com.valoralo.data.repository.CentroRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // The sample data (FakeCentroRepository) is kept for tests and previews.
    @Binds
    abstract fun bindCentroRepository(impl: FirestoreCentroRepository): CentroRepository
}
