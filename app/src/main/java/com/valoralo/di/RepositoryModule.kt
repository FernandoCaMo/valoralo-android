package com.valoralo.di

import com.valoralo.data.repository.CentroRepository
import com.valoralo.data.repository.fake.FakeCentroRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // Phase 2: sample data. Swap for the Firestore implementation later.
    @Binds
    abstract fun bindCentroRepository(impl: FakeCentroRepository): CentroRepository
}
