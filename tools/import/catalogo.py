"""Catálogo resumido por provincia: catalogo/{provincia}-{parte}.

The app downloads these few documents on first launch instead of the whole
centros collection (~10 reads instead of ~3,700). Each summary carries only what
Inicio and its filters need; the detail screen reads the full centro live.
A Firestore document is limited to 1 MiB, so a provincia is split into parts
when its summary would not fit with a safety margin.
"""
import collections
import json
import unicodedata

# Safety margin under Firestore's 1 MiB limit (field names, indexes, encoding overhead).
LIMITE_BYTES = 800_000


def resumen_centro(centro, stats_total):
    niveles = []
    for e in centro.get("ensenanzas", []):
        if e["nivel"] not in niveles:
            niveles.append(e["nivel"])
    stats_total = stats_total or {}
    return {
        "codigo": centro["codigo"],
        "nombre": centro["nombre"],
        "tipo": centro["tipo"],
        "localidad": centro["localidad"],
        "municipio": centro["municipio"],
        "niveles": niveles,
        "nota": stats_total.get("global"),
        "resenas": stats_total.get("n", 0) or 0,
    }


def id_documento(provincia, parte):
    sin_tildes = unicodedata.normalize("NFD", provincia.strip().lower())
    slug = "".join(c for c in sin_tildes if unicodedata.category(c) != "Mn").replace(" ", "-")
    return f"{slug}-{parte}"


def tamano_estimado(resumenes):
    """Upper bound of the stored size: UTF-8 JSON plus 25 % for Firestore's per-field overhead."""
    return int(len(json.dumps(resumenes, ensure_ascii=False).encode("utf-8")) * 1.25) + 1024


def _partir(resumenes, limite_bytes):
    partes, actual = [], []
    for r in resumenes:
        if actual and tamano_estimado(actual + [r]) > limite_bytes:
            partes.append(actual)
            actual = []
        actual.append(r)
    if actual:
        partes.append(actual)
    return partes


def documentos_catalogo(centros, stats_por_codigo, limite_bytes=LIMITE_BYTES):
    """{doc_id: {provincia, parte, partes, centros: [resumen...]}} keeping the input order."""
    por_provincia = collections.OrderedDict()
    for c in centros:
        por_provincia.setdefault(c["provincia"], []).append(
            resumen_centro(c, stats_por_codigo.get(c["codigo"])))
    documentos = {}
    for provincia, resumenes in por_provincia.items():
        partes = _partir(resumenes, limite_bytes)
        for i, parte in enumerate(partes, start=1):
            documentos[id_documento(provincia, i)] = {
                "provincia": provincia, "parte": i, "partes": len(partes), "centros": parte,
            }
    return documentos
