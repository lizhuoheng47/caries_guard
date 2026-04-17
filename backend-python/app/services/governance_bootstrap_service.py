from __future__ import annotations

from typing import Any

from app.core.config import Settings
from app.core.logging import get_logger
from app.infra.model.model_registry import ModelRegistry
from app.repositories.governance_repository import GovernanceRepository

log = get_logger("cariesguard-ai.governance")


class GovernanceBootstrapService:
    """Registers active runtime adapters and knowledge/LLM components."""

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
                dataset_version=self._settings.rag_knowledge_version,
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

        for component in self._knowledge_components():
            self._repository.ensure_model_version(**component)
            count += 1
        return count

    def _knowledge_components(self) -> list[dict[str, Any]]:
        llm_code = self._safe_code(f"llm-{self._settings.llm_provider_code}-{self._settings.llm_model_name}")
        rag_code = self._safe_code(f"rag-kb-{self._settings.rag_default_kb_code}")
        return [
            {
                "model_code": llm_code,
                "model_name": f"General LLM {self._settings.llm_model_name}",
                "model_type_code": "LLM",
                "version_no": self._settings.model_version,
                "artifact_path": None,
                "dataset_version": self._settings.rag_knowledge_version,
                "status_code": "APPROVED",
                "active_flag": "1",
                "metrics_json": {
                    "route": "GENERAL_LLM_WITH_KNOWLEDGE_BASE",
                    "providerCode": self._settings.llm_provider_code,
                    "modelName": self._settings.llm_model_name,
                    "fineTuned": False,
                },
            },
            {
                "model_code": rag_code,
                "model_name": self._settings.rag_default_kb_name,
                "model_type_code": "RAG",
                "version_no": self._settings.rag_knowledge_version,
                "artifact_path": self._settings.rag_index_dir,
                "dataset_version": self._settings.rag_knowledge_version,
                "status_code": "APPROVED",
                "active_flag": "1",
                "metrics_json": {
                    "route": "GENERAL_LLM_WITH_KNOWLEDGE_BASE",
                    "embeddingModel": self._settings.rag_embedding_model,
                    "vectorStoreType": self._settings.rag_vector_store_type,
                    "defaultTopK": self._settings.rag_top_k,
                },
            },
        ]

    @staticmethod
    def _safe_code(value: str) -> str:
        code = value.strip().lower().replace(" ", "-").replace("/", "-")
        return code[:64]
