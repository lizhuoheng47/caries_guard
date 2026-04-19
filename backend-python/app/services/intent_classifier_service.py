from __future__ import annotations


class IntentClassifierService:
    def classify(self, query: str) -> str:
        lowered = (query or "").lower()
        if any(token in lowered for token in ("建议", "风险", "复查", "多久")):
            return "GRAPH_QUERY"
        if any(token in lowered for token in ("患者", "家长", "通俗")):
            return "PATIENT_EXPLAIN"
        return "DOCTOR_QA"
