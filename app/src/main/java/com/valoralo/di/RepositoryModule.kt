package com.valoralo.di

import com.valoralo.data.auth.CuentaRepository
import com.valoralo.data.auth.FirebaseCuentaRepository
import com.valoralo.data.destacadas.DestacadasRepository
import com.valoralo.data.destacadas.FirebaseDestacadasRepository
import com.valoralo.data.favoritos.FavoritosRepository
import com.valoralo.data.favoritos.FirebaseFavoritosRepository
import com.valoralo.data.local.room.RoomCatalogoLocal
import com.valoralo.data.valoraciones.FirebaseValoracionRepository
import com.valoralo.data.valoraciones.ValoracionRepository
import com.valoralo.data.verificacion.FirebaseVerificacionRepository
import com.valoralo.data.verificacion.VerificacionRepository
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

    @Binds
    abstract fun bindCuentaRepository(impl: FirebaseCuentaRepository): CuentaRepository

    @Binds
    abstract fun bindValoracionRepository(impl: FirebaseValoracionRepository): ValoracionRepository

    @Binds
    abstract fun bindVerificacionRepository(impl: FirebaseVerificacionRepository): VerificacionRepository

    @Binds
    abstract fun bindFavoritosRepository(impl: FirebaseFavoritosRepository): FavoritosRepository

    @Binds
    abstract fun bindDestacadasRepository(impl: FirebaseDestacadasRepository): DestacadasRepository
}
