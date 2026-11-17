import { describe, expect, test } from "vitest";
import casos from "../casos_moderacion.json";
import { cambiosModeracion, ErrorModeracion, MOTIVOS_OCULTAR, validarModeracion } from "../../src/moderacion";

const ahora = new Date("2026-11-17T10:00:00Z");
const nota = (n: string | null) => (n === "LARGA_501" ? "x".repeat(501) : n);

describe("moderación: mismos casos que el script", () => {
  test("los motivos fijos son los acordados", () => {
    expect(MOTIVOS_OCULTAR).toEqual(casos.motivos);
  });

  for (const c of casos.casos) {
    test(c.nombre, () => {
      const ejecutar = () => cambiosModeracion(c.estado as never, c.accion, c.motivo, nota(c.nota), "mod1", ahora);
      if ("error" in c && c.error) {
        expect(ejecutar).toThrow(ErrorModeracion);
        return;
      }
      const r = ejecutar();
      const esperado = c.valoracion as Record<string, unknown> & { moderacion: Record<string, unknown> };
      expect(r.valoracion).toEqual({ ...esperado, moderacion: { ...esperado.moderacion, fecha: ahora, moderadorUid: "mod1" } });
      expect(r.denuncias).toEqual({ estado: "RESUELTA", decision: c.denuncias, resuelta: ahora, moderadorUid: "mod1" });
    });
  }
});

describe("validarModeracion", () => {
  test("datos de la llamada", () => {
    expect(validarModeracion({ codigo: "21000693", uid: "u1", accion: "OCULTAR", motivo: "OTRO", nota: "  ver  " }))
      .toEqual({ codigo: "21000693", uid: "u1", accion: "OCULTAR", motivo: "OTRO", nota: "ver" });
    expect(validarModeracion({ codigo: "21000693", uid: "u1", accion: "APROBAR" }))
      .toEqual({ codigo: "21000693", uid: "u1", accion: "APROBAR", motivo: null, nota: null });
    expect(() => validarModeracion({ codigo: "123", uid: "u1", accion: "APROBAR" })).toThrow(ErrorModeracion);
    expect(() => validarModeracion({ codigo: "21000693", uid: "../x", accion: "APROBAR" })).toThrow(ErrorModeracion);
    expect(() => validarModeracion(null)).toThrow(ErrorModeracion);
  });
});
