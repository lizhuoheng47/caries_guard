import json

from app.schemas.base import dump_camel
from app.schemas.callback import AnalysisCallbackPayload, Summary, VisualAsset


def test_callback_payload_uses_camel_case_only() -> None:
    payload = AnalysisCallbackPayload(
        task_no="TASK202604150001",
        task_status_code="SUCCESS",
        model_version="caries-v1",
        summary=Summary(),
        visual_assets=[
            VisualAsset(
                asset_type_code="MASK",
                bucket_name="caries-visual",
                object_key="visual/2026/04/15/CASE/mask_90001_16.png",
                related_image_id=90001,
                tooth_code="16",
            )
        ],
        trace_id="trace-1",
    )

    body = dump_camel(payload)
    body_text = json.dumps(body)

    assert "taskNo" in body
    assert "task_no" not in body_text
    assert "visualAssets" in body
    assert "visual_assets" not in body_text

