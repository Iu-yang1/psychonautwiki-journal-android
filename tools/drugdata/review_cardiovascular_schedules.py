#!/usr/bin/env python3
import json
import re
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


SOURCE_DIR = Path("tools/drugdata/cardiovascular")
BASE_FILES = (
    Path("app/src/main/res/raw/substances_base.json"),
    Path("app/src/main/res/raw-zh-rCN/substances_base.json"),
)
ACCESSED_DATE = "2026-06-24"
USER_AGENT = "psychonautwiki-journal-drugdata/1.0"

FREQUENCY_PATTERNS = (
    (r"\bonce[- ]daily\b|\bonce a day\b", "once daily"),
    (r"\btwice[- ]daily\b|\btwice a day\b|\bb\.?i\.?d\.?\b", "twice daily"),
    (r"\bthree times daily\b|\bt\.?i\.?d\.?\b", "three times daily"),
    (r"\bfour times daily\b|\bq\.?i\.?d\.?\b", "four times daily"),
    (r"\bevery other day\b", "every other day"),
    (
        r"\bevery\s+(?:\d+(?:\.\d+)?|four|six|eight|twelve|twenty-four|24)"
        r"\s*(?:to|-)\s*(?:\d+(?:\.\d+)?|six|eight|twelve|twenty-four|48)"
        r"\s+(?:hours?|days?|weeks?|months?)\b",
        None,
    ),
    (
        r"\bevery\s+(?:\d+(?:\.\d+)?|four|six|eight|twelve|twenty-four|24|48)"
        r"\s+(?:hours?|days?|weeks?|months?)\b",
        None,
    ),
    (r"\bweekly\b|\bonce a week\b", "weekly"),
    (r"\bmonthly\b|\bonce a month\b", "monthly"),
    (r"\bsingle (?:oral |intravenous |subcutaneous )?dose\b", "single dose"),
    (r"\bcontinuous (?:intravenous )?infusion\b", "continuous infusion"),
    (r"\bintravenous bolus\b|\badministered as a bolus\b", "intravenous bolus"),
    (r"\bdivided doses?\b", "divided doses"),
    (r"\bwith the morning meal\b.*?\bwith the evening meal\b", "morning and evening"),
)

INDIVIDUALIZATION_PATTERNS = (
    "titrate",
    "titration",
    "adjusted according",
    "adjust the dose",
    "individualized",
    "individualize",
    "based on response",
    "according to response",
    "based on inr",
    "according to inr",
    "anti-xa",
    "renal function",
    "creatinine clearance",
)

MANUAL_SCHEDULES = {
    "Acetazolamide": "The cited label uses indication-specific schedules ranging from once-daily or intermittent morning administration to divided daily dosing; renal function, electrolytes, and treatment response require separate review.",
    "Digitoxin": "The cited literature describes repeated oral maintenance use; frequency is patient- and concentration-specific and must be verified in an approved local label.",
    "Xipamide": "The cited literature describes oral dosing in clinical studies; no single product-label frequency was confirmed for this data pack.",
    "Canrenone": "The cited human pharmacokinetic study used study-specific oral administration; it is not a general prescribing schedule.",
    "Naftidrofuryl": "The cited review covers oral treatment regimens used across trials; frequency varies by product and study protocol.",
    "Nicergoline": "The cited regulatory review covers multiple products and indications; frequency must be checked in the applicable local product information.",
    "Nicotinyl Alcohol": "No current authoritative product-label frequency was confirmed; this legacy reference remains unsuitable for regimen guidance.",
    "Troxerutin": "The cited human pharmacokinetic source used a study-specific administration schedule; it is not a general prescribing schedule.",
    "Calcium Dobesilate": "The cited clinical study used a protocol-specific oral schedule; it is not a universal product regimen.",
    "Rutoside": "The cited human exposure study used a study-specific oral regimen; it is not a product-label prescribing schedule.",
    "Phenprocoumon": "Oral administration is individualized using anticoagulation response and bleeding risk; no fixed frequency can be used for dose adjustment.",
    "Mannitol": "The cited label uses indication-specific single or repeated intravenous infusions, with infusion duration and repetition guided by clinical response, fluid balance, renal function, and serum osmolality; no universal fixed interval applies.",
}

MANUAL_REGIMEN_REVIEWS = {
    "Moxonidine": {
        "amountText": "0.2 mg daily initially; the cited SmPC permits titration up to 0.6 mg total daily",
        "scheduleText": "Once daily in the morning; 0.4 mg may be given once daily or divided morning and evening, and 0.6 mg is divided",
        "ranges": [
            {
                "min": 0.2,
                "max": 0.6,
                "unit": "mg",
                "basis": "daily-total",
                "frequency": "once daily or divided twice daily according to total dose",
                "rangeKind": "label-regimen",
                "label": "SmPC regimen",
                "note": "Renal function and tolerability affect titration. This is not a dosing recommendation.",
                "components": [],
            }
        ],
    },
    "Bosentan": {
        "amountText": "62.5 mg twice daily for 4 weeks, then 125 mg twice daily in the cited adult PAH label",
        "scheduleText": "Twice daily; the cited label uses an initial 4-week phase before the maintenance regimen",
        "ranges": [
            {
                "min": 62.5,
                "max": 125,
                "unit": "mg",
                "basis": "per-dose",
                "frequency": "twice daily",
                "rangeKind": "label-regimen",
                "label": "Adult PAH label regimen",
                "note": "Liver monitoring and indication-specific restrictions apply. This is not a dosing recommendation.",
                "components": [],
            }
        ],
    },
    "Bendroflumethiazide": {
        "amountText": "2.5 mg once daily for hypertension; 5-10 mg once daily or on alternate days for edema in cited product information",
        "scheduleText": "Morning administration; frequency differs between hypertension, initial edema treatment, and intermittent maintenance",
        "ranges": [
            {
                "min": 2.5,
                "max": 10,
                "unit": "mg",
                "basis": "per-dose",
                "frequency": "once daily, alternate days, or intermittent weekly maintenance depending on indication",
                "rangeKind": "label-regimen",
                "label": "Product-information regimen",
                "note": "Indication, electrolytes, renal function, and volume status change interpretation. This is not a dosing recommendation.",
                "components": [],
            }
        ],
    },
    "Lercanidipine": {
        "amountText": "10 mg once daily; may be increased to 20 mg once daily in the cited product information",
        "scheduleText": "Once daily, at least 15 minutes before a meal",
        "ranges": [
            {
                "min": 10,
                "max": 20,
                "unit": "mg",
                "basis": "daily-total",
                "frequency": "once daily",
                "rangeKind": "label-regimen",
                "label": "Product-information regimen",
                "note": "Titration is response- and tolerability-dependent. This is not a dosing recommendation.",
                "components": [],
            }
        ],
    },
    "Eprosartan": {
        "amountText": "600 mg once daily initially; 400-800 mg total daily once or in two divided doses in the cited label",
        "scheduleText": "Once daily or divided twice daily depending on total daily regimen",
        "ranges": [
            {
                "min": 400,
                "max": 800,
                "unit": "mg",
                "basis": "daily-total",
                "frequency": "once daily or divided twice daily",
                "rangeKind": "label-regimen",
                "label": "Label regimen",
                "note": "Volume status, renal function, and combination-product context matter. This is not a dosing recommendation.",
                "components": [],
            }
        ],
    },
    "Ticlopidine": {
        "amountText": "250 mg twice daily with food in the cited label",
        "scheduleText": "Twice daily with meals",
        "ranges": [
            {
                "min": 250,
                "max": 250,
                "unit": "mg",
                "basis": "per-dose",
                "frequency": "twice daily",
                "rangeKind": "label-regimen",
                "label": "Label regimen",
                "note": "Serious hematologic toxicity and bleeding monitoring apply; availability varies by country. This is not a dosing recommendation.",
                "components": [],
            }
        ],
    },
    "Abciximab": {
        "amountText": "0.25 mg/kg IV bolus followed by 0.125 mcg/kg/min infusion, maximum 10 mcg/min, for 12 hours in the cited FDA label",
        "scheduleText": "Single IV bolus followed immediately by a continuous 12-hour infusion",
        "ranges": [
            {
                "min": 0.25,
                "max": 0.25,
                "unit": "mg/kg",
                "basis": "weight-based",
                "frequency": "single intravenous bolus",
                "rangeKind": "label-regimen",
                "label": "Bolus component",
                "note": "PCI protocol and bleeding-risk monitoring are essential. This is not a dosing recommendation.",
                "components": [],
            },
            {
                "min": 0.125,
                "max": 0.125,
                "unit": "mcg/kg/min",
                "basis": "weight-based",
                "frequency": "continuous infusion for 12 hours; maximum 10 mcg/min",
                "rangeKind": "label-regimen",
                "label": "Infusion component",
                "note": "This infusion rate must not be collapsed into a scalar mg range. This is not a dosing recommendation.",
                "components": [],
            },
        ],
    },
    "Vorapaxar": {
        "amountText": "2.08 mg orally once daily in the cited label",
        "scheduleText": "Once daily, with or without food",
        "ranges": [
            {
                "min": 2.08,
                "max": 2.08,
                "unit": "mg",
                "basis": "daily-total",
                "frequency": "once daily",
                "rangeKind": "label-regimen",
                "label": "Label regimen",
                "note": "Bleeding risk and concomitant antiplatelet therapy are central to interpretation. This is not a dosing recommendation.",
                "components": [],
            }
        ],
    },
    "Betrixaban": {
        "amountText": "160 mg initial single dose followed by 80 mg once daily; cited label includes 80 mg then 40 mg once daily in specified high-risk contexts",
        "scheduleText": "Single loading dose followed by once-daily administration for the label-specified treatment duration",
        "ranges": [
            {
                "min": 80,
                "max": 160,
                "unit": "mg",
                "basis": "per-dose",
                "frequency": "single initial dose",
                "rangeKind": "initial",
                "label": "Initial label dose",
                "note": "The lower initial dose applies only to specified renal-function or interaction contexts. This is not a dosing recommendation.",
                "components": [],
            },
            {
                "min": 40,
                "max": 80,
                "unit": "mg",
                "basis": "daily-total",
                "frequency": "once daily",
                "rangeKind": "maintenance",
                "label": "Maintenance label dose",
                "note": "Bleeding risk, renal function, and P-gp interactions change interpretation. This is not a dosing recommendation.",
                "components": [],
            },
        ],
    },
    "Acenocoumarol": {
        "amountText": "2-4 mg once daily initially without loading; maintenance commonly 1-8 mg once daily, individualized by PT/INR in the cited SmPC",
        "scheduleText": "Single dose at the same time each day; amount is adjusted according to PT/INR",
        "ranges": [
            {
                "min": 2,
                "max": 4,
                "unit": "mg",
                "basis": "daily-total",
                "frequency": "once daily during initial treatment",
                "rangeKind": "initial",
                "label": "Initial SmPC reference",
                "note": "Alternative loading regimens exist in the cited SmPC. This is not a dosing recommendation.",
                "components": [],
            },
            {
                "min": 1,
                "max": 8,
                "unit": "mg",
                "basis": "daily-total",
                "frequency": "once daily; individualized by PT/INR",
                "rangeKind": "maintenance",
                "label": "Maintenance SmPC reference",
                "note": "INR, indication, bleeding risk, diet, illness, and interactions govern dosing. This section must not be used for dose adjustment.",
                "components": [],
            },
        ],
    },
}


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, data: dict) -> None:
    path.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def query_names(substance: dict) -> list[str]:
    candidates = [substance.get("name", ""), *substance.get("commonNames", [])]
    result = []
    for candidate in candidates:
        candidate = candidate.strip()
        if not candidate or any("\u4e00" <= char <= "\u9fff" for char in candidate):
            continue
        if len(candidate) > 60 or candidate.lower().startswith(("atc-", "class ")):
            continue
        if candidate not in result:
            result.append(candidate)
    return result[:8]


def fetch_openfda_dosage(substance: dict) -> tuple[str | None, str | None]:
    fields = ("generic_name", "brand_name", "substance_name")
    for candidate in query_names(substance):
        for field in fields:
            query = urllib.parse.quote(f'openfda.{field}:"{candidate}"')
            url = f"https://api.fda.gov/drug/label.json?search={query}&limit=1"
            request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            try:
                with urllib.request.urlopen(request, timeout=20) as response:
                    result = json.load(response)["results"][0]
                dosage = (result.get("dosage_and_administration") or [None])[0]
                if dosage:
                    return dosage, candidate
            except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError, KeyError):
                pass
            time.sleep(0.03)
    return None, None


def normalize_frequency(match: re.Match, normalized: str | None) -> str:
    if normalized:
        return normalized
    return re.sub(r"\s+", " ", match.group(0).lower()).strip(" .,;:")


def summarize_schedule(text: str) -> str:
    compact = re.sub(r"\s+", " ", text)
    lower = compact.lower()
    frequencies: list[str] = []
    for pattern, normalized in FREQUENCY_PATTERNS:
        for match in re.finditer(pattern, lower, flags=re.IGNORECASE):
            value = normalize_frequency(match, normalized)
            if value not in frequencies:
                frequencies.append(value)
    individualized = any(pattern in lower for pattern in INDIVIDUALIZATION_PATTERNS)
    if frequencies:
        frequency_text = ", ".join(frequencies[:6])
        if len(frequencies) == 1 and not individualized:
            return (
                f"The cited label dosage section specifies {frequency_text}; "
                "verify the indication, formulation, and patient-specific conditions."
            )
        suffix = (
            " Dosing is also individualized or titrated in the cited section."
            if individualized
            else ""
        )
        return (
            f"The cited label contains multiple or context-specific schedules: {frequency_text}."
            f"{suffix} Verify the exact indication and treatment phase."
        )
    if individualized:
        return (
            "The cited label uses individualized, response-guided, laboratory-guided, "
            "or renal-function-adjusted administration; no single fixed frequency applies."
        )
    return (
        "The cited label dosage section is regimen- and indication-specific; "
        "no single frequency could be safely normalized from the section."
    )


def update_source_metadata(reference: dict) -> None:
    for source in reference.get("sourceRefs", []):
        if source.get("sourceType") == "regulatory-label":
            source.setdefault("labelSection", "Dosage and Administration")
            source["note"] = (
                "Dose-use frequency reviewed against the cited regulatory label or "
                "official label dataset on 2026-06-24."
            )


def main() -> int:
    reviewed = 0
    openfda_hits = 0
    manual = 0
    unresolved: list[str] = []
    source_files = [*sorted(SOURCE_DIR.glob("*.json")), *BASE_FILES]
    for path in source_files:
        data = load_json(path)
        changed = False
        for substance in data.get("substances", []):
            pending = [
                reference
                for reference in substance.get("doseUseReferences", [])
                if "source needed" in (reference.get("scheduleText") or "").lower()
            ]
            if not pending:
                continue
            name = substance.get("name", "<unnamed>")
            if name in MANUAL_SCHEDULES:
                schedule = MANUAL_SCHEDULES[name]
                manual += 1
            elif name in MANUAL_REGIMEN_REVIEWS:
                override = MANUAL_REGIMEN_REVIEWS[name]
                schedule = override["scheduleText"]
                manual += 1
            else:
                dosage, matched_name = fetch_openfda_dosage(substance)
                if dosage:
                    schedule = summarize_schedule(dosage)
                    openfda_hits += 1
                    for reference in pending:
                        reference["reviewedLabelMatch"] = matched_name
                else:
                    unresolved.append(name)
                    continue
            for reference in pending:
                reference["scheduleText"] = schedule
                if name in MANUAL_REGIMEN_REVIEWS:
                    reference.update(MANUAL_REGIMEN_REVIEWS[name])
                reference["scheduleReviewStatus"] = "reviewed"
                reference["scheduleReviewedDate"] = ACCESSED_DATE
                update_source_metadata(reference)
            reviewed += 1
            changed = True
        if changed:
            write_json(path, data)

    print(f"Reviewed substances: {reviewed}")
    print(f"openFDA label matches: {openfda_hits}")
    print(f"Manual non-US/literature reviews: {manual}")
    print(f"Unresolved substances: {len(set(unresolved))}")
    for name in sorted(set(unresolved)):
        print(f"- {name}")
    return 1 if unresolved else 0


if __name__ == "__main__":
    raise SystemExit(main())
