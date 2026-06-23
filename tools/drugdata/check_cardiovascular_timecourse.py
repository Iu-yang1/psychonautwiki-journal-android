#!/usr/bin/env python3
import json
from pathlib import Path


SOURCE_DIR = Path("tools/drugdata/cardiovascular")
DRAWABLE_FIELDS = (
    "onset",
    "tmax",
    "peakEffect",
    "durationOfAction",
    "eliminationHalfLife",
    "washout",
)
ATC_PREFIXES = ("B01", "C01", "C02", "C03", "C04", "C05", "C07", "C08", "C09", "C10")


def contains_chinese(values: list[str]) -> bool:
    return any(any("\u4e00" <= char <= "\u9fff" for char in value) for value in values)


def has_numeric_time_value(time_value: dict | None) -> bool:
    if not isinstance(time_value, dict):
        return False
    return isinstance(time_value.get("min"), (int, float)) or isinstance(time_value.get("max"), (int, float))


def has_drawable_time_course(substance: dict) -> bool:
    for time_course in substance.get("timeCourse", []):
        if any(has_numeric_time_value(time_course.get(field)) for field in DRAWABLE_FIELDS):
            return True
    return False


def has_source_refs(substance: dict) -> bool:
    clinical = substance.get("clinicalInfo") or {}
    if clinical.get("sourceRefs"):
        return True
    return any(time_course.get("sourceRefs") for time_course in substance.get("timeCourse", []))


def atc_codes(substance: dict) -> list[str]:
    clinical = substance.get("clinicalInfo") or {}
    return [code for code in clinical.get("atcCodes", []) if isinstance(code, str)]


def main() -> int:
    missing_time_course: list[tuple[str, str]] = []
    missing_atc: list[tuple[str, str]] = []
    missing_source: list[tuple[str, str]] = []
    missing_chinese: list[tuple[str, str]] = []
    group_counts = {prefix: 0 for prefix in ATC_PREFIXES}
    total = 0
    for path in sorted(SOURCE_DIR.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        for substance in data.get("substances", []):
            total += 1
            name = substance.get("name", "<unnamed>")
            if not has_drawable_time_course(substance):
                missing_time_course.append((path.name, name))
            codes = atc_codes(substance)
            if not any(code.startswith(ATC_PREFIXES) for code in codes):
                missing_atc.append((path.name, name))
            for code in codes:
                prefix = code[:3]
                if prefix in group_counts:
                    group_counts[prefix] += 1
            if not has_source_refs(substance):
                missing_source.append((path.name, name))
            if not contains_chinese(substance.get("commonNames", [])):
                missing_chinese.append((path.name, name))

    print(f"Checked cardiovascular substances: {total}")
    print("ATC group counts:")
    for prefix, count in group_counts.items():
        print(f"- {prefix}: {count}")
    if not (missing_time_course or missing_atc or missing_source or missing_chinese):
        print("All cardiovascular substances have numeric drawable timeCourse data, ATC codes, sourceRefs, and Chinese common names.")
        return 0

    if missing_time_course:
        print("Missing numeric drawable timeCourse data:")
        for filename, name in missing_time_course:
            print(f"- {filename}: {name}")
    if missing_atc:
        print("Missing B01/C ATC code:")
        for filename, name in missing_atc:
            print(f"- {filename}: {name}")
    if missing_source:
        print("Missing sourceRefs:")
        for filename, name in missing_source:
            print(f"- {filename}: {name}")
    if missing_chinese:
        print("Missing Chinese commonNames:")
        for filename, name in missing_chinese:
            print(f"- {filename}: {name}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
