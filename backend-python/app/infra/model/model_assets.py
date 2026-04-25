from __future__ import annotations

import copy
import json
from dataclasses import replace
from pathlib import Path
from typing import Any

import yaml

from app.infra.model.manifest_loader import ManifestLoader, ModuleManifest


class ModelAssets:
    def __init__(self, settings: Any, manifest_loader: ManifestLoader | None = None) -> None:
        self._project_root = Path(__file__).resolve().parents[3]
        self._manifest_loader = manifest_loader or ManifestLoader(self._project_root)
        self._segmentation_manifest = self._manifest_loader.load(
            "segmentation",
            getattr(settings, "model_segmentation_manifest_path", "assets/models/manifests/segmentation_v1.yaml"),
        )
        self._grading_manifest = self._manifest_loader.load(
            "grading",
            getattr(settings, "model_grading_manifest_path", "assets/models/manifests/grading_v1.yaml"),
        )

        class_map_path = self._segmentation_manifest.class_map_path or self._grading_manifest.class_map_path
        if class_map_path is None:
            raise ValueError("model manifests must declare dataset.classMapPath")
        self._class_map_path = class_map_path
        self._class_map = self._load_json(class_map_path)

        self._segmentation_manifest = self._with_class_map_defaults(self._segmentation_manifest)
        self._grading_manifest = self._with_class_map_defaults(self._grading_manifest)

        self._preprocess_path = self._segmentation_manifest.preprocess_path or self._grading_manifest.preprocess_path
        self._postprocess_path = self._segmentation_manifest.postprocess_path or self._grading_manifest.postprocess_path
        if self._preprocess_path is None or self._postprocess_path is None:
            raise ValueError("model manifests must declare preprocess and postprocess paths")
        self._preprocess = self._load_yaml(self._preprocess_path)
        self._postprocess = self._load_yaml(self._postprocess_path)

        self._grading_labels = list(self._grading_manifest.label_order)
        if not self._grading_labels:
            self._grading_labels = [
                str(item.get("labelCode")).strip().upper()
                for item in self._class_map.get("gradingLabels", [])
                if str(item.get("labelCode") or "").strip()
            ]
        if not self._grading_labels:
            raise ValueError("class_map.json is missing gradingLabels")
        self._grading_order = {label: index for index, label in enumerate(self._grading_labels)}

    @property
    def class_map_path(self) -> Path:
        return self._class_map_path

    @property
    def preprocess_path(self) -> Path:
        return self._preprocess_path

    @property
    def postprocess_path(self) -> Path:
        return self._postprocess_path

    @property
    def segmentation_manifest(self) -> ModuleManifest:
        return self._segmentation_manifest

    @property
    def grading_manifest(self) -> ModuleManifest:
        return self._grading_manifest

    def grading_labels(self) -> list[str]:
        return list(self._grading_labels)

    def class_map(self) -> dict[str, Any]:
        return copy.deepcopy(self._class_map)

    def preprocess_config(self) -> dict[str, Any]:
        return copy.deepcopy(self._preprocess)

    def postprocess_config(self) -> dict[str, Any]:
        return copy.deepcopy(self._postprocess)

    def segmentation_foreground_class_id(self, default: int = 1) -> int:
        output_spec = self._segmentation_manifest.raw.get("outputSpec", {})
        class_code = str(output_spec.get("foregroundClassCode") or "CARIES_LESION").strip().upper()
        for item in self._class_map.get("segmentationClasses", []):
            if str(item.get("classCode") or "").strip().upper() == class_code:
                try:
                    return int(item.get("classId"))
                except (TypeError, ValueError):
                    return int(default)
        return int(default)

    def segmentation_mask_threshold(self, default: float = 0.5) -> float:
        segmentation = self._postprocess.get("segmentation", {})
        try:
            return float(segmentation.get("maskThreshold", default))
        except (TypeError, ValueError):
            return float(default)

    def segmentation_connected_components_enabled(self, default: bool = True) -> bool:
        segmentation = self._postprocess.get("segmentation", {})
        value = segmentation.get("connectedComponents", default)
        if isinstance(value, bool):
            return value
        return str(value).strip().lower() in {"1", "true", "yes", "y", "on"}

    def normalize_grading_label(self, value: Any) -> str:
        label = str(value or "").strip().upper()
        if label not in self._grading_order:
            raise ValueError(f"invalid grading label: {value!r}")
        return label

    def is_valid_grading_label(self, value: Any) -> bool:
        return str(value or "").strip().upper() in self._grading_order

    def severity_rank(self, value: Any) -> int:
        return self._grading_order.get(str(value or "").strip().upper(), -1)

    def uncertainty_review_threshold(self, default: float) -> float:
        grading = self._postprocess.get("grading", {})
        try:
            if "uncertaintyReviewThreshold" in grading:
                return float(grading.get("uncertaintyReviewThreshold", default))
            uncertainty = grading.get("uncertainty", {})
            return float(uncertainty.get("reviewThreshold", default))
        except (TypeError, ValueError):
            return float(default)

    def segmentation_min_region_area(self, default: int = 0) -> int:
        segmentation = self._postprocess.get("segmentation", {})
        try:
            if "minRegionAreaPx" in segmentation:
                return int(segmentation.get("minRegionAreaPx", default))
            return int(segmentation.get("minRegionArea", default))
        except (TypeError, ValueError):
            return int(default)

    def module_descriptor(self, module_name: str) -> dict[str, Any]:
        if module_name == "segmentation":
            return self._manifest_descriptor(self._segmentation_manifest)
        if module_name == "grading":
            descriptor = self._manifest_descriptor(self._grading_manifest)
            descriptor["labelOrder"] = self.grading_labels()
            return descriptor
        return {
            "manifestPath": None,
            "manifestStatus": None,
            "checkpointPath": None,
            "checkpointExists": False,
            "checkpointSha256": None,
            "checkpointFormat": None,
            "releasedAt": None,
        }

    def _manifest_descriptor(self, manifest: ModuleManifest) -> dict[str, Any]:
        return {
            "manifestPath": str(manifest.manifest_path),
            "manifestStatus": manifest.status,
            "datasetCode": manifest.dataset_code,
            "datasetVersion": manifest.dataset_version,
            "arch": manifest.arch,
            "numClasses": manifest.expected_num_classes,
            "checkpointPath": str(manifest.checkpoint_path) if manifest.checkpoint_path is not None else None,
            "checkpointExists": manifest.checkpoint_exists,
            "checkpointSha256": manifest.checkpoint_sha256,
            "checkpointDeclaredSha256": manifest.checkpoint_declared_sha256,
            "checkpointActualSha256": manifest.checkpoint_actual_sha256,
            "checkpointFormat": manifest.checkpoint_format,
            "releasedAt": manifest.exported_at,
            "modelCode": manifest.model_code,
            "taskType": manifest.task_type,
            "classMapPath": str(manifest.class_map_path) if manifest.class_map_path is not None else None,
            "preprocessPath": str(manifest.preprocess_path) if manifest.preprocess_path is not None else None,
            "postprocessPath": str(manifest.postprocess_path) if manifest.postprocess_path is not None else None,
            "expectedInputSize": list(manifest.expected_input_size) if manifest.expected_input_size is not None else None,
        }

    def _with_class_map_defaults(self, manifest: ModuleManifest) -> ModuleManifest:
        if manifest.module_name == "segmentation":
            classes = self._class_map.get("segmentationClasses", [])
            expected_num_classes = len(classes) if isinstance(classes, list) else manifest.expected_num_classes
            return replace(manifest, expected_num_classes=expected_num_classes)

        label_order = manifest.label_order
        if not label_order:
            label_order = tuple(
                str(item.get("labelCode")).strip().upper()
                for item in self._class_map.get("gradingLabels", [])
                if str(item.get("labelCode") or "").strip()
            )
        expected_num_classes = manifest.expected_num_classes or (len(label_order) if label_order else None)
        return replace(manifest, label_order=label_order, expected_num_classes=expected_num_classes)

    @staticmethod
    def _load_yaml(path: Path) -> dict[str, Any]:
        with path.open("r", encoding="utf-8") as file:
            loaded = yaml.safe_load(file)
        if not isinstance(loaded, dict):
            raise ValueError(f"YAML config must be a mapping: {path}")
        return loaded

    @staticmethod
    def _load_json(path: Path) -> dict[str, Any]:
        with path.open("r", encoding="utf-8") as file:
            loaded = json.load(file)
        if not isinstance(loaded, dict):
            raise ValueError(f"JSON config must be a mapping: {path}")
        return loaded
