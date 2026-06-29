#!/usr/bin/env python3
import json
from pathlib import Path


RAW_FILES = (
    Path("app/src/main/res/raw/substances.json"),
    Path("app/src/main/res/raw-zh-rCN/substances.json"),
)
SOURCE_DIR = Path("tools/drugdata")

PK_PEAK_MARKERS = (
    "plasma",
    "concentration",
    "pharmacokinetic",
    "metabolite",
    "cmax",
    "tmax",
)
ALLOWED_PEAK_EFFECT_BASIS = (
    "clinical effect",
    "pharmacodynamic effect",
)


def load_packs(path: Path) -> list[dict]:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return []
    if isinstance(data, dict):
        return [data]
    if isinstance(data, list):
        return [item for item in data if isinstance(item, dict)]
    return []


def iter_substances(path: Path):
    for pack in load_packs(path):
        for substance in pack.get("substances", []) or []:
            if isinstance(substance, dict):
                yield substance


def is_invalid_peak_effect(value: dict) -> bool:
    basis = str(value.get("basis", "")).lower()
    note = str(value.get("note", "")).lower()
    if any(marker in basis for marker in ALLOWED_PEAK_EFFECT_BASIS):
        return False
    text = f"{basis} {note}"
    return any(marker in text for marker in PK_PEAK_MARKERS)


def main() -> int:
    errors: list[str] = []
    paths = sorted(SOURCE_DIR.rglob("*.json")) + [path for path in RAW_FILES if path.exists()]
    for path in paths:
        for substance in iter_substances(path):
            for index, time_course in enumerate(substance.get("timeCourse", []) or []):
                if not isinstance(time_course, dict):
                    continue
                peak_effect = time_course.get("peakEffect")
                if isinstance(peak_effect, dict) and is_invalid_peak_effect(peak_effect):
                    errors.append(
                        f"{path}: {substance.get('name', '<unnamed>')} "
                        f"timeCourse[{index}].peakEffect appears to describe PK peak: "
                        f"basis={peak_effect.get('basis')!r}, note={peak_effect.get('note')!r}"
                    )

    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print("Time-course semantic validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
