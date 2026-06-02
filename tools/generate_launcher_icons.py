"""Generate Android launcher mipmaps from app/src/main/ic_launcher_source.jpg."""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app" / "src" / "main" / "ic_launcher_source.jpg"
RES = ROOT / "app" / "src" / "main" / "res"

# Legacy launcher icon sizes (px)
LEGACY_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Adaptive foreground layer sizes (108dp @ density)
FOREGROUND_SIZES = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}

# Logo occupies ~72% of adaptive canvas (safe zone)
FOREGROUND_SCALE = 0.72

PLAY_STORE_SIZE = 512


def load_source() -> Image.Image:
    img = Image.open(SOURCE).convert("RGBA")
    if img.width != img.height:
        side = min(img.width, img.height)
        left = (img.width - side) // 2
        top = (img.height - side) // 2
        img = img.crop((left, top, left + side, top + side))
    return img


def composite_on_white(source: Image.Image, size: int) -> Image.Image:
    logo = source.resize((size, size), Image.Resampling.LANCZOS)
    if logo.mode != "RGBA":
        return logo.convert("RGB")
    background = Image.new("RGBA", (size, size), (255, 255, 255, 255))
    background.paste(logo, (0, 0), logo)
    return background.convert("RGB")


def make_foreground(source: Image.Image, canvas: int) -> Image.Image:
    logo_size = max(1, int(canvas * FOREGROUND_SCALE))
    logo = source.resize((logo_size, logo_size), Image.Resampling.LANCZOS)
    foreground = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    offset = (canvas - logo_size) // 2
    foreground.paste(logo, (offset, offset), logo)
    return foreground


def save_webp(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if img.mode == "RGBA":
        img.save(path, "WEBP", quality=95, method=6)
    else:
        img.save(path, "WEBP", quality=95, method=6)


def main() -> None:
    if not SOURCE.exists():
        raise SystemExit(f"Source icon not found: {SOURCE}")

    source = load_source()

    for folder, size in LEGACY_SIZES.items():
        legacy = composite_on_white(source, size)
        out_dir = RES / folder
        save_webp(legacy, out_dir / "ic_launcher.webp")
        save_webp(legacy, out_dir / "ic_launcher_round.webp")

    for folder, size in FOREGROUND_SIZES.items():
        foreground = make_foreground(source, size)
        save_webp(foreground, RES / folder / "ic_launcher_foreground.webp")

    play_store = composite_on_white(source, PLAY_STORE_SIZE)
    play_store.save(ROOT / "app" / "src" / "main" / "ic_launcher-playstore.png", "PNG")
    print("Generated launcher icons from", SOURCE)


if __name__ == "__main__":
    main()
