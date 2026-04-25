from __future__ import annotations

from typing import Any, Sequence

from app.core.exceptions import ModelRuntimeException
from app.infra.model.manifest_loader import ModuleManifest


class CheckpointValidator:
    def __init__(self, module_name: str) -> None:
        self._module_name = str(module_name or "").strip().lower()

    def validate_manifest_assets(
        self,
        manifest: ModuleManifest,
        *,
        class_map: dict[str, Any],
        preprocess: dict[str, Any],
        postprocess: dict[str, Any],
    ) -> None:
        if manifest.class_map_path is None:
            self._raise("validate", f"{self._module_name} manifest is missing dataset.classMapPath")
        if manifest.preprocess_path is None:
            self._raise("validate", f"{self._module_name} manifest is missing inputSpec.preprocessConfigPath")
        if manifest.postprocess_path is None:
            self._raise("validate", f"{self._module_name} manifest is missing outputSpec.postprocessConfigPath")
        if manifest.expected_input_size is None:
            self._raise("validate", f"{self._module_name} manifest is missing a valid expectedImageSize")
        if manifest.arch is None:
            self._raise("validate", f"{self._module_name} manifest is missing model arch metadata")
        if not isinstance(class_map, dict) or not class_map:
            self._raise("validate", f"{self._module_name} class map is empty or invalid")
        if not isinstance(preprocess, dict) or not preprocess:
            self._raise("validate", f"{self._module_name} preprocess config is empty or invalid")
        if not isinstance(postprocess, dict) or not postprocess:
            self._raise("validate", f"{self._module_name} postprocess config is empty or invalid")

        expected_classes = self.expected_num_classes(manifest, class_map)
        if expected_classes <= 0:
            self._raise("validate", f"{self._module_name} label map produced an invalid num_classes={expected_classes}")

        if manifest.expected_num_classes is not None and manifest.expected_num_classes != expected_classes:
            self._raise(
                "validate",
                f"{self._module_name} manifest num_classes mismatch: "
                f"manifest={manifest.expected_num_classes} labelMap={expected_classes}",
            )

        shared = preprocess.get("shared", {}) if isinstance(preprocess.get("shared"), dict) else {}
        preprocess_size = shared.get("imageSize")
        if isinstance(preprocess_size, list) and len(preprocess_size) == 2:
            try:
                parsed = (int(preprocess_size[0]), int(preprocess_size[1]))
            except (TypeError, ValueError):
                parsed = None
            if parsed is not None and manifest.expected_input_size is not None and parsed != manifest.expected_input_size:
                self._raise(
                    "validate",
                    f"{self._module_name} input size mismatch: manifest={manifest.expected_input_size} preprocess={parsed}",
                )

        if self._module_name == "grading" and manifest.label_order:
            normalized = tuple(str(item).strip().upper() for item in manifest.label_order if str(item).strip())
            class_map_order = tuple(
                str(item.get("labelCode")).strip().upper()
                for item in class_map.get("gradingLabels", [])
                if str(item.get("labelCode") or "").strip()
            )
            if class_map_order and normalized != class_map_order:
                self._raise(
                    "validate",
                    f"grading labelOrder mismatch: manifest={list(normalized)} classMap={list(class_map_order)}",
                )

    def validate_checkpoint_ready(self, manifest: ModuleManifest) -> None:
        if manifest.checkpoint_path is None:
            self._raise("load", f"{self._module_name} manifest is missing artifact.checkpointPath")
        if not manifest.checkpoint_exists or manifest.checkpoint_path is None:
            self._raise("load", f"{self._module_name} checkpoint does not exist: {manifest.checkpoint_path}")
        if str(manifest.status or "").strip().upper() in {"", "SPEC_ONLY", "MISSING_CHECKPOINT"}:
            self._raise("load", f"{self._module_name} manifest status is not runnable: {manifest.status}")
        if manifest.checkpoint_declared_sha256 and manifest.checkpoint_actual_sha256:
            if manifest.checkpoint_declared_sha256.lower() != manifest.checkpoint_actual_sha256.lower():
                self._raise(
                    "load",
                    f"{self._module_name} checkpoint sha256 mismatch: "
                    f"manifest={manifest.checkpoint_declared_sha256} actual={manifest.checkpoint_actual_sha256}",
                )

    def validate_model_shapes(
        self,
        manifest: ModuleManifest,
        *,
        class_map: dict[str, Any],
        input_shape: Sequence[Any] | None,
        output_shape: Sequence[Any] | None,
    ) -> None:
        expected_width, expected_height = manifest.expected_input_size or (None, None)
        if expected_width is None or expected_height is None:
            self._raise("validate", f"{self._module_name} expected input size is unavailable")

        if input_shape:
            input_width, input_height, channels = self._parse_input_shape(list(input_shape))
            if input_width is not None and input_height is not None:
                if (input_width, input_height) != (expected_width, expected_height):
                    self._raise(
                        "load",
                        f"{self._module_name} checkpoint input size mismatch: "
                        f"checkpoint={(input_width, input_height)} manifest={(expected_width, expected_height)}",
                    )
            expected_channels = self._expected_channels(manifest)
            if channels is not None and channels != expected_channels:
                self._raise(
                    "load",
                    f"{self._module_name} checkpoint input channel mismatch: checkpoint={channels} manifest={expected_channels}",
                )

        expected_classes = self.expected_num_classes(manifest, class_map)
        observed_classes = self._parse_output_classes(list(output_shape or []))
        if observed_classes is None:
            return
        if self._module_name == "segmentation":
            if observed_classes not in {1, expected_classes}:
                self._raise(
                    "load",
                    f"{self._module_name} checkpoint num_classes mismatch: checkpoint={observed_classes} manifest={expected_classes}",
                )
            return
        if observed_classes != expected_classes:
            self._raise(
                "load",
                f"{self._module_name} checkpoint num_classes mismatch: checkpoint={observed_classes} manifest={expected_classes}",
            )

    def expected_num_classes(self, manifest: ModuleManifest, class_map: dict[str, Any]) -> int:
        if self._module_name == "segmentation":
            classes = class_map.get("segmentationClasses", [])
            return len(classes) if isinstance(classes, list) else 0

        if manifest.label_order:
            return len(manifest.label_order)
        labels = class_map.get("gradingLabels", [])
        return len(labels) if isinstance(labels, list) else 0

    def _expected_channels(self, manifest: ModuleManifest) -> int:
        input_spec = manifest.raw.get("inputSpec", {})
        try:
            return max(1, int(input_spec.get("imageChannels") or 1))
        except (TypeError, ValueError):
            return 1

    @staticmethod
    def _parse_input_shape(shape: list[Any]) -> tuple[int | None, int | None, int | None]:
        if len(shape) != 4:
            return None, None, None

        if isinstance(shape[-1], int) and shape[-1] in {1, 3}:
            width = int(shape[2]) if isinstance(shape[2], int) and shape[2] > 0 else None
            height = int(shape[1]) if isinstance(shape[1], int) and shape[1] > 0 else None
            channels = int(shape[-1])
            return width, height, channels

        if isinstance(shape[1], int) and shape[1] in {1, 3}:
            width = int(shape[3]) if isinstance(shape[3], int) and shape[3] > 0 else None
            height = int(shape[2]) if isinstance(shape[2], int) and shape[2] > 0 else None
            channels = int(shape[1])
            return width, height, channels

        return None, None, None

    def _parse_output_classes(self, shape: list[Any]) -> int | None:
        if not shape:
            return None

        if self._module_name == "segmentation":
            if len(shape) == 4:
                if isinstance(shape[1], int) and shape[1] > 0:
                    return int(shape[1])
                if isinstance(shape[-1], int) and shape[-1] > 0:
                    return int(shape[-1])
            if len(shape) == 3:
                if isinstance(shape[0], int) and shape[0] <= 8:
                    return int(shape[0])
                if isinstance(shape[-1], int) and shape[-1] <= 8:
                    return int(shape[-1])
            if len(shape) == 2:
                return 1
            return None

        if len(shape) == 1 and isinstance(shape[0], int) and shape[0] > 0:
            return int(shape[0])
        if len(shape) >= 2:
            last = shape[-1]
            if isinstance(last, int) and last > 0:
                return int(last)
            second = shape[1]
            if isinstance(second, int) and second > 0:
                return int(second)
        return None

    def _raise(self, stage: str, message: str) -> None:
        raise ModelRuntimeException(
            self._module_name,
            stage,
            message,
        )
