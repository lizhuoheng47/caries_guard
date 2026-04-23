from __future__ import annotations

import os
import yaml
from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class CypherTemplate:
    code: str
    purpose: str
    required_entity_types: tuple[str, ...]
    return_schema: tuple[str, ...]
    parameter_name: str
    cypher: str
    version: str = "v1"
    enabled: bool = True
    owner: str = "unknown"
    test_case_id: str | None = None


class CypherTemplateRegistry:
    def __init__(self, config_path: str | None = None) -> None:
        if config_path is None:
            base_dir = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))
            config_path = os.path.join(base_dir, "config", "cypher_templates.yaml")

        self.config_path = config_path
        self.templates: dict[str, CypherTemplate] = {}
        self.load_templates()

    def load_templates(self) -> None:
        if not os.path.exists(self.config_path):
            return

        with open(self.config_path, "r", encoding="utf-8") as f:
            data = yaml.safe_load(f)
            if not data:
                return

            for item in data:
                self._register_item(item)

    def _register_item(self, item: dict[str, Any]) -> None:
        code = item.get("code")
        if not code:
            raise ValueError("Cypher template missing 'code'")

        if code in self.templates:
            raise ValueError(f"Duplicate Cypher template code: {code}")

        enabled = item.get("enabled", True)
        if not enabled:
            return

        cypher = item.get("cypher")
        if not cypher:
            raise ValueError(f"Template {code} missing 'cypher'")

        parameter_name = item.get("parameter_name")
        if not parameter_name:
            raise ValueError(f"Template {code} missing 'parameter_name'")

        return_schema = item.get("return_schema")
        if not return_schema:
            raise ValueError(f"Template {code} missing 'return_schema'")

        template = CypherTemplate(
            code=code,
            purpose=item.get("purpose", ""),
            required_entity_types=tuple(item.get("required_entity_types", [])),
            return_schema=tuple(return_schema),
            parameter_name=parameter_name,
            cypher=cypher.strip(),
            version=item.get("version", "v1"),
            enabled=enabled,
            owner=item.get("owner", "unknown"),
            test_case_id=item.get("test_case_id"),
        )
        self.templates[code] = template

    def get(self, code: str) -> CypherTemplate:
        if code not in self.templates:
            raise KeyError(f"Cypher template code not found or disabled: {code}")
        return self.templates[code]

    def codes(self) -> list[str]:
        return list(self.templates.keys())

    def matching_templates(self, entity_type_code: str) -> list[CypherTemplate]:
        return [
            template
            for template in self.templates.values()
            if template.required_entity_types and entity_type_code in template.required_entity_types
        ]
