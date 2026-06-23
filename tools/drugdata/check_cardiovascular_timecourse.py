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


def has_drawable_time_course(substance: dict) -> bool:
    for time_course in substance.get("timeCourse", []):
        if any(time_course.get(field) for field in DRAWABLE_FIELDS):
            return True
    return False


def main() -> int:
    missing: list[tuple[str, str]] = []
    total = 0
    for path in sorted(SOURCE_DIR.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        for substance in data.get("substances", []):
            total += 1
            if not has_drawable_time_course(substance):
                missing.append((path.name, substance.get("name", "<unnamed>")))

    print(f"Checked cardiovascular substances: {total}")
    if not missing:
        print("All cardiovascular substances have drawable pharmacokinetic/time-course data.")
        return 0

    print("Missing drawable timeCourse data:")
    for filename, name in missing:
        print(f"- {filename}: {name}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
