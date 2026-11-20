package com.valoralo.data.auth

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.valoralo.data.red.Comprobacion
import com.valoralo.data.red.LlamadasFunciones
import com.valoralo.data.valoraciones.ErrorPublicacion
import com.valoralo.data.valoraciones.TipoErrorPublicacion
import com.valoralo.domain.model.CONDICIONES_VIGENTES
import com.valoralo.domain.model.PerfilNuevo
import com.valoralo.domain.model.Rol
import com.valoralo.domain.model.Sesion
import com.valoralo.domain.model.Usuario
import com.valoralo.domain.usecase.normalizarAlias
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseCuentaRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val llamadas: LlamadasFunciones,
) : CuentaRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        auth.setLanguageCode("es") // password reset e-mails in Spanish
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val sesion: StateFlow<Sesion> = usuarioAuth()
        .flatMapLatest { usuario -> if (usuario == null) flowOf(Sesion.SinSesion) else sesionDe(usuario) }
        .stateIn(scope, SharingStarted.Eagerly, Sesion.Cargando)

    private fun usuarioAuth(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** Emits once the profile document has been read (perfil = null means "not created yet"). */
    private fun sesionDe(usuario: FirebaseUser): Flow<Sesion> = callbackFlow {
        val conGoogle = usuario.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
        val registro = perfil(usuario.uid).addSnapshotListener { snap, error ->
            if (error != null) {
                // E.g. right after deleting the account; the auth listener will follow.
                Log.w(TAG, "Error leyendo el perfil", error)
                return@addSnapshotListener
            }
            val datos = snap?.takeIf { it.exists() }
            trySend(
                Sesion.Iniciada(
                    uid = usuario.uid,
                    correo = usuario.email,
                    perfil = datos?.let {
                        Usuario(
                            uid = usuario.uid,
                            alias = it.getString("alias").orEmpty(),
                            rol = Rol.entries.firstOrNull { r -> r.name == it.getString("rol") } ?: Rol.OTRO,
                            verificado = it.getBoolean("verificado") ?: false,
                            condiciones = it.getString("condiciones"),
                        )
                    },
                    conGoogle = conGoogle,
                ),
            )
        }
        awaitClose { registro.remove() }
    }

    override suspend fun iniciarSesion(correo: String, contrasena: String) = traducirErrores {
        auth.signInWithEmailAndPassword(correo.trim(), contrasena).await()
        Unit
    }

    override suspend fun registrar(correo: String, contrasena: String, perfil: PerfilNuevo) = traducirErrores {
        val usuario = auth.createUserWithEmailAndPassword(correo.trim(), contrasena).await().user
            ?: throw ErrorCuenta(TipoErrorCuenta.DESCONOCIDO)
        try {
            escribirPerfil(usuario.uid, perfil)
        } catch (e: Exception) {
            // Don't leave an Auth account without profile behind — unless we don't know whether the
            // profile was created (connection lost): then the account stays, and if the profile is
            // missing the app asks to complete it.
            if (!(e is ErrorCuenta && e.tipo == TipoErrorCuenta.INCIERTO)) runCatching { usuario.delete().await() }
            throw e
        }
    }

    override suspend fun iniciarConGoogle(idToken: String) = traducirErrores {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        Unit
    }

    override suspend fun crearPerfil(perfil: PerfilNuevo) = traducirErrores {
        escribirPerfil(usuarioActual().uid, perfil)
    }

    override suspend fun cancelarRegistro() = traducirErrores {
        auth.currentUser?.delete()?.await()
        auth.signOut()
        Unit
    }

    /** Through the function cambiarAlias: the alias is public and is validated on the server. */
    override suspend fun cambiarAlias(alias: String) = traducirErrores {
        val uid = usuarioActual().uid
        val nuevo = normalizarAlias(alias)
        llamarFuncion("cambiarAlias", mapOf("alias" to nuevo)) {
            if (perfil(uid).get(Source.SERVER).await().getString("alias") == nuevo) Comprobacion.Hecha() else Comprobacion.NoHecha
        }
        Unit
    }

    override suspend fun cambiarRol(rol: Rol) = traducirErrores {
        require(rol == Rol.ESTUDIANTE || rol == Rol.OTRO) { "Solo ESTUDIANTE u OTRO" }
        perfil(usuarioActual().uid).update("rol", rol.name).await()
        Unit
    }

    override suspend fun recuperarContrasena(correo: String) = traducirErrores {
        auth.sendPasswordResetEmail(correo.trim()).await()
        Unit
    }

    override suspend fun cerrarSesion() {
        auth.signOut()
    }

    override suspend fun eliminarCuenta(reautenticacion: Reautenticacion) = traducirErrores {
        val usuario = usuarioActual()
        val credencial = when (reautenticacion) {
            is Reautenticacion.ConContrasena ->
                EmailAuthProvider.getCredential(usuario.email.orEmpty(), reautenticacion.contrasena)
            is Reautenticacion.ConGoogle -> GoogleAuthProvider.getCredential(reautenticacion.idToken, null)
        }
        // Re-authenticate first so that nothing is deleted if the user can't prove who they are.
        usuario.reauthenticate(credencial).await()
        perfil(usuario.uid).delete().await()
        usuario.delete().await()
        // alBorrarUsuario (Cloud Function) removes the rest: reviews, likes, reports, blocks…
        Unit
    }

    /**
     * Through the function crearPerfil (the rules no longer let the app create usuarios/{uid}): it validates
     * the alias and writes { alias, rol, verificado: false, mayorDe14: true, fechaAlta, condiciones }.
     */
    private suspend fun escribirPerfil(uid: String, nuevo: PerfilNuevo) {
        llamarFuncion(
            "crearPerfil",
            mapOf(
                "alias" to normalizarAlias(nuevo.alias),
                "rol" to nuevo.rol.name,
                "mayorDe14" to true,
                // Accepted with the checkbox of the sign-up form.
                "condiciones" to CONDICIONES_VIGENTES,
            ),
        ) {
            if (perfil(uid).get(Source.SERVER).await().exists()) Comprobacion.Hecha() else Comprobacion.NoHecha
        }
    }

    /** [LlamadasFunciones] with account errors: the server's texts (alias…) and honest network messages. */
    private suspend fun llamarFuncion(nombre: String, datos: Map<String, Any?>, comprobar: suspend () -> Comprobacion) {
        try {
            llamadas.llamar(nombre, datos, mensajeIncierto = TipoErrorCuenta.INCIERTO.mensaje, comprobar = comprobar)
        } catch (e: ErrorPublicacion) {
            throw when (e.tipo) {
                TipoErrorPublicacion.SIN_CONEXION -> ErrorCuenta(TipoErrorCuenta.SIN_CONEXION)
                TipoErrorPublicacion.INCIERTO -> ErrorCuenta(TipoErrorCuenta.INCIERTO)
                TipoErrorPublicacion.CORTE_SIN_HACER -> ErrorCuenta(TipoErrorCuenta.RECHAZADO_SERVIDOR, mensajeUsuario = e.message)
                TipoErrorPublicacion.SIN_SESION -> ErrorCuenta(TipoErrorCuenta.REQUIERE_LOGIN_RECIENTE)
                else -> ErrorCuenta(TipoErrorCuenta.RECHAZADO_SERVIDOR, e, mensajeUsuario = e.message, campo = e.campo)
            }
        }
    }

    override suspend fun aceptarCondiciones() = traducirErrores {
        perfil(usuarioActual().uid).update("condiciones", CONDICIONES_VIGENTES).await()
        Unit
    }

    private fun perfil(uid: String) = db.collection("usuarios").document(uid)

    private fun usuarioActual(): FirebaseUser =
        auth.currentUser ?: throw ErrorCuenta(TipoErrorCuenta.REQUIERE_LOGIN_RECIENTE)

    private suspend fun <T> traducirErrores(bloque: suspend () -> T): T = try {
        bloque()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ErrorCuenta) {
        throw e
    } catch (e: FirebaseAuthException) {
        throw ErrorCuenta(tipoErrorAuth(e.errorCode), e)
    } catch (e: FirebaseNetworkException) {
        throw ErrorCuenta(TipoErrorCuenta.SIN_CONEXION, e)
    } catch (e: FirebaseFirestoreException) {
        val tipo = if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) TipoErrorCuenta.SIN_CONEXION
        else TipoErrorCuenta.DESCONOCIDO
        throw ErrorCuenta(tipo, e)
    } catch (e: Exception) {
        Log.w(TAG, "Error de cuenta", e)
        throw ErrorCuenta(TipoErrorCuenta.DESCONOCIDO, e)
    }

    private companion object {
        const val TAG = "Cuenta"
    }
}
