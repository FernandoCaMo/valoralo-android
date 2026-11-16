"""Moderación de reseñas desde la línea de comandos (hasta el panel de la Fase 6).

Usa la clave de la cuenta de servicio (GOOGLE_APPLICATION_CREDENTIALS o --credenciales),
igual que tools/import. Cada aprobación o rechazo recalcula las notas del centro
(lo hace la Cloud Function alEscribirValoracion) y resuelve sus denuncias pendientes.

  python tools/moderacion/moderar.py listar
  python tools/moderacion/moderar.py aprobar CODIGO_CENTRO UID_AUTOR
  python tools/moderacion/moderar.py rechazar CODIGO_CENTRO UID_AUTOR --nota "Nombra a un profesor"

Con --emulador HOST:PUERTO --proyecto demo-valoralo trabaja contra el emulador.
"""
import argparse
import datetime
import os
import sys

DECISIONES = {"aprobar": "APROBADA", "rechazar": "RECHAZADA"}


def actualizacion_valoracion(accion, ahora, nota=None):
    if accion not in DECISIONES:
        raise ValueError(f"Acción desconocida: {accion}")
    moderacion = {"decision": DECISIONES[accion], "fecha": ahora, "nota": nota}
    if accion == "aprobar":
        return {"estado": "PUBLICADA", "revision": None, "moderacion": moderacion}
    return {"estado": "OCULTA", "moderacion": moderacion}


def actualizacion_denuncias(accion, ahora):
    return {"estado": "RESUELTA", "decision": DECISIONES[accion], "resuelta": ahora}


def describir(codigo, nombre_centro, uid, v, denuncias):
    revision = v.get("revision") or {}
    fragmentos = ", ".join(f"«{f.get('texto')}» {f.get('motivo')}" for f in revision.get("fragmentos", [])) or "-"
    profesorado = ", ".join(f"{p['departamento']} {p['nota']}" for p in v.get("profesorado", [])) or "-"
    return "\n".join([
        f"• {codigo} {nombre_centro}",
        f"  autor: {v.get('alias')} ({v.get('rolAutor')})  uid: {uid}",
        f"  motivo de revisión: {revision.get('origen', '-')}   denuncias pendientes: {denuncias}",
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
    if not pendientes:
        print("No hay reseñas en revisión.")
        return
    print(f"{len(pendientes)} reseña(s) en revisión:\n")
    for snap in pendientes:
        codigo = snap.reference.parent.parent.id
        nombre = (db.document(f"centros/{codigo}").get().to_dict() or {}).get("nombre", "")
        print(describir(codigo, nombre, snap.id, snap.to_dict(), len(denuncias_pendientes(db, codigo, snap.id))))
        print()


def decidir(db, accion, codigo, uid, nota):
    ref = db.document(f"centros/{codigo}/valoraciones/{uid}")
    snap = ref.get()
    if not snap.exists:
        sys.exit("No existe esa reseña.")
    if snap.get("estado") != "EN_REVISION":
        sys.exit(f"Esa reseña no está en revisión (estado: {snap.get('estado')}).")
    ahora = datetime.datetime.now(datetime.timezone.utc)
    lote = db.batch()
    lote.update(ref, actualizacion_valoracion(accion, ahora, nota))
    for denuncia in denuncias_pendientes(db, codigo, uid):
        lote.update(denuncia.reference, actualizacion_denuncias(accion, ahora))
    lote.commit()
    print(f"Reseña {DECISIONES[accion].lower()}: {codigo}/{uid}")


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("accion", choices=["listar", "aprobar", "rechazar"])
    p.add_argument("codigo", nargs="?", help="Código del centro")
    p.add_argument("uid", nargs="?", help="UID del autor de la reseña")
    p.add_argument("--nota", help="Nota interna de la decisión (no se muestra al usuario)")
    p.add_argument("--emulador", metavar="HOST:PUERTO")
    p.add_argument("--proyecto", default="demo-valoralo")
    p.add_argument("--credenciales")
    args = p.parse_args(argv)
    db = cliente(args)
    if args.accion == "listar":
        listar(db)
    else:
        if not (args.codigo and args.uid):
            p.error("aprobar y rechazar necesitan CODIGO y UID")
        decidir(db, args.accion, args.codigo, args.uid, args.nota)


if __name__ == "__main__":
    main()
