from pathlib import Path

from app.core.config import Settings
from app.infra.model.model_assets import ModelAssets
from app.infra.model.model_registry import ModelRegistry
from app.services.governance_bootstrap_service import GovernanceBootstrapService


class FakeGovernanceRepository:
    def __init__(self):
        self.rows = []

    def ensure_model_version(self, **fields):
        self.rows.append(fields)
        return {"id": len(self.rows), **fields}


def _settings(**overrides) -> Settings:
    repo_root = Path(__file__).resolve().parents[3]
    values = {
        "ai_runtime_mode": "hybrid",
        "rag_runtime_enabled": False,
        "analysis_kb_enhancement_enabled": False,
        "model_quality_enabled": False,
        "model_tooth_detect_enabled": False,
        "model_segmentation_enabled": False,
        "model_grading_enabled": False,
        "model_risk_enabled": True,
        "model_quality_impl_type": "HEURISTIC",
        "model_tooth_detect_impl_type": "HEURISTIC",
        "model_segmentation_impl_type": "HEURISTIC",
        "model_grading_impl_type": "HEURISTIC",
        "model_risk_impl_type": "HEURISTIC",
        "model_weights_dir": str(repo_root / "model-weights"),
        "quality_model_param_path": str(repo_root / "model-weights" / "quality" / "quality_model_params.json"),
        "quality_model_weights_path": str(repo_root / "model-weights" / "quality" / "quality_model_params.json"),
        "model_version": "caries-v-test",
        "rag_knowledge_version": "kb-v-test",
        "rag_default_kb_code": "caries-default",
        "llm_provider_code": "MOCK",
        "llm_model_name": "template-llm-v1",
    }
    values.update(overrides)
    return Settings(**values)


def test_governance_bootstrap_registers_runtime_and_knowledge_components():
    settings = _settings()
    registry = ModelRegistry(settings, ModelAssets(settings))
    registry.startup()
    repo = FakeGovernanceRepository()

    GovernanceBootstrapService(settings, registry, repo).bootstrap()

    model_codes = {row["model_code"] for row in repo.rows}
    assert "risk-fusion-heuristic-v1" in model_codes
    assert "llm-mock-template-llm-v1" in model_codes
    assert "rag-kb-caries-default" in model_codes
    risk_row = next(row for row in repo.rows if row["model_code"] == "risk-fusion-heuristic-v1")
    assert risk_row["model_type_code"] == "RISK"
    assert risk_row["status_code"] == "APPROVED"
