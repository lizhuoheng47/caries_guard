import pytest
import os
import yaml
from app.services.cypher_template_registry import CypherTemplateRegistry

@pytest.fixture
def temp_yaml(tmp_path):
    d = tmp_path / "config"
    d.mkdir()
    f = d / "cypher_templates.yaml"
    content = [
        {
            "code": "TEST_TEMPLATE",
            "purpose": "Testing",
            "required_entity_types": ["TestType"],
            "return_schema": ["a", "b"],
            "parameter_name": "p",
            "cypher": "MATCH (n) RETURN n",
            "enabled": True
        },
        {
            "code": "DISABLED_TEMPLATE",
            "purpose": "Testing disabled",
            "required_entity_types": ["TestType"],
            "return_schema": ["a"],
            "parameter_name": "p",
            "cypher": "MATCH (n) RETURN n",
            "enabled": False
        }
    ]
    f.write_text(yaml.dump(content))
    return str(f)

def test_load_yaml(temp_yaml):
    registry = CypherTemplateRegistry(config_path=temp_yaml)
    assert "TEST_TEMPLATE" in registry.codes()
    assert "DISABLED_TEMPLATE" not in registry.codes()
    
    template = registry.get("TEST_TEMPLATE")
    assert template.parameter_name == "p"
    assert template.required_entity_types == ("TestType",)

def test_duplicate_code(tmp_path):
    f = tmp_path / "dup.yaml"
    content = [
        {"code": "DUP", "cypher": "A", "parameter_name": "p", "return_schema": ["s"]},
        {"code": "DUP", "cypher": "B", "parameter_name": "p", "return_schema": ["s"]}
    ]
    f.write_text(yaml.dump(content))
    
    with pytest.raises(ValueError, match="Duplicate Cypher template code"):
        CypherTemplateRegistry(config_path=str(f))

def test_missing_fields(tmp_path):
    f = tmp_path / "missing.yaml"
    content = [{"code": "MISSING_CYPHER", "parameter_name": "p", "return_schema": ["s"]}]
    f.write_text(yaml.dump(content))
    
    with pytest.raises(ValueError, match="missing 'cypher'"):
        CypherTemplateRegistry(config_path=str(f))

def test_matching_templates(temp_yaml):
    registry = CypherTemplateRegistry(config_path=temp_yaml)
    matches = registry.matching_templates("TestType")
    assert len(matches) == 1
    assert matches[0].code == "TEST_TEMPLATE"
    
    no_matches = registry.matching_templates("UnknownType")
    assert len(no_matches) == 0
