#!/usr/bin/env python3
import json
import re
from pathlib import Path


RAW_FILES = (
    Path("app/src/main/res/raw/substances.json"),
    Path("app/src/main/res/raw-zh-rCN/substances.json"),
)
CLINICAL_CATEGORIES = {"cardiovascular", "endocrine", "hrt-related"}
SEARCH_TERMS = (
    "hypertension",
    "heart failure",
    "angina",
    "feminizing hormone therapy",
    "oral",
    "sublingual",
    "patch",
    "gel",
    "injection",
    "infusion",
    "daily-total",
    "per-dose",
    "patch-delivery-rate",
    "label-regimen",
    "guideline-regimen",
    "study-regimen",
    "source needed",
)


def has_number(value) -> bool:
    return isinstance(value, (int, float))


def concrete_amount(text: str) -> bool:
    return text.strip().lower() != "source needed" and bool(re.search(r"\d", text))


def searchable_strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for child in value.values():
            yield from searchable_strings(child)
    elif isinstance(value, list):
        for child in value:
            yield from searchable_strings(child)


def main() -> int:
    errors: list[str] = []
    for path in RAW_FILES:
        data = json.loads(path.read_text(encoding="utf-8"))
        clinical = [
            substance
            for substance in data.get("substances", [])
            if CLINICAL_CATEGORIES & set(substance.get("categories", []))
        ]
        for substance in clinical:
            name = substance.get("name", "<unnamed>")
            prefix = f"{path}: {name}"
            if any(isinstance(roa.get("dose"), dict) for roa in substance.get("roas", [])):
                errors.append(f"{prefix}: clinical roas.dose remains")
            references = substance.get("doseUseReferences", [])
            if not references:
                errors.append(f"{prefix}: missing doseUseReferences")
                continue
            for index, reference in enumerate(references):
                reference_prefix = f"{prefix}: doseUseReferences[{index}]"
                amount = reference.get("amountText", "")
                refs = reference.get("sourceRefs", [])
                if concrete_amount(amount) and not refs:
                    errors.append(f"{reference_prefix}: concrete amountText missing sourceRefs")
                for range_index, dose_range in enumerate(reference.get("ranges", [])):
                    numeric = has_number(dose_range.get("min")) or has_number(
                        dose_range.get("max")
                    )
                    component_numeric = any(
                        has_number(component.get("min")) or has_number(component.get("max"))
                        for component in dose_range.get("components", [])
                    )
                    if (numeric or component_numeric) and not refs:
                        errors.append(
                            f"{reference_prefix}.ranges[{range_index}]: numeric range missing sourceRefs"
                        )
                    if not dose_range.get("basis"):
                        errors.append(
                            f"{reference_prefix}.ranges[{range_index}]: missing basis"
                        )
            categories = set(substance.get("categories", []))
            if "endocrine" in categories or "hrt-related" in categories:
                injection_reference = any(
                    "inject" in (
                        f"{reference.get('route', '')} {reference.get('formulation', '')}"
                    ).lower()
                    or "depot" in (
                        f"{reference.get('route', '')} {reference.get('formulation', '')}"
                    ).lower()
                    for reference in references
                )
                if injection_reference:
                    caveats = " ".join(
                        str(reference.get("note", ""))
                        + " "
                        + " ".join(
                            str(dose_range.get("note", ""))
                            for dose_range in reference.get("ranges", [])
                        )
                        for reference in references
                    ).lower()
                    if not any(term in caveats for term in ("timing", "interval", "peak")):
                        errors.append(f"{prefix}: injectable/depot HRT missing timing caveat")
        print(f"{path}: checked {len(clinical)} clinical entries")
        searchable = [text.lower() for substance in clinical for text in searchable_strings(substance)]
        for term in SEARCH_TERMS:
            if not any(term.lower() in text for text in searchable):
                errors.append(f"{path}: search term has no clinical match: {term}")

    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print("Clinical dose-reference validation passed with no roas.dose remnants.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
