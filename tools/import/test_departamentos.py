"""Tests de la asignación de departamentos (plantilla orgánica o lista estándar).

Ejecutar:  python -m unittest discover -s tools/import
"""
import os
import unittest

from departamentos import (
    SIEMPRE_ESTANDAR,
    asignar,
    cargar_estandar,
    cargar_tabla,
    departamentos_por_centro,
)

AQUI = os.path.dirname(os.path.abspath(__file__))
TABLA = {"590006": "Matemáticas", "590107": "Informática y Comunicaciones",
         "590227": "Informática y Comunicaciones", "597038": "Educación Primaria", "061020": ""}
ESTANDAR = {"CEIP": ["Educación Infantil", "Educación Primaria"],
            "SEP": ["Ámbito de Comunicación", "Ámbito Social"]}


def fila(centro, puesto, pla="1"):
    return {"CUERPO": "X", "PROVINCIA": "HUELVA", "LOCALIDAD": "HUELVA",
            "CENTRO": centro, "PUESTO": puesto, "PLA": pla}


class DepartamentosPorCentroTest(unittest.TestCase):

    def test_agrupa_especialidades_y_variantes_bilingues(self):
        filas = [
            fila("21000001-I.E.S. UNO", "00590006-MATEMATICAS P.E.S.", "4"),
            fila("21000001-I.E.S. UNO", "11590006-MATEMATICAS (INGLES) P.E.S.", "1"),
            fila("21000001-I.E.S. UNO", "00590107-INFORMATICA P.E.S."),
            fila("21000001-I.E.S. UNO", "00590227-SISTEMAS Y APLIC. INFORMÁTICOS"),
            fila("21000002-C.E.I.P. DOS", "00597038-EDUCACIÓN PRIMARIA", "8"),
        ]
        por_centro, desconocidas = departamentos_por_centro(filas, TABLA)
        self.assertEqual({"Matemáticas", "Informática y Comunicaciones"}, por_centro["21000001"])
        self.assertEqual({"Educación Primaria"}, por_centro["21000002"])
        self.assertEqual({}, desconocidas)

    def test_ignora_plazas_a_cero_y_especialidades_sin_departamento(self):
        filas = [
            fila("21000001-I.E.S. UNO", "00590006-MATEMATICAS P.E.S.", "0"),
            fila("21000001-I.E.S. UNO", "00061020-INSPECCIÓN EDUCATIVA", "1"),
        ]
        por_centro, _ = departamentos_por_centro(filas, TABLA)
        self.assertNotIn("21000001", por_centro)

    def test_cuenta_las_especialidades_que_faltan_en_la_tabla(self):
        filas = [fila("21000001-I.E.S. UNO", "00590999-NUEVA ESPECIALIDAD")]
        _, desconocidas = departamentos_por_centro(filas, TABLA)
        self.assertEqual({"00590999-NUEVA ESPECIALIDAD": 1}, desconocidas)


class AsignarTest(unittest.TestCase):

    def test_con_plantilla_usa_la_plantilla_ordenada(self):
        por_centro = {"21000001": {"Matemáticas", "Educación Física", "Ámbito X"}}
        self.assertEqual(
            (["Ámbito X", "Educación Física", "Matemáticas"], "plantilla", "2025/2026"),
            asignar("21000001", "IES", por_centro, ESTANDAR, "2025/2026"),
        )

    def test_sin_plantilla_usa_la_lista_estandar(self):
        self.assertEqual(
            (["Educación Infantil", "Educación Primaria"], "estandar", None),
            asignar("21000009", "CEIP", {}, ESTANDAR, "2025/2026"),
        )

    def test_adultos_siempre_estandar_aunque_tengan_plantilla(self):
        self.assertIn("CEPER", SIEMPRE_ESTANDAR)
        self.assertIn("SEP", SIEMPRE_ESTANDAR)
        por_centro = {"21500001": {"Educación Primaria"}}
        self.assertEqual(
            (["Ámbito de Comunicación", "Ámbito Social"], "estandar", None),
            asignar("21500001", "SEP", por_centro, ESTANDAR, "2025/2026"),
        )

    def test_tipo_sin_lista_estandar(self):
        self.assertEqual(([], "estandar", None), asignar("1", "XYZ", {}, ESTANDAR, "2025/2026"))


class TablasVersionadasTest(unittest.TestCase):
    """Sanity checks over the real CSVs in the repo."""

    def test_tabla_real(self):
        tabla = cargar_tabla(os.path.join(AQUI, "especialidades_departamentos.csv"))
        self.assertGreater(len(tabla), 200)
        self.assertEqual("Informática y Comunicaciones", tabla["590107"])
        self.assertEqual("Matemáticas", tabla["590006"])
        self.assertEqual("", tabla["061020"])

    def test_estandar_real_cubre_todos_los_tipos_importados(self):
        from mapeo import TIPOS
        estandar = cargar_estandar(os.path.join(AQUI, "departamentos_estandar.csv"))
        importados = {t for t in TIPOS.values() if t}
        self.assertEqual(set(), importados - set(estandar))
        self.assertEqual(
            ["Ámbito de Comunicación", "Ámbito Científico-Tecnológico", "Ámbito Social"], estandar["SEP"])


if __name__ == "__main__":
    unittest.main()
