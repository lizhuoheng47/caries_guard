from __future__ import annotations

from typing import Any

from app.schemas.base import CamelModel


class RiskFactor(CamelModel):
    code: str
    weight: float
    source: str
    evidence: str


class StructuredRiskAssessment(CamelModel):
    risk_level: str
    risk_score: float
    risk_factors: list[RiskFactor]
    followup_suggestion: str
    review_suggested: bool
    explanation: str
    fusion_version: str
    evidence_quality: str = "SUFFICIENT"
    raw_signals: dict[str, Any] = {}
