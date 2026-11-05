"""Build cross-language aliases and reviewed identity links without changing drug records."""
import json
import re
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

def normalized(value):
    return re.sub(r"[\s_-]+", "", unicodedata.normalize("NFKC", value).strip().lower())

def build():
    registry = json.loads((ROOT / "tools/drugdata/medication_ids.json").read_text(encoding="utf-8"))
    overrides = json.loads((ROOT / "tools/drugdata/medication_identity_overrides.json").read_text(encoding="utf-8"))
    entries = {}
    for name, identifier in registry.items():
        entries.setdefault(identifier, {"id": identifier, "aliases": [], "sourceUrls": []})["aliases"].append(name)
    for path in sorted((ROOT / "app/src/main/res").glob("raw*/substances*.json")):
        for substance in json.loads(path.read_text(encoding="utf-8")).get("substances", []):
            entry = entries[substance["id"]]
            entry["aliases"] += [substance["name"], *substance.get("commonNames", [])]
            if substance.get("url"):
                entry["sourceUrls"].append(substance["url"])
    for identifier, entry in entries.items():
        entry["aliases"] = sorted(set(entry["aliases"]))
        entry["sourceUrls"] = sorted(set(entry["sourceUrls"]))
        entry.update(overrides.get(identifier, {}))
    aliases = {}
    for entry in entries.values():
        for alias in entry["aliases"]:
            aliases.setdefault(normalized(alias), set()).add(entry["id"])
    ambiguous = {alias: sorted(ids) for alias, ids in sorted(aliases.items()) if len(ids) > 1}
    catalog = {"version": 1, "entries": sorted(entries.values(), key=lambda entry: entry["id"])}
    return catalog, ambiguous

if __name__ == "__main__":
    catalog, ambiguous = build()
    (ROOT / "app/src/main/res/raw/medication_catalog.json").write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    # Known collisions remain ambiguous at runtime; this is an audit list, not a winner map.
    (ROOT / "tools/drugdata/medication_alias_ambiguities.json").write_text(json.dumps(ambiguous, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Catalog: {len(catalog['entries'])} identities; {len(ambiguous)} ambiguous aliases requiring explicit identity")
