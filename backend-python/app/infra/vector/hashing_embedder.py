from __future__ import annotations

import hashlib
import math
import re


class HashingEmbedder:
    def __init__(self, dimension: int = 256) -> None:
        self.dimension = dimension

    def embed(self, text: str) -> list[float]:
        vector = [0.0] * self.dimension
        for token in self._tokens(text):
            digest = hashlib.md5(token.encode("utf-8")).hexdigest()
            vector[int(digest[:8], 16) % self.dimension] += 1.0
        norm = math.sqrt(sum(value * value for value in vector))
        if norm == 0:
            return vector
        return [round(value / norm, 8) for value in vector]

    @staticmethod
    def _tokens(text: str) -> list[str]:
        lower = text.lower()
        latin = re.findall(r"[a-z0-9]+", lower)
        cjk = re.findall(r"[\u4e00-\u9fff]", text)
        cjk_bigrams = [f"{cjk[i]}{cjk[i + 1]}" for i in range(len(cjk) - 1)]
        return latin + cjk + cjk_bigrams
