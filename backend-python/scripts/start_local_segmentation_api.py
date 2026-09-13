from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

parser = argparse.ArgumentParser(description="Start the standalone real-model segmentation API.")
parser.add_argument("--host", default="127.0.0.1", help="HTTP listen address")
parser.add_argument("--port", type=int, default=8001, help="HTTP listen port")
parser.add_argument("--device", default="cuda:0", help="Torch inference device")
args = parser.parse_args()

os.environ.setdefault("CG_HTTP_ENABLED", "true")
os.environ.setdefault("CG_HTTP_HOST", args.host)
os.environ.setdefault("CG_HTTP_PORT", str(args.port))
os.environ.setdefault("CG_MQ_WORKER_ENABLED", "false")
os.environ.setdefault("CG_AI_RUNTIME_MODE", "hybrid")
os.environ.setdefault("CG_MODEL_QUALITY_ENABLED", "false")
os.environ.setdefault("CG_MODEL_TOOTH_DETECT_ENABLED", "false")
os.environ.setdefault("CG_MODEL_SEGMENTATION_ENABLED", "true")
os.environ.setdefault("CG_MODEL_SEGMENTATION_IMPL_TYPE", "ML_MODEL")
os.environ.setdefault("CG_MODEL_GRADING_ENABLED", "false")
os.environ.setdefault("CG_MODEL_RISK_ENABLED", "false")
os.environ.setdefault("CG_MODEL_DEVICE", args.device)
os.environ.setdefault("CG_STRICT_MODEL_STARTUP_VALIDATION", "true")
os.environ.setdefault("CG_LOCAL_SEGMENTATION_API_ENABLED", "true")
os.environ.setdefault(
    "CG_LOCAL_SEGMENTATION_API_OUTPUT_DIR",
    str(PROJECT_ROOT / "runtime-assets" / "segmentation"),
)
os.environ.setdefault("CG_TEMP_DIR", str(PROJECT_ROOT / "runtime-work"))

import uvicorn  # noqa: E402

from app.api.app import create_app  # noqa: E402
from app.core.logging import configure_logging  # noqa: E402


if __name__ == "__main__":
    configure_logging()
    uvicorn.run(create_app(), host=args.host, port=args.port)
