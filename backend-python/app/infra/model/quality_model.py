"""Compatibility wrappers for the real quality CV adapter."""

from __future__ import annotations

from app.quality.quality_adapter import QualityAssessmentAdapter


class QualityModelAdapter(QualityAssessmentAdapter):
    """Backward-compatible alias used by legacy tests/imports."""


class QualityHeuristicAdapter(QualityAssessmentAdapter):
    """Router-facing alias (keeps existing mapping stable)."""

