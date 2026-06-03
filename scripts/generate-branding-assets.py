#!/usr/bin/env python3
"""Genera íconos Android desde app_icon_source (paridad visual con iOS).

Android adaptive icon recorta ~18 dp por lado; el arte debe ir más pequeño
y centrado (zona segura ~66 % del lienzo 108 dp).
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/branding/app_icon_source.png"
RES = ROOT / "app/src/main/res"

# Fracción del lienzo que ocupa el arte (iOS ~84 %; Android safe zone ~61 %).
ARTWORK_SCALE = 0.62

MIPMAP = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
FOREGROUND = {
    "drawable-mdpi": 108,
    "drawable-hdpi": 162,
    "drawable-xhdpi": 216,
    "drawable-xxhdpi": 324,
    "drawable-xxxhdpi": 432,
}
SPLASH = {
    "drawable-xhdpi": 256,
    "drawable-xxhdpi": 384,
    "drawable-xxxhdpi": 512,
}


def compose_icon(canvas_px: int, src: Image.Image, scale: float) -> Image.Image:
    out = Image.new("RGBA", (canvas_px, canvas_px), (0, 0, 0, 255))
    inner = int(canvas_px * scale)
    art = src.resize((inner, inner), Image.Resampling.LANCZOS)
    x = (canvas_px - inner) // 2
    y = (canvas_px - inner) // 2
    out.paste(art, (x, y), art)
    return out


def main() -> None:
    if not SRC.exists():
        raise SystemExit(f"Falta fuente: {SRC}")
    src = Image.open(SRC).convert("RGBA")

    for folder, px in MIPMAP.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        icon = compose_icon(px, src, ARTWORK_SCALE)
        icon.save(out_dir / "ic_launcher.png", "PNG", optimize=True)
        icon.save(out_dir / "ic_launcher_round.png", "PNG", optimize=True)

    for folder, px in FOREGROUND.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        icon = compose_icon(px, src, ARTWORK_SCALE)
        icon.save(out_dir / "ic_launcher_foreground.png", "PNG", optimize=True)

    for folder, px in SPLASH.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        icon = compose_icon(px, src, 0.72)
        icon.save(out_dir / "splash_logo.png", "PNG", optimize=True)

    print(f"OK — arte al {ARTWORK_SCALE:.0%} del lienzo (launcher)")


if __name__ == "__main__":
    main()
