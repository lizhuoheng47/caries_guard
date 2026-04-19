from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class EmbeddingMetadata:
    provider: str
    model: str
    dimension: int
    version: str


class BaseEmbeddingProvider(Protocol):
    @property
    def metadata(self) -> EmbeddingMetadata:
        ...

    def embed(self, text: str) -> list[float]:
        ...

    def embed_many(self, texts: list[str]) -> list[list[float]]:
        ...
