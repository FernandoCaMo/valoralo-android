"""Google Play graphics drawn from the app's own launcher icon (no images from the internet).

    python tools/play/graficos.py            -> docs/play/icono-512.png, docs/play/grafico-destacado-1024x500.png

Needs Pillow. The shapes are those of app/src/main/res/drawable/ic_launcher_foreground.xml
(108x108 viewport): star, roof and building with door and windows.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

AZUL = (0x1F, 0x5E, 0x8C, 255)
AZUL_OSCURO = (0x0B, 0x4A, 0x75, 255)
BLANCO = (255, 255, 255, 255)
ESTRELLA = (0xFF, 0xC9, 0x4A, 255)
SALIDA = Path(__file__).resolve().parents[2] / "docs" / "play"
SUPER = 4  # supersampling for smooth edges

ESTRELLA_PUNTOS = [(54, 27), (56.4, 32.2), (62, 32.8), (57.8, 36.6), (59, 42.2), (54, 39.3), (49, 42.2), (50.2, 36.6), (46, 32.8), (51.6, 32.2)]
TEJADO = [(34, 52), (54, 43), (74, 52)]
CUERPO = (37, 54, 71, 78)
HUECOS = [(51, 68, 57, 78), (41, 58, 47, 64), (61, 58, 67, 64), (41, 68, 47, 74), (61, 68, 67, 74)]


def logo(dibujo: ImageDraw.ImageDraw, x0: float, y0: float, escala: float, fondo) -> None:
    """Draws the launcher foreground; (x0, y0) is the top-left of the 108x108 viewport."""
    p = lambda pts: [(x0 + x * escala, y0 + y * escala) for x, y in pts]
    r = lambda a, b, c, d: [x0 + a * escala, y0 + b * escala, x0 + c * escala, y0 + d * escala]
    dibujo.polygon(p(ESTRELLA_PUNTOS), fill=ESTRELLA)
    dibujo.polygon(p(TEJADO), fill=BLANCO)
    dibujo.rectangle(r(*CUERPO), fill=BLANCO)
    for hueco in HUECOS:
        dibujo.rectangle(r(*hueco), fill=fondo)


def icono(lado: int = 512) -> Image.Image:
    # Play applies its own mask: a full square, no transparency, no rounded corners.
    # The drawing (x 34..74, y 27..78 in the viewport) fills about 60 % of the height, centred.
    grande = lado * SUPER
    img = Image.new("RGBA", (grande, grande), AZUL)
    escala = grande * 0.60 / 51
    logo(ImageDraw.Draw(img), grande / 2 - 54 * escala, grande / 2 - 52.5 * escala, escala, AZUL)
    return img.resize((lado, lado), Image.LANCZOS)


def fuente(negrita: bool, tamano: int) -> ImageFont.FreeTypeFont:
    for nombre in (["segoeuib.ttf", "arialbd.ttf"] if negrita else ["segoeui.ttf", "arial.ttf"]):
        try:
            return ImageFont.truetype(nombre, tamano)
        except OSError:
            continue
    return ImageFont.load_default()


def grafico_destacado() -> Image.Image:
    ancho, alto = 1024 * SUPER, 500 * SUPER
    img = Image.new("RGBA", (ancho, alto), AZUL)
    d = ImageDraw.Draw(img)
    # Soft lighter band on the right so the text area reads well.
    d.rectangle([ancho * 0.36, 0, ancho, alto], fill=AZUL_OSCURO)
    escala = alto * 0.55 / 51
    logo(d, ancho * 0.18 - 54 * escala, alto / 2 - 52.5 * escala, escala, AZUL)
    x = ancho * 0.41
    d.text((x, alto * 0.27), "Valoralo", font=fuente(True, 112 * SUPER), fill=BLANCO)
    subtitulo = fuente(False, 34 * SUPER)
    d.text((x, alto * 0.56), "Opiniones de colegios e institutos", font=subtitulo, fill=BLANCO)
    d.text((x, alto * 0.65), "públicos de Andalucía", font=subtitulo, fill=BLANCO)
    assert d.textlength("Opiniones de colegios e institutos", font=subtitulo) < ancho * 0.56, "el subtítulo no cabe"
    # Play may crop the edges: everything important stays inside the central area.
    return img.resize((1024, 500), Image.LANCZOS)


def main() -> None:
    SALIDA.mkdir(parents=True, exist_ok=True)
    icono().convert("RGB").save(SALIDA / "icono-512.png", optimize=True)
    grafico_destacado().convert("RGB").save(SALIDA / "grafico-destacado-1024x500.png", optimize=True)
    for f in sorted(SALIDA.glob("*.png")):
        with Image.open(f) as i:
            print(f.name, i.size, f"{f.stat().st_size // 1024} KiB")


if __name__ == "__main__":
    main()
