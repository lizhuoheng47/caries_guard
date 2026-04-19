from __future__ import annotations


class CypherTemplateRegistry:
    TEMPLATES: dict[str, str] = {
        "RISK_TO_RECOMMENDATION": """
MATCH (r:RiskFactor {name: $risk})-[:SUGGESTS]->(a:Recommendation)
OPTIONAL MATCH (a)-[:SUPPORTED_BY]->(c:EvidenceChunk)-[:MENTIONS]->(d:EvidenceDocument)
RETURN r, a, c, d
LIMIT 20
""",
        "SEVERITY_TO_FOLLOWUP": """
MATCH (s:Severity {name: $severity})-[:REQUIRES_FOLLOWUP]->(f:FollowUpInterval)
OPTIONAL MATCH (f)-[:SUPPORTED_BY]->(c:EvidenceChunk)
RETURN s, f, c
LIMIT 20
""",
        "FINDING_TO_DISEASE": """
MATCH (f:ImagingFinding)-[:RELATED_TO]->(d:Disease)
WHERE f.name CONTAINS $keyword
RETURN f, d
LIMIT 20
""",
        "POPULATION_RULES": """
MATCH (p:Population {name: $population})-[:HAS_RISK_FACTOR|APPLIES_TO|SUGGESTS*1..2]-(n)
RETURN p, n
LIMIT 50
""",
    }

    def get(self, code: str) -> str:
        return self.TEMPLATES[code]

    def codes(self) -> list[str]:
        return list(self.TEMPLATES.keys())
