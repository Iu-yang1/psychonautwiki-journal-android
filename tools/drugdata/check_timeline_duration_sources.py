#!/usr/bin/env python3
import argparse
import json
from pathlib import Path


CLINICAL_MARKERS = {
    "clinical-psychiatry",
    "cardiovascular",
    "endocrine",
    "hrt-related",
    "prescription-medicine",
}

HANDLED_EFFECT_TIMELINE_STATUSES = {
    "not-recommended",
    "pk-estimated",
    "source-needed",
}

ROUTE_ALIASES = {
    "ORAL": {"oral", "po", "by mouth"},
    "SUBLINGUAL": {"sublingual", "sl"},
    "BUCCAL": {"buccal"},
    "INSUFFLATED": {"intranasal", "insufflated", "nasal", "nasal spray"},
    "RECTAL": {"rectal"},
    "TRANSDERMAL": {"transdermal", "topical patch", "patch"},
    "SUBCUTANEOUS": {"subcutaneous", "sc", "subcut", "subcutaneous injection"},
    "INTRAMUSCULAR": {"intramuscular", "im", "intramuscular injection"},
    "INTRAVENOUS": {"intravenous", "iv", "iv infusion", "infusion", "intravenous infusion"},
    "SMOKED": {"smoked", "vaporized"},
    "INHALED": {"inhaled"},
}


def normalize(value: str | None) -> str:
    return (value or "").strip().lower().replace("_", " ")


def route_words(value: str | None) -> set[str]:
    normalized = normalize(value)
    for separator in ["/", ",", ";"]:
        normalized = normalized.replace(separator, " ")
    return {word for word in normalized.split(" ") if word}


def route_matches(time_course_route: str | None, app_route: str) -> bool:
    normalized = normalize(time_course_route)
    words = route_words(time_course_route)
    route_text = normalize(app_route)
    aliases = ROUTE_ALIASES[app_route]
    return (
        normalized == route_text
        or normalized in aliases
        or any(alias in words or alias in normalized for alias in aliases | {route_text})
    )


def has_number(time_value: object) -> bool:
    return isinstance(time_value, dict) and (
        isinstance(time_value.get("min"), (int, float))
        or isinstance(time_value.get("max"), (int, float))
    )


def has_reliable_timeline_end(time_course: dict) -> bool:
    return has_number(time_course.get("durationOfAction")) or has_number(time_course.get("washout"))


def has_handled_timeline_status(time_course: dict) -> bool:
    return str(time_course.get("effectTimelineStatus") or "").strip().lower() in HANDLED_EFFECT_TIMELINE_STATUSES


def roa_has_duration(roa: dict) -> bool:
    duration = roa.get("duration")
    if not isinstance(duration, dict):
        return False
    return any(
        has_number(duration.get(field_name))
        for field_name in ["onset", "comeup", "peak", "offset", "total", "afterglow"]
    )


def app_routes_for_substance(substance: dict) -> list[str]:
    route_names = {
        normalize(roa.get("name"))
        for roa in substance.get("roas", [])
        if isinstance(roa, dict) and roa.get("name")
    }
    matched_routes = [
        app_route
        for app_route, aliases in ROUTE_ALIASES.items()
        if any(route_name == normalize(app_route) or route_name in aliases for route_name in route_names)
    ]
    if matched_routes:
        return matched_routes
    return list(ROUTE_ALIASES)


def find_missing_duration_sources(substances: list[dict]) -> list[dict]:
    rows = []
    for substance in substances:
        categories = set(substance.get("categories", []))
        if not categories.intersection(CLINICAL_MARKERS):
            continue
        time_courses = substance.get("timeCourse", [])
        if not time_courses:
            continue
        app_routes = app_routes_for_substance(substance)
        roa_duration_routes = {
            app_route
            for app_route, aliases in ROUTE_ALIASES.items()
            for roa in substance.get("roas", [])
            if isinstance(roa, dict)
            and roa_has_duration(roa)
            and (
                normalize(roa.get("name")) == normalize(app_route)
                or normalize(roa.get("name")) in aliases
            )
        }
        for app_route in app_routes:
            if app_route in roa_duration_routes:
                continue
            for time_course in time_courses:
                if not route_matches(time_course.get("route"), app_route):
                    continue
                if has_reliable_timeline_end(time_course):
                    continue
                if has_handled_timeline_status(time_course):
                    continue
                if not has_number(time_course.get("eliminationHalfLife")):
                    continue
                rows.append(
                    {
                        "name": substance.get("name"),
                        "appRoute": app_route,
                        "timeCourseRoute": time_course.get("route"),
                        "formulation": time_course.get("formulation"),
                        "halfLife": time_course.get("eliminationHalfLife"),
                        "categories": substance.get("categories", []),
                    }
                )
    return rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--input",
        default="app/src/main/res/raw/substances.json",
        help="Generated substances JSON to audit.",
    )
    parser.add_argument(
        "--fail-on-missing",
        action="store_true",
        help="Exit with status 1 if half-life-only timeline candidates are found.",
    )
    args = parser.parse_args()

    data = json.loads(Path(args.input).read_text(encoding="utf-8"))
    rows = find_missing_duration_sources(data.get("substances", []))
    print(f"Half-life-only timeline candidates: {len(rows)}")
    for row in rows:
        print(json.dumps(row, ensure_ascii=False, sort_keys=True))
    return 1 if rows and args.fail_on_missing else 0


if __name__ == "__main__":
    raise SystemExit(main())
