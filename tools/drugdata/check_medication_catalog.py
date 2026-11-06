"""Reject identity drift, missing aliases and unreviewed alias collision changes."""

import json
import re
from build_medication_catalog import ROOT, build

expected, collisions = build()
actual = json.loads(
    (ROOT / "app/src/main/res/raw/medication_catalog.json").read_text(encoding="utf-8")
)
known = json.loads(
    (ROOT / "tools/drugdata/medication_alias_ambiguities.json").read_text(
        encoding="utf-8"
    )
)
route_source = (
    ROOT
    / "app/src/main/java/com/isaakhanimann/journal/data/substances/AdministrationRoute.kt"
)
routes = {
    value.decode("ascii")
    for value in re.findall(
        rb"^    ([A-Z_]+) \{", route_source.read_bytes(), re.MULTILINE
    )
}
assert {"ORAL", "INTRAVENOUS", "INTRAMUSCULAR"} <= routes, (
    "Cannot read production administration routes"
)
assert actual == expected, (
    "Medication catalog is stale; review identity and locale changes"
)
assert collisions == known, (
    "Alias ambiguity changed; review before rebuilding the audit list"
)
for entry in actual["entries"]:
    assert entry["aliases"] and entry["sourceUrls"], entry["id"]
    for field in ("ingredientId", "chemicalFormId", "formulationId"):
        assert (
            entry.get(field) is None
            or isinstance(entry[field], str)
            and ":" in entry[field]
        )
    assert set(entry.get("administrationRoutes", [])) <= routes, entry["id"]
registry = json.loads(
    (ROOT / "tools/drugdata/medication_ids.json").read_text(encoding="utf-8")
)
by_id = {entry["id"]: entry for entry in actual["entries"]}
for name, identifier in registry.items():
    assert name in by_id[identifier]["aliases"], (
        f"Historical canonical name disappeared: {name}"
    )
oral_e2 = by_id[registry["Oral Estradiol"]]
oral_ev = by_id[registry["Oral Estradiol Valerate"]]
injection_ev = by_id[registry["Estradiol Valerate Injection"]]
assert (
    oral_e2["ingredientId"] == oral_ev["ingredientId"] == injection_ev["ingredientId"]
)
assert (
    oral_e2["chemicalFormId"]
    != oral_ev["chemicalFormId"]
    == injection_ev["chemicalFormId"]
)
assert oral_ev["formulationId"] != injection_ev["formulationId"]
assert by_id[registry["Ethinylestradiol"]]["ingredientId"] != oral_e2["ingredientId"]
assert (
    by_id[registry["Conjugated Estrogens"]]["ingredientId"] != oral_e2["ingredientId"]
)
print(
    f"Medication catalog validation passed: {len(actual['entries'])} identities, {len(known)} known ambiguous aliases"
)
