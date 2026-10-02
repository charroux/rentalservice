from __future__ import annotations

import hashlib
import json
from pathlib import Path


def snapshot(root: Path) -> dict[str, object]:
    files = {
        path.relative_to(root).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()
        for path in sorted(root.rglob("*"))
        if path.is_file()
    }
    return {"formatVersion": 1, "root": root.as_posix(), "files": files}


def check_manifest(repository: Path, manifest_path: Path) -> list[str]:
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    root = repository / manifest["root"]
    expected = manifest["files"]
    if manifest.get("allowAdditionalFiles", False):
        actual = {
            name: hashlib.sha256((root / name).read_bytes()).hexdigest()
            for name in expected
            if (root / name).is_file()
        }
    else:
        actual = snapshot(root)["files"]
    errors: list[str] = []
    for name in sorted(set(expected) | set(actual)):
        if name not in actual:
            errors.append(f"removed published file: {root / name}")
        elif name not in expected:
            errors.append(f"added file inside published module: {root / name}")
        elif actual[name] != expected[name]:
            errors.append(f"modified published file: {root / name}")
    return errors
