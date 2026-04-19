from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class RerankMetadata:
    provider: str
    model: str
    version: str


class BaseRerankProvider(Protocol):
    @property
    def metadata(self) -> RerankMetadata:
        ...

    def score(self, query: str, candidates: list[dict]) -> list[float]:
        ...
