import { describe, expect, test } from "vitest";
import { calcularStats, ValoracionParaStats } from "../../src/calculo";

const v = (
  rolAutor: "DOCENTE" | "ESTUDIANTE" | "OTRO",
  centro: number,
  instalaciones: number,
  deptos: Record<string, number> = {},
  estado: "PUBLICADA" | "EN_REVISION" | "OCULTA" = "PUBLICADA",
): ValoracionParaStats => ({
  rolAutor,
  estado,
  centro: { nota: centro },
  instalaciones: { nota: instalaciones },
  profesorado: Object.entries(deptos).map(([departamento, nota]) => ({ departamento, nota })),
});

const vacio = { n: 0, global: null, centro: null, instalaciones: null, profesorado: null, porDepartamento: {} };

describe("calcularStats (doc 02, Cálculo de las notas)", () => {
  test("sin valoraciones, todo vacío", () => {
    expect(calcularStats([])).toEqual({ total: vacio, docente: vacio, estudiante: vacio, otro: vacio });
  });

  test("agrega por rol ignorando las no publicadas", () => {
    const stats = calcularStats([
      v("DOCENTE", 5, 3, { Matemáticas: 5, Lengua: 3 }), // profesorado 4 → global (5+3+4)/3 = 4
      v("ESTUDIANTE", 3, 2), // sin departamentos → global (3+2)/2 = 2.5
      v("OTRO", 1, 1, { Matemáticas: 1 }, "EN_REVISION"),
      v("OTRO", 1, 1, {}, "OCULTA"),
    ]);
    expect(stats.docente).toEqual({
      n: 1, global: 4, centro: 5, instalaciones: 3, profesorado: 4,
      porDepartamento: { Matemáticas: { n: 1, media: 5 }, Lengua: { n: 1, media: 3 } },
    });
    expect(stats.estudiante).toEqual({
      n: 1, global: 2.5, centro: 3, instalaciones: 2, profesorado: null, porDepartamento: {},
    });
    expect(stats.otro).toEqual(vacio);
    // profesorado only averages the reviews that have it
    expect(stats.total).toEqual({
      n: 2, global: 3.25, centro: 4, instalaciones: 2.5, profesorado: 4,
      porDepartamento: { Matemáticas: { n: 1, media: 5 }, Lengua: { n: 1, media: 3 } },
    });
  });

  test("el desglose por departamento es distinto en cada rol y no se redondea", () => {
    const stats = calcularStats([
      v("DOCENTE", 4, 4, { Inglés: 5 }),
      v("ESTUDIANTE", 4, 4, { Inglés: 2 }),
      v("ESTUDIANTE", 4, 4, { Inglés: 3, Dibujo: 4 }),
    ]);
    expect(stats.docente.porDepartamento).toEqual({ Inglés: { n: 1, media: 5 } });
    expect(stats.estudiante.porDepartamento).toEqual({ Inglés: { n: 2, media: 2.5 }, Dibujo: { n: 1, media: 4 } });
    expect(stats.total.porDepartamento["Inglés"]).toEqual({ n: 3, media: 10 / 3 });
  });
});
