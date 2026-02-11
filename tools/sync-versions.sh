#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

POM_FILE="${REPO_ROOT}/pom.xml"
UI_PACKAGE_JSON="${REPO_ROOT}/ui/package.json"
UI_PACKAGE_LOCK="${REPO_ROOT}/ui/package-lock.json"

if [[ ! -f "${POM_FILE}" ]]; then
  echo "pom.xml not found at: ${POM_FILE}" >&2
  exit 1
fi

if [[ ! -f "${UI_PACKAGE_JSON}" ]]; then
  echo "ui/package.json not found at: ${UI_PACKAGE_JSON}" >&2
  exit 1
fi

if [[ ! -f "${UI_PACKAGE_LOCK}" ]]; then
  echo "ui/package-lock.json not found at: ${UI_PACKAGE_LOCK}" >&2
  exit 1
fi

REPO_ROOT="${REPO_ROOT}" python3 - <<'PY'
from __future__ import annotations

import os
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

repo_root_raw = os.environ.get("REPO_ROOT")
if not repo_root_raw:
    raise SystemExit("REPO_ROOT env is required")
repo_root = pathlib.Path(repo_root_raw).resolve()
pom_file = repo_root / "pom.xml"
pkg_json = repo_root / "ui" / "package.json"
pkg_lock = repo_root / "ui" / "package-lock.json"

def read_text(path: pathlib.Path) -> str:
    return path.read_text(encoding="utf-8")

def write_text(path: pathlib.Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")

def parse_revision(pom_path: pathlib.Path) -> str:
    try:
        tree = ET.parse(pom_path)
    except Exception as e:
        raise SystemExit(f"Failed to parse {pom_path}: {e}") from e

    root = tree.getroot()
    ns = ""
    if root.tag.startswith("{") and "}" in root.tag:
        ns = root.tag.split("}")[0] + "}"

    rev = root.find(f".//{ns}properties/{ns}revision")
    if rev is None or (rev.text or "").strip() == "":
        raise SystemExit("pom.xml is missing <properties><revision>...</revision></properties>")
    return rev.text.strip()

revision = parse_revision(pom_file)

pkg_json_text = read_text(pkg_json)
pkg_json_new, n1 = re.subn(r'("version"\s*:\s*")[^"]+(")', rf"\g<1>{revision}\2", pkg_json_text, count=1)
if n1 != 1:
    raise SystemExit(f"Failed to update version in {pkg_json} (expected 1 replacement, got {n1})")

pkg_lock_text = read_text(pkg_lock)

# top-level "version"
pkg_lock_new, n2 = re.subn(r'(\A\s*\{\s*\n\s*"name"\s*:\s*"[^"]+"\s*,\s*\n\s*"version"\s*:\s*")[^"]+(")',
                           rf"\g<1>{revision}\2", pkg_lock_text, count=1, flags=re.MULTILINE)
if n2 != 1:
    raise SystemExit(f"Failed to update top-level version in {pkg_lock} (expected 1 replacement, got {n2})")

# packages[""].version
pkg_lock_new2, n3 = re.subn(r'("packages"\s*:\s*\{\s*\n\s*""\s*:\s*\{\s*\n\s*"name"\s*:\s*"[^"]+"\s*,\s*\n\s*"version"\s*:\s*")[^"]+(")',
                            rf"\g<1>{revision}\2", pkg_lock_new, count=1, flags=re.MULTILINE)
if n3 != 1:
    raise SystemExit(f"Failed to update packages[\"\"] version in {pkg_lock} (expected 1 replacement, got {n3})")

changed = False
if pkg_json_new != pkg_json_text:
    write_text(pkg_json, pkg_json_new)
    changed = True
if pkg_lock_new2 != pkg_lock_text:
    write_text(pkg_lock, pkg_lock_new2)
    changed = True

if changed:
    print(f"Synced UI version to {revision}")
else:
    print(f"UI version already {revision}")
PY
