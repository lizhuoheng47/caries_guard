from app.core.config import Settings
from app.infra.model.model_registry import ModelRegistry
from app.services.governance_bootstrap_service import GovernanceBootstrapService


class FakeGovernanceRepository:
    def __init__(self):
        self.rows = []

    def ensure_model_version(self, **fields):
        self.rows.append(fields)
        return {"id": len(self.rows), **fields}


def test_governance_bootstrap_registers_adapter_llm_and_rag():
    settings = Settings(
        ai_runtime_mode="hybrid",
        model_risk_enabled=True,
        model_version="caries-v-test",
        rag_knowledge_version="kb-v-test",
        llm_provider_code="MOCK",
        llm_model_name="template-llm-v1",
    )
    registry = ModelRegistry(settings)
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
