package com.valoralo.data.remote.firestore

import com.valoralo.domain.model.Centro
import com.valoralo.domain.model.Ensenanza
import com.valoralo.domain.model.EstadoValoracion
import com.valoralo.domain.model.Fragmento
import com.valoralo.domain.model.Fuente
import com.valoralo.domain.model.OrigenRevision
import com.valoralo.domain.model.Revision
import com.valoralo.domain.model.NotaCentro
import com.valoralo.domain.model.NotaDepartamento
import com.valoralo.domain.model.NotaInstalaciones
import com.valoralo.domain.model.Rol
import com.valoralo.domain.model.Stats
import com.valoralo.domain.model.StatsDepartamento
import com.valoralo.domain.model.StatsGrupo
import com.valoralo.domain.model.Valoracion
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/*
 * Firestore document (Map) → domain model. Works on plain maps (DocumentSnapshot.data)
 * so it can be unit-tested on the JVM. Missing or malformed fields fall back to empty
 * values instead of crashing the screen.
 */

private val ZONA = ZoneId.of("Europe/Madrid")

fun Map<String, Any?>.aCentro(id: String): Centro = Centro(
    codigo = id,
    nombre = texto("nombre"),
    tipo = texto("tipo"),
    direccion = texto("direccion"),
    cp = texto("cp"),
    localidad = texto("localidad"),
    municipio = texto("municipio"),
    codMunicipio = texto("codMunicipio"),
    provincia = texto("provincia"),
    comunidad = texto("comunidad").ifEmpty { "Andalucía" },
    telefono = textoONull("telefono"),
    email = textoONull("email"),
    web = textoONull("web"),
    fotoUrl = textoONull("fotoUrl"),
    ensenanzas = lista("ensenanzas").mapNotNull { item ->
        val m = item as? Map<*, *> ?: return@mapNotNull null
        val nivel = m["nivel"] as? String ?: return@mapNotNull null
        Ensenanza(nivel, m["nombre"] as? String ?: nivel)
    },
    departamentos = lista("departamentos").filterIsInstance<String>(),
    departamentosFuente = textoONull("departamentosFuente"),
    stats = mapa("stats").let { s ->
        Stats(
            total = s.grupo("total"),
            docente = s.grupo("docente"),
            estudiante = s.grupo("estudiante"),
            otro = s.grupo("otro"),
        )
    },
    fuente = mapa("fuente").let { f ->
        val dataset = f["dataset"] as? String
        if (dataset == null) null else Fuente(dataset, f["curso"] as? String ?: "")
    },
)

/**
 * catalogo/{provincia}-{parte} → summarised centros (what Inicio and its filters need).
 * Entries without codigo are skipped.
 */
fun Map<String, Any?>.aCentrosDeCatalogo(): List<Centro> {
    val provincia = texto("provincia")
    return lista("centros").mapNotNull { item ->
        @Suppress("UNCHECKED_CAST")
        val c = item as? Map<String, Any?> ?: return@mapNotNull null
        val codigo = c["codigo"] as? String ?: return@mapNotNull null
        Centro(
            codigo = codigo,
            nombre = c.texto("nombre"),
            tipo = c.texto("tipo"),
            direccion = "",
            cp = "",
            localidad = c.texto("localidad"),
            municipio = c.texto("municipio"),
            provincia = provincia,
            ensenanzas = c.lista("niveles").filterIsInstance<String>().map { Ensenanza(it, it) },
            stats = Stats(total = StatsGrupo(n = c.entero("resenas") ?: 0, global = c.decimal("nota"))),
        )
    }
}

/** Returns null when the document lacks the minimum data (role and both notes). */
fun Map<String, Any?>.aValoracion(id: String): Valoracion? {
    val rol = enumONull<Rol>(get("rolAutor")) ?: return null
    val centro = mapa("centro")
    val instalaciones = mapa("instalaciones")
    val notaCentro = centro.entero("nota") ?: return null
    val notaInstalaciones = instalaciones.entero("nota") ?: return null
    return Valoracion(
        uid = (get("uid") as? String) ?: id,
        alias = texto("alias"),
        rolAutor = rol,
        verificado = get("verificado") as? Boolean ?: false,
        fecha = fecha(get("fecha")) ?: LocalDate.now(ZONA),
        editada = get("editada") as? Boolean ?: false,
        centro = NotaCentro(
            nota = notaCentro,
            gestion = centro.entero("gestion"),
            convivencia = centro.entero("convivencia"),
            comunicacion = centro.entero("comunicacion"),
        ),
        instalaciones = NotaInstalaciones(
            nota = notaInstalaciones,
            aulas = instalaciones.entero("aulas"),
            equipamiento = instalaciones.entero("equipamiento"),
            limpieza = instalaciones.entero("limpieza"),
            accesibilidad = instalaciones.entero("accesibilidad"),
        ),
        profesorado = lista("profesorado").mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val departamento = m["departamento"] as? String ?: return@mapNotNull null
            val nota = (m["nota"] as? Number)?.toInt() ?: return@mapNotNull null
            NotaDepartamento(departamento, nota)
        },
        texto = texto("texto"),
        estado = enumONull<EstadoValoracion>(get("estado")) ?: EstadoValoracion.EN_REVISION,
        revision = (get("revision") as? Map<*, *>)?.let { r ->
            val origen = enumONull<OrigenRevision>(r["origen"]) ?: return@let null
            Revision(origen, aFragmentos(r["fragmentos"]))
        },
    )
}

/** List of fragments as sent by the functions (also used in callable error details). */
fun aFragmentos(valor: Any?): List<Fragmento> = (valor as? List<*>).orEmpty().mapNotNull { item ->
    val f = item as? Map<*, *> ?: return@mapNotNull null
    Fragmento(
        inicio = f.entero("inicio") ?: -1,
        fin = f.entero("fin") ?: -1,
        texto = f["texto"] as? String ?: return@mapNotNull null,
        motivo = f["motivo"] as? String ?: "",
    )
}

private fun Map<*, *>.grupo(clave: String): StatsGrupo {
    val g = this[clave] as? Map<*, *> ?: return StatsGrupo()
    return StatsGrupo(
        n = g.entero("n") ?: 0,
        global = g.decimal("global"),
        centro = g.decimal("centro"),
        instalaciones = g.decimal("instalaciones"),
        profesorado = g.decimal("profesorado"),
        porDepartamento = (g["porDepartamento"] as? Map<*, *>).orEmpty().mapNotNull { (k, v) ->
            val d = v as? Map<*, *> ?: return@mapNotNull null
            val media = d.decimal("media") ?: return@mapNotNull null
            (k as? String ?: return@mapNotNull null) to StatsDepartamento(d.entero("n") ?: 0, media)
        }.toMap(),
    )
}

private fun Map<String, Any?>.texto(clave: String): String = (get(clave) as? String).orEmpty()
private fun Map<String, Any?>.textoONull(clave: String): String? = (get(clave) as? String)?.takeIf { it.isNotBlank() }
private fun Map<String, Any?>.lista(clave: String): List<*> = get(clave) as? List<*> ?: emptyList<Any>()
private fun Map<String, Any?>.mapa(clave: String): Map<*, *> = get(clave) as? Map<*, *> ?: emptyMap<Any, Any>()
private fun Map<*, *>.entero(clave: String): Int? = (this[clave] as? Number)?.toInt()
private fun Map<*, *>.decimal(clave: String): Double? = (this[clave] as? Number)?.toDouble()

private inline fun <reified E : Enum<E>> enumONull(valor: Any?): E? =
    enumValues<E>().firstOrNull { it.name == valor }

/** Accepts java.util.Date or a Firebase Timestamp (it exposes toDate()). */
private fun fecha(valor: Any?): LocalDate? {
    val date = when (valor) {
        is Date -> valor
        is com.google.firebase.Timestamp -> valor.toDate()
        else -> null
    }
    return date?.toInstant()?.atZone(ZONA)?.toLocalDate()
}
