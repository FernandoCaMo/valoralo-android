"""Transformación de una fila del "Directorio de centros docentes no universitarios
de Andalucía" (CSV de la Junta, CC BY 4.0) al documento centros/{codigo} del doc 02.

Solo funciones puras: sin red ni Firestore, para poder probarlas.
"""

DATASET = "directorio-de-centros-docentes-de-andalucia"

# D_DENOMINA -> abreviatura (tipo). None = tipo excluido (decisiones.md, 16/11/2026).
TIPOS = {
    "Colegio de Educación Infantil y Primaria": "CEIP",
    "Colegio de Educación Primaria": "CEPR",
    "Colegio Público Rural": "CPR",
    "Instituto de Educación Secundaria": "IES",
    "Sección de Educación Secundaria Obligatoria": "SES",
    "Centro público integrado de formación profesional": "CIFP",
    "Centro Específico de Educación Especial": "CEEE",
    "Centro de Educación Permanente": "CEPER",
    "Sección de Educación Permanente": "SEP",
    "Instituto Provincial de Educación Permanente": "IPEP",
    "Sección de Instituto Provincial de Educación Permanente": "SIPEP",
    "Escuela Oficial de Idiomas": "EOI",
    "Conservatorio Elemental de Música": "CEM",
    "Conservatorio Profesional de Música": "CPM",
    "Conservatorio Profesional de Danza": "CPD",
    "Conservatorio Superior de Música": "CSM",
    "Conservatorio Superior de Danza": "CSD",
    "Escuela de Arte": "EA",
    "Escuela de Arte y Superior de Diseño": "EASD",
    "Escuela Superior de Arte Dramático": "ESAD",
    # Excluidos
    "Escuela Infantil": None,
    "Escuela Municipal de Música": None,
    "Escuela Municipal de Música y Danza": None,
    "Escuela Municipal de Danza": None,
    "Centro de Convenio": None,
    "Instituto Andaluz del Deporte": None,
}

# Columna S/N del CSV -> (nivel, nombre). The order is the display order.
# Column names are the real CSV headers (they differ from the published dictionary).
CAMPOS_ENSENANZAS = {
    "pub_adh_inf1": ("Infantil", "Educación Infantil (primer ciclo)"),
    "pub_noadh_inf1": ("Infantil", "Educación Infantil (primer ciclo)"),
    "pub_inf2": ("Infantil", "Educación Infantil (segundo ciclo)"),
    "pub_pri": ("Primaria", "Educación Primaria"),
    "pub_eso": ("ESO", "Educación Secundaria Obligatoria"),
    "pub_bach_ord": ("Bachillerato", "Bachillerato"),
    "pub_bach_adul": ("Bachillerato", "Bachillerato para personas adultas"),
    "pub_bach_semi_dist": ("Bachillerato", "Bachillerato semipresencial / a distancia"),
    "pub_fpbasica": ("FP Grado Básico", "FP Grado Básico"),
    "pub_fpgm_ord": ("FP Grado Medio", "FP Grado Medio"),
    "pub_fpgm_adul": ("FP Grado Medio", "FP Grado Medio para personas adultas"),
    "pub_fpgm_semi_dist": ("FP Grado Medio", "FP Grado Medio semipresencial / a distancia"),
    "pub_ce_gm": ("FP Grado Medio", "Cursos de especialización de Grado Medio"),
    "pub_fpgs_ord": ("FP Grado Superior", "FP Grado Superior"),
    "pub_fpgs_adul": ("FP Grado Superior", "FP Grado Superior para personas adultas"),
    "pub_fpgs_semi_dist": ("FP Grado Superior", "FP Grado Superior semipresencial / a distancia"),
    "pub_ce_gs": ("FP Grado Superior", "Cursos de especialización de Grado Superior"),
    "pub_ee": ("Educación Especial", "Educación Especial"),
    "pub_Adul_formal": ("Personas Adultas", "Educación para personas adultas"),
    "pub_planes": ("Personas Adultas", "Planes educativos no formales"),
    "pub_idi": ("Idiomas", "Enseñanzas de idiomas"),
    "pub_idi_libre": ("Idiomas", "Idiomas (matrícula libre)"),
    "pub_Ens_no_reg_idi": ("Idiomas", "Cursos de actualización y perfeccionamiento"),
    "pub_Ens_Mus": ("Música", "Enseñanzas de Música"),
    "pub_Ens_no_reg_mus": ("Música", "Música (no reglada)"),
    "pub_Ens_Dan": ("Danza", "Enseñanzas de Danza"),
    "pub_Ens_no_reg_dan": ("Danza", "Danza (no reglada)"),
    "pub_ccff_gm_apd": ("Artes Plásticas y Diseño", "Ciclos de Grado Medio de Artes Plásticas y Diseño"),
    "pub_ccff_gs_apd": ("Artes Plásticas y Diseño", "Ciclos de Grado Superior de Artes Plásticas y Diseño"),
    "pub_ESD": ("Enseñanzas Artísticas Superiores", "Enseñanzas Superiores de Diseño"),
    "pub_ESCR": ("Enseñanzas Artísticas Superiores", "Conservación y Restauración de Bienes Culturales"),
    "pub_Dra": ("Arte Dramático", "Enseñanzas de Arte Dramático"),
    "pub_Dep_gm": ("Enseñanzas Deportivas", "Enseñanzas Deportivas de Grado Medio"),
    "pub_Dep_gs": ("Enseñanzas Deportivas", "Enseñanzas Deportivas de Grado Superior"),
}

# Rough bounding box of Andalucía, to discard broken coordinates.
_LAT = (35.9, 38.8)
_LNG = (-7.6, -1.6)


def _texto(valor):
    return (valor or "").strip()


def _o_none(valor):
    v = _texto(valor)
    return v or None


def _numero(valor):
    try:
        return float(_texto(valor).replace(",", "."))
    except ValueError:
        return None


def _geo(fila):
    lat, lng = _numero(fila.get("N_LATITUD")), _numero(fila.get("N_LONGITUD"))
    if lat is None or lng is None:
        return None
    if not (_LAT[0] <= lat <= _LAT[1] and _LNG[0] <= lng <= _LNG[1]):
        return None
    return (lat, lng)


def _ensenanzas(fila):
    resultado = []
    for columna, (nivel, nombre) in CAMPOS_ENSENANZAS.items():
        item = {"nivel": nivel, "nombre": nombre}
        if _texto(fila.get(columna)) == "S" and item not in resultado:
            resultado.append(item)
    return resultado


def ignoradas_con_s(fila):
    """pub_* columns marked 'S' that we don't map (new columns in a future CSV)."""
    return [c for c, v in fila.items() if c.startswith("pub_") and c not in CAMPOS_ENSENANZAS and _texto(v) == "S"]


def fila_a_centro(fila, curso):
    """Devuelve el documento del centro, o None si no se importa (privado o tipo excluido).

    Lanza ValueError con un tipo de centro desconocido, para no importar a ciegas.
    No incluye `stats` (solo backend) ni `web` (el dataset no la trae).
    """
    if _texto(fila.get("D_TIPO")) != "Público":
        return None
    denominacion = _texto(fila.get("D_DENOMINA"))
    if denominacion not in TIPOS:
        raise ValueError(f"Tipo de centro desconocido: {denominacion!r} ({fila.get('codigo')})")
    tipo = TIPOS[denominacion]
    if tipo is None:
        return None

    especifica = _texto(fila.get("D_ESPECIFICA"))
    nombre = especifica if especifica.startswith(tipo + " ") else f"{tipo} {especifica}"
    telefono = _texto(fila.get("N_TELEFONO")).replace(" ", "") or None

    return {
        "codigo": _texto(fila.get("codigo")),
        "nombre": nombre,
        "tipo": tipo,
        "titularidad": "PUBLICA",
        "direccion": _texto(fila.get("D_DOMICILIO")),
        "cp": _texto(fila.get("C_POSTAL")),
        "localidad": _texto(fila.get("D_LOCALIDAD")),
        "municipio": _texto(fila.get("D_MUNICIPIO")),
        "codMunicipio": _texto(fila.get("cod_municipio")),
        "provincia": _texto(fila.get("D_PROVINCIA")),
        "comunidad": "Andalucía",
        "telefono": telefono,
        "email": _o_none(fila.get("Correo_e")),
        "geo": _geo(fila),
        "ensenanzas": _ensenanzas(fila),
        "departamentos": [],
        "fuente": {"dataset": DATASET, "curso": curso},
    }
