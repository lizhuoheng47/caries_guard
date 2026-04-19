import json

from app.schemas.base import dump_camel
from app.schemas.callback import AnalysisCallbackPayload, Summary, VisualAsset
from app.schemas.rag import KnowledgeDocumentUpdateRequest, KnowledgeVersionActionRequest, RagEvalRunRequest


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


def test_rag_admin_requests_use_camel_case_aliases() -> None:
    update_payload = KnowledgeDocumentUpdateRequest(
        trace_id="trace-update-1",
        doc_title="Updated title",
        content_text="normalized content",
        change_summary="tighten contract",
        operator_id=7,
    )
    version_payload = KnowledgeVersionActionRequest(
        trace_id="trace-action-1",
        version_no="v1.2",
        comment="approve after review",
        reviewer_id=9,
        org_id=3,
    )
    eval_payload = RagEvalRunRequest(
        trace_id="trace-eval-1",
        dataset_id=11,
        operator_id=5,
        org_id=3,
    )

    update_body = dump_camel(update_payload)
    version_body = dump_camel(version_payload)
    eval_body = dump_camel(eval_payload)
    serialized = json.dumps({"update": update_body, "version": version_body, "eval": eval_body})

    assert update_body["traceId"] == "trace-update-1"
    assert update_body["contentText"] == "normalized content"
    assert version_body["versionNo"] == "v1.2"
    assert eval_body["datasetId"] == 11
    assert "trace_id" not in serialized
    assert "content_text" not in serialized
    assert "version_no" not in serialized
    assert "dataset_id" not in serialized
