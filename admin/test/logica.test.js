// node --test (from admin/)
import { test } from "node:test";
import assert from "node:assert/strict";
import { comprobaciones, diasDesde, esAdmin, formatearFecha, textoAntiguedad, validarCentro, validarMotivo } from "../logica.js";

test("antigüedad: hoy, ayer, hace N días", () => {
  const ahora = new Date("2026-11-22T12:00:00Z");
  assert.equal(textoAntiguedad(new Date("2026-11-22T08:00:00Z"), ahora), "hoy");
  assert.equal(textoAntiguedad(new Date("2026-11-21T08:00:00Z"), ahora), "ayer");
  assert.equal(textoAntiguedad(new Date("2026-11-17T08:00:00Z"), ahora), "hace 5 días");
});

test("solo la marca admin === true da acceso", () => {
  assert.equal(esAdmin({ admin: true }), true);
  assert.equal(esAdmin({ admin: "true" }), false);
  assert.equal(esAdmin({ rol: "DOCENTE" }), false);
  assert.equal(esAdmin(undefined), false);
});

test("fechas en hora de Madrid", () => {
  assert.equal(formatearFecha(new Date("2026-11-17T09:05:00Z")), "17/11/2026 10:05");
  assert.equal(formatearFecha(new Date("2026-07-01T22:30:00Z"), false), "02/07/2026");
  assert.equal(formatearFecha(null), "—");
});

test("días desde la solicitud", () => {
  assert.equal(diasDesde(new Date("2026-11-01T00:00:00Z"), new Date("2026-11-17T12:00:00Z")), 16);
});

test("motivo de rechazo y centro actual", () => {
  assert.notEqual(validarMotivo(""), null);
  assert.notEqual(validarMotivo("x".repeat(301)), null);
  assert.equal(validarMotivo("  El nombre no coincide. "), null);
  assert.equal(validarCentro(""), null);
  assert.equal(validarCentro("21000693"), null);
  assert.notEqual(validarCentro("2100"), null);
});

test("las comprobaciones incluyen la fecha de alta", () => {
  const c = comprobaciones(new Date("2026-11-10T10:00:00Z"));
  assert.equal(c.length, 3);
  assert.match(c[2], /10\/11\/2026/);
});
