// node --test (from admin/)
import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { agruparDenuncias, MOTIVO_DENUNCIA, MOTIVOS_OCULTAR, segmentos, textoDestacadas, usarAppCheck, validarNota } from "../logica.js";

const casos = JSON.parse(readFileSync(new URL("../../functions/test/casos_moderacion.json", import.meta.url), "utf8"));

test("los motivos del panel son los mismos que los de la función y el script", () => {
  assert.deepEqual(MOTIVOS_OCULTAR, casos.motivos);
});

test("resaltado de fragmentos", () => {
  assert.deepEqual(segmentos("La profe Ana es genial", [{ inicio: 9, fin: 12, motivo: "NOMBRE_PROPIO" }]), [
    { texto: "La profe ", marcado: false },
    { texto: "Ana", marcado: true, motivo: "NOMBRE_PROPIO" },
    { texto: " es genial", marcado: false },
  ]);
  assert.deepEqual(segmentos("juan", [{ inicio: 0, fin: 4, motivo: "NOMBRE_DUDOSO" }]), [{ texto: "juan", marcado: true, motivo: "NOMBRE_DUDOSO" }]);
  // Fragments of "Otro departamento" (inicio -1) or out of range are ignored.
  assert.deepEqual(segmentos("Hola", [{ inicio: -1, fin: -1 }, { inicio: 2, fin: 99 }]), [{ texto: "Hola", marcado: false }]);
  assert.deepEqual(segmentos("", undefined), []);
});

test("denuncias agrupadas por reseña, las más denunciadas primero", () => {
  const g = agruparDenuncias([
    { centro: "1", valoracionUid: "a", motivo: "INSULTOS" },
    { centro: "1", valoracionUid: "b", motivo: "OTRO" },
    { centro: "1", valoracionUid: "a", motivo: "INSULTOS" },
    { centro: "2", valoracionUid: "a", motivo: "NOMBRE_PROPIO" },
  ]);
  assert.deepEqual(g[0], { centro: "1", uid: "a", n: 2, motivos: { INSULTOS: 2 } });
  assert.equal(g.length, 3);
});

test("texto del recálculo de destacadas", () => {
  const c = { minCaracteres: 120, maxDias: 180, maxResenas: 12, maxFraccionAltas: 1 / 3, umbralAlta: 4.5, minimoParaMostrar: 3 };
  assert.equal(textoDestacadas(5, c), "5 reseñas destacadas (texto ≥ 120 caracteres, últimos 180 días, máx. 12, hasta un 33 % con 4.5 o más).");
  assert.match(textoDestacadas(1, c), /^1 reseña destacada .*Son menos de 3/);
});

test("App Check solo en el panel publicado y con la clave puesta", () => {
  assert.equal(usarAppCheck("", "valoralo-admin.web.app"), false);
  assert.equal(usarAppCheck("6Lc-clave", "valoralo-admin.web.app"), true);
  assert.equal(usarAppCheck("6Lc-clave", "localhost"), false);
  assert.equal(usarAppCheck("6Lc-clave", "127.0.0.1"), false);
});

test("el motivo de denuncia de usuario tiene etiqueta", () => {
  assert.ok(MOTIVO_DENUNCIA.USUARIO_ABUSIVO);
});

test("nota interna de hasta 500 caracteres", () => {
  assert.equal(validarNota(""), null);
  assert.notEqual(validarNota("x".repeat(501)), null);
});
