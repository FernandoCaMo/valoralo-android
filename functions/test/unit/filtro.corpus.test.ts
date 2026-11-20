/**
 * Corpus of realistic reviews to catch false positives of the name filter (QA 1.0.5, finding 5:
 * "Segunda evaluación…" was blocked as a person's name). Add a sentence here whenever a real review
 * is wrongly blocked or wrongly let through.
 *
 *  - PUBLICAN: normal reviews, with common words that are also first names or surnames
 *    (Rosa, Bienvenido, Castillo, Cándido, Prudencio, Segunda…), lists and line breaks. Must be OK
 *    (never BLOQUEADO; a few marked DUDOSO are allowed only where the sentence is genuinely ambiguous).
 *  - BLOQUEAN: reviews that name a person. Must be BLOQUEADO.
 */
import { describe, expect, test } from "vitest";
import { filtrarTexto } from "../../src/filtro/filtro";

const contexto = {
  permitidas: ["IES Monte Claro", "Sevilla", "Matemáticas", "Lengua Castellana y Literatura", "Inglés", "Educación Física",
    "Biología y Geología", "Orientación", "Música", "Tecnología", "Geografía e Historia"],
};
const filtrar = (t: string) => filtrarTexto(t, contexto);
const detalle = (t: string) => filtrar(t).fragmentos.map((f) => `${f.motivo}:${f.texto}`).join(", ");

const PUBLICAN = [
  // Common words that are also names, at the start of a sentence or a line.
  "Segunda evaluación muy floja en general, la primera fue mejor.",
  "Primera parte del curso bien.\nSegunda parte peor, con muchos exámenes juntos.",
  "Bienvenido sea el cambio de horario: ahora salimos antes.",
  "Bienvenida la nueva biblioteca, se nota mucho.",
  "Castillo de naipes: así se organizan las guardias, todo se cae si falta alguien.",
  "Cándido no es el adjetivo para la dirección: son muy exigentes con las normas.",
  "Prudencio es un nombre antiguo, pero en el centro se habla de prudencia con las redes sociales.",
  "Rosa es el color del nuevo uniforme y a casi nadie le gusta.",
  "Pilar fundamental del centro: el departamento de Orientación.",
  "Luz natural en todas las aulas, eso se agradece.",
  "Paz y tranquilidad en el patio de Infantil.",
  "Victoria del equipo de baloncesto en el campeonato provincial.",
  "Esperanza de que arreglen el gimnasio el año que viene.",
  "Dolores de cabeza con el ruido de las obras de al lado.",
  "Salud mental: hay un programa de acompañamiento que funciona.",
  "Sierra, playa y museo: las excursiones están muy bien pensadas.",
  "Vega del río al lado del centro, ideal para las salidas de Biología.",
  "León de peluche en la clase de Infantil, a los peques les encanta.",
  // Lists with line breaks.
  "Lo mejor:\nBuen ambiente\nMucha participación de las familias\nComedor bueno",
  "Ventajas\nBiblioteca abierta por la tarde\nAulas con proyector\n\nInconvenientes\nPatio pequeño",
  "Pros:\n- Profesorado implicado\n- Actividades extraescolares\nContras:\n- Calor en junio",
  // Ordinary reviews.
  "El centro está bien organizado y el claustro se implica con las familias.",
  "Buen instituto para Bachillerato, con mucha exigencia en Matemáticas.",
  "Las tutorías responden rápido por iPasen y la acogida en 1º de ESO fue muy cuidada.",
  "El departamento de Orientación acompaña muy bien en la elección de estudios.",
  "Instalaciones antiguas pero limpias. El comedor ha mejorado este año.",
  "Mucho cambio de profesorado interino y se nota en la continuidad de las asignaturas.",
  "En Semana Santa y en Navidad hay actividades para todo el centro.",
  "Fuimos de excursión a Doñana, a Granada y al Museo de Bellas Artes.",
  "El IES Monte Claro tiene buena relación con las empresas para la FP.",
  "Muy contentos 😊👍 con el centro y con el trato a las familias.",
  "Los de 2º de Bachillerato tienen tutorías de orientación universitaria cada mes.",
  "La AMPA organiza la fiesta de fin de curso y el mercadillo solidario.",
  "Sin quejas: buen ambiente, buen nivel y buena comunicación.",
];

const BLOQUEAN = [
  "La profesora Ana es muy mala y no explica nada.",
  "El profe de Matemáticas, Juan García, siempre llega tarde.",
  "Don Manuel, el director, no responde a las familias.",
  "Pedro Martínez es el peor tutor que hemos tenido.",
  "Con la maestra Lucía los niños aprenden mucho, pero con Javier no.",
  "El conserje Antonio López es un borde.",
  "Hablad con María José Fernández, la jefa de estudios, si tenéis problemas.",
  "La de Inglés, Carmen Ruiz Sánchez, pone exámenes imposibles.",
];

describe("corpus: reseñas normales que se publican", () => {
  test.each(PUBLICAN)("se publica: %s", (texto) => {
    const r = filtrar(texto);
    expect(r.resultado, detalle(texto)).not.toBe("BLOQUEADO");
  });

  test("como mucho un 10 % queda en revisión", () => {
    const dudosas = PUBLICAN.filter((t) => filtrar(t).resultado === "DUDOSO");
    expect(dudosas.length, dudosas.map((t) => `${t} → ${detalle(t)}`).join("\n")).toBeLessThanOrEqual(Math.floor(PUBLICAN.length / 10));
  });
});

test("un salto de línea empieza frase: un apellido al principio de la línea no es dudoso", () => {
  // Before the fix, trimEnd() removed the "\n" and "Campos" counted as mid-sentence (DUDOSO).
  expect(filtrar("Lo que más nos gusta\nCampos de deporte amplios y bien cuidados").resultado).toBe("OK");
  expect(filtrar("Lo que más nos gusta, Campos de deporte amplios").resultado).toBe("DUDOSO");
});

describe("corpus: reseñas con nombres de personas que se bloquean", () => {
  test.each(BLOQUEAN)("se bloquea: %s", (texto) => {
    expect(filtrar(texto).resultado, detalle(texto)).toBe("BLOQUEADO");
  });
});
