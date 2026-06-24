#!/usr/bin/env python3
import argparse
import json
from pathlib import Path


DEFAULT_BASE = Path("app/src/main/res/raw/substances_base.json")
DEFAULT_LOCALIZED_BASES = [
    (
        Path("app/src/main/res/raw-zh-rCN/substances_base.json"),
        Path("app/src/main/res/raw-zh-rCN/substances.json"),
    ),
]
DEFAULT_SOURCE_DIR = Path("tools/drugdata")
DEFAULT_OUTPUT = Path("app/src/main/res/raw/substances.json")


def load_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as file:
        return json.load(file)


def write_json(path: Path, data: dict) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as file:
        json.dump(data, file, ensure_ascii=False, indent=2)
        file.write("\n")


def write_hybrid_json(path: Path, data: dict) -> None:
    write_json(path, data)


def upsert_by_name(items: list[dict], incoming: list[dict]) -> list[dict]:
    by_name = {item.get("name"): index for index, item in enumerate(items)}
    result = list(items)
    for item in incoming:
        name = item.get("name")
        if name in by_name:
            result[by_name[name]] = item
        else:
            by_name[name] = len(result)
            result.append(item)
    return result


def iter_source_files(source_dir: Path) -> list[Path]:
    return sorted(
        path
        for path in source_dir.rglob("*.json")
        if path.name not in {"substances.json", "substances_base.json"}
    )


def build(base_path: Path, source_dir: Path, output_path: Path, hybrid: bool = False) -> None:
    data = load_json(base_path)
    categories = data.get("categories", [])
    substances = data.get("substances", [])

    for source_path in iter_source_files(source_dir):
        source_data = load_json(source_path)
        categories = upsert_by_name(categories, source_data.get("categories", []))
        substances = upsert_by_name(substances, source_data.get("substances", []))

    data["categories"] = categories
    data["substances"] = substances
    if hybrid:
        write_hybrid_json(output_path, data)
    else:
        write_json(output_path, data)


def build_default_resources(source_dir: Path) -> None:
    build(DEFAULT_BASE, source_dir, DEFAULT_OUTPUT)
    for localized_base, localized_output in DEFAULT_LOCALIZED_BASES:
        if localized_base.exists():
            build(localized_base, source_dir, localized_output, hybrid=True)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Merge local drug-data packs into substances.json."
    )
    parser.add_argument("--base", type=Path, default=DEFAULT_BASE)
    parser.add_argument("--source-dir", type=Path, default=DEFAULT_SOURCE_DIR)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    if args.output is None and args.base == DEFAULT_BASE:
        build_default_resources(args.source_dir)
    else:
        build(args.base, args.source_dir, args.output or DEFAULT_OUTPUT)


if __name__ == "__main__":
    main()
