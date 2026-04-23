from __future__ import annotations

from app.schemas.risk_assessment import RiskFactor

_RISK_LEVEL_CHINESE: dict[str, str] = {
    "HIGH": "高",
    "MEDIUM": "中",
    "LOW": "低",
    "VERY_HIGH": "极高",
    "MINIMAL": "极低",
}

_FACTOR_CHINESE: dict[str, str] = {
    "SEVERITY_FACTOR": "严重程度",
    "UNCERTAINTY_FACTOR": "不确定性",
    "MULTI_LESION_FACTOR": "多发病灶",
    "TOOTH_COUNT_FACTOR": "牙齿数量",
    "AGE_FACTOR": "年龄",
    "DIET_FACTOR": "饮食习惯",
    "HYGIENE_FACTOR": "口腔卫生",
    "FLUORIDE_FACTOR": "氟化物使用",
    "HISTORY_FACTOR": "既往龋齿史",
    "QUALITY_FACTOR": "影像质量",
    "REVIEW_NEEDED_FACTOR": "复核需求",
}


class RiskExplanationService:
    def explain(self, risk_level: str, factors: list[RiskFactor], review_suggested: bool) -> str:
        positive = [item for item in factors if item.weight > 0]
        positive.sort(key=lambda item: item.weight, reverse=True)
        drivers = ", ".join(item.code for item in positive[:3]) or "NO_MAJOR_RISK_DRIVER"
        review = " Human review is suggested before using this as a final clinical conclusion." if review_suggested else ""

        cn_level = _RISK_LEVEL_CHINESE.get(risk_level, risk_level)
        cn_drivers = "、".join(_FACTOR_CHINESE.get(item.code, item.code) for item in positive[:3]) or "无主要风险驱动"
        cn_review = " 建议在用作最终临床结论前进行人工复核。" if review_suggested else ""
        cn_part = f" [中文] 风险等级: {cn_level}。主要驱动因子: {cn_drivers}。{cn_review}"

        return f"Risk level is {risk_level}. Main drivers: {drivers}.{review}{cn_part}"
