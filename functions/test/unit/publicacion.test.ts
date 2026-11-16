import { describe, expect, test } from "vitest";
import { construirValoracion, ErrorFiltro, PerfilAutor, ValoracionGuardada } from "../../src/publicacion";
import { Formulario } from "../../src/validacion";

const ahora = new Date("2026-11-17T10:00:00Z");
const antes = new Date("2026-10-01T09:00:00Z");
const formulario = (texto = "Muy buen centro."): Formulario => ({
  codigo: "21000693", centro: { nota: 4 }, instalaciones: { nota: 3 }, profesorado: [], texto,
});
const perfil = (cambios: Partial<PerfilAutor> = {}): PerfilAutor =>
  ({ uid: "u1", alias: "Lucía R.", rol: "ESTUDIANTE", verificado: false, ...cambios });
const contexto = { permitidas: ["IES Delgado Hernández"] };

describe("construirValoracion", () => {
  test("nueva y limpia: se publica con el rol del perfil", () => {
    const v = construirValoracion(formulario(), perfil(), null, ahora, contexto);
    expect(v).toMatchObject({
      uid: "u1", alias: "Lucía R.", rolAutor: "ESTUDIANTE", verificado: false,
      fecha: ahora, actualizada: ahora, editada: false, estado: "PUBLICADA", revision: null,
      centro: { nota: 4 }, instalaciones: { nota: 3 }, profesorado: [], texto: "Muy buen centro.",
    });
  });

  test("docente verificado publica como DOCENTE; sin verificar, como OTRO", () => {
    expect(construirValoracion(formulario(), perfil({ rol: "DOCENTE", verificado: true }), null, ahora, contexto))
      .toMatchObject({ rolAutor: "DOCENTE", verificado: true });
    expect(construirValoracion(formulario(), perfil({ rol: "DOCENTE", verificado: false }), null, ahora, contexto))
      .toMatchObject({ rolAutor: "OTRO", verificado: false });
  });

  test("texto bloqueado: no se guarda y devuelve los fragmentos", () => {
    try {
      construirValoracion(formulario("La profe Ana es genial"), perfil(), null, ahora, contexto);
      expect.unreachable();
    } catch (e) {
      expect(e).toBeInstanceOf(ErrorFiltro);
      expect((e as ErrorFiltro).fragmentos.map((f) => f.texto)).toEqual(["Ana"]);
    }
  });

  test("'Otro departamento' también pasa el filtro", () => {
    const f = { ...formulario(), profesorado: [{ departamento: "Departamento Pérez", nota: 3, otro: true as const }] };
    expect(() => construirValoracion(f, perfil(), null, ahora, contexto)).toThrow(ErrorFiltro);
  });

  test("texto dudoso: se guarda EN_REVISION con los fragmentos", () => {
    const v = construirValoracion(formulario("juan explica fatal"), perfil(), null, ahora, contexto);
    expect(v.estado).toBe("EN_REVISION");
    expect(v.revision).toMatchObject({ origen: "FILTRO", fragmentos: [{ texto: "juan", motivo: "NOMBRE_DUDOSO" }] });
  });

  const previa = (cambios: Partial<ValoracionGuardada>): ValoracionGuardada => ({
    uid: "u1", alias: "Lucía R.", rolAutor: "OTRO", verificado: false, fecha: antes, estado: "PUBLICADA",
    revision: null, ...cambios,
  });

  test("al editar se mantienen la fecha y el rolAutor original y se marca editada", () => {
    const v = construirValoracion(formulario(), perfil({ rol: "ESTUDIANTE", alias: "Nuevo" }), previa({}), ahora, contexto);
    expect(v).toMatchObject({ fecha: antes, actualizada: ahora, editada: true, rolAutor: "OTRO", alias: "Nuevo", estado: "PUBLICADA" });
  });

  test("si estaba en revisión por el filtro y al editar pasa, se publica directamente", () => {
    const v = construirValoracion(formulario(), perfil(),
      previa({ estado: "EN_REVISION", revision: { origen: "FILTRO", fragmentos: [] } }), ahora, contexto);
    expect(v).toMatchObject({ estado: "PUBLICADA", revision: null });
  });

  test("si estaba en revisión por denuncias o la ocultó un moderador, editar no la publica", () => {
    const porDenuncias = construirValoracion(formulario(), perfil(),
      previa({ estado: "EN_REVISION", revision: { origen: "DENUNCIAS", fragmentos: [] } }), ahora, contexto);
    expect(porDenuncias).toMatchObject({ estado: "EN_REVISION", revision: { origen: "DENUNCIAS" } });
    const oculta = construirValoracion(formulario(), perfil(), previa({ estado: "OCULTA" }), ahora, contexto);
    expect(oculta).toMatchObject({ estado: "EN_REVISION", revision: { origen: "MODERACION" } });
  });
});
