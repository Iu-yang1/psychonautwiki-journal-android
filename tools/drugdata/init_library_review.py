"""Freeze identities and seed an honest, resumable field-by-field review ledger."""

import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "tools/drugdata"
FIELDS = (
    "identity",
    "commonNames",
    "categories",
    "roas",
    "summary",
    "clinicalInfo.indications",
    "clinicalInfo.contraindications",
    "clinicalInfo.majorWarnings",
    "clinicalInfo.majorInteractions",
    "clinicalInfo.monitoring",
    "doseUseReferences",
    "timeCourse",
    "sourceRefs",
    "translation",
    "interactions",
)
GROUPS = ("endocrine", "neurology", "psychiatry", "cardiovascular", "baseline")


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def write(path, value):
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )


def group(substance):
    categories = set(substance.get("categories", []))
    if categories & {"endocrine", "hrt-related"}:
        return "endocrine"
    if "neurology" in categories:
        return "neurology"
    if "clinical-psychiatry" in categories:
        return "psychiatry"
    if "cardiovascular" in categories or any(c.startswith("atc-") for c in categories):
        return "cardiovascular"
    return "baseline"


def main():
    entries = {}
    for filename in ("substances_base.json", "substances.json"):
        entries.update(
            {
                s["id"]: s
                for s in read(ROOT / "app/src/main/res/raw" / filename)["substances"]
            }
        )
    baseline_path = DATA / "library_baseline.json"
    if not baseline_path.exists():
        model = ROOT / "app/src/main/res/raw/hrt_reference_models.json"
        write(
            baseline_path,
            {
                "schemaVersion": 1,
                "sourceRevision": "61680f70547f8ab3447bdf569caf47dce1baaacb",
                "modelSha256": hashlib.sha256(model.read_bytes()).hexdigest(),
                "entries": [
                    {"id": s["id"], "canonicalName": s["name"], "group": group(s)}
                    for s in sorted(entries.values(), key=lambda s: s["id"])
                ],
            },
        )
    baseline = read(baseline_path)
    assert {e["id"]: e["canonicalName"] for e in baseline["entries"]} == {
        k: s["name"] for k, s in entries.items()
    }
    manifest_path = DATA / "review_manifest.json"
    manifest = (
        read(manifest_path)
        if manifest_path.exists()
        else {"schemaVersion": 1, "entries": {}}
    )
    for group_id in GROUPS:
        members = sorted(
            (e for e in baseline["entries"] if e["group"] == group_id),
            key=lambda e: e["canonicalName"],
        )
        for index, entry in enumerate(members):
            manifest["entries"].setdefault(
                entry["id"],
                {
                    "canonicalName": entry["canonicalName"],
                    "batch": f"{group_id}-{index // 20 + 1:02d}",
                    "fields": {field: {"status": "pending"} for field in FIELDS},
                    "sources": [],
                    "searches": [],
                    "aliasDecisions": [],
                },
            )
            for field in FIELDS:
                manifest["entries"][entry["id"]]["fields"].setdefault(
                    field, {"status": "pending"}
                )
    write(manifest_path, manifest)
    print(f"Frozen {len(entries)} identities; existing review decisions preserved")


if __name__ == "__main__":
    main()
