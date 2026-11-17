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
    nLikes: 0, likesVentana: 0, likesHoy: 0,
    ...cambios,
  };
};

describe("configuración", () => {
  test("valores por defecto si no existe", () => {
    expect(leerConfig(undefined)).toEqual(CONFIG_POR_DEFECTO);
    expect(CONFIG_POR_DEFECTO).toMatchObject({ minCaracteres: 120, maxDias: 180, maxResenas: 12, maxFraccionAltas: 1 / 3, minimoParaMostrar: 3 });
    expect(CONFIG_POR_DEFECTO).toMatchObject({ diasLikes: 7, huecosNuevas: 3, diasNueva: 3, maxLikesNueva: 2 });
  });

  test("lee también los números de likes y reseñas nuevas", () => {
    expect(leerConfig({ diasLikes: 14, huecosNuevas: 0, diasNueva: 1, maxLikesNueva: 5 }))
      .toMatchObject({ diasLikes: 14, huecosNuevas: 0, diasNueva: 1, maxLikesNueva: 5 });
    expect(leerConfig({ diasLikes: 0, huecosNuevas: 99 })).toMatchObject({ diasLikes: 7, huecosNuevas: 3 });
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

  test("guarda el identificador de la reseña (uid del autor) para poder dar like, y sus likes", () => {
    const [r] = seleccionar([c({ uid: "autor1", nLikes: 7 })], cfg, ahora);
    expect(r.clave).toMatch(/^[0-9a-f]{24}$/);
    expect(r).toMatchObject({ autorUid: "autor1", nLikes: 7, alias: "Ana", rolAutor: "ESTUDIANTE", verificado: false });
    expect(r.extracto.length).toBeLessThanOrEqual(201);
  });

  describe("likes", () => {
    test("primero las que tienen más likes en la ventana; si empatan, más likes hoy; luego la más reciente", () => {
      const pocas = c({ likesVentana: 1, actualizada: hace(1) });
      const muchas = c({ likesVentana: 9, actualizada: hace(30) });
      const empateAyer = c({ likesVentana: 5, likesHoy: 0, actualizada: hace(2) });
      const empateHoy = c({ likesVentana: 5, likesHoy: 3, actualizada: hace(20) });
      const r = seleccionar([pocas, muchas, empateAyer, empateHoy], { ...cfg, huecosNuevas: 0 }, ahora);
      expect(r.map((x) => x.codigo)).toEqual([muchas, empateHoy, empateAyer, pocas].map((x) => x.codigo));
    });

    test("3 huecos para reseñas nuevas (3 días, 2 likes o menos), en las posiciones 3, 6 y 9", () => {
      const populares = Array.from({ length: 15 }, (_, i) => c({ likesVentana: 50 - i, nota: 3, actualizada: hace(20) }));
      const nuevas = Array.from({ length: 5 }, (_, i) => c({ fecha: hace(i * 0.5), actualizada: hace(i * 0.5), nota: 3, likesVentana: i % 3 }));
      const r = seleccionar([...populares, ...nuevas], cfg, ahora);
      expect(r).toHaveLength(12);
      const esNueva = (x: { codigo: string }) => nuevas.some((n) => n.codigo === x.codigo);
      expect(r.map((x, i) => (esNueva(x) ? i : -1)).filter((i) => i >= 0)).toEqual([2, 5, 8]);
      // The most recent new ones.
      expect(r.filter(esNueva).map((x) => x.codigo)).toEqual(nuevas.slice(0, 3).map((x) => x.codigo));
      // Populars in order around them.
      expect(r.filter((x) => !esNueva(x)).map((x) => x.codigo)).toEqual(populares.slice(0, 9).map((x) => x.codigo));
    });

    test("no es nueva con más de 2 likes o más de 3 días; sin nuevas, los huecos son para populares", () => {
      const vieja = c({ fecha: hace(4), actualizada: hace(4), likesVentana: 0 });
      const conLikes = c({ fecha: hace(1), actualizada: hace(1), likesVentana: 3 });
      const populares = Array.from({ length: 12 }, (_, i) => c({ likesVentana: 40 - i, nota: 3, actualizada: hace(20) }));
      const r = seleccionar([vieja, conLikes, ...populares], cfg, ahora);
      expect(r.map((x) => x.codigo)).toEqual(populares.map((x) => x.codigo));
    });

    test("una oculta no cuenta aunque tenga muchos likes", () => {
      const oculta = c({ estado: "OCULTA", likesVentana: 99 });
      expect(seleccionar([oculta, c()], cfg, ahora).map((x) => x.codigo)).not.toContain(oculta.codigo);
    });
  });
});
