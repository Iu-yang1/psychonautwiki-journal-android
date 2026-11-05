"""Inventory offline library gaps for review; never rewrite clinical data."""

import argparse
import json
import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TEXT_MARKERS = re.compile(
    r"source-needed|product-specific|depending on local|资料待|学习|研究性|"
    r"研究用途|研究参考|研究估算|模型|model|占位|待补|未核实",
    re.IGNORECASE,
)
CLINICAL_FIELDS = (
    "indications",
    "contraindications",
    "majorWarnings",
    "majorInteractions",
    "monitoring",
)


def strings(value, path=""):
    if isinstance(value, dict):
        for key, child in value.items():
            # Bibliographic metadata is not user-facing clinical prose.
            if key not in {"sourceRefs", "url", "hrtModelInfo", "id"}:
                yield from strings(child, f"{path}.{key}".strip("."))
    elif isinstance(value, list):
        for index, child in enumerate(value):
            yield from strings(child, f"{path}[{index}]")
    elif isinstance(value, str):
        yield path, value


def audit():
    resources = ROOT / "app/src/main/res"
    entries, categories, localized = {}, set(), {}
    for locale in ("raw", "raw-zh-rCN"):
        for filename in ("substances_base.json", "substances.json"):
            data = json.loads(
                (resources / locale / filename).read_text(encoding="utf-8")
            )
            categories.update(c["name"] for c in data["categories"])
            for substance in data["substances"]:
                (entries if locale == "raw" else localized)[substance["id"]] = substance
    rows = []
    for identifier, substance in sorted(entries.items()):
        clinical = substance.get("clinicalInfo", {})
        issues = []
        for field in CLINICAL_FIELDS:
            if not clinical.get(field):
                issues.append(f"missing_clinical_{field}")
        refs = substance.get("sourceRefs", []) + clinical.get("sourceRefs", [])
        if not any(ref.get("sourceType") == "regulatory-label" for ref in refs):
            issues.append("no_direct_regulatory_label_reference")
        if not substance.get("roas"):
            issues.append("no_routes")
        zh = localized.get(identifier, {})
        if not re.search(
            r"[\u4e00-\u9fff]",
            " ".join([zh.get("name", ""), *zh.get("commonNames", [])]),
        ):
            issues.append("no_chinese_name_or_alias")
        unknown = set(substance.get("categories", [])) - categories
        if unknown:
            issues.append("undefined_categories")
        hits = [
            {"path": path, "text": text}
            for path, text in strings(substance)
            if TEXT_MARKERS.search(text)
        ]
        if hits:
            issues.append("text_review_needed")
        rows.append(
            {
                "id": identifier,
                "name": substance["name"],
                "issues": issues,
                "undefinedCategories": sorted(unknown),
                "textCandidates": hits,
            }
        )
    counts = Counter(issue for row in rows for issue in row["issues"])
    return {
        "schemaVersion": 1,
        "entryCount": len(rows),
        "issueCounts": dict(sorted(counts.items())),
        "note": "Heuristic review candidates; a source reference is not proof of verified clinical content. Research substances may have no regulatory label.",
        "entries": rows,
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = audit()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(
        json.dumps(
            {key: result[key] for key in ("entryCount", "issueCounts")},
            ensure_ascii=False,
        )
    )
