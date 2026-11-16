"""Genera las listas de nombres y apellidos del filtro de texto a partir del INE.

Fuente: INE, "Apellidos y nombres más frecuentes"
(https://www.ine.es/daco/daco42/nombyapel/nombyapel2.htm):
  - nombres_por_edad_media.xls  (nombres con frecuencia >= 20, Padrón)
  - apellidos_frecuencia.xls    (apellidos con frecuencia >= 20)

Uso (necesita xlrd, solo para regenerar; las listas generadas sí van al repo):
  python tools/filtro/generar_listas.py RUTA/nombres.xls RUTA/apellidos.xls

Escribe functions/src/filtro/datos/{nombres,apellidos}.json: palabras sueltas,
normalizadas (minúsculas y sin tildes), ordenadas por frecuencia.
"""
import json
import os
import sys
import unicodedata

import xlrd

NOMBRES_POR_SEXO = 1500   # most frequent full names of each sex (split into words)
APELLIDOS = 4000          # most frequent surnames
PALABRAS_VACIAS = {"de", "del", "la", "las", "los", "y", "i", "el", "san", "santa"}

AQUI = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(AQUI, "..", "..", "functions", "src", "filtro", "datos")


def normalizar(texto):
    sin_tildes = unicodedata.normalize("NFD", texto.strip().lower())
    return "".join(c for c in sin_tildes if unicodedata.category(c) != "Mn")


def filas_con_datos(hoja, columna_nombre, columna_frecuencia):
    for i in range(hoja.nrows):
        fila = hoja.row_values(i)
        frecuencia = fila[columna_frecuencia]
        if isinstance(frecuencia, float) and isinstance(fila[columna_nombre], str) and fila[columna_nombre].strip():
            yield fila[columna_nombre], frecuencia


def palabras_unicas(nombres_ordenados, limite):
    vistas, salida = set(), []
    for nombre in nombres_ordenados:
        for palabra in normalizar(nombre).replace("-", " ").split():
            if len(palabra) >= 3 and palabra not in PALABRAS_VACIAS and palabra not in vistas:
                vistas.add(palabra)
                salida.append(palabra)
        if len(salida) >= limite:
            break
    return salida


def main(ruta_nombres, ruta_apellidos):
    libro = xlrd.open_workbook(ruta_nombres)
    nombres = []
    for hoja in libro.sheets():  # Hombres, Mujeres
        filas = sorted(filas_con_datos(hoja, 1, 2), key=lambda f: -f[1])[:NOMBRES_POR_SEXO]
        nombres.extend(n for n, _ in filas)
    lista_nombres = palabras_unicas(nombres, limite=10**6)

    libro = xlrd.open_workbook(ruta_apellidos)
    filas = []
    for hoja in libro.sheets():
        filas.extend(filas_con_datos(hoja, 1, 2))
    filas.sort(key=lambda f: -f[1])
    lista_apellidos = palabras_unicas((a for a, _ in filas), limite=APELLIDOS)

    os.makedirs(SALIDA, exist_ok=True)
    for nombre, datos in (("nombres", lista_nombres), ("apellidos", lista_apellidos)):
        with open(os.path.join(SALIDA, f"{nombre}.json"), "w", encoding="utf-8") as f:
            json.dump(datos, f, ensure_ascii=False, separators=(",", ":"))
        print(f"{nombre}: {len(datos)} palabras -> {nombre}.json  (primeras: {datos[:8]})")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
