package com.valoralo.data.auth

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Non-fatal reports to Crashlytics for failures we need to diagnose in the field (Google sign-in).
 * Only the technical type and code: no uid, e-mail or name. Crashlytics only sends in release.
 */
object InformeErrores {
    private const val TAG = "Valoralo"

    /** [firmas]: SHA-1 of the certificate(s) Android sees for this installation (to compare with Firebase). */
    fun noFatal(operacion: String, e: ErrorCuenta, firmas: List<CertificadoFirma> = emptyList()) {
        if (e.tipo == TipoErrorCuenta.GOOGLE_CANCELADO) return
        val detalle = e.detalle
        // Log.e survives R8 (only v/d/i/w are stripped): visible with adb logcat on a Play build.
        Log.e(TAG, "$operacion falló: ${detalle?.resumen ?: e.tipo.name}", e)
        runCatching {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("operacion", operacion)
                setCustomKey("error_cuenta", e.tipo.name)
                setCustomKey("error_tipo", detalle?.tipo.orEmpty())
                setCustomKey("error_codigo", detalle?.codigo.orEmpty())
                setCustomKey("error_clase", detalle?.clase.orEmpty())
                setCustomKey("error_visible", detalle?.codigoVisible.orEmpty())
                setCustomKey("error_detalle", detalle?.mensaje.orEmpty().take(500))
                setCustomKey("firma_sha1", firmas.joinToString(" | ") { (if (it.actual) "" else "anterior ") + it.sha1 })
                recordException(ErrorNoFatal("$operacion: ${detalle?.resumen ?: e.tipo.name}", e.cause ?: e))
            }
        }
    }
}

/** Gives the Crashlytics issue a readable title ("login_google: …") whatever R8 renamed the cause to. */
class ErrorNoFatal(mensaje: String, causa: Throwable) : Exception(mensaje, causa)
