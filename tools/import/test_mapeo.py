"""Tests de la transformación fila CSV -> documento centros/{codigo}.

Ejecutar:  python -m unittest discover -s tools/import
"""
import unittest

from mapeo import CAMPOS_ENSENANZAS, fila_a_centro, ignoradas_con_s


def fila(**cambios):
    """Fila mínima de un IES público, con todas las enseñanzas a 'N'."""
    base = {
        "curso": "24",
        "codigo": "21000001",
        "D_DENOMINA": "Instituto de Educación Secundaria",
        "D_ESPECIFICA": "La Rábida",
        "D_TIPO": "Público",
        "D_DOMICILIO": "C/ Ejemplo, 1",
        "D_LOCALIDAD": "Huelva",
        "cod_municipio": "21041",
        "D_MUNICIPIO": "Huelva",
        "D_PROVINCIA": "Huelva",
        "C_POSTAL": "21003",
        "N_TELEFONO": "959 00 00 00",
        "Correo_e": "21000001.edu@juntadeandalucia.es",
        "N_LATITUD": "37,2614",
        "N_LONGITUD": "-6,9447",
        "N_COUTMX": "1",
        "N_COUTMY": "1",
        "Regimen_general": "S",
        "Regimen_adultos": "N",
        "Regimen_especial": "N",
    }
    base.update({c: "N" for c in CAMPOS_ENSENANZAS})
    base.update(cambios)
    return base


class FilaACentroTest(unittest.TestCase):

    def test_ies_publico_completo(self):
        centro = fila_a_centro(fila(pub_eso="S", pub_bach_ord="S", pub_fpgs_ord="S"), curso="2024/2025")
        self.assertEqual("21000001", centro["codigo"])
        self.assertEqual("IES La Rábida", centro["nombre"])
        self.assertEqual("IES", centro["tipo"])
        self.assertEqual("PUBLICA", centro["titularidad"])
        self.assertEqual("C/ Ejemplo, 1", centro["direccion"])
        self.assertEqual("21003", centro["cp"])
        self.assertEqual("Huelva", centro["localidad"])
        self.assertEqual("Huelva", centro["municipio"])
        self.assertEqual("21041", centro["codMunicipio"])
        self.assertEqual("Huelva", centro["provincia"])
        self.assertEqual("Andalucía", centro["comunidad"])
        self.assertEqual("959000000", centro["telefono"])
        self.assertEqual("21000001.edu@juntadeandalucia.es", centro["email"])
        self.assertEqual((37.2614, -6.9447), centro["geo"])
        self.assertEqual([], centro["departamentos"])
        self.assertEqual(
            {"dataset": "directorio-de-centros-docentes-de-andalucia", "curso": "2024/2025"},
            centro["fuente"],
        )
        self.assertEqual(
            [
                {"nivel": "ESO", "nombre": "Educación Secundaria Obligatoria"},
                {"nivel": "Bachillerato", "nombre": "Bachillerato"},
                {"nivel": "FP Grado Superior", "nombre": "FP Grado Superior"},
            ],
            centro["ensenanzas"],
        )
        self.assertNotIn("stats", centro)  # stats are only written by the backend
        self.assertNotIn("web", centro)

    def test_privados_fuera(self):
        self.assertIsNone(fila_a_centro(fila(D_TIPO="Privado"), curso="2024/2025"))

    def test_tipos_excluidos(self):
        for denominacion in [
            "Escuela Infantil",
            "Escuela Municipal de Música",
            "Escuela Municipal de Música y Danza",
            "Escuela Municipal de Danza",
            "Centro de Convenio",
            "Instituto Andaluz del Deporte",
        ]:
            with self.subTest(denominacion):
                self.assertIsNone(fila_a_centro(fila(D_DENOMINA=denominacion), curso="2024/2025"))

    def test_tipo_desconocido_falla(self):
        with self.assertRaises(ValueError):
            fila_a_centro(fila(D_DENOMINA="Tipo inventado"), curso="2024/2025")

    def test_abreviaturas(self):
        casos = {
            "Colegio de Educación Infantil y Primaria": "CEIP",
            "Colegio de Educación Primaria": "CEPR",
            "Colegio Público Rural": "CPR",
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
        }
        for denominacion, tipo in casos.items():
            with self.subTest(denominacion):
                self.assertEqual(tipo, fila_a_centro(fila(D_DENOMINA=denominacion), curso="2024/2025")["tipo"])

    def test_nombre_no_duplica_la_abreviatura(self):
        centro = fila_a_centro(fila(D_ESPECIFICA="IES La Rábida"), curso="2024/2025")
        self.assertEqual("IES La Rábida", centro["nombre"])

    def test_campos_vacios_quedan_a_none(self):
        centro = fila_a_centro(fila(N_TELEFONO=" ", Correo_e="", C_POSTAL=""), curso="2024/2025")
        self.assertIsNone(centro["telefono"])
        self.assertIsNone(centro["email"])
        self.assertEqual("", centro["cp"])

    def test_coordenadas_fuera_de_andalucia_o_invalidas(self):
        self.assertIsNone(fila_a_centro(fila(N_LATITUD="0", N_LONGITUD="0"), curso="2024/2025")["geo"])
        self.assertIsNone(fila_a_centro(fila(N_LATITUD="", N_LONGITUD=""), curso="2024/2025")["geo"])
        self.assertIsNone(fila_a_centro(fila(N_LATITUD="abc", N_LONGITUD="-6"), curso="2024/2025")["geo"])

    def test_ensenanzas_sin_duplicados_y_en_orden(self):
        centro = fila_a_centro(
            fila(pub_adh_inf1="S", pub_noadh_inf1="S", pub_inf2="S", pub_pri="S", pub_ee="S"),
            curso="2024/2025",
        )
        self.assertEqual(
            [
                {"nivel": "Infantil", "nombre": "Educación Infantil (primer ciclo)"},
                {"nivel": "Infantil", "nombre": "Educación Infantil (segundo ciclo)"},
                {"nivel": "Primaria", "nombre": "Educación Primaria"},
                {"nivel": "Educación Especial", "nombre": "Educación Especial"},
            ],
            centro["ensenanzas"],
        )

    def test_modalidades(self):
        centro = fila_a_centro(fila(pub_bach_semi_dist="S", pub_fpgm_adul="S"), curso="2024/2025")
        self.assertEqual(
            [
                {"nivel": "Bachillerato", "nombre": "Bachillerato semipresencial / a distancia"},
                {"nivel": "FP Grado Medio", "nombre": "FP Grado Medio para personas adultas"},
            ],
            centro["ensenanzas"],
        )

    def test_avisa_de_columnas_pub_desconocidas_con_s(self):
        self.assertEqual(["pub_nueva"], ignoradas_con_s(fila(pub_nueva="S", priv_algo="S")))
        self.assertEqual([], ignoradas_con_s(fila()))


if __name__ == "__main__":
    unittest.main()
