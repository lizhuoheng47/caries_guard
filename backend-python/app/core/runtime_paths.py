from pathlib import Path
from typing import Any


def local_segmentation_output_dir(settings: Any) -> Path:
    path = Path(str(settings.local_segmentation_api_output_dir))
    if not path.is_absolute():
        path = Path(__file__).resolve().parents[2] / path
    return path.resolve()
