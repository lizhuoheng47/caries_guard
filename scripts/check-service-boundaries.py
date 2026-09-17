#!/usr/bin/env python3
"""Fail when new cross-domain coupling is added to the first extraction candidate."""

from __future__ import annotations

import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEBT_FILE = ROOT / "microservices" / "architecture-debt.json"
POM_FILE = ROOT / "backend-java" / "caries-analysis" / "pom.xml"
CROSS_DOMAIN = re.compile(
    r"^import (com\.cariesguard\.(?:patient|image|system|report|followup|dashboard)\.[^;]+);$",
    re.MULTILINE,
)


def actual_imports(source_root: Path) -> dict[str, list[str]]:
    result: dict[str, list[str]] = {}
    for path in sorted(source_root.rglob("*.java")):
        imports = sorted(CROSS_DOMAIN.findall(path.read_text(encoding="utf-8")))
        if imports:
            result[path.relative_to(source_root).as_posix()] = imports
    return result


def artifact_dependencies() -> list[str]:
    root = ET.parse(POM_FILE).getroot()
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    dependencies: list[str] = []
    for dependency in root.findall("m:dependencies/m:dependency", namespace):
        group_id = dependency.findtext("m:groupId", namespaces=namespace)
        artifact_id = dependency.findtext("m:artifactId", namespaces=namespace)
        if group_id == "com.cariesguard" and artifact_id:
            dependencies.append(artifact_id)
    return sorted(dependencies)


def main() -> int:
    debt = json.loads(DEBT_FILE.read_text(encoding="utf-8"))
    source_root = ROOT / debt["sourceRoot"]
    expected_imports = {
        path: sorted(imports)
        for path, imports in debt["allowedCrossDomainImports"].items()
    }
    observed_imports = actual_imports(source_root)
    expected_artifacts = sorted(debt["allowedArtifactDependencies"])
    observed_artifacts = artifact_dependencies()

    errors: list[str] = []
    if observed_imports != expected_imports:
        new_imports = {
            path: sorted(set(imports) - set(expected_imports.get(path, [])))
            for path, imports in observed_imports.items()
            if set(imports) - set(expected_imports.get(path, []))
        }
        removed_imports = {
            path: sorted(set(imports) - set(observed_imports.get(path, [])))
            for path, imports in expected_imports.items()
            if set(imports) - set(observed_imports.get(path, []))
        }
        if new_imports:
            errors.append(f"new cross-domain imports: {new_imports}")
        if removed_imports:
            errors.append(f"debt ledger has resolved imports; update it: {removed_imports}")
    if observed_artifacts != expected_artifacts:
        errors.append(
            f"analysis module dependencies changed: expected {expected_artifacts}, observed {observed_artifacts}"
        )

    if errors:
        print("Service boundary validation failed:")
        for error in errors:
            print(f"- {error}")
        return 1
    import_count = sum(len(imports) for imports in observed_imports.values())
    print(
        "Service boundary valid: "
        f"{import_count} explicitly tracked cross-domain imports in {len(observed_imports)} files"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
