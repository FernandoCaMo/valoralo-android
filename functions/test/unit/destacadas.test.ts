import { describe, expect, test } from "vitest";
import { Candidata, CONFIG_POR_DEFECTO, extracto, leerConfig, notaDeResena, seleccionar } from "../../src/destacadas";

const ahora = new Date("2026-11-17T12:00:00Z");
const hace = (dias: number) => new Date(ahora.getTime() - dias * 86400000);
const texto = (n = 150) => "Buen ambiente en el centro y profesorado implicado. ".repeat(10).slice(0, n);
let id = 0;
const c = (cambios: Partial<Candidata> = {}): Candidata => {
  id += 1;
  return {
    codigo: `2100${String(id).padStart(4, "0")}`, uid: `u${id}`, alias: "Ana", rolAutor: "ESTUDIANTE", verificado: false,
    nota: 4, texto: texto(), fecha: hace(10), actualizada: hace(10), estado: "PUBLICADA", denunciasPendientes: false,
    ...cambios,
  };
};

describe("configuración", () => {
  test("valores por defecto si no existe", () => {
    expect(leerConfig(undefined)).toEqual(CONFIG_POR_DEFECTO);
    expect(CONFIG_POR_DEFECTO).toMatchObject({ minCaracteres: 120, maxDias: 180, maxResenas: 12, maxFraccionAltas: 1 / 3, minimoParaMostrar: 3 });
  });

  test("usa los valores válidos y descarta los que no lo son", () => {
    expect(leerConfig({ minCaracteres: 80, maxResenas: 20, maxDias: "mucho", maxFraccionAltas: 2, minimoParaMostrar: -1 }))
      .toEqual({ ...CONFIG_POR_DEFECTO, minCaracteres: 80, maxResenas: 20 });
  });
});

describe("nota y extracto", () => {
  test("nota de la reseña: media de sus dimensiones (doc 02)", () => {
    expect(notaDeResena({ centro: { nota: 5 }, instalaciones: { nota: 3 }, profesorado: [{ nota: 4 }, { nota: 2 }] })).toBe(11 / 3);
    expect(notaDeResena({ centro: { nota: 4 }, instalaciones: { nota: 2 }, profesorado: [] })).toBe(3);
  });

  test("extracto cortado en una palabra", () => {
    expect(extracto("Corto.", 200)).toBe("Corto.");
    const e = extracto("palabra ".repeat(60).trim(), 200);
    expect(e.length).toBeLessThanOrEqual(201);
    expect(e.endsWith("palabra…")).toBe(true);
  });
});

describe("seleccionar", () => {
  const cfg = CONFIG_POR_DEFECTO;

  test("criterios: publicada, texto largo, reciente y sin denuncias pendientes", () => {
    const buenas = c();
    const r = seleccionar([
      buenas,
      c({ estado: "EN_REVISION" }),
      c({ texto: texto(119) }),
      c({ fecha: hace(400), actualizada: hace(181) }),
      c({ denunciasPendientes: true }),
    ], cfg, ahora);
    expect(r.map((x) => x.codigo)).toEqual([buenas.codigo]);
  });

  test("una editada hace poco cuenta como reciente", () => {
    expect(seleccionar([c({ fecha: hace(400), actualizada: hace(5) })], cfg, ahora)).toHaveLength(1);
  });

  test("como mucho una por centro y una por autor", () => {
    const a = c({ codigo: "21000001", uid: "x", actualizada: hace(1) });
    const r = seleccionar([a, c({ codigo: "21000001", uid: "y" }), c({ codigo: "21000002", uid: "x" }), c({ codigo: "21000003", uid: "z" })], cfg, ahora);
    expect(r.map((x) => x.codigo)).toEqual(["21000001", "21000003"]);
  });

  test("como mucho 12, las más recientes primero", () => {
    const muchas = Array.from({ length: 20 }, (_, i) => c({ actualizada: hace(i + 1), nota: 3 }));
    const r = seleccionar(muchas, cfg, ahora);
    expect(r).toHaveLength(12);
    expect(r[0].codigo).toBe(muchas[0].codigo);
  });

  test("como mucho un tercio con 4,5 o más, y alguna de 3,5 o menos si la hay", () => {
    const altas = Array.from({ length: 10 }, (_, i) => c({ nota: 5, actualizada: hace(i + 1) }));
    const medias = Array.from({ length: 10 }, (_, i) => c({ nota: 4, actualizada: hace(i + 20) }));
    const baja = c({ nota: 2, actualizada: hace(100) });
    const r = seleccionar([...altas, ...medias, baja], cfg, ahora);
    expect(r).toHaveLength(12);
    expect(r.filter((x) => x.nota >= 4.5)).toHaveLength(4);
    expect(r.some((x) => x.nota <= 3.5)).toBe(true);
  });

  test("con pocas candidatas el tercio se respeta sobre las elegidas", () => {
    const r = seleccionar([c({ nota: 5 }), c({ nota: 5 }), c({ nota: 5 }), c({ nota: 4 })], cfg, ahora);
    expect(r.filter((x) => x.nota >= 4.5).length).toBeLessThanOrEqual(Math.max(1, Math.floor(r.length / 3)));
  });

  test("el resultado no expone el uid del autor", () => {
    const [r] = seleccionar([c()], cfg, ahora);
    expect(r).not.toHaveProperty("uid");
    expect(r.clave).toMatch(/^[0-9a-f]{24}$/);
    expect(JSON.stringify(r)).not.toContain(`"u${id}"`);
    expect(r).toMatchObject({ alias: "Ana", rolAutor: "ESTUDIANTE", verificado: false });
    expect(r.extracto.length).toBeLessThanOrEqual(201);
  });
});
