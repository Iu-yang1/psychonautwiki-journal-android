#!/usr/bin/env python3
import json
from pathlib import Path


SOURCE_DIR = Path("tools/drugdata/cardiovascular")
BASE_FILES = (
    Path("app/src/main/res/raw/substances_base.json"),
    Path("app/src/main/res/raw-zh-rCN/substances_base.json"),
)
DISCLAIMER = (
    "This is a label- or literature-reported regimen reference, not a dosing "
    "recommendation. It must not be used for self-medication or dose adjustment."
)


def write_json(path: Path, data: dict) -> None:
    path.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def route_text(route: str | None) -> str:
    return (route or "unknown").lower().replace("_", " ")


def source_refs(substance: dict, route: str) -> list[dict]:
    matching = [
        ref
        for time_course in substance.get("timeCourse", [])
        if route_text(time_course.get("route")) == route_text(route)
        for ref in time_course.get("sourceRefs", [])
    ]
    if matching:
        return matching
    clinical = (substance.get("clinicalInfo") or {}).get("sourceRefs", [])
    if clinical:
        return clinical
    return [
        ref
        for time_course in substance.get("timeCourse", [])
        for ref in time_course.get("sourceRefs", [])
    ]


def evidence_level(refs: list[dict]) -> str:
    if refs and refs[0].get("sourceType") == "regulatory-label":
        return "REGULATORY_LABEL"
    return "LITERATURE_OR_DATABASE"


def formulation_for(substance: dict, route: str) -> str | None:
    for time_course in substance.get("timeCourse", []):
        if route_text(time_course.get("route")) == route_text(route):
            return time_course.get("formulation")
    return None


def basis_for(route: str, unit: str) -> str:
    normalized = unit.lower()
    if "/m2" in normalized or "/m²" in normalized:
        return "body-surface-area-based"
    if "/kg" in normalized:
        return "weight-based"
    if route_text(route) == "transdermal" and ("/h" in normalized or "/day" in normalized):
        return "patch-delivery-rate"
    if "/min" in normalized or "/h" in normalized or "/hr" in normalized:
        return "infusion-rate"
    return "per-dose"


def readable(value: float | int) -> str:
    return str(int(value)) if float(value).is_integer() else str(value)


def migrated_reference(substance: dict, roa: dict) -> dict:
    dose = roa["dose"]
    route = route_text(roa.get("name"))
    values = [
        dose.get("lightMin"),
        dose.get("commonMin"),
        dose.get("strongMin"),
        dose.get("heavyMin"),
    ]
    numeric = [value for value in values if isinstance(value, (int, float))]
    minimum = min(numeric)
    maximum = max(numeric)
    unit = dose["units"]
    refs = source_refs(substance, route)
    indications = (substance.get("clinicalInfo") or {}).get("indications", [])
    indication = indications[0] if indications else "Clinical use context in cited source"
    note = (
        "This span was migrated from the former clinical use of roas.dose thresholds. "
        "The previous Light/Common/Strong/Heavy labels have been removed. "
        "Verify the indication-, formulation-, organ-function-, and monitoring-specific "
        "regimen in the cited source. "
        + DISCLAIMER
    )
    drug_class = " ".join((substance.get("clinicalInfo") or {}).get("drugClass", [])).lower()
    if "anticoagulant" in drug_class or "anticoagulation" in indication.lower():
        note += (
            " Anticoagulant interpretation also depends on bleeding risk and, where "
            "applicable, INR, anti-Xa, renal function, and interacting drugs."
        )
    return {
        "indication": indication,
        "population": "Adults or source-specific population; verify cited source",
        "route": route,
        "formulation": formulation_for(substance, route),
        "amountText": f"{readable(minimum)}-{readable(maximum)} {unit} legacy reference span",
        "scheduleText": "source needed for indication-specific schedule; verify cited source",
        "ranges": [
            {
                "min": minimum,
                "max": maximum,
                "unit": unit,
                "basis": basis_for(route, unit),
                "frequency": "source specific",
                "rangeKind": "protocol-range",
                "label": "Migrated clinical reference span",
                "note": note,
                "components": [],
            }
        ],
        "sourceType": refs[0].get("sourceType", "source-needed") if refs else "source-needed",
        "evidenceLevel": evidence_level(refs),
        "note": note,
        "sourceRefs": refs,
    }


def dose_range(
    minimum,
    maximum,
    unit,
    basis,
    frequency,
    range_kind,
    label,
    note=DISCLAIMER,
    components=None,
):
    return {
        "min": minimum,
        "max": maximum,
        "unit": unit,
        "basis": basis,
        "frequency": frequency,
        "rangeKind": range_kind,
        "label": label,
        "note": note,
        "components": components or [],
    }


def reference(
    substance: dict,
    indication: str,
    route: str,
    formulation: str,
    amount_text: str,
    schedule_text: str,
    ranges: list[dict],
    note: str = DISCLAIMER,
) -> dict:
    refs = source_refs(substance, route)
    return {
        "indication": indication,
        "population": "Adults; indication- and label-specific",
        "route": route,
        "formulation": formulation,
        "amountText": amount_text,
        "scheduleText": schedule_text,
        "ranges": ranges,
        "sourceType": refs[0].get("sourceType", "source-needed") if refs else "source-needed",
        "evidenceLevel": evidence_level(refs),
        "note": note,
        "sourceRefs": refs,
    }


def apply_examples(substance: dict) -> None:
    name = substance["name"]
    monitoring_note = (
        "Clinical context, indication, renal function, electrolytes, blood pressure, "
        "heart rate, ECG, INR/anti-Xa, and interacting drugs may change interpretation. "
        + DISCLAIMER
    )
    if name == "Amlodipine":
        substance["doseUseReferences"] = [
            reference(
                substance,
                "Hypertension in the cited adult label",
                "oral",
                "tablet",
                "5 mg initial label dose; maximum 10 mg once daily",
                "Once daily",
                [
                    dose_range(2.5, 5, "mg", "per-dose", "once daily", "initial", "Initial reference"),
                    dose_range(None, 10, "mg", "daily-total", "once daily", "maximum-labeled", "Upper bound"),
                ],
                monitoring_note,
            )
        ]
    elif name == "Metoprolol":
        substance["doseUseReferences"] = [
            reference(
                substance,
                "Hypertension with immediate-release metoprolol tartrate in the cited label",
                "oral",
                "immediate-release tablet",
                "100-450 mg total daily dose in the cited label",
                "Single or divided daily doses depending on regimen",
                [dose_range(100, 450, "mg", "daily-total", "daily", "label-regimen", "Label regimen")],
                monitoring_note,
            )
        ]
    elif name == "Digoxin":
        substance["doseUseReferences"] = [
            reference(
                substance,
                "Maintenance therapy in the cited oral tablet label",
                "oral",
                "tablet",
                "0.0625-0.25 mg once daily in commonly cited adult maintenance contexts",
                "Once daily; individualized by renal function, age, lean body weight, response, and concentration timing",
                [dose_range(0.0625, 0.25, "mg", "daily-total", "once daily", "maintenance", "Maintenance reference")],
                "Digoxin has a narrow therapeutic index. Concentration sampling time, renal function, electrolytes, ECG, symptoms, and interactions are essential. " + DISCLAIMER,
            )
        ]
    elif name == "Nitroglycerin":
        substance["doseUseReferences"] = [
            reference(
                substance,
                "Acute angina in the cited sublingual label",
                "sublingual",
                "tablet",
                "0.3-0.6 mg per sublingual dose",
                "May be repeated at 5-minute intervals up to the cited label limit",
                [dose_range(0.3, 0.6, "mg", "per-dose", "5-minute intervals when indicated by label", "label-regimen", "Sublingual label regimen")],
                monitoring_note,
            ),
            *[
                migrated
                for migrated in substance.get("doseUseReferences", [])
                if route_text(migrated.get("route")) in {"transdermal", "intravenous"}
            ],
        ]
    elif name == "Warfarin":
        substance["doseUseReferences"] = [
            reference(
                substance,
                "Anticoagulation initiation in the cited label",
                "oral",
                "tablet",
                "Initial dose is individualized; the cited label commonly describes 2-5 mg daily initiation",
                "Subsequent dosing is adjusted according to INR and clinical context",
                [dose_range(2, 5, "mg", "daily-total", "initial daily dosing", "initial", "Initial label reference")],
                "Warfarin must be individualized by INR, indication, bleeding risk, diet, genetics, illness, and interactions. This section must not be used for dose adjustment. " + DISCLAIMER,
            )
        ]
    elif name == "Apixaban":
        substance["doseUseReferences"] = [
            reference(
                substance,
                "Multiple anticoagulation indications in the cited label",
                "oral",
                "tablet",
                "2.5-10 mg per dose across distinct cited label regimens",
                "Once or twice daily depending on indication and treatment phase",
                [dose_range(2.5, 10, "mg", "per-dose", "indication- and phase-specific", "label-regimen", "Label regimen span")],
                "The endpoints represent different indications and phases and must not be treated as an interchangeable titration range. Renal function, age, weight, bleeding risk, and interacting drugs matter. " + DISCLAIMER,
            )
        ]
    elif name == "Sacubitril/Valsartan":
        substance["doseUseReferences"] = [
            reference(
                substance,
                "Heart failure in the cited Entresto label",
                "oral",
                "tablet",
                "Component strengths 24/26 mg, 49/51 mg, and 97/103 mg",
                "Twice daily in the cited adult label; starting strength depends on prior therapy and clinical factors",
                [
                    dose_range(
                        None,
                        None,
                        "mg",
                        "component-dose",
                        "twice daily",
                        "label-regimen",
                        "Sacubitril/valsartan component strengths",
                        "Component-aware display; do not collapse the combination into a single scalar dose. " + DISCLAIMER,
                        [
                            {"substance": "sacubitril", "min": 24, "max": 97, "unit": "mg"},
                            {"substance": "valsartan", "min": 26, "max": 103, "unit": "mg"},
                        ],
                    )
                ],
                monitoring_note,
            )
        ]
    elif name == "Furosemide":
        substance["doseUseReferences"] = [
            reference(
                substance,
                "Edema in the cited oral label",
                "oral",
                "tablet",
                "20-80 mg initial oral dose; selected severe edematous states may use higher total daily amounts under close supervision",
                "Response-guided and indication-specific",
                [
                    dose_range(20, 80, "mg", "per-dose", "initial dose", "initial", "Initial reference"),
                    dose_range(None, 600, "mg", "daily-total", "daily under close medical supervision", "maximum-labeled", "Upper label context"),
                ],
                monitoring_note,
            )
        ]
    elif name == "Atorvastatin":
        substance["doseUseReferences"] = [
            reference(
                substance,
                "Hyperlipidemia and cardiovascular-risk indications in the cited label",
                "oral",
                "tablet",
                "10-80 mg once daily",
                "Once daily",
                [dose_range(10, 80, "mg", "daily-total", "once daily", "label-regimen", "Label regimen")],
                monitoring_note,
            )
        ]
    elif name == "Cilostazol":
        substance["doseUseReferences"] = [
            reference(
                substance,
                "Intermittent claudication in the cited label",
                "oral",
                "tablet",
                "100 mg twice daily; 50 mg twice daily with specified CYP inhibitors",
                "Twice daily before or after meals as specified by the cited label",
                [dose_range(50, 100, "mg", "per-dose", "twice daily", "label-regimen", "Label regimen")],
                monitoring_note,
            )
        ]


def migrate_substance(substance: dict) -> int:
    migrated = []
    count = 0
    for roa in substance.get("roas", []):
        if isinstance(roa.get("dose"), dict):
            migrated.append(migrated_reference(substance, roa))
            del roa["dose"]
            count += 1
    if migrated:
        substance["doseUseReferences"] = migrated
    for reference in substance.get("doseUseReferences", []):
        if reference.get("scheduleText") == "Source- and indication-specific; verify cited source":
            reference["scheduleText"] = (
                "source needed for indication-specific schedule; verify cited source"
            )
    if str(substance.get("dosageRemark", "")).startswith("临床参考剂量"):
        substance.pop("dosageRemark", None)
    apply_examples(substance)
    return count


def main() -> None:
    substances = 0
    routes = 0
    for path in sorted(SOURCE_DIR.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        before = json.dumps(data, ensure_ascii=False, sort_keys=True)
        for substance in data.get("substances", []):
            migrated = migrate_substance(substance)
            if migrated:
                substances += 1
                routes += migrated
        after = json.dumps(data, ensure_ascii=False, sort_keys=True)
        if before != after:
            write_json(path, data)
    for path in BASE_FILES:
        data = json.loads(path.read_text(encoding="utf-8"))
        before = json.dumps(data, ensure_ascii=False, sort_keys=True)
        for substance in data.get("substances", []):
            if not (
                {"cardiovascular", "endocrine", "hrt-related"}
                & set(substance.get("categories", []))
            ):
                continue
            migrated = migrate_substance(substance)
            if migrated:
                substances += 1
                routes += migrated
        after = json.dumps(data, ensure_ascii=False, sort_keys=True)
        if before != after:
            write_json(path, data)
    print(f"Migrated cardiovascular substances: {substances}")
    print(f"Migrated route dose objects: {routes}")


if __name__ == "__main__":
    main()
