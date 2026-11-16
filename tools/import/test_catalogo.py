"""Tests del catálogo resumido por provincia (catalogo/{id}).

Ejecutar:  python -m unittest discover -s tools/import
"""
import json
import unittest

from catalogo import (LIMITE_BYTES, documento_de_cada_centro, documentos_catalogo, id_documento,
                      resumen_centro, tamano_estimado)


def centro(codigo, provincia="Huelva", nombre=None):
    return {
        "codigo": codigo, "nombre": nombre or f"IES {codigo}", "tipo": "IES",
        "localidad": "Corrales", "municipio": "Aljaraque", "provincia": provincia,
        "direccion": "C/ Larga, 1", "telefono": "959000000", "email": "x@y.es",
        "ensenanzas": [{"nivel": "ESO", "nombre": "ESO"}, {"nivel": "Bachillerato", "nombre": "B1"},
                       {"nivel": "Bachillerato", "nombre": "B2"}],
        "departamentos": ["Matemáticas"],
    }


class ResumenTest(unittest.TestCase):

    def test_solo_lo_que_necesitan_inicio_y_los_filtros(self):
        stats_total = {"n": 3, "global": 4.25, "centro": 4.0}
        self.assertEqual(
            {"codigo": "21000001", "nombre": "IES 21000001", "tipo": "IES", "localidad": "Corrales",
             "municipio": "Aljaraque", "niveles": ["ESO", "Bachillerato"], "nota": 4.25, "resenas": 3},
            resumen_centro(centro("21000001"), stats_total),
        )

    def test_sin_stats(self):
        r = resumen_centro(centro("21000001"), None)
        self.assertIsNone(r["nota"])
        self.assertEqual(0, r["resenas"])


class IdTest(unittest.TestCase):

    def test_ids_sin_tildes_ni_mayusculas(self):
        self.assertEqual("almeria-1", id_documento("Almería", 1))
        self.assertEqual("cadiz-2", id_documento("Cádiz", 2))
        self.assertEqual("malaga-1", id_documento("Málaga", 1))


class DocumentosTest(unittest.TestCase):

    def test_un_documento_por_provincia_si_cabe(self):
        centros = [centro("1"), centro("2"), centro("3", provincia="Sevilla")]
        docs = documentos_catalogo(centros, {})
        self.assertEqual(["huelva-1", "sevilla-1"], sorted(docs))
        self.assertEqual({"provincia": "Huelva", "parte": 1, "partes": 1}, {k: docs["huelva-1"][k] for k in ("provincia", "parte", "partes")})
        self.assertEqual(["1", "2"], [c["codigo"] for c in docs["huelva-1"]["centros"]])

    def test_divide_en_partes_si_supera_el_limite(self):
        centros = [centro(str(i), nombre="X" * 200) for i in range(30)]
        limite = tamano_estimado([resumen_centro(centros[0], None)] * 12)
        docs = documentos_catalogo(centros, {}, limite_bytes=limite)
        self.assertEqual(["huelva-1", "huelva-2", "huelva-3"], sorted(docs))
        self.assertTrue(all(d["partes"] == 3 for d in docs.values()))
        codigos = [c["codigo"] for k in sorted(docs) for c in docs[k]["centros"]]
        self.assertEqual([str(i) for i in range(30)], codigos)  # nothing lost or duplicated
        for d in docs.values():
            self.assertLessEqual(tamano_estimado(d["centros"]), limite)

    def test_documento_de_cada_centro(self):
        centros = [centro(str(i), nombre="X" * 200) for i in range(30)] + [centro("S1", provincia="Sevilla")]
        limite = tamano_estimado([resumen_centro(centros[0], None)] * 12)
        docs = documentos_catalogo(centros, {}, limite_bytes=limite)
        mapa = documento_de_cada_centro(docs)
        self.assertEqual("huelva-1", mapa["0"])
        self.assertEqual("huelva-3", mapa["29"])
        self.assertEqual("sevilla-1", mapa["S1"])
        self.assertEqual(31, len(mapa))

    def test_usa_las_notas_existentes(self):
        docs = documentos_catalogo([centro("1")], {"1": {"n": 2, "global": 3.5}})
        self.assertEqual((3.5, 2), (docs["huelva-1"]["centros"][0]["nota"], docs["huelva-1"]["centros"][0]["resenas"]))

    def test_el_limite_real_deja_margen_bajo_1_MiB(self):
        self.assertLess(LIMITE_BYTES, 1024 * 1024)

    def test_tamano_estimado_es_conservador(self):
        datos = [resumen_centro(centro(str(i)), None) for i in range(10)]
        self.assertGreaterEqual(tamano_estimado(datos), len(json.dumps(datos, ensure_ascii=False).encode("utf-8")))


if __name__ == "__main__":
    unittest.main()
