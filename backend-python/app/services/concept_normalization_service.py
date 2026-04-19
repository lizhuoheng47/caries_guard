from __future__ import annotations

import re
from dataclasses import dataclass


@dataclass(frozen=True)
class CanonicalConcept:
    concept_id: str
    canonical_name: str
    normalized_name: str
    entity_type_code: str
    aliases: list[str]
    confidence_score: float


class ConceptNormalizationService:
    def __init__(self) -> None:
        self._alias_groups: dict[str, dict[str, list[str]]] = {
            "Disease": {
                "龋病": ["龋病", "龋齿", "龋坏"],
                "早期龋": ["早期龋", "浅龋", "初期龋"],
                "中龋": ["中龋"],
                "深龋": ["深龋"],
            },
            "ImagingFinding": {
                "邻面龋影": ["邻面龋影", "邻面透射影"],
                "咬合面龋影": ["咬合面龋影", "窝沟龋影"],
                "脱矿": ["脱矿", "脱矿影"],
            },
            "Severity": {
                "低风险": ["低风险", "低危"],
                "中风险": ["中风险", "中危"],
                "高风险": ["高风险", "高危"],
                "轻度": ["轻度"],
                "中度": ["中度"],
                "重度": ["重度"],
            },
            "RiskFactor": {
                "高糖饮食": ["高糖饮食", "高糖摄入", "频繁糖摄入"],
                "夜奶": ["夜奶", "夜间喂养"],
                "口腔卫生差": ["口腔卫生差", "刷牙不足"],
                "菌斑": ["菌斑", "牙菌斑"],
                "低氟": ["低氟", "氟暴露不足"],
            },
            "Recommendation": {
                "定期复查": ["定期复查", "规律复查"],
                "局部涂氟": ["局部涂氟", "涂氟"],
                "窝沟封闭": ["窝沟封闭"],
                "控制糖摄入": ["控制糖摄入", "减少糖摄入"],
                "规范刷牙": ["规范刷牙", "正确刷牙"],
                "及时复诊": ["及时复诊", "尽快复诊"],
            },
            "Population": {
                "儿童": ["儿童", "小儿"],
                "学龄前儿童": ["学龄前儿童", "幼儿"],
                "青少年": ["青少年", "少年"],
                "成人": ["成人"],
                "孕妇": ["孕妇"],
            },
            "AgeGroup": {
                "婴幼儿": ["婴幼儿"],
                "学龄前": ["学龄前", "学龄前儿童"],
                "学龄期": ["学龄期"],
                "青少年": ["青少年"],
                "成人": ["成人"],
            },
        }
        self._reverse_aliases: dict[str, dict[str, str]] = {}
        for entity_type, groups in self._alias_groups.items():
            entity_map: dict[str, str] = {}
            for canonical_name, aliases in groups.items():
                all_aliases = {canonical_name, *aliases}
                for alias in all_aliases:
                    entity_map[self.normalize(alias)] = canonical_name
            self._reverse_aliases[entity_type] = entity_map

    def canonicalize(self, entity_type_code: str, raw_name: str, confidence_score: float = 0.85) -> CanonicalConcept:
        normalized_name = self.normalize(raw_name)
        canonical_name = self._reverse_aliases.get(entity_type_code, {}).get(normalized_name, raw_name.strip())
        canonical_normalized = self.normalize(canonical_name)
        aliases = self.aliases_for(entity_type_code, canonical_name)
        concept_key = f"{entity_type_code}:{canonical_normalized}"
        concept_id = re.sub(r"[^a-z0-9:_-]+", "-", concept_key.lower())
        return CanonicalConcept(
            concept_id=concept_id,
            canonical_name=canonical_name,
            normalized_name=canonical_normalized,
            entity_type_code=entity_type_code,
            aliases=aliases,
            confidence_score=confidence_score,
        )

    def aliases_for(self, entity_type_code: str, canonical_name: str) -> list[str]:
        groups = self._alias_groups.get(entity_type_code, {})
        aliases = groups.get(canonical_name, [])
        all_names = {canonical_name, *aliases}
        return sorted(all_names)

    def match_terms(self, query: str) -> list[CanonicalConcept]:
        lowered = query or ""
        results: dict[str, CanonicalConcept] = {}
        for entity_type_code, groups in self._alias_groups.items():
            for canonical_name, aliases in groups.items():
                for alias in {canonical_name, *aliases}:
                    if alias and alias in lowered:
                        concept = self.canonicalize(entity_type_code, alias, confidence_score=0.9)
                        results[concept.concept_id] = concept
        return list(results.values())

    @staticmethod
    def normalize(value: str) -> str:
        return re.sub(r"\s+", "", value or "").strip().lower()
