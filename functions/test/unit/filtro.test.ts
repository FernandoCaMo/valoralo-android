import { describe, expect, test } from "vitest";
import { filtrarTexto } from "../../src/filtro/filtro";

const contexto = {
  permitidas: ["IES Delgado Hernández", "Bollullos Par del Condado", "Huelva", "Matemáticas", "Lengua Castellana y Literatura", "Inglés"],
};
const resultado = (texto: string) => filtrarTexto(texto, contexto).resultado;
const motivos = (texto: string) => filtrarTexto(texto, contexto).fragmentos.map((f) => `${f.motivo}:${f.texto}`);

describe("textos que se publican", () => {
  test.each([
    "Buen ambiente y el equipo directivo responde rápido.",
    "El departamento de Matemáticas explica muy bien.",
    "La profesora de Inglés es muy exigente pero justa.",
    "Las aulas tienen mucha luz y el patio es amplio.",
    "El IES Delgado Hernández tiene buenas instalaciones.",
    "En el Delgado Hernández hay buen ambiente.",
    "La computadora de la biblioteca es muy antigua.",
    "Hubo una disputa en el patio y se resolvió bien.",
    "En 2024 cambiaron 15 ventanas y 3 pizarras.",
    "Está muy cerca de Sevilla y llegas en autobús.",
    "Mi hijo llegó retrasado el primer día y nadie le dijo nada.",
    "La rosa del jardín y el pilar de la entrada están cuidados.",
    "",
  ])("OK: %s", (texto) => {
    expect(resultado(texto)).toBe("OK");
  });
});

describe("reseñas realistas sin problemas", () => {
  test.each([
    "El ciclo de DAM está muy bien organizado y en Informática se nota que preparan las prácticas en empresa con tiempo.",
    "Buen ambiente en el claustro y un equipo directivo accesible. Las aulas del edificio antiguo pasan mucho calor en junio.",
    "Laboratorio pequeño pero bien cuidado. La convivencia en ESO es buena y hay protocolos claros.",
    "Como madre, la comunicación con el centro es fluida y responden rápido por iPASEN.",
    "Bachillerato exigente pero bien explicado. Faltan enchufes en las aulas para los portátiles.",
    "El aula de artes es increíble, con mucha luz y material. Muy recomendable para el Bachillerato de Artes.",
    "Instalaciones nuevas. La coordinación entre departamentos se puede mejorar.",
    "Mucho cariño en Infantil y un patio amplio. El comedor funciona bien.",
    "Talleres muy bien equipados y muchas empresas para las prácticas. Salida laboral asegurada.",
    "Relación excelente con las empresas de la bahía. Equipo docente muy implicado.",
    "Los baños están asquerosos y nadie hace nada, es inútil quejarse.",
  ])("OK: %s", (texto) => {
    expect(filtrarTexto(texto).resultado).toBe("OK");
  });
});

describe("textos que se bloquean", () => {
  test("nombre de pila tras un tratamiento", () => {
    expect(resultado("La profe Ana es genial")).toBe("BLOQUEADO");
    expect(motivos("La profe Ana es genial")).toEqual(["NOMBRE_PROPIO:Ana"]);
  });

  test("apellido en mayúscula tras un tratamiento", () => {
    expect(motivos("El profesor de lengua, Pérez, no explica nada")).toContain("NOMBRE_PROPIO:Pérez");
    expect(resultado("Don Manuel siempre llega tarde")).toBe("BLOQUEADO");
  });

  test("nombre y apellido seguidos", () => {
    expect(resultado("Juan García no sabe dar clase")).toBe("BLOQUEADO");
    expect(resultado("Me suspendió la Martínez López sin motivo")).toBe("BLOQUEADO");
  });

  test("nombre de pila poco ambiguo en mayúscula", () => {
    expect(resultado("El año pasado Antonio nos dio clase")).toBe("BLOQUEADO");
  });

  test("insultos, también camuflados", () => {
    for (const texto of [
      "Es un gilipollas",
      "Vaya panda de imbéciles",
      "p.u.t.a mierda de centro",
      "subnormaaaal",
      "es un g1l1poll4s",
      "Menudo malaje el de conserjería",
    ]) {
      expect(resultado(texto), texto).toBe("BLOQUEADO");
    }
    expect(motivos("Es un gilipollas")).toEqual(["INSULTO:gilipollas"]);
  });

  test("datos personales", () => {
    expect(motivos("Escribidme a ana.g@gmail.com")).toEqual(["DATO_PERSONAL:ana.g@gmail.com"]);
    expect(resultado("Llamad al 600 123 456 para quejaros")).toBe("BLOQUEADO");
    expect(resultado("Llamad al +34 959123456")).toBe("BLOQUEADO");
    expect(resultado("Mirad www.ejemplo.com")).toBe("BLOQUEADO");
    expect(resultado("Más info en https://foro.ejemplo.es/hilo")).toBe("BLOQUEADO");
  });
});

describe("textos dudosos (quedan en revisión)", () => {
  test("nombre de pila en minúscula", () => {
    expect(resultado("juan explica fatal")).toBe("DUDOSO");
    expect(motivos("juan explica fatal")).toEqual(["NOMBRE_DUDOSO:juan"]);
  });

  test("nombre ambiguo en mayúscula a mitad de frase", () => {
    expect(resultado("Hablé con Rosa en secretaría y me ayudó")).toBe("DUDOSO");
  });

  test("apellido en mayúscula a mitad de frase", () => {
    expect(resultado("El de Moreno es muy estricto")).toBe("DUDOSO");
  });

  test("insulto ambiguo con un artículo delante", () => {
    expect(resultado("El conserje es un tonto")).toBe("DUDOSO");
    expect(resultado("Eres un payaso")).toBe("DUDOSO");
  });
});

describe("posiciones de los fragmentos", () => {
  test("apuntan al texto original", () => {
    const texto = "La profe Ana es genial";
    const [f] = filtrarTexto(texto, contexto).fragmentos;
    expect(texto.slice(f.inicio, f.fin)).toBe("Ana");
  });

  test("lo bloqueado manda sobre lo dudoso", () => {
    expect(resultado("juan es un gilipollas")).toBe("BLOQUEADO");
  });
});
