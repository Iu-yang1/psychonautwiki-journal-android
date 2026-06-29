#!/usr/bin/env python3
import json
from pathlib import Path


SOURCE_DIR = Path("tools/drugdata/cardiovascular")
DRAWABLE_FIELDS = (
    "onset",
    "tmax",
    "peakEffect",
    "peakWindow",
    "durationOfAction",
    "eliminationHalfLife",
    "washout",
)
ATC_PREFIXES = (
    "A10",
    "B01",
    "C01",
    "C02",
    "C03",
    "C04",
    "C05",
    "C07",
    "C08",
    "C09",
    "C10",
    "G04",
)


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


def has_legacy_roa_dose(substance: dict) -> bool:
    return any(isinstance(roa.get("dose"), dict) for roa in substance.get("roas", []))


def validate_dose_references(substance: dict) -> list[str]:
    errors: list[str] = []
    references = substance.get("doseUseReferences", [])
    if not references:
        return ["missing doseUseReferences"]
    for index, reference in enumerate(references):
        prefix = f"doseUseReferences[{index}]"
        if not reference.get("amountText"):
            errors.append(f"{prefix} missing amountText")
        for range_index, dose_range in enumerate(reference.get("ranges", [])):
            has_number = isinstance(dose_range.get("min"), (int, float)) or isinstance(
                dose_range.get("max"), (int, float)
            )
            has_component_number = any(
                isinstance(component.get("min"), (int, float))
                or isinstance(component.get("max"), (int, float))
                for component in dose_range.get("components", [])
            )
            if (has_number or has_component_number) and not reference.get("sourceRefs"):
                errors.append(f"{prefix}.ranges[{range_index}] numeric range missing sourceRefs")
    clinical = substance.get("clinicalInfo") or {}
    anticoagulant = " ".join(
        clinical.get("drugClass", []) + clinical.get("indications", [])
    ).lower()
    if "anticoagulant" in anticoagulant or "anticoagulation" in anticoagulant:
        caveat = " ".join(
            str(reference.get("note", ""))
            + " "
            + " ".join(str(item.get("note", "")) for item in reference.get("ranges", []))
            for reference in references
        ).lower()
        if not any(term in caveat for term in ("inr", "anti-xa", "bleeding")):
            errors.append("anticoagulant dose reference missing INR/anti-Xa/bleeding caveat")
    return errors


def validate_peak_effect_semantics(substance: dict) -> list[str]:
    errors: list[str] = []
    pk_markers = ("plasma", "concentration", "pharmacokinetic", "metabolite", "cmax", "tmax")
    allowed_basis = ("clinical effect", "pharmacodynamic effect")
    for index, time_course in enumerate(substance.get("timeCourse", [])):
        peak_effect = time_course.get("peakEffect")
        if not isinstance(peak_effect, dict):
            continue
        basis = str(peak_effect.get("basis", "")).lower()
        note = str(peak_effect.get("note", "")).lower()
        if any(marker in basis for marker in allowed_basis):
            continue
        if any(marker in f"{basis} {note}" for marker in pk_markers):
            errors.append(f"timeCourse[{index}].peakEffect appears to describe a PK peak")
    return errors


def main() -> int:
    missing_time_course: list[tuple[str, str]] = []
    missing_atc: list[tuple[str, str]] = []
    missing_source: list[tuple[str, str]] = []
    missing_chinese: list[tuple[str, str]] = []
    dose_reference_errors: list[tuple[str, str, str]] = []
    group_counts = {prefix: 0 for prefix in ATC_PREFIXES}
    total = 0
    for path in sorted(SOURCE_DIR.rglob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(data, dict) or "substances" not in data:
            continue
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
            if has_legacy_roa_dose(substance):
                dose_reference_errors.append((path.name, name, "legacy roas.dose remains"))
            for error in validate_dose_references(substance):
                dose_reference_errors.append((path.name, name, error))
            for error in validate_peak_effect_semantics(substance):
                dose_reference_errors.append((path.name, name, error))

    print(f"Checked cardiovascular substances: {total}")
    print("ATC group counts:")
    for prefix, count in group_counts.items():
        print(f"- {prefix}: {count}")
    if not (
        missing_time_course
        or missing_atc
        or missing_source
        or missing_chinese
        or dose_reference_errors
    ):
        print(
            "All cardiovascular substances have numeric drawable timeCourse data, "
            "ATC codes, sourceRefs, Chinese common names, and migrated doseUseReferences."
        )
        return 0

    if missing_time_course:
        print("Missing numeric drawable timeCourse data:")
        for filename, name in missing_time_course:
            print(f"- {filename}: {name}")
    if missing_atc:
        print("Missing supported cardiovascular-context ATC code:")
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
    if dose_reference_errors:
        print("Dose reference migration errors:")
        for filename, name, error in dose_reference_errors:
            print(f"- {filename}: {name}: {error}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
