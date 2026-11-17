/**
 * Builds the centros/{codigo}/valoraciones/{uid} document from a validated form
 * (docs/decisiones.md, Fase 5):
 *  - the text and any "Otro departamento" go through the filter: BLOQUEADO is never saved,
 *    DUDOSO is saved EN_REVISION with the fragments so the author can edit them;
 *  - editing a review that was EN_REVISION because of the filter publishes it if it now passes;
 *    one under review because of reports, or hidden by a moderator, goes (back) to review;
 *  - rolAutor comes from the profile on creation (an unverified teacher counts as OTRO) and
 *    is kept on later edits; fecha is the creation date.
 */
import { Estado, Rol } from "./calculo";
import { ContextoFiltro, filtrarTexto, Fragmento } from "./filtro/filtro";
import { Formulario } from "./validacion";

export type OrigenRevision = "FILTRO" | "DENUNCIAS" | "MODERACION";
export interface Revision {
  origen: OrigenRevision;
  fragmentos: Fragmento[];
}

export interface PerfilAutor {
  uid: string;
  alias: string;
  rol: Rol;
  verificado: boolean;
}

export interface ValoracionGuardada {
  uid: string;
  alias: string;
  rolAutor: Rol;
  verificado: boolean;
  fecha: Date;
  estado: Estado;
  revision: Revision | null;
}

export interface ValoracionNueva extends ValoracionGuardada {
  actualizada: Date;
  editada: boolean;
  centro: Formulario["centro"];
  instalaciones: Formulario["instalaciones"];
  profesorado: Formulario["profesorado"];
  texto: string;
}

export class ErrorFiltro extends Error {
  constructor(public readonly fragmentos: Fragmento[]) {
    super("El texto contiene nombres propios, insultos o datos personales.");
  }
}

/** Filters the text plus the names of "Otro departamento" (shifting their positions past the text). */
function filtrar(formulario: Formulario, contexto: ContextoFiltro) {
  const otros = formulario.profesorado.filter((p) => p.otro).map((p) => p.departamento);
  const resultadoTexto = filtrarTexto(formulario.texto, contexto);
  const resultadosOtros = otros.map((nombre) => filtrarTexto(nombre, { permitidas: contexto.permitidas }));
  // A capitalised surname alone in a department name is reason enough to block it.
  const fragmentosOtros = resultadosOtros.flatMap((r, i) => r.fragmentos.map((f) => ({
    ...f,
    motivo: f.motivo === "NOMBRE_DUDOSO" ? "NOMBRE_PROPIO" as const : f.motivo,
    texto: `${f.texto} (en "${otros[i]}")`,
    inicio: -1,
    fin: -1,
  })));
  const fragmentos = [...resultadoTexto.fragmentos, ...fragmentosOtros];
  const bloquea = resultadoTexto.resultado === "BLOQUEADO"
    || fragmentosOtros.some((f) => ["INSULTO", "NOMBRE_PROPIO", "DATO_PERSONAL"].includes(f.motivo));
  return { fragmentos, resultado: bloquea ? "BLOQUEADO" : fragmentos.length ? "DUDOSO" : "OK" };
}

function rolDeAutor(perfil: PerfilAutor): { rolAutor: Rol; verificado: boolean } {
  if (perfil.rol === "DOCENTE") return perfil.verificado ? { rolAutor: "DOCENTE", verificado: true } : { rolAutor: "OTRO", verificado: false };
  return { rolAutor: perfil.rol, verificado: false };
}

export function construirValoracion(
  formulario: Formulario,
  perfil: PerfilAutor,
  previa: ValoracionGuardada | null,
  ahora: Date,
  contexto: ContextoFiltro,
): ValoracionNueva {
  const filtro = filtrar(formulario, contexto);
  if (filtro.resultado === "BLOQUEADO") throw new ErrorFiltro(filtro.fragmentos);

  // A review a moderator hid, or one held because of reports, only goes back to public through
  // a moderator: editing it sends it to review even if the new text passes (or is doubtful).
  // Only reviews held by the filter alone are published automatically once corrected.
  const origenModeracion: OrigenRevision | null =
    previa?.estado === "OCULTA" ? "MODERACION"
      : previa?.estado === "EN_REVISION" && previa.revision && previa.revision.origen !== "FILTRO" ? previa.revision.origen
        : null;

  let estado: Estado = "PUBLICADA";
  let revision: Revision | null = null;
  if (origenModeracion) {
    estado = "EN_REVISION";
    revision = { origen: origenModeracion, fragmentos: filtro.fragmentos };
  } else if (filtro.resultado === "DUDOSO") {
    estado = "EN_REVISION";
    revision = { origen: "FILTRO", fragmentos: filtro.fragmentos };
  }

  const rol = previa ? { rolAutor: previa.rolAutor, verificado: previa.verificado } : rolDeAutor(perfil);
  return {
    uid: perfil.uid,
    alias: perfil.alias,
    ...rol,
    fecha: previa?.fecha ?? ahora,
    actualizada: ahora,
    editada: previa !== null,
    centro: formulario.centro,
    instalaciones: formulario.instalaciones,
    profesorado: formulario.profesorado,
    texto: formulario.texto,
    estado,
    revision,
  };
}
