#!/usr/bin/env python3
import json
from pathlib import Path
from urllib.parse import quote


INPUT_PATH = Path("app/src/main/res/raw/substances.json")
JSON_OUTPUT_PATH = Path("tools/drugdata/psychiatry/psychiatry_regional_source_map.json")
MD_OUTPUT_PATH = Path("tools/drugdata/psychiatry/psychiatry_regional_calibration_report.md")


def source_types(substance: dict) -> set[str]:
    refs = []
    refs.extend(substance.get("sourceRefs", []))
    refs.extend(substance.get("clinicalInfo", {}).get("sourceRefs", []))
    for course in substance.get("timeCourse", []):
        refs.extend(course.get("sourceRefs", []))
    for ref in substance.get("doseUseReferences", []):
        refs.extend(ref.get("sourceRefs", []))
    text = json.dumps(refs, ensure_ascii=False).lower()
    found = set()
    if "openfda" in text:
        found.add("openFDA")
    if "dailymed" in text:
        found.add("DailyMed")
    if "medicines.org.uk" in text or "emc" in text:
        found.add("UK eMC")
    if "nhc.gov.cn" in text or "国家基本药物目录" in text:
        found.add("China NEML")
    return found


def has_numeric_time_course(substance: dict) -> bool:
    fields = {
        "onset",
        "tmax",
        "peakEffect",
        "durationOfAction",
        "eliminationHalfLife",
        "timeToSteadyState",
        "washout",
    }
    return any(fields.intersection(course.keys()) for course in substance.get("timeCourse", []))


def has_numeric_dose_reference(substance: dict) -> bool:
    for ref in substance.get("doseUseReferences", []):
        for dose_range in ref.get("ranges", []):
            if dose_range.get("min") is not None or dose_range.get("max") is not None:
                return True
    return False


def search_urls(name: str) -> dict:
    q = quote(name)
    return {
        "dailyMed": f"https://dailymed.nlm.nih.gov/dailymed/search.cfm?query={q}",
        "openFDA": f"https://api.fda.gov/drug/label.json?search=openfda.generic_name:%22{quote(name.upper())}%22&limit=1",
        "ukEmc": f"https://www.medicines.org.uk/emc/search?q={q}",
        "ema": f"https://www.ema.europa.eu/en/search?search_api_fulltext={q}",
        "nmpaDataSearch": "https://www.nmpa.gov.cn/datasearch/home-index.html",
        "nhcEssentialMedicines": "https://www.nhc.gov.cn/wjw/jbywml/201810/8b68d28bd3754898b339e06da8c7d907/files/1733375109455_35909.pdf",
    }


def main() -> None:
    data = json.loads(INPUT_PATH.read_text(encoding="utf-8"))
    substances = data["substances"] if isinstance(data, dict) else data
    psychiatry = [s for s in substances if "clinical-psychiatry" in s.get("categories", [])]

    rows = []
    for substance in sorted(psychiatry, key=lambda item: item["name"].lower()):
        name = substance["name"]
        refs = source_types(substance)
        row = {
            "name": name,
            "commonNames": substance.get("commonNames", []),
            "categories": substance.get("categories", []),
            "confirmedSourceFamilies": sorted(refs),
            "hasNumericTimeCourse": has_numeric_time_course(substance),
            "hasNumericDoseUseReference": has_numeric_dose_reference(substance),
            "hasSourceNeeded": "source needed" in json.dumps(substance, ensure_ascii=False).lower(),
            "calibrationSearchUrls": search_urls(name),
        }
        rows.append(row)

    JSON_OUTPUT_PATH.write_text(json.dumps(rows, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    no_time = [row["name"] for row in rows if not row["hasNumericTimeCourse"]]
    no_dose = [row["name"] for row in rows if not row["hasNumericDoseUseReference"]]
    has_emc = [row["name"] for row in rows if "UK eMC" in row["confirmedSourceFamilies"]]
    has_openfda = [row["name"] for row in rows if "openFDA" in row["confirmedSourceFamilies"]]

    lines = [
        "# Psychiatry Regional Calibration Report",
        "",
        "This report maps every clinical psychiatry entry to regulatory database search targets for ongoing reconciliation.",
        "Numeric values are only written into substances data when a product label or SmPC has been reviewed.",
        "",
        f"- Clinical psychiatry entries: {len(rows)}",
        f"- Entries with openFDA label source: {len(has_openfda)}",
        f"- Entries with UK eMC SmPC source: {len(has_emc)}",
        f"- Entries with numeric timeCourse: {len(rows) - len(no_time)}",
        f"- Entries still needing numeric timeCourse review: {len(no_time)}",
        f"- Entries with numeric doseUseReferences: {len(rows) - len(no_dose)}",
        f"- Entries still needing numeric dose-reference review: {len(no_dose)}",
        "",
        "## Still Needs Numeric TimeCourse Review",
        "",
        ", ".join(no_time) if no_time else "None",
        "",
        "## Still Needs Numeric Dose Reference Review",
        "",
        ", ".join(no_dose) if no_dose else "None",
        "",
        "## Source Strategy",
        "",
        "- US labels: DailyMed and openFDA drug labeling.",
        "- EU/UK labels: EMA medicine search and UK eMC SmPC pages.",
        "- China references: NMPA data search and National Essential Medicines List; product-specific domestic labels require manual confirmation because public pages are not consistently machine-readable.",
        "",
    ]
    MD_OUTPUT_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")

    print(f"Wrote {len(rows)} calibration rows")
    print(f"Missing numeric timeCourse: {len(no_time)}")
    print(f"Missing numeric doseUseReferences: {len(no_dose)}")


if __name__ == "__main__":
    main()
