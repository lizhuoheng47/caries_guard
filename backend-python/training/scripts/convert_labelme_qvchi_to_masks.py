from __future__ import annotations

import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

INPUT_DIR = Path("data/traindata")
TARGET_LABEL = "qvchi"
MASK_SUFFIX = "-mask.png"


def convert_one(json_path: Path) -> None:
    data = json.loads(json_path.read_text(encoding="utf-8"))

    width = int(data["imageWidth"])
    height = int(data["imageHeight"])

    image_path = data.get("imagePath")
    if image_path:
        stem = Path(image_path).stem
    else:
        stem = json_path.stem

    mask = Image.new("L", (width, height), 0)
    draw = ImageDraw.Draw(mask)

    for shape in data.get("shapes", []):
        if shape.get("label") != TARGET_LABEL:
            continue
        if shape.get("shape_type", "polygon") != "polygon":
            continue

        points = shape.get("points", [])
        if len(points) < 3:
            continue

        polygon = [(float(x), float(y)) for x, y in points]
        draw.polygon(polygon, outline=255, fill=255)

    out_path = INPUT_DIR / f"{stem}{MASK_SUFFIX}"
    mask.save(out_path)
    print(f"[OK] {json_path.name} -> {out_path.name}")


def main() -> None:
    json_files = sorted(INPUT_DIR.glob("*.json"))
    if not json_files:
        raise SystemExit(f"No json files found in {INPUT_DIR}")

    for json_file in json_files:
        convert_one(json_file)

    print("Done.")


if __name__ == "__main__":
    main()