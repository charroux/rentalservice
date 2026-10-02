from __future__ import annotations

import json
from pathlib import Path

from jsonschema import Draft202012Validator, FormatChecker


def validate_contracts(root: Path) -> list[str]:
    errors: list[str] = []
    schemas = sorted(root.glob("*/v*/schema.json"))
    if not schemas:
        return [f"no versioned event contracts found under {root}"]
    for schema_path in schemas:
        schema = json.loads(schema_path.read_text(encoding="utf-8"))
        validator = Draft202012Validator(schema, format_checker=FormatChecker())
        examples = sorted((schema_path.parent / "examples").glob("*.json"))
        if not examples:
            errors.append(f"{schema_path}: no examples found")
        for example_path in examples:
            instance = json.loads(example_path.read_text(encoding="utf-8"))
            for error in validator.iter_errors(instance):
                location = ".".join(str(part) for part in error.absolute_path) or "<root>"
                errors.append(f"{example_path}:{location}: {error.message}")
    return errors
