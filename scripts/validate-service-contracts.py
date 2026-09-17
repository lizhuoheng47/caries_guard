#!/usr/bin/env python3
"""Validate microservice catalog and versioned JSON contract metadata without dependencies."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "microservices" / "service-catalog.json"
CONTRACT_ROOT = ROOT / "microservices" / "contracts"
CONTRACT_NAME = re.compile(r"^[a-z0-9-]+\.v[1-9][0-9]*\.schema\.json$")


def load_object(path: Path) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise ValueError(f"{path}: root must be a JSON object")
    return value


def validate_catalog() -> list[str]:
    errors: list[str] = []
    catalog = load_object(CATALOG)
    services = catalog.get("services")
    if not isinstance(services, list) or not services:
        return [f"{CATALOG}: services must be a non-empty array"]
    names: set[str] = set()
    for index, service in enumerate(services):
        location = f"{CATALOG}: services[{index}]"
        if not isinstance(service, dict):
            errors.append(f"{location} must be an object")
            continue
        for field in ("name", "domain", "state", "sourcePaths", "dataOwner"):
            if not service.get(field):
                errors.append(f"{location}.{field} is required")
        name = service.get("name")
        if isinstance(name, str):
            if name in names:
                errors.append(f"{location}: duplicate service name {name}")
            names.add(name)
        for source_path in service.get("sourcePaths", []):
            if not (ROOT / source_path).exists():
                errors.append(f"{location}: source path does not exist: {source_path}")
    return errors


def validate_contracts() -> list[str]:
    errors: list[str] = []
    identifiers: set[str] = set()
    contracts = sorted(CONTRACT_ROOT.rglob("*.schema.json"))
    if not contracts:
        return [f"{CONTRACT_ROOT}: no versioned contracts found"]
    for path in contracts:
        if not CONTRACT_NAME.match(path.name):
            errors.append(f"{path}: filename must contain a major version")
        schema = load_object(path)
        for field in ("$schema", "$id", "title", "type", "properties"):
            if field not in schema:
                errors.append(f"{path}: missing {field}")
        identifier = schema.get("$id")
        if isinstance(identifier, str):
            if identifier in identifiers:
                errors.append(f"{path}: duplicate $id {identifier}")
            identifiers.add(identifier)
        if schema.get("type") != "object" or not isinstance(schema.get("properties"), dict):
            errors.append(f"{path}: root contract must be an object with properties")
        required = schema.get("required", [])
        if not isinstance(required, list):
            errors.append(f"{path}: required must be an array")
        elif isinstance(schema.get("properties"), dict):
            unknown = sorted(set(required) - set(schema["properties"]))
            if unknown:
                errors.append(f"{path}: required fields missing from properties: {unknown}")
    return errors


def main() -> int:
    try:
        errors = validate_catalog() + validate_contracts()
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        errors = [str(exc)]
    if errors:
        print("Microservice contract validation failed:")
        for error in errors:
            print(f"- {error}")
        return 1
    contract_count = len(list(CONTRACT_ROOT.rglob("*.schema.json")))
    service_count = len(load_object(CATALOG)["services"])
    print(f"Microservice bootstrap valid: {service_count} services, {contract_count} contracts")
    return 0


if __name__ == "__main__":
    sys.exit(main())
