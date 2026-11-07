#!/usr/bin/env python3
import argparse
import json
import re
from copy import deepcopy
from pathlib import Path
from tempfile import NamedTemporaryFile
from time import sleep
from build_medication_catalog import normalized


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
    file = NamedTemporaryFile(
        mode="w", encoding="utf-8", newline="\n", dir=path.parent, delete=False
    )
    temporary = Path(file.name)
    try:
        with file:
            json.dump(data, file, ensure_ascii=False, indent=2)
            file.write("\n")
        for attempt in range(20):
            try:
                temporary.replace(path)
                break
            except PermissionError:
                if attempt == 19:
                    raise
                sleep(0.25)
    finally:
        temporary.unlink(missing_ok=True)


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


TIMELINE_ROUTE_ALIASES = {
    "ORAL": {"oral", "po", "by mouth"},
    "SUBLINGUAL": {"sublingual", "sl"},
    "BUCCAL": {"buccal"},
    "INSUFFLATED": {"intranasal", "insufflated", "nasal", "nasal spray"},
    "RECTAL": {"rectal"},
    "TRANSDERMAL": {"transdermal", "topical patch", "patch"},
    "SUBCUTANEOUS": {"subcutaneous", "sc", "subcut", "subcutaneous injection"},
    "INTRAMUSCULAR": {"intramuscular", "im", "intramuscular injection"},
    "INTRAVENOUS": {
        "intravenous",
        "iv",
        "iv infusion",
        "infusion",
        "intravenous infusion",
    },
    "SMOKED": {"smoked", "vaporized"},
    "INHALED": {"inhaled", "inhalation"},
}


def normalize_route(value: str | None) -> str:
    return (value or "").strip().lower().replace("_", " ")


def route_matches(time_course_route: str | None, app_route: str) -> bool:
    normalized_route = normalize_route(time_course_route)
    route_text = normalize_route(app_route)
    aliases = TIMELINE_ROUTE_ALIASES[app_route]
    return (
        normalized_route == route_text
        or normalized_route in aliases
        or any(
            re.search(rf"(?<!\w){re.escape(alias)}(?!\w)", normalized_route)
            for alias in aliases | {route_text}
        )
    )


def has_numeric_time(value: object) -> bool:
    return isinstance(value, dict) and any(
        isinstance(value.get(key), (int, float)) for key in ("min", "max")
    )


def has_numeric_duration(time_course: dict) -> bool:
    return (
        has_numeric_time(time_course.get("durationOfAction"))
        or has_numeric_time(time_course.get("washout"))
        or has_numeric_time(time_course.get("effectTimelineWindow"))
    )


def roa_has_numeric_duration(roa: dict) -> bool:
    duration = roa.get("duration")
    if not isinstance(duration, dict):
        return False
    return any(
        has_numeric_time(duration.get(field_name))
        for field_name in ("onset", "comeup", "peak", "offset", "total", "afterglow")
    )


def has_numeric_half_life(time_course: dict) -> bool:
    return has_numeric_time(time_course.get("eliminationHalfLife")) or has_numeric_time(
        time_course.get("effectiveHalfLife")
    )


def time_value(value: list | dict) -> dict:
    if isinstance(value, dict):
        return deepcopy(value)
    if not isinstance(value, list) or len(value) != 3:
        raise ValueError(f"Invalid time value: {value}")
    return {
        "min": value[0],
        "max": value[1],
        "unit": value[2],
        "basis": "approximate published pharmacokinetics",
    }


TIMELINE_TIME_FIELDS = {
    "tmax",
    "onset",
    "peakEffect",
    "effectTimelineWindow",
    "durationOfAction",
    "washout",
    "timeToSteadyState",
    "peakWindow",
    "troughWindow",
}

TIMELINE_END_FIELDS = {
    "effectTimelineWindow",
    "durationOfAction",
    "washout",
    "eliminationHalfLife",
    "effectiveHalfLife",
}


def timeline_field(field_name: str, raw_value: object) -> tuple[str, object]:
    if field_name == "halfLife":
        return "eliminationHalfLife", time_value(raw_value)
    if field_name in TIMELINE_TIME_FIELDS | {"effectiveHalfLife"}:
        return field_name, time_value(raw_value)
    if field_name == "status":
        return "effectTimelineStatus", raw_value
    if field_name == "note":
        return "effectTimelineNote", raw_value
    return field_name, deepcopy(raw_value)


def merge_timeline_field(target: dict, field_name: str, raw_value: object) -> None:
    target_field, value = timeline_field(field_name, raw_value)
    if target_field in {"notes", "sourceRefs"}:
        target[target_field] = append_unique(target.get(target_field, []), value)
    elif target_field == "effectTimelineStatus":
        existing = str(target.get(target_field) or "").strip().lower()
        if not existing or existing == "source-needed":
            target[target_field] = value
    elif target_field in TIMELINE_END_FIELDS:
        if not has_numeric_time(target.get(target_field)):
            target[target_field] = value
    elif target.get(target_field) in (None, "", [], {}):
        target[target_field] = value


def apply_timeline_patch(
    item: dict, time_courses: list[dict], route_patch: dict
) -> None:
    route = route_patch.get("route")
    target = next(
        (
            time_course
            for time_course in time_courses
            if route_matches(time_course.get("route"), route)
        ),
        None,
    )
    if target is None:
        target = {"route": route}
        source_refs = item.get("sourceRefs", [])
        if source_refs:
            target["sourceRefs"] = deepcopy(source_refs[:2])
        time_courses.append(target)
    for field_name, raw_value in route_patch.items():
        if field_name != "route":
            merge_timeline_field(target, field_name, raw_value)


def missing_route_timeline(item: dict, route: str) -> dict:
    entry = {
        "route": route,
        "effectTimelineStatus": "not-recommended",
        "effectTimelineNote": (
            "No route-specific pharmacokinetic or clinical-effect timeline is "
            "available; values from another administration route are not copied."
        ),
        "notes": [
            "No route-specific timeline was found; another route's values are not used."
        ],
    }
    source_refs = item.get("sourceRefs", [])
    if source_refs:
        entry["sourceRefs"] = deepcopy(source_refs[:2])
    return entry


def infer_route_timelines(item: dict, time_courses: list[dict]) -> None:
    for roa in item.get("roas", []):
        roa_name = normalize_route(roa.get("name"))
        route = next(
            (
                app_route
                for app_route, aliases in TIMELINE_ROUTE_ALIASES.items()
                if roa_name == normalize_route(app_route) or roa_name in aliases
            ),
            None,
        )
        if not route or roa_has_numeric_duration(roa):
            continue
        if any(route_matches(tc.get("route"), route) for tc in time_courses):
            continue
        time_courses.append(missing_route_timeline(item, route))


def mark_pk_estimates(time_courses: list[dict]) -> None:
    for time_course in time_courses:
        if time_course.get("effectTimelineStatus") == "not-recommended":
            continue
        if (
            not has_numeric_duration(time_course)
            and has_numeric_half_life(time_course)
            and time_course.get("effectTimelineStatus") in (None, "source-needed")
        ):
            time_course["effectTimelineStatus"] = "pk-estimated"
            time_course["effectTimelineNote"] = (
                time_course.get("effectTimelineNote")
                or "Approximate pharmacokinetic timeline for effect visualization."
            )


def apply_timeline_coverage(items: list[dict], coverage: dict) -> list[dict]:
    by_name = {item.get("name"): item for item in items}
    for patch in coverage.get("timelineCoverage", []):
        item = by_name.get(patch.get("name"))
        if not item:
            raise ValueError(
                f"Timeline coverage identity not found: {patch.get('name')}"
            )
        time_courses = item.get("timeCourse")
        if not isinstance(time_courses, list):
            time_courses = []
            item["timeCourse"] = time_courses
        for route_patch in patch.get("routes", []):
            apply_timeline_patch(item, time_courses, route_patch)

    for item in items:
        time_courses = item.get("timeCourse", [])
        infer_route_timelines(item, time_courses)
        mark_pk_estimates(time_courses)
    return items


def validate_timeline_coverage(items: list[dict]) -> list[str]:
    missing = []
    for item in items:
        routes = set()
        for roa in item.get("roas", []):
            roa_name = normalize_route(roa.get("name"))
            for app_route, aliases in TIMELINE_ROUTE_ALIASES.items():
                if roa_name == normalize_route(app_route) or roa_name in aliases:
                    routes.add(app_route)
        for time_course in item.get("timeCourse", []):
            for app_route in TIMELINE_ROUTE_ALIASES:
                if route_matches(time_course.get("route"), app_route):
                    routes.add(app_route)
        for route in routes:
            roa = next(
                (
                    roa
                    for roa in item.get("roas", [])
                    if route_matches(roa.get("name"), route)
                ),
                None,
            )
            if roa and roa_has_numeric_duration(roa):
                continue
            time_courses = [
                time_course
                for time_course in item.get("timeCourse", [])
                if route_matches(time_course.get("route"), route)
            ]
            if any(
                has_numeric_duration(time_course)
                or time_course.get("effectTimelineStatus") == "not-recommended"
                or (
                    time_course.get("effectTimelineStatus") == "pk-estimated"
                    and has_numeric_half_life(time_course)
                )
                for time_course in time_courses
            ):
                continue
            missing.append(f"{item.get('name')} / {route}")
    return missing


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
    "benzodiazepine",
    "barbiturate",
    "eugeroic",
    "gabapentinoid",
    "hypnotic",
    "z-drug",
    "苯二氮䓬类",
    "巴比妥类",
    "促醒剂",
    "催眠药",
    "Z 类催眠药",
    "Z类催眠药",
    "加巴喷丁类",
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


def filter_categories_by_usage(
    categories: list[dict], substances: list[dict]
) -> list[dict]:
    used_categories = {
        category
        for substance in substances
        for category in substance.get("categories", [])
    }
    return [
        category for category in categories if category.get("name") in used_categories
    ]


def iter_source_files(source_dir: Path) -> list[Path]:
    return sorted(
        path
        for path in source_dir.rglob("*.json")
        if path.name not in {"substances.json", "substances_base.json"}
    )


def is_data_pack(data: object) -> bool:
    return isinstance(data, dict) and any(
        key in data
        for key in ("buildConfig", "categories", "substances", "substancePatches")
    )


def build(
    base_path: Path, source_dir: Path, output_path: Path, hybrid: bool = False
) -> None:
    data = load_json(base_path)
    categories = data.get("categories", [])
    substances = data.get("substances", [])
    retain_categories: set[str] = set()

    for source_path in iter_source_files(source_dir):
        source_data = load_json(source_path)
        if not is_data_pack(source_data):
            continue
        build_config = source_data.get("buildConfig", {})
        retain_categories.update(build_config.get("retainSubstanceCategories", []))
        categories = upsert_by_name(categories, source_data.get("categories", []))
        substances = upsert_by_name(substances, source_data.get("substances", []))
        substances = apply_substance_patches(
            substances,
            source_data.get("substancePatches", []),
        )

    substances = sanitize_clinical_psychiatry_categories(substances)
    registry_path = source_dir / "medication_ids.json"
    if registry_path.exists():
        registry = load_json(registry_path)
        for substance in substances:
            identifier = registry.get(substance["name"])
            if identifier:
                if substance.get("id", identifier) != identifier:
                    raise ValueError(
                        f"Medication identity changed: {substance['name']}"
                    )
                substance["id"] = identifier
    baseline_path = source_dir / "library_baseline.json"
    frozen_ids = (
        {entry["id"] for entry in load_json(baseline_path)["entries"]}
        if baseline_path.exists()
        else set()
    )
    substances = [
        item
        for item in substances
        if item.get("id") in frozen_ids
        or not retain_categories
        or retain_categories.intersection(item.get("categories", []))
    ]
    manifest_path = source_dir / "review_manifest.json"
    manifest = (
        load_json(manifest_path).get("entries", {}) if manifest_path.exists() else {}
    )
    overrides_path = source_dir / "library_overrides.json"
    overrides = (
        load_json(overrides_path).get("entries", {}) if overrides_path.exists() else {}
    )
    alias_path = source_dir / "alias_review.json"
    alias_policy = (
        load_json(alias_path).get("entries", {}) if alias_path.exists() else {}
    )
    locale = "zh-CN" if "zh" in base_path.parent.name else "en"
    for item in substances:
        identifier = item.get("id")
        old_aliases = item.get("commonNames", [])
        override = overrides.get(identifier, {})
        fields = {
            **override.get("fields", {}),
            **override.get("locales", {}).get(locale, {}),
        }
        if "name" in fields or "id" in fields:
            raise ValueError(f"Cannot override immutable identity: {identifier}")
        for path, value in fields.items():
            target = item
            parts = path.split(".")
            for part in parts[:-1]:
                if not isinstance(target.get(part), dict):
                    target[part] = {}
                target = target[part]
            target[parts[-1]] = deepcopy(value)
        keyword_aliases = {
            alias
            for alias in ([] if "searchTerms" in fields else old_aliases)
            + item.get("commonNames", [])
            if (decision := alias_policy.get(normalized(alias), {})).get("status")
            == "reviewed"
            and decision.get("kind") == "search_keyword"
            and identifier in decision.get("affectedIds", [])
        }
        if keyword_aliases:
            item["commonNames"] = [
                alias
                for alias in item.get("commonNames", [])
                if alias not in keyword_aliases
            ]
            item["searchTerms"] = sorted(
                set(item.get("searchTerms", [])) | keyword_aliases
            )
        reviewed = manifest.get(identifier, {}).get("fields", {})
        locked = [
            field
            for field in ("commonNames", "categories", "roas", "interactions")
            if reviewed.get(field, {}).get("status")
            in {"verified", "not_applicable", "evidence_unavailable"}
        ]
        if locked:
            item["reviewedFields"] = locked
    timeline_coverage_path = source_dir / "timeline_coverage.json"
    if timeline_coverage_path.exists():
        substances = apply_timeline_coverage(
            substances, load_json(timeline_coverage_path)
        )
        missing_timeline = validate_timeline_coverage(substances)
        if missing_timeline:
            raise ValueError(
                "Timeline coverage is incomplete: " + ", ".join(missing_timeline[:20])
            )
    taxonomy_path = source_dir / "medication_taxonomy.json"
    if taxonomy_path.exists():
        definitions = {category["name"]: category for category in categories}
        for node in load_json(taxonomy_path)["nodes"]:
            label = node["labelZh"] if locale == "zh-CN" else node["labelEn"]
            for category_id in [node["id"], *node["legacyIds"]]:
                category = definitions.setdefault(
                    category_id, {"name": category_id, "color": 4287985101}
                )
                category.update(description=label, displayName=label, kind=node["kind"])
        categories = list(definitions.values())
    categories = filter_categories_by_usage(categories, substances)
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
    taxonomy_path = source_dir / "medication_taxonomy.json"
    if taxonomy_path.exists():
        (DEFAULT_OUTPUT.parent / "medication_taxonomy.json").write_bytes(
            taxonomy_path.read_bytes()
        )


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
