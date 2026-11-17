"""Moderación de reseñas desde la línea de comandos (respaldo del panel de administración).

La vía principal es el panel (https://valoralo-admin.web.app). Este script aplica las MISMAS
transiciones que la función moderarValoracion (casos compartidos en
functions/test/casos_moderacion.json) y deja la misma constancia: decisión, fecha,
moderador, motivo visible para el autor y nota interna. Cada decisión resuelve las
denuncias pendientes de la reseña; la Cloud Function alEscribirValoracion recalcula las notas.

  python tools/moderacion/moderar.py listar
  python tools/moderacion/moderar.py aprobar CODIGO_CENTRO UID_AUTOR [--nota "…"]
  python tools/moderacion/moderar.py ocultar CODIGO_CENTRO UID_AUTOR --motivo NOMBRE_PERSONA [--nota "…"]
  python tools/moderacion/moderar.py descartar-denuncias CODIGO_CENTRO UID_AUTOR [--nota "…"]

Motivos: NOMBRE_PERSONA, INSULTOS, DATOS_PERSONALES, NO_TRATA_DEL_CENTRO, OTRO.
Credenciales: GOOGLE_APPLICATION_CREDENTIALS o --credenciales. Con --emulador HOST:PUERTO
trabaja contra el emulador.
"""
import argparse
import datetime
import os
import sys

MOTIVOS_OCULTAR = {
    "NOMBRE_PERSONA": "Contiene el nombre de una persona",
    "INSULTOS": "Insultos o lenguaje ofensivo",
    "DATOS_PERSONALES": "Datos personales",
    "NO_TRATA_DEL_CENTRO": "No trata sobre el centro",
    "OTRO": "Otro",
}
DECISION = {"APROBAR": "APROBADA", "OCULTAR": "OCULTADA", "DESCARTAR_DENUNCIAS": "DENUNCIAS_DESCARTADAS"}
DECISION_DENUNCIAS = {"APROBAR": "APROBADA", "OCULTAR": "OCULTADA", "DESCARTAR_DENUNCIAS": "DESCARTADA"}
ESTADOS_PERMITIDOS = {
    "APROBAR": ["EN_REVISION"],
    "OCULTAR": ["EN_REVISION", "PUBLICADA"],
    "DESCARTAR_DENUNCIAS": ["PUBLICADA"],
}
ACCIONES_CLI = {"aprobar": "APROBAR", "ocultar": "OCULTAR", "descartar-denuncias": "DESCARTAR_DENUNCIAS"}
MODERADOR_SCRIPT = "script-moderar"


class ErrorModeracion(Exception):
    pass


def cambios_moderacion(estado_actual, accion, motivo, nota, moderador_uid, ahora):
    """Same as cambiosModeracion in functions/src/moderacion.ts. Returns (valoracion, denuncias)."""
    if accion not in DECISION:
        raise ErrorModeracion("Acción no válida.")
    if estado_actual not in ESTADOS_PERMITIDOS[accion]:
        raise ErrorModeracion(f"No se puede aplicar {accion} a una reseña en estado {estado_actual}.")
    if accion == "OCULTAR" and motivo not in MOTIVOS_OCULTAR:
        raise ErrorModeracion("Elige el motivo por el que se oculta; el autor lo verá.")
    if nota is not None and len(nota) > 500:
        raise ErrorModeracion("La nota interna puede tener como máximo 500 caracteres.")

    motivo_final = motivo if accion == "OCULTAR" else None
    moderacion = {
        "decision": DECISION[accion],
        "motivo": motivo_final,
        "motivoTexto": MOTIVOS_OCULTAR[motivo_final] if motivo_final else None,
        "nota": nota,
        "fecha": ahora,
        "moderadorUid": moderador_uid,
    }
    if accion == "APROBAR":
        valoracion = {"estado": "PUBLICADA", "revision": None, "moderacion": moderacion}
    elif accion == "OCULTAR":
        valoracion = {"estado": "OCULTA", "moderacion": moderacion}
    else:
        valoracion = {"moderacion": moderacion}
    denuncias = {"estado": "RESUELTA", "decision": DECISION_DENUNCIAS[accion], "resuelta": ahora, "moderadorUid": moderador_uid}
    return valoracion, denuncias


def describir(codigo, nombre_centro, uid, v, denuncias):
    revision = v.get("revision") or {}
    fragmentos = ", ".join(f"«{f.get('texto')}» {f.get('motivo')}" for f in revision.get("fragmentos", [])) or "-"
    profesorado = ", ".join(f"{p['departamento']} {p['nota']}" for p in v.get("profesorado", [])) or "-"
    return "\n".join([
        f"• {codigo} {nombre_centro}",
        f"  autor: {v.get('alias')} ({v.get('rolAutor')})  uid: {uid}",
        f"  estado: {v.get('estado')}   motivo de revisión: {revision.get('origen', '-')}   denuncias pendientes: {denuncias}",
        f"  marcado: {fragmentos}",
        f"  notas: centro {v.get('centro', {}).get('nota')} · instalaciones {v.get('instalaciones', {}).get('nota')} · profesorado: {profesorado}",
        f"  texto: {v.get('texto') or '(sin texto)'}",
    ])


def cliente(args):
    if args.emulador:
        os.environ["FIRESTORE_EMULATOR_HOST"] = args.emulador
        from google.cloud import firestore
        return firestore.Client(project=args.proyecto)
    import firebase_admin
    from firebase_admin import credentials, firestore
    if args.credenciales:
        cred = credentials.Certificate(args.credenciales)
    elif os.environ.get("GOOGLE_APPLICATION_CREDENTIALS"):
        cred = credentials.ApplicationDefault()
    else:
        sys.exit("Faltan credenciales: usa --credenciales RUTA o GOOGLE_APPLICATION_CREDENTIALS.")
    firebase_admin.initialize_app(cred)
    return firestore.client()


def denuncias_pendientes(db, codigo, uid):
    return list(db.collection("denuncias").where("centro", "==", codigo)
                .where("valoracionUid", "==", uid).where("estado", "==", "PENDIENTE").stream())


def listar(db):
    pendientes = list(db.collection_group("valoraciones").where("estado", "==", "EN_REVISION").stream())
    con_denuncias = {(d.get("centro"), d.get("valoracionUid")) for d in db.collection("denuncias").where("estado", "==", "PENDIENTE").stream()}
    vistas = set()
    print(f"{len(pendientes)} reseña(s) en revisión:\n")
    for snap in pendientes:
        codigo = snap.reference.parent.parent.id
        vistas.add((codigo, snap.id))
        nombre = (db.document(f"centros/{codigo}").get().to_dict() or {}).get("nombre", "")
        print(describir(codigo, nombre, snap.id, snap.to_dict(), len(denuncias_pendientes(db, codigo, snap.id))) + "\n")
    otras = sorted(con_denuncias - vistas)
    print(f"{len(otras)} reseña(s) publicada(s) con denuncias pendientes:\n")
    for codigo, uid in otras:
        snap = db.document(f"centros/{codigo}/valoraciones/{uid}").get()
        if snap.exists:
            nombre = (db.document(f"centros/{codigo}").get().to_dict() or {}).get("nombre", "")
            print(describir(codigo, nombre, uid, snap.to_dict(), len(denuncias_pendientes(db, codigo, uid))) + "\n")


def decidir(db, accion, codigo, uid, motivo, nota):
    from google.cloud import firestore
    ref = db.document(f"centros/{codigo}/valoraciones/{uid}")
    transaccion = db.transaction()

    @firestore.transactional
    def aplicar(tx):
        snap = ref.get(transaction=tx)
        if not snap.exists:
            raise ErrorModeracion("No existe esa reseña.")
        pendientes = denuncias_pendientes(db, codigo, uid)
        valoracion, denuncias = cambios_moderacion(
            snap.get("estado"), accion, motivo, nota, MODERADOR_SCRIPT, datetime.datetime.now(datetime.timezone.utc))
        tx.update(ref, valoracion)
        for d in pendientes:
            tx.update(d.reference, denuncias)
        return valoracion.get("estado", snap.get("estado")), len(pendientes)

    try:
        estado, n = aplicar(transaccion)
    except ErrorModeracion as e:
        sys.exit(str(e))
    print(f"Hecho: {codigo}/{uid} → {estado} ({n} denuncia(s) resuelta(s)).")


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("accion", choices=["listar", *ACCIONES_CLI])
    p.add_argument("codigo", nargs="?", help="Código del centro")
    p.add_argument("uid", nargs="?", help="UID del autor de la reseña")
    p.add_argument("--motivo", choices=list(MOTIVOS_OCULTAR), help="Obligatorio al ocultar; lo verá el autor")
    p.add_argument("--nota", help="Nota interna (no la ve el autor)")
    p.add_argument("--emulador", metavar="HOST:PUERTO")
    p.add_argument("--proyecto", default="demo-valoralo")
    p.add_argument("--credenciales")
    args = p.parse_args(argv)
    db = cliente(args)
    if args.accion == "listar":
        listar(db)
        return
    if not (args.codigo and args.uid):
        p.error(f"{args.accion} necesita CODIGO y UID")
    decidir(db, ACCIONES_CLI[args.accion], args.codigo, args.uid, args.motivo, args.nota)


if __name__ == "__main__":
    main()
