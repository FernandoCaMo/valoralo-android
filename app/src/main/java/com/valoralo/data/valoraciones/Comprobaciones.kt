package com.valoralo.data.valoraciones

/**
 * After a lost connection: does the review saved on the server match what was sent?
 * Firestore returns numbers as Long and the server tidies the text (validacion.ts), so both sides
 * are normalised the same way before comparing.
 */
fun coincideConGuardada(enviado: Map<String, Any?>, guardado: Map<String, Any?>?): Boolean {
    if (guardado == null) return false
    return notas(enviado["centro"]) == notas(guardado["centro"]) &&
        notas(enviado["instalaciones"]) == notas(guardado["instalaciones"]) &&
        profesorado(enviado["profesorado"]) == profesorado(guardado["profesorado"]) &&
        textoComoServidor(enviado["texto"] as? String) == textoComoServidor(guardado["texto"] as? String)
}

/** Same normalisation as functions/src/validacion.ts: at most one blank line, trimmed. */
fun textoComoServidor(texto: String?): String = (texto ?: "")
    .replace(Regex("\r\n?"), "\n")
    .replace(Regex("[ \t]+\n"), "\n")
    .replace(Regex("\n{3,}"), "\n\n")
    .trim()

private fun notas(bloque: Any?): Map<String, Int> =
    (bloque as? Map<*, *>).orEmpty().mapNotNull { (k, v) -> (v as? Number)?.let { k.toString() to it.toInt() } }.toMap()

private fun profesorado(lista: Any?): Set<Triple<String, Int, Boolean>> =
    (lista as? List<*>).orEmpty().mapNotNull { item ->
        val m = item as? Map<*, *> ?: return@mapNotNull null
        val nota = (m["nota"] as? Number)?.toInt() ?: return@mapNotNull null
        Triple(m["departamento"].toString(), nota, m["otro"] == true)
    }.toSet()
