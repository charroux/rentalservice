import json
from pathlib import Path

from training_ci.freeze import check_manifest, snapshot


def test_frozen_manifest_detects_changes(tmp_path: Path):
    module = tmp_path / "training/modules/01"
    module.mkdir(parents=True)
    (module / "slides.qmd").write_text("# Stable", encoding="utf-8")
    releases = tmp_path / "training/releases"
    releases.mkdir(parents=True)
    manifest = snapshot(module)
    manifest["root"] = "training/modules/01"
    manifest_path = releases / "01.json"
    manifest_path.write_text(json.dumps(manifest), encoding="utf-8")

    assert check_manifest(tmp_path, manifest_path) == []
    (module / "slides.qmd").write_text("# Changed", encoding="utf-8")
    assert "modified published file" in check_manifest(tmp_path, manifest_path)[0]


def test_append_only_manifest_allows_new_files(tmp_path: Path):
    root = tmp_path / "platform"
    root.mkdir()
    stable = root / "stable.py"
    stable.write_text("VERSION = 1", encoding="utf-8")
    manifest = snapshot(root)
    manifest["root"] = "platform"
    manifest["allowAdditionalFiles"] = True
    manifest_path = tmp_path / "platform.json"
    manifest_path.write_text(json.dumps(manifest), encoding="utf-8")

    (root / "new_extension.py").write_text("NEW = True", encoding="utf-8")
    assert check_manifest(tmp_path, manifest_path) == []

    stable.write_text("VERSION = 2", encoding="utf-8")
    assert "modified published file" in check_manifest(tmp_path, manifest_path)[0]
