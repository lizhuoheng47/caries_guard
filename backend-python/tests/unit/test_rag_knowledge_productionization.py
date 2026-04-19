from __future__ import annotations

from app.core.config import Settings
from app.infra.rerank.heuristic_rerank_provider import HeuristicRerankProvider
from app.services.citation_assembler import CitationAssembler
from app.services.concept_normalization_service import ConceptNormalizationService
from app.services.cypher_template_registry import CypherTemplateRegistry
from app.services.entity_extraction_service import EntityExtractionService
from app.services.fusion_service import FusionService
from app.services.rerank_service import RerankService


def test_concept_normalization_merges_aliases() -> None:
    service = ConceptNormalizationService()

    left = service.canonicalize("Disease", "龋齿")
    right = service.canonicalize("Disease", "龋病")

    assert left.canonical_name == "龋病"
    assert right.canonical_name == "龋病"
    assert left.concept_id == right.concept_id
    assert "龋齿" in left.aliases


def test_entity_extraction_returns_concept_ids_and_relations() -> None:
    service = EntityExtractionService()

    entities, relations, refs = service.extract(
        [
            {
                "id": 11,
                "chunk_text": "高糖饮食的儿童建议局部涂氟，并在3月后定期复查。",
            }
        ]
    )

    concept_ids = {entity["concept_id"] for entity in entities}
    relation_codes = {relation["relation_type_code"] for relation in relations}
    assert any(concept_id.startswith("riskfactor:") for concept_id in concept_ids)
    assert any(concept_id.startswith("recommendation:") for concept_id in concept_ids)
    assert "SUGGESTS" in relation_codes
    assert "APPLIES_TO" in relation_codes
    assert refs[0]["concept_ids"]


def test_cypher_registry_covers_required_graph_routes() -> None:
    registry = CypherTemplateRegistry()

    expected_codes = {
        "FINDING_TO_DISEASE",
        "DISEASE_TO_FINDING",
        "DISEASE_TO_RISK",
        "DISEASE_TO_RECOMMENDATION",
        "SEVERITY_TO_RECOMMENDATION",
        "SEVERITY_TO_FOLLOWUP",
        "POPULATION_TO_RECOMMENDATION",
        "POPULATION_TO_CONTRAINDICATION",
        "RECOMMENDATION_TO_EVIDENCE",
        "GUIDELINE_VERSION_TRACE",
        "SOURCE_CONFLICT_DETECTION",
    }

    assert expected_codes.issubset(set(registry.codes()))
    disease_templates = {template.code for template in registry.matching_templates("Disease")}
    assert "DISEASE_TO_FINDING" in disease_templates
    assert "DISEASE_TO_RECOMMENDATION" in disease_templates


def test_fusion_rerank_and_citation_assembly_keep_unified_provenance() -> None:
    settings = Settings()
    fusion = FusionService(settings)
    rerank = RerankService(HeuristicRerankProvider())
    assembler = CitationAssembler()
    lexical_hits = [
        {
            "evidence_id": "chunk-1",
            "chunk_id": 1,
            "doc_id": 101,
            "doc_no": "DOC-101",
            "doc_title": "Guide A",
            "doc_version": "v2.0",
            "chunk_text": "Fluoride toothpaste is recommended for high caries risk.",
            "score": 0.91,
            "channel": "LEXICAL",
            "source_authority_score": 1.0,
            "freshness_score": 0.9,
            "evidence_kind": "TEXT",
        }
    ]
    dense_hits = [
        {
            "evidence_id": "chunk-1",
            "chunk_id": 1,
            "doc_id": 101,
            "doc_no": "DOC-101",
            "doc_title": "Guide A",
            "doc_version": "v2.0",
            "chunk_text": "Fluoride toothpaste is recommended for high caries risk.",
            "score": 0.88,
            "channel": "DENSE",
            "source_authority_score": 1.0,
            "freshness_score": 0.9,
            "evidence_kind": "TEXT",
        }
    ]
    graph_hits = [
        {
            "evidence_id": "graph-DISEASE_TO_RECOMMENDATION-1",
            "graph_path_id": "DISEASE_TO_RECOMMENDATION-1",
            "doc_id": 101,
            "chunk_id": 1,
            "doc_title": "Guide A",
            "doc_version": "v2.0",
            "evidence_text": "disease: 龋病 | recommendation: 局部涂氟",
            "score": 0.7,
            "channel": "GRAPH",
            "cypher_template_code": "DISEASE_TO_RECOMMENDATION",
            "graph_confidence_score": 0.8,
            "provenance_path": [{"nodeType": "disease"}],
            "evidence_kind": "GRAPH",
        }
    ]

    fused = fusion.fuse(lexical_hits, dense_hits, graph_hits, top_k=5)
    reranked = rerank.rerank("What is recommended for high caries risk?", fused, top_k=5)
    citations = assembler.citations({"kb_code": "caries-default"}, reranked)
    evidence = assembler.evidence(reranked)

    assert reranked[0]["rerank_provider"] == "HEURISTIC"
    assert citations[0].knowledge_base_code == "caries-default"
    assert any(item.evidence_type == "GRAPH" for item in evidence)
    assert any(item.cypher_template_code == "DISEASE_TO_RECOMMENDATION" for item in evidence)
