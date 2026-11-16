/**
 * Word lists of the text filter. All entries are normalised (lower case, no accents).
 * Names and surnames come from the INE (see tools/filtro/generar_listas.py).
 */
import nombresIne from "./datos/nombres.json";
import apellidosIne from "./datos/apellidos.json";

/** Insults that always block. An entry ending in "*" matches any word starting with it. */
export const INSULTOS = [
  "gilipollas", "gilipolla*", "gilipuertas", "gilipichi", "imbecil*", "idiota*", "estupid*", "subnormal*",
  "cabron*", "hijoputa*", "hijaputa*", "hdp", "puta", "putas", "puto", "putos", "putero*", "zorra*", "mierda*",
  "capullo*", "mamon", "mamona", "mamones", "malparid*", "cretin*", "tarad*", "mongolo*", "mongola*",
  "maricon*", "marica", "maricas", "bollera*", "sudaca*", "panchito*", "negrata*", "polla", "pollas",
  "coño", "cono", "chocho", "follar*", "follad*", "soplapollas", "chupapollas", "tontopolla*",
  "tontolculo", "tontolaba", "comemierda*", "lameculos", "mamarracho*", "pringad*", "lerdo", "lerda",
  "lerdos", "lerdas", "merluzo*", "carajote*", "malaje*", "malafolla*", "mangurrian*", "esaborio*",
  "malasombra*", "mascachapas", "cagaprisas",
];

/** Insults that also have innocent meanings: only doubtful, and only after "un", "eres"… */
export const INSULTOS_AMBIGUOS = [
  "tonto", "tonta", "tontos", "tontas", "inutil", "inutiles", "payaso", "payasa", "payasos", "cerdo", "cerda",
  "cateto", "cateta", "bobo", "boba", "retrasado", "retrasada", "burro", "burra", "rata", "asno", "borrico",
  "gañan", "ganan", "cansino", "cansina", "jartible", "pelota", "pelotas", "vago", "vaga",
];
export const ANTE_INSULTO = ["un", "una", "unos", "unas", "so", "eres", "sois", "menudo", "menuda", "vaya", "pedazo"];

/** Words that announce a person: the next capitalised word or name is blocked. */
export const TRATAMIENTOS = [
  "profe", "profes", "profesor", "profesora", "profesores", "profesoras", "maestro", "maestra", "maestros",
  "maestras", "tutor", "tutora", "director", "directora", "jefe", "jefa", "jefatura", "conserje", "orientador",
  "orientadora", "don", "dona", "doña", "sr", "sra", "srta", "senor", "senora", "senorita", "señor", "señora",
  "señorita", "secretario", "secretaria", "coordinador", "coordinadora",
];

/** First names that are also common words: only doubtful, and only when capitalised. */
export const NOMBRES_AMBIGUOS = [
  "rosa", "pilar", "luz", "mar", "sol", "paz", "cruz", "blanca", "esperanza", "victoria", "dolores", "angeles",
  "rocio", "consuelo", "amparo", "mercedes", "soledad", "aurora", "gloria", "alegria", "fe", "caridad", "salud",
  "remedios", "milagros", "socorro", "concepcion", "asuncion", "encarnacion", "inmaculada", "estrella", "perla",
  "palma", "reyes", "nieves", "flor", "flora", "angel", "leon", "franco", "clemente", "justo", "modesto",
  "severo", "inocencio", "felicidad", "carmen", "pura", "purificacion", "trinidad", "natividad", "rosario",
  "marina", "alba", "iris", "gracia", "celeste", "azucena", "margarita", "violeta", "jazmin", "dalia", "gema",
  "coral", "oliva", "pastora", "marcos", "tomas", "domingo", "ventura", "jesus", "santiago", "luna", "paloma",
  "alma", "cielo", "pia", "benigno", "leal", "abril", "olivia", "triana", "macarena", "sierra", "vega", "valle",
  "nieve", "candela", "noemi", "camino", "carmelo", "loreto", "lourdes", "montserrat", "sagrario", "begoña",
  "fatima", "cristo", "salvador", "santos", "bueno", "cano", "rey", "conde", "bravo", "prado", "rio",
];

/** Lowercase names flagged as doubtful: only the most frequent, unambiguous ones. */
export const NOMBRES_FRECUENTES_EN_MINUSCULA = 300;

/** Capitalised words that are not people (places, institutions, subjects…). */
export const PERMITIDAS_BASE = [
  "andalucia", "espana", "almeria", "cadiz", "cordoba", "granada", "huelva", "jaen", "malaga", "sevilla",
  "madrid", "europa", "junta", "consejeria", "ministerio", "ayuntamiento", "ampa", "eso", "fp", "bachillerato",
  "selectividad", "pevau", "pau", "erasmus", "navidad", "semana", "santa", "feria", "carnaval", "pasen",
  "ipasen", "seneca", "moodle", "classroom", "google", "teams", "matematicas", "lengua", "literatura",
  "ingles", "frances", "aleman", "historia", "geografia", "biologia", "geologia", "fisica", "quimica",
  "tecnologia", "informatica", "dibujo", "musica", "filosofia", "economia", "latin", "griego", "religion",
  "educacion", "plastica", "orientacion", "departamento", "infantil", "primaria", "secundaria",
  "artes", "ciencias", "humanidades", "sociales", "general", "tecnologico", "deporte", "deportes", "ciclo",
  "ciclos", "grado", "medio", "superior", "basico", "basica", "dam", "daw", "smr", "asir", "fct", "fol",
  "ies", "ceip", "cepr", "cpr", "cifp", "eoi", "ceper", "sep", "conservatorio", "escuela", "instituto",
  "colegio", "centro", "aula", "biblioteca", "gimnasio", "comedor", "laboratorio", "taller", "talleres",
  "claustro", "consejo", "escolar", "dual", "empresa", "mates", "ingles", "edufis",
  "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre",
  "noviembre", "diciembre", "lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo",
];

export const NOMBRES: string[] = nombresIne as string[];
export const APELLIDOS: string[] = apellidosIne as string[];
