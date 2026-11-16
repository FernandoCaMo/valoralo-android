"""Tests de la lógica del script de moderación.

Ejecutar:  python -m unittest discover -s tools/moderacion
"""
import datetime
import unittest

from moderar import actualizacion_denuncias, actualizacion_valoracion, describir

AHORA = datetime.datetime(2026, 11, 17, 10, 0, tzinfo=datetime.timezone.utc)


class ActualizacionTest(unittest.TestCase):

    def test_aprobar_publica_y_quita_la_revision(self):
        self.assertEqual(
            {"estado": "PUBLICADA", "revision": None, "moderacion": {"decision": "APROBADA", "fecha": AHORA, "nota": None}},
            actualizacion_valoracion("aprobar", AHORA),
        )

    def test_rechazar_oculta(self):
        cambios = actualizacion_valoracion("rechazar", AHORA, nota="Nombra a un profesor")
        self.assertEqual("OCULTA", cambios["estado"])
        self.assertEqual({"decision": "RECHAZADA", "fecha": AHORA, "nota": "Nombra a un profesor"}, cambios["moderacion"])
        self.assertNotIn("revision", cambios)  # keeps why it was under review

    def test_decision_desconocida(self):
        with self.assertRaises(ValueError):
            actualizacion_valoracion("borrar", AHORA)

    def test_las_denuncias_quedan_resueltas(self):
        self.assertEqual({"estado": "RESUELTA", "decision": "APROBADA", "resuelta": AHORA},
                         actualizacion_denuncias("aprobar", AHORA))
        self.assertEqual("RECHAZADA", actualizacion_denuncias("rechazar", AHORA)["decision"])


class DescribirTest(unittest.TestCase):

    def test_resumen_legible(self):
        texto = describir("21000693", "IES Delgado Hernández", "uidLargo123456", {
            "alias": "Lucía R.", "rolAutor": "ESTUDIANTE", "texto": "juan explica fatal",
            "revision": {"origen": "FILTRO", "fragmentos": [{"texto": "juan", "motivo": "NOMBRE_DUDOSO"}]},
            "centro": {"nota": 2}, "instalaciones": {"nota": 3},
        }, denuncias=0)
        self.assertIn("21000693 IES Delgado Hernández", texto)
        self.assertIn("Lucía R. (ESTUDIANTE)", texto)
        self.assertIn("FILTRO", texto)
        self.assertIn("«juan» NOMBRE_DUDOSO", texto)
        self.assertIn("juan explica fatal", texto)


if __name__ == "__main__":
    unittest.main()
