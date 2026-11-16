"""Marca (o desmarca) una cuenta como administradora de Valoralo.

Pone la marca admin = true en la cuenta de Firebase Auth. Con ella se entra en el panel
de verificación (sitio valoralo-admin) y se puede llamar a resolverVerificacion.
La cuenta tiene que haber entrado antes al menos una vez en la app.

  python tools/admin/hacer_admin.py CORREO            # marcar
  python tools/admin/hacer_admin.py CORREO --quitar   # desmarcar
  python tools/admin/hacer_admin.py --listar          # ver quién es administrador

Credenciales: GOOGLE_APPLICATION_CREDENTIALS o --credenciales (nunca dentro del repo).
La persona tiene que cerrar sesión y volver a entrar (o esperar ~1 h) para que su token
incluya la marca.
"""
import argparse
import os
import sys


def marcas_nuevas(actuales, quitar):
    marcas = dict(actuales or {})
    if quitar:
        marcas.pop("admin", None)
    else:
        marcas["admin"] = True
    return marcas


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("correo", nargs="?")
    p.add_argument("--quitar", action="store_true")
    p.add_argument("--listar", action="store_true")
    p.add_argument("--credenciales")
    args = p.parse_args(argv)

    import firebase_admin
    from firebase_admin import auth, credentials
    if args.credenciales:
        firebase_admin.initialize_app(credentials.Certificate(args.credenciales))
    elif os.environ.get("GOOGLE_APPLICATION_CREDENTIALS") or os.environ.get("FIREBASE_AUTH_EMULATOR_HOST"):
        firebase_admin.initialize_app()
    else:
        sys.exit("Faltan credenciales: usa --credenciales RUTA o GOOGLE_APPLICATION_CREDENTIALS.")

    if args.listar:
        for usuario in auth.list_users().iterate_all():
            if (usuario.custom_claims or {}).get("admin") is True:
                print(f"{usuario.email}  ({usuario.uid})")
        return
    if not args.correo:
        p.error("indica el correo de la cuenta")
    try:
        usuario = auth.get_user_by_email(args.correo)
    except auth.UserNotFoundError:
        sys.exit(f"No existe ninguna cuenta con {args.correo}. Tiene que entrar antes en la app.")
    auth.set_custom_user_claims(usuario.uid, marcas_nuevas(usuario.custom_claims, args.quitar))
    print(f"{'Desmarcada' if args.quitar else 'Marcada'} como administradora: {args.correo} ({usuario.uid}).")
    print("Tiene que cerrar sesión y volver a entrar para que la marca se aplique.")


if __name__ == "__main__":
    main()
