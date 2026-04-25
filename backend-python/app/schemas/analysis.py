from __future__ import annotations

from typing import Any

from pydantic import Field

from app.schemas.base import CamelModel


class MissingRequirement(CamelModel):
    module_name: str
    requirement: str
    message: str
    expected_path: str | None = None
    actual_path: str | None = None
    candidates: list[str] = Field(default_factory=list)


class ModuleAssetStatus(CamelModel):
    module_name: str
    enabled: bool = False
    impl_type: str = "DISABLED"
    ready: bool = False
    mode: str = "disabled"
    model_code: str | None = None
    manifest_path: str | None = None
    checkpoint_path: str | None = None
    checkpoint_format: str | None = None
    class_map_path: str | None = None
    preprocess_path: str | None = None
    postprocess_path: str | None = None
    expected_input_size: list[int] | None = None
    normalization: dict[str, Any] = Field(default_factory=dict)
    postprocess: dict[str, Any] = Field(default_factory=dict)
    label_order: list[str] = Field(default_factory=list)
    discovered_candidates: list[str] = Field(default_factory=list)
    missing_items: list[MissingRequirement] = Field(default_factory=list)
    load_error: str | None = None


class ImageRuntimeRecord(CamelModel):
    image_id: int | None = None
    image_type_code: str | None = None
    source_format: str | None = None
    width_px: int
    height_px: int
    channels: int
    local_path: str
    bucket_name: str | None = None
    object_key: str | None = None


class RuntimeSnapshot(CamelModel):
    pipeline_version: str
    mode: str
    modules: dict[str, ModuleAssetStatus] = Field(default_factory=dict)
    missing_items: list[MissingRequirement] = Field(default_factory=list)
