package com.valoralo.domain.model

/** Groups of centro types, each with its own illustration (never photos or logos from the internet). */
enum class GrupoCentro {
    INFANTIL_PRIMARIA,
    SECUNDARIA,
    FP,
    ADULTOS,
    IDIOMAS,
    MUSICA_DANZA,
    ARTE,
    EDUCACION_ESPECIAL,
    OTRO;

    companion object {
        private val POR_TIPO = mapOf(
            "CEIP" to INFANTIL_PRIMARIA, "CEPR" to INFANTIL_PRIMARIA, "CPR" to INFANTIL_PRIMARIA, "CEI" to INFANTIL_PRIMARIA,
            "IES" to SECUNDARIA, "SES" to SECUNDARIA,
            "CIFP" to FP,
            "CEPER" to ADULTOS, "SEP" to ADULTOS, "IPEP" to ADULTOS, "SIPEP" to ADULTOS,
            "EOI" to IDIOMAS,
            // Conservatorios: elemental and profesional de música, de danza, superiores.
            "CEM" to MUSICA_DANZA, "CPM" to MUSICA_DANZA, "CPD" to MUSICA_DANZA, "CSM" to MUSICA_DANZA, "CSD" to MUSICA_DANZA,
            "EA" to ARTE, "EASD" to ARTE, "ESAD" to ARTE,
            "CEEE" to EDUCACION_ESPECIAL,
        )

        fun de(tipo: String?): GrupoCentro = POR_TIPO[tipo?.trim()?.uppercase()] ?: OTRO
    }
}
