#!/usr/bin/env python3
import json
from pathlib import Path


DATE = "2026-06-23"
SOURCE_DIR = Path("tools/drugdata/cardiovascular")
BATCH_FILE = SOURCE_DIR / "cardiovascular_batch_2026_06_23.json"

DISCLAIMER = (
    "临床参考剂量兼容旧剂量模型显示：light/common/strong/heavy 仅表示低参考、常见参考、较高或上限参考层级，"
    "不表示娱乐性强度，也不等同个体化处方剂量或确定中毒剂量。毒理风险与适应证、年龄、体重、肝肾功能、合并用药、"
    "给药途径和监测结果有关。本资料仅用于学习和资料索引，不构成医疗建议，不用于诊断、处方、自行用药或调整剂量；"
    "实际用药必须遵循医生医嘱和当地批准说明书。"
)


def source_ref(title: str, query: str, source_type: str = "regulatory-label") -> dict:
    return {
        "title": title,
        "url": "https://dailymed.nlm.nih.gov/dailymed/search.cfm?query=" + query.replace(" ", "%20"),
        "sourceType": source_type,
        "accessedDate": DATE,
    }


def atc_ref(code: str) -> dict:
    return {
        "title": f"WHO ATC/DDD Index: {code}",
        "url": f"https://atcddd.fhi.no/atc_ddd_index/?code={code}",
        "sourceType": "terminology",
        "accessedDate": DATE,
    }


def time_value(
    min_value: float | None = None,
    max_value: float | None = None,
    unit: str = "h",
    basis: str = "label pharmacokinetics",
    note: str | None = None,
) -> dict:
    result = {"min": min_value, "max": max_value, "unit": unit, "basis": basis}
    if note:
        result["note"] = note
    return result


def dose_roas(entries: list[tuple[str, str, float | None, float | None, float | None, float | None]]) -> list[dict]:
    result = []
    for route, units, light, common, strong, heavy in entries:
        dose = {"units": units}
        if light is not None:
            dose["lightMin"] = light
        if common is not None:
            dose["commonMin"] = common
        if strong is not None:
            dose["strongMin"] = strong
        if heavy is not None:
            dose["heavyMin"] = heavy
        result.append({"name": route, "dose": dose})
    return result


def tdm(reason: str = "Clinical monitoring is not usually plasma-concentration guided.") -> dict:
    return {
        "isRoutinelyMonitored": False,
        "monitoringType": "not routinely monitored by plasma concentration",
        "reason": reason,
        "pharmacokineticParametersAvailable": True,
    }


def build_entry(
    name: str,
    chinese_name: str,
    aliases: list[str],
    source_query: str,
    atc_code: str,
    drug_class: list[str],
    indications: list[str],
    warnings: list[str],
    interactions: list[str],
    monitoring: list[str],
    time_courses: list[dict],
    dose_entries: list[tuple[str, str, float | None, float | None, float | None, float | None]],
    categories: list[str] | None = None,
    summary: str | None = None,
    toxicities: list[str] | None = None,
) -> dict:
    categories = categories or ["cardiovascular", "prescription-medicine"]
    refs = [source_ref(f"DailyMed {name} label search", source_query), atc_ref(atc_code)]
    return {
        "name": name,
        "commonNames": [name] + aliases + [chinese_name] + drug_class[:2],
        "url": refs[0]["url"],
        "isApproved": True,
        "crossTolerances": [],
        "toxicities": toxicities
        or [
            "Clinically important toxicity depends on indication, dose context, organ function, interacting drugs, and patient-specific risk factors."
        ],
        "categories": categories,
        "summary": summary
        or f"{chinese_name}是心血管相关处方药。本条目用于药代/药效时间进程、监测和相互作用学习索引，不构成医疗建议。",
        "dosageRemark": DISCLAIMER,
        "clinicalInfo": {
            "atcCodes": [atc_code],
            "drugClass": drug_class,
            "indications": indications,
            "contraindications": [],
            "majorWarnings": warnings,
            "majorInteractions": interactions,
            "monitoring": monitoring,
            "sourceRefs": refs,
        },
        "timeCourse": [dict(time_course, sourceRefs=[refs[0]]) for time_course in time_courses],
        "tdm": tdm(),
        "roas": dose_roas(dose_entries),
    }


def existing_time_course_updates() -> dict[str, list[dict]]:
    return {
        "Lidocaine": [
            {
                "route": "intravenous",
                "formulation": "injection/infusion",
                "onset": time_value(45, 90, "s", "clinical effect"),
                "durationOfAction": time_value(10, 20, "min", "clinical effect"),
                "eliminationHalfLife": time_value(1.5, 2),
                "notes": [
                    "Antiarrhythmic effect after IV use is rapid; plasma concentration and toxicity interpretation depend on infusion timing and hepatic blood flow.",
                    "Source values should be interpreted with ECG, neurologic symptoms, hepatic function, acid-base status, and interacting drugs.",
                ],
                "sourceRefs": [
                    {
                        "title": "DailyMed lidocaine hydrochloride injection label",
                        "url": "https://dailymed.nlm.nih.gov/dailymed/lookup.cfm?setid=46480253-29b9-4d7f-98ed-10377929026b",
                        "sourceType": "regulatory-label",
                        "accessedDate": DATE,
                    }
                ],
            }
        ],
        "Procainamide": [
            {
                "route": "intravenous",
                "formulation": "injection/infusion",
                "eliminationHalfLife": time_value(2.5, 5),
                "notes": [
                    "Procainamide and active metabolite NAPA interpretation depends on renal function, acetylator status, ECG/QRS/QT, dose timing, and infusion context."
                ],
                "sourceRefs": [source_ref("DailyMed procainamide hydrochloride injection label", "procainamide hydrochloride injection")],
            },
            {
                "route": "oral",
                "formulation": "capsule/tablet",
                "tmax": time_value(1, 2, basis="plasma concentration"),
                "eliminationHalfLife": time_value(2.5, 5),
                "notes": [
                    "Oral absorption and active metabolite accumulation vary; use ECG and clinical context rather than concentration alone."
                ],
                "sourceRefs": [source_ref("DailyMed procainamide label search", "procainamide")],
            },
        ],
        "Flecainide": [
            {
                "route": "oral",
                "formulation": "tablet",
                "tmax": time_value(1, 6, basis="plasma concentration"),
                "eliminationHalfLife": time_value(12, 27),
                "timeToSteadyState": time_value(3, 5, "day"),
                "notes": [
                    "QRS widening, PR prolongation, renal/hepatic function, CYP2D6 context, dose timing, and symptoms are central to interpretation."
                ],
                "sourceRefs": [source_ref("DailyMed flecainide acetate tablet label", "flecainide acetate tablet")],
            }
        ],
        "Quinidine": [
            {
                "route": "oral",
                "formulation": "sulfate tablet/capsule",
                "tmax": time_value(1.5, 3, basis="plasma concentration"),
                "eliminationHalfLife": time_value(6, 8),
                "notes": [
                    "Quinidine time course varies by salt and formulation. Interpret concentrations with ECG/QT, renal/hepatic function, timing, and symptoms."
                ],
                "sourceRefs": [source_ref("DailyMed quinidine sulfate label search", "quinidine sulfate")],
            }
        ],
        "Disopyramide": [
            {
                "route": "oral",
                "formulation": "capsule",
                "tmax": time_value(2, 3, basis="plasma concentration"),
                "eliminationHalfLife": time_value(6, 8),
                "notes": [
                    "Anticholinergic effects, QRS/QT changes, renal function, heart failure symptoms, and timing should guide interpretation."
                ],
                "sourceRefs": [source_ref("DailyMed disopyramide phosphate capsule label", "disopyramide phosphate capsule")],
            }
        ],
        "Mexiletine": [
            {
                "route": "oral",
                "formulation": "capsule",
                "tmax": time_value(2, 3, basis="plasma concentration"),
                "eliminationHalfLife": time_value(10, 12),
                "notes": [
                    "Mexiletine is structurally similar to lidocaine but orally active; interpret with ECG, neurologic symptoms, hepatic function, timing, and interacting drugs."
                ],
                "sourceRefs": [source_ref("DailyMed mexiletine hydrochloride capsule label", "mexiletine hydrochloride capsule")],
            }
        ],
        "Ibutilide": [
            {
                "route": "intravenous",
                "formulation": "injection",
                "durationOfAction": time_value(3, 6, basis="pharmacodynamic effect"),
                "eliminationHalfLife": time_value(2, 12),
                "notes": [
                    "QT prolongation and torsades risk are pharmacodynamic safety issues requiring ECG monitoring after infusion; effect timing is not simply plasma Tmax."
                ],
                "sourceRefs": [source_ref("DailyMed ibutilide fumarate injection label", "ibutilide fumarate injection")],
            }
        ],
        "Warfarin": [
            {
                "route": "oral",
                "formulation": "tablet",
                "tmax": time_value(4, 4, basis="plasma concentration"),
                "onset": time_value(24, 24, basis="pharmacodynamic effect"),
                "peakEffect": time_value(72, 96, basis="pharmacodynamic effect"),
                "durationOfAction": time_value(2, 5, "day", "pharmacodynamic effect"),
                "eliminationHalfLife": time_value(20, 60),
                "notes": [
                    "Warfarin clinical monitoring is INR, not routine plasma concentration. Anticoagulant effect reflects vitamin K-dependent clotting factor turnover."
                ],
                "sourceRefs": [source_ref("DailyMed warfarin sodium tablet label", "warfarin sodium tablet")],
            }
        ],
        "Digoxin": [
            {
                "route": "oral",
                "formulation": "tablet",
                "tmax": time_value(1, 3, basis="plasma concentration"),
                "peakEffect": time_value(2, 6, basis="pharmacodynamic effect"),
                "eliminationHalfLife": time_value(36, 48),
                "timeToSteadyState": time_value(5, 7, "day"),
                "notes": [
                    "Serum digoxin concentrations should generally be interpreted at least 6 hours after the last dose and with potassium, renal function, ECG, symptoms, and interacting drugs."
                ],
                "sourceRefs": [source_ref("DailyMed digoxin tablet label", "digoxin tablet")],
            },
            {
                "route": "intravenous",
                "formulation": "injection",
                "onset": time_value(5, 30, "min", "clinical effect"),
                "peakEffect": time_value(1, 4, basis="pharmacodynamic effect"),
                "eliminationHalfLife": time_value(36, 48),
                "timeToSteadyState": time_value(5, 7, "day"),
                "notes": [
                    "Intravenous effect timing and serum concentration interpretation still require post-distribution timing and clinical context."
                ],
                "sourceRefs": [source_ref("DailyMed digoxin injection label", "digoxin injection")],
            },
        ],
        "Diltiazem": [
            {
                "route": "oral",
                "formulation": "extended-release capsule/tablet",
                "tmax": time_value(10, 14, basis="plasma concentration"),
                "eliminationHalfLife": time_value(5, 8),
                "durationOfAction": time_value(24, 24, basis="label formulation profile"),
                "notes": [
                    "Diltiazem absorption and Tmax are formulation-specific; immediate-release and extended-release records should not be collapsed."
                ],
                "sourceRefs": [
                    {
                        "title": "DailyMed diltiazem hydrochloride extended-release label",
                        "url": "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=465dbab3-f201-4441-8f55-92a843b5134f",
                        "sourceType": "regulatory-label",
                        "accessedDate": DATE,
                    }
                ],
            }
        ],
    }


def batch_substances() -> list[dict]:
    return [
        build_entry(
            "Captopril",
            "卡托普利",
            ["Capoten"],
            "captopril tablet",
            "C09AA01",
            ["ACE抑制剂", "血管紧张素转换酶抑制剂", "ACE inhibitor", "Antihypertensive"],
            ["Hypertension", "Heart failure contexts depending on label"],
            [
                "Can cause hypotension, renal impairment, hyperkalemia, cough, and angioedema.",
                "Fetal toxicity warnings apply to renin-angiotensin system medicines.",
            ],
            [
                "Potassium supplements or potassium-sparing diuretics can increase hyperkalemia risk.",
                "NSAIDs can reduce antihypertensive effect and increase renal risk in susceptible patients.",
            ],
            ["Blood pressure", "Serum creatinine/eGFR", "Serum potassium", "Angioedema symptoms"],
            [
                {
                    "route": "oral",
                    "formulation": "tablet",
                    "tmax": time_value(1, 1),
                    "eliminationHalfLife": time_value(2, 3),
                    "durationOfAction": time_value(6, 12, basis="clinical effect"),
                    "notes": ["Food can reduce absorption; clinical blood pressure response is not identical to plasma Tmax."],
                }
            ],
            [("ORAL", "mg", 6.25, 12.5, 25, 150)],
        ),
        build_entry(
            "Ramipril",
            "雷米普利",
            ["Altace"],
            "ramipril capsule",
            "C09AA05",
            ["ACE抑制剂", "血管紧张素转换酶抑制剂", "ACE inhibitor", "Antihypertensive"],
            ["Hypertension", "Cardiovascular risk-reduction and heart failure contexts depending on label"],
            ["Can cause hypotension, renal impairment, hyperkalemia, cough, and angioedema.", "Fetal toxicity warnings apply."],
            ["Potassium-increasing drugs can increase hyperkalemia risk.", "NSAIDs may reduce antihypertensive effect and increase renal risk."],
            ["Blood pressure", "Serum creatinine/eGFR", "Serum potassium", "Angioedema symptoms"],
            [
                {
                    "route": "oral",
                    "formulation": "capsule/tablet",
                    "tmax": time_value(1, 1, note="Peak ramipril concentrations are rapid; active ramiprilat peaks later."),
                    "peakEffect": time_value(2, 4, basis="plasma concentration", note="Approximate peak active ramiprilat concentration window."),
                    "eliminationHalfLife": time_value(13, 17, note="Effective half-life for accumulation of ramiprilat after repeated dosing."),
                    "notes": ["Ramipril is a prodrug; active ramiprilat kinetics are more relevant to sustained ACE inhibition."],
                }
            ],
            [("ORAL", "mg", 1.25, 2.5, 5, 10)],
        ),
        build_entry(
            "Olmesartan",
            "奥美沙坦",
            ["Benicar", "Olmesartan medoxomil"],
            "olmesartan medoxomil tablet",
            "C09CA08",
            ["ARB", "血管紧张素II受体阻滞剂", "Angiotensin II receptor blocker", "Antihypertensive"],
            ["Hypertension"],
            ["Can cause fetal toxicity, hypotension, renal impairment, and hyperkalemia.", "Sprue-like enteropathy has been reported with olmesartan."],
            ["Potassium-increasing drugs can increase hyperkalemia risk.", "NSAIDs can reduce antihypertensive effect and increase renal risk."],
            ["Blood pressure", "Serum creatinine/eGFR", "Serum potassium", "Severe chronic diarrhea/weight loss symptoms"],
            [{"route": "oral", "formulation": "tablet", "tmax": time_value(1, 2), "eliminationHalfLife": time_value(13, 13), "notes": ["Tmax is a plasma concentration parameter and does not define the full antihypertensive response."]}],
            [("ORAL", "mg", 5, 20, 40, None)],
        ),
        build_entry(
            "Irbesartan",
            "厄贝沙坦",
            ["Avapro"],
            "irbesartan tablet",
            "C09CA04",
            ["ARB", "血管紧张素II受体阻滞剂", "Angiotensin II receptor blocker", "Antihypertensive"],
            ["Hypertension", "Diabetic nephropathy context depending on label"],
            ["Can cause fetal toxicity, hypotension, renal impairment, and hyperkalemia."],
            ["Potassium-increasing drugs can increase hyperkalemia risk.", "NSAIDs can reduce antihypertensive effect and increase renal risk."],
            ["Blood pressure", "Serum creatinine/eGFR", "Serum potassium"],
            [{"route": "oral", "formulation": "tablet", "tmax": time_value(1.5, 2), "eliminationHalfLife": time_value(11, 15), "notes": ["Exposure and response vary; clinical monitoring is blood-pressure and laboratory based."]}],
            [("ORAL", "mg", 75, 150, 300, None)],
        ),
        build_entry(
            "Telmisartan",
            "替米沙坦",
            ["Micardis"],
            "telmisartan tablet",
            "C09CA07",
            ["ARB", "血管紧张素II受体阻滞剂", "Angiotensin II receptor blocker", "Antihypertensive"],
            ["Hypertension", "Cardiovascular risk-reduction contexts depending on label"],
            ["Can cause fetal toxicity, hypotension, renal impairment, and hyperkalemia."],
            ["Potassium-increasing drugs can increase hyperkalemia risk.", "Digoxin exposure can increase in some contexts; monitor when clinically indicated.", "NSAIDs can reduce antihypertensive effect and increase renal risk."],
            ["Blood pressure", "Serum creatinine/eGFR", "Serum potassium", "Interacting drugs"],
            [{"route": "oral", "formulation": "tablet", "tmax": time_value(0.5, 1), "eliminationHalfLife": time_value(24, 24, note="Terminal elimination half-life is greater than 24 hours in labeling."), "notes": ["Long terminal half-life supports a long concentration tail; clinical response is monitored by blood pressure."]}],
            [("ORAL", "mg", 20, 40, 80, None)],
        ),
        build_entry(
            "Candesartan",
            "坎地沙坦",
            ["Atacand", "Candesartan cilexetil"],
            "candesartan cilexetil tablet",
            "C09CA06",
            ["ARB", "血管紧张素II受体阻滞剂", "Angiotensin II receptor blocker", "Antihypertensive"],
            ["Hypertension", "Heart failure contexts depending on label"],
            ["Can cause fetal toxicity, hypotension, renal impairment, and hyperkalemia."],
            ["Potassium-increasing drugs can increase hyperkalemia risk.", "NSAIDs can reduce antihypertensive effect and increase renal risk."],
            ["Blood pressure", "Serum creatinine/eGFR", "Serum potassium"],
            [{"route": "oral", "formulation": "tablet", "tmax": time_value(3, 4), "eliminationHalfLife": time_value(9, 9), "notes": ["Candesartan cilexetil is a prodrug converted to candesartan during absorption."]}],
            [("ORAL", "mg", 4, 8, 16, 32)],
        ),
        build_entry(
            "Chlorthalidone",
            "氯噻酮",
            ["Thalitone"],
            "chlorthalidone tablet",
            "C03BA04",
            ["利尿剂", "噻嗪样利尿剂", "Thiazide-like diuretic", "Antihypertensive"],
            ["Hypertension", "Edema contexts depending on label"],
            ["Can cause electrolyte abnormalities, volume depletion, renal function changes, and hyperuricemia."],
            ["Lithium toxicity risk may increase with diuretics.", "Other antihypertensives can increase hypotension risk."],
            ["Blood pressure", "Serum sodium", "Serum potassium", "Serum creatinine/eGFR", "Uric acid when clinically indicated"],
            [{"route": "oral", "formulation": "tablet", "tmax": time_value(2, 6), "eliminationHalfLife": time_value(40, 60), "durationOfAction": time_value(48, 72, basis="pharmacodynamic effect"), "notes": ["Long half-life and duration distinguish chlorthalidone from shorter-acting thiazides."]}],
            [("ORAL", "mg", 12.5, 25, 50, 100)],
        ),
        build_entry(
            "Indapamide",
            "吲达帕胺",
            ["Lozol"],
            "indapamide tablet",
            "C03BA11",
            ["利尿剂", "噻嗪样利尿剂", "Thiazide-like diuretic", "Antihypertensive"],
            ["Hypertension", "Edema contexts depending on label"],
            ["Can cause electrolyte abnormalities, volume depletion, renal function changes, and hyperuricemia."],
            ["Lithium toxicity risk may increase with diuretics.", "Other antihypertensives can increase hypotension risk."],
            ["Blood pressure", "Serum sodium", "Serum potassium", "Serum creatinine/eGFR", "Uric acid when clinically indicated"],
            [{"route": "oral", "formulation": "tablet", "tmax": time_value(2, 2), "eliminationHalfLife": time_value(14, 18), "notes": ["Antihypertensive response is monitored clinically and does not equal plasma Tmax."]}],
            [("ORAL", "mg", 1.25, 2.5, 5, None)],
        ),
        build_entry(
            "Bumetanide",
            "布美他尼",
            ["Bumex"],
            "bumetanide tablet injection",
            "C03CA02",
            ["利尿剂", "袢利尿剂", "Loop diuretic"],
            ["Edema contexts depending on label"],
            ["Can cause excessive diuresis, dehydration, hypotension, electrolyte depletion, renal function changes, and ototoxicity risk."],
            ["Other diuretics or antihypertensives can increase volume depletion or hypotension risk.", "Lithium toxicity risk may increase with diuretics."],
            ["Volume status", "Blood pressure", "Serum sodium", "Serum potassium", "Serum creatinine/eGFR"],
            [
                {"route": "oral", "formulation": "tablet", "onset": time_value(30, 60, "min", "pharmacodynamic effect"), "peakEffect": time_value(1, 2, basis="pharmacodynamic effect"), "durationOfAction": time_value(4, 6, basis="pharmacodynamic effect"), "eliminationHalfLife": time_value(1, 1.5), "notes": ["Diuretic effect timing and plasma half-life are related but not identical."]},
                {"route": "intravenous", "formulation": "injection", "onset": time_value(2, 3, "min", "pharmacodynamic effect"), "durationOfAction": time_value(2, 3, basis="pharmacodynamic effect"), "eliminationHalfLife": time_value(1, 1.5), "notes": ["Intravenous onset is rapid; monitor volume status and electrolytes."]},
            ],
            [("ORAL", "mg", 0.5, 1, 2, 10), ("INTRAVENOUS", "mg", 0.5, 1, 2, None)],
        ),
        build_entry(
            "Eplerenone",
            "依普利酮",
            ["Inspra"],
            "eplerenone tablet",
            "C03DA04",
            ["醛固酮受体拮抗剂", "盐皮质激素受体拮抗剂", "Mineralocorticoid receptor antagonist", "Potassium-sparing diuretic"],
            ["Hypertension", "Heart failure or post-myocardial infarction contexts depending on label"],
            ["Can cause hyperkalemia, especially with renal impairment or interacting drugs."],
            ["Potassium supplements, ACEI/ARB, or other potassium-sparing drugs can increase hyperkalemia risk.", "Strong CYP3A inhibitors can increase eplerenone exposure."],
            ["Serum potassium", "Serum creatinine/eGFR", "Blood pressure"],
            [{"route": "oral", "formulation": "tablet", "tmax": time_value(1.5, 1.5), "eliminationHalfLife": time_value(4, 6), "timeToSteadyState": time_value(2, 2, "day"), "notes": ["Potassium monitoring is more clinically important than routine concentration monitoring."]}],
            [("ORAL", "mg", 25, 50, 100, None)],
        ),
        build_entry(
            "Hydralazine",
            "肼屈嗪",
            ["Apresoline"],
            "hydralazine hydrochloride tablet injection",
            "C02DB02",
            ["直接血管扩张剂", "Peripheral vasodilator", "Direct vasodilator", "Antihypertensive"],
            ["Hypertension contexts depending on label", "Heart failure combination contexts depending on label"],
            ["Can cause hypotension, tachycardia, fluid retention, headache, and lupus-like syndrome."],
            ["Other antihypertensives can increase hypotension risk.", "Beta blockers or diuretics may be co-used clinically but require monitoring."],
            ["Blood pressure", "Heart rate", "Fluid retention symptoms", "Lupus-like symptoms when clinically indicated"],
            [
                {"route": "oral", "formulation": "tablet", "tmax": time_value(1, 2), "eliminationHalfLife": time_value(3, 7), "notes": ["Clinical response and tolerability vary with acetylator status and comorbid conditions."]},
                {"route": "intravenous", "formulation": "injection", "onset": time_value(5, 20, "min", "clinical effect"), "durationOfAction": time_value(2, 6, basis="clinical effect"), "eliminationHalfLife": time_value(3, 7), "notes": ["IV blood pressure response can be variable; monitor hemodynamics."]},
            ],
            [("ORAL", "mg", 10, 25, 50, 300), ("INTRAVENOUS", "mg", 5, 10, 20, None)],
            categories=["cardiovascular", "peripheral-circulation", "prescription-medicine"],
        ),
        build_entry(
            "Isosorbide Mononitrate",
            "单硝酸异山梨酯",
            ["Isosorbide mononitrate", "Imdur"],
            "isosorbide mononitrate tablet",
            "C01DA14",
            ["硝酸酯类", "Nitrate", "Antianginal"],
            ["Angina prophylaxis contexts depending on label"],
            ["Can cause headache, hypotension, syncope, and tolerance with continuous nitrate exposure."],
            ["PDE5 inhibitors and riociguat can cause severe hypotension with nitrates.", "Other vasodilators or antihypertensives can increase hypotension risk."],
            ["Blood pressure", "Headache", "Dizziness/syncope", "Nitrate-free interval per label"],
            [
                {"route": "oral", "formulation": "immediate-release tablet", "tmax": time_value(0.5, 1), "eliminationHalfLife": time_value(5, 5), "durationOfAction": time_value(6, 8, basis="clinical effect"), "notes": ["Nitrate tolerance and nitrate-free interval are clinical concepts separate from Tmax."]},
                {"route": "oral", "formulation": "extended-release tablet", "tmax": time_value(3, 4.5), "eliminationHalfLife": time_value(5, 5), "durationOfAction": time_value(12, 24, basis="label formulation profile"), "notes": ["Extended-release formulations are designed for longer antianginal coverage."]},
            ],
            [("ORAL", "mg", 10, 20, 60, 120)],
            categories=["cardiovascular", "peripheral-circulation", "prescription-medicine"],
        ),
        build_entry(
            "Isosorbide Dinitrate",
            "硝酸异山梨酯",
            ["Isordil", "Isosorbide dinitrate"],
            "isosorbide dinitrate tablet sublingual",
            "C01DA08",
            ["硝酸酯类", "Nitrate", "Antianginal"],
            ["Angina treatment or prophylaxis contexts depending on route/formulation and label"],
            ["Can cause headache, hypotension, syncope, and tolerance with continuous nitrate exposure."],
            ["PDE5 inhibitors and riociguat can cause severe hypotension with nitrates.", "Other vasodilators or antihypertensives can increase hypotension risk."],
            ["Blood pressure", "Headache", "Dizziness/syncope", "Nitrate-free interval per label"],
            [
                {"route": "sublingual", "formulation": "tablet", "onset": time_value(2, 5, "min", "clinical effect"), "durationOfAction": time_value(1, 2, basis="clinical effect"), "eliminationHalfLife": time_value(1, 1), "notes": ["Sublingual onset is a clinical effect estimate, not a plasma Tmax."]},
                {"route": "oral", "formulation": "tablet", "tmax": time_value(1, 1), "eliminationHalfLife": time_value(1, 1), "durationOfAction": time_value(4, 6, basis="clinical effect"), "notes": ["Active metabolites contribute to clinical effect and duration."]},
            ],
            [("SUBLINGUAL", "mg", 2.5, 5, 10, None), ("ORAL", "mg", 5, 10, 40, 160)],
            categories=["cardiovascular", "peripheral-circulation", "prescription-medicine"],
        ),
        build_entry(
            "Minoxidil",
            "米诺地尔",
            ["Loniten"],
            "minoxidil tablet",
            "C02DC01",
            ["直接血管扩张剂", "Peripheral vasodilator", "Direct vasodilator", "Antihypertensive"],
            ["Severe hypertension contexts depending on label"],
            ["Can cause fluid retention, tachycardia, pericardial effusion, and hypertrichosis."],
            ["Other antihypertensives can increase hypotension risk.", "Diuretics and beta blockers are often relevant in clinical management but require monitoring."],
            ["Blood pressure", "Heart rate", "Fluid retention/weight", "Pericardial symptoms when clinically indicated"],
            [{"route": "oral", "formulation": "tablet", "tmax": time_value(1, 1), "durationOfAction": time_value(24, 75, basis="clinical effect"), "eliminationHalfLife": time_value(4.2, 4.2), "notes": ["Blood-pressure effect persists much longer than plasma half-life; do not equate duration with half-life."]}],
            [("ORAL", "mg", 2.5, 5, 10, 100)],
            categories=["cardiovascular", "peripheral-circulation", "prescription-medicine"],
        ),
        build_entry(
            "Pravastatin",
            "普伐他汀",
            ["Pravachol"],
            "pravastatin sodium tablet",
            "C10AA03",
            ["他汀类", "HMG-CoA还原酶抑制剂", "Statin", "Lipid-lowering medicine"],
            ["Hyperlipidemia and cardiovascular risk-reduction contexts depending on label"],
            ["Can cause myopathy/rhabdomyolysis and liver enzyme abnormalities."],
            ["Other myopathy-risk drugs can increase muscle toxicity risk.", "Some transport inhibitors can increase statin exposure."],
            ["LDL-C response", "Muscle symptoms", "Creatine kinase when clinically indicated", "Liver enzymes when clinically indicated"],
            [{"route": "oral", "formulation": "tablet", "tmax": time_value(1, 1.5), "eliminationHalfLife": time_value(1.8, 1.8), "notes": ["LDL-C reduction is a longer-term pharmacodynamic endpoint and is not equivalent to Tmax."]}],
            [("ORAL", "mg", 10, 20, 40, 80)],
        ),
        build_entry(
            "Simvastatin",
            "辛伐他汀",
            ["Zocor"],
            "simvastatin tablet",
            "C10AA01",
            ["他汀类", "HMG-CoA还原酶抑制剂", "Statin", "Lipid-lowering medicine"],
            ["Hyperlipidemia and cardiovascular risk-reduction contexts depending on label"],
            ["Can cause myopathy/rhabdomyolysis and liver enzyme abnormalities; risk increases with interacting drugs and higher dose contexts."],
            ["Strong CYP3A inhibitors can markedly increase exposure.", "Gemfibrozil and other myopathy-risk combinations require caution or avoidance per label."],
            ["LDL-C response", "Muscle symptoms", "Creatine kinase when clinically indicated", "Liver enzymes when clinically indicated", "Interacting drugs"],
            [{"route": "oral", "formulation": "tablet", "tmax": time_value(1.3, 2.4), "eliminationHalfLife": time_value(2, 2), "notes": ["Simvastatin is a lactone prodrug; lipid response is a longer-term endpoint."]}],
            [("ORAL", "mg", 5, 10, 20, 40)],
        ),
        build_entry(
            "Ezetimibe",
            "依折麦布",
            ["Zetia"],
            "ezetimibe tablet",
            "C10AX09",
            ["胆固醇吸收抑制剂", "Cholesterol absorption inhibitor", "Lipid-lowering medicine"],
            ["Hyperlipidemia contexts depending on label"],
            ["Can be associated with liver enzyme abnormalities when combined with statins and rare muscle symptoms."],
            ["Cyclosporine can increase ezetimibe exposure.", "Bile acid sequestrants can reduce ezetimibe exposure if timing is not separated."],
            ["LDL-C response", "Liver enzymes when combined with statins and clinically indicated", "Muscle symptoms when clinically indicated"],
            [{"route": "oral", "formulation": "tablet", "tmax": time_value(4, 12, note="Approximate Tmax for parent ezetimibe; ezetimibe-glucuronide peaks earlier in labeling."), "eliminationHalfLife": time_value(22, 22), "notes": ["Lipid response is a longer-term endpoint and should not be equated with plasma Tmax."]}],
            [("ORAL", "mg", 10, 10, None, None)],
        ),
        build_entry(
            "Ticagrelor",
            "替格瑞洛",
            ["Brilinta"],
            "ticagrelor tablet",
            "B01AC24",
            ["P2Y12抑制剂", "P2Y12 inhibitor", "Antiplatelet", "Antithrombotic"],
            ["Acute coronary syndrome and selected cardiovascular risk contexts depending on label"],
            ["Bleeding can be serious or fatal; dyspnea and bradyarrhythmia-related events may occur."],
            ["Strong CYP3A inhibitors can increase ticagrelor exposure.", "Strong CYP3A inducers can reduce exposure.", "Other antithrombotics or NSAIDs can increase bleeding risk."],
            ["Bleeding symptoms", "Dyspnea", "Heart rate symptoms when clinically indicated", "Interacting drugs"],
            [{"route": "oral", "formulation": "tablet", "tmax": time_value(1.5, 1.5), "eliminationHalfLife": time_value(7, 9, note="Approximate half-life for ticagrelor and active metabolite."), "notes": ["Platelet inhibition and bleeding risk are pharmacodynamic outcomes distinct from plasma Tmax."]}],
            [("ORAL", "mg", 60, 90, 180, None)],
            categories=["cardiovascular", "antithrombotic", "prescription-medicine"],
        ),
        build_entry(
            "Prasugrel",
            "普拉格雷",
            ["Effient"],
            "prasugrel tablet",
            "B01AC22",
            ["P2Y12抑制剂", "P2Y12 inhibitor", "Antiplatelet", "Antithrombotic"],
            ["Acute coronary syndrome with PCI contexts depending on label"],
            ["Bleeding can be serious or fatal; risk depends on age, body weight, history of stroke/TIA, procedures, and interacting drugs."],
            ["Other antithrombotics or NSAIDs can increase bleeding risk.", "CYP interactions are less central than with some alternatives but clinical context matters."],
            ["Bleeding symptoms", "Procedure timing", "Concomitant antithrombotic drugs"],
            [{"route": "oral", "formulation": "tablet", "tmax": time_value(0.5, 0.5, note="Approximate Tmax for active metabolite."), "eliminationHalfLife": time_value(7, 7, note="Approximate half-life for active metabolite."), "notes": ["Platelet inhibition is not the same concept as plasma Tmax."]}],
            [("ORAL", "mg", 5, 10, 60, None)],
            categories=["cardiovascular", "antithrombotic", "prescription-medicine"],
        ),
        build_entry(
            "Edoxaban",
            "依度沙班",
            ["Savaysa", "Lixiana"],
            "edoxaban tablet",
            "B01AF03",
            ["直接口服抗凝药", "Xa因子抑制剂", "DOAC", "Factor Xa inhibitor", "Antithrombotic"],
            ["Stroke/systemic embolism risk reduction and venous thromboembolism contexts depending on label"],
            ["Bleeding can be serious or fatal. Renal function affects exposure and labeled use."],
            ["P-gp inhibitors can increase exposure in some contexts.", "Other anticoagulants, antiplatelets, or NSAIDs can increase bleeding risk."],
            ["Bleeding symptoms", "Renal function", "Concomitant antithrombotics/interacting drugs"],
            [{"route": "oral", "formulation": "tablet", "tmax": time_value(1, 2), "eliminationHalfLife": time_value(10, 14), "notes": ["Routine plasma concentration monitoring is not typical; selected anti-Xa/drug-calibrated testing may be used in specific scenarios."]}],
            [("ORAL", "mg", 15, 30, 60, None)],
            categories=["cardiovascular", "antithrombotic", "prescription-medicine"],
        ),
        build_entry(
            "Enoxaparin",
            "依诺肝素",
            ["Lovenox", "Enoxaparin sodium"],
            "enoxaparin sodium injection",
            "B01AB05",
            ["低分子肝素", "Low molecular weight heparin", "Anticoagulant", "Antithrombotic"],
            ["Venous thromboembolism treatment/prophylaxis and selected ACS contexts depending on label"],
            ["Bleeding can be serious or fatal. Renal impairment can increase exposure and bleeding risk."],
            ["Other anticoagulants, antiplatelets, or NSAIDs can increase bleeding risk.", "Neuraxial anesthesia/spinal puncture can increase spinal hematoma risk."],
            ["Bleeding symptoms", "CBC/platelets", "Renal function", "Anti-Xa in selected scenarios", "Procedure timing"],
            [{"route": "subcutaneous", "formulation": "injection", "peakEffect": time_value(3, 5, basis="pharmacodynamic effect", note="Peak anti-Xa activity after subcutaneous administration."), "eliminationHalfLife": time_value(4.5, 7), "notes": ["Anti-Xa activity is an anticoagulant activity measure, not routine plasma concentration monitoring."]}],
            [("SUBCUTANEOUS", "mg", 30, 40, 80, 150)],
            categories=["cardiovascular", "antithrombotic", "prescription-medicine"],
        ),
        build_entry(
            "Ranolazine",
            "雷诺嗪",
            ["Ranexa"],
            "ranolazine extended-release tablet",
            "C01EB18",
            ["抗心绞痛药", "Late sodium current inhibitor", "Antianginal"],
            ["Chronic angina contexts depending on label"],
            ["Can prolong QT interval; renal/hepatic impairment and interacting drugs affect risk."],
            ["Strong CYP3A inhibitors can increase exposure and may be contraindicated per label.", "CYP3A inducers can reduce exposure.", "Other QT-prolonging contexts require caution."],
            ["Angina symptoms", "ECG/QT when clinically indicated", "Renal function", "Interacting drugs"],
            [{"route": "oral", "formulation": "extended-release tablet", "tmax": time_value(2, 5), "eliminationHalfLife": time_value(7, 7), "timeToSteadyState": time_value(3, 3, "day"), "notes": ["Antianginal response and QT effects are pharmacodynamic outcomes distinct from plasma Tmax."]}],
            [("ORAL", "mg", 500, 1000, None, None)],
        ),
    ]


def apply_existing_updates() -> None:
    updates = existing_time_course_updates()
    for path in SOURCE_DIR.glob("*.json"):
        data = json.loads(path.read_text(encoding="utf-8"))
        changed = False
        for substance in data.get("substances", []):
            name = substance.get("name")
            if name in updates:
                substance["timeCourse"] = updates[name]
                changed = True
            if substance.get("roas") and ("?" in substance.get("dosageRemark", "")):
                substance["dosageRemark"] = DISCLAIMER
                changed = True
        if changed:
            path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    apply_existing_updates()
    BATCH_FILE.write_text(
        json.dumps({"substances": batch_substances()}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Wrote {BATCH_FILE} with {len(batch_substances())} substances")


if __name__ == "__main__":
    main()
