/** Validation of the review form sent to publicarValoracion. */
export const LIMITE_TEXTO = 1000;
const MAX_DEPARTAMENTOS = 30;

export class ErrorValidacion extends Error {
  constructor(public readonly campo: string, mensaje: string) {
    super(mensaje);
  }
}

export interface Formulario {
  codigo: string;
  centro: { nota: number; gestion?: number; convivencia?: number; comunicacion?: number };
  instalaciones: { nota: number; aulas?: number; equipamiento?: number; limpieza?: number; accesibilidad?: number };
  profesorado: { departamento: string; nota: number; otro?: true }[];
  texto: string;
}

const esNota = (v: unknown): v is number => typeof v === "number" && Number.isInteger(v) && v >= 1 && v <= 5;

function nota(valor: unknown, campo: string): number {
  if (!esNota(valor)) throw new ErrorValidacion(campo, "Las notas van de 1 a 5.");
  return valor;
}

function bloque<K extends string>(valor: unknown, campo: string, opcionales: K[]): { nota: number } & Partial<Record<K, number>> {
  const datos = (typeof valor === "object" && valor !== null ? valor : {}) as Record<string, unknown>;
  const resultado = { nota: nota(datos.nota, `${campo}.nota`) } as { nota: number } & Partial<Record<K, number>>;
  for (const clave of opcionales) {
    if (datos[clave] !== undefined && datos[clave] !== null) {
      (resultado as Record<string, number>)[clave] = nota(datos[clave], `${campo}.${clave}`);
    }
  }
  return resultado;
}

const clave = (s: string) => s.normalize("NFD").replace(/\p{Mn}+/gu, "").toLowerCase();

export function validarFormulario(datos: unknown, departamentosDelCentro: string[]): Formulario {
  const d = (typeof datos === "object" && datos !== null ? datos : {}) as Record<string, unknown>;

  if (typeof d.codigo !== "string" || !/^\d{8}$/.test(d.codigo)) {
    throw new ErrorValidacion("codigo", "Código de centro no válido.");
  }

  const lista = Array.isArray(d.profesorado) ? d.profesorado : [];
  if (lista.length > MAX_DEPARTAMENTOS) throw new ErrorValidacion("profesorado", "Demasiados departamentos.");
  const vistos = new Set<string>();
  const profesorado = lista.map((item, i) => {
    const p = (typeof item === "object" && item !== null ? item : {}) as Record<string, unknown>;
    const campo = `profesorado[${i}]`;
    const nombre = typeof p.departamento === "string" ? p.departamento.trim().replace(/\s+/g, " ") : "";
    const otro = p.otro === true;
    if (otro) {
      if (nombre.length < 3 || nombre.length > 40 || !/^[\p{L} -]+$/u.test(nombre)) {
        throw new ErrorValidacion(campo, "\"Otro departamento\" debe tener entre 3 y 40 letras.");
      }
    } else if (!departamentosDelCentro.includes(nombre)) {
      throw new ErrorValidacion(campo, `El departamento "${nombre}" no es de este centro.`);
    }
    if (vistos.has(clave(nombre))) throw new ErrorValidacion(campo, `Departamento repetido: "${nombre}".`);
    vistos.add(clave(nombre));
    const resultado: Formulario["profesorado"][number] = { departamento: nombre, nota: nota(p.nota, `${campo}.nota`) };
    if (otro) resultado.otro = true;
    return resultado;
  });

  // At most one blank line between paragraphs (QA 1.0.5, finding 6: 8 line breaks left a huge gap).
  const texto = typeof d.texto === "string"
    ? d.texto.replace(/\r\n?/g, "\n").replace(/[ \t]+\n/g, "\n").replace(/\n{3,}/g, "\n\n").trim()
    : "";
  if (texto.length > LIMITE_TEXTO) {
    throw new ErrorValidacion("texto", "El texto puede tener como máximo 1.000 caracteres.");
  }

  return {
    codigo: d.codigo,
    centro: bloque(d.centro, "centro", ["gestion", "convivencia", "comunicacion"]),
    instalaciones: bloque(d.instalaciones, "instalaciones", ["aulas", "equipamiento", "limpieza", "accesibilidad"]),
    profesorado,
    texto,
  };
}
