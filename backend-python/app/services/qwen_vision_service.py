from __future__ import annotations

import base64
import json
import mimetypes
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import requests
from PIL import Image, ImageDraw, ImageFilter

from app.core.config import Settings
from app.core.logging import get_logger
from app.schemas.callback import ToothDetection

log = get_logger("cariesguard-ai.qwen-vision")


@dataclass(frozen=True)
class VisionFinding:
    tooth_code: str | None
    severity_code: str
    confidence_score: float
    uncertainty_score: float
    lesion_area_ratio: float
    lesion_area_px: int
    bbox: list[int]
    polygon: list[list[int]]
    summary: str | None
    treatment_suggestion: str | None


@dataclass(frozen=True)
class VisionAnalysisResult:
    image_id: int | None
    image_width: int
    image_height: int
    overall_severity_code: str
    overall_confidence_score: float
    overall_uncertainty_score: float
    clinical_summary: str | None
    treatment_plan: list[dict[str, Any]]
    findings: list[VisionFinding]
    raw_result: dict[str, Any]

    def to_regions(self) -> list[dict[str, Any]]:
        regions: list[dict[str, Any]] = []
        for index, finding in enumerate(self.findings):
            regions.append(
                {
                    "toothCode": finding.tooth_code,
                    "severityCode": finding.severity_code,
                    "bbox": finding.bbox,
                    "polygon": finding.polygon,
                    "score": finding.confidence_score,
                    "regionIndex": index,
                    "lesionAreaRatio": finding.lesion_area_ratio,
                    "treatmentSuggestion": finding.treatment_suggestion,
                    "summary": finding.summary,
                }
            )
        return regions


@dataclass(frozen=True)
class VisionRenderResult:
    mask_path: Path
    overlay_path: Path
    heatmap_path: Path


class QwenVisionService:
    def __init__(self, settings: Settings) -> None:
        self._settings = settings

    def is_enabled(self) -> bool:
        return self._settings.qwen_vision_enabled

    def analyze(
        self,
        image_path: Path,
        image_id: int | None,
        tooth_detections: list[ToothDetection],
    ) -> VisionAnalysisResult:
        width, height = self._image_size(image_path)
        payload = self._build_payload(image_path, width, height, tooth_detections)

        started = time.perf_counter()
        response_json = self._post(payload)
        latency_ms = int((time.perf_counter() - started) * 1000)

        content = self._response_text(response_json)
        parsed = self._parse_json(content)
        findings = self._normalize_findings(parsed, width, height)
        treatment_plan = self._normalize_treatment_plan(parsed.get("treatmentPlan"))
        overall_severity = self._normalize_severity(
            parsed.get("overallSeverityCode"),
            default=self._highest_severity(findings),
        )
        overall_confidence = self._clamp_score(
            parsed.get("overallConfidenceScore"),
            default=max((item.confidence_score for item in findings), default=0.0),
        )
        overall_uncertainty = self._clamp_score(
            parsed.get("overallUncertaintyScore"),
            default=max((item.uncertainty_score for item in findings), default=1.0 - overall_confidence),
        )
        clinical_summary = self._clean_text(parsed.get("clinicalSummary") or parsed.get("summary"))

        raw_result = {
            "provider": "QWEN",
            "model": self._settings.qwen_vision_model,
            "baseUrl": self._settings.qwen_vision_base_url,
            "imageWidth": width,
            "imageHeight": height,
            "latencyMs": latency_ms,
            "usage": response_json.get("usage"),
            "finishReason": self._finish_reason(response_json),
            "clinicalSummary": clinical_summary,
            "treatmentPlan": treatment_plan,
            "findings": [self._dump_finding(item) for item in findings],
            "responseText": content,
        }
        return VisionAnalysisResult(
            image_id=image_id,
            image_width=width,
            image_height=height,
            overall_severity_code=overall_severity,
            overall_confidence_score=overall_confidence,
            overall_uncertainty_score=overall_uncertainty,
            clinical_summary=clinical_summary,
            treatment_plan=treatment_plan,
            findings=findings,
            raw_result=raw_result,
        )

    def render_visual_assets(
        self,
        image_path: Path,
        result: VisionAnalysisResult,
        output_dir: Path,
    ) -> VisionRenderResult:
        output_dir.mkdir(parents=True, exist_ok=True)
        image_id = result.image_id if result.image_id is not None else "unknown"
        mask_path = output_dir / f"mask_{image_id}_qwen.png"
        overlay_path = output_dir / f"overlay_{image_id}_qwen.png"
        heatmap_path = output_dir / f"heatmap_{image_id}_qwen.png"

        try:
            base = Image.open(image_path).convert("RGB")
        except Exception:
            base = Image.new("RGB", (result.image_width or 512, result.image_height or 256), color=(245, 245, 245))

        mask = Image.new("L", base.size, color=0)
        overlay = base.convert("RGBA")
        heat = Image.new("RGBA", base.size, color=(255, 80, 0, 0))
        draw_mask = ImageDraw.Draw(mask)
        draw_overlay = ImageDraw.Draw(overlay)
        draw_heat = ImageDraw.Draw(heat)
        line_width = max(2, base.size[0] // 320)

        for finding in result.findings:
            polygon = [(int(point[0]), int(point[1])) for point in finding.polygon]
            if len(polygon) < 3:
                polygon = self._bbox_polygon(finding.bbox)

            alpha = max(60, min(190, int(60 + finding.confidence_score * 110)))
            color = self._severity_color(finding.severity_code)
            label = " ".join(part for part in [finding.severity_code, finding.tooth_code or "UNK"] if part)
            x1, y1, _, _ = finding.bbox

            draw_mask.polygon(polygon, fill=255)
            draw_overlay.polygon(polygon, outline=color + (255,), width=line_width)
            draw_overlay.rectangle([x1, max(0, y1 - 24), x1 + 110, y1], fill=color + (210,))
            draw_overlay.text((x1 + 6, max(0, y1 - 20)), label, fill=(255, 255, 255, 255))
            draw_heat.polygon(polygon, fill=(255, 80, 0, alpha))

        mask.save(mask_path)
        overlay.convert("RGB").save(overlay_path)
        heat = heat.filter(ImageFilter.GaussianBlur(radius=max(2, base.size[0] // 180)))
        Image.alpha_composite(base.convert("RGBA"), heat).convert("RGB").save(heatmap_path)
        return VisionRenderResult(mask_path=mask_path, overlay_path=overlay_path, heatmap_path=heatmap_path)

    def _build_payload(
        self,
        image_path: Path,
        width: int,
        height: int,
        tooth_detections: list[ToothDetection],
    ) -> dict[str, Any]:
        image_url = self._data_uri(image_path)
        detections = [
            {
                "toothCode": item.tooth_code,
                "bbox": item.bbox,
                "score": item.detection_score,
            }
            for item in tooth_detections
        ]
        prompt = (
            "Return JSON only. Analyze this dental image conservatively for visible caries lesions. "
            "Use severityCode from C0,C1,C2,C3. Provide approximate lesionAreaRatio between 0 and 1. "
            "If no clear lesion is visible, return findings as an empty array and overallSeverityCode as C0.\n"
            "Expected JSON shape:\n"
            "{"
            "\"overallSeverityCode\":\"C1\","
            "\"overallConfidenceScore\":0.0,"
            "\"overallUncertaintyScore\":0.0,"
            "\"clinicalSummary\":\"...\","
            "\"treatmentPlan\":[{\"priority\":\"LOW|MEDIUM|HIGH\",\"title\":\"...\",\"details\":\"...\"}],"
            "\"findings\":["
            "{\"toothCode\":\"16\",\"severityCode\":\"C2\",\"confidenceScore\":0.0,\"uncertaintyScore\":0.0,"
            "\"bbox\":[0,0,0,0],\"polygon\":[[0,0],[1,0],[1,1],[0,1]],"
            "\"lesionAreaRatio\":0.0,\"summary\":\"...\",\"treatmentSuggestion\":\"...\"}"
            "]"
            "}\n"
            f"Image size: {width}x{height}.\n"
            f"Detected teeth priors: {json.dumps(detections, ensure_ascii=False)}"
        )
        return {
            "model": self._settings.qwen_vision_model,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "You are a dental radiograph analysis assistant. "
                        "Do not output markdown. Output valid JSON only."
                    ),
                },
                {
                    "role": "user",
                    "content": [
                        {"type": "image_url", "image_url": {"url": image_url}},
                        {"type": "text", "text": prompt},
                    ],
                },
            ],
            "temperature": self._settings.qwen_vision_temperature,
        }

    def _post(self, payload: dict[str, Any]) -> dict[str, Any]:
        if not self._settings.qwen_vision_base_url:
            raise RuntimeError("CG_QWEN_VISION_BASE_URL is required")
        if not self._settings.qwen_vision_api_key:
            raise RuntimeError("CG_QWEN_VISION_API_KEY is required")

        url = self._settings.qwen_vision_base_url.rstrip("/") + "/chat/completions"
        response = requests.post(
            url,
            json=payload,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {self._settings.qwen_vision_api_key}",
            },
            timeout=self._settings.qwen_vision_timeout_seconds,
        )
        response.raise_for_status()
        return response.json()

    @staticmethod
    def _response_text(response_json: dict[str, Any]) -> str:
        choice = (response_json.get("choices") or [{}])[0]
        message = choice.get("message") or {}
        content = message.get("content")
        if isinstance(content, str):
            return content.strip()
        if isinstance(content, list):
            texts: list[str] = []
            for item in content:
                if isinstance(item, dict) and item.get("type") == "text":
                    texts.append(str(item.get("text") or "").strip())
            return "\n".join(part for part in texts if part)
        raise RuntimeError("Qwen vision response did not contain text content")

    @staticmethod
    def _finish_reason(response_json: dict[str, Any]) -> str | None:
        choice = (response_json.get("choices") or [{}])[0]
        value = choice.get("finish_reason")
        return str(value).strip() if value else None

    @staticmethod
    def _parse_json(content: str) -> dict[str, Any]:
        cleaned = content.strip()
        if cleaned.startswith("```"):
            cleaned = cleaned.strip("`")
            if cleaned.startswith("json"):
                cleaned = cleaned[4:].lstrip()
        start = cleaned.find("{")
        end = cleaned.rfind("}")
        if start >= 0 and end >= start:
            cleaned = cleaned[start:end + 1]
        parsed = json.loads(cleaned)
        if not isinstance(parsed, dict):
            raise RuntimeError("Qwen vision JSON payload must be an object")
        return parsed

    def _normalize_findings(self, parsed: dict[str, Any], width: int, height: int) -> list[VisionFinding]:
        raw_findings = parsed.get("findings") or parsed.get("lesions") or []
        if not isinstance(raw_findings, list):
            raise RuntimeError("Qwen vision findings must be a list")

        findings: list[VisionFinding] = []
        for item in raw_findings:
            if not isinstance(item, dict):
                continue
            polygon = self._parse_polygon(item.get("polygon"), width, height)
            bbox = self._normalize_bbox(item.get("bbox"), polygon, width, height)
            polygon = self._normalize_polygon(polygon, bbox)
            area_ratio = self._normalize_area_ratio(item.get("lesionAreaRatio"), bbox, polygon, width, height)
            confidence = self._clamp_score(item.get("confidenceScore"), default=0.7)
            uncertainty = self._clamp_score(item.get("uncertaintyScore"), default=1.0 - confidence)
            findings.append(
                VisionFinding(
                    tooth_code=self._clean_text(item.get("toothCode")),
                    severity_code=self._normalize_severity(item.get("severityCode"), default="C1"),
                    confidence_score=confidence,
                    uncertainty_score=uncertainty,
                    lesion_area_ratio=area_ratio,
                    lesion_area_px=int(round(area_ratio * width * height)),
                    bbox=bbox,
                    polygon=polygon,
                    summary=self._clean_text(item.get("summary") or item.get("description")),
                    treatment_suggestion=self._clean_text(item.get("treatmentSuggestion") or item.get("treatment")),
                )
            )
        return findings

    def _normalize_treatment_plan(self, raw_plan: Any) -> list[dict[str, Any]]:
        if raw_plan is None:
            return []
        if isinstance(raw_plan, str):
            text = self._clean_text(raw_plan)
            return [{"priority": "MEDIUM", "title": "Clinical follow-up", "details": text}] if text else []
        if not isinstance(raw_plan, list):
            return []

        plan: list[dict[str, Any]] = []
        for item in raw_plan:
            if isinstance(item, str):
                text = self._clean_text(item)
                if text:
                    plan.append({"priority": "MEDIUM", "title": "Clinical follow-up", "details": text})
                continue
            if not isinstance(item, dict):
                continue
            details = self._clean_text(item.get("details") or item.get("description") or item.get("content"))
            title = self._clean_text(item.get("title")) or "Clinical follow-up"
            if details:
                plan.append(
                    {
                        "priority": self._normalize_priority(item.get("priority")),
                        "title": title,
                        "details": details,
                    }
                )
        return plan

    @staticmethod
    def _normalize_priority(value: Any) -> str:
        text = str(value or "").strip().upper()
        if text in {"LOW", "MEDIUM", "HIGH"}:
            return text
        return "MEDIUM"

    @staticmethod
    def _image_size(image_path: Path) -> tuple[int, int]:
        with Image.open(image_path) as image:
            return image.size

    @staticmethod
    def _data_uri(image_path: Path) -> str:
        mime = mimetypes.guess_type(str(image_path))[0] or "image/png"
        encoded = base64.b64encode(image_path.read_bytes()).decode("ascii")
        return f"data:{mime};base64,{encoded}"

    def _normalize_bbox(
        self,
        value: Any,
        polygon: list[list[int]],
        width: int,
        height: int,
    ) -> list[int]:
        if isinstance(value, list) and len(value) == 4:
            try:
                x1, y1, x2, y2 = [int(round(float(item))) for item in value]
                return self._clamp_box([x1, y1, x2, y2], width, height)
            except (TypeError, ValueError):
                pass
        if polygon:
            xs = [point[0] for point in polygon]
            ys = [point[1] for point in polygon]
            return self._clamp_box([min(xs), min(ys), max(xs), max(ys)], width, height)
        if self._settings.ai_runtime_mode == "real":
            raise RuntimeError("Qwen vision finding is missing a valid bbox or polygon")
        return self._stable_box(width, height)

    def _parse_polygon(
        self,
        value: Any,
        width: int,
        height: int,
    ) -> list[list[int]]:
        if isinstance(value, list):
            points: list[list[int]] = []
            for item in value:
                if not isinstance(item, (list, tuple)) or len(item) != 2:
                    continue
                try:
                    x = max(0, min(width - 1, int(round(float(item[0])))))
                    y = max(0, min(height - 1, int(round(float(item[1])))))
                    points.append([x, y])
                except (TypeError, ValueError):
                    continue
            if len(points) >= 3:
                return points
        return []

    def _normalize_polygon(
        self,
        polygon: list[list[int]],
        bbox: list[int],
    ) -> list[list[int]]:
        if polygon:
            return polygon
        return [[point[0], point[1]] for point in self._bbox_polygon(bbox)]

    def _normalize_area_ratio(
        self,
        value: Any,
        bbox: list[int],
        polygon: list[list[int]],
        width: int,
        height: int,
    ) -> float:
        area_ratio = self._clamp_score(value, default=-1.0)
        if area_ratio >= 0.0:
            return area_ratio

        image_area = max(1, width * height)
        polygon_area = self._polygon_area(polygon)
        if polygon_area > 0:
            return round(min(1.0, polygon_area / image_area), 6)

        x1, y1, x2, y2 = bbox
        bbox_area = max(0, x2 - x1) * max(0, y2 - y1)
        return round(min(1.0, bbox_area / image_area), 6)

    @staticmethod
    def _highest_severity(findings: list[VisionFinding]) -> str:
        if not findings:
            return "C0"
        order = {"C0": 0, "C1": 1, "C2": 2, "C3": 3}
        return max(findings, key=lambda item: order.get(item.severity_code, 0)).severity_code

    @staticmethod
    def _dump_finding(item: VisionFinding) -> dict[str, Any]:
        return {
            "toothCode": item.tooth_code,
            "severityCode": item.severity_code,
            "confidenceScore": item.confidence_score,
            "uncertaintyScore": item.uncertainty_score,
            "lesionAreaRatio": item.lesion_area_ratio,
            "lesionAreaPx": item.lesion_area_px,
            "bbox": item.bbox,
            "polygon": item.polygon,
            "summary": item.summary,
            "treatmentSuggestion": item.treatment_suggestion,
        }

    @staticmethod
    def _polygon_area(points: list[list[int]]) -> float:
        if len(points) < 3:
            return 0.0
        area = 0.0
        for index, point in enumerate(points):
            next_point = points[(index + 1) % len(points)]
            area += point[0] * next_point[1] - next_point[0] * point[1]
        return abs(area) / 2.0

    @staticmethod
    def _clamp_box(box: list[int], width: int, height: int) -> list[int]:
        x1, y1, x2, y2 = box
        clamped = [
            max(0, min(width - 1, x1)),
            max(0, min(height - 1, y1)),
            max(0, min(width - 1, x2)),
            max(0, min(height - 1, y2)),
        ]
        if clamped[2] <= clamped[0]:
            clamped[2] = min(width - 1, clamped[0] + max(8, width // 12))
        if clamped[3] <= clamped[1]:
            clamped[3] = min(height - 1, clamped[1] + max(8, height // 12))
        return clamped

    @staticmethod
    def _stable_box(width: int, height: int) -> list[int]:
        x1 = max(0, int(width * 0.35))
        y1 = max(0, int(height * 0.35))
        x2 = min(width - 1, int(width * 0.55))
        y2 = min(height - 1, int(height * 0.65))
        return [x1, y1, x2, y2]

    @staticmethod
    def _bbox_polygon(bbox: list[int]) -> list[tuple[int, int]]:
        x1, y1, x2, y2 = bbox
        return [(x1, y1), (x2, y1), (x2, y2), (x1, y2)]

    @staticmethod
    def _clamp_score(value: Any, default: float) -> float:
        try:
            score = float(value)
        except (TypeError, ValueError):
            score = default
        return round(max(0.0, min(1.0, score)), 6)

    @staticmethod
    def _clean_text(value: Any) -> str | None:
        text = str(value or "").strip()
        return text or None

    @staticmethod
    def _normalize_severity(value: Any, default: str) -> str:
        text = str(value or "").strip().upper()
        mapping = {
            "C0": "C0",
            "C1": "C1",
            "C2": "C2",
            "C3": "C3",
            "NONE": "C0",
            "MILD": "C1",
            "MODERATE": "C2",
            "SEVERE": "C3",
            "DEEP": "C3",
        }
        return mapping.get(text, default)

    @staticmethod
    def _severity_color(severity_code: str) -> tuple[int, int, int]:
        mapping = {
            "C0": (80, 160, 80),
            "C1": (235, 168, 52),
            "C2": (230, 111, 34),
            "C3": (200, 48, 48),
        }
        return mapping.get(severity_code, (230, 111, 34))
