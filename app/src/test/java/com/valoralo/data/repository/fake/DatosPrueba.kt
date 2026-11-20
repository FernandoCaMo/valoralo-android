package com.valoralo.data.repository.fake

import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.Ensenanza
import com.valoralo.domain.model.EstadoValoracion
import com.valoralo.domain.model.NotaCentro
import com.valoralo.domain.model.NotaDepartamento
import com.valoralo.domain.model.NotaInstalaciones
import com.valoralo.domain.model.Rol
import com.valoralo.domain.model.Valoracion
import java.time.LocalDate

/**
 * Sample data for Phase 2. FICTITIOUS: names, codes, addresses, phones and
 * reviews are made up for testing and do not describe real centres.
 * Municipios and provincias are real (Andalucía only).
 */
internal object DatosPrueba {

    val centros: List<Centro> = listOf(
        Centro(
            codigo = "41700001",
            nombre = "IES Fuente Clara",
            tipo = "IES",
            direccion = "Avenida de la Ribera, 12",
            cp = "41010",
            municipio = "Sevilla",
            provincia = "Sevilla",
            telefono = "955 000 101",
            email = "ies.fuenteclara@ejemplo.es",
            web = "https://ies-guadalquivir.ejemplo.es",
            ensenanzas = listOf(
                Ensenanza("ESO", "Educación Secundaria Obligatoria"),
                Ensenanza("Bachillerato", "Bachillerato de Ciencias y Tecnología"),
                Ensenanza("Bachillerato", "Bachillerato de Humanidades y Ciencias Sociales"),
                Ensenanza("FP Grado Medio", "Sistemas Microinformáticos y Redes (SMR)"),
                Ensenanza("FP Grado Superior", "Desarrollo de Aplicaciones Multiplataforma (DAM)"),
            ),
            departamentos = listOf("Matemáticas", "Lengua Castellana y Literatura", "Inglés", "Informática", "Biología y Geología"),
        ),
        Centro(
            codigo = "29700002",
            nombre = "IES Mirador del Mediterráneo",
            tipo = "IES",
            direccion = "Calle Marinero, 4",
            cp = "29017",
            municipio = "Málaga",
            provincia = "Málaga",
            telefono = "951 000 202",
            email = "ies.mirador@ejemplo.es",
            ensenanzas = listOf(
                Ensenanza("ESO", "Educación Secundaria Obligatoria"),
                Ensenanza("Bachillerato", "Bachillerato de Ciencias y Tecnología"),
                Ensenanza("Bachillerato", "Bachillerato de Artes"),
            ),
            departamentos = listOf("Matemáticas", "Lengua Castellana y Literatura", "Dibujo", "Física y Química", "Geografía e Historia"),
        ),
        Centro(
            codigo = "18700003",
            nombre = "CEIP Alhambra Nueva",
            tipo = "CEIP",
            direccion = "Plaza de los Cármenes, 2",
            cp = "18008",
            municipio = "Granada",
            provincia = "Granada",
            telefono = "958 000 303",
            email = "ceip.alhambranueva@ejemplo.es",
            ensenanzas = listOf(
                Ensenanza("Infantil", "Educación Infantil (segundo ciclo)"),
                Ensenanza("Primaria", "Educación Primaria"),
            ),
            departamentos = listOf("Educación Infantil", "Educación Primaria", "Inglés", "Educación Física"),
        ),
        Centro(
            codigo = "14700004",
            nombre = "IES Patio de los Naranjos",
            tipo = "IES",
            direccion = "Calle del Azahar, 27",
            cp = "14003",
            municipio = "Córdoba",
            provincia = "Córdoba",
            telefono = "957 000 404",
            email = "ies.naranjos@ejemplo.es",
            ensenanzas = listOf(
                Ensenanza("ESO", "Educación Secundaria Obligatoria"),
                Ensenanza("Bachillerato", "Bachillerato General"),
                Ensenanza("FP Grado Superior", "Administración y Finanzas"),
            ),
            departamentos = listOf("Matemáticas", "Lengua Castellana y Literatura", "Inglés", "Administración de Empresas"),
        ),
        Centro(
            codigo = "11700005",
            nombre = "CIFP Bahía Atlántica",
            tipo = "CIFP",
            direccion = "Polígono Tres Caminos, nave 8",
            cp = "11100",
            municipio = "San Fernando",
            provincia = "Cádiz",
            telefono = "956 000 505",
            email = "cifp.bahia@ejemplo.es",
            web = "https://cifp-bahia.ejemplo.es",
            ensenanzas = listOf(
                Ensenanza("FP Grado Medio", "Mantenimiento Electromecánico"),
                Ensenanza("FP Grado Superior", "Automatización y Robótica Industrial"),
                Ensenanza("FP Grado Superior", "Mecatrónica Industrial"),
            ),
            departamentos = listOf("Electricidad y Electrónica", "Fabricación Mecánica", "Formación y Orientación Laboral"),
        ),
    )

    val valoraciones: Map<String, List<Valoracion>> = mapOf(
        "41700001" to listOf(
            v("u1", "Familia de 2º", Rol.ESTUDIANTE, false, "2025-06-15", 4, 4,
                "Matemáticas" to 5, "Informática" to 4,
                texto = "El ciclo de DAM está muy bien organizado y en Informática se nota que preparan las prácticas en empresa con tiempo."),
            v("u2", "Profe de Lengua", Rol.DOCENTE, true, "2025-06-26", 5, 3,
                "Lengua Castellana y Literatura" to 5, "Matemáticas" to 4,
                texto = "Buen ambiente en el claustro y un equipo directivo accesible. Las aulas del edificio antiguo pasan mucho calor en junio."),
            v("u3", "Profe de Bio", Rol.DOCENTE, true, "2025-02-03", 4, 3,
                "Biología y Geología" to 4,
                texto = "Laboratorio pequeño pero bien cuidado. La convivencia en ESO es buena y hay protocolos claros."),
            v("u4", "Estudiante de 1º Bach", Rol.OTRO, false, "2024-11-20", 4, 4,
                "Inglés" to 3,
                texto = "Como madre, la comunicación con el centro es fluida y responden rápido por iPASEN.", editada = true),
            v("u5", "Exalumno 2019", Rol.OTRO, false, "2025-09-10", 2, 2,
                texto = "Reseña retirada por moderación.", estado = EstadoValoracion.OCULTA),
            v("u6", "Familia de Infantil", Rol.ESTUDIANTE, false, "2025-03-12", 3, 3,
                "Matemáticas" to 4, "Inglés" to 3,
                texto = "Bachillerato exigente pero bien explicado. Faltan enchufes en las aulas para los portátiles."),
        ),
        "29700002" to listOf(
            v("u7", "Estudiante de FP", Rol.ESTUDIANTE, false, "2025-05-08", 4, 5,
                "Dibujo" to 5,
                texto = "El aula de artes es increíble, con mucha luz y material. Muy recomendable para el Bachillerato de Artes."),
            v("u8", "Profe interina", Rol.DOCENTE, true, "2025-10-01", 3, 4,
                "Física y Química" to 3, "Matemáticas" to 4,
                texto = "Instalaciones nuevas. La coordinación entre departamentos se puede mejorar."),
            v("u9", "Exalumna 2020", Rol.OTRO, false, "2025-01-17", 4, 4,
                texto = "Centro tranquilo y bien comunicado. Buena atención en secretaría."),
        ),
        "18700003" to listOf(
            v("u10", "Familia del AMPA", Rol.OTRO, false, "2025-04-22", 5, 4,
                "Educación Infantil" to 5, "Inglés" to 4,
                texto = "Mucho cariño en Infantil y un patio amplio. El comedor funciona bien."),
            v("u11", "Maestro de Primaria", Rol.DOCENTE, true, "2025-09-05", 4, 3,
                "Educación Primaria" to 4,
                texto = "Claustro joven y con ganas. El edificio necesita una reforma de ventanas."),
        ),
        "14700004" to listOf(
            v("u12", "Estudiante de 4º", Rol.ESTUDIANTE, false, "2025-06-30", 3, 2,
                "Administración de Empresas" to 4,
                texto = "El ciclo de Administración y Finanzas está bien, pero los ordenadores son muy antiguos."),
            v("u13", "Familia del barrio", Rol.ESTUDIANTE, false, "2025-02-14", 4, 3,
                "Matemáticas" to 4, "Lengua Castellana y Literatura" to 5,
                texto = "Buen ambiente en clase y profesorado que se preocupa por la selectividad."),
            // Teacher pending verification: counts as OTRO until verified (doc 04).
            v("u14", "Nuevo en el claustro", Rol.OTRO, false, "2025-08-01", 3, 3,
                "Inglés" to 3,
                texto = "Recién llegado al centro. Buena acogida, aunque falta espacio en la sala de profesores."),
        ),
        "11700005" to listOf(
            v("u15", "Profe de Taller", Rol.ESTUDIANTE, false, "2025-07-11", 5, 5,
                "Electricidad y Electrónica" to 5, "Fabricación Mecánica" to 4,
                texto = "Talleres muy bien equipados y muchas empresas para las prácticas. Salida laboral asegurada."),
            v("u16", "Tutor FCT", Rol.DOCENTE, true, "2025-03-28", 5, 4,
                "Formación y Orientación Laboral" to 4, "Electricidad y Electrónica" to 5,
                texto = "Relación excelente con las empresas de la bahía. Equipo docente muy implicado."),
            v("u17", "Estudiante de DAM", Rol.OTRO, false, "2025-10-20", 4, 5,
                texto = "Mi hijo está encantado. Instalaciones modernas y limpias."),
        ),
    )

    private fun v(
        uid: String,
        alias: String,
        rol: Rol,
        verificado: Boolean,
        fecha: String,
        centro: Int,
        instalaciones: Int,
        vararg departamentos: Pair<String, Int>,
        texto: String,
        editada: Boolean = false,
        estado: EstadoValoracion = EstadoValoracion.PUBLICADA,
    ) = Valoracion(
        uid = uid,
        alias = alias,
        rolAutor = rol,
        verificado = verificado,
        fecha = LocalDate.parse(fecha),
        editada = editada,
        centro = NotaCentro(centro),
        instalaciones = NotaInstalaciones(instalaciones),
        profesorado = departamentos.map { NotaDepartamento(it.first, it.second) },
        texto = texto,
        estado = estado,
    )
}
