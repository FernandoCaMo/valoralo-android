/**
 * Aggregated averages of a centro (doc 02, "Cálculo de las notas"):
 *  - per review: profesorado = mean of the departments it rates (none → no profesorado);
 *    global = mean of its dimensions, equal weight;
 *  - per centro and role group: each average apart, over the reviews that have that data;
 *  - only PUBLICADA reviews count; values are stored unrounded.
 */
export type Rol = "DOCENTE" | "ESTUDIANTE" | "OTRO";
export type Estado = "PUBLICADA" | "EN_REVISION" | "OCULTA";

export interface ValoracionParaStats {
  rolAutor: Rol;
  estado: Estado;
  centro: { nota: number };
  instalaciones: { nota: number };
  profesorado: { departamento: string; nota: number }[];
}

export interface StatsGrupo {
  n: number;
  /** Reviews of the group that rate at least one department (the profesorado average uses only these). */
  nProfesorado: number;
  global: number | null;
  centro: number | null;
  instalaciones: number | null;
  profesorado: number | null;
  porDepartamento: Record<string, { n: number; media: number }>;
}

export interface Stats {
  total: StatsGrupo;
  docente: StatsGrupo;
  estudiante: StatsGrupo;
  otro: StatsGrupo;
}

const media = (valores: number[]): number | null =>
  valores.length === 0 ? null : valores.reduce((a, b) => a + b, 0) / valores.length;

function grupo(valoraciones: ValoracionParaStats[]): StatsGrupo {
  const profesorado = valoraciones.map((v) => media(v.profesorado.map((d) => d.nota)));
  const global = valoraciones.map((v, i) => {
    const dimensiones = [v.centro.nota, v.instalaciones.nota];
    if (profesorado[i] !== null) dimensiones.push(profesorado[i] as number);
    return media(dimensiones) as number;
  });
  const porDepartamento: Record<string, { n: number; media: number }> = {};
  const notasPorDepartamento = new Map<string, number[]>();
  for (const v of valoraciones) {
    for (const d of v.profesorado) {
      notasPorDepartamento.set(d.departamento, [...(notasPorDepartamento.get(d.departamento) ?? []), d.nota]);
    }
  }
  for (const [departamento, notas] of notasPorDepartamento) {
    porDepartamento[departamento] = { n: notas.length, media: media(notas) as number };
  }
  return {
    n: valoraciones.length,
    nProfesorado: profesorado.filter((p) => p !== null).length,
    global: media(global),
    centro: media(valoraciones.map((v) => v.centro.nota)),
    instalaciones: media(valoraciones.map((v) => v.instalaciones.nota)),
    profesorado: media(profesorado.filter((p): p is number => p !== null)),
    porDepartamento,
  };
}

export function calcularStats(valoraciones: ValoracionParaStats[]): Stats {
  const publicadas = valoraciones.filter((v) => v.estado === "PUBLICADA");
  return {
    total: grupo(publicadas),
    docente: grupo(publicadas.filter((v) => v.rolAutor === "DOCENTE")),
    estudiante: grupo(publicadas.filter((v) => v.rolAutor === "ESTUDIANTE")),
    otro: grupo(publicadas.filter((v) => v.rolAutor === "OTRO")),
  };
}
