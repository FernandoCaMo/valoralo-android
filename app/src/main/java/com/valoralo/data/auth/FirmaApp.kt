package com.valoralo.data.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/** A certificate the system reports for this installation, with its fingerprints. */
data class CertificadoFirma(val sha1: String, val sha256: String, val actual: Boolean)

/** "8F:B8:ED:…" (upper-case hex pairs separated by colons, as Firebase and Play Console show them). */
fun huella(certificado: ByteArray, algoritmo: String): String =
    MessageDigest.getInstance(algoritmo).digest(certificado).joinToString(":") { "%02X".format(it) }

/**
 * Certificates Android sees for the installed app: the current signer(s) and, with key rotation
 * (Play's quantum-ready hybrid signing), the older ones in the lineage. Google Sign-In only works if
 * the current one is registered in Firebase, so About shows them to compare.
 */
object FirmaApp {
    @SuppressLint("PackageManagerGetSignatures")
    @Suppress("DEPRECATION")
    fun certificados(context: Context): List<CertificadoFirma> = runCatching {
        val pm = context.packageManager
        val (actuales, historial) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
                ?: return emptyList()
            if (info.hasMultipleSigners()) info.apkContentsSigners.toList() to emptyList()
            else info.signingCertificateHistory.takeLast(1) to info.signingCertificateHistory.dropLast(1)
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures.orEmpty().toList() to emptyList()
        }
        actuales.map { CertificadoFirma(huella(it.toByteArray(), "SHA-1"), huella(it.toByteArray(), "SHA-256"), actual = true) } +
            historial.reversed().map { CertificadoFirma(huella(it.toByteArray(), "SHA-1"), huella(it.toByteArray(), "SHA-256"), actual = false) }
    }.getOrDefault(emptyList())
}
