"""Build cross-language aliases and reviewed identity links without changing drug records."""

import json
import re
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def normalized(value):
    return re.sub(r"[\s_-]+", "", unicodedata.normalize("NFKC", value).strip().lower())


def build():
    registry = json.loads(
        (ROOT / "tools/drugdata/medication_ids.json").read_text(encoding="utf-8")
    )
    overrides = json.loads(
        (ROOT / "tools/drugdata/medication_identity_overrides.json").read_text(
            encoding="utf-8"
        )
    )
    policy_path = ROOT / "tools/drugdata/alias_review.json"
    alias_policy = (
        json.loads(policy_path.read_text(encoding="utf-8")).get("entries", {})
        if policy_path.exists()
        else {}
    )
    entries = {}
    for name, identifier in registry.items():
        entries.setdefault(
            identifier, {"id": identifier, "aliases": [], "sourceUrls": []}
        )["aliases"].append(name)
    for directory in sorted((ROOT / "app/src/main/res").glob("raw*")):
        primary_path = directory / "substances.json"
        if not primary_path.exists():
            continue
        primary = json.loads(primary_path.read_text(encoding="utf-8")).get(
            "substances", []
        )
        authoritative = {
            sub["id"]
            for sub in primary
            if "commonNames" in sub.get("reviewedFields", [])
        }
        baseline_path = directory / "substances_base.json"
        baseline = (
            json.loads(baseline_path.read_text(encoding="utf-8")).get("substances", [])
            if baseline_path.exists()
            else []
        )
        for substance in primary + [
            sub for sub in baseline if sub["id"] not in authoritative
        ]:
            entry = entries[substance["id"]]
            entry["aliases"] += [substance["name"], *substance.get("commonNames", [])]
            entry.setdefault("searchTerms", []).extend(substance.get("searchTerms", []))
            if substance.get("url"):
                entry["sourceUrls"].append(substance["url"])
    for identifier, entry in entries.items():
        entry["aliases"] = sorted(set(entry["aliases"]))
        entry["sourceUrls"] = sorted(set(entry["sourceUrls"]))
        entry.update(overrides.get(identifier, {}))
        candidates = set(entry["aliases"] + entry.get("searchTerms", []))
        keyword_aliases = {
            alias
            for alias in candidates
            if (decision := alias_policy.get(normalized(alias), {})).get("status")
            == "reviewed"
            and decision.get("kind") == "search_keyword"
            and identifier in decision.get("affectedIds", [])
        }
        if keyword_aliases:
            entry["aliases"] = [
                alias for alias in entry["aliases"] if alias not in keyword_aliases
            ]
            entry["searchTerms"] = sorted(
                set(entry.get("searchTerms", [])) | keyword_aliases
            )
            entry["excludedAliases"] = sorted(keyword_aliases)
        if not entry.get("searchTerms"):
            entry.pop("searchTerms", None)
    aliases = {}
    for entry in entries.values():
        for alias in entry["aliases"]:
            aliases.setdefault(normalized(alias), set()).add(entry["id"])
    ambiguous = {
        alias: sorted(ids) for alias, ids in sorted(aliases.items()) if len(ids) > 1
    }
    catalog = {
        "version": 1,
        "entries": sorted(entries.values(), key=lambda entry: entry["id"]),
    }
    return catalog, ambiguous


if __name__ == "__main__":
    catalog, ambiguous = build()
    (ROOT / "app/src/main/res/raw/medication_catalog.json").write_text(
        json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    # Known collisions remain ambiguous at runtime; this is an audit list, not a winner map.
    (ROOT / "tools/drugdata/medication_alias_ambiguities.json").write_text(
        json.dumps(ambiguous, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(
        f"Catalog: {len(catalog['entries'])} identities; {len(ambiguous)} ambiguous aliases requiring explicit identity"
    )
