import { describe, expect, test } from "vitest";
import { CONFIG_DENUNCIAS_POR_DEFECTO, leerConfigDenuncias, siguienteContador } from "../../src/denuncias";

describe("config/denuncias", () => {
  test("por defecto: 10 por hora y 2 denuncias para salir de Inicio", () => {
    expect(leerConfigDenuncias(undefined)).toEqual({ maxPorHora: 10, paraRetirarDestacada: 2 });
  });
  test("lee valores válidos", () => {
    expect(leerConfigDenuncias({ maxPorHora: 5, paraRetirarDestacada: 3 })).toEqual({ maxPorHora: 5, paraRetirarDestacada: 3 });
  });
  test("ignora valores fuera de rango o de otro tipo", () => {
    expect(leerConfigDenuncias({ maxPorHora: 0, paraRetirarDestacada: "3" })).toEqual(CONFIG_DENUNCIAS_POR_DEFECTO);
    expect(leerConfigDenuncias({ maxPorHora: 2.5, paraRetirarDestacada: 999 })).toEqual(CONFIG_DENUNCIAS_POR_DEFECTO);
  });
});

describe("contador por hora", () => {
  const t0 = new Date("2026-11-20T10:00:00Z");
  const mas = (min: number) => new Date(t0.getTime() + min * 60000);

  test("la primera denuncia empieza la hora en 1", () => {
    expect(siguienteContador(null, t0, 10)).toEqual({ inicio: t0, n: 1 });
  });
  test("dentro de la hora suma 1 y conserva el inicio", () => {
    expect(siguienteContador({ inicio: t0, n: 3 }, mas(20), 10)).toEqual({ inicio: t0, n: 4 });
  });
  test("al llegar al máximo, rechaza (null)", () => {
    expect(siguienteContador({ inicio: t0, n: 10 }, mas(59), 10)).toBeNull();
  });
  test("pasada la hora, vuelve a empezar", () => {
    expect(siguienteContador({ inicio: t0, n: 10 }, mas(60), 10)).toEqual({ inicio: mas(60), n: 1 });
  });
  test("diez seguidas sí, la undécima no", () => {
    let c = null as ReturnType<typeof siguienteContador>;
    for (let i = 0; i < 10; i++) c = siguienteContador(c, mas(i), 10);
    expect(c?.n).toBe(10);
    expect(siguienteContador(c, mas(11), 10)).toBeNull();
  });
});
