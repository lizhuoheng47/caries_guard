from __future__ import annotations

import argparse
import json
import mimetypes
from pathlib import Path

import requests


def main() -> int:
    parser = argparse.ArgumentParser(description="Send one dental X-ray to the local segmentation API.")
    parser.add_argument("--image", required=True, help="PNG, JPG, or DICOM file")
    parser.add_argument("--base-url", default="http://127.0.0.1:8001", help="API server base URL")
    args = parser.parse_args()

    image_path = Path(args.image).resolve()
    if not image_path.is_file():
        raise SystemExit(f"image does not exist: {image_path}")
    content_type = {
        ".png": "image/png",
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".dcm": "application/dicom",
        ".dicom": "application/dicom",
    }.get(image_path.suffix.lower()) or mimetypes.guess_type(image_path.name)[0]
    if content_type not in {"image/png", "image/jpeg", "application/dicom"}:
        raise SystemExit("unsupported image extension; use PNG, JPG, JPEG, DCM, or DICOM")

    base_url = args.base_url.rstrip("/")
    health_response = requests.get(f"{base_url}/ai/v1/segment/health", timeout=30)
    health_response.raise_for_status()
    health = health_response.json().get("data") or {}
    with image_path.open("rb") as image_file:
        response = requests.post(
            f"{base_url}/ai/v1/segment",
            data=image_file,
            headers={"Content-Type": content_type},
            timeout=180,
        )
    response.raise_for_status()
    payload = response.json()
    if payload.get("code") != "00000":
        print(json.dumps(payload, ensure_ascii=False, indent=2))
        return 2
    result = payload.get("data") or {}
    print(json.dumps(
        {
            "serverStatus": health.get("status"),
            "modelReady": health.get("ready"),
            "modelCode": result.get("modelCode"),
            "implementationType": result.get("implementationType"),
            "device": result.get("device"),
            "inferenceMode": result.get("inferenceMode"),
            "regionCount": result.get("regionCount"),
            "segmentationScore": result.get("segmentationScore"),
            "assets": result.get("assets"),
            "needsReview": result.get("needsReview"),
        },
        ensure_ascii=False,
        indent=2,
    ))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
