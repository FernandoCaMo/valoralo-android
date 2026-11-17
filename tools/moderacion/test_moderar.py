"""Tests del script de moderación con los MISMOS casos que la función moderarValoracion
(functions/test/casos_moderacion.json), para que los dos dejen la misma constancia.

Ejecutar:  python -m unittest discover -s tools/moderacion
"""
import datetime
import json
import os
import unittest

from moderar import MOTIVOS_OCULTAR, ErrorModeracion, cambios_moderacion, describir

AQUI = os.path.dirname(os.path.abspath(__file__))
with open(os.path.join(AQUI, "..", "..", "functions", "test", "casos_moderacion.json"), encoding="utf-8") as f:
    CASOS = json.load(f)
AHORA = datetime.datetime(2026, 11, 17, 10, 0, tzinfo=datetime.timezone.utc)


class MismosCasosQueLaFuncion(unittest.TestCase):

    def test_motivos(self):
        self.assertEqual(CASOS["motivos"], MOTIVOS_OCULTAR)

    def test_casos(self):
        for c in CASOS["casos"]:
            with self.subTest(c["nombre"]):
                nota = "x" * 501 if c["nota"] == "LARGA_501" else c["nota"]
                if c.get("error"):
                    with self.assertRaises(ErrorModeracion):
                        cambios_moderacion(c["estado"], c["accion"], c["motivo"], nota, "mod1", AHORA)
                    continue
                valoracion, denuncias = cambios_moderacion(c["estado"], c["accion"], c["motivo"], nota, "mod1", AHORA)
                esperado = dict(c["valoracion"])
                esperado["moderacion"] = {**c["valoracion"]["moderacion"], "fecha": AHORA, "moderadorUid": "mod1"}
                self.assertEqual(esperado, valoracion)
                self.assertEqual({"estado": "RESUELTA", "decision": c["denuncias"], "resuelta": AHORA, "moderadorUid": "mod1"}, denuncias)


class DescribirTest(unittest.TestCase):

    def test_resumen_legible(self):
        texto = describir("21000693", "IES Delgado Hernández", "uidLargo123456", {
            "alias": "Lucía R.", "rolAutor": "ESTUDIANTE", "texto": "juan explica fatal",
            "revision": {"origen": "FILTRO", "fragmentos": [{"texto": "juan", "motivo": "NOMBRE_DUDOSO"}]},
            "centro": {"nota": 2}, "instalaciones": {"nota": 3},
        }, denuncias=0)
        for esperado in ("21000693 IES Delgado Hernández", "Lucía R. (ESTUDIANTE)", "FILTRO", "«juan» NOMBRE_DUDOSO", "juan explica fatal"):
            self.assertIn(esperado, texto)


if __name__ == "__main__":
    unittest.main()
