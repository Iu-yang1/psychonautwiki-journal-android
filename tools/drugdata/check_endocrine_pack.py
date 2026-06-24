#!/usr/bin/env python3
import json
import re
from pathlib import Path


SOURCE_DIR = Path("tools/drugdata/endocrine")
REQUIRED_FIELDS = (
    "clinicalInfo",
    "endocrineInfo",
    "timeCourse",
    "doseUseReferences",
    "hrtModelInfo",
)
REQUIRED_SOURCE_FIELDS = ("title", "url", "sourceType", "accessedDate")
DOSE_TIER_FIELDS = ("lightMin", "commonMin", "strongMin", "heavyMin")
INJECTION_ESTERS = {
    "Estradiol Valerate Injection",
    "Estradiol Cypionate Injection",
    "Estradiol Enanthate Injection",
}
NON_STANDARD_E2 = {"Ethinylestradiol", "Conjugated Estrogens"}
REQUIRED_HRT_EXTENSIONS = {
    "Bicalutamide",
    "Leuprolide Acetate",
    "Histrelin Implant",
    "Goserelin",
    "Triptorelin",
    "Degarelix",
    "Relugolix",
}
SEARCH_TERMS = (
    "CPA",
    "醋酸环丙孕酮",
    "E2",
    "Estradiol",
    "戊酸雌二醇",
    "estradiol valerate",
    "patch",
    "gel",
    "injection",
    "depot",
    "Tmax",
    "half-life",
    "LH",
    "FSH",
    "prolactin",
    "testosterone suppression",
    "meningioma",
    "ethinylestradiol",
    "conjugated estrogens",
    "micronized progesterone",
    "bicalutamide",
    "比卡鲁胺",
    "leuprolide",
    "亮丙瑞林",
    "histrelin",
    "goserelin",
    "triptorelin",
    "degarelix",
    "relugolix",
    "GnRH antagonist",
)


def source_refs_in(value):
    if isinstance(value, dict):
        if "sourceRefs" in value:
            yield from value.get("sourceRefs") or []
        for child in value.values():
            yield from source_refs_in(child)
    elif isinstance(value, list):
        for child in value:
            yield from source_refs_in(child)


def has_numeric_dose_text(text: str) -> bool:
    if text.strip().lower() == "source needed":
        return False
    return bool(re.search(r"\d", text))


def normalized(value: str) -> str:
    return re.sub(r"[- ]", "", value).lower()


def searchable_values(substance: dict) -> list[str]:
    clinical = substance.get("clinicalInfo") or {}
    endocrine = substance.get("endocrineInfo") or {}
    model = substance.get("hrtModelInfo") or {}
    values = [
        substance.get("name", ""),
        *substance.get("commonNames", []),
        *substance.get("categories", []),
        *clinical.get("atcCodes", []),
        *clinical.get("drugClass", []),
        *clinical.get("indications", []),
        *clinical.get("monitoring", []),
        *endocrine.get("hormoneClass", []),
        *endocrine.get("mechanisms", []),
        *endocrine.get("affectedHormones", []),
        *endocrine.get("monitoringLabs", []),
        *endocrine.get("assayCaveats", []),
        *endocrine.get("safetySignals", []),
        *endocrine.get("modelRoles", []),
        *model.get("modelRoles", []),
        *model.get("primaryModeledAnalytes", []),
        *model.get("requiredEventFields", []),
        *model.get("requiredLabFields", []),
        *model.get("caveats", []),
    ]
    for time_course in substance.get("timeCourse", []):
        values.extend(
            filter(
                None,
                [
                    time_course.get("route"),
                    time_course.get("formulation"),
                    "Tmax" if time_course.get("tmax") else None,
                    "half-life" if time_course.get("eliminationHalfLife") else None,
                    *time_course.get("notes", []),
                ],
            )
        )
    for reference in substance.get("doseUseReferences", []):
        values.extend(
            str(value)
            for value in reference.values()
            if isinstance(value, (str, int, float))
        )
    return [value for value in values if isinstance(value, str)]


def main() -> int:
    errors: list[str] = []
    warnings: list[str] = []
    files = sorted(SOURCE_DIR.rglob("*.json"))
    substances: list[tuple[Path, dict]] = []

    for path in files:
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception as error:
            errors.append(f"{path}: invalid JSON: {error}")
            continue
        for substance in data.get("substances", []):
            substances.append((path, substance))

    names = [substance.get("name") for _, substance in substances]
    if len(files) != 24:
        errors.append(f"Expected 24 endocrine JSON files, found {len(files)}")
    if len(substances) != 30:
        errors.append(f"Expected 30 endocrine substances, found {len(substances)}")
    if len(set(names)) != len(names):
        errors.append("Endocrine substance names must be unique")
    missing_extensions = sorted(REQUIRED_HRT_EXTENSIONS - set(names))
    if missing_extensions:
        errors.append(f"Missing HRT extension entries: {missing_extensions}")

    for path, substance in substances:
        name = substance.get("name", "<unnamed>")
        prefix = f"{path}: {name}"
        for field in ("name", "commonNames", "categories"):
            if not substance.get(field):
                errors.append(f"{prefix}: missing {field}")
        for field in REQUIRED_FIELDS:
            if field not in substance or substance[field] in (None, [], {}):
                errors.append(f"{prefix}: missing or empty {field}")

        for roa in substance.get("roas", []):
            dose = roa.get("dose") or {}
            present = [field for field in DOSE_TIER_FIELDS if field in dose]
            if present:
                errors.append(f"{prefix}: forbidden recreational dose tiers: {present}")

        for reference in substance.get("doseUseReferences", []):
            if has_numeric_dose_text(reference.get("amountText", "")) and not reference.get("sourceRefs"):
                errors.append(f"{prefix}: numeric dose reference has no sourceRefs")
            if reference.get("amountText", "").strip().lower() == "source needed":
                warnings.append(f"{name}: dose amount remains source needed")
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
                    errors.append(
                        f"{prefix}: numeric ranges[{range_index}] has no sourceRefs"
                    )

        refs = list(source_refs_in(substance))
        if not refs:
            errors.append(f"{prefix}: no sourceRefs found")
        for ref in refs:
            missing = [field for field in REQUIRED_SOURCE_FIELDS if not ref.get(field)]
            if missing:
                errors.append(f"{prefix}: sourceRef missing {missing}")

        if name in NON_STANDARD_E2:
            model = substance.get("hrtModelInfo") or {}
            caveats = " ".join(model.get("caveats", [])).lower()
            if model.get("modelCompatible") is not False:
                errors.append(f"{prefix}: must set modelCompatible false")
            if "estradiol" not in caveats or "not" not in caveats:
                errors.append(f"{prefix}: missing ordinary serum E2 incompatibility caveat")

        if name in INJECTION_ESTERS:
            time_course = (substance.get("timeCourse") or [{}])[0]
            text = " ".join(
                time_course.get("notes", [])
                + (substance.get("hrtModelInfo") or {}).get("caveats", [])
                + (substance.get("endocrineInfo") or {}).get("modelRoles", [])
            ).lower()
            for concept in ("peak", "trough", "timing"):
                if concept not in text:
                    errors.append(f"{prefix}: missing {concept} caveat")
            for flag in ("depotRelease", "injectionIntervalSensitive", "assayTimingSensitive"):
                if time_course.get(flag) is not True:
                    errors.append(f"{prefix}: {flag} must be true")

        if name == "Cyproterone Acetate":
            safety = " ".join(
                (substance.get("endocrineInfo") or {}).get("safetySignals", [])
            ).lower()
            for concept in ("meningioma", "hepato", "prolactin"):
                if concept not in safety:
                    errors.append(f"{prefix}: CPA safety missing {concept}")
            if substance.get("toxicities"):
                errors.append(f"{prefix}: CPA warning text duplicated in toxicities")
            if (substance.get("clinicalInfo") or {}).get("majorWarnings"):
                errors.append(f"{prefix}: CPA warning text duplicated in clinicalInfo")
            cpa_ranges = [
                dose_range
                for reference in substance.get("doseUseReferences", [])
                for dose_range in reference.get("ranges", [])
                if dose_range.get("basis") == "daily-total"
            ]
            if not any(
                dose_range.get("min") == 12.5 and dose_range.get("max") == 50
                for dose_range in cpa_ranges
            ):
                errors.append(f"{prefix}: missing 12.5-50 mg daily-total HRT range")

        if name == "Bicalutamide":
            text = " ".join(
                (substance.get("endocrineInfo") or {}).get("assayCaveats", [])
                + (substance.get("hrtModelInfo") or {}).get("caveats", [])
            ).lower()
            if "testosterone" not in text or "receptor" not in text:
                errors.append(f"{prefix}: must distinguish receptor blockade from testosterone suppression")
            if (substance.get("hrtModelInfo") or {}).get("modelCompatible") is not False:
                errors.append(f"{prefix}: HRT model must remain disabled")

        if name in {"Leuprolide Acetate", "Histrelin Implant", "Goserelin", "Triptorelin"}:
            time_course = (substance.get("timeCourse") or [{}])[0]
            if time_course.get("depotRelease") is not True:
                errors.append(f"{prefix}: GnRH agonist depot/implant must set depotRelease")
            text = " ".join(
                time_course.get("notes", [])
                + (substance.get("endocrineInfo") or {}).get("safetySignals", [])
            ).lower()
            if "flare" not in text:
                errors.append(f"{prefix}: missing initial agonist flare caveat")

        if name in {"Degarelix", "Relugolix"}:
            model = substance.get("hrtModelInfo") or {}
            if model.get("modelCompatible") is not False:
                errors.append(f"{prefix}: oncology-only antagonist evidence must not enable HRT model")
            text = " ".join(
                (substance.get("endocrineInfo") or {}).get("mechanisms", [])
                + model.get("caveats", [])
            ).lower()
            if "without an agonist flare" not in text:
                errors.append(f"{prefix}: missing no-agonist-flare distinction")

    for term in SEARCH_TERMS:
        if not any(
            normalized(term) in normalized(value)
            for _, substance in substances
            for value in searchable_values(substance)
        ):
            errors.append(f"Search term has no endocrine match: {term}")

    print(f"Checked endocrine files: {len(files)}")
    print(f"Checked endocrine substances: {len(substances)}")
    print(f"Entries with source-needed dose amount: {len(warnings)}")
    for warning in warnings:
        print(f"WARNING: {warning}")
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print("Endocrine/HRT data pack validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
