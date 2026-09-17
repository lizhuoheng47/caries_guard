from __future__ import annotations

import json
from dataclasses import dataclass
from typing import Any

import requests

from app.core.config import Settings
from app.core.logging import get_logger
from app.schemas.request import AnalyzeRequest
from app.services.knowledge_base_service import KnowledgeBaseService, KnowledgeHit

log = get_logger("cariesguard-ai.rag")


@dataclass(frozen=True)
class RagResult:
    status: str
    generator: str
    knowledge_version: str | None
    citations: list[dict[str, Any]]
    evidence_refs: list[dict[str, Any]]
    treatment_plan: list[dict[str, str]]
    clinical_summary: str | None
    error: str | None = None


class RagService:
    def __init__(self, settings: Settings, knowledge_base: KnowledgeBaseService) -> None:
        self._settings = settings
        self._knowledge_base = knowledge_base

    def enrich(
        self,
        task: AnalyzeRequest,
        raw_result: dict[str, Any],
        *,
        severity_code: str | None,
        risk_level: str | None,
    ) -> RagResult:
        if not self._settings.rag_enabled:
            return RagResult("DISABLED", "NONE", None, [], [], [], None)

        query = self._build_query(task, raw_result, severity_code, risk_level)
        hits = self._knowledge_base.search(query, self._settings.rag_top_k)
        citations = [hit.to_citation(index) for index, hit in enumerate(hits, start=1)]
        evidence_refs = self._evidence_refs(hits)
        fallback_summary, fallback_plan = self._template_result(severity_code, risk_level, hits)
        if not hits:
            return RagResult(
                status="EMPTY",
                generator="LOCAL_TEMPLATE",
                knowledge_version=self._knowledge_base.version,
                citations=[],
                evidence_refs=[],
                treatment_plan=fallback_plan,
                clinical_summary=fallback_summary,
                error="no relevant knowledge chunk was retrieved",
            )

        if not self._settings.rag_llm_enabled:
            return RagResult(
                status="READY",
                generator="LOCAL_TEMPLATE",
                knowledge_version=self._knowledge_base.version,
                citations=citations,
                evidence_refs=evidence_refs,
                treatment_plan=fallback_plan,
                clinical_summary=fallback_summary,
            )

        try:
            generated = self._generate_with_llm(query, severity_code, risk_level, citations)
            return RagResult(
                status="READY",
                generator="QWEN_GROUNDED",
                knowledge_version=self._knowledge_base.version,
                citations=citations,
                evidence_refs=evidence_refs,
                treatment_plan=self._normalize_plan(generated.get("treatmentPlan")) or fallback_plan,
                clinical_summary=self._clean_text(generated.get("clinicalSummary")) or fallback_summary,
            )
        except Exception as exc:
            log.warning("RAG LLM generation failed; using grounded template: %s", exc)
            return RagResult(
                status="DEGRADED",
                generator="LOCAL_TEMPLATE",
                knowledge_version=self._knowledge_base.version,
                citations=citations,
                evidence_refs=evidence_refs,
                treatment_plan=fallback_plan,
                clinical_summary=fallback_summary,
                error=str(exc),
            )

    @staticmethod
    def _build_query(
        task: AnalyzeRequest,
        raw_result: dict[str, Any],
        severity_code: str | None,
        risk_level: str | None,
    ) -> str:
        parts = ["龋齿 影像 检查 处置 随访", f"severity {severity_code or 'UNKNOWN'}", f"risk {risk_level or 'UNKNOWN'}"]
        profile = task.patient_profile
        if profile:
            parts.extend(
                [
                    f"age {profile.age}" if profile.age is not None else "",
                    f"sugar {profile.sugar_diet_level_code}" if profile.sugar_diet_level_code else "",
                    f"fluoride {profile.fluoride_use_flag}" if profile.fluoride_use_flag else "",
                    f"previous caries {profile.previous_caries_count}" if profile.previous_caries_count is not None else "",
                ]
            )
        factors = raw_result.get("riskFactors") or []
        parts.extend(str(item) for item in factors[:8])
        return " ".join(part for part in parts if part)

    @staticmethod
    def _template_result(
        severity_code: str | None,
        risk_level: str | None,
        hits: list[KnowledgeHit],
    ) -> tuple[str, list[dict[str, str]]]:
        severity = (severity_code or "UNKNOWN").upper()
        risk = (risk_level or "UNKNOWN").upper()
        evidence_note = f"已关联 {len(hits)} 条版本化知识证据" if hits else "未检索到匹配知识证据"
        summary = (
            f"影像辅助分析提示最高分级为 {severity}，综合风险为 {risk}；{evidence_note}。"
            "该结果仅用于辅助排序与复核，最终诊断和处置应由口腔医生结合临床检查确认。"
        )
        urgent = severity in {"C3", "SEVERE", "DEEP_CARIES"} or risk in {"HIGH", "CRITICAL"}
        medium = severity in {"C2", "MODERATE"} or risk == "MEDIUM"
        if urgent:
            plan = [
                {"priority": "HIGH", "title": "尽快完成临床复核", "details": "结合症状、口内检查及必要的补充影像评估病变深度与牙髓状态。"},
                {"priority": "MEDIUM", "title": "制定个体化处置", "details": "由医生确认修复、牙髓评估或其他治疗路径，并记录知情沟通。"},
            ]
        elif medium:
            plan = [
                {"priority": "MEDIUM", "title": "安排口腔检查", "details": "复核可疑区域并结合龋风险因素决定非手术管理或修复治疗。"},
                {"priority": "LOW", "title": "加强风险控制", "details": "评估含氟措施、饮食频率和口腔卫生，并按风险安排随访。"},
            ]
        else:
            plan = [
                {"priority": "LOW", "title": "常规复核与预防", "details": "由医生确认影像结果，持续口腔卫生、含氟防护和风险因素管理。"}
            ]
        return summary, plan

    @staticmethod
    def _evidence_refs(hits: list[KnowledgeHit]) -> list[dict[str, Any]]:
        return [
            {
                "refType": "KNOWLEDGE",
                "refCode": hit.chunk.doc_no,
                "summary": hit.chunk.doc_title,
                "source": hit.chunk.source_uri or hit.chunk.source_file,
            }
            for hit in hits
        ]

    def _generate_with_llm(
        self,
        query: str,
        severity_code: str | None,
        risk_level: str | None,
        citations: list[dict[str, Any]],
    ) -> dict[str, Any]:
        evidence = [
            {
                "rankNo": item["rankNo"],
                "title": item["docTitle"],
                "text": item["chunkText"][:1200],
            }
            for item in citations
        ]
        payload = {
            "model": self._settings.rag_llm_model,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "You are a conservative dental decision-support writer. Use only supplied evidence. "
                        "Return JSON only with clinicalSummary and treatmentPlan. Never claim a definitive diagnosis."
                    ),
                },
                {
                    "role": "user",
                    "content": json.dumps(
                        {
                            "query": query,
                            "severityCode": severity_code,
                            "riskLevel": risk_level,
                            "evidence": evidence,
                            "responseSchema": {
                                "clinicalSummary": "string",
                                "treatmentPlan": [{"priority": "LOW|MEDIUM|HIGH", "title": "string", "details": "string"}],
                            },
                        },
                        ensure_ascii=False,
                    ),
                },
            ],
            "temperature": self._settings.rag_llm_temperature,
        }
        response = requests.post(
            self._settings.rag_llm_base_url.rstrip("/") + "/chat/completions",
            json=payload,
            headers={"Authorization": f"Bearer {self._settings.rag_llm_api_key}", "Content-Type": "application/json"},
            timeout=self._settings.rag_llm_timeout_seconds,
        )
        response.raise_for_status()
        body = response.json()
        content = body["choices"][0]["message"]["content"]
        if isinstance(content, list):
            content = "".join(str(item.get("text") or "") for item in content if isinstance(item, dict))
        text = str(content).strip()
        if text.startswith("```"):
            text = text.strip("`").removeprefix("json").strip()
        parsed = json.loads(text)
        if not isinstance(parsed, dict):
            raise ValueError("RAG model response must be a JSON object")
        return parsed

    @staticmethod
    def _normalize_plan(value: Any) -> list[dict[str, str]]:
        if not isinstance(value, list):
            return []
        normalized: list[dict[str, str]] = []
        for item in value[:5]:
            if not isinstance(item, dict):
                continue
            title = str(item.get("title") or "").strip()
            details = str(item.get("details") or item.get("description") or "").strip()
            if not title or not details:
                continue
            priority = str(item.get("priority") or "MEDIUM").upper()
            if priority not in {"LOW", "MEDIUM", "HIGH"}:
                priority = "MEDIUM"
            normalized.append({"priority": priority, "title": title[:120], "details": details[:1000]})
        return normalized

    @staticmethod
    def _clean_text(value: Any) -> str | None:
        text = " ".join(str(value or "").split()).strip()
        return text[:2000] or None
