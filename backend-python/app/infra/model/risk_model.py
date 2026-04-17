"""Risk fusion adapter - HEURISTIC implementation.

Phase 5D combines structured patient factors with image-derived severity,
uncertainty, and lesion evidence. It is an auditable algorithmic adapter, not a
trained risk model.
"""

from __future__ import annotations

from typing import Any

from app.infra.model.base_model import BaseModelAdapter, ImplType
from app.schemas.request import PatientProfile


class RiskFusionAdapter(BaseModelAdapter):
    """Heuristic risk-fusion adapter for Phase 5D."""

    model_code = "risk-fusion-heuristic-v1"
    model_type_code = "RISK"
    impl_type = ImplType.HEURISTIC

    def __init__(self, confidence_threshold: float = 0.5) -> None:
        self._loaded = False
        self._confidence_threshold = confidence_threshold

    def load(self) -> None:
        self._loaded = True

    def is_loaded(self) -> bool:
        return self._loaded

    def unload(self) -> None:
        self._loaded = False

    def infer(
        self,
        *,
        patient_profile: PatientProfile | None,
        grading_result: Any | None,
        segmentation_regions: list[dict[str, Any]] | None = None,
        tooth_detection_count: int = 0,
    ) -> dict[str, Any]:
        grade = getattr(grading_result, "grading_label", None) or "C1"
        uncertainty = float(getattr(grading_result, "uncertainty_score", 0.1) or 0.1)
        needs_review = bool(getattr(grading_result, "needs_review", False))
        regions = segmentation_regions or []

        factors = self._factor_scores(patient_profile, grade, uncertainty, needs_review, len(regions))
        risk_score = round(min(100.0, max(0.0, sum(item["points"] for item in factors))), 2)
        risk_level = self._level(risk_score)
        cycle_days = self._cycle_days(risk_level, needs_review)

        return {
            "riskLevelCode": risk_level,
            "riskScore": int(round(risk_score)),
            "recommendedCycleDays": cycle_days,
            "implType": ImplType.HEURISTIC.value,
            "explanationFactors": [
                {
                    "featureCode": item["featureCode"],
                    "contribution": round(item["points"] / 100.0, 4),
                    "direction": item["direction"],
                }
                for item in factors
                if item["points"] != 0
            ],
            "rawResult": {
                "modelCode": self.model_code,
                "implType": ImplType.HEURISTIC.value,
                "riskScore": int(round(risk_score)),
                "riskLevelCode": risk_level,
                "recommendedCycleDays": cycle_days,
                "gradingLabel": grade,
                "uncertaintyScore": round(uncertainty, 4),
                "needsReview": needs_review,
                "lesionRegionCount": len(regions),
                "toothDetectionCount": tooth_detection_count,
                "confidenceThreshold": self._confidence_threshold,
                "factors": factors,
            },
        }

    def _factor_scores(
        self,
        patient_profile: PatientProfile | None,
        grade: str,
        uncertainty: float,
        needs_review: bool,
        lesion_region_count: int,
    ) -> list[dict[str, Any]]:
        factors: list[dict[str, Any]] = [
            {
                "featureCode": "grading_label",
                "points": {"C0": 4, "C1": 18, "C2": 34, "C3": 48}.get(grade, 18),
                "direction": "POSITIVE",
            },
            {
                "featureCode": "uncertainty_score",
                "points": min(16.0, uncertainty * 28.0),
                "direction": "POSITIVE",
            },
            {
                "featureCode": "lesion_region_count",
                "points": min(12.0, lesion_region_count * 4.0),
                "direction": "POSITIVE",
            },
        ]
        if needs_review:
            factors.append({"featureCode": "needs_review", "points": 8.0, "direction": "POSITIVE"})

        if patient_profile is None:
            return factors

        previous = patient_profile.previous_caries_count or 0
        if previous > 0:
            factors.append({
                "featureCode": "previous_caries_count",
                "points": min(16.0, previous * 4.0),
                "direction": "POSITIVE",
            })

        sugar = (patient_profile.sugar_diet_level_code or "").upper()
        if sugar in {"HIGH", "H", "HIGH_SUGAR"}:
            factors.append({"featureCode": "sugar_diet_level", "points": 10.0, "direction": "POSITIVE"})
        elif sugar in {"LOW", "L"}:
            factors.append({"featureCode": "sugar_diet_level", "points": -4.0, "direction": "NEGATIVE"})

        brushing = (patient_profile.brushing_frequency_code or "").upper()
        if brushing in {"LOW", "ONCE", "LESS_THAN_DAILY"}:
            factors.append({"featureCode": "brushing_frequency", "points": 8.0, "direction": "POSITIVE"})
        elif brushing in {"TWICE_DAILY", "HIGH", "GOOD"}:
            factors.append({"featureCode": "brushing_frequency", "points": -6.0, "direction": "NEGATIVE"})

        fluoride = (patient_profile.fluoride_use_flag or "").upper()
        if fluoride in {"1", "Y", "YES", "TRUE"}:
            factors.append({"featureCode": "fluoride_use", "points": -5.0, "direction": "NEGATIVE"})

        months = patient_profile.last_dental_check_months
        if months is not None and months > 12:
            factors.append({"featureCode": "last_dental_check_months", "points": 6.0, "direction": "POSITIVE"})

        return factors

    @staticmethod
    def _level(score: float) -> str:
        if score >= 70:
            return "HIGH"
        if score >= 40:
            return "MEDIUM"
        return "LOW"

    @staticmethod
    def _cycle_days(risk_level: str, needs_review: bool) -> int:
        if needs_review or risk_level == "HIGH":
            return 90
        if risk_level == "MEDIUM":
            return 180
        return 365
