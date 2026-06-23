#!/usr/bin/env python3
import json
from pathlib import Path


DATE = "2026-06-23"
SOURCE_DIR = Path("tools/drugdata/cardiovascular")
OUTPUT_FILE = SOURCE_DIR / "atc_cardiovascular_catalog_2026_06_23.json"

DOSE_REMARK = (
    "临床参考剂量：light/common/strong/heavy 仅表示低参考、常见参考、较高或上限参考层级，"
    "不表示娱乐性强度，也不等同个体化处方剂量或确定中毒剂量。毒理风险与适应证、年龄、体重、"
    "肝肾功能、合并用药、给药途径和监测结果有关。本资料仅用于学习和资料索引，不构成医疗建议，"
    "不用于诊断、处方、自行用药或调整剂量；实际用药必须遵循医生医嘱和当地批准说明书。"
)

GROUPS = {
    "B01": ("atc-b01", "B01 抗血栓药物", "Antithrombotic agents"),
    "C01": ("atc-c01", "C01 心脏治疗药物", "Cardiac therapy"),
    "C02": ("atc-c02", "C02 抗高血压药", "Antihypertensives"),
    "C03": ("atc-c03", "C03 利尿剂", "Diuretics"),
    "C04": ("atc-c04", "C04 外周血管扩张剂", "Peripheral vasodilators"),
    "C05": ("atc-c05", "C05 血管保护药", "Vasoprotectives"),
    "C07": ("atc-c07", "C07 β受体阻滞剂", "Beta blocking agents"),
    "C08": ("atc-c08", "C08 钙通道阻滞剂", "Calcium channel blockers"),
    "C09": ("atc-c09", "C09 肾素-血管紧张素系统药物", "Agents acting on the renin-angiotensin system"),
    "C10": ("atc-c10", "C10 调脂药", "Lipid modifying agents"),
}

ATC_CATEGORY_DEFINITIONS = [
    {
        "name": slug,
        "description": f"{zh} / {en}. ATC 分类用于学习索引，不替代本地批准适应证或说明书。",
        "color": color,
        "url": f"https://atcddd.fhi.no/atc_ddd_index/?code={code}",
    }
    for code, (slug, zh, en), color in [
        ("B01", GROUPS["B01"], 4284513675),
        ("C01", GROUPS["C01"], 4280391411),
        ("C02", GROUPS["C02"], 4284374622),
        ("C03", GROUPS["C03"], 4287792575),
        ("C04", GROUPS["C04"], 4283215696),
        ("C05", GROUPS["C05"], 4288454041),
        ("C07", GROUPS["C07"], 4282551504),
        ("C08", GROUPS["C08"], 4286578688),
        ("C09", GROUPS["C09"], 4280063111),
        ("C10", GROUPS["C10"], 4289959458),
    ]
]

OPENFDA_LABEL_NOT_FOUND = {
    "Digitoxin",
    "Moxonidine",
    "Bendroflumethiazide",
    "Xipamide",
    "Canrenone",
    "Naftidrofuryl",
    "Nicergoline",
    "Nicotinyl Alcohol",
    "Troxerutin",
    "Calcium Dobesilate",
    "Rutoside",
    "Lercanidipine",
    "Sacubitril/Valsartan",
    "Eprosartan",
    "Ticlopidine",
    "Abciximab",
    "Vorapaxar",
    "Betrixaban",
    "Acenocoumarol",
    "Phenprocoumon",
}


def source_ref(name: str, query: str | None = None, source_type: str = "regulatory-label") -> dict:
    label_query = query or name
    return {
        "title": f"DailyMed {name} label search",
        "url": "https://dailymed.nlm.nih.gov/dailymed/search.cfm?query=" + label_query.replace(" ", "%20"),
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
    min_value: float | None,
    max_value: float | None = None,
    unit: str = "h",
    basis: str = "label pharmacokinetics",
    note: str | None = None,
) -> dict:
    result = {"min": min_value, "max": max_value, "unit": unit, "basis": basis}
    if note:
        result["note"] = note
    return result


def dose(route: str, units: str, light: float | None, common: float | None, strong: float | None, heavy: float | None) -> dict:
    values = {"units": units}
    if light is not None:
        values["lightMin"] = light
    if common is not None:
        values["commonMin"] = common
    if strong is not None:
        values["strongMin"] = strong
    if heavy is not None:
        values["heavyMin"] = heavy
    return {"name": route, "dose": values}


def atc_group(code: str) -> tuple[str, str, str]:
    return GROUPS[code[:3]]


def ordered_unique(values: list[str]) -> list[str]:
    seen = set()
    result = []
    for value in values:
        if value and value not in seen:
            result.append(value)
            seen.add(value)
    return result


def time_course(
    *,
    route: str = "oral",
    formulation: str = "tablet/capsule",
    onset: tuple[float | None, float | None, str] | None = None,
    tmax: tuple[float | None, float | None, str] | None = None,
    peak: tuple[float | None, float | None, str] | None = None,
    duration: tuple[float | None, float | None, str] | None = None,
    half_life: tuple[float | None, float | None, str] | None = None,
    steady: tuple[float | None, float | None, str] | None = None,
    notes: list[str] | None = None,
) -> dict:
    result = {"route": route, "formulation": formulation}
    if onset:
        result["onset"] = time_value(onset[0], onset[1], onset[2], "clinical effect")
    if tmax:
        result["tmax"] = time_value(tmax[0], tmax[1], tmax[2], "plasma concentration")
    if peak:
        result["peakEffect"] = time_value(peak[0], peak[1], peak[2], "pharmacodynamic effect")
    if duration:
        result["durationOfAction"] = time_value(duration[0], duration[1], duration[2], "clinical effect")
    if half_life:
        result["eliminationHalfLife"] = time_value(half_life[0], half_life[1], half_life[2])
    if steady:
        result["timeToSteadyState"] = time_value(steady[0], steady[1], steady[2])
    result["notes"] = [
        "Tmax、药效峰值、持续时间和半衰期不是同一概念；曲线仅用于学习索引。",
        "具体数值需按制剂、适应证、人群、肝肾功能、合并用药和当地批准说明书复核。",
    ] + (notes or [])
    return result


def tdm_default(code: str, drug_class: list[str]) -> dict:
    if code.startswith("B01"):
        return {
            "isRoutinelyMonitored": False,
            "monitoringType": "coagulation/platelet activity rather than routine plasma concentration",
            "reason": "多数抗血栓药不按常规血药浓度调剂量；临床更关注出血/血栓风险、凝血或抗Xa等检测和器官功能。",
            "pharmacokineticParametersAvailable": True,
        }
    if any("抗心律失常" in item or "Antiarrhythmic" in item for item in drug_class):
        return {
            "isRoutinelyMonitored": False,
            "monitoringType": "selected ECG/concentration monitoring depending on drug and scenario",
            "reason": "部分抗心律失常药可在特定场景监测浓度，但本条目重点是药代/药效时间进程。",
            "pharmacokineticParametersAvailable": True,
        }
    return {
        "isRoutinelyMonitored": False,
        "monitoringType": "not routinely monitored by plasma concentration",
        "reason": "临床通常按症状、血压/心率、实验室安全性指标或目标指标监测，而不是常规血药浓度。",
        "pharmacokineticParametersAvailable": True,
    }


def verification_ref(title: str, url: str, source_type: str) -> dict:
    return {
        "title": title,
        "url": url,
        "sourceType": source_type,
        "accessedDate": DATE,
    }


def retime(
    *,
    route: str = "oral",
    formulation: str = "tablet/capsule",
    onset: dict | None = None,
    tmax: dict | None = None,
    peak: dict | None = None,
    duration: dict | None = None,
    half_life: dict | None = None,
    steady: dict | None = None,
    notes: list[str],
    refs: list[dict],
) -> list[dict]:
    result = {"route": route, "formulation": formulation, "notes": notes, "sourceRefs": refs}
    if onset:
        result["onset"] = onset
    if tmax:
        result["tmax"] = tmax
    if peak:
        result["peakEffect"] = peak
    if duration:
        result["durationOfAction"] = duration
    if half_life:
        result["eliminationHalfLife"] = half_life
    if steady:
        result["timeToSteadyState"] = steady
    return [result]


def apply_verified_override(entry: dict) -> None:
    name = entry["name"]
    atc = (entry.get("clinicalInfo") or {}).get("atcCodes", [""])[0]
    atc_source = atc_ref(atc) if atc else None
    base_notes = [
        "Tmax、药效峰值、持续时间和半衰期不是同一概念；曲线仅用于学习索引。",
        "复核来源仍需结合具体国家/地区批准说明书、制剂和患者人群；本资料不构成医疗建议。",
    ]

    overrides = {
        "Digitoxin": {
            "refs": [
                verification_ref(
                    "Clinical Pharmacokinetics of Digitoxin",
                    "https://experts.arizona.edu/en/publications/clinical-pharmacokinetics-of-digitoxin/",
                    "literature-review",
                ),
                verification_ref(
                    "Pharmacokinetics and bioavailability of digitoxin by a specific assay",
                    "https://pubmed.ncbi.nlm.nih.gov/6489430/",
                    "human-pk-study",
                ),
            ],
            "timeCourse": retime(
                duration=time_value(5, 14, "day", "literature estimate"),
                half_life=time_value(5.8, 5.8, "day", "human pharmacokinetic study"),
                notes=base_notes + [
                    "Digitoxin 分布完成约需 4-6 h；临床反应与分布相期间血浆浓度不直接对应。",
                    "半衰期估计受检测方法影响较大，条目使用文献中口服后约 5.8 天的人体 PK 数据。"
                ],
                refs=[],
            ),
        },
        "Moxonidine": {
            "refs": [
                verification_ref(
                    "Moxonidine 200 microgram film-coated tablets SmPC",
                    "https://www.medicines.org.uk/emc/product/4140/smpc",
                    "regulatory-label",
                ),
                verification_ref(
                    "Pharmacokinetics of moxonidine after single and repeated daily doses",
                    "https://pubmed.ncbi.nlm.nih.gov/3437071/",
                    "human-pk-study",
                ),
            ],
            "timeCourse": retime(
                tmax=time_value(0.5, 3, "h", "label pharmacokinetics"),
                duration=time_value(12, 24, "h", "clinical effect"),
                half_life=time_value(2.2, 2.8, "h", "label pharmacokinetics"),
                notes=base_notes + ["SmPC 提示肾功能不全时暴露和半衰期升高，需按肾功能调整解释。"],
                refs=[],
            ),
        },
        "Bendroflumethiazide": {
            "refs": [
                verification_ref(
                    "Bendroflumethiazide Evolan SmPC",
                    "https://docetp.mpa.se/LMF/Bendroflumetiazid%20Evolan%20tablet%20ENG%20SmPC_09001bee807a0c7b.pdf",
                    "regulatory-label",
                ),
                verification_ref(
                    "Arrow Bendrofluazide New Zealand Data Sheet",
                    "https://www.medsafe.govt.nz/profs/datasheet/a/arrow-bendrofluazidetab.pdf",
                    "regulatory-label",
                ),
            ],
            "timeCourse": retime(
                onset=time_value(2, 2, "h", "clinical effect"),
                tmax=time_value(2, 2.5, "h", "label pharmacokinetics"),
                duration=time_value(12, 18, "h", "clinical effect"),
                half_life=time_value(3, 8.5, "h", "label pharmacokinetics"),
                notes=base_notes + ["利尿作用起效和抗高血压作用起效不是同一时间尺度。"],
                refs=[],
            ),
        },
        "Xipamide": {
            "refs": [
                verification_ref(
                    "Xipamide. A review of its pharmacodynamic and pharmacokinetic properties",
                    "https://pubmed.ncbi.nlm.nih.gov/3905333/",
                    "literature-review",
                ),
                verification_ref(
                    "Xipamide review summary",
                    "https://www.scilit.com/publications/af44e5f93558fdc1556547ec1e955a51",
                    "literature-index",
                ),
            ],
            "timeCourse": retime(
                onset=time_value(1, 1, "h", "clinical effect"),
                tmax=time_value(1, 2, "h", "literature estimate"),
                peak=time_value(3, 6, "h", "pharmacodynamic effect"),
                duration=time_value(24, 24, "h", "clinical effect"),
                half_life=time_value(5.8, 8.2, "h", "literature estimate"),
                notes=base_notes + ["Xipamide 的公开药代资料主要来自综述和较早研究，需按当地产品资料复核。"],
                refs=[],
            ),
        },
        "Canrenone": {
            "refs": [
                verification_ref(
                    "Pharmacokinetics of canrenone after oral administration",
                    "https://pubmed.ncbi.nlm.nih.gov/6653638/",
                    "human-pk-study",
                ),
                verification_ref(
                    "Spironolactone/canrenone pharmacokinetic review",
                    "https://pdfs.semanticscholar.org/5a43/a36970f3e3ffbb6e7524d89a3f67d688d96b.pdf",
                    "literature-review",
                ),
            ],
            "timeCourse": retime(
                tmax=time_value(2, 4.3, "h", "literature estimate"),
                duration=time_value(24, 48, "h", "pharmacodynamic effect"),
                half_life=time_value(3.9, 22.6, "h", "literature estimate", "Direct canrenone and canrenone-as-metabolite estimates differ across studies."),
                notes=base_notes + ["Canrenone 作为给药药物和作为螺内酯活性代谢物时，半衰期估计可能不同。"],
                refs=[],
            ),
        },
        "Naftidrofuryl": {
            "refs": [
                verification_ref(
                    "Naftidrofuryl oxalate BCS/pharmacokinetic discussion",
                    "https://pmc.ncbi.nlm.nih.gov/articles/PMC7766335/",
                    "literature-review",
                ),
            ],
            "timeCourse": retime(
                onset=time_value(0.8, 1, "h", "clinical effect"),
                tmax=time_value(0.5, 1, "h", "literature estimate"),
                duration=time_value(2, 3, "h", "clinical effect"),
                half_life=time_value(1.2, 2, "h", "literature estimate"),
                notes=base_notes + ["公开资料提示制剂间生物利用度差异较大；需按具体 naftidrofuryl oxalate 产品资料复核。"],
                refs=[],
            ),
        },
        "Nicergoline": {
            "refs": [
                verification_ref(
                    "EMA referral: nicergoline-containing medicines",
                    "https://ec.europa.eu/health/documents/community-register/2013/20130927126534/anx_126534_en.pdf",
                    "regulatory-review",
                ),
                verification_ref(
                    "Nicergoline generic pharmacokinetic summary",
                    "https://www.mims.com/singapore/drug/info/nicergoline?mtype=generic",
                    "clinical-drug-reference",
                ),
            ],
            "timeCourse": retime(
                tmax=time_value(1.5, 5, "h", "clinical-drug-reference", "Parent nicergoline and metabolites have different Tmax estimates."),
                duration=time_value(8, 12, "h", "clinical effect"),
                half_life=time_value(11, 20, "h", "clinical-drug-reference"),
                notes=base_notes + ["EMA 曾限制含麦角衍生物药物适应证；本条目仅保留药代索引，不代表推荐使用。"],
                refs=[],
            ),
        },
        "Nicotinyl Alcohol": {
            "refs": [
                verification_ref(
                    "DrugCentral: nicotinyl alcohol",
                    "https://drugcentral.org/drugcard/1921",
                    "drug-database",
                ),
                verification_ref(
                    "Vasodilator effects of nicotinyl tartrate",
                    "https://pubmed.ncbi.nlm.nih.gov/14066712/",
                    "legacy-literature",
                ),
            ],
            "timeCourse": retime(
                duration=time_value(4, 6, "h", "literature estimate"),
                notes=base_notes + [
                    "未找到可靠现代人体血浆 Tmax/半衰期标签资料；此条仅按旧文献和药物数据库保留 C04AC02 外周血管扩张药索引。",
                    "不要将此条曲线解释为血浆浓度曲线。"
                ],
                refs=[],
            ),
        },
        "Troxerutin": {
            "refs": [
                verification_ref(
                    "LC-MS-MS determination of troxerutin in plasma and human PK study",
                    "https://doi.org/10.1002/rcm.2764",
                    "human-pk-study",
                ),
            ],
            "timeCourse": retime(
                tmax=time_value(1.06, 1.82, "h", "human pharmacokinetic study", "Mean 1.44 h with SD 0.38 h after 300 mg in healthy volunteers."),
                duration=time_value(12, 24, "h", "clinical effect"),
                half_life=time_value(4.32, 11.56, "h", "human pharmacokinetic study", "Mean 7.94 h with SD 3.62 h after 300 mg in healthy volunteers."),
                notes=base_notes + ["人体 PK 来自小样本健康志愿者研究；不同制剂和适应证需另行复核。"],
                refs=[],
            ),
        },
        "Calcium Dobesilate": {
            "refs": [
                verification_ref(
                    "Calcium dobesilate capsule bioequivalence and PK study",
                    "https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0284576",
                    "human-pk-study",
                ),
                verification_ref(
                    "Doxium product information",
                    "https://www.e-lactancia.org/media/papers/Dobesilate-DS-OMPharma2018.pdf",
                    "product-information",
                ),
            ],
            "timeCourse": retime(
                tmax=time_value(6, 6, "h", "human pharmacokinetic study"),
                duration=time_value(12, 24, "h", "clinical effect"),
                half_life=time_value(5, 5, "h", "human pharmacokinetic study"),
                notes=base_notes + ["食物可延迟并降低吸收；不同研究中的 Tmax/t1/2 可能受制剂和进食状态影响。"],
                refs=[],
            ),
        },
        "Rutoside": {
            "refs": [
                verification_ref(
                    "Pharmacokinetics of quercetin from quercetin aglycone and rutin in healthy volunteers",
                    "https://pubmed.ncbi.nlm.nih.gov/11151743/",
                    "human-pk-study",
                ),
                verification_ref(
                    "DrugBank: Rutin",
                    "https://go.drugbank.com/drugs/DB01698",
                    "drug-database",
                ),
            ],
            "timeCourse": retime(
                tmax=time_value(6.5, 6.5, "h", "human pharmacokinetic study", "Reported for quercetin exposure after rutin, not unchanged rutoside."),
                duration=time_value(12, 24, "h", "literature estimate"),
                notes=base_notes + [
                    "未找到可直接用于处方药标签的 rutoside/rutin 人体半衰期资料；DrugBank 也未列出 half-life。",
                    "此条不能解释为未变化 rutoside 的完整血浆浓度曲线。"
                ],
                refs=[],
            ),
        },
        "Lercanidipine": {
            "refs": [
                verification_ref(
                    "Lercanidipine public assessment report",
                    "https://www.geneesmiddeleninformatiebank.nl/pars/h102333.pdf",
                    "regulatory-assessment",
                ),
                verification_ref(
                    "Zanidip product information",
                    "https://medsinfo.com.au/api/documents/Zanidip_PI?format=pdf",
                    "regulatory-label",
                ),
            ],
            "timeCourse": retime(
                tmax=time_value(1.5, 3, "h", "label pharmacokinetics"),
                duration=time_value(24, 24, "h", "clinical effect"),
                half_life=time_value(5.8, 10, "h", "label pharmacokinetics"),
                notes=base_notes + ["半衰期较短但因脂膜结合而有 24 h 抗高血压活性；不要把 Tmax 当作最大降压时间。"],
                refs=[],
            ),
        },
        "Sacubitril/Valsartan": {
            "refs": [
                verification_ref(
                    "DailyMed: ENTRESTO sacubitril and valsartan",
                    "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=000dc81d-ab91-450c-8eae-8eb74e72296f",
                    "regulatory-label",
                ),
                verification_ref(
                    "EMA Entresto product information",
                    "https://www.ema.europa.eu/en/documents/product-information/entresto-epar-product-information_en.pdf",
                    "regulatory-label",
                ),
            ],
            "timeCourse": retime(
                tmax=time_value(0.5, 2, "h", "label pharmacokinetics"),
                duration=time_value(12, 24, "h", "clinical effect"),
                half_life=time_value(1.4, 11.5, "h", "label pharmacokinetics", "Sacubitril, LBQ657, and valsartan have different half-lives."),
                notes=base_notes + ["半衰期范围同时涵盖 sacubitril、活性代谢物 LBQ657 和 valsartan；具体组分需看说明书。"],
                refs=[],
            ),
        },
        "Eprosartan": {
            "refs": [
                verification_ref(
                    "DailyMed: TEVETEN HCT eprosartan/hydrochlorothiazide",
                    "https://dailymed.nlm.nih.gov/dailymed/fda/fdaDrugXsl.cfm?setid=b1dfcac3-ef66-4d3e-9d58-37d3f516eb31",
                    "regulatory-label",
                ),
                verification_ref(
                    "Pharmacokinetics of eprosartan in healthy subjects and special populations",
                    "https://pubmed.ncbi.nlm.nih.gov/10213525/",
                    "literature-review",
                ),
            ],
            "timeCourse": retime(
                tmax=time_value(1, 2, "h", "literature estimate"),
                duration=time_value(12, 24, "h", "clinical effect"),
                half_life=time_value(5, 20, "h", "label/literature estimate", "Single-dose oral literature often reports 5-9 h; DailyMed combo label reports about 20 h after multiple 600 mg doses."),
                notes=base_notes + ["半衰期估计与单次/多次给药和制剂资料来源有关。"],
                refs=[],
            ),
        },
        "Ticlopidine": {
            "refs": [
                verification_ref(
                    "DailyMed: Ticlopidine hydrochloride label search",
                    "https://dailymed.nlm.nih.gov/dailymed/search.cfm?labeltype=all&query=TICLOPIDINE",
                    "regulatory-label",
                ),
                verification_ref(
                    "Canadian product monograph: Ticlopidine hydrochloride tablets",
                    "https://pdf.hres.ca/dpd_pm/00015080.PDF",
                    "regulatory-label",
                ),
            ],
            "timeCourse": retime(
                tmax=time_value(2, 2, "h", "label pharmacokinetics"),
                peak=time_value(8, 11, "day", "pharmacodynamic effect"),
                duration=time_value(5, 10, "day", "pharmacodynamic effect"),
                half_life=time_value(20, 50, "h", "literature estimate"),
                notes=base_notes + ["抗血小板效应与血小板功能恢复相关，不等同 ticlopidine 血浆半衰期。"],
                refs=[],
            ),
        },
        "Abciximab": {
            "refs": [
                verification_ref(
                    "FDA label: ReoPro abciximab",
                    "https://www.accessdata.fda.gov/drugsatfda_docs/label/1997/abcicen110597-lab.pdf",
                    "regulatory-label",
                ),
                verification_ref(
                    "Canadian product monograph: ReoPro",
                    "https://pdf.hres.ca/dpd_pm/00042357.PDF",
                    "regulatory-label",
                ),
            ],
            "timeCourse": retime(
                route="intravenous",
                formulation="bolus/infusion",
                onset=time_value(10, 10, "min", "pharmacodynamic effect"),
                duration=time_value(24, 48, "h", "pharmacodynamic effect"),
                half_life=time_value(10, 30, "min", "label pharmacokinetics", "Free plasma concentration has an initial phase <10 min and second phase about 30 min."),
                notes=base_notes + ["血小板功能通常 24-48 h 恢复，但药物可在血小板结合状态下更久存在。"],
                refs=[],
            ),
        },
        "Vorapaxar": {
            "refs": [
                verification_ref(
                    "DailyMed: ZONTIVITY vorapaxar",
                    "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=f001f406-8cf1-40a4-bbf5-1775fe020122",
                    "regulatory-label",
                ),
            ],
            "timeCourse": retime(
                tmax=time_value(1, 2, "h", "label pharmacokinetics"),
                duration=time_value(14, 28, "day", "pharmacodynamic effect"),
                half_life=time_value(5, 13, "day", "label pharmacokinetics"),
                steady=time_value(21, 21, "day", "label pharmacokinetics"),
                notes=base_notes + ["有效半衰期约 3-4 天，终末半衰期约 8 天；抗血小板效应消退不能简单按 Tmax 判断。"],
                refs=[],
            ),
        },
        "Betrixaban": {
            "refs": [
                verification_ref(
                    "FDA label: BEVYXXA betrixaban",
                    "https://www.accessdata.fda.gov/drugsatfda_docs/label/2020/208383s007lbl.pdf",
                    "regulatory-label",
                ),
            ],
            "timeCourse": retime(
                tmax=time_value(3, 4, "h", "label pharmacokinetics"),
                duration=time_value(24, 72, "h", "pharmacodynamic effect"),
                half_life=time_value(19, 27, "h", "label pharmacokinetics"),
                notes=base_notes + ["食物可降低暴露；常规用药通常不按血药浓度调整。"],
                refs=[],
            ),
        },
        "Acenocoumarol": {
            "refs": [
                verification_ref(
                    "Sinthrome acenocoumarol SmPC",
                    "https://www.medicines.org.uk/emc/product/2058/smpc",
                    "regulatory-label",
                ),
                verification_ref(
                    "Acenocoumarol pharmacokinetic review",
                    "https://pmc.ncbi.nlm.nih.gov/articles/PMC6785764/",
                    "literature-review",
                ),
            ],
            "timeCourse": retime(
                tmax=time_value(1, 3, "h", "literature estimate"),
                peak=time_value(24, 48, "h", "pharmacodynamic effect"),
                duration=time_value(2, 4, "day", "pharmacodynamic effect"),
                half_life=time_value(8, 11, "h", "label pharmacokinetics"),
                notes=base_notes + ["维生素 K 拮抗剂主要按 INR 监测，抗凝效应受凝血因子周转影响。"],
                refs=[],
            ),
        },
        "Phenprocoumon": {
            "refs": [
                verification_ref(
                    "Phenprocoumon pharmacokinetics in emergency situations",
                    "https://pubmed.ncbi.nlm.nih.gov/36422567/",
                    "clinical-study",
                ),
                verification_ref(
                    "Acute Porphyria Drugs: Phenprocoumon",
                    "https://drugsporphyria.net/monograph/677/B01AA04",
                    "clinical-drug-reference",
                ),
            ],
            "timeCourse": retime(
                tmax=time_value(1, 3, "h", "literature estimate"),
                peak=time_value(48, 96, "h", "pharmacodynamic effect"),
                duration=time_value(5, 14, "day", "pharmacodynamic effect"),
                half_life=time_value(5, 9, "day", "clinical/literature estimate"),
                notes=base_notes + ["维生素 K 拮抗剂主要按 INR 监测；phenprocoumon 半衰期长，停药后效应可持续多日。"],
                refs=[],
            ),
        },
    }

    override = overrides.get(name)
    if not override:
        return

    refs = override["refs"] + ([atc_source] if atc_source else [])
    updated_time_courses = []
    for time_course_item in override["timeCourse"]:
        copied = dict(time_course_item)
        copied["sourceRefs"] = refs
        updated_time_courses.append(copied)
    entry["timeCourse"] = updated_time_courses
    entry["url"] = refs[0]["url"]
    clinical = entry.get("clinicalInfo") or {}
    clinical["sourceRefs"] = refs
    if name in {"Nicotinyl Alcohol", "Rutoside"}:
        clinical.setdefault("majorWarnings", []).append("公开人体药代资料有限，相关数值仅作资料索引，不应用于临床推断。")


DEFAULT_WARNINGS = [
    "严重不良反应风险与适应证、剂量、器官功能、合并用药和个体易感性有关。",
    "本资料仅用于学习索引，不能用于自行用药或调整剂量。",
]
DEFAULT_INTERACTIONS = [
    "与同类药或影响血压、心率、凝血、肾功能、电解质、CYP/P-gp 转运的药物合用时需查阅说明书。",
]
DEFAULT_MONITORING = [
    "适应证相关疗效指标",
    "血压/心率或症状",
    "肝肾功能、电解质或凝血指标，按药物类别和说明书选择",
]


def build_entry(item: dict) -> dict:
    code = item["atc"]
    slug, group_zh, group_en = atc_group(code)
    cn = item["cn"]
    drug_class = ordered_unique([group_zh, item.get("classZh", ""), group_en] + item.get("class", []))
    categories = ["cardiovascular", "prescription-medicine", slug]
    if code.startswith("B01"):
        categories.insert(1, "antithrombotic")
    if code.startswith("C04"):
        categories.insert(1, "peripheral-circulation")
    needs_local_label_review = item["name"] in OPENFDA_LABEL_NOT_FOUND
    refs = [
        source_ref(
            item["name"],
            item.get("query"),
            source_type="source-search" if needs_local_label_review else "regulatory-label",
        ),
        atc_ref(code),
    ]
    time_courses = item["timeCourse"]
    if needs_local_label_review:
        time_courses = []
        for time_course_item in item["timeCourse"]:
            copied = dict(time_course_item)
            copied["notes"] = copied.get("notes", []) + [
                "openFDA drug label API 未找到可直接核对的 FDA 标签；本条药代数值需继续按 DailyMed 搜索结果、本地批准说明书或权威药学资料逐项复核。"
            ]
            time_courses.append(copied)
    entry = {
        "name": item["name"],
        "commonNames": ordered_unique(
            [item["name"]]
            + item.get("aliases", [])
            + [cn, group_zh, item.get("classZh", ""), code, slug.upper()]
        ),
        "url": refs[0]["url"],
        "isApproved": True,
        "crossTolerances": [],
        "toxicities": item.get(
            "toxicities",
            [
                "毒性表现取决于药物类别、摄入量、给药途径、器官功能、合并用药和基础疾病。",
                "过量或严重不良反应应按急诊/毒理学流程处理，不能仅凭本条目判断。",
            ],
        ),
        "categories": ordered_unique(categories),
        "summary": (
            f"{cn}属于{group_zh}。本条目用于 ATC 分类、药代/药效时间进程、相互作用和监测要点的学习索引，"
            "不构成医疗建议、处方建议或剂量调整建议。"
        ),
        "dosageRemark": DOSE_REMARK,
        "clinicalInfo": {
            "atcCodes": [code],
            "drugClass": drug_class,
            "indications": item.get("indications", ["Indications depend on approved local labeling and clinical context."]),
            "contraindications": item.get("contraindications", []),
            "majorWarnings": item.get("warnings", DEFAULT_WARNINGS),
            "majorInteractions": item.get("interactions", DEFAULT_INTERACTIONS),
            "monitoring": item.get("monitoring", DEFAULT_MONITORING),
            "sourceRefs": refs,
        },
        "timeCourse": [dict(tc, sourceRefs=refs) for tc in time_courses],
        "tdm": item.get("tdm", tdm_default(code, drug_class)),
        "roas": item.get("roas", [dose(item.get("routeDose", "ORAL"), item.get("doseUnit", "mg"), 1, 5, 10, 50)]),
    }
    apply_verified_override(entry)
    return entry


DRUGS = [
    # C01 心脏治疗药物
    {"name": "Digitoxin", "cn": "洋地黄毒苷", "atc": "C01AA04", "classZh": "强心苷", "aliases": ["Digitalis glycoside"], "timeCourse": [time_course(tmax=(1, 2, "h"), half_life=(5, 7, "day"), duration=(5, 14, "day"))], "roas": [dose("ORAL", "mg", 0.05, 0.1, 0.2, 0.5)]},
    {"name": "Dronedarone", "cn": "决奈达隆", "atc": "C01BD07", "classZh": "III类抗心律失常药", "aliases": ["Multaq"], "class": ["Antiarrhythmic"], "timeCourse": [time_course(tmax=(3, 6, "h"), half_life=(13, 19, "h"), steady=(4, 8, "day"))], "roas": [dose("ORAL", "mg", 200, 400, 800, 1200)]},
    {"name": "Ivabradine", "cn": "伊伐布雷定", "atc": "C01EB17", "classZh": "窦房结 If 电流抑制剂", "aliases": ["Corlanor"], "timeCourse": [time_course(tmax=(1, 1, "h"), half_life=(6, 11, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 2.5, 5, 7.5, 15)]},
    {"name": "Dobutamine", "cn": "多巴酚丁胺", "atc": "C01CA07", "classZh": "正性肌力药", "aliases": ["Beta-1 agonist"], "timeCourse": [time_course(route="intravenous", formulation="infusion", onset=(1, 2, "min"), duration=(10, 20, "min"), half_life=(2, 2, "min"))], "roas": [dose("INTRAVENOUS", "mcg/kg/min", 1, 2.5, 10, 20)]},
    {"name": "Dopamine", "cn": "多巴胺", "atc": "C01CA04", "classZh": "儿茶酚胺类血管活性药", "timeCourse": [time_course(route="intravenous", formulation="infusion", onset=(5, 5, "min"), duration=(10, 10, "min"), half_life=(2, 2, "min"))], "roas": [dose("INTRAVENOUS", "mcg/kg/min", 1, 2, 10, 20)]},
    {"name": "Milrinone", "cn": "米力农", "atc": "C01CE02", "classZh": "磷酸二酯酶3抑制剂", "timeCourse": [time_course(route="intravenous", formulation="injection/infusion", onset=(5, 15, "min"), duration=(30, 60, "min"), half_life=(2, 2.5, "h"))], "roas": [dose("INTRAVENOUS", "mcg/kg/min", 0.125, 0.25, 0.5, 0.75)]},
    {"name": "Vericiguat", "cn": "维立西呱", "atc": "C01DX22", "classZh": "可溶性鸟苷酸环化酶刺激剂", "aliases": ["sGC stimulator"], "timeCourse": [time_course(tmax=(1, 4, "h"), half_life=(30, 30, "h"), steady=(6, 6, "day"))], "roas": [dose("ORAL", "mg", 2.5, 5, 10, 20)]},
    # C02 抗高血压药
    {"name": "Clonidine", "cn": "可乐定", "atc": "C02AC01", "classZh": "中枢性α2受体激动剂", "timeCourse": [time_course(tmax=(1, 3, "h"), half_life=(12, 16, "h"), duration=(6, 12, "h"))], "roas": [dose("ORAL", "mg", 0.05, 0.1, 0.3, 0.8)]},
    {"name": "Methyldopa", "cn": "甲基多巴", "atc": "C02AB01", "classZh": "中枢性抗高血压药", "timeCourse": [time_course(tmax=(2, 3, "h"), half_life=(1.5, 2, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 125, 250, 500, 2000)]},
    {"name": "Moxonidine", "cn": "莫索尼定", "atc": "C02AC05", "classZh": "咪唑啉受体激动剂", "timeCourse": [time_course(tmax=(1, 1, "h"), half_life=(2, 3, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 0.2, 0.4, 0.6, 0.8)]},
    {"name": "Prazosin", "cn": "哌唑嗪", "atc": "C02CA01", "classZh": "α1受体阻滞剂", "timeCourse": [time_course(tmax=(1, 3, "h"), half_life=(2, 3, "h"), duration=(6, 10, "h"))], "roas": [dose("ORAL", "mg", 0.5, 1, 5, 20)]},
    {"name": "Doxazosin", "cn": "多沙唑嗪", "atc": "C02CA04", "classZh": "α1受体阻滞剂", "timeCourse": [time_course(tmax=(2, 3, "h"), half_life=(16, 22, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 1, 2, 8, 16)]},
    {"name": "Terazosin", "cn": "特拉唑嗪", "atc": "C02CA03", "classZh": "α1受体阻滞剂", "timeCourse": [time_course(tmax=(1, 2, "h"), half_life=(12, 12, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 1, 2, 10, 20)]},
    {"name": "Bosentan", "cn": "波生坦", "atc": "C02KX01", "classZh": "内皮素受体拮抗剂", "timeCourse": [time_course(tmax=(3, 5, "h"), half_life=(5, 5, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 62.5, 125, 250, 500)]},
    {"name": "Sodium Nitroprusside", "cn": "硝普钠", "atc": "C02DD01", "classZh": "直接血管扩张剂", "timeCourse": [time_course(route="intravenous", formulation="infusion", onset=(0.5, 2, "min"), duration=(1, 10, "min"), half_life=(2, 2, "min"))], "roas": [dose("INTRAVENOUS", "mcg/kg/min", 0.1, 0.5, 3, 10)]},
    # C03 利尿剂
    {"name": "Amiloride", "cn": "阿米洛利", "atc": "C03DB01", "classZh": "保钾利尿剂", "timeCourse": [time_course(tmax=(3, 4, "h"), half_life=(6, 9, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 2.5, 5, 10, 20)]},
    {"name": "Triamterene", "cn": "氨苯蝶啶", "atc": "C03DB02", "classZh": "保钾利尿剂", "timeCourse": [time_course(tmax=(2, 4, "h"), half_life=(2, 4, "h"), duration=(7, 9, "h"))], "roas": [dose("ORAL", "mg", 25, 50, 100, 200)]},
    {"name": "Metolazone", "cn": "美托拉宗", "atc": "C03BA08", "classZh": "噻嗪样利尿剂", "timeCourse": [time_course(tmax=(2, 4, "h"), half_life=(8, 14, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 2.5, 5, 10, 20)]},
    {"name": "Ethacrynic Acid", "cn": "依他尼酸", "atc": "C03CC01", "classZh": "袢利尿剂", "timeCourse": [time_course(tmax=(1, 2, "h"), half_life=(1, 4, "h"), duration=(6, 8, "h"))], "roas": [dose("ORAL", "mg", 25, 50, 100, 200)]},
    {"name": "Chlorothiazide", "cn": "氯噻嗪", "atc": "C03AA04", "classZh": "噻嗪类利尿剂", "timeCourse": [time_course(tmax=(2, 4, "h"), half_life=(1, 2, "h"), duration=(6, 12, "h"))], "roas": [dose("ORAL", "mg", 250, 500, 1000, 2000)]},
    {"name": "Bendroflumethiazide", "cn": "苄氟噻嗪", "atc": "C03AA01", "classZh": "噻嗪类利尿剂", "timeCourse": [time_course(tmax=(2, 4, "h"), half_life=(3, 8, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 1.25, 2.5, 5, 10)]},
    {"name": "Xipamide", "cn": "希帕胺", "atc": "C03BA10", "classZh": "噻嗪样利尿剂", "timeCourse": [time_course(tmax=(1, 2, "h"), half_life=(6, 8, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 10, 20, 40, 80)]},
    {"name": "Canrenone", "cn": "坎利酮", "atc": "C03DA02", "classZh": "醛固酮拮抗剂", "timeCourse": [time_course(tmax=(2, 4, "h"), half_life=(10, 22, "h"), duration=(24, 48, "h"))], "roas": [dose("ORAL", "mg", 25, 50, 100, 200)]},
    {"name": "Finerenone", "cn": "非奈利酮", "atc": "C03DA05", "classZh": "非甾体盐皮质激素受体拮抗剂", "timeCourse": [time_course(tmax=(0.5, 1.25, "h"), half_life=(2, 3, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 10, 20, 40, 80)]},
    {"name": "Tolvaptan", "cn": "托伐普坦", "atc": "C03XA01", "classZh": "加压素V2受体拮抗剂/利尿剂", "timeCourse": [time_course(tmax=(2, 4, "h"), half_life=(3, 12, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 7.5, 15, 30, 60)]},
    # C04 外周血管扩张剂
    {"name": "Pentoxifylline", "cn": "己酮可可碱", "atc": "C04AD03", "classZh": "外周血管扩张剂/血液流变改善药", "timeCourse": [time_course(tmax=(2, 4, "h"), half_life=(0.4, 0.8, "h"), duration=(8, 12, "h"))], "roas": [dose("ORAL", "mg", 200, 400, 800, 1200)]},
    {"name": "Naftidrofuryl", "cn": "萘呋胺", "atc": "C04AX21", "classZh": "外周血管扩张剂", "timeCourse": [time_course(tmax=(2, 3, "h"), half_life=(1, 2, "h"), duration=(6, 8, "h"))], "roas": [dose("ORAL", "mg", 100, 200, 400, 600)]},
    {"name": "Nicergoline", "cn": "尼麦角林", "atc": "C04AE02", "classZh": "麦角生物碱类血管活性药", "timeCourse": [time_course(tmax=(1, 1.5, "h"), half_life=(2, 4, "h"), duration=(8, 12, "h"))], "roas": [dose("ORAL", "mg", 5, 10, 30, 60)]},
    {"name": "Phentolamine", "cn": "酚妥拉明", "atc": "C04AB01", "classZh": "α受体阻滞性血管扩张剂", "timeCourse": [time_course(route="intravenous", formulation="injection", onset=(1, 2, "min"), duration=(10, 30, "min"), half_life=(0.3, 0.5, "h"))], "roas": [dose("INTRAVENOUS", "mg", 1, 5, 10, 20)]},
    {"name": "Nicotinyl Alcohol", "cn": "烟醇", "atc": "C04AC02", "classZh": "烟酸衍生物类外周血管扩张剂", "aliases": ["Nicotinic acid derivative", "Roniacol"], "timeCourse": [time_course(duration=(4, 6, "h"), notes=["未找到可靠现代人体血浆 Tmax/半衰期标签资料；此处仅保留外周血管扩张药效时间索引。"])], "roas": [dose("ORAL", "mg", 25, 50, 100, 200)]},
    # C05 血管保护药
    {"name": "Diosmin", "cn": "地奥司明", "atc": "C05CA03", "classZh": "生物类黄酮血管保护药", "timeCourse": [time_course(tmax=(1, 5, "h"), half_life=(11, 11, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 250, 500, 1000, 2000)]},
    {"name": "Hesperidin", "cn": "橙皮苷", "atc": "C05CA53", "classZh": "生物类黄酮血管保护药", "timeCourse": [time_course(tmax=(5, 7, "h"), half_life=(3, 6, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 50, 100, 500, 1000)]},
    {"name": "Troxerutin", "cn": "曲克芦丁", "atc": "C05CA04", "classZh": "毛细血管稳定剂", "timeCourse": [time_course(tmax=(2, 3, "h"), half_life=(8, 12, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 300, 600, 900, 1800)]},
    {"name": "Calcium Dobesilate", "cn": "羟苯磺酸钙", "atc": "C05BX01", "classZh": "血管保护药", "timeCourse": [time_course(tmax=(6, 6, "h"), half_life=(5, 5, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 250, 500, 1000, 1500)]},
    {"name": "Rutoside", "cn": "芦丁", "atc": "C05CA01", "classZh": "生物类黄酮血管保护药", "timeCourse": [time_course(tmax=(2, 6, "h"), half_life=(8, 12, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 50, 100, 300, 600)]},
    # C07 β 受体阻滞剂
    {"name": "Acebutolol", "cn": "醋丁洛尔", "atc": "C07AB04", "classZh": "β1选择性阻滞剂", "timeCourse": [time_course(tmax=(2, 4, "h"), half_life=(3, 4, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 100, 200, 400, 1200)]},
    {"name": "Betaxolol", "cn": "倍他洛尔", "atc": "C07AB05", "classZh": "β1选择性阻滞剂", "timeCourse": [time_course(tmax=(2, 4, "h"), half_life=(14, 22, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 5, 10, 20, 40)]},
    {"name": "Nadolol", "cn": "纳多洛尔", "atc": "C07AA12", "classZh": "非选择性β受体阻滞剂", "timeCourse": [time_course(tmax=(3, 4, "h"), half_life=(20, 24, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 20, 40, 160, 320)]},
    {"name": "Pindolol", "cn": "吲哚洛尔", "atc": "C07AA03", "classZh": "非选择性β受体阻滞剂", "timeCourse": [time_course(tmax=(1, 2, "h"), half_life=(3, 4, "h"), duration=(8, 12, "h"))], "roas": [dose("ORAL", "mg", 2.5, 5, 15, 60)]},
    {"name": "Timolol", "cn": "噻吗洛尔", "atc": "C07AA06", "classZh": "非选择性β受体阻滞剂", "timeCourse": [time_course(tmax=(1, 2, "h"), half_life=(4, 5, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 5, 10, 30, 60)]},
    # C08 钙通道阻滞剂
    {"name": "Felodipine", "cn": "非洛地平", "atc": "C08CA02", "classZh": "二氢吡啶类钙通道阻滞剂", "timeCourse": [time_course(tmax=(2.5, 5, "h"), half_life=(11, 16, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 2.5, 5, 10, 20)]},
    {"name": "Isradipine", "cn": "伊拉地平", "atc": "C08CA03", "classZh": "二氢吡啶类钙通道阻滞剂", "timeCourse": [time_course(tmax=(1, 1.5, "h"), half_life=(8, 12, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 2.5, 5, 10, 20)]},
    {"name": "Nicardipine", "cn": "尼卡地平", "atc": "C08CA04", "classZh": "二氢吡啶类钙通道阻滞剂", "timeCourse": [time_course(route="oral", formulation="capsule/injection", tmax=(0.5, 2, "h"), half_life=(2, 4, "h"), duration=(4, 8, "h"))], "roas": [dose("ORAL", "mg", 20, 30, 60, 120)]},
    {"name": "Nimodipine", "cn": "尼莫地平", "atc": "C08CA06", "classZh": "二氢吡啶类钙通道阻滞剂", "timeCourse": [time_course(tmax=(0.5, 1, "h"), half_life=(8, 9, "h"), duration=(4, 6, "h"))], "roas": [dose("ORAL", "mg", 30, 60, 120, 360)]},
    {"name": "Lercanidipine", "cn": "乐卡地平", "atc": "C08CA13", "classZh": "二氢吡啶类钙通道阻滞剂", "timeCourse": [time_course(tmax=(1.5, 3, "h"), half_life=(8, 10, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 5, 10, 20, 40)]},
    {"name": "Clevidipine", "cn": "氯维地平", "atc": "C08CA16", "classZh": "静脉二氢吡啶类钙通道阻滞剂", "timeCourse": [time_course(route="intravenous", formulation="emulsion infusion", onset=(2, 4, "min"), duration=(5, 15, "min"), half_life=(1, 15, "min"))], "roas": [dose("INTRAVENOUS", "mg/h", 1, 2, 16, 32)]},
    {"name": "Nisoldipine", "cn": "尼索地平", "atc": "C08CA07", "classZh": "二氢吡啶类钙通道阻滞剂", "timeCourse": [time_course(tmax=(6, 12, "h"), half_life=(7, 12, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 8.5, 17, 34, 68)]},
    # C09 肾素-血管紧张素系统药物
    {"name": "Perindopril", "cn": "培哚普利", "atc": "C09AA04", "classZh": "ACE抑制剂", "timeCourse": [time_course(tmax=(1, 2, "h"), half_life=(17, 17, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 2, 4, 8, 16)]},
    {"name": "Benazepril", "cn": "贝那普利", "atc": "C09AA07", "classZh": "ACE抑制剂", "timeCourse": [time_course(tmax=(0.5, 1, "h"), half_life=(10, 11, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 5, 10, 20, 40)]},
    {"name": "Fosinopril", "cn": "福辛普利", "atc": "C09AA09", "classZh": "ACE抑制剂", "timeCourse": [time_course(tmax=(3, 3, "h"), half_life=(11, 12, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 5, 10, 20, 40)]},
    {"name": "Quinapril", "cn": "喹那普利", "atc": "C09AA06", "classZh": "ACE抑制剂", "timeCourse": [time_course(tmax=(1, 2, "h"), half_life=(2, 3, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 5, 10, 20, 80)]},
    {"name": "Trandolapril", "cn": "群多普利", "atc": "C09AA10", "classZh": "ACE抑制剂", "timeCourse": [time_course(tmax=(4, 10, "h"), half_life=(16, 24, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 1, 2, 4, 8)]},
    {"name": "Azilsartan", "cn": "阿齐沙坦", "atc": "C09CA09", "classZh": "ARB 血管紧张素II受体拮抗剂", "timeCourse": [time_course(tmax=(1.5, 3, "h"), half_life=(11, 11, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 20, 40, 80, 160)]},
    {"name": "Sacubitril/Valsartan", "cn": "沙库巴曲/缬沙坦", "atc": "C09DX04", "classZh": "ARNI 血管紧张素受体-脑啡肽酶抑制剂", "aliases": ["Entresto", "ARNI"], "timeCourse": [time_course(tmax=(0.5, 2, "h"), half_life=(9, 11, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 24, 49, 97, 194)]},
    {"name": "Aliskiren", "cn": "阿利吉仑", "atc": "C09XA02", "classZh": "直接肾素抑制剂", "timeCourse": [time_course(tmax=(1, 3, "h"), half_life=(24, 40, "h"), steady=(5, 8, "day"))], "roas": [dose("ORAL", "mg", 75, 150, 300, 600)]},
    {"name": "Eprosartan", "cn": "依普罗沙坦", "atc": "C09CA02", "classZh": "ARB 血管紧张素II受体拮抗剂", "timeCourse": [time_course(tmax=(1, 2, "h"), half_life=(5, 9, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 300, 600, 800, 1200)]},
    {"name": "Moexipril", "cn": "莫昔普利", "atc": "C09AA13", "classZh": "ACE抑制剂", "timeCourse": [time_course(tmax=(1, 2, "h"), half_life=(2, 9, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 3.75, 7.5, 15, 30)]},
    # C10 调脂药
    {"name": "Evolocumab", "cn": "依洛尤单抗", "atc": "C10AX13", "classZh": "PCSK9抑制剂", "timeCourse": [time_course(route="subcutaneous", formulation="injection", tmax=(3, 4, "day"), half_life=(11, 17, "day"), duration=(14, 30, "day"))], "roas": [dose("SUBCUTANEOUS", "mg", 140, 140, 420, 840)]},
    {"name": "Alirocumab", "cn": "阿利西尤单抗", "atc": "C10AX14", "classZh": "PCSK9抑制剂", "timeCourse": [time_course(route="subcutaneous", formulation="injection", tmax=(3, 7, "day"), half_life=(17, 20, "day"), duration=(14, 30, "day"))], "roas": [dose("SUBCUTANEOUS", "mg", 75, 150, 300, 600)]},
    {"name": "Inclisiran", "cn": "英克司兰", "atc": "C10AX16", "classZh": "PCSK9 siRNA 调脂药", "timeCourse": [time_course(route="subcutaneous", formulation="injection", tmax=(4, 6, "h"), half_life=(9, 9, "h"), duration=(90, 180, "day"))], "roas": [dose("SUBCUTANEOUS", "mg", 284, 284, 568, 852)]},
    {"name": "Bempedoic Acid", "cn": "贝派地酸", "atc": "C10AX15", "classZh": "ATP柠檬酸裂解酶抑制剂", "timeCourse": [time_course(tmax=(3.5, 3.5, "h"), half_life=(21, 21, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 90, 180, 360, 540)]},
    {"name": "Fenofibrate", "cn": "非诺贝特", "atc": "C10AB05", "classZh": "贝特类调脂药", "timeCourse": [time_course(tmax=(6, 8, "h"), half_life=(20, 23, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 48, 145, 200, 300)]},
    {"name": "Gemfibrozil", "cn": "吉非罗齐", "atc": "C10AB04", "classZh": "贝特类调脂药", "timeCourse": [time_course(tmax=(1, 2, "h"), half_life=(1.5, 2, "h"), duration=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 300, 600, 1200, 2400)]},
    {"name": "Cholestyramine", "cn": "考来烯胺", "atc": "C10AC01", "classZh": "胆汁酸螯合剂", "timeCourse": [time_course(tmax=(None, None, "h"), duration=(12, 24, "h"), notes=["本药基本不系统吸收；曲线表示给药后胃肠道结合/药效索引，不代表血浆浓度。"])], "roas": [dose("ORAL", "g", 4, 8, 16, 24)]},
    {"name": "Colesevelam", "cn": "考来维仑", "atc": "C10AC04", "classZh": "胆汁酸螯合剂", "timeCourse": [time_course(tmax=(None, None, "h"), duration=(12, 24, "h"), notes=["本药基本不系统吸收；曲线表示肠道作用时间索引，不代表血浆浓度。"])], "roas": [dose("ORAL", "g", 1.875, 3.75, 4.375, 7.5)]},
    {"name": "Fluvastatin", "cn": "氟伐他汀", "atc": "C10AA04", "classZh": "他汀类 HMG-CoA还原酶抑制剂", "timeCourse": [time_course(tmax=(0.5, 1, "h"), half_life=(1, 3, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 20, 40, 80, 160)]},
    {"name": "Lovastatin", "cn": "洛伐他汀", "atc": "C10AA02", "classZh": "他汀类 HMG-CoA还原酶抑制剂", "timeCourse": [time_course(tmax=(2, 4, "h"), half_life=(2, 5, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 10, 20, 40, 80)]},
    {"name": "Pitavastatin", "cn": "匹伐他汀", "atc": "C10AA08", "classZh": "他汀类 HMG-CoA还原酶抑制剂", "timeCourse": [time_course(tmax=(1, 2, "h"), half_life=(12, 12, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 1, 2, 4, 8)]},
    # B01 抗血栓药物
    {"name": "Heparin", "cn": "肝素", "atc": "B01AB01", "classZh": "普通肝素抗凝药", "timeCourse": [time_course(route="intravenous/subcutaneous", formulation="injection", onset=(1, 60, "min"), duration=(2, 6, "h"), half_life=(0.5, 2, "h"))], "roas": [dose("INTRAVENOUS", "units", 1000, 5000, 10000, 30000)]},
    {"name": "Dalteparin", "cn": "达肝素", "atc": "B01AB04", "classZh": "低分子肝素", "timeCourse": [time_course(route="subcutaneous", formulation="injection", tmax=(3, 5, "h"), half_life=(3, 5, "h"), duration=(12, 24, "h"))], "roas": [dose("SUBCUTANEOUS", "IU", 2500, 5000, 10000, 20000)]},
    {"name": "Fondaparinux", "cn": "磺达肝癸钠", "atc": "B01AX05", "classZh": "选择性Xa因子抑制剂", "timeCourse": [time_course(route="subcutaneous", formulation="injection", tmax=(2, 3, "h"), half_life=(17, 21, "h"), duration=(24, 24, "h"))], "roas": [dose("SUBCUTANEOUS", "mg", 2.5, 5, 7.5, 10)]},
    {"name": "Aspirin", "cn": "阿司匹林", "atc": "B01AC06", "classZh": "抗血小板药", "aliases": ["Acetylsalicylic acid", "ASA"], "timeCourse": [time_course(tmax=(0.5, 2, "h"), peak=(1, 2, "h"), duration=(5, 10, "day"), half_life=(0.25, 0.33, "h"), notes=["抗血小板作用持续时间与血小板更新相关，不等同水杨酸血浆半衰期。"])], "roas": [dose("ORAL", "mg", 75, 81, 325, 1000)]},
    {"name": "Ticlopidine", "cn": "噻氯匹定", "atc": "B01AC05", "classZh": "P2Y12受体抑制剂", "timeCourse": [time_course(tmax=(2, 2, "h"), peak=(3, 5, "day"), duration=(5, 10, "day"), half_life=(12, 24, "h"))], "roas": [dose("ORAL", "mg", 125, 250, 500, 1000)]},
    {"name": "Dipyridamole", "cn": "双嘧达莫", "atc": "B01AC07", "classZh": "抗血小板/磷酸二酯酶抑制剂", "timeCourse": [time_course(tmax=(2, 3, "h"), half_life=(10, 12, "h"), duration=(8, 12, "h"))], "roas": [dose("ORAL", "mg", 25, 75, 200, 400)]},
    {"name": "Tirofiban", "cn": "替罗非班", "atc": "B01AC17", "classZh": "GPIIb/IIIa受体拮抗剂", "timeCourse": [time_course(route="intravenous", formulation="infusion", onset=(5, 10, "min"), duration=(4, 8, "h"), half_life=(2, 2, "h"))], "roas": [dose("INTRAVENOUS", "mcg/kg/min", 0.05, 0.1, 0.15, 0.3)]},
    {"name": "Eptifibatide", "cn": "依替巴肽", "atc": "B01AC16", "classZh": "GPIIb/IIIa受体拮抗剂", "timeCourse": [time_course(route="intravenous", formulation="bolus/infusion", onset=(5, 10, "min"), duration=(4, 8, "h"), half_life=(2.5, 2.5, "h"))], "roas": [dose("INTRAVENOUS", "mcg/kg/min", 0.5, 1, 2, 4)]},
    {"name": "Abciximab", "cn": "阿昔单抗", "atc": "B01AC13", "classZh": "GPIIb/IIIa受体拮抗剂", "timeCourse": [time_course(route="intravenous", formulation="bolus/infusion", onset=(10, 10, "min"), duration=(24, 48, "h"), half_life=(0.5, 0.5, "h"))], "roas": [dose("INTRAVENOUS", "mg", 5, 10, 20, 40)]},
    {"name": "Cangrelor", "cn": "坎格雷洛", "atc": "B01AC25", "classZh": "静脉P2Y12受体抑制剂", "timeCourse": [time_course(route="intravenous", formulation="infusion", onset=(2, 2, "min"), duration=(30, 60, "min"), half_life=(3, 6, "min"))], "roas": [dose("INTRAVENOUS", "mcg/kg/min", 1, 4, 8, 16)]},
    {"name": "Alteplase", "cn": "阿替普酶", "atc": "B01AD02", "classZh": "纤溶酶原激活剂/溶栓药", "timeCourse": [time_course(route="intravenous", formulation="infusion", onset=(5, 30, "min"), duration=(1, 2, "h"), half_life=(4, 8, "min"))], "roas": [dose("INTRAVENOUS", "mg", 10, 50, 90, 100)]},
    {"name": "Tenecteplase", "cn": "替奈普酶", "atc": "B01AD11", "classZh": "纤溶酶原激活剂/溶栓药", "timeCourse": [time_course(route="intravenous", formulation="bolus", onset=(5, 30, "min"), duration=(1, 2, "h"), half_life=(20, 24, "min"))], "roas": [dose("INTRAVENOUS", "mg", 30, 40, 50, 60)]},
    {"name": "Argatroban", "cn": "阿加曲班", "atc": "B01AE03", "classZh": "直接凝血酶抑制剂", "timeCourse": [time_course(route="intravenous", formulation="infusion", onset=(1, 3, "h"), duration=(2, 4, "h"), half_life=(39, 51, "min"))], "roas": [dose("INTRAVENOUS", "mcg/kg/min", 0.5, 1, 2, 10)]},
    {"name": "Bivalirudin", "cn": "比伐芦定", "atc": "B01AE06", "classZh": "直接凝血酶抑制剂", "timeCourse": [time_course(route="intravenous", formulation="bolus/infusion", onset=(5, 5, "min"), duration=(1, 2, "h"), half_life=(25, 25, "min"))], "roas": [dose("INTRAVENOUS", "mg/kg/h", 0.25, 1.75, 2.5, 5)]},
    {"name": "Vorapaxar", "cn": "沃拉帕沙", "atc": "B01AC26", "classZh": "PAR-1受体拮抗剂", "timeCourse": [time_course(tmax=(1, 2, "h"), half_life=(5, 13, "day"), duration=(14, 28, "day"))], "roas": [dose("ORAL", "mg", 1.08, 2.08, 4.16, 8.32)]},
    {"name": "Betrixaban", "cn": "贝曲沙班", "atc": "B01AF04", "classZh": "直接Xa因子抑制剂", "timeCourse": [time_course(tmax=(3, 4, "h"), half_life=(19, 27, "h"), duration=(24, 24, "h"))], "roas": [dose("ORAL", "mg", 40, 80, 160, 320)]},
    {"name": "Acenocoumarol", "cn": "醋硝香豆素", "atc": "B01AA07", "classZh": "维生素K拮抗剂", "timeCourse": [time_course(tmax=(1, 3, "h"), peak=(24, 48, "h"), duration=(2, 4, "day"), half_life=(8, 11, "h"))], "roas": [dose("ORAL", "mg", 1, 2, 4, 8)]},
    {"name": "Phenprocoumon", "cn": "苯丙香豆素", "atc": "B01AA04", "classZh": "维生素K拮抗剂", "timeCourse": [time_course(tmax=(1, 3, "h"), peak=(48, 96, "h"), duration=(5, 14, "day"), half_life=(110, 130, "h"))], "roas": [dose("ORAL", "mg", 1, 3, 6, 12)]},
]


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, data: dict) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as file:
        json.dump(data, file, ensure_ascii=False, indent=2)
        file.write("\n")


def add_atc_tags(substance: dict) -> None:
    clinical = substance.get("clinicalInfo")
    codes = clinical.get("atcCodes", []) if isinstance(clinical, dict) else []
    matching_groups = [code[:3] for code in codes if code[:3] in GROUPS]
    if not matching_groups:
        return

    categories = substance.setdefault("categories", [])
    common_names = substance.setdefault("commonNames", [])
    for group_code in matching_groups:
        slug, zh, en = GROUPS[group_code]
        categories.append(slug)
        common_names.extend([zh, en, group_code, slug.upper()])
        if group_code == "B01":
            categories.append("antithrombotic")
        if group_code == "C04":
            categories.append("peripheral-circulation")
        if group_code.startswith("C"):
            categories.append("cardiovascular")
        if isinstance(clinical, dict):
            clinical.setdefault("drugClass", []).extend([zh, en])

    categories.append("prescription-medicine")
    substance["categories"] = ordered_unique(categories)
    substance["commonNames"] = ordered_unique(common_names)
    if substance.get("roas"):
        substance["dosageRemark"] = DOSE_REMARK
    if isinstance(clinical, dict):
        clinical["drugClass"] = ordered_unique(clinical.get("drugClass", []))


def retag_existing_sources() -> set[str]:
    existing_names = set()
    for path in sorted(SOURCE_DIR.glob("*.json")):
        if path.name == OUTPUT_FILE.name:
            continue
        data = load_json(path)
        changed = False
        for substance in data.get("substances", []):
            name = substance.get("name")
            if name:
                existing_names.add(name)
            before = json.dumps(substance, sort_keys=True, ensure_ascii=False)
            add_atc_tags(substance)
            after = json.dumps(substance, sort_keys=True, ensure_ascii=False)
            changed = changed or before != after
        if changed:
            write_json(path, data)
    return existing_names


def main() -> None:
    existing_names = retag_existing_sources()
    incoming = [build_entry(item) for item in DRUGS if item["name"] not in existing_names]
    write_json(OUTPUT_FILE, {"categories": ATC_CATEGORY_DEFINITIONS, "substances": incoming})
    print(f"Wrote {len(incoming)} new ATC cardiovascular/antithrombotic substances to {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
