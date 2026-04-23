from __future__ import annotations

from app.core.config import Settings
from app.core.logging import get_logger
from app.infra.model.model_registry import ModelRegistry
from app.repositories.governance_repository import GovernanceRepository

log = get_logger("cariesguard-ai.governance")


class GovernanceBootstrapService:
    """Registers active runtime adapters into the governance registry."""

    def __init__(
        self,
        settings: Settings,
        registry: ModelRegistry,
        repository: GovernanceRepository,
    ) -> None:
        self._settings = settings
        self._registry = registry
        self._repository = repository

    def bootstrap(self) -> None:
        try:
            rows = self._register_runtime_components()
            log.info("governance bootstrap completed modelVersionRows=%s", rows)
        except Exception as exc:
            log.warning("governance bootstrap skipped error=%s", exc)

    def _register_runtime_components(self) -> int:
        count = 0
        status = self._registry.status()
        for adapter in status.get("adapters", {}).values():
            self._repository.ensure_model_version(
                model_code=adapter["modelCode"],
                model_name=f"{adapter['modelTypeCode']} {adapter['implType']}",
                model_type_code=adapter["modelTypeCode"],
                version_no=self._settings.model_version,
                artifact_path=self._settings.model_weights_dir,
                dataset_version=None,
                status_code="APPROVED" if adapter.get("loaded") else "CANDIDATE",
                active_flag="1" if adapter.get("loaded") else "0",
                metrics_json={
                    "runtimeMode": status.get("aiRuntimeMode"),
                    "implType": adapter["implType"],
                    "loaded": adapter.get("loaded", False),
                    "source": "runtime-adapter-bootstrap",
                },
            )
            count += 1
        return count
