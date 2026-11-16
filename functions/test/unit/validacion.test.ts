import { describe, expect, test } from "vitest";
import { ErrorValidacion, LIMITE_TEXTO, validarFormulario } from "../../src/validacion";

const departamentosDelCentro = ["Matemáticas", "Inglés"];
const base = {
  codigo: "21000693",
  centro: { nota: 4 },
  instalaciones: { nota: 3 },
  profesorado: [] as unknown[],
  texto: "",
};
const validar = (cambios: Record<string, unknown>) => validarFormulario({ ...base, ...cambios }, departamentosDelCentro);

describe("validarFormulario", () => {
  test("formulario mínimo: solo las dos notas obligatorias", () => {
    expect(validar({})).toEqual({
      codigo: "21000693",
      centro: { nota: 4 },
      instalaciones: { nota: 3 },
      profesorado: [],
      texto: "",
    });
  });

  test("subnotas opcionales de 1 a 5; las demás claves se ignoran", () => {
    const f = validar({
      centro: { nota: 4, gestion: 5, convivencia: 3, inventada: 9 },
      instalaciones: { nota: 3, limpieza: 2 },
    });
    expect(f.centro).toEqual({ nota: 4, gestion: 5, convivencia: 3 });
    expect(f.instalaciones).toEqual({ nota: 3, limpieza: 2 });
  });

  test("notas fuera de rango o no enteras", () => {
    for (const nota of [0, 6, 3.5, "4", null, undefined]) {
      expect(() => validar({ centro: { nota } }), String(nota)).toThrow(ErrorValidacion);
    }
    expect(() => validar({ instalaciones: { nota: 3, aulas: 7 } })).toThrow(ErrorValidacion);
  });

  test("departamentos del centro y 'Otro departamento'", () => {
    const f = validar({
      profesorado: [
        { departamento: "Matemáticas", nota: 5 },
        { departamento: "  Robótica   educativa ", nota: 4, otro: true },
      ],
    });
    expect(f.profesorado).toEqual([
      { departamento: "Matemáticas", nota: 5 },
      { departamento: "Robótica educativa", nota: 4, otro: true },
    ]);
  });

  test("un departamento que no es del centro tiene que ir como 'otro'", () => {
    expect(() => validar({ profesorado: [{ departamento: "Química", nota: 4 }] })).toThrow(/no es de este centro/);
  });

  test("'Otro departamento' de 3 a 40 letras y sin repetir", () => {
    expect(() => validar({ profesorado: [{ departamento: "AB", nota: 4, otro: true }] })).toThrow(ErrorValidacion);
    expect(() => validar({ profesorado: [{ departamento: "x".repeat(41), nota: 4, otro: true }] })).toThrow(ErrorValidacion);
    expect(() => validar({ profesorado: [{ departamento: "Dept. 3 <b>", nota: 4, otro: true }] })).toThrow(ErrorValidacion);
    expect(() => validar({
      profesorado: [{ departamento: "Matemáticas", nota: 4 }, { departamento: "matemáticas", nota: 2, otro: true }],
    })).toThrow(/repetido/);
  });

  test("texto opcional, recortado y como máximo 1.000 caracteres", () => {
    expect(LIMITE_TEXTO).toBe(1000);
    expect(validar({ texto: "  Muy bien.  " }).texto).toBe("Muy bien.");
    expect(validar({ texto: undefined }).texto).toBe("");
    expect(validar({ texto: "x".repeat(1000) }).texto).toHaveLength(1000);
    expect(() => validar({ texto: "x".repeat(1001) })).toThrow(/1.000/);
  });

  test("código de centro de 8 cifras", () => {
    expect(() => validar({ codigo: "123" })).toThrow(ErrorValidacion);
    expect(() => validar({ codigo: "../x" })).toThrow(ErrorValidacion);
  });
});
