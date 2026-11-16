import { describe, expect, test } from "vitest";
import {
  CADUCIDAD_DIAS, ErrorVerificacion, hashCsv, MAX_SOLICITUDES, normalizarCsv, solicitudesRecientes,
  validarNombre, validarPdf, validarResolucion,
} from "../../src/verificacion/logica";

const dia = 24 * 60 * 60 * 1000;
const ahora = new Date("2026-11-17T12:00:00Z");

describe("CSV", () => {
  test("se normaliza (mayúsculas, sin espacios ni guiones)", () => {
    expect(normalizarCsv(" vh5dp-nPy5 axnh.x4h6 ")).toBe("VH5DPNPY5AXNHX4H6");
  });

  test("formato: de 10 a 64 letras y números", () => {
    expect(() => normalizarCsv("ABC123")).toThrow(ErrorVerificacion);
    expect(() => normalizarCsv("X".repeat(65))).toThrow(ErrorVerificacion);
    expect(() => normalizarCsv("VH5DP<script>12345")).toThrow(ErrorVerificacion);
  });

  test("el hash es estable, no contiene el CSV y no depende del formato", () => {
    const h = hashCsv(normalizarCsv("VH5DPNPY5AXNHX4H6"));
    expect(h).toMatch(/^[0-9a-f]{64}$/);
    expect(h).toBe(hashCsv(normalizarCsv("vh5dp npy5 axnh x4h6")));
    expect(h).not.toContain("VH5DP");
  });
});

describe("nombre declarado", () => {
  test("nombre y al menos un apellido", () => {
    expect(validarNombre("  Lucía   Ruiz Pérez ")).toBe("Lucía Ruiz Pérez");
    expect(validarNombre("María José O'Neill-García")).toBe("María José O'Neill-García");
    expect(() => validarNombre("Lucía")).toThrow(ErrorVerificacion);
    expect(() => validarNombre("L R")).toThrow(ErrorVerificacion);
    expect(() => validarNombre("Lucía 123")).toThrow(ErrorVerificacion);
    expect(() => validarNombre("x".repeat(101) + " y")).toThrow(ErrorVerificacion);
  });
});

describe("PDF", () => {
  test("opcional; si viene, en la carpeta del usuario", () => {
    expect(validarPdf("u1", undefined)).toBeNull();
    expect(validarPdf("u1", "verificaciones/u1/hoja-servicios_2026.pdf")).toBe("verificaciones/u1/hoja-servicios_2026.pdf");
    expect(() => validarPdf("u1", "verificaciones/u2/hoja.pdf")).toThrow(ErrorVerificacion);
    expect(() => validarPdf("u1", "verificaciones/u1/../u2/hoja.pdf")).toThrow(ErrorVerificacion);
    expect(() => validarPdf("u1", "verificaciones/u1/hoja.exe")).toThrow(ErrorVerificacion);
  });
});

describe("límite de solicitudes", () => {
  test(`como máximo ${MAX_SOLICITUDES} en 30 días`, () => {
    const hace = (dias: number) => new Date(ahora.getTime() - dias * dia);
    expect(solicitudesRecientes([hace(1), hace(10), hace(29)], ahora)).toBe(3);
    expect(solicitudesRecientes([hace(1), hace(31), hace(60)], ahora)).toBe(1);
    expect(solicitudesRecientes([], ahora)).toBe(0);
  });

  test("las pendientes caducan a los 30 días", () => {
    expect(CADUCIDAD_DIAS).toBe(30);
  });
});

describe("resolución", () => {
  test("aprobar, con centro actual opcional", () => {
    expect(validarResolucion({ uid: "u1", decision: "APROBAR" })).toEqual({ uid: "u1", decision: "APROBAR", motivo: null, centroActualCodigo: null });
    expect(validarResolucion({ uid: "u1", decision: "APROBAR", centroActualCodigo: "21000693" }).centroActualCodigo).toBe("21000693");
    expect(() => validarResolucion({ uid: "u1", decision: "APROBAR", centroActualCodigo: "123" })).toThrow(ErrorVerificacion);
  });

  test("rechazar exige un motivo", () => {
    expect(validarResolucion({ uid: "u1", decision: "RECHAZAR", motivo: "  El nombre no coincide.  " }).motivo).toBe("El nombre no coincide.");
    expect(() => validarResolucion({ uid: "u1", decision: "RECHAZAR" })).toThrow(/motivo/);
    expect(() => validarResolucion({ uid: "u1", decision: "RECHAZAR", motivo: "x".repeat(301) })).toThrow(ErrorVerificacion);
  });

  test("datos no válidos", () => {
    expect(() => validarResolucion({ uid: "u1", decision: "QUIZAS" })).toThrow(ErrorVerificacion);
    expect(() => validarResolucion({ uid: "../x", decision: "APROBAR" })).toThrow(ErrorVerificacion);
    expect(() => validarResolucion(null)).toThrow(ErrorVerificacion);
  });
});
