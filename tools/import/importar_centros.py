"""Importa los centros públicos de Andalucía a Firestore (centros/{codigo}).

Fuente: "Directorio de centros docentes no universitarios de Andalucía",
Junta de Andalucía, CC BY 4.0 (ver docs/02-arquitectura-y-datos.md).

Hay que elegir siempre el destino; sin él no se escribe nada:

  # 1. Revisar sin escribir: genera un JSON y el recuento por tipo
  python importar_centros.py --provincia Huelva --dry-run --salida huelva.json

  # 2. Emulador de Firestore (firebase emulators:start --only firestore)
  python importar_centros.py --provincia Huelva --emulador localhost:8080 --proyecto demo-valoralo

  # 3. Firestore real. Credenciales: --credenciales RUTA o la variable
  #    GOOGLE_APPLICATION_CREDENTIALS. Nunca guardes la clave en el repo.
  python importar_centros.py --provincia Huelva --produccion --credenciales RUTA

Re-importar es seguro: escribe con merge y solo crea `stats` vacíos en los
centros nuevos, así que no pisa las valoraciones agregadas.
"""
import argparse
import collections
import csv
import datetime
import io
import json
import os
import sys
import unicodedata
import urllib.request

from catalogo import documento_de_cada_centro, documentos_catalogo, tamano_estimado
from departamentos import asignar, cargar_estandar, cargar_tabla, departamentos_por_centro
from mapeo import fila_a_centro, ignoradas_con_s

URL_CSV_2024_25 = (
    "https://www.juntadeandalucia.es/datosabiertos/portal/dataset/"
    "e039df22-4b82-4d0d-9884-0ab5952e24e4/resource/b5924e81-0b53-4418-9d93-b1f39ba1ef65/download/da_centros.csv"
)
CURSO_2024_25 = "2024/2025"
URL_PLANTILLA_2025_26 = (
    "https://www.juntadeandalucia.es/datosabiertos/portal/dataset/"
    "b2b4a3f6-690e-4e0e-b2ab-50e3da65c08b/resource/a639f1f4-c949-421f-887d-65496d9d69dd/download/plantilla_organica_2025_2026.csv"
)
CURSO_PLANTILLA_2025_26 = "2025/2026"
AQUI = os.path.dirname(os.path.abspath(__file__))
TABLA_ESPECIALIDADES = os.path.join(AQUI, "especialidades_departamentos.csv")
TABLA_ESTANDAR = os.path.join(AQUI, "departamentos_estandar.csv")
LOTE = 400  # Firestore allows 500 writes per batch


def stats_vacios():
    grupo = {"n": 0, "global": None, "centro": None, "instalaciones": None,
             "profesorado": None, "porDepartamento": {}}
    return {rol: dict(grupo) for rol in ("total", "docente", "estudiante", "otro")}


def _normalizar(texto):
    sin_tildes = unicodedata.normalize("NFD", texto.strip().lower())
    return "".join(c for c in sin_tildes if unicodedata.category(c) != "Mn")


def leer_csv(ruta_o_url, delimitador=";"):
    if ruta_o_url.startswith("https://"):
        with urllib.request.urlopen(ruta_o_url, timeout=120) as respuesta:
            datos = respuesta.read()
    else:
        with open(ruta_o_url, "rb") as f:
            datos = f.read()
    return list(csv.DictReader(io.StringIO(datos.decode("utf-8-sig")), delimiter=delimitador))


def anadir_departamentos(centros, filas_plantilla, curso_plantilla, estandar):
    """Fills departamentos / departamentosFuente / departamentosCurso. Returns warnings."""
    por_centro, desconocidas = departamentos_por_centro(filas_plantilla, cargar_tabla(TABLA_ESPECIALIDADES))
    for centro in centros:
        lista, fuente, curso = asignar(centro["codigo"], centro["tipo"], por_centro, estandar, curso_plantilla)
        centro["departamentos"] = lista
        centro["departamentosFuente"] = fuente
        centro["departamentosCurso"] = curso
    return [f"especialidad sin fila en la tabla: {p} ({n} filas)" for p, n in desconocidas.items()]


def transformar(filas, provincia, curso):
    """Devuelve (centros, excluidos_por_denominacion, avisos). Lanza ValueError ante datos raros."""
    if provincia:
        objetivo = _normalizar(provincia)
        filas = [f for f in filas if _normalizar(f.get("D_PROVINCIA", "")) == objetivo]
        if not filas:
            raise ValueError(f"No hay filas de la provincia {provincia!r}")

    centros, excluidos, avisos = [], collections.Counter(), []
    for fila in filas:
        for columna in ignoradas_con_s(fila):
            avisos.append(f"{fila.get('codigo')}: columna sin mapear con 'S': {columna}")
        centro = fila_a_centro(fila, curso)
        if centro is None:
            if fila.get("D_TIPO", "").strip() == "Público":
                excluidos[fila.get("D_DENOMINA", "").strip()] += 1
            continue
        if not (len(centro["codigo"]) == 8 and centro["codigo"].isdigit()):
            raise ValueError(f"Código de centro no válido: {centro['codigo']!r}")
        if centro["geo"] is None:
            avisos.append(f"{centro['codigo']}: sin coordenadas válidas")
        centros.append(centro)

    codigos = [c["codigo"] for c in centros]
    duplicados = [c for c, n in collections.Counter(codigos).items() if n > 1]
    if duplicados:
        raise ValueError(f"Códigos duplicados: {duplicados}")
    return centros, excluidos, avisos


def resumen(centros, excluidos, avisos):
    print(f"Centros a importar: {len(centros)}")
    for tipo, n in collections.Counter(c["tipo"] for c in centros).most_common():
        print(f"  {n:5}  {tipo}")
    if excluidos:
        print(f"Públicos excluidos: {sum(excluidos.values())}")
        for denominacion, n in excluidos.most_common():
            print(f"  {n:5}  {denominacion}")
    sin = {campo: sum(1 for c in centros if not c[campo]) for campo in ("telefono", "email", "cp", "geo")}
    print(f"Sin dato: {sin}")
    origen = collections.Counter(c.get("departamentosFuente") for c in centros)
    print(f"Departamentos: {dict(origen)}")
    estandar_por_tipo = collections.Counter(c["tipo"] for c in centros if c.get("departamentosFuente") == "estandar")
    if estandar_por_tipo:
        print(f"  lista estándar por tipo: {dict(estandar_por_tipo.most_common())}")
    for aviso in avisos:
        print(f"AVISO {aviso}")


def a_json(centros):
    def convertir(c):
        c = dict(c)
        if c["geo"]:
            c["geo"] = {"lat": c["geo"][0], "lng": c["geo"][1]}
        return c
    return [convertir(c) for c in centros]


def cliente_firestore(args):
    if args.emulador:
        os.environ["FIRESTORE_EMULATOR_HOST"] = args.emulador
        from google.cloud import firestore  # anonymous credentials against the emulator
        return firestore.Client(project=args.proyecto)

    import firebase_admin
    from firebase_admin import credentials, firestore
    if args.credenciales:
        cred = credentials.Certificate(args.credenciales)
    elif os.environ.get("GOOGLE_APPLICATION_CREDENTIALS"):
        cred = credentials.ApplicationDefault()
    else:
        sys.exit("Faltan credenciales: usa --credenciales RUTA o GOOGLE_APPLICATION_CREDENTIALS.")
    opciones = {"projectId": args.proyecto} if args.proyecto else None
    firebase_admin.initialize_app(cred, opciones)
    return firestore.client()


def resumen_catalogo(documentos):
    for doc_id, d in sorted(documentos.items()):
        print(f"  catalogo/{doc_id}: {len(d['centros'])} centros, ~{tamano_estimado(d['centros']) // 1024} KiB")


def escribir_catalogo(db, documentos, estandar):
    """catalogo/{provincia}-{parte}, config/departamentos and config/catalogo {version, partes}.

    Parts of the imported provincias that are no longer needed are deleted; parts of
    other provincias (when importing only one) are kept. The version only changes on
    import, never with reviews.
    """
    importadas = {d["provincia"] for d in documentos.values()}
    for ref in db.collection("catalogo").list_documents():
        snap = ref.get()
        if snap.exists and snap.get("provincia") in importadas and ref.id not in documentos:
            ref.delete()
    for doc_id, datos in documentos.items():
        db.document(f"catalogo/{doc_id}").set(datos)
    partes = sorted(ref.id for ref in db.collection("catalogo").list_documents())

    db.document("config/departamentos").set({"porTipo": estandar})
    version = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    db.document("config/catalogo").set({"version": version, "partes": partes})
    print(f"config/catalogo.version = {version}; partes = {partes}")


def leer_stats_existentes(db, centros):
    """{codigo: stats.total} of the centros that already exist (keeps their averages)."""
    refs = [db.collection("centros").document(c["codigo"]) for c in centros]
    return {snap.id: (snap.to_dict().get("stats") or {}).get("total") for snap in db.get_all(refs) if snap.exists}


def escribir(db, centros, existentes):
    """Writes centros/{codigo} with merge; only new centros get empty stats."""
    from google.cloud.firestore import GeoPoint

    coleccion = db.collection("centros")
    refs = [coleccion.document(c["codigo"]) for c in centros]

    nuevos = 0
    for inicio in range(0, len(centros), LOTE):
        lote = db.batch()
        for centro, ref in zip(centros[inicio:inicio + LOTE], refs[inicio:inicio + LOTE]):
            datos = dict(centro)
            datos["geo"] = GeoPoint(*centro["geo"]) if centro["geo"] else None
            if ref.id not in existentes:
                datos["stats"] = stats_vacios()
                nuevos += 1
            lote.set(ref, datos, merge=True)
        lote.commit()
    print(f"Escritos {len(centros)} centros ({nuevos} nuevos, {len(centros) - nuevos} actualizados).")


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--csv", default=URL_CSV_2024_25, help="Ruta local o URL https del CSV (por defecto, 2024/25)")
    p.add_argument("--curso", default=CURSO_2024_25, help="Curso del CSV, p. ej. 2024/2025")
    p.add_argument("--plantilla", default=URL_PLANTILLA_2025_26,
                   help="Ruta local o URL https de la plantilla orgánica (por defecto, 2025/26)")
    p.add_argument("--curso-plantilla", default=CURSO_PLANTILLA_2025_26, help="Curso de la plantilla")
    p.add_argument("--provincia", help="Importar solo esta provincia (p. ej. Huelva)")
    destino = p.add_mutually_exclusive_group(required=True)
    destino.add_argument("--dry-run", action="store_true", help="No escribe; genera --salida")
    destino.add_argument("--emulador", metavar="HOST:PUERTO", help="Escribe en el emulador de Firestore")
    destino.add_argument("--produccion", action="store_true", help="Escribe en el Firestore real")
    p.add_argument("--salida", default="centros.json", help="JSON del --dry-run")
    p.add_argument("--proyecto", help="Id del proyecto de Firebase (obligatorio con --emulador)")
    p.add_argument("--credenciales", help="Clave de cuenta de servicio (o GOOGLE_APPLICATION_CREDENTIALS)")
    args = p.parse_args(argv)

    if args.emulador and not args.proyecto:
        p.error("--emulador necesita --proyecto (p. ej. demo-valoralo)")

    centros, excluidos, avisos = transformar(leer_csv(args.csv), args.provincia, args.curso)
    estandar = cargar_estandar(TABLA_ESTANDAR)
    avisos += anadir_departamentos(centros, leer_csv(args.plantilla, ","), args.curso_plantilla, estandar)
    resumen(centros, excluidos, avisos)

    if args.dry_run:
        resumen_catalogo(documentos_catalogo(centros, {}))
        with open(args.salida, "w", encoding="utf-8") as f:
            json.dump(a_json(centros), f, ensure_ascii=False, indent=2)
        print(f"Dry-run: nada escrito en Firestore. JSON en {args.salida}")
        return

    db = cliente_firestore(args)
    stats = leer_stats_existentes(db, centros)
    documentos = documentos_catalogo(centros, stats)
    documento_de = documento_de_cada_centro(documentos)
    for centro in centros:
        centro["catalogoDoc"] = documento_de[centro["codigo"]]
    escribir(db, centros, set(stats))
    resumen_catalogo(documentos)
    escribir_catalogo(db, documentos, estandar)


if __name__ == "__main__":
    main()
