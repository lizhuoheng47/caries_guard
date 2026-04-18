from __future__ import annotations

from typing import Any

from app.core.config import Settings
from app.schemas.request import PatientProfile
from app.schemas.risk_assessment import RiskFactor


class RiskRuleEngine:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def factors(self, signals: dict[str, Any], patient_profile: PatientProfile | None) -> list[RiskFactor]:
        factors: list[RiskFactor] = []
        grade = str(signals.get("grading_label") or "C1").upper()
        grade_weight = {"C0": 0.08, "C1": 0.18, "C2": 0.38, "C3": 0.55}.get(grade, 0.18)
        factors.append(RiskFactor(code="IMAGE_GRADING", weight=grade_weight, source="image", evidence=f"gradingLabel={grade}"))

        uncertainty = self._float(signals.get("uncertainty_score"), 0.1)
        if uncertainty >= self.settings.uncertainty_review_threshold:
            factors.append(
                RiskFactor(
                    code="HIGH_UNCERTAINTY",
                    weight=min(0.16, uncertainty * 0.25),
                    source="businessRule",
                    evidence=f"uncertaintyScore={uncertainty:.4f}",
                )
            )
        elif uncertainty > 0:
            factors.append(
                RiskFactor(
                    code="UNCERTAINTY_SIGNAL",
                    weight=min(0.08, uncertainty * 0.12),
                    source="image",
                    evidence=f"uncertaintyScore={uncertainty:.4f}",
                )
            )

        lesion_region_count = self._int(signals.get("lesion_region_count"), 0)
        if lesion_region_count > 0:
            factors.append(
                RiskFactor(
                    code="LESION_REGION_COUNT",
                    weight=min(0.12, lesion_region_count * 0.04),
                    source="image",
                    evidence=f"lesionRegionCount={lesion_region_count}",
                )
            )

        suspicious_tooth_count = self._int(signals.get("suspicious_tooth_count"), 0)
        if suspicious_tooth_count > 1:
            factors.append(
                RiskFactor(
                    code="MULTI_TOOTH_SUSPECTED",
                    weight=min(0.12, suspicious_tooth_count * 0.04),
                    source="image",
                    evidence=f"suspiciousToothCount={suspicious_tooth_count}",
                )
            )

        quality_status = str(signals.get("quality_status_code") or "PASS").upper()
        if quality_status not in {"PASS", "OK", "NORMAL"}:
            factors.append(
                RiskFactor(
                    code="EVIDENCE_INSUFFICIENT",
                    weight=0.10,
                    source="businessRule",
                    evidence=f"qualityStatusCode={quality_status}",
                )
            )

        if patient_profile is None:
            factors.append(
                RiskFactor(
                    code="PATIENT_PROFILE_MISSING",
                    weight=0.04,
                    source="businessRule",
                    evidence="patientProfile=null",
                )
            )
            return factors

        previous = patient_profile.previous_caries_count or 0
        if previous > 0:
            factors.append(
                RiskFactor(
                    code="PREVIOUS_CARIES",
                    weight=min(0.14, previous * 0.035),
                    source="patientProfile",
                    evidence=f"previousCariesCount={previous}",
                )
            )

        sugar = (patient_profile.sugar_diet_level_code or "").upper()
        if sugar in {"HIGH", "H", "HIGH_SUGAR"}:
            factors.append(RiskFactor(code="HIGH_SUGAR_DIET", weight=0.12, source="patientProfile", evidence=f"sugarDietLevelCode={sugar}"))
        elif sugar in {"MEDIUM", "M"}:
            factors.append(RiskFactor(code="MEDIUM_SUGAR_DIET", weight=0.06, source="patientProfile", evidence=f"sugarDietLevelCode={sugar}"))
        elif sugar in {"LOW", "L"}:
            factors.append(RiskFactor(code="LOW_SUGAR_DIET", weight=-0.03, source="patientProfile", evidence=f"sugarDietLevelCode={sugar}"))

        brushing = (patient_profile.brushing_frequency_code or "").upper()
        if brushing in {"LOW", "ONCE", "LESS_THAN_DAILY"}:
            factors.append(RiskFactor(code="POOR_HYGIENE_PROFILE", weight=0.08, source="patientProfile", evidence=f"brushingFrequencyCode={brushing}"))
        elif brushing in {"TWICE_DAILY", "HIGH", "GOOD"}:
            factors.append(RiskFactor(code="GOOD_HYGIENE_PROFILE", weight=-0.05, source="patientProfile", evidence=f"brushingFrequencyCode={brushing}"))

        fluoride = (patient_profile.fluoride_use_flag or "").upper()
        if fluoride in {"1", "Y", "YES", "TRUE"}:
            factors.append(RiskFactor(code="FLUORIDE_PROTECTIVE", weight=-0.04, source="patientProfile", evidence="fluorideUseFlag=YES"))

        months = patient_profile.last_dental_check_months
        if months is not None and months > 12:
            factors.append(
                RiskFactor(
                    code="DENTAL_CHECK_OVERDUE",
                    weight=0.06,
                    source="patientProfile",
                    evidence=f"lastDentalCheckMonths={months}",
                )
            )
        return factors

    @staticmethod
    def level(score: float) -> str:
        if score >= 0.70:
            return "HIGH"
        if score >= 0.40:
            return "MEDIUM"
        return "LOW"

    @staticmethod
    def followup_suggestion(risk_level: str) -> str:
        if risk_level == "HIGH":
            return "3_MONTH_RECHECK"
        if risk_level == "MEDIUM":
            return "6_MONTH_RECHECK"
        return "12_MONTH_RECHECK"

    def review_suggested(self, risk_level: str, signals: dict[str, Any], factors: list[RiskFactor]) -> bool:
        uncertainty = self._float(signals.get("uncertainty_score"), 0.0)
        return (
            risk_level == "HIGH"
            or bool(signals.get("needs_review"))
            or uncertainty >= self.settings.uncertainty_review_threshold
            or any(item.code == "EVIDENCE_INSUFFICIENT" for item in factors)
        )

    @staticmethod
    def evidence_quality(factors: list[RiskFactor]) -> str:
        return "INSUFFICIENT" if any(item.code == "EVIDENCE_INSUFFICIENT" for item in factors) else "SUFFICIENT"

    @staticmethod
    def _float(value: Any, default: float) -> float:
        try:
            return float(value)
        except (TypeError, ValueError):
            return default

    @staticmethod
    def _int(value: Any, default: int) -> int:
        try:
            return int(value)
        except (TypeError, ValueError):
            return default
