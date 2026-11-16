"""Departamentos de cada centro (docs/decisiones.md).

Fuente: "Plantillas orgánicas de centros docentes públicos" (Junta de Andalucía, CC BY 4.0).
Cada puesto de la plantilla se traduce a un departamento con la tabla versionada
especialidades_departamentos.csv. Si el centro no tiene plantilla, o es de adultos
(CEPER, SEP), se usa la lista estándar por tipo de departamentos_estandar.csv.
"""
import collections
import csv
import unicodedata

# Adult education is organised by ámbitos, not by the specialties of its staff.
SIEMPRE_ESTANDAR = {"CEPER", "SEP"}


def cargar_tabla(ruta):
    """especialidad (6 caracteres) -> departamento ('' = ignorar)."""
    with open(ruta, encoding="utf-8") as f:
        return {r["especialidad"]: r["departamento"].strip() for r in csv.DictReader(f, delimiter=";")}


def cargar_estandar(ruta):
    """tipo -> [departamentos], en el orden del CSV."""
    with open(ruta, encoding="utf-8") as f:
        return {r["tipo"]: [d.strip() for d in r["departamentos"].split("|") if d.strip()]
                for r in csv.DictReader(f, delimiter=";")}


def departamentos_por_centro(filas, tabla):
    """Devuelve ({codigo_centro: {departamentos}}, {puesto_sin_fila_en_tabla: n}).

    The specialty code is the PUESTO code without its 2-char language prefix
    (00 = normal, 11 = bilingual English…). Rows with 0 posts are ignored.
    """
    por_centro = collections.defaultdict(set)
    desconocidas = collections.Counter()
    for fila in filas:
        codigo_centro = fila["CENTRO"].split("-", 1)[0].strip()
        codigo_puesto = fila["PUESTO"].split("-", 1)[0].strip()
        try:
            plazas = int(fila.get("PLA") or 0)
        except ValueError:
            plazas = 0
        if plazas <= 0:
            continue
        especialidad = codigo_puesto[2:]
        if especialidad not in tabla:
            desconocidas[fila["PUESTO"].strip()] += 1
            continue
        if tabla[especialidad]:
            por_centro[codigo_centro].add(tabla[especialidad])
    return dict(por_centro), dict(desconocidas)


def _clave(texto):
    sin_tildes = unicodedata.normalize("NFD", texto.lower())
    return "".join(c for c in sin_tildes if unicodedata.category(c) != "Mn")


def asignar(codigo, tipo, por_centro, estandar, curso_plantilla):
    """Devuelve (departamentos, departamentosFuente, departamentosCurso)."""
    desde_plantilla = por_centro.get(codigo)
    if tipo not in SIEMPRE_ESTANDAR and desde_plantilla:
        return sorted(desde_plantilla, key=_clave), "plantilla", curso_plantilla
    return list(estandar.get(tipo, [])), "estandar", None
