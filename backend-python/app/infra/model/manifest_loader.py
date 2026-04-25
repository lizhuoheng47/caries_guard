from __future__ import annotations

import hashlib
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import yaml

from app.core.exceptions import ModelRuntimeException


@dataclass(frozen=True)
class ModuleManifest:
    module_name: str
    manifest_path: Path
    model_code: str
    task_type: str
    status: str
    dataset_code: str | None
    dataset_version: str | None
    arch: str | None
    checkpoint_path: Path | None
    checkpoint_exists: bool
    checkpoint_format: str | None
    checkpoint_sha256: str | None
    checkpoint_declared_sha256: str | None
    checkpoint_actual_sha256: str | None
    exported_at: str | None
    class_map_path: Path | None
    preprocess_path: Path | None
    postprocess_path: Path | None
    expected_input_size: tuple[int, int] | None
    expected_num_classes: int | None
    label_order: tuple[str, ...]
    raw: dict[str, Any]


class ManifestLoader:
    def __init__(self, project_root: Path | None = None) -> None:
        self._project_root = (project_root or Path(__file__).resolve().parents[3]).resolve()

    def load(self, module_name: str, path_value: str) -> ModuleManifest:
        manifest_path = self.resolve_path(path_value)
        if not manifest_path.is_file():
            raise ModelRuntimeException(
                module_name,
                "manifest",
                f"{module_name} manifest does not exist: {manifest_path}",
                details={"manifestPath": str(manifest_path)},
            )

        raw = self._load_yaml(manifest_path, module_name)
        artifact = raw.get("artifact", {})
        dataset = raw.get("dataset", {})
        input_spec = raw.get("inputSpec", {})
        label_spec = raw.get("labelSpec", {})

        checkpoint_path = None
        if isinstance(artifact, dict) and artifact.get("checkpointPath"):
            checkpoint_path = self.resolve_path(str(artifact["checkpointPath"]))
        checkpoint_exists = bool(checkpoint_path is not None and checkpoint_path.is_file())
        checkpoint_actual_sha256 = self._sha256(checkpoint_path) if checkpoint_exists and checkpoint_path is not None else None
        checkpoint_declared_sha256 = self._text(artifact.get("checkpointSha256")) if isinstance(artifact, dict) else None
        checkpoint_sha256 = checkpoint_actual_sha256 or checkpoint_declared_sha256
        checkpoint_format = self._text(artifact.get("checkpointFormat")) if isinstance(artifact, dict) else None

        dataset_code = self._text(dataset.get("datasetCode")) if isinstance(dataset, dict) else None
        dataset_version = self._dataset_version(dataset, dataset_code)
        arch = self._infer_arch(raw, module_name, checkpoint_format)
        expected_input_size = self._parse_input_size(input_spec.get("expectedImageSize") if isinstance(input_spec, dict) else None)
        expected_num_classes = self._parse_positive_int(self._nested_get(raw, "outputSpec", "numClasses"))
        label_order = self._label_order(label_spec)

        if expected_num_classes is None and label_order:
            expected_num_classes = len(label_order)

        return ModuleManifest(
            module_name=str(module_name or "").strip().lower(),
            manifest_path=manifest_path,
            model_code=self._text(raw.get("modelCode")) or str(module_name or "").strip().lower(),
            task_type=self._text(raw.get("taskType")) or str(module_name or "").strip().upper(),
            status=self._text(raw.get("status")) or "UNKNOWN",
            dataset_code=dataset_code,
            dataset_version=dataset_version,
            arch=arch,
            checkpoint_path=checkpoint_path,
            checkpoint_exists=checkpoint_exists,
            checkpoint_format=checkpoint_format,
            checkpoint_sha256=checkpoint_sha256,
            checkpoint_declared_sha256=checkpoint_declared_sha256,
            checkpoint_actual_sha256=checkpoint_actual_sha256,
            exported_at=self._text(artifact.get("exportedAt")) if isinstance(artifact, dict) else None,
            class_map_path=self._resolve_optional_path(dataset.get("classMapPath")) if isinstance(dataset, dict) else None,
            preprocess_path=self._resolve_optional_path(input_spec.get("preprocessConfigPath")) if isinstance(input_spec, dict) else None,
            postprocess_path=self._resolve_optional_path(self._nested_get(raw, "outputSpec", "postprocessConfigPath")),
            expected_input_size=expected_input_size,
            expected_num_classes=expected_num_classes,
            label_order=label_order,
            raw=raw,
        )

    def resolve_path(self, path_value: str) -> Path:
        path = Path(path_value)
        normalized = path.as_posix()
        if normalized.startswith("/app/"):
            return (self._project_root / normalized.removeprefix("/app/")).resolve()
        if path.is_absolute():
            return path
        return (self._project_root / path).resolve()

    def _resolve_optional_path(self, value: Any) -> Path | None:
        text = self._text(value)
        if text is None:
            return None
        return self.resolve_path(text)

    @staticmethod
    def _load_yaml(path: Path, module_name: str) -> dict[str, Any]:
        with path.open("r", encoding="utf-8") as file:
            loaded = yaml.safe_load(file)
        if not isinstance(loaded, dict):
            raise ModelRuntimeException(
                module_name,
                "manifest",
                f"{module_name} manifest must be a YAML mapping: {path}",
                details={"manifestPath": str(path)},
            )
        return loaded

    @staticmethod
    def _dataset_version(dataset: Any, dataset_code: str | None) -> str | None:
        if not isinstance(dataset, dict):
            return dataset_code
        for key in ("annotationVersion", "datasetVersion", "version"):
            value = ManifestLoader._text(dataset.get(key))
            if value is not None:
                return value
        return dataset_code

    @staticmethod
    def _infer_arch(raw: dict[str, Any], module_name: str, checkpoint_format: str | None) -> str | None:
        candidates = (
            ("arch",),
            ("runtime", "arch"),
            ("runtime", "architecture"),
            ("artifact", "arch"),
            ("model", "arch"),
            ("training", "arch"),
        )
        for path in candidates:
            value = ManifestLoader._text(ManifestLoader._nested_get(raw, *path))
            if value is not None:
                return value

        fmt = str(checkpoint_format or "").strip().lower()
        module_key = str(module_name or "").strip().lower()
        if module_key == "segmentation":
            if fmt == "onnx":
                return "onnx_segmentation"
            if fmt in {"pt", "pth", "torchscript"}:
                return "torchscript_segmentation"
        if module_key == "grading":
            if fmt == "onnx":
                return "onnx_classifier"
            if fmt in {"pt", "pth", "torchscript"}:
                return "torchscript_classifier"
        return None

    @staticmethod
    def _label_order(label_spec: Any) -> tuple[str, ...]:
        if not isinstance(label_spec, dict):
            return ()
        raw_order = label_spec.get("labelOrder")
        if not isinstance(raw_order, list):
            return ()
        values = [str(item).strip().upper() for item in raw_order if str(item).strip()]
        return tuple(values)

    @staticmethod
    def _parse_input_size(raw_size: Any) -> tuple[int, int] | None:
        if not isinstance(raw_size, list) or len(raw_size) != 2:
            return None
        try:
            width = int(raw_size[0])
            height = int(raw_size[1])
        except (TypeError, ValueError):
            return None
        if width <= 0 or height <= 0:
            return None
        return width, height

    @staticmethod
    def _parse_positive_int(value: Any) -> int | None:
        if value is None or value == "":
            return None
        try:
            parsed = int(value)
        except (TypeError, ValueError):
            return None
        return parsed if parsed > 0 else None

    @staticmethod
    def _text(value: Any) -> str | None:
        text = str(value or "").strip()
        return text or None

    @staticmethod
    def _nested_get(data: Any, *path: str) -> Any:
        current = data
        for key in path:
            if not isinstance(current, dict):
                return None
            current = current.get(key)
        return current

    @staticmethod
    def _sha256(path: Path) -> str:
        digest = hashlib.sha256()
        with path.open("rb") as file:
            for chunk in iter(lambda: file.read(65536), b""):
                digest.update(chunk)
        return digest.hexdigest()
