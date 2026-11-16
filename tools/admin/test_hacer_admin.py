"""Tests del script de administradores.  python -m unittest discover -s tools/admin"""
import unittest

from hacer_admin import marcas_nuevas


class MarcasTest(unittest.TestCase):

    def test_anade_admin_sin_tocar_las_demas_marcas(self):
        self.assertEqual({"rol": "DOCENTE", "verificado": True, "admin": True},
                         marcas_nuevas({"rol": "DOCENTE", "verificado": True}, quitar=False))

    def test_sin_marcas_previas(self):
        self.assertEqual({"admin": True}, marcas_nuevas(None, quitar=False))

    def test_quitar_admin(self):
        self.assertEqual({"rol": "DOCENTE"}, marcas_nuevas({"rol": "DOCENTE", "admin": True}, quitar=True))
        self.assertEqual({}, marcas_nuevas({}, quitar=True))


if __name__ == "__main__":
    unittest.main()
