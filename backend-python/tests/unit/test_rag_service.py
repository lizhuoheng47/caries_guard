from __future__ import annotations

import json

from app.core.config import Settings
from app.schemas.request import AnalyzeRequest, PatientProfile
from app.services.knowledge_base_service import KnowledgeBaseService
from app.services.rag_service import RagService


def _knowledge_file(tmp_path):
    path = tmp_path / "knowledge.json"
    path.write_text(
        json.dumps(
            {
                "knowledgeVersion": "test-v1",
                "documents": [
                    {
                        "docNo": "DOC-RISK",
                        "title": "高风险管理",
                        "tags": ["龋齿", "高风险", "随访"],
                        "chunks": [{"text": "高风险患者应缩短复查间隔并加强含氟防护。"}],
                    },
                    {
                        "docNo": "DOC-IMAGE",
                        "title": "影像质量",
                        "tags": ["模糊", "曝光"],
                        "chunks": [{"text": "影像模糊时需要人工确认是否足以支持临床判断。"}],
                    },
                ],
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )
    return path


def test_local_retrieval_is_versioned_and_ranked(tmp_path):
    settings = Settings(rag_knowledge_path=str(_knowledge_file(tmp_path)), rag_top_k=1)
    service = KnowledgeBaseService(settings)

    hits = service.search("龋齿高风险随访", top_k=1)

    assert service.version == "test-v1"
    assert len(hits) == 1
    assert hits[0].chunk.doc_no == "DOC-RISK"
    assert service.status()["ready"] is True


def test_rag_enrichment_returns_grounded_citations_and_safe_plan(tmp_path):
    settings = Settings(rag_knowledge_path=str(_knowledge_file(tmp_path)), rag_top_k=2, rag_llm_enabled=False)
    knowledge = KnowledgeBaseService(settings)
    service = RagService(settings, knowledge)
    task = AnalyzeRequest(
        task_no="TASK-1",
        patient_profile=PatientProfile(previous_caries_count=3, sugar_diet_level_code="HIGH"),
    )

    result = service.enrich(task, {"riskFactors": ["高糖频率"]}, severity_code="C3", risk_level="HIGH")

    assert result.status == "READY"
    assert result.generator == "LOCAL_TEMPLATE"
    assert result.knowledge_version == "test-v1"
    assert result.citations
    assert result.evidence_refs[0]["refType"] == "KNOWLEDGE"
    assert result.treatment_plan[0]["priority"] == "HIGH"
    assert "最终诊断" in (result.clinical_summary or "")


def test_reload_rejects_invalid_knowledge_without_replacing_snapshot(tmp_path):
    path = _knowledge_file(tmp_path)
    settings = Settings(rag_knowledge_path=str(path))
    service = KnowledgeBaseService(settings)
    path.write_text('{"knowledgeVersion":"broken","documents":[]}', encoding="utf-8")

    try:
        service.reload()
    except ValueError:
        pass
    else:
        raise AssertionError("invalid knowledge must be rejected")

    assert service.version == "test-v1"
    assert service.status()["ready"] is True
    assert service.status()["error"]
