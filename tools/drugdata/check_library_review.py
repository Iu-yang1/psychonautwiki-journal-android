"""Validate review provenance and identity preservation; gate final publication explicitly."""

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "tools/drugdata"
STATES = {"verified", "not_applicable", "evidence_unavailable", "pending"}


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def validate(require_complete=False):
    baseline = read(DATA / "library_baseline.json")
    manifest = read(DATA / "review_manifest.json")["entries"]
    frozen = {entry["id"]: entry["canonicalName"] for entry in baseline["entries"]}
    assert (
        len(frozen) == 595
        and {key: row["canonicalName"] for key, row in manifest.items()} == frozen
    )
    assert (
        hashlib.sha256(
            (ROOT / "app/src/main/res/raw/hrt_reference_models.json").read_bytes()
        ).hexdigest()
        == baseline["modelSha256"]
    ), "HRT model parameters changed outside library scope"
    states = Counter()
    pending_entries = 0
    for identifier, row in manifest.items():
        sources = {source["id"]: source for source in row["sources"]}
        assert len(sources) == len(row["sources"]), identifier
        for source in sources.values():
            assert all(
                source.get(key)
                for key in ("url", "title", "region", "version", "accessedDate")
            ), identifier
            assert source["url"].startswith("https://"), identifier
        entry_pending = False
        for field, review in row["fields"].items():
            state = review["status"]
            assert state in STATES, (identifier, field)
            states[state] += 1
            if state == "pending":
                entry_pending = True
                continue
            assert review.get("reviewedDate"), (identifier, field)
            refs = review.get("sourceIds", [])
            assert all(ref in sources for ref in refs), (identifier, field)
            if state == "verified":
                assert refs, (identifier, field, "verified without sources")
            if state in {"not_applicable", "evidence_unavailable"}:
                assert review.get("reason"), (identifier, field, "missing reason")
            if state == "evidence_unavailable":
                assert row.get("searches"), (identifier, field, "missing search record")
        pending_entries += entry_pending
    taxonomy = read(DATA / "medication_taxonomy.json")
    nodes = {node["id"]: node for node in taxonomy["nodes"]}
    assert len(nodes) == len(taxonomy["nodes"])
    for node in nodes.values():
        assert node["kind"] in {"domain", "category", "mechanism", "attribute"}
        assert node["labelZh"] and node["labelEn"]
        assert node["parentId"] is None or node["parentId"] in nodes
    for locale in ("raw", "raw-zh-rCN"):
        entries = read(ROOT / "app/src/main/res" / locale / "substances.json")[
            "substances"
        ]
        assert {entry["id"]: entry["name"] for entry in entries} == frozen, locale
    assert (DATA / "medication_taxonomy.json").read_bytes() == (
        ROOT / "app/src/main/res/raw/medication_taxonomy.json"
    ).read_bytes()
    report = {
        "identities": len(frozen),
        "closedEntries": len(frozen) - pending_entries,
        "pendingEntries": pending_entries,
        "fieldStates": dict(states),
    }
    alias_path = DATA / "alias_review.json"
    if alias_path.exists():
        aliases = read(alias_path)["entries"]
        assert len(aliases) == 106, "Original alias-conflict roster changed"
        for alias, decision in aliases.items():
            assert decision.get("reason") and all(
                identifier in frozen for identifier in decision["affectedIds"]
            ), alias
            assert decision["status"] in {"pending", "reviewed"}, alias
        report["pendingAliasDecisions"] = sum(
            decision["status"] == "pending" for decision in aliases.values()
        )
    print(json.dumps(report, ensure_ascii=False))
    if require_complete:
        assert pending_entries == 0, "Publication blocked: field reviews remain pending"
        assert report.get("pendingAliasDecisions", 0) == 0, (
            "Publication blocked: alias conflicts remain unreviewed"
        )
    return report


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--require-complete", action="store_true")
    args = parser.parse_args()
    validate(args.require_complete)
