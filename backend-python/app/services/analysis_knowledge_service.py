from __future__ import annotations

from typing import TYPE_CHECKING, Any

from app.core.config import Settings
from app.schemas.rag import RagAskRequest
from app.schemas.request import AnalyzeRequest
from app.services.qwen_vision_service import VisionAnalysisResult

if TYPE_CHECKING:
    from app.services.rag_service import RagService


class AnalysisKnowledgeService:
    def __init__(self, settings: Settings, rag_service: "RagService") -> None:
        self._settings = settings
        self._rag_service = rag_service

    def is_enabled(self) -> bool:
        return self._settings.analysis_kb_enhancement_enabled

    def generate_guidance(
        self,
        task: AnalyzeRequest,
        vision_result: VisionAnalysisResult | None,
        risk_assessment: Any | None,
    ) -> dict[str, Any] | None:
        if not self.is_enabled():
            return None

        question = self._build_question(vision_result, risk_assessment)
        request = RagAskRequest(
            trace_id=task.trace_id,
            kb_code=self._settings.analysis_kb_code,
            related_biz_no=task.task_no,
            patient_uuid=str(task.patient_id) if task.patient_id is not None else None,
            org_id=task.org_id,
            question=question,
            scene="DOCTOR_QA",
            include_debug=False,
            case_context=self._build_case_context(task, vision_result, risk_assessment),
        )
        result = self._rag_service.ask(request)
        return {
            "question": question,
            "answer": result.get("answer"),
            "citations": result.get("citations") or [],
            "knowledgeVersion": result.get("knowledgeVersion"),
            "confidence": result.get("confidence"),
            "safetyFlags": result.get("safetyFlags") or [],
            "refusalReason": result.get("refusalReason"),
            "latencyMs": result.get("latencyMs"),
            "llmTelemetry": result.get("llmTelemetry"),
            "evidenceSufficient": result.get("evidenceSufficient"),
            "distinctDocumentCount": result.get("distinctDocumentCount"),
        }

    @staticmethod
    def _build_question(
        vision_result: VisionAnalysisResult | None,
        risk_assessment: Any | None,
    ) -> str:
        severity = vision_result.overall_severity_code if vision_result is not None else "UNKNOWN"
        lesion_count = len(vision_result.findings) if vision_result is not None else 0
        risk_level = (
            getattr(risk_assessment, "risk_level_code", None)
            or getattr(risk_assessment, "overall_risk_level_code", None)
            or "UNKNOWN"
        )
        return (
            "Based only on the published local dental knowledge base, provide conservative dentist-facing "
            "management advice for the current caries image findings. Do not give a final diagnosis, do not "
            "prescribe medication dosage, and explicitly frame the output as AI-assisted suggestions pending "
            "clinical confirmation. Include immediate management priority, follow-up recommendation, and key "
            f"patient education points. Severity={severity}; lesionCount={lesion_count}; riskLevel={risk_level}."
        )

    @staticmethod
    def _build_case_context(
        task: AnalyzeRequest,
        vision_result: VisionAnalysisResult | None,
        risk_assessment: Any | None,
    ) -> dict[str, Any]:
        findings: list[dict[str, Any]] = []
        if vision_result is not None:
            for item in vision_result.findings[:5]:
                findings.append(
                    {
                        "toothCode": item.tooth_code,
                        "severityCode": item.severity_code,
                        "summary": item.summary,
                        "lesionAreaRatio": item.lesion_area_ratio,
                        "treatmentSuggestion": item.treatment_suggestion,
                    }
                )

        profile = task.patient_profile
        return {
            "caseNo": task.case_no,
            "caseId": task.case_id,
            "highestSeverity": vision_result.overall_severity_code if vision_result is not None else None,
            "uncertaintyScore": vision_result.overall_uncertainty_score if vision_result is not None else None,
            "lesionCount": len(vision_result.findings) if vision_result is not None else 0,
            "abnormalToothCount": len({item.tooth_code for item in (vision_result.findings if vision_result is not None else []) if item.tooth_code}),
            "riskLevelCode": (
                getattr(risk_assessment, "risk_level_code", None)
                or getattr(risk_assessment, "overall_risk_level_code", None)
            ),
            "recommendedCycleDays": getattr(risk_assessment, "recommended_cycle_days", None),
            "reviewSuggestedFlag": "1" if getattr(risk_assessment, "review_suggested", False) else "0",
            "toothFindings": findings,
            "ageGroup": AnalysisKnowledgeService._age_group(profile.age if profile is not None else None),
            "brushingFrequencyCode": profile.brushing_frequency_code if profile is not None else None,
            "sugarDietLevelCode": profile.sugar_diet_level_code if profile is not None else None,
            "previousCariesCount": profile.previous_caries_count if profile is not None else None,
        }

    @staticmethod
    def _age_group(age: int | None) -> str | None:
        if age is None:
            return None
        if age < 6:
            return "PRESCHOOL"
        if age < 18:
            return "CHILD"
        if age < 60:
            return "ADULT"
        return "SENIOR"
