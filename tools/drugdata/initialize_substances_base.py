#!/usr/bin/env python3
import json
from pathlib import Path


SOURCE_DIR = Path("tools/drugdata")
RESOURCE_PAIRS = (
    (
        Path("app/src/main/res/raw/substances.json"),
        Path("app/src/main/res/raw/substances_base.json"),
    ),
    (
        Path("app/src/main/res/raw-zh-rCN/substances.json"),
        Path("app/src/main/res/raw-zh-rCN/substances_base.json"),
    ),
)


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def managed_names_and_categories() -> tuple[set[str], set[str]]:
    names: set[str] = set()
    categories: set[str] = set()
    for path in sorted(SOURCE_DIR.rglob("*.json")):
        if path.name in {"substances.json", "substances_base.json"}:
            continue
        data = load_json(path)
        names.update(
            item["name"]
            for item in data.get("substances", [])
            if isinstance(item, dict) and isinstance(item.get("name"), str)
        )
        categories.update(
            item["name"]
            for item in data.get("categories", [])
            if isinstance(item, dict) and isinstance(item.get("name"), str)
        )
    return names, categories


def main() -> None:
    names, categories = managed_names_and_categories()
    for source, output in RESOURCE_PAIRS:
        data = load_json(source)
        data["substances"] = [
            item for item in data.get("substances", []) if item.get("name") not in names
        ]
        data["categories"] = [
            item for item in data.get("categories", []) if item.get("name") not in categories
        ]
        output.write_text(
            json.dumps(data, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
            newline="\n",
        )
        print(
            f"Wrote {output}: {len(data['substances'])} base substances, "
            f"{len(data['categories'])} base categories"
        )


if __name__ == "__main__":
    main()
