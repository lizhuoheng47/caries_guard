from __future__ import annotations

import json
import re
from typing import Any


class CaseContextBuilder:
    _allowed_keys = {
        "caseNo",
        "caseId",
        "highestSeverity",
        "uncertaintyScore",
        "lesionCount",
        "abnormalToothCount",
        "riskLevelCode",
        "recommendedCycleDays",
        "reviewSuggestedFlag",
        "toothFindings",
        "visualAssetCount",
        "correctionCount",
        "ageGroup",
        "brushingFrequencyCode",
        "sugarDietLevelCode",
        "previousCariesCount",
    }

    def build(self, context: dict[str, Any] | None) -> str | None:
        if not context:
            return None
        minimal = {key: value for key, value in context.items() if key in self._allowed_keys}
        if not minimal:
            return None
        return self.redact(json.dumps(minimal, ensure_ascii=False, sort_keys=True))

    @staticmethod
    def redact(text: str | None) -> str | None:
        if text is None:
            return None
        redacted = re.sub(r"[\w.+-]+@[\w-]+(?:\.[\w-]+)+", "[REDACTED_EMAIL]", text)
        redacted = re.sub(r"\b1[3-9]\d{9}\b", "[REDACTED_PHONE]", redacted)
        redacted = re.sub(r"\b\d{15}(?:\d{2}[\dXx])?\b", "[REDACTED_ID]", redacted)
        return redacted
