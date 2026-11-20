import { describe, expect, test } from "vitest";
import { aliasEsValido, ErrorAlias, validarAlias } from "../../src/alias";

const error = (alias: unknown) => {
  try {
    validarAlias(alias);
    return null;
  } catch (e) {
    if (e instanceof ErrorAlias) return e.message;
    throw e;
  }
};

describe("alias válidos", () => {
  test.each([
    "Familia de 5º B", "Estudiante de 1º Bach", "Exalumna promoción 2024", "Profe de Bio", "Familia del barrio",
    "Lucía_93", "Nuevo en el claustro", "Ana G.", "maría-josé", "Pepe", "Mamá de dos",
  ])("%s", (alias) => {
    expect(error(alias)).toBeNull();
  });

  test("normaliza espacios", () => {
    expect(validarAlias("  Familia   de 5º B  ")).toBe("Familia de 5º B");
  });
});

describe("longitud y caracteres", () => {
  test.each([["ab", "al menos 3"], ["   a  ", "al menos 3"], ["x".repeat(31), "máximo 30"], [42, "al menos 3"], [null, "al menos 3"]])(
    "%s → %s", (alias, mensaje) => {
      expect(error(alias)).toContain(mensaje);
    });
  test.each(["Hola 😀 qué tal", "yo@correo.es", "<b>Hola</b>", "Alias#1", "a/b/c"])("caracteres no permitidos: %s", (alias) => {
    expect(error(alias)).toContain("solo puede llevar");
  });
});

describe("contacto: teléfonos y enlaces", () => {
  test.each(["612345678", "Llámame 612 34 56 78", "Tel 955.123.456", "miweb.com", "www.algo", "visita algo.es", "http_algo"])(
    "%s", (alias) => {
      expect(error(alias)).toContain("teléfonos, correos ni enlaces");
    });
  test("hasta 5 cifras sí (años, cursos)", () => {
    expect(error("Promo 2019 grupo 3")).toBeNull();
  });
});

describe("palabras reservadas (rol, moderación, app)", () => {
  test.each([
    "Docente verificado", "docente de Ciencias", "DOCENTE", "D0cente", "d.o.c.e.n.t.e", "Verificado", "Verificada 2024",
    "Moderador", "Equipo de moderación", "Admin", "Administración", "administrador_1", "Valoralo", "Equipo Valoralo",
    "V4l0r4l0", "Junta de Andalucía", "Consejería", "Soporte técnico", "Cuenta oficial",
  ])("%s", (alias) => {
    expect(error(alias)).toContain("puede confundirse");
  });
});

describe("insultos", () => {
  test.each(["Gilipollas", "El puto amo", "g1l1poll4s", "Cabrón total"])("%s", (alias) => {
    expect(error(alias)).toContain("insultos");
  });
});

test("aliasEsValido", () => {
  expect(aliasEsValido("Familia de 5º B")).toBe(true);
  expect(aliasEsValido("Docente verificado")).toBe(false);
  expect(aliasEsValido(undefined)).toBe(false);
});
