package com.valoralo.domain.usecase

import com.valoralo.domain.model.Valoracion

/** Same limits as functions/src/validacion.ts. */
const val LIMITE_TEXTO_VALORACION = 1000

enum class ErrorFormulario(val mensaje: String) {
    FALTA_NOTA_CENTRO("Valora el centro en general."),
    FALTA_NOTA_INSTALACIONES("Valora las aulas e instalaciones."),
    TEXTO_LARGO("El texto puede tener como máximo 1.000 caracteres."),
    OTRO_DEPARTAMENTO_NO_VALIDO("\"Otro departamento\" debe tener entre 3 y 40 letras."),
    OTRO_DEPARTAMENTO_SIN_NOTA("Pon una nota a cada departamento que añadas."),
    DEPARTAMENTO_REPETIDO("Has puesto el mismo departamento dos veces."),
}

data class OtroDepartamento(val nombre: String, val nota: Int?)

/** Review form: centro and instalaciones are required; departments and text are optional. */
data class FormularioValoracion(
    val notaCentro: Int? = null,
    val subnotasCentro: Map<String, Int> = emptyMap(),
    val notaInstalaciones: Int? = null,
    val subnotasInstalaciones: Map<String, Int> = emptyMap(),
    /** Departments of the centro → note (null = not rated). */
    val departamentos: Map<String, Int?> = emptyMap(),
    val otros: List<OtroDepartamento> = emptyList(),
    val texto: String = "",
) {
    fun error(departamentosDelCentro: List<String>): ErrorFormulario? {
        if (notaCentro == null) return ErrorFormulario.FALTA_NOTA_CENTRO
        if (notaInstalaciones == null) return ErrorFormulario.FALTA_NOTA_INSTALACIONES
        if (texto.trim().length > LIMITE_TEXTO_VALORACION) return ErrorFormulario.TEXTO_LARGO
        val vistos = departamentos.filterValues { it != null }.keys.map { it.normalizado() }.toMutableSet()
        for (otro in otros) {
            val nombre = limpiar(otro.nombre)
            if (nombre.length !in 3..40 || !OTRO_VALIDO.matches(nombre)) return ErrorFormulario.OTRO_DEPARTAMENTO_NO_VALIDO
            if (otro.nota == null) return ErrorFormulario.OTRO_DEPARTAMENTO_SIN_NOTA
            val clave = nombre.normalizado()
            if (clave in vistos || departamentosDelCentro.any { it.normalizado() == clave }) {
                return ErrorFormulario.DEPARTAMENTO_REPETIDO
            }
            vistos += clave
        }
        return null
    }

    /** Payload for the publicarValoracion callable. */
    fun paraEnviar(codigo: String): Map<String, Any> = mapOf(
        "codigo" to codigo,
        "centro" to mapOf("nota" to checkNotNull(notaCentro)) + subnotasCentro,
        "instalaciones" to mapOf("nota" to checkNotNull(notaInstalaciones)) + subnotasInstalaciones,
        "profesorado" to departamentos.mapNotNull { (d, n) -> n?.let { mapOf("departamento" to d, "nota" to it) } } +
            otros.map { mapOf("departamento" to limpiar(it.nombre), "nota" to checkNotNull(it.nota), "otro" to true) },
        "texto" to texto.trim(),
    )

    companion object {
        private val OTRO_VALIDO = "^[\\p{L} -]+$".toRegex()
        private fun limpiar(s: String) = s.trim().replace("\\s+".toRegex(), " ")

        fun desde(v: Valoracion, departamentosDelCentro: List<String>): FormularioValoracion {
            val (delCentro, otros) = v.profesorado.partition { it.departamento in departamentosDelCentro }
            return FormularioValoracion(
                notaCentro = v.centro.nota,
                subnotasCentro = listOfNotNull(
                    v.centro.gestion?.let { "gestion" to it },
                    v.centro.convivencia?.let { "convivencia" to it },
                    v.centro.comunicacion?.let { "comunicacion" to it },
                ).toMap(),
                notaInstalaciones = v.instalaciones.nota,
                subnotasInstalaciones = listOfNotNull(
                    v.instalaciones.aulas?.let { "aulas" to it },
                    v.instalaciones.equipamiento?.let { "equipamiento" to it },
                    v.instalaciones.limpieza?.let { "limpieza" to it },
                    v.instalaciones.accesibilidad?.let { "accesibilidad" to it },
                ).toMap(),
                departamentos = delCentro.associate { it.departamento to it.nota },
                otros = otros.map { OtroDepartamento(it.departamento, it.nota) },
                texto = v.texto,
            )
        }
    }
}
