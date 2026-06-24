#!/usr/bin/env python3
import json
from pathlib import Path


ROOT = Path("tools/drugdata/endocrine")
ACCESSED_DATE = "2026-06-24"


def source(
    title: str,
    url: str,
    source_type: str,
    evidence_level: str,
    label_section: str | None = None,
    note: str | None = None,
) -> dict:
    result = {
        "title": title,
        "url": url,
        "sourceType": source_type,
        "accessedDate": ACCESSED_DATE,
        "evidenceLevel": evidence_level,
    }
    if label_section:
        result["labelSection"] = label_section
    if note:
        result["note"] = note
    return result


SOURCES = {
    "endocrine_guideline": source(
        "Endocrine Treatment of Gender-Dysphoric/Gender-Incongruent Persons",
        "https://www.endocrine.org/clinical-practice-guidelines/gender-dysphoria-gender-incongruence",
        "clinical-guideline",
        "CLINICAL_GUIDELINE",
    ),
    "wpath": source(
        "WPATH Standards of Care Version 8",
        "https://wpath.org/publications/soc8/",
        "clinical-guideline",
        "CLINICAL_GUIDELINE",
    ),
    "ucsf": source(
        "UCSF Overview of feminizing hormone therapy",
        "https://transcare.ucsf.edu/guidelines/feminizing-hormone-therapy",
        "clinical-guideline",
        "CLINICAL_GUIDELINE",
    ),
    "assay": source(
        "Oral estrogen leads to falsely low concentrations of estradiol in a common immunoassay",
        "https://pubmed.ncbi.nlm.nih.gov/35015702/",
        "assay-study",
        "HUMAN_STUDY",
        note="Assay bias can depend on formulation and assay platform.",
    ),
    "progestogen_review": source(
        "Progestogen Use in Gender-Affirming Hormone Therapy: A Systematic Review",
        "https://pubmed.ncbi.nlm.nih.gov/36007714/",
        "systematic-review",
        "SYSTEMATIC_REVIEW",
    ),
    "oral_e2": source(
        "DailyMed Activella label",
        "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=d4b0c1dd-aae9-4ca2-958c-71dbe5d0fab0",
        "regulatory-label",
        "REGULATORY_LABEL",
        "12.3 Pharmacokinetics",
    ),
    "sublingual_e2": source(
        "Pharmacokinetics of Sublingual Versus Oral Estradiol in Transgender Women",
        "https://pubmed.ncbi.nlm.nih.gov/34781041/",
        "human-pk-study",
        "HUMAN_STUDY",
    ),
    "buccal_e2": source(
        "Pharmacokinetics after transbuccal administration to postmenopausal women",
        "https://pubmed.ncbi.nlm.nih.gov/12841880/",
        "human-pk-study",
        "HUMAN_STUDY",
    ),
    "patch": source(
        "DailyMed Estradiol Transdermal System label",
        "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=396686ba-ab8d-4223-80e7-9c4f7ddb7852",
        "regulatory-label",
        "REGULATORY_LABEL",
        "12.3 Pharmacokinetics",
    ),
    "patch_removal": source(
        "DailyMed Estradiol Transdermal System pharmacokinetics",
        "https://dailymed.nlm.nih.gov/dailymed/fda/fdaDrugXsl.cfm?setid=bf185599-2138-4083-9c52-2b95dce09ae0",
        "regulatory-label",
        "REGULATORY_LABEL",
        "12.3 Pharmacokinetics",
    ),
    "gel": source(
        "DailyMed Estradiol Gel 0.1% label",
        "https://dailymed.nlm.nih.gov/dailymed/lookup.cfm?setid=d4a3606f-83b2-45df-bc77-faebfa796f9a",
        "regulatory-label",
        "REGULATORY_LABEL",
        "12.3 Pharmacokinetics",
    ),
    "spray": source(
        "DailyMed Evamist label",
        "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=9a0aa631-133d-406b-9d32-8a1a99af4e50",
        "regulatory-label",
        "REGULATORY_LABEL",
        "12.3 Pharmacokinetics",
    ),
    "ev_injection": source(
        "DailyMed Estradiol Valerate injection label",
        "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=2e5c7559-7420-168d-6272-92bb4d20035d",
        "regulatory-label",
        "REGULATORY_LABEL",
        "Clinical Pharmacology",
    ),
    "ev_pk": source(
        "Pharmacokinetics and biotransformation of estradiol valerate",
        "https://pubmed.ncbi.nlm.nih.gov/2987096/",
        "human-pk-study",
        "HUMAN_STUDY",
    ),
    "ec_injection": source(
        "DailyMed Depo-Estradiol label",
        "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=9a4229fd-fecd-4ac1-9c4f-6d442533457f",
        "regulatory-label",
        "REGULATORY_LABEL",
        "Clinical Pharmacology",
    ),
    "ec_pk": source(
        "Steady-state pharmacokinetics of medroxyprogesterone acetate and estradiol cypionate",
        "https://pubmed.ncbi.nlm.nih.gov/10640167/",
        "human-pk-study",
        "HUMAN_STUDY",
    ),
    "ee_pk": source(
        "Pharmacokinetic studies of estradiol enanthate in menopausal women",
        "https://pubmed.ncbi.nlm.nih.gov/3814225/",
        "human-pk-study",
        "HUMAN_STUDY",
    ),
    "oral_ev": source(
        "Progynova 2 mg tablets SmPC",
        "https://www.medicines.org.uk/emc/product/1417/smpc",
        "regulatory-label",
        "REGULATORY_LABEL",
        "5.2 Pharmacokinetic properties",
    ),
    "conjugated": source(
        "DailyMed Premarin label",
        "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=258e1602-a3cf-4ccc-ca80-73dbbfb812ff",
        "regulatory-label",
        "REGULATORY_LABEL",
        "12.3 Pharmacokinetics",
    ),
    "ethinyl": source(
        "DailyMed norethindrone acetate and ethinyl estradiol label",
        "https://dailymed.nlm.nih.gov/dailymed/fda/fdaDrugXsl.cfm?setid=e6634d24-08af-44bc-8e76-e759619857fa&type=display",
        "regulatory-label",
        "REGULATORY_LABEL",
        "12.3 Pharmacokinetics",
    ),
    "femhrt": source(
        "DailyMed femhrt norethindrone acetate/ethinyl estradiol label",
        "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=434511bb-ebf9-4e25-9ef8-90e4c6786361",
        "regulatory-label",
        "REGULATORY_LABEL",
        "2 Dosage and Administration; 3 Dosage Forms and Strengths",
    ),
    "estetrol": source(
        "DailyMed Nextstellis label",
        "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=c5270073-d083-4109-ae4b-156986175e0a",
        "regulatory-label",
        "REGULATORY_LABEL",
        "12.3 Pharmacokinetics",
    ),
    "cpa_label": source(
        "Androcur 50 mg tablets SmPC",
        "https://www.medicines.org.uk/emc/product/15996/smpc",
        "regulatory-label",
        "REGULATORY_LABEL",
        "5.2 Pharmacokinetic properties",
    ),
    "cpa_safety": source(
        "EMA referral: cyproterone-containing medicinal products",
        "https://www.ema.europa.eu/en/medicines/human/referrals/cyproterone-containing-medicinal-products",
        "regulatory-safety-communication",
        "REGULATORY_SAFETY",
    ),
    "cpa_suppression": source(
        "Low-Dose Cyproterone Acetate Treatment for Transgender Women",
        "https://pubmed.ncbi.nlm.nih.gov/34176757/",
        "clinical-study",
        "HUMAN_STUDY",
    ),
    "cma": source(
        "Pharmacokinetics of chlormadinone acetate following single and multiple dosing",
        "https://pubmed.ncbi.nlm.nih.gov/16904418/",
        "human-pk-study",
        "HUMAN_STUDY",
    ),
    "cma_label": source(
        "Belara chlormadinone acetate/ethinylestradiol product information",
        "https://mohpublic.z6.web.core.windows.net/IsraelDrugs/Rishum01_7_88033023.pdf",
        "regulatory-label",
        "REGULATORY_LABEL",
        "Posology and method of administration",
    ),
    "mpa": source(
        "DailyMed medroxyprogesterone acetate tablet label",
        "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=e5f5c46c-b8b7-4502-9d2b-8f12284d1a63",
        "regulatory-label",
        "REGULATORY_LABEL",
        "12.3 Pharmacokinetics",
    ),
    "nomegestrol": source(
        "Zoely SmPC",
        "https://www.medicines.org.uk/emc/product/3038/smpc",
        "regulatory-label",
        "REGULATORY_LABEL",
        "5.2 Pharmacokinetic properties",
    ),
    "drospirenone": source(
        "DailyMed drospirenone and ethinyl estradiol label",
        "https://dailymed.nlm.nih.gov/dailymed/fda/fdaDrugXsl.cfm?setid=0f8f8a21-cee8-462f-98b3-6c06f2f33e0d",
        "regulatory-label",
        "REGULATORY_LABEL",
        "12.3 Pharmacokinetics",
    ),
    "slynd": source(
        "DailyMed Slynd drospirenone label",
        "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=db32bc55-f295-4d87-9dbb-0a2f45573dcf",
        "regulatory-label",
        "REGULATORY_LABEL",
        "2 Dosage and Administration",
    ),
    "dydrogesterone": source(
        "Nalvee dydrogesterone SmPC",
        "https://www.medicines.org.uk/emc/product/101122/smpc",
        "regulatory-label",
        "REGULATORY_LABEL",
        "5.2 Pharmacokinetic properties",
    ),
    "progesterone": source(
        "DailyMed progesterone capsule label",
        "https://dailymed.nlm.nih.gov/dailymed/lookup.cfm?setid=462ae686-7264-4852-e063-6294a90aae98",
        "regulatory-label",
        "REGULATORY_LABEL",
        "12.3 Pharmacokinetics",
    ),
    "neta": source(
        "DailyMed norethindrone acetate tablet label",
        "https://dailymed.nlm.nih.gov/dailymed/fda/fdaDrugXsl.cfm?setid=f73f9807-4af6-4faa-ba9a-656a47a4c5e0&type=display",
        "regulatory-label",
        "REGULATORY_LABEL",
        "12.3 Pharmacokinetics",
    ),
    "levonorgestrel": source(
        "DailyMed levonorgestrel tablet label",
        "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=d0bbb668-cc24-436b-9144-673a28b81f7d",
        "regulatory-label",
        "REGULATORY_LABEL",
        "12.3 Pharmacokinetics",
    ),
    "dienogest": source(
        "Sawis dienogest SmPC",
        "https://www.medicines.org.uk/emc/product/15543/smpc",
        "regulatory-label",
        "REGULATORY_LABEL",
        "5.2 Pharmacokinetic properties",
    ),
}


def time_value(
    minimum: float | None,
    maximum: float | None,
    unit: str,
    basis: str,
    note: str | None = None,
) -> dict:
    result = {"min": minimum, "max": maximum, "unit": unit, "basis": basis}
    if note:
        result["note"] = note
    return result


def unique_sources(refs: list[dict]) -> list[dict]:
    result: list[dict] = []
    seen: set[tuple[str, str]] = set()
    for ref in refs:
        key = (ref["title"], ref["url"])
        if key not in seen:
            seen.add(key)
            result.append(ref)
    return result


def time_course(
    route: str,
    formulation: str,
    refs: list[dict],
    *,
    tmax: tuple[float | None, float | None, str] | None = None,
    half_life: tuple[float | None, float | None, str] | None = None,
    steady: tuple[float | None, float | None, str] | None = None,
    duration: tuple[float | None, float | None, str] | None = None,
    peak_window: tuple[float | None, float | None, str] | None = None,
    trough_window: tuple[float | None, float | None, str] | None = None,
    depot: bool = False,
    injection_sensitive: bool = False,
    assay_sensitive: bool = False,
    notes: list[str] | None = None,
) -> dict:
    result = {
        "route": route,
        "formulation": formulation,
        "depotRelease": depot,
        "injectionIntervalSensitive": injection_sensitive,
        "assayTimingSensitive": assay_sensitive,
        "notes": notes or [],
        "sourceRefs": refs,
    }
    if tmax:
        result["tmax"] = time_value(*tmax, "plasma concentration")
    if half_life:
        result["eliminationHalfLife"] = time_value(*half_life, "literature or label pharmacokinetics")
    if steady:
        result["timeToSteadyState"] = time_value(*steady, "label pharmacokinetics")
    if duration:
        result["durationOfAction"] = time_value(*duration, "formulation delivery or clinical effect")
    if peak_window:
        result["peakWindow"] = time_value(*peak_window, "plasma concentration")
    if trough_window:
        result["troughWindow"] = time_value(*trough_window, "sampling window")
    return result


ESTROGEN_WARNINGS = [
    "Thromboembolic and cardiovascular risk depends on patient factors, route, formulation, and indication.",
    "Estrogen exposure can affect breast and endometrial tissue; product-specific contraindications and warnings apply.",
    "Hepatic disease and hormone-sensitive malignancy require product-specific clinical review.",
]
ESTROGEN_INTERACTIONS = [
    "Enzyme inducers or inhibitors can alter exposure depending on route and formulation.",
    "Estrogens can alter thyroid-binding globulin and may affect interpretation or requirements of thyroid therapy.",
    "Estrogens can reduce lamotrigine concentrations in some contexts.",
]
ESTROGEN_MONITORING = [
    "Clinical response and adverse effects",
    "Estradiol and total testosterone when clinically indicated",
    "Blood pressure",
    "Cardiometabolic risk factors when clinically indicated",
    "Prolactin only when symptoms, regimen, or local guidance indicate",
]
ESTROGEN_ASSAY_CAVEATS = [
    "Sampling time, route, formulation, and time since last dose can materially change measured estradiol.",
    "Immunoassays and LC-MS/MS methods may not agree, particularly at low concentrations or with oral estrogen metabolites.",
    "The laboratory method and local reference interval must be recorded with each result.",
]
ESTROGEN_SAFETY = [
    "Venous thromboembolism and cardiovascular risk context",
    "Blood pressure and migraine changes",
    "Breast and endometrial safety context",
    "Liver and gallbladder adverse effects",
]
PROGESTIN_MONITORING = [
    "Clinical response and adverse effects",
    "Bleeding pattern when relevant",
    "Mood symptoms",
    "Blood pressure and cardiometabolic risk factors when clinically indicated",
]
PROGESTIN_SAFETY = [
    "Mood changes and fatigue",
    "Bleeding-pattern changes",
    "Thromboembolic and cardiovascular risk depends on product and estrogen combination",
    "Evidence for benefits in feminizing HRT is limited and context-dependent",
]


def dose_reference(route: str, formulation: str, refs: list[dict], indication: str) -> list[dict]:
    return [
        {
            "indication": indication,
            "population": "Adults; product- and indication-specific",
            "route": route,
            "formulation": formulation,
            "amountText": "source needed",
            "scheduleText": "source needed",
            "sourceType": refs[0]["sourceType"] if refs else "source-needed",
            "evidenceLevel": refs[0]["evidenceLevel"] if refs else "SOURCE_NEEDED",
            "note": (
                "This field indexes label, guideline, or literature use only. "
                "It is not a recommended dose and must not be used for self-medication or dose adjustment."
            ),
            "sourceRefs": refs,
        }
    ]


def e2_model(route: str, depot: bool = False) -> dict:
    event_fields = [
        "timestamp",
        "compound",
        "route",
        "formulation",
        "amount",
        "unit",
        "lastDoseTime",
    ]
    roles = ["e2-source", "estrogen-receptor-agonist", "gonadotropin-suppressor"]
    caveats = [
        "Sampling time, formulation, and assay method strongly affect interpretation.",
        "Interindividual variability can be large.",
        "The model must not be used for dose adjustment.",
    ]
    if depot:
        roles.append("depot-release")
        event_fields.insert(2, "ester")
        caveats.insert(0, "Peak and trough variation can be large.")
    return {
        "modelCompatible": True,
        "modelRoles": roles,
        "primaryModeledAnalytes": ["Estradiol"],
        "requiredEventFields": event_fields,
        "requiredLabFields": ["estradiol", "sampleTimestamp", "assayMethod"],
        "caveats": caveats,
        "sourceRefs": [SOURCES["endocrine_guideline"], SOURCES["assay"]],
    }


def incompatible_estrogen_model() -> dict:
    return {
        "modelCompatible": False,
        "modelRoles": [
            "estrogenic-agent",
            "not-compatible-with-standard-e2-assay-model",
        ],
        "primaryModeledAnalytes": [],
        "requiredEventFields": [],
        "requiredLabFields": [],
        "caveats": [
            "This product should not be interpreted as ordinary 17beta-estradiol exposure.",
            "Standard estradiol assays may not accurately reflect estrogenic exposure.",
            "Do not use this entry with a serum estradiol prediction model.",
        ],
        "sourceRefs": [SOURCES["assay"], SOURCES["endocrine_guideline"]],
    }


def base_entry(
    name: str,
    common_names: list[str],
    url: str,
    categories: list[str],
    summary: str,
    atc: list[str],
    drug_class: list[str],
    indications: list[str],
    contraindications: list[str],
    warnings: list[str],
    interactions: list[str],
    monitoring: list[str],
    clinical_refs: list[dict],
    endocrine_info: dict,
    time_courses: list[dict],
    dose_refs: list[dict],
    model_info: dict,
    approved: bool = True,
) -> dict:
    return {
        "name": name,
        "commonNames": common_names,
        "url": url,
        "isApproved": approved,
        "crossTolerances": [],
        "toxicities": warnings,
        "categories": categories,
        "summary": summary,
        "clinicalInfo": {
            "atcCodes": atc,
            "drugClass": drug_class,
            "indications": indications,
            "contraindications": contraindications,
            "majorWarnings": warnings,
            "majorInteractions": interactions,
            "monitoring": monitoring,
            "sourceRefs": clinical_refs,
        },
        "endocrineInfo": endocrine_info,
        "timeCourse": time_courses,
        "doseUseReferences": dose_refs,
        "hrtModelInfo": model_info,
        "roas": [],
    }


def estrogen_info(refs: list[dict], roles: list[str] | None = None) -> dict:
    return {
        "hormoneClass": ["Estrogen", "Estrogen receptor agonist"],
        "mechanisms": [
            "Estrogen receptor alpha and beta agonism",
            "Feedback effects on hypothalamic-pituitary-gonadal signaling",
        ],
        "affectedHormones": ["Estradiol", "Estrone", "LH", "FSH", "Total testosterone", "SHBG"],
        "monitoringLabs": ESTROGEN_MONITORING,
        "assayCaveats": ESTROGEN_ASSAY_CAVEATS,
        "safetySignals": ESTROGEN_SAFETY,
        "modelRoles": roles or ["e2-source", "estrogen-receptor-agonist", "gonadotropin-suppressor"],
        "sourceRefs": unique_sources(refs + [SOURCES["assay"]]),
    }


def progestin_info(
    refs: list[dict],
    hormone_class: list[str],
    mechanisms: list[str],
    affected: list[str],
    roles: list[str],
    safety: list[str] | None = None,
    monitoring: list[str] | None = None,
) -> dict:
    return {
        "hormoneClass": hormone_class,
        "mechanisms": mechanisms,
        "affectedHormones": affected,
        "monitoringLabs": monitoring or PROGESTIN_MONITORING,
        "assayCaveats": [
            "Routine serum progestin concentration monitoring is generally not established for these entries.",
            "Hormone results must be interpreted with indication, formulation, timing, and co-administered estrogen.",
        ],
        "safetySignals": safety or PROGESTIN_SAFETY,
        "modelRoles": roles,
        "sourceRefs": unique_sources(refs + [SOURCES["progestogen_review"]]),
    }


def limited_progestin_model(roles: list[str], refs: list[dict]) -> dict:
    return {
        "modelCompatible": False,
        "modelRoles": roles,
        "primaryModeledAnalytes": [],
        "requiredEventFields": ["timestamp", "amount", "unit", "route"],
        "requiredLabFields": [],
        "caveats": [
            "Evidence for direct use in a testosterone or estradiol prediction model is limited or context-dependent.",
            "Do not infer a dosing recommendation from this metadata.",
        ],
        "sourceRefs": unique_sources(refs + [SOURCES["progestogen_review"]]),
    }


def dose_range(
    minimum: float | None,
    maximum: float | None,
    unit: str,
    basis: str,
    frequency: str,
    range_kind: str,
    label: str,
    note: str,
    components: list[dict] | None = None,
) -> dict:
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


def apply_regimen_overrides(entries: list[tuple[Path, dict, list[dict]]]) -> None:
    not_recommendation = (
        "This is a label- or study-reported regimen reference, not a dosing recommendation. "
        "It must not be used for self-medication or dose adjustment."
    )
    overrides = {
        "Estradiol": {
            "indication": "Menopausal vasomotor symptoms or hypoestrogenism in the cited oral label",
            "route": "oral",
            "formulation": "micronized tablet",
            "amountText": "1-2 mg estradiol daily in the cited oral label",
            "scheduleText": "Daily; the cited label describes cyclic administration for some indications",
            "ranges": [
                dose_range(1, 2, "mg", "daily-total", "daily", "label-regimen", "Oral label regimen", not_recommendation),
                dose_range(2, 4, "mg", "daily-total", "daily", "guideline-regimen", "UCSF guideline regimen", "A guideline-reported regimen is not an individualized recommendation. " + not_recommendation),
            ],
            "sourceRefs": [SOURCES["oral_e2"], SOURCES["ucsf"]],
        },
        "Oral Estradiol": {
            "indication": "Menopausal vasomotor symptoms or hypoestrogenism in the cited label",
            "amountText": "1-2 mg estradiol daily",
            "scheduleText": "Daily; cyclic administration is described for some labeled indications",
            "ranges": [dose_range(1, 2, "mg", "daily-total", "daily", "label-regimen", "Label regimen", not_recommendation)],
        },
        "Sublingual/Buccal Estradiol": {
            "indication": "Human pharmacokinetic study regimen",
            "amountText": "1 mg single sublingual estradiol dose in the cited PK study",
            "scheduleText": "Single-dose crossover pharmacokinetic study",
            "ranges": [dose_range(1, 1, "mg", "per-dose", "single study dose", "study-regimen", "PK study regimen", "Rapid peak exposure makes sampling time essential. " + not_recommendation)],
        },
        "Estradiol Transdermal Patch": {
            "indication": "Approved transdermal estradiol product strengths",
            "amountText": "0.025-0.1 mg estradiol delivered per day",
            "scheduleText": "Product-specific once- or twice-weekly patch replacement",
            "ranges": [dose_range(0.025, 0.1, "mg/day", "patch-delivery-rate", "continuous delivery; product-specific replacement interval", "label-regimen", "Patch delivery range", not_recommendation)],
        },
        "Estradiol Gel": {
            "indication": "Menopausal vasomotor symptoms in the cited label",
            "amountText": "0.25-1.25 mg estradiol applied daily",
            "scheduleText": "Once daily transdermal application",
            "ranges": [dose_range(0.25, 1.25, "mg", "daily-total", "once daily", "label-regimen", "Gel label regimen", not_recommendation)],
        },
        "Estradiol Spray": {
            "indication": "Menopausal vasomotor symptoms in the cited label",
            "amountText": "1-3 metered sprays daily; each spray contains 1.53 mg estradiol",
            "scheduleText": "Once daily application",
            "ranges": [dose_range(1, 3, "spray", "daily-total", "once daily", "label-regimen", "Metered spray regimen", "Delivered systemic exposure is not equivalent to the amount applied to skin. " + not_recommendation)],
        },
        "Estradiol Valerate Injection": {
            "indication": "Female hypoestrogenism in the cited approved label",
            "amountText": "10-20 mg intramuscularly",
            "scheduleText": "Every 4 weeks in the cited label",
            "ranges": [dose_range(10, 20, "mg", "per-dose", "every 4 weeks", "label-regimen", "Injection label regimen", "Peak/trough and assay timing are interval-sensitive. " + not_recommendation)],
        },
        "Estradiol Cypionate Injection": {
            "indication": "Menopausal symptoms in the cited approved label",
            "amountText": "1-5 mg intramuscularly",
            "scheduleText": "Every 3-4 weeks in the cited label",
            "ranges": [dose_range(1, 5, "mg", "per-dose", "every 3-4 weeks", "label-regimen", "Injection label regimen", "Peak/trough and assay timing are interval-sensitive. " + not_recommendation)],
        },
        "Estradiol Enanthate Injection": {
            "indication": "Human pharmacokinetic study regimen",
            "amountText": "10 mg intramuscular estradiol enanthate in the cited PK study",
            "scheduleText": "Single study dose",
            "ranges": [dose_range(10, 10, "mg", "per-dose", "single study dose", "study-regimen", "PK study regimen", "This is not a standalone-product label regimen; peak/trough and assay timing are interval-sensitive. " + not_recommendation)],
        },
        "Oral Estradiol Valerate": {
            "indication": "Menopausal symptoms in the cited SmPC",
            "amountText": "1-2 mg estradiol valerate daily",
            "scheduleText": "Continuous once-daily administration in the cited SmPC",
            "ranges": [dose_range(1, 2, "mg", "daily-total", "once daily", "label-regimen", "SmPC regimen", not_recommendation)],
        },
        "Conjugated Estrogens": {
            "indication": "Female hypogonadism in the cited label",
            "amountText": "0.3-0.625 mg conjugated estrogens daily",
            "scheduleText": "Cyclic administration is described in the cited label",
            "ranges": [dose_range(0.3, 0.625, "mg", "daily-total", "daily, cyclic regimen", "label-regimen", "Label regimen", "This mixture must not be interpreted as ordinary serum estradiol exposure. " + not_recommendation)],
        },
        "Ethinylestradiol": {
            "indication": "Component dose in cited menopausal combination products",
            "amountText": "2.5-5 mcg ethinylestradiol component per tablet",
            "scheduleText": "One combination tablet daily in the cited label",
            "ranges": [dose_range(None, None, "mcg", "component-dose", "once daily", "label-regimen", "Ethinylestradiol component", "Do not interpret as ordinary serum E2.", [{"substance": "ethinylestradiol", "min": 2.5, "max": 5, "unit": "mcg"}])],
            "sourceRefs": [SOURCES["femhrt"]],
        },
        "Estetrol": {
            "indication": "Component dose in the cited combined oral contraceptive label",
            "amountText": "14.2 mg estetrol component in each active tablet",
            "scheduleText": "One active tablet daily for 24 days, followed by 4 inert tablets",
            "ranges": [dose_range(None, None, "mg", "component-dose", "once daily for 24 active days per 28-day pack", "label-regimen", "Estetrol component", not_recommendation, [{"substance": "estetrol", "min": 14.2, "max": 14.2, "unit": "mg"}])],
        },
        "Cyproterone Acetate": {
            "indication": "Androgen-dependent indications in the cited Androcur SmPC",
            "amountText": "50 mg twice daily at treatment initiation in the cited high-dose SmPC",
            "scheduleText": "Twice daily after meals; indication-specific tapering is described",
            "ranges": [dose_range(100, 100, "mg", "daily-total", "50 mg twice daily", "label-regimen", "High-dose approved-label regimen", "This approved-label regimen is not a feminizing-HRT recommendation. Cumulative exposure affects meningioma risk. " + not_recommendation)],
        },
        "Chlormadinone Acetate": {
            "indication": "Component dose in a cited combined oral contraceptive product",
            "amountText": "2 mg chlormadinone acetate component per active tablet",
            "scheduleText": "One active tablet daily for 21 days followed by a 7-day break",
            "ranges": [dose_range(None, None, "mg", "component-dose", "once daily for 21 days", "label-regimen", "Chlormadinone component", not_recommendation, [{"substance": "chlormadinone acetate", "min": 2, "max": 2, "unit": "mg"}])],
            "sourceRefs": [SOURCES["cma_label"]],
        },
        "Medroxyprogesterone Acetate": {
            "indication": "Endometrial protection with daily conjugated estrogens in the cited oral label",
            "amountText": "5-10 mg medroxyprogesterone acetate daily",
            "scheduleText": "12-14 consecutive days per month in the cited label",
            "ranges": [dose_range(5, 10, "mg", "daily-total", "12-14 days per month", "label-regimen", "Oral label regimen", not_recommendation)],
        },
        "Nomegestrol Acetate": {
            "indication": "Component dose in the cited combined oral contraceptive SmPC",
            "amountText": "2.5 mg nomegestrol acetate component per active tablet",
            "scheduleText": "One active tablet daily for 24 days followed by 4 placebo tablets",
            "ranges": [dose_range(None, None, "mg", "component-dose", "once daily for 24 active days", "label-regimen", "Nomegestrol component", not_recommendation, [{"substance": "nomegestrol acetate", "min": 2.5, "max": 2.5, "unit": "mg"}])],
        },
        "Drospirenone": {
            "indication": "Progestin-only contraception in the cited label",
            "amountText": "4 mg drospirenone per active tablet",
            "scheduleText": "One active tablet daily for 24 days followed by 4 inert tablets",
            "ranges": [dose_range(4, 4, "mg", "per-dose", "once daily for 24 active days", "label-regimen", "Label regimen", not_recommendation)],
            "sourceRefs": [SOURCES["slynd"]],
        },
        "Dydrogesterone": {
            "indication": "Progestogen supplementation with estrogen therapy in the cited SmPC",
            "amountText": "10-20 mg dydrogesterone daily depending on cited regimen and clinical context",
            "scheduleText": "Last 12-14 days of an estrogen cycle in the cited HRT section",
            "ranges": [dose_range(10, 20, "mg", "daily-total", "12-14 days per cycle", "label-regimen", "SmPC regimen", not_recommendation)],
        },
        "Micronized Progesterone": {
            "indication": "Prevention of endometrial hyperplasia with estrogen in the cited label",
            "amountText": "200 mg micronized progesterone at bedtime",
            "scheduleText": "12 consecutive days per 28-day cycle",
            "ranges": [dose_range(200, 200, "mg", "per-dose", "bedtime for 12 days per 28-day cycle", "label-regimen", "Label regimen", "Sedation and dizziness are clinically relevant. " + not_recommendation)],
        },
        "Norethisterone Acetate": {
            "indication": "Endometriosis in the cited label",
            "amountText": "5-15 mg norethisterone acetate daily during label titration",
            "scheduleText": "Initial 5 mg daily with label-described stepwise increases",
            "ranges": [dose_range(5, 15, "mg", "daily-total", "daily", "label-regimen", "Label titration range", not_recommendation)],
        },
        "Levonorgestrel": {
            "indication": "Emergency contraception in the cited label",
            "amountText": "1.5 mg levonorgestrel as a single oral dose",
            "scheduleText": "As soon as possible within 72 hours in the cited label",
            "ranges": [dose_range(1.5, 1.5, "mg", "per-dose", "single dose", "label-regimen", "Label regimen", not_recommendation)],
        },
        "Dienogest": {
            "indication": "Endometriosis in the cited SmPC",
            "amountText": "2 mg dienogest daily",
            "scheduleText": "Continuous once-daily administration",
            "ranges": [dose_range(2, 2, "mg", "daily-total", "once daily", "label-regimen", "SmPC regimen", not_recommendation)],
        },
    }
    for _, substance, _ in entries:
        override = overrides[substance["name"]]
        reference = substance["doseUseReferences"][0]
        reference.update(override)
        reference["note"] = not_recommendation
        reference["sourceType"] = reference["sourceRefs"][0]["sourceType"]
        reference["evidenceLevel"] = reference["sourceRefs"][0]["evidenceLevel"]


def candidate_suppressor_model(roles: list[str], refs: list[dict]) -> dict:
    return {
        "modelCompatible": True,
        "modelRoles": roles,
        "primaryModeledAnalytes": ["Total testosterone", "LH", "FSH"],
        "requiredEventFields": ["timestamp", "amount", "unit", "route"],
        "requiredLabFields": ["totalTestosterone", "lh", "fsh", "sampleTimestamp"],
        "caveats": [
            "Evidence and effect size are product-, indication-, and regimen-dependent.",
            "Pharmacodynamic suppression is delayed relative to plasma concentration.",
            "Do not infer dosing recommendations from this compatibility marker.",
        ],
        "sourceRefs": unique_sources(refs + [SOURCES["progestogen_review"]]),
    }


def build_entries() -> list[tuple[Path, dict, list[dict]]]:
    categories = [
        {
            "name": "endocrine",
            "description": "Endocrine medicines and hormone pharmacology reference entries.",
            "color": 4283215696,
            "url": None,
        },
        {
            "name": "hrt-related",
            "description": "Medicines relevant to hormone therapy data indexing and future model research.",
            "color": 4288423856,
            "url": None,
        },
    ]
    common_categories = ["endocrine", "hrt-related", "prescription-medicine"]
    entries: list[tuple[Path, dict, list[dict]]] = []

    def add(relative: str, entry: dict, include_categories: bool = False) -> None:
        entries.append((ROOT / relative, entry, categories if include_categories else []))

    generic_refs = [SOURCES["endocrine_guideline"], SOURCES["wpath"], SOURCES["ucsf"]]
    add(
        "estrogens/estradiol.json",
        base_entry(
            "Estradiol",
            ["17beta-estradiol", "E2", "雌二醇", "17β-雌二醇"],
            SOURCES["endocrine_guideline"]["url"],
            common_categories,
            "雌二醇（E2）是内源性主要雌激素。本条目用于汇总不同制剂的共同机制、监测和模型角色；具体药代必须按给药途径查看对应条目。",
            ["G03CA03"],
            ["Estrogen", "17beta-estradiol"],
            ["Estrogen replacement contexts defined by local labeling", "Feminizing hormone therapy guideline context"],
            ["Use product-specific contraindications; contraindications vary by route and indication"],
            ESTROGEN_WARNINGS,
            ESTROGEN_INTERACTIONS,
            ESTROGEN_MONITORING,
            generic_refs,
            estrogen_info(generic_refs),
            [
                time_course(
                    "multiple routes",
                    "route-specific formulations",
                    generic_refs,
                    assay_sensitive=True,
                    notes=[
                        "No single Tmax or half-life applies across oral, sublingual, transdermal, and depot injection formulations.",
                        "Use the route-specific entries for drawable pharmacokinetic time-course data.",
                    ],
                )
            ],
            dose_reference("multiple routes", "route-specific formulations", generic_refs, "Guideline and approved-label contexts"),
            e2_model("multiple routes"),
        ),
        include_categories=True,
    )

    estrogen_specs = [
        (
            "estrogens/oral_estradiol.json",
            "Oral Estradiol",
            ["Estradiol oral tablet", "Oral E2", "口服雌二醇", "E2 oral"],
            SOURCES["oral_e2"],
            "oral",
            "micronized tablet",
            ["Oral estrogen", "17beta-estradiol"],
            time_course(
                "oral",
                "micronized tablet",
                [SOURCES["oral_e2"]],
                tmax=(5, 8, "h"),
                half_life=(12, 14, "h"),
                assay_sensitive=True,
                notes=[
                    "Values are label-derived from an oral estradiol combination product and may vary by product.",
                    "First-pass metabolism produces substantial estrone and conjugated estrogen exposure.",
                ],
            ),
            True,
        ),
        (
            "estrogens/sublingual_buccal_estradiol.json",
            "Sublingual/Buccal Estradiol",
            ["Sublingual estradiol", "Buccal estradiol", "Sublingual E2", "舌下雌二醇", "颊含雌二醇"],
            SOURCES["sublingual_e2"],
            "sublingual",
            "micronized tablet used sublingually or buccally",
            ["Sublingual estrogen", "Buccal estrogen", "17beta-estradiol"],
            time_course(
                "sublingual",
                "micronized tablet",
                [SOURCES["sublingual_e2"], SOURCES["buccal_e2"]],
                tmax=(0.5, 1, "h"),
                assay_sensitive=True,
                notes=[
                    "Rapid peaks and steep early declines make sampling time especially important.",
                    "Some of the dose may be swallowed; exposure varies with technique and formulation.",
                ],
            ),
            False,
        ),
        (
            "estrogens/estradiol_patch.json",
            "Estradiol Transdermal Patch",
            ["Estradiol patch", "Transdermal E2", "经皮雌二醇贴片", "E2 patch"],
            SOURCES["patch"],
            "transdermal",
            "continuous-delivery patch",
            ["Transdermal estrogen", "17beta-estradiol"],
            time_course(
                "transdermal",
                "continuous-delivery patch",
                [SOURCES["patch"], SOURCES["patch_removal"]],
                half_life=(6.2, 7.9, "h"),
                duration=(3, 7, "day"),
                trough_window=(0, 24, "h"),
                assay_sensitive=True,
                notes=[
                    "Wear interval is product-specific; common products are changed once or twice weekly.",
                    "After patch removal, serum estradiol can return toward baseline within about 24 hours in cited labeling.",
                ],
            ),
            True,
        ),
        (
            "estrogens/estradiol_gel.json",
            "Estradiol Gel",
            ["Estradiol transdermal gel", "E2 gel", "雌二醇凝胶", "EstroGel", "Divigel"],
            SOURCES["gel"],
            "transdermal",
            "gel",
            ["Transdermal estrogen", "17beta-estradiol"],
            time_course(
                "transdermal",
                "gel",
                [SOURCES["gel"]],
                tmax=(8, 16, "h"),
                half_life=(10, 10, "h"),
                steady=(3, 12, "day"),
                duration=(24, 24, "h"),
                assay_sensitive=True,
                notes=[
                    "Tmax and steady-state estimates vary substantially among gel products and application sites.",
                    "Skin application area, washing, transfer, and adherence affect exposure.",
                ],
            ),
            True,
        ),
        (
            "estrogens/estradiol_spray.json",
            "Estradiol Spray",
            ["Estradiol transdermal spray", "E2 spray", "雌二醇喷雾", "Evamist"],
            SOURCES["spray"],
            "transdermal",
            "metered spray",
            ["Transdermal estrogen", "17beta-estradiol"],
            time_course(
                "transdermal",
                "metered spray",
                [SOURCES["spray"]],
                steady=(7, 8, "day"),
                duration=(24, 24, "h"),
                assay_sensitive=True,
                notes=[
                    "The cited label supports steady-state timing; a product-independent terminal half-life was not entered.",
                    "Application technique, drying, washing, and transfer precautions affect exposure.",
                ],
            ),
            True,
        ),
    ]
    for path, name, aliases, ref, route, formulation, classes, tc, approved in estrogen_specs:
        refs = [ref, SOURCES["endocrine_guideline"]]
        add(
            path,
            base_entry(
                name,
                aliases,
                ref["url"],
                common_categories,
                f"{aliases[-1] if aliases else name} 的制剂专属资料条目，重点记录药代时间进程、采样时点和检测方法注意事项。",
                ["G03CA03"],
                classes,
                ["Approved estrogen indications vary by product", "Feminizing hormone therapy guideline or literature context"],
                ["Use product-specific estrogen contraindications"],
                ESTROGEN_WARNINGS,
                ESTROGEN_INTERACTIONS,
                ESTROGEN_MONITORING,
                refs,
                estrogen_info(refs),
                [tc],
                dose_reference(route, formulation, refs, "Approved-label or guideline context"),
                e2_model(route),
                approved=approved,
            ),
        )

    depot_specs = [
        (
            "estrogens/estradiol_valerate_injection.json",
            "Estradiol Valerate Injection",
            ["Estradiol valerate", "EV", "戊酸雌二醇注射剂", "Delestrogen"],
            [SOURCES["ev_injection"], SOURCES["ev_pk"]],
            "G03CA03",
            (1, 3, "day"),
            (4, 5, "day"),
        ),
        (
            "estrogens/estradiol_cypionate_injection.json",
            "Estradiol Cypionate Injection",
            ["Estradiol cypionate", "EC", "环戊丙酸雌二醇注射剂", "Depo-Estradiol"],
            [SOURCES["ec_injection"], SOURCES["ec_pk"]],
            "G03CA03",
            (3, 5, "day"),
            (7, 10, "day"),
        ),
        (
            "estrogens/estradiol_enanthate_injection.json",
            "Estradiol Enanthate Injection",
            ["Estradiol enanthate", "EEn", "庚酸雌二醇注射剂", "estradiol enantate"],
            [SOURCES["ee_pk"]],
            "G03CA03",
            (3, 8, "day"),
            (5.6, 7.5, "day"),
        ),
    ]
    for path, name, aliases, refs, atc, peak, half_life in depot_specs:
        add(
            path,
            base_entry(
                name,
                aliases,
                refs[0]["url"],
                common_categories,
                f"{aliases[2]}属于长效雌二醇酯储库制剂。峰值、谷值和化验结果高度依赖注射间隔、剂量、部位、个体差异与采样时间。",
                [atc],
                ["Depot estradiol ester", "Long-acting estrogen"],
                ["Approved indications vary by product and country", "Feminizing hormone therapy literature context"],
                ["Use product-specific estrogen and injectable-product contraindications"],
                ESTROGEN_WARNINGS,
                ESTROGEN_INTERACTIONS,
                ESTROGEN_MONITORING,
                refs,
                estrogen_info(
                    refs,
                    [
                        "e2-source",
                        "estrogen-receptor-agonist",
                        "gonadotropin-suppressor",
                        "depot-release",
                        "injection-interval-sensitive",
                        "peak-trough-sensitive",
                        "sample-timing-sensitive",
                    ],
                ),
                [
                    time_course(
                        "intramuscular",
                        "oil depot injection",
                        refs,
                        tmax=peak,
                        half_life=half_life,
                        peak_window=peak,
                        depot=True,
                        injection_sensitive=True,
                        assay_sensitive=True,
                        notes=[
                            "Published estimates vary by study, assay, dose, ester, injection site, and population.",
                            "Peak and trough variation can be large; sample timing must be recorded.",
                            "These literature estimates must not be used to select or adjust a dose.",
                        ],
                    )
                ],
                dose_reference("intramuscular", "oil depot injection", refs, "Approved-label or pharmacokinetic literature context"),
                e2_model("intramuscular", depot=True),
            ),
        )

    oral_ev_refs = [SOURCES["oral_ev"], SOURCES["endocrine_guideline"]]
    add(
        "estrogens/oral_estradiol_valerate.json",
        base_entry(
            "Oral Estradiol Valerate",
            ["Estradiol valerate tablet", "Oral EV", "口服戊酸雌二醇", "Progynova"],
            SOURCES["oral_ev"]["url"],
            common_categories,
            "口服戊酸雌二醇是雌二醇前药制剂；口服首过代谢、雌酮暴露和检测时点会影响化验解释。",
            ["G03CA03"],
            ["Oral estrogen", "Estradiol ester prodrug"],
            ["Approved estrogen indications vary by country and product", "Feminizing hormone therapy literature context"],
            ["Use product-specific estrogen contraindications"],
            ESTROGEN_WARNINGS,
            ESTROGEN_INTERACTIONS,
            ESTROGEN_MONITORING,
            oral_ev_refs,
            estrogen_info(oral_ev_refs),
            [
                time_course(
                    "oral",
                    "film-coated tablet",
                    [SOURCES["oral_ev"]],
                    half_life=(27, 27, "h"),
                    assay_sensitive=True,
                    notes=[
                        "The cited 27-hour value is an excretion half-life reported in the SmPC and should not be treated as a universal plasma estradiol terminal half-life.",
                        "A reliable product-independent Tmax was not entered.",
                    ],
                )
            ],
            dose_reference("oral", "film-coated tablet", oral_ev_refs, "Approved-label or guideline context"),
            e2_model("oral"),
        ),
    )

    non_e2_estrogens = [
        (
            "estrogens/conjugated_estrogens.json",
            "Conjugated Estrogens",
            ["Conjugated equine estrogens", "CEE", "结合雌激素", "Premarin"],
            SOURCES["conjugated"],
            ["G03CA57"],
            ["Conjugated estrogen mixture", "Estrogenic agent"],
            time_course(
                "oral",
                "tablet",
                [SOURCES["conjugated"]],
                half_life=(11, 17, "h"),
                assay_sensitive=True,
                notes=[
                    "The product is a mixture; component-specific half-lives differ.",
                    "Ordinary serum estradiol is not a complete measure of estrogenic exposure.",
                ],
            ),
        ),
        (
            "estrogens/ethinylestradiol.json",
            "Ethinylestradiol",
            ["Ethinyl estradiol", "EE", "乙炔雌二醇", "Ethinylestradiol"],
            SOURCES["ethinyl"],
            ["G03CA01"],
            ["Synthetic estrogen", "Ethinylated estrogen"],
            time_course(
                "oral",
                "tablet",
                [SOURCES["ethinyl"]],
                tmax=(1, 2, "h"),
                half_life=(17, 24, "h"),
                assay_sensitive=True,
                notes=[
                    "Values vary among combination products.",
                    "Serum estradiol assays do not quantify ethinylestradiol exposure as ordinary E2.",
                ],
            ),
        ),
        (
            "estrogens/estetrol.json",
            "Estetrol",
            ["E4", "雌四醇", "Estetrol monohydrate", "Nextstellis"],
            SOURCES["estetrol"],
            ["G03CA06"],
            ["Estrogen", "Estetrol", "E4"],
            time_course(
                "oral",
                "tablet",
                [SOURCES["estetrol"]],
                tmax=(0.5, 2, "h"),
                half_life=(27, 27, "h"),
                steady=(4, 4, "day"),
                assay_sensitive=True,
                notes=[
                    "Values are from a combination-product label.",
                    "Estetrol is not 17beta-estradiol and is not compatible with an ordinary serum E2 exposure model.",
                ],
            ),
        ),
    ]
    for path, name, aliases, ref, atc, classes, tc in non_e2_estrogens:
        refs = [ref, SOURCES["assay"]]
        add(
            path,
            base_entry(
                name,
                aliases,
                ref["url"],
                common_categories,
                f"{aliases[2]}是雌激素相关制剂，但不能当作普通 17β-雌二醇血清暴露解释。",
                atc,
                classes,
                ["Approved indications depend on product"],
                ["Use product-specific estrogen contraindications"],
                ESTROGEN_WARNINGS,
                ESTROGEN_INTERACTIONS,
                ESTROGEN_MONITORING,
                refs,
                estrogen_info(
                    refs,
                    ["estrogenic-agent", "not-compatible-with-standard-e2-assay-model"],
                ),
                [tc],
                dose_reference("oral", "tablet", refs, "Approved-label context"),
                incompatible_estrogen_model(),
            ),
        )

    cpa_refs = [SOURCES["cpa_label"], SOURCES["cpa_safety"], SOURCES["cpa_suppression"]]
    cpa_roles = ["testosterone-suppressor", "lh-fsh-suppressor", "prolactin-risk"]
    add(
        "antiandrogenic_progestins/cyproterone_acetate.json",
        base_entry(
            "Cyproterone Acetate",
            ["Cyproterone acetate", "CPA", "醋酸环丙孕酮", "Androcur", "Cyprostat"],
            SOURCES["cpa_label"]["url"],
            common_categories,
            "醋酸环丙孕酮（CPA）兼具甾体抗雄激素、孕激素和抗促性腺激素作用。累积暴露相关脑膜瘤风险、肝毒性和泌乳素变化是本条目的核心安全信息。",
            ["G03HA01"],
            ["Steroidal antiandrogen", "Progestin", "Antigonadotropin"],
            ["Androgen-dependent indications defined by local approved labeling", "Feminizing hormone therapy literature context"],
            ["Current or previous meningioma", "Severe liver disease", "Use other product-specific contraindications"],
            [
                "Meningioma risk increases with higher dose and cumulative exposure.",
                "Hepatotoxicity, including severe hepatic injury, has been reported.",
                "Hyperprolactinemia can occur and may be dose-related.",
                "Mood changes, fatigue, and sexual-function changes can occur.",
                "Thromboembolic risk context requires attention when combined with estrogen therapy.",
            ],
            ["Other hepatotoxic drugs can increase liver-safety concern", "Combined estrogen therapy changes the overall thromboembolic risk context"],
            ["Total testosterone", "Estradiol", "LH", "FSH", "Prolactin", "AST", "ALT", "Bilirubin", "Metabolic markers when clinically indicated"],
            cpa_refs,
            progestin_info(
                cpa_refs,
                ["Steroidal antiandrogen", "Progestin", "Antigonadotropin"],
                [
                    "Androgen receptor antagonism",
                    "Progestogenic suppression of gonadotropins",
                    "Reduction of gonadal testosterone production",
                ],
                ["Total testosterone", "LH", "FSH", "Prolactin", "Estradiol indirectly when combined with estrogen therapy"],
                cpa_roles,
                safety=[
                    "Meningioma risk related to higher dose and cumulative exposure",
                    "Hepatotoxicity",
                    "Hyperprolactinemia",
                    "Mood changes",
                    "Fatigue",
                    "Sexual function changes",
                    "Thromboembolic risk context when combined with estrogen therapy",
                ],
                monitoring=["Total testosterone", "Estradiol", "LH", "FSH", "Prolactin", "AST", "ALT", "Bilirubin", "Metabolic markers when clinically indicated"],
            ),
            [
                time_course(
                    "oral",
                    "tablet",
                    cpa_refs,
                    tmax=(1.5, 2, "h"),
                    half_life=(1.7, 2.3, "day"),
                    steady=(8, 16, "day"),
                    assay_sensitive=True,
                    notes=[
                        "Testosterone suppression is delayed relative to plasma concentration.",
                        "Terminal half-life and accumulation vary with dose and repeated exposure.",
                    ],
                )
            ],
            dose_reference("oral", "tablet", cpa_refs, "Approved-label and feminizing hormone therapy literature contexts"),
            {
                "modelCompatible": True,
                "modelRoles": cpa_roles,
                "primaryModeledAnalytes": ["Total testosterone", "LH", "FSH", "Prolactin"],
                "requiredEventFields": ["timestamp", "amount", "unit", "route"],
                "requiredLabFields": ["totalTestosterone", "lh", "fsh", "prolactin", "sampleTimestamp"],
                "caveats": [
                    "Testosterone suppression is delayed relative to plasma concentration.",
                    "Do not infer dosing recommendations from the model.",
                    "Cumulative exposure matters for safety risk.",
                    "Liver function and prolactin monitoring may be clinically relevant.",
                ],
                "sourceRefs": cpa_refs,
            },
        ),
    )

    suppressor_specs = [
        (
            "antiandrogenic_progestins/chlormadinone_acetate.json",
            "Chlormadinone Acetate",
            ["CMA", "醋酸氯地孕酮", "Chlormadinone"],
            SOURCES["cma"],
            "G03DB06",
            (1, 2, "h"),
            (25, 39, "h"),
            (7, 15, "day"),
            ["Progestin", "Steroidal antiandrogen", "Antigonadotropin"],
        ),
        (
            "antiandrogenic_progestins/medroxyprogesterone_acetate.json",
            "Medroxyprogesterone Acetate",
            ["MPA", "醋酸甲羟孕酮", "Medroxyprogesterone", "Provera"],
            SOURCES["mpa"],
            "G03DA02",
            None,
            None,
            None,
            ["Progestin", "Antigonadotropin"],
        ),
        (
            "antiandrogenic_progestins/nomegestrol_acetate.json",
            "Nomegestrol Acetate",
            ["NOMAC", "醋酸诺美孕酮", "Nomegestrol", "Zoely"],
            SOURCES["nomegestrol"],
            "G03DB04",
            (1.5, 2, "h"),
            (28, 83, "h"),
            (5, 5, "day"),
            ["Progestin", "Antigonadotropin"],
        ),
    ]
    candidate_roles = ["progestin", "possible-gonadotropin-suppressor", "testosterone-suppression-candidate"]
    for path, name, aliases, ref, atc, tmax, half_life, steady, classes in suppressor_specs:
        refs = [ref, SOURCES["progestogen_review"]]
        tc = time_course(
            "oral",
            "tablet",
            refs,
            tmax=tmax,
            half_life=half_life,
            steady=steady,
            assay_sensitive=True,
            notes=[
                "Evidence for use in a testosterone-suppression model is limited and context-dependent.",
                "Pharmacodynamic gonadotropin effects do not equal plasma Tmax.",
                *(
                    ["Reliable product-specific numeric PK values remain source needed for this entry."]
                    if tmax is None
                    else []
                ),
            ],
        )
        add(
            path,
            base_entry(
                name,
                [name] + aliases,
                ref["url"],
                common_categories,
                f"{aliases[1]}是合成孕激素；其抗促性腺激素或抗雄相关作用须按具体制剂、适应证和证据谨慎解释。",
                [atc],
                classes,
                ["Approved gynecologic or contraceptive indications vary by product", "Feminizing hormone therapy evidence is limited"],
                ["Use product-specific contraindications"],
                PROGESTIN_SAFETY,
                ["Enzyme-inducing drugs may reduce exposure", "Combined estrogen products have additional interaction and thrombotic-risk considerations"],
                PROGESTIN_MONITORING,
                refs,
                progestin_info(
                    refs,
                    classes,
                    ["Progesterone receptor agonism", "Possible suppression of gonadotropin secretion"],
                    ["LH", "FSH", "Total testosterone indirectly"],
                    candidate_roles,
                ),
                [tc],
                dose_reference("oral", "tablet", refs, "Approved-label or literature context"),
                candidate_suppressor_model(candidate_roles, refs),
            ),
        )

    limited_specs = [
        (
            "progestogens/drospirenone.json",
            "Drospirenone",
            ["DRSP", "屈螺酮", "Slynd", "Yasmin"],
            SOURCES["drospirenone"],
            "G03AC10",
            (1, 2, "h"),
            (30, 30, "h"),
            (8, 10, "day"),
            ["Progestin", "Antimineralocorticoid progestin", "Antiandrogenic progestin"],
            ["Potassium and renal function when clinically indicated"],
            ["Hyperkalemia risk in susceptible patients or with potassium-raising drugs"],
        ),
        (
            "progestogens/dydrogesterone.json",
            "Dydrogesterone",
            ["DHD prodrug", "地屈孕酮", "Duphaston", "Nalvee"],
            SOURCES["dydrogesterone"],
            "G03DB01",
            (0.5, 2.5, "h"),
            (5, 17, "h"),
            (3, 3, "day"),
            ["Progestogen", "Pregnadien derivative"],
            PROGESTIN_MONITORING,
            PROGESTIN_SAFETY,
        ),
        (
            "progestogens/micronized_progesterone.json",
            "Micronized Progesterone",
            ["Progesterone", "Natural progesterone", "P4", "微粒化黄体酮", "天然黄体酮", "Prometrium"],
            SOURCES["progesterone"],
            "G03DA04",
            (1.5, 3, "h"),
            None,
            None,
            ["Progesterone", "Micronized natural progestogen", "Neuroactive steroid precursor"],
            PROGESTIN_MONITORING,
            ["Sedation and dizziness", "Mood changes", "Product excipients and allergy context", "Evidence in feminizing HRT is limited"],
        ),
        (
            "progestogens/norethisterone_acetate.json",
            "Norethisterone Acetate",
            ["Norethindrone acetate", "NETA", "醋酸炔诺酮", "Aygestin"],
            SOURCES["neta"],
            "G03DC02",
            (1.25, 2.4, "h"),
            (6.3, 10.7, "h"),
            None,
            ["Progestin", "19-nortestosterone derivative"],
            PROGESTIN_MONITORING,
            PROGESTIN_SAFETY,
        ),
        (
            "progestogens/levonorgestrel.json",
            "Levonorgestrel",
            ["LNG", "左炔诺孕酮", "Plan B", "Levonorgestrel oral"],
            SOURCES["levonorgestrel"],
            "G03AC03",
            (1, 4, "h"),
            (21.9, 33.1, "h"),
            None,
            ["Progestin", "19-nortestosterone derivative"],
            PROGESTIN_MONITORING,
            PROGESTIN_SAFETY,
        ),
        (
            "progestogens/dienogest.json",
            "Dienogest",
            ["DNG", "地诺孕素", "Visanne", "Sawis"],
            SOURCES["dienogest"],
            "G03DB08",
            None,
            (9, 11, "h"),
            None,
            ["Progestin", "Antiandrogenic progestin"],
            PROGESTIN_MONITORING,
            PROGESTIN_SAFETY,
        ),
    ]
    limited_roles = ["progestin", "limited-hrt-model-evidence"]
    for path, name, aliases, ref, atc, tmax, half_life, steady, classes, monitoring, safety in limited_specs:
        refs = [ref, SOURCES["progestogen_review"]]
        add(
            path,
            base_entry(
                name,
                [name] + aliases,
                ref["url"],
                common_categories,
                f"{aliases[1]}以药品资料记录为主；目前不默认纳入睾酮或雌二醇预测模型。",
                [atc],
                classes,
                ["Approved indications vary by product", "Evidence for routine use in feminizing HRT is limited or context-dependent"],
                ["Use product-specific contraindications"],
                safety,
                ["Enzyme-inducing drugs may reduce exposure", "Product-specific interactions apply"],
                monitoring,
                refs,
                progestin_info(
                    refs,
                    classes,
                    ["Progesterone receptor agonism", "Product-specific secondary receptor activity"],
                    ["Progesterone-related signaling", "LH and FSH may change depending on regimen"],
                    limited_roles,
                    safety=safety,
                    monitoring=monitoring,
                ),
                [
                    time_course(
                        "oral",
                        "tablet or capsule",
                        refs,
                        tmax=tmax,
                        half_life=half_life,
                        steady=steady,
                        notes=[
                            "PK values are formulation- and study-specific.",
                            *(
                                ["A reliable numeric value remains source needed for omitted fields."]
                                if tmax is None or half_life is None
                                else []
                            ),
                        ],
                    )
                ],
                dose_reference("oral", "tablet or capsule", refs, "Approved-label context"),
                limited_progestin_model(limited_roles, refs),
            ),
        )

    apply_regimen_overrides(entries)
    return entries


def main() -> None:
    entries = build_entries()
    for path, substance, categories in entries:
        path.parent.mkdir(parents=True, exist_ok=True)
        payload = {"substances": [substance]}
        if categories:
            payload["categories"] = categories
        path.write_text(
            json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
            newline="\n",
        )
        print(f"Wrote {path}")
    print(f"Generated endocrine entries: {len(entries)}")


if __name__ == "__main__":
    main()
