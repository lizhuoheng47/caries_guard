from __future__ import annotations

import re


class QueryRewriteService:
    def rewrite(self, query: str) -> str:
        normalized = re.sub(r"\s+", " ", (query or "").strip())
        return normalized
