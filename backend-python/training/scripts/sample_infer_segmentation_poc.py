from __future__ import annotations

import argparse
import json
from pathlib import Path

import numpy as np
import onnxruntime as ort
from PIL import Image
import yaml


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Single-image inference for segmentation_v1 ONNX artifact.")
    parser.add_argument("--image", required=True, help="Input image path.")
    parser.add_argument(
        "--artifact-dir",
        default=str(Path(__file__).resolve().parent),
        help="Directory containing model.onnx and preprocess/postprocess files.",
    )
    parser.add_argument(
        "--output-mask",
        help="Optional output mask path. Defaults to <artifact-dir>/inference_mask.png.",
    )
    return parser


def load_configs(artifact_dir: Path) -> tuple[dict, dict]:
    preprocess = yaml.safe_load((artifact_dir / "preprocess.yaml").read_text(encoding="utf-8"))
    postprocess = yaml.safe_load((artifact_dir / "postprocess.yaml").read_text(encoding="utf-8"))
    return preprocess, postprocess


def preprocess_image(image_path: Path, preprocess: dict) -> tuple[np.ndarray, tuple[int, int]]:
    shared = preprocess.get("shared", {})
    normalize = shared.get("normalize", {})
    image_size = tuple(shared.get("imageSize") or [256, 256])
    mean = float((normalize.get("mean") or [0.5])[0])
    std = float((normalize.get("std") or [0.5])[0]) or 1.0

    image = Image.open(image_path).convert("L")
    original_size = image.size
    resized = image.resize(image_size, Image.Resampling.BILINEAR)
    image_array = np.asarray(resized, dtype=np.float32) / 255.0
    image_array = (image_array - mean) / std
    return image_array[None, None, ...].astype(np.float32), original_size


def postprocess_output(raw_output: np.ndarray, original_size: tuple[int, int], threshold: float) -> np.ndarray:
    output = np.asarray(raw_output, dtype=np.float32)
    if output.ndim == 4:
        output = output[0, 0]
    elif output.ndim == 3:
        output = output[0]
    probability = 1.0 / (1.0 + np.exp(-output))
    mask = (probability >= threshold).astype(np.uint8) * 255
    resized = Image.fromarray(mask, mode="L").resize(original_size, Image.Resampling.NEAREST)
    return np.asarray(resized, dtype=np.uint8)


def main() -> int:
    args = build_parser().parse_args()
    image_path = Path(args.image).resolve()
    artifact_dir = Path(args.artifact_dir).resolve()
    if not image_path.is_file():
        raise FileNotFoundError(f"image does not exist: {image_path}")
    if not artifact_dir.is_dir():
        raise FileNotFoundError(f"artifact directory does not exist: {artifact_dir}")

    preprocess, postprocess = load_configs(artifact_dir)
    threshold = float(postprocess.get("segmentation", {}).get("maskThreshold", 0.5))
    output_mask = Path(args.output_mask).resolve() if args.output_mask else artifact_dir / "inference_mask.png"

    session = ort.InferenceSession(str((artifact_dir / "model.onnx").resolve()), providers=["CPUExecutionProvider"])
    input_tensor, original_size = preprocess_image(image_path, preprocess)
    outputs = session.run(["output"], {"input": input_tensor})
    mask = postprocess_output(outputs[0], original_size, threshold)
    Image.fromarray(mask, mode="L").save(output_mask)

    payload = {
        "image": str(image_path),
        "artifactDir": str(artifact_dir),
        "outputMask": str(output_mask),
        "positivePixels": int((mask > 127).sum()),
        "provider": "CPUExecutionProvider",
    }
    print(json.dumps(payload, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
