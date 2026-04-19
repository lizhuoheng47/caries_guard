from __future__ import annotations

import hashlib
import json
import re
import uuid
from pathlib import Path
from typing import Any

from app.core.config import Settings
from app.infra.storage.minio_client import MinioStorageClient
from app.repositories.graph_repository import GraphRepository
from app.repositories.knowledge_repository import KnowledgeRepository
from app.schemas.rag import KnowledgeDocumentRequest, KnowledgeRebuildRequest
from app.services.chunk_build_service import ChunkBuildService
from app.services.document_parse_service import DocumentParseService
from app.services.entity_extraction_service import EntityExtractionService
from app.services.graph_upsert_service import GraphUpsertService
from app.services.open_search_index_service import OpenSearchIndexService


class KnowledgeService:
    def __init__(
        self,
        settings: Settings,
        repository: KnowledgeRepository,
        storage: MinioStorageClient,
        parser_service: DocumentParseService,
        chunk_build_service: ChunkBuildService,
        entity_extraction_service: EntityExtractionService,
        open_search_index_service: OpenSearchIndexService,
        graph_upsert_service: GraphUpsertService,
        graph_repository: GraphRepository,
    ) -> None:
        self.settings = settings
        self.repository = repository
        self.storage = storage
        self.parser_service = parser_service
        self.chunk_build_service = chunk_build_service
        self.entity_extraction_service = entity_extraction_service
        self.open_search_index_service = open_search_index_service
        self.graph_upsert_service = graph_upsert_service
        self.graph_repository = graph_repository

    def import_document(self, request: KnowledgeDocumentRequest) -> dict[str, Any]:
        kb = self.ensure_knowledge_base(
            kb_code=request.kb_code,
            kb_name=request.kb_name,
            kb_type_code=request.kb_type_code,
            knowledge_version=request.doc_version,
            org_id=request.org_id,
        )
        file_name = f"{request.doc_no or uuid.uuid4().hex}.md"
        payload = self._ingest_text_payload(
            kb=kb,
            doc_title=request.doc_title,
            doc_source_code=request.doc_source_code,
            source_uri=request.source_uri,
            content_text=request.content_text,
            doc_no=request.doc_no,
            version_no=request.doc_version,
            org_id=request.org_id,
            operator_id=None,
            file_name=file_name,
            trace_id=request.trace_id,
        )
        if request.review_status_code in {"APPROVED", "PUBLISHED"}:
            self.submit_review(payload["docId"], payload["versionNo"], None)
            self.approve(payload["docId"], payload["versionNo"], None, request.org_id, "Imported approved content")
        if request.review_status_code == "PUBLISHED":
            self.publish(payload["docId"], payload["versionNo"], None, request.org_id, "Imported and published")
        return payload

    def upload_document(
        self,
        *,
        file_name: str,
        content_type: str | None,
        data: bytes,
        kb_code: str | None,
        kb_name: str | None,
        kb_type_code: str,
        doc_title: str | None,
        doc_source_code: str,
        source_uri: str | None,
        doc_no: str | None,
        doc_version: str | None,
        change_summary: str | None,
        org_id: int | None,
        operator_id: int | None,
        trace_id: str | None,
    ) -> dict[str, Any]:
        kb = self.ensure_knowledge_base(
            kb_code=kb_code,
            kb_name=kb_name,
            kb_type_code=kb_type_code,
            knowledge_version=doc_version,
            org_id=org_id,
        )
        self.storage.ensure_bucket(self.settings.bucket_knowledge)
        object_key = f"kb/{kb['kb_code']}/{uuid.uuid4().hex}/{file_name}"
        upload = self.storage.upload_bytes(
            bucket_name=self.settings.bucket_knowledge,
            object_key=object_key,
            data=data,
            content_type=content_type or "application/octet-stream",
            file_name=file_name,
        )
        md5 = hashlib.md5(data).hexdigest()
        source_file = self.repository.create_source_file(
            kb_id=kb["id"],
            doc_id=None,
            bucket_name=upload.bucket_name,
            object_key=upload.object_key,
            file_name=file_name,
            mime_type=content_type,
            file_size_bytes=upload.size,
            md5=md5,
            source_type_code="UPLOAD",
            org_id=org_id,
            uploaded_by=operator_id,
        )
        ingest_job = self.repository.create_ingest_job(
            kb_id=kb["id"],
            doc_id=None,
            source_file_id=source_file["id"],
            org_id=org_id,
            created_by=operator_id,
            trace_id=trace_id,
        )
        try:
            self.repository.record_ingest_step(ingest_job["id"], "PARSE", 1, "RUNNING")
            parsed = self.parser_service.parse_bytes(file_name, data)
            artifact_key = f"kb/{kb['kb_code']}/{source_file['source_file_no']}/parsed.json"
            self.storage.upload_bytes(
                bucket_name=self.settings.bucket_knowledge,
                object_key=artifact_key,
                data=json.dumps(parsed, ensure_ascii=False, indent=2).encode("utf-8"),
                content_type="application/json",
                file_name="parsed.json",
            )
            self.repository.save_parse_result(
                source_file_id=source_file["id"],
                parse_status_code="SUCCESS",
                normalized_markdown=parsed["normalized_markdown"],
                structured_json=parsed["structured_json"],
                section_tree=parsed["section_tree"],
                table_json=parsed["table_json"],
                metadata_json=parsed["metadata_json"],
                artifact_bucket_name=self.settings.bucket_knowledge,
                artifact_object_key=artifact_key,
            )
            result = self._ingest_parsed_content(
                kb=kb,
                doc_title=doc_title or Path(file_name).stem,
                doc_source_code=doc_source_code,
                source_uri=source_uri,
                doc_no=doc_no,
                version_no=doc_version or "v1.0",
                change_summary=change_summary,
                parsed=parsed,
                source_file_id=source_file["id"],
                org_id=org_id,
                operator_id=operator_id,
                trace_id=trace_id,
            )
            self.repository.mark_source_file(source_file["id"], "PARSED", doc_id=result["docId"], kb_id=kb["id"])
            self.repository.finish_ingest_job(ingest_job["id"], "SUCCESS", "REVIEW_PENDING", doc_id=result["docId"])
            result["ingestJobNo"] = ingest_job["ingest_job_no"]
            result["sourceFileNo"] = source_file["source_file_no"]
            return result
        except Exception as exc:
            self.repository.mark_source_file(source_file["id"], "FAILED")
            self.repository.save_parse_result(
                source_file_id=source_file["id"],
                parse_status_code="FAILED",
                normalized_markdown=None,
                structured_json=None,
                section_tree=None,
                table_json=None,
                metadata_json=None,
                artifact_bucket_name=None,
                artifact_object_key=None,
                error_message=str(exc),
            )
            self.repository.finish_ingest_job(ingest_job["id"], "FAILED", "FAILED", error_message=str(exc))
            raise

    def update_document(
        self,
        doc_id: int,
        *,
        doc_title: str | None,
        doc_source_code: str | None,
        source_uri: str | None,
        content_text: str,
        change_summary: str | None,
        operator_id: int | None,
        trace_id: str | None,
    ) -> dict[str, Any]:
        document = self.repository.get_document(doc_id)
        if document is None:
            raise ValueError(f"document {doc_id} not found")
        if doc_title or doc_source_code or source_uri:
            document = self.repository.update_document_metadata(
                doc_id,
                doc_title=doc_title,
                doc_source_code=doc_source_code,
                source_uri=source_uri,
                updated_by=operator_id,
            )
        kb = self.repository.get_knowledge_base(kb_id=document["kb_id"])
        return self._ingest_text_payload(
            kb=kb,
            doc_title=document["doc_title"],
            doc_source_code=document["doc_source_code"],
            source_uri=document.get("source_uri"),
            content_text=content_text,
            doc_no=document["doc_no"],
            version_no=self._next_version(document.get("current_version_no") or document.get("doc_version")),
            org_id=document.get("org_id"),
            operator_id=operator_id,
            file_name=f"{document['doc_no']}.md",
            trace_id=trace_id,
            doc_id=doc_id,
            change_summary=change_summary,
        )

    def submit_review(self, doc_id: int, version_no: str, reviewer_id: int | None) -> None:
        self.repository.submit_review(doc_id, version_no, reviewer_id)

    def approve(self, doc_id: int, version_no: str, reviewer_id: int | None, org_id: int | None, comment: str | None) -> None:
        self.repository.record_review(doc_id, version_no, "APPROVE", comment, reviewer_id, org_id)

    def reject(self, doc_id: int, version_no: str, reviewer_id: int | None, org_id: int | None, comment: str | None) -> None:
        self.repository.record_review(doc_id, version_no, "REJECT", comment, reviewer_id, org_id)

    def publish(self, doc_id: int, version_no: str, operator_id: int | None, org_id: int | None, comment: str | None) -> None:
        document = self.repository.get_document(doc_id)
        if document is None:
            raise ValueError(f"document {doc_id} not found")
        version = self.repository.get_document_version(doc_id, version_no)
        if version is None:
            raise ValueError(f"document version {version_no} not found")
        self.repository.publish_version(doc_id, version_no, operator_id, org_id, "PUBLISH", comment)
        kb = self.repository.get_knowledge_base(kb_id=document["kb_id"])
        chunks = self.repository.list_chunks_for_version(doc_id, version_no)
        for chunk in chunks:
            chunk["publish_status"] = "PUBLISHED"
        self.open_search_index_service.delete_document_chunks(doc_id)
        self.open_search_index_service.index_document_version(kb["kb_code"], document, version, chunks)

    def rollback(self, doc_id: int, version_no: str, operator_id: int | None, org_id: int | None, comment: str | None) -> None:
        self.repository.publish_version(doc_id, version_no, operator_id, org_id, "ROLLBACK", comment)
        self.publish(doc_id, version_no, operator_id, org_id, comment)

    def get_document_detail(self, doc_id: int) -> dict[str, Any]:
        document = self.repository.get_document(doc_id)
        if document is None:
            raise ValueError(f"document {doc_id} not found")
        document["versions"] = self.repository.list_document_versions(doc_id)
        current_version_no = document.get("current_version_no") or document.get("doc_version")
        document["currentVersion"] = self.repository.get_document_version(doc_id, current_version_no)
        document["embeddingProvider"] = self.open_search_index_service.embedding_provider.metadata.provider
        document["embeddingModel"] = self.open_search_index_service.embedding_provider.metadata.model
        document["embeddingVersion"] = self.open_search_index_service.embedding_provider.metadata.version
        return document

    def list_documents(self, kb_code: str | None, org_id: int | None, keyword: str | None = None) -> list[dict[str, Any]]:
        kb_id = None
        if kb_code:
            kb = self.repository.get_knowledge_base(kb_code=kb_code)
            kb_id = kb["id"] if kb else None
        return self.repository.list_documents(kb_id=kb_id, org_id=org_id, keyword=keyword)

    def overview(self, kb_code: str | None, org_id: int | None) -> dict[str, Any]:
        kb_id = None
        if kb_code:
            kb = self.repository.get_knowledge_base(kb_code=kb_code)
            kb_id = kb["id"] if kb else None
        overview = self.repository.overview(kb_id=kb_id, org_id=org_id)
        overview.update(self.graph_repository.entity_counts())
        overview.update(
            {
                "embeddingProvider": self.open_search_index_service.embedding_provider.metadata.provider,
                "embeddingModel": self.open_search_index_service.embedding_provider.metadata.model,
                "embeddingVersion": self.open_search_index_service.embedding_provider.metadata.version,
            }
        )
        return overview

    def list_ingest_jobs(self, org_id: int | None) -> list[dict[str, Any]]:
        return self.repository.list_ingest_jobs(org_id=org_id)

    def list_rebuild_jobs(self, kb_code: str | None, org_id: int | None) -> list[dict[str, Any]]:
        kb_id = None
        if kb_code:
            kb = self.repository.get_knowledge_base(kb_code=kb_code)
            kb_id = kb["id"] if kb else None
        return self.repository.list_rebuild_jobs(kb_id=kb_id, org_id=org_id)

    def graph_statistics(self, kb_code: str | None, org_id: int | None) -> dict[str, Any]:
        overview = self.overview(kb_code=kb_code, org_id=org_id)
        return {
            "kbCode": kb_code or self.settings.rag_default_kb_code,
            "entityCount": overview.get("entityCount", 0),
            "relationCount": overview.get("relationCount", 0),
            "embeddingProvider": self.open_search_index_service.embedding_provider.metadata.provider,
            "embeddingModel": self.open_search_index_service.embedding_provider.metadata.model,
            "embeddingVersion": self.open_search_index_service.embedding_provider.metadata.version,
        }

    def rebuild(self, request: KnowledgeRebuildRequest) -> dict[str, Any]:
        kb = self.ensure_knowledge_base(
            kb_code=request.kb_code,
            kb_name=request.kb_name,
            kb_type_code=request.kb_type_code,
            knowledge_version=request.knowledge_version,
            org_id=request.org_id,
        )
        from app.repositories.rag_repository import RagRepository

        rag_repository = RagRepository()
        job = rag_repository.create_rebuild_job(
            kb_id=kb["id"],
            knowledge_version=kb["knowledge_version"],
            vector_store_path=kb.get("vector_store_path"),
            org_id=request.org_id,
        )
        try:
            documents = self.repository.list_documents(kb_id=kb["id"], org_id=request.org_id)
            report: list[dict[str, Any]] = []
            total_chunks = 0
            for document in documents:
                version_no = document.get("published_version_no")
                if not version_no:
                    continue
                version = self.repository.get_document_version(document["id"], version_no)
                rebuilt = self._rebuild_document_version(
                    kb=kb,
                    document=document,
                    version=version,
                    request=request,
                )
                total_chunks += rebuilt["chunkCount"]
                report.append(rebuilt)
            finished = rag_repository.finish_rebuild_job(job["id"], "SUCCESS", total_chunks)
            return {
                "rebuildJobNo": finished["rebuild_job_no"],
                "rebuildStatusCode": finished["rebuild_status_code"],
                "kbCode": kb["kb_code"],
                "chunkCount": total_chunks,
                "documents": report,
                "graphStats": self.graph_statistics(kb["kb_code"], request.org_id),
            }
        except Exception as exc:
            rag_repository.finish_rebuild_job(job["id"], "FAILED", 0, str(exc))
            raise

    def ensure_knowledge_base(
        self,
        kb_code: str | None = None,
        kb_name: str | None = None,
        kb_type_code: str = "PATIENT_GUIDE",
        knowledge_version: str | None = None,
        org_id: int | None = None,
    ) -> dict[str, Any]:
        code = kb_code or self.settings.rag_default_kb_code
        version = knowledge_version or self.settings.rag_knowledge_version
        vector_store_path = None if self.settings.rag_vector_store_type == "OPENSEARCH" else str(Path(self.settings.rag_index_dir) / f"{self._safe_name(code)}.json")
        return self.repository.ensure_knowledge_base(
            kb_code=code,
            kb_name=kb_name or self.settings.rag_default_kb_name,
            kb_type_code=kb_type_code,
            knowledge_version=version,
            embedding_model=self.settings.rag_embedding_model,
            vector_store_type_code=self.settings.rag_vector_store_type,
            vector_store_path=vector_store_path,
            org_id=org_id,
        )

    def _ingest_text_payload(
        self,
        *,
        kb: dict[str, Any],
        doc_title: str,
        doc_source_code: str,
        source_uri: str | None,
        content_text: str,
        doc_no: str | None,
        version_no: str,
        org_id: int | None,
        operator_id: int | None,
        file_name: str,
        trace_id: str | None,
        doc_id: int | None = None,
        change_summary: str | None = None,
    ) -> dict[str, Any]:
        parsed = self.parser_service.parse_bytes(file_name, content_text.encode("utf-8"))
        return self._ingest_parsed_content(
            kb=kb,
            doc_title=doc_title,
            doc_source_code=doc_source_code,
            source_uri=source_uri,
            doc_no=doc_no,
            version_no=version_no,
            change_summary=change_summary,
            parsed=parsed,
            source_file_id=None,
            org_id=org_id,
            operator_id=operator_id,
            trace_id=trace_id,
            doc_id=doc_id,
        )

    def _ingest_parsed_content(
        self,
        *,
        kb: dict[str, Any],
        doc_title: str,
        doc_source_code: str,
        source_uri: str | None,
        doc_no: str | None,
        version_no: str,
        change_summary: str | None,
        parsed: dict[str, Any],
        source_file_id: int | None,
        org_id: int | None,
        operator_id: int | None,
        trace_id: str | None,
        doc_id: int | None = None,
    ) -> dict[str, Any]:
        if doc_id is None:
            document = self.repository.create_document(
                kb_id=kb["id"],
                doc_title=doc_title,
                doc_no=doc_no,
                doc_source_code=doc_source_code,
                source_uri=source_uri,
                content_text=parsed["normalized_markdown"],
                doc_version=version_no,
                org_id=org_id,
                source_file_id=source_file_id,
                created_by=operator_id,
            )
        else:
            document = self.repository.get_document(doc_id)
        version = self.repository.create_document_version(
            doc_id=document["id"],
            version_no=version_no,
            parent_version_no=document.get("current_version_no"),
            normalized_content=parsed["normalized_markdown"],
            structured_json=parsed["structured_json"],
            section_tree=parsed["section_tree"],
            table_json=parsed["table_json"],
            metadata_json=parsed["metadata_json"],
            change_summary=change_summary,
            source_file_id=source_file_id,
            org_id=org_id,
            created_by=operator_id,
        )
        chunks = self.chunk_build_service.build(
            parsed["normalized_markdown"],
            doc_title=document["doc_title"],
            doc_source_code=document["doc_source_code"],
            source_uri=document.get("source_uri"),
            org_id=org_id,
        )
        stored_chunks = self.repository.replace_chunks(
            kb_id=kb["id"],
            doc_id=document["id"],
            version_no=version_no,
            chunks=chunks,
            embedding_model=self.settings.rag_embedding_model,
            vector_store_path=kb.get("vector_store_path"),
            publish_status="INDEXED",
        )
        self.open_search_index_service.index_document_version(kb["kb_code"], document, version, stored_chunks)
        chunk_entities, relations, chunk_refs = self.entity_extraction_service.extract(stored_chunks)
        entities, stored_relations = self.graph_upsert_service.sync_document_graph(
            doc_id=document["id"],
            doc_title=document["doc_title"],
            version_no=version_no,
            chunk_entities=chunk_entities,
            relations=relations,
            org_id=org_id,
            created_by=operator_id,
        )
        self.repository.update_chunk_graph_refs(chunk_refs)
        self.repository.create_graph_sync_log(
            doc_id=document["id"],
            version_no=version_no,
            sync_status_code="SUCCESS",
            entity_count=len(entities),
            relation_count=len(stored_relations),
            trace_id=trace_id,
            error_message=None,
            org_id=org_id,
            created_by=operator_id,
        )
        self.submit_review(document["id"], version_no, operator_id)
        return {
            "kbCode": kb["kb_code"],
            "kbId": kb["id"],
            "docId": document["id"],
            "docNo": document["doc_no"],
            "docTitle": document["doc_title"],
            "versionNo": version_no,
            "reviewStatusCode": "REVIEW_PENDING",
            "publishStatusCode": "DRAFT",
            "chunkCount": len(stored_chunks),
            "entityCount": len(entities),
            "relationCount": len(stored_relations),
        }

    def _rebuild_document_version(
        self,
        *,
        kb: dict[str, Any],
        document: dict[str, Any],
        version: dict[str, Any],
        request: KnowledgeRebuildRequest,
    ) -> dict[str, Any]:
        content_text = version.get("normalized_content") or document.get("content_text") or ""
        parsed = {
            "normalized_markdown": content_text,
            "structured_json": version.get("structured_json"),
            "section_tree": version.get("section_tree"),
            "table_json": version.get("table_json"),
            "metadata_json": version.get("metadata_json"),
        }
        if request.rebuild_parse and self.settings.rag_rebuild_parse_enabled:
            parsed = self.parser_service.parse_bytes(f"{document['doc_no']}.md", content_text.encode("utf-8"))
        chunks = self.chunk_build_service.build(
            parsed["normalized_markdown"],
            doc_title=document["doc_title"],
            doc_source_code=document["doc_source_code"],
            source_uri=document.get("source_uri"),
            org_id=document.get("org_id"),
        )
        stored_chunks = self.repository.replace_chunks(
            kb_id=kb["id"],
            doc_id=document["id"],
            version_no=version["version_no"],
            chunks=chunks,
            embedding_model=self.settings.rag_embedding_model,
            vector_store_path=kb.get("vector_store_path"),
            publish_status="PUBLISHED",
        )
        indexing_report = {"indexedChunkCount": 0}
        if request.rebuild_lexical or request.rebuild_dense:
            self.open_search_index_service.delete_document_chunks(document["id"])
            indexing_report = self.open_search_index_service.index_document_version(kb["kb_code"], document, version, stored_chunks)
        entity_count = 0
        relation_count = 0
        if request.cleanup_stale and request.rebuild_graph and self.settings.rag_rebuild_cleanup_enabled:
            self.graph_repository.delete_document_graph(document["id"])
            self.graph_upsert_service.cleanup_document_graph(document["id"])
        if request.rebuild_graph and self.settings.rag_rebuild_graph_enabled:
            chunk_entities, relations, chunk_refs = self.entity_extraction_service.extract(stored_chunks)
            entities, stored_relations = self.graph_upsert_service.sync_document_graph(
                doc_id=document["id"],
                doc_title=document["doc_title"],
                version_no=version["version_no"],
                chunk_entities=chunk_entities,
                relations=relations,
                org_id=document.get("org_id"),
                created_by=document.get("updated_by"),
            )
            self.repository.update_chunk_graph_refs(chunk_refs)
            entity_count = len(entities)
            relation_count = len(stored_relations)
        return {
            "docId": document["id"],
            "docNo": document["doc_no"],
            "versionNo": version["version_no"],
            "chunkCount": len(stored_chunks),
            "entityCount": entity_count,
            "relationCount": relation_count,
            "embeddingProvider": indexing_report.get("embeddingProvider"),
            "embeddingModel": indexing_report.get("embeddingModel"),
            "embeddingVersion": indexing_report.get("embeddingVersion"),
        }

    @staticmethod
    def _safe_name(value: str) -> str:
        cleaned = re.sub(r"[^A-Za-z0-9._-]+", "-", value.strip())
        return cleaned.strip("-") or "default"

    @staticmethod
    def _next_version(version_no: str) -> str:
        match = re.match(r"v?(\d+)(?:\.(\d+))?$", version_no or "v1.0")
        if not match:
            return f"{version_no}-next"
        major = int(match.group(1))
        minor = int(match.group(2) or 0) + 1
        return f"v{major}.{minor}"
