from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class CypherTemplate:
    code: str
    purpose: str
    required_entity_types: tuple[str, ...]
    return_schema: tuple[str, ...]
    parameter_name: str
    cypher: str


class CypherTemplateRegistry:
    def __init__(self) -> None:
        self.templates = {
            template.code: template
            for template in (
                CypherTemplate(
                    code="RISK_TO_RECOMMENDATION",
                    purpose="Map risk factors to recommended actions and evidence.",
                    required_entity_types=("RiskFactor",),
                    return_schema=("risk", "recommendation", "chunk", "doc"),
                    parameter_name="risk",
                    cypher="""
MATCH (r:Concept {entityTypeCode: 'RiskFactor', name: $risk})-[:SUGGESTS|RECOMMENDED_FOR]->(a:Concept {entityTypeCode: 'Recommendation'})
OPTIONAL MATCH (c:EvidenceChunk)-[:MENTIONS]->(r)
OPTIONAL MATCH (c)-[:PART_OF]->(d:EvidenceDocument)
RETURN r AS risk, a AS recommendation, c AS chunk, d AS doc
LIMIT 20
""",
                ),
                CypherTemplate(
                    code="SEVERITY_TO_FOLLOWUP",
                    purpose="Find follow-up policies for a severity class.",
                    required_entity_types=("Severity",),
                    return_schema=("severity", "followUp", "chunk", "doc"),
                    parameter_name="severity",
                    cypher="""
MATCH (s:Concept {entityTypeCode: 'Severity', name: $severity})-[:REQUIRES_FOLLOWUP]->(f:Concept {entityTypeCode: 'FollowUpInterval'})
OPTIONAL MATCH (c:EvidenceChunk)-[:MENTIONS]->(f)
OPTIONAL MATCH (c)-[:PART_OF]->(d:EvidenceDocument)
RETURN s AS severity, f AS followUp, c AS chunk, d AS doc
LIMIT 20
""",
                ),
                CypherTemplate(
                    code="FINDING_TO_DISEASE",
                    purpose="Route imaging findings to disease concepts.",
                    required_entity_types=("ImagingFinding",),
                    return_schema=("finding", "disease", "chunk", "doc"),
                    parameter_name="finding",
                    cypher="""
MATCH (f:Concept {entityTypeCode: 'ImagingFinding', name: $finding})-[:INDICATES|RELATED_TO]->(d:Concept {entityTypeCode: 'Disease'})
OPTIONAL MATCH (c:EvidenceChunk)-[:MENTIONS]->(f)
OPTIONAL MATCH (c)-[:PART_OF]->(doc:EvidenceDocument)
RETURN f AS finding, d AS disease, c AS chunk, doc
LIMIT 20
""",
                ),
                CypherTemplate(
                    code="DISEASE_TO_FINDING",
                    purpose="Explain common findings for a disease.",
                    required_entity_types=("Disease",),
                    return_schema=("disease", "finding", "chunk", "doc"),
                    parameter_name="disease",
                    cypher="""
MATCH (d:Concept {entityTypeCode: 'Disease', name: $disease})-[:RELATED_TO|INDICATES]-(f:Concept {entityTypeCode: 'ImagingFinding'})
OPTIONAL MATCH (c:EvidenceChunk)-[:MENTIONS]->(f)
OPTIONAL MATCH (c)-[:PART_OF]->(doc:EvidenceDocument)
RETURN d AS disease, f AS finding, c AS chunk, doc
LIMIT 20
""",
                ),
                CypherTemplate(
                    code="DISEASE_TO_RISK",
                    purpose="Retrieve disease-associated risk factors.",
                    required_entity_types=("Disease",),
                    return_schema=("disease", "risk", "chunk", "doc"),
                    parameter_name="disease",
                    cypher="""
MATCH (d:Concept {entityTypeCode: 'Disease', name: $disease})-[:HAS_RISK_FACTOR]->(r:Concept {entityTypeCode: 'RiskFactor'})
OPTIONAL MATCH (c:EvidenceChunk)-[:MENTIONS]->(r)
OPTIONAL MATCH (c)-[:PART_OF]->(doc:EvidenceDocument)
RETURN d AS disease, r AS risk, c AS chunk, doc
LIMIT 20
""",
                ),
                CypherTemplate(
                    code="DISEASE_TO_RECOMMENDATION",
                    purpose="Retrieve disease-specific recommendations.",
                    required_entity_types=("Disease",),
                    return_schema=("disease", "recommendation", "chunk", "doc"),
                    parameter_name="disease",
                    cypher="""
MATCH (d:Concept {entityTypeCode: 'Disease', name: $disease})-[:RECOMMENDED_FOR]->(r:Concept {entityTypeCode: 'Recommendation'})
OPTIONAL MATCH (c:EvidenceChunk)-[:MENTIONS]->(r)
OPTIONAL MATCH (c)-[:PART_OF]->(doc:EvidenceDocument)
RETURN d AS disease, r AS recommendation, c AS chunk, doc
LIMIT 20
""",
                ),
                CypherTemplate(
                    code="SEVERITY_TO_RECOMMENDATION",
                    purpose="Find severity-specific recommendations.",
                    required_entity_types=("Severity",),
                    return_schema=("severity", "recommendation", "chunk", "doc"),
                    parameter_name="severity",
                    cypher="""
MATCH (s:Concept {entityTypeCode: 'Severity', name: $severity})-[:RECOMMENDED_FOR]->(r:Concept {entityTypeCode: 'Recommendation'})
OPTIONAL MATCH (c:EvidenceChunk)-[:MENTIONS]->(r)
OPTIONAL MATCH (c)-[:PART_OF]->(doc:EvidenceDocument)
RETURN s AS severity, r AS recommendation, c AS chunk, doc
LIMIT 20
""",
                ),
                CypherTemplate(
                    code="POPULATION_TO_RECOMMENDATION",
                    purpose="Find recommendation rules for a population.",
                    required_entity_types=("Population", "AgeGroup"),
                    return_schema=("population", "recommendation", "chunk", "doc"),
                    parameter_name="population",
                    cypher="""
MATCH (p:Concept {name: $population})-[:APPLIES_TO|RECOMMENDED_FOR]->(r:Concept {entityTypeCode: 'Recommendation'})
OPTIONAL MATCH (c:EvidenceChunk)-[:MENTIONS]->(p)
OPTIONAL MATCH (c)-[:PART_OF]->(doc:EvidenceDocument)
RETURN p AS population, r AS recommendation, c AS chunk, doc
LIMIT 20
""",
                ),
                CypherTemplate(
                    code="POPULATION_TO_CONTRAINDICATION",
                    purpose="Find contraindications for a population.",
                    required_entity_types=("Population", "AgeGroup"),
                    return_schema=("population", "contraindication", "chunk", "doc"),
                    parameter_name="population",
                    cypher="""
MATCH (p:Concept {name: $population})-[:CONTRAINDICATED_FOR]->(c1:Concept {entityTypeCode: 'Contraindication'})
OPTIONAL MATCH (c:EvidenceChunk)-[:MENTIONS]->(c1)
OPTIONAL MATCH (c)-[:PART_OF]->(doc:EvidenceDocument)
RETURN p AS population, c1 AS contraindication, c AS chunk, doc
LIMIT 20
""",
                ),
                CypherTemplate(
                    code="RECOMMENDATION_TO_EVIDENCE",
                    purpose="Backtrace a recommendation to chunk evidence.",
                    required_entity_types=("Recommendation",),
                    return_schema=("recommendation", "chunk", "doc"),
                    parameter_name="recommendation",
                    cypher="""
MATCH (r:Concept {entityTypeCode: 'Recommendation', name: $recommendation})<-[:MENTIONS]-(c:EvidenceChunk)-[:PART_OF]->(doc:EvidenceDocument)
RETURN r AS recommendation, c AS chunk, doc
LIMIT 20
""",
                ),
                CypherTemplate(
                    code="GUIDELINE_VERSION_TRACE",
                    purpose="Backtrace disease or finding mentions to evidence documents and versions.",
                    required_entity_types=("Disease", "ImagingFinding"),
                    return_schema=("concept", "chunk", "doc"),
                    parameter_name="concept",
                    cypher="""
MATCH (n:Concept {name: $concept})<-[:MENTIONS]-(c:EvidenceChunk)-[:PART_OF]->(doc:EvidenceDocument)
RETURN n AS concept, c AS chunk, doc
LIMIT 20
""",
                ),
                CypherTemplate(
                    code="TOOTH_POSITION_CONTEXT",
                    purpose="Find evidence connected to a tooth position mention.",
                    required_entity_types=("ToothPosition",),
                    return_schema=("position", "chunk", "doc"),
                    parameter_name="position",
                    cypher="""
MATCH (p:Concept {entityTypeCode: 'ToothPosition', name: $position})<-[:MENTIONS]-(c:EvidenceChunk)-[:PART_OF]->(doc:EvidenceDocument)
RETURN p AS position, c AS chunk, doc
LIMIT 20
""",
                ),
                CypherTemplate(
                    code="SOURCE_CONFLICT_DETECTION",
                    purpose="Detect conflicting document versions that mention the same concept.",
                    required_entity_types=("Disease", "Recommendation", "ImagingFinding"),
                    return_schema=("concept", "doc"),
                    parameter_name="concept",
                    cypher="""
MATCH (n:Concept {name: $concept})<-[:MENTIONS]-(c:EvidenceChunk)-[:PART_OF]->(doc:EvidenceDocument)
WITH n, collect(DISTINCT doc) AS docs
UNWIND docs AS doc
RETURN n AS concept, doc
LIMIT 20
""",
                ),
                CypherTemplate(
                    code="KEYWORD_EVIDENCE_TRACE",
                    purpose="Fallback keyword evidence traversal for graph recall.",
                    required_entity_types=tuple(),
                    return_schema=("concept", "chunk", "doc"),
                    parameter_name="keyword",
                    cypher="""
MATCH (n:Concept)<-[:MENTIONS]-(c:EvidenceChunk)-[:PART_OF]->(doc:EvidenceDocument)
WHERE n.name CONTAINS $keyword OR any(alias IN coalesce(n.aliases, []) WHERE alias CONTAINS $keyword)
RETURN n AS concept, c AS chunk, doc
LIMIT 20
""",
                ),
            )
        }

    def get(self, code: str) -> CypherTemplate:
        return self.templates[code]

    def codes(self) -> list[str]:
        return list(self.templates.keys())

    def matching_templates(self, entity_type_code: str) -> list[CypherTemplate]:
        return [
            template
            for template in self.templates.values()
            if template.required_entity_types and entity_type_code in template.required_entity_types
        ]
