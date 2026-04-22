"""Base class and taxonomy for all model adapters.

Phase 5 introduces a three-tier implementation type taxonomy:

- **MOCK** — hard-coded template data (Phase 4 and earlier).
- **HEURISTIC** — rule-based / image-statistics / algorithmic (Phase 5A).
- **ML_MODEL** — real PyTorch / ONNX weights (Phase 5B+).
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from enum import Enum

from app.core.logging import get_logger

log = get_logger("cariesguard-ai.model")


class ImplType(str, Enum):
    """Implementation type for a model adapter."""

    MOCK = "MOCK"
    HEURISTIC = "HEURISTIC"
    ML_MODEL = "ML_MODEL"


class BaseModelAdapter(ABC):
    """Abstract base for all model adapters in Phase 5.

    Every concrete adapter MUST declare:
    - ``model_code``       — unique identifier, e.g. ``"quality-check-heuristic-v1"``
    - ``model_type_code``  — functional category, e.g. ``"QUALITY"``
    - ``impl_type``        — one of :class:`ImplType`
    """

    model_code: str
    model_type_code: str
    impl_type: ImplType
    dataset_version: str | None = None
    manifest_path: str | None = None

    @abstractmethod
    def load(self) -> None:
        """Pre-load resources (weights, lookup tables, etc.)."""

    @abstractmethod
    def is_loaded(self) -> bool:
        """Return *True* if the adapter is ready for inference."""

    @abstractmethod
    def unload(self) -> None:
        """Release resources held by the adapter."""

    def bind_manifest(self, manifest: object) -> None:
        model_code = getattr(manifest, "model_code", None)
        if isinstance(model_code, str) and model_code.strip():
            self.model_code = model_code.strip()

        dataset_version = getattr(manifest, "dataset_version", None)
        value = str(dataset_version or "").strip()
        self.dataset_version = value or None

        manifest_path = getattr(manifest, "manifest_path", None)
        self.manifest_path = str(manifest_path) if manifest_path is not None else None

    def info(self) -> dict:
        """Return a lightweight descriptor for health-check / debug."""
        return {
            "modelCode": self.model_code,
            "modelTypeCode": self.model_type_code,
            "implType": self.impl_type.value,
            "datasetVersion": self.dataset_version,
            "manifestPath": self.manifest_path,
            "loaded": self.is_loaded(),
        }
