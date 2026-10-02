from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys

from .contracts import validate_contracts
from .freeze import check_manifest, snapshot


def main() -> None:
    parser = argparse.ArgumentParser(prog="training-ci")
    subparsers = parser.add_subparsers(dest="command", required=True)

    check = subparsers.add_parser("check-frozen")
    check.add_argument("--repository", type=Path, default=Path.cwd())
    check.add_argument("--manifests", type=Path, default=Path("training/releases"))

    create = subparsers.add_parser("create-manifest")
    create.add_argument("root", type=Path)

    contracts = subparsers.add_parser("validate-contracts")
    contracts.add_argument("root", type=Path)

    args = parser.parse_args()
    if args.command == "check-frozen":
        manifests_dir = args.repository / args.manifests
        errors = [
            error
            for manifest in sorted(manifests_dir.glob("*.json"))
            for error in check_manifest(args.repository, manifest)
        ]
        _finish(errors, "Published training modules are unchanged")
    elif args.command == "create-manifest":
        print(json.dumps(snapshot(args.root), indent=2))
    elif args.command == "validate-contracts":
        _finish(validate_contracts(args.root), "Event contract examples are valid")


def _finish(errors: list[str], success: str) -> None:
    if errors:
        print("\n".join(errors), file=sys.stderr)
        raise SystemExit(1)
    print(success)


if __name__ == "__main__":
    main()
