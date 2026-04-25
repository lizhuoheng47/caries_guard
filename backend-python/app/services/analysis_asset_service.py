from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import yaml

from app.core.config import Settings
from app.core.exceptions import AnalysisRuntimeException
from app.infra.model.model_assets import ModelAssets
from app.schemas.analysis import MissingRequirement, ModuleAssetStatus, RuntimeSnapshot
from app.schemas.base import dump_camel


class AnalysisAssetService:
    def __init__(self, settings: Settings, model_assets: ModelAssets) -> None:
        self._settings = settings
        self._model_assets = model_assets
        self._project_root = Path(__file__).resolve().parents[2]
        self._repo_root = Path(__file__).resolve().parents[3]

    def runtime_snapshot(self, registry: Any | None = None) -> RuntimeSnapshot:
        modules = {
            "quality": self._quality_status(registry),
            "tooth_detect": self._tooth_detection_status(registry),
            "segmentation": self._manifest_status("segmentation", registry),
            "grading": self._manifest_status("grading", registry),
        }
        missing_items: list[MissingRequirement] = []
        for status in modules.values():
            missing_items.extend(status.missing_items)
        return RuntimeSnapshot(
            pipeline_version="analysis-v2",
            mode=self._settings.ai_runtime_mode,
            modules=modules,
            missing_items=missing_items,
        )

    def require_ready_modules(self, modules: list[str], registry: Any | None = None) -> RuntimeSnapshot:
        snapshot = self.runtime_snapshot(registry)
        missing_items: list[dict[str, Any]] = []
        module_payloads: dict[str, Any] = {}
        for name in modules:
            status = snapshot.modules.get(name)
            if status is None:
                missing_items.append(
                    dump_camel(
                        MissingRequirement(
                            module_name=name,
                            requirement="module",
                            message=f"unsupported analysis module: {name}",
                        )
                    )
                )
                continue
            module_payloads[name] = dump_camel(status)
            if not status.enabled:
                missing_items.append(
                    dump_camel(
                        MissingRequirement(
                            module_name=name,
                            requirement="enabled",
                            message=f"{name} module is disabled in runtime configuration",
                        )
                    )
                )
            for item in status.missing_items:
                missing_items.append(dump_camel(item))
            if not status.ready and status.load_error:
                missing_items.append(
                    dump_camel(
                        MissingRequirement(
                            module_name=name,
                            requirement="adapterLoad",
                            message=status.load_error,
                        )
                    )
                )
            elif status.enabled and not status.ready and not status.missing_items:
                missing_items.append(
                    dump_camel(
                        MissingRequirement(
                            module_name=name,
                            requirement="ready",
                            message=f"{name} module is not ready for real inference",
                        )
                    )
                )
        if missing_items:
            raise AnalysisRuntimeException(
                "M5101",
                "real inference assets are not ready",
                missing_items=missing_items,
                details={
                    "pipelineVersion": snapshot.pipeline_version,
                    "mode": snapshot.mode,
                    "modules": module_payloads,
                },
            )
        return snapshot

    def module_payload(self, registry: Any | None = None) -> dict[str, Any]:
        snapshot = self.runtime_snapshot(registry)
        return {name: dump_camel(status) for name, status in snapshot.modules.items()}

    def _quality_status(self, registry: Any | None) -> ModuleAssetStatus:
        enabled = self._settings.model_quality_enabled
        impl_type = self._settings.model_quality_impl_type if enabled else "DISABLED"
        candidates = self._quality_candidates()
        chosen = next((item for item in candidates if item.is_file()), None)
        missing_items: list[MissingRequirement] = []
        normalization: dict[str, Any] = {}
        expected_input_size: list[int] | None = None
        model_code = "quality-assessment-ml-v1"
        if enabled and chosen is None:
            missing_items.append(
                MissingRequirement(
                    module_name="quality",
                    requirement="modelParams",
                    message="quality model params json is missing",
                    expected_path=str(candidates[0]) if candidates else None,
                    candidates=[str(item) for item in candidates],
                )
            )
        if chosen is not None:
            loaded = self._load_json(chosen)
            model_code = str(loaded.get("modelCode") or model_code)
            expected_input_size = self._parse_size(
                loaded.get("expectedImageSize")
                or loaded.get("inputSize")
                or loaded.get("imageSize")
            )
            normalization = self._extract_normalization(loaded)
            issue_models = loaded.get("issueModels")
            if enabled and (not isinstance(issue_models, dict) or not issue_models):
                missing_items.append(
                    MissingRequirement(
                        module_name="quality",
                        requirement="issueModels",
                        message="quality model params json is missing issueModels metadata",
                        actual_path=str(chosen),
                    )
                )
        status = ModuleAssetStatus(
            module_name="quality",
            enabled=enabled,
            impl_type=impl_type,
            ready=enabled and not missing_items and self._loaded(registry, "quality"),
            mode="real" if enabled else "disabled",
            model_code=model_code,
            checkpoint_path=str(chosen) if chosen is not None else None,
            checkpoint_format="json" if chosen is not None else None,
            expected_input_size=expected_input_size,
            normalization=normalization,
            discovered_candidates=[str(item) for item in candidates],
            missing_items=missing_items,
            load_error=self._load_error(registry, "quality"),
        )
        if not enabled:
            status.ready = False
        elif not missing_items and registry is None:
            status.ready = chosen is not None
        return status

    def _tooth_detection_status(self, registry: Any | None) -> ModuleAssetStatus:
        enabled = self._settings.model_tooth_detect_enabled
        impl_type = self._settings.model_tooth_detect_impl_type if enabled else "DISABLED"
        checkpoint_candidates = self._tooth_checkpoint_candidates()
        checkpoint = next((item for item in checkpoint_candidates if item.is_file()), None)
        config_candidates = self._tooth_config_candidates(checkpoint)
        config = next((item for item in config_candidates if item.is_file()), None)
        metadata = self._load_mapping(config) if config is not None else {}
        label_order = self._label_order(metadata)
        expected_input_size = self._parse_size(
            metadata.get("expectedImageSize")
            or metadata.get("inputSize")
            or metadata.get("imageSize")
            or metadata.get("imgsz")
            or metadata.get("imgSize")
            or self._nested(metadata, "inputSpec", "expectedImageSize")
        )
        missing_items: list[MissingRequirement] = []
        if enabled and checkpoint is None:
            missing_items.append(
                MissingRequirement(
                    module_name="tooth_detect",
                    requirement="checkpoint",
                    message="tooth detection checkpoint is missing",
                    expected_path=str(checkpoint_candidates[0]) if checkpoint_candidates else None,
                    candidates=[str(item) for item in checkpoint_candidates],
                )
            )
        if enabled and config is None:
            missing_items.append(
                MissingRequirement(
                    module_name="tooth_detect",
                    requirement="config",
                    message="tooth detection metadata/config is missing",
                    candidates=[str(item) for item in config_candidates],
                )
            )
        if enabled and not label_order:
            missing_items.append(
                MissingRequirement(
                    module_name="tooth_detect",
                    requirement="labelOrder",
                    message="tooth detection metadata is missing label mapping / FDI codes",
                    actual_path=str(config) if config is not None else None,
                )
            )
        if enabled and expected_input_size is None:
            missing_items.append(
                MissingRequirement(
                    module_name="tooth_detect",
                    requirement="expectedInputSize",
                    message="tooth detection metadata is missing expected input size",
                    actual_path=str(config) if config is not None else None,
                )
            )
        status = ModuleAssetStatus(
            module_name="tooth_detect",
            enabled=enabled,
            impl_type=impl_type,
            ready=enabled and not missing_items and self._loaded(registry, "tooth_detect"),
            mode="real" if enabled else "disabled",
            model_code="tooth-detect-yolo-v8",
            checkpoint_path=str(checkpoint) if checkpoint is not None else None,
            checkpoint_format=checkpoint.suffix.lstrip(".").lower() if checkpoint is not None else None,
            preprocess_path=str(config) if config is not None else None,
            expected_input_size=expected_input_size,
            normalization=self._extract_normalization(metadata),
            postprocess=self._extract_postprocess(metadata),
            label_order=label_order,
            discovered_candidates=[str(item) for item in checkpoint_candidates],
            missing_items=missing_items,
            load_error=self._load_error(registry, "tooth_detect"),
        )
        if enabled and not missing_items and registry is None:
            status.ready = checkpoint is not None and config is not None
        return status

    def _manifest_status(self, module_name: str, registry: Any | None) -> ModuleAssetStatus:
        manifest = self._model_assets.segmentation_manifest if module_name == "segmentation" else self._model_assets.grading_manifest
        preprocess = self._model_assets.preprocess_config()
        postprocess = self._model_assets.postprocess_config()
        enabled = getattr(self._settings, f"model_{module_name}_enabled")
        impl_type = getattr(self._settings, f"model_{module_name}_impl_type") if enabled else "DISABLED"
        missing_items: list[MissingRequirement] = []
        if enabled and not manifest.manifest_path.is_file():
            missing_items.append(
                MissingRequirement(
                    module_name=module_name,
                    requirement="manifest",
                    message=f"{module_name} manifest is missing",
                    expected_path=str(manifest.manifest_path),
                )
            )
        if enabled and manifest.checkpoint_path is None:
            missing_items.append(
                MissingRequirement(
                    module_name=module_name,
                    requirement="checkpointPath",
                    message=f"{module_name} manifest is missing artifact.checkpointPath",
                    actual_path=str(manifest.manifest_path),
                )
            )
        if enabled and not manifest.checkpoint_exists:
            missing_items.append(
                MissingRequirement(
                    module_name=module_name,
                    requirement="checkpoint",
                    message=f"{module_name} checkpoint is missing",
                    expected_path=str(manifest.checkpoint_path) if manifest.checkpoint_path is not None else None,
                )
            )
        if enabled and str(manifest.status or "").strip().upper() in {"", "SPEC_ONLY", "MISSING_CHECKPOINT"}:
            missing_items.append(
                MissingRequirement(
                    module_name=module_name,
                    requirement="manifestStatus",
                    message=f"{module_name} manifest status is not runnable: {manifest.status}",
                    actual_path=str(manifest.manifest_path),
                )
            )
        if enabled and manifest.class_map_path is None:
            missing_items.append(
                MissingRequirement(
                    module_name=module_name,
                    requirement="classMapPath",
                    message=f"{module_name} manifest is missing classMapPath",
                    actual_path=str(manifest.manifest_path),
                )
            )
        if enabled and manifest.preprocess_path is None:
            missing_items.append(
                MissingRequirement(
                    module_name=module_name,
                    requirement="preprocessConfigPath",
                    message=f"{module_name} manifest is missing preprocessConfigPath",
                    actual_path=str(manifest.manifest_path),
                )
            )
        if enabled and manifest.postprocess_path is None:
            missing_items.append(
                MissingRequirement(
                    module_name=module_name,
                    requirement="postprocessConfigPath",
                    message=f"{module_name} manifest is missing postprocessConfigPath",
                    actual_path=str(manifest.manifest_path),
                )
            )
        status = ModuleAssetStatus(
            module_name=module_name,
            enabled=enabled,
            impl_type=impl_type,
            ready=enabled and not missing_items and self._loaded(registry, module_name),
            mode="real" if enabled else "disabled",
            model_code=manifest.model_code,
            manifest_path=str(manifest.manifest_path),
            checkpoint_path=str(manifest.checkpoint_path) if manifest.checkpoint_path is not None else None,
            checkpoint_format=manifest.checkpoint_format,
            class_map_path=str(manifest.class_map_path) if manifest.class_map_path is not None else None,
            preprocess_path=str(manifest.preprocess_path) if manifest.preprocess_path is not None else None,
            postprocess_path=str(manifest.postprocess_path) if manifest.postprocess_path is not None else None,
            expected_input_size=list(manifest.expected_input_size) if manifest.expected_input_size is not None else None,
            normalization=self._extract_normalization(preprocess),
            postprocess=postprocess.get(module_name, {}) if isinstance(postprocess.get(module_name), dict) else {},
            label_order=self._model_assets.grading_labels() if module_name == "grading" else [],
            missing_items=missing_items,
            load_error=self._load_error(registry, module_name),
        )
        if enabled and not missing_items and registry is None:
            status.ready = manifest.checkpoint_exists
        return status

    def _quality_candidates(self) -> list[Path]:
        candidates: list[Path] = []
        for raw in (
            getattr(self._settings, "quality_model_weights_path", ""),
            getattr(self._settings, "quality_model_param_path", ""),
            Path(getattr(self._settings, "model_weights_dir", "")) / "quality" / "quality_model_params.json",
            self._repo_root / "model-weights" / "quality" / "quality_model_params.json",
            self._project_root / "app" / "quality" / "quality_model_params.json",
        ):
            if not raw:
                continue
            path = Path(raw) if isinstance(raw, Path) else Path(str(raw))
            candidates.append(self._resolve_path(path))
        return self._dedupe_paths(candidates)

    def _tooth_checkpoint_candidates(self) -> list[Path]:
        configured = getattr(self._settings, "model_tooth_detect_checkpoint_path", "")
        candidates: list[Path] = []
        if configured:
            candidates.append(self._resolve_path(Path(configured)))
        search_roots = [
            self._repo_root / "model-weights",
            self._project_root / "assets",
            self._project_root / "training" / "outputs",
        ]
        patterns = (
            "*tooth*.onnx",
            "*tooth*.pt",
            "*detect*.onnx",
            "*detect*.pt",
            "*yolo*.onnx",
            "*yolo*.pt",
        )
        for root in search_roots:
            if not root.exists():
                continue
            for pattern in patterns:
                candidates.extend(root.rglob(pattern))
        return self._dedupe_paths(candidates)

    def _tooth_config_candidates(self, checkpoint: Path | None) -> list[Path]:
        candidates: list[Path] = []
        configured = getattr(self._settings, "model_tooth_detect_config_path", "")
        if configured:
            candidates.append(self._resolve_path(Path(configured)))
        if checkpoint is not None:
            for sibling in checkpoint.parent.glob("*.json"):
                candidates.append(sibling)
            for sibling in checkpoint.parent.glob("*.yaml"):
                candidates.append(sibling)
            for sibling in checkpoint.parent.glob("*.yml"):
                candidates.append(sibling)
        return self._dedupe_paths(candidates)

    def _resolve_path(self, path: Path) -> Path:
        normalized = path.as_posix()
        if normalized.startswith("/app/"):
            return (self._project_root / normalized.removeprefix("/app/")).resolve()
        if path.is_absolute():
            return path
        return (self._repo_root / path).resolve()

    @staticmethod
    def _dedupe_paths(paths: list[Path]) -> list[Path]:
        seen: set[str] = set()
        deduped: list[Path] = []
        for item in paths:
            key = str(item.resolve() if item.exists() else item)
            if key in seen:
                continue
            seen.add(key)
            deduped.append(item)
        return deduped

    @staticmethod
    def _load_json(path: Path) -> dict[str, Any]:
        with path.open("r", encoding="utf-8") as fp:
            loaded = json.load(fp)
        if not isinstance(loaded, dict):
            raise RuntimeError(f"json config must be an object: {path}")
        return loaded

    def _load_mapping(self, path: Path | None) -> dict[str, Any]:
        if path is None or not path.is_file():
            return {}
        return self._load_json(path) if path.suffix.lower() == ".json" else self._load_yaml(path)

    @staticmethod
    def _load_yaml(path: Path) -> dict[str, Any]:
        with path.open("r", encoding="utf-8") as fp:
            loaded = yaml.safe_load(fp)
        if not isinstance(loaded, dict):
            raise RuntimeError(f"yaml config must be a mapping: {path}")
        return loaded

    @staticmethod
    def _parse_size(value: Any) -> list[int] | None:
        if isinstance(value, (list, tuple)) and len(value) == 2:
            try:
                return [int(value[0]), int(value[1])]
            except (TypeError, ValueError):
                return None
        return None

    @staticmethod
    def _nested(mapping: dict[str, Any], *keys: str) -> Any:
        current: Any = mapping
        for key in keys:
            if not isinstance(current, dict):
                return None
            current = current.get(key)
        return current

    @staticmethod
    def _extract_normalization(mapping: dict[str, Any]) -> dict[str, Any]:
        shared = mapping.get("shared") if isinstance(mapping.get("shared"), dict) else mapping
        normalize = shared.get("normalize") if isinstance(shared, dict) and isinstance(shared.get("normalize"), dict) else {}
        clip = shared.get("valueClip") if isinstance(shared, dict) and isinstance(shared.get("valueClip"), dict) else {}
        result: dict[str, Any] = {}
        if normalize:
            result["mode"] = normalize.get("mode")
            result["mean"] = normalize.get("mean")
            result["std"] = normalize.get("std")
        if clip:
            result["valueClip"] = clip
        return result

    @staticmethod
    def _extract_postprocess(mapping: dict[str, Any]) -> dict[str, Any]:
        result: dict[str, Any] = {}
        for key in ("confidenceThreshold", "scoreThreshold", "nmsThreshold", "iouThreshold"):
            if key in mapping:
                result[key] = mapping.get(key)
        postprocess = mapping.get("postprocess")
        if isinstance(postprocess, dict):
            result.update(postprocess)
        return result

    @staticmethod
    def _label_order(mapping: dict[str, Any]) -> list[str]:
        for key in ("labelOrder", "classNames", "classLabels", "toothCodes", "fdiCodes"):
            value = mapping.get(key)
            if isinstance(value, list):
                return [str(item).strip() for item in value if str(item).strip()]
        class_map = mapping.get("classMap")
        if isinstance(class_map, dict):
            return [str(key).strip() for key in class_map.keys() if str(key).strip()]
        return []

    @staticmethod
    def _loaded(registry: Any | None, module_name: str) -> bool:
        if registry is None:
            return False
        try:
            return bool(registry.is_module_loaded(module_name))
        except AttributeError:
            return bool(registry.is_module_real(module_name))

    @staticmethod
    def _load_error(registry: Any | None, module_name: str) -> str | None:
        if registry is None:
            return None
        try:
            return registry.get_module_error(module_name)
        except AttributeError:
            return None
