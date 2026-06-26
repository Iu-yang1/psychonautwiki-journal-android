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


def append_unique(current: list, incoming: list) -> list:
    result = list(current)
    seen = {
        json.dumps(item, ensure_ascii=False, sort_keys=True)
        if isinstance(item, dict)
        else item
        for item in result
    }
    for item in incoming:
        marker = (
            json.dumps(item, ensure_ascii=False, sort_keys=True)
            if isinstance(item, dict)
            else item
        )
        if marker not in seen:
            seen.add(marker)
            result.append(item)
    return result


def remove_values(current: list, values_to_remove: list) -> list:
    remove_set = set(values_to_remove)
    return [item for item in current if item not in remove_set]


def apply_substance_patches(items: list[dict], patches: list[dict]) -> list[dict]:
    by_name = {item.get("name"): item for item in items}
    for patch in patches:
        name = patch.get("name")
        if name not in by_name:
            continue
        item = by_name[name]
        if "commonNamesRemove" in patch:
            item["commonNames"] = remove_values(
                item.get("commonNames", []),
                patch.get("commonNamesRemove", []),
            )
        if "commonNamesAdd" in patch:
            item["commonNames"] = append_unique(
                item.get("commonNames", []),
                patch.get("commonNamesAdd", []),
            )
        if "categoriesRemove" in patch:
            item["categories"] = remove_values(
                item.get("categories", []),
                patch.get("categoriesRemove", []),
            )
        if "categoriesAdd" in patch:
            item["categories"] = append_unique(
                item.get("categories", []),
                patch.get("categoriesAdd", []),
            )
        for field_name, value in patch.get("fieldsSet", {}).items():
            item[field_name] = value
    return items


def filter_by_retain_categories(
    items: list[dict],
    retain_categories: set[str],
) -> list[dict]:
    if not retain_categories:
        return items
    return [
        item
        for item in items
        if retain_categories.intersection(item.get("categories", []))
    ]


RECREATIONAL_PSYCHIATRY_CATEGORY_MARKERS = {
    "common",
    "habit-forming",
    "research-chemical",
    "tentative",
    "psychedelic",
    "stimulant",
    "depressant",
    "opioid",
    "dissociative",
    "deliriant",
    "hallucinogen",
    "oneirogen",
    "nootropic",
    "cannabinoid",
    "entactogen",
    "常见的",
    "易成瘾",
    "研究性化学品",
    "信息不确定",
    "迷幻剂",
    "兴奋剂",
    "抑制剂",
    "阿片类物质",
    "分离剂",
    "谵妄剂",
    "致幻剂",
    "致梦剂",
    "益智药",
    "大麻素",
    "同感剂",
}


def sanitize_clinical_psychiatry_categories(items: list[dict]) -> list[dict]:
    retained_clinical_routes = {
        "oral",
        "sublingual",
        "buccal",
        "transdermal",
        "subcutaneous",
        "intramuscular",
        "intravenous",
    }
    for item in items:
        categories = item.get("categories", [])
        if "clinical-psychiatry" not in categories:
            continue
        item["categories"] = [
            category
            for category in categories
            if category not in RECREATIONAL_PSYCHIATRY_CATEGORY_MARKERS
        ]
        if "roas" in item:
            item["roas"] = [
                roa
                for roa in item.get("roas", [])
                if roa.get("name") in retained_clinical_routes
            ]
    return items


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
    retain_categories: set[str] = set()

    for source_path in iter_source_files(source_dir):
        source_data = load_json(source_path)
        build_config = source_data.get("buildConfig", {})
        retain_categories.update(build_config.get("retainSubstanceCategories", []))
        categories = upsert_by_name(categories, source_data.get("categories", []))
        substances = upsert_by_name(substances, source_data.get("substances", []))
        substances = apply_substance_patches(
            substances,
            source_data.get("substancePatches", []),
        )

    substances = filter_by_retain_categories(substances, retain_categories)
    substances = sanitize_clinical_psychiatry_categories(substances)
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
