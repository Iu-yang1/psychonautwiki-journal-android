"""Validate immutable identities across offline packs; does not rewrite data."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
registry = json.loads((ROOT / "tools/drugdata/medication_ids.json").read_text(encoding="utf-8"))
assert all(name and identifier.startswith("pw:") for name, identifier in registry.items()), "Invalid medication registry"
errors = []
for path in list((ROOT / "app/src/main/res").glob("raw*/substances*.json")) + list((ROOT / "tools/drugdata/endocrine").rglob("*.json")):
    data = json.loads(path.read_text(encoding="utf-8"))
    seen = set()
    for substance in data.get("substances", []):
        name = substance["name"]
        identifier = substance.get("id")
        if identifier != registry.get(name) or not identifier or identifier in seen:
            errors.append(f"{path}: missing, changed or duplicate identity: {name}")
        seen.add(identifier)
if errors:
    raise SystemExit("\n".join(errors))
print(f"Medication identity validation passed: {len(registry)} registered identities")
