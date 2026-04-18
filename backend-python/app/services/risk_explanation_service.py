from __future__ import annotations

from app.schemas.risk_assessment import RiskFactor


class RiskExplanationService:
    def explain(self, risk_level: str, factors: list[RiskFactor], review_suggested: bool) -> str:
        positive = [item for item in factors if item.weight > 0]
        positive.sort(key=lambda item: item.weight, reverse=True)
        drivers = ", ".join(item.code for item in positive[:3]) or "NO_MAJOR_RISK_DRIVER"
        review = " Human review is suggested before using this as a final clinical conclusion." if review_suggested else ""
        return f"Risk level is {risk_level}. Main drivers: {drivers}.{review}"
