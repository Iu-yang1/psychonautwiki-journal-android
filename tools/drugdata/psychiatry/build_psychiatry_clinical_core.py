#!/usr/bin/env python3
import json
from pathlib import Path


ACCESSED_DATE = "2026-06-27"
OUTPUT_PATH = Path("tools/drugdata/psychiatry/psychiatry_clinical_core.json")


SOURCE_ATC_N05 = {
    "title": "WHO ATC/DDD Index N05 Psycholeptics",
    "url": "https://atcddd.fhi.no/atc_ddd_index/?code=N05&showdescription=yes",
    "sourceType": "classification",
    "accessedDate": ACCESSED_DATE,
    "evidenceLevel": "REFERENCE_DATABASE",
}
SOURCE_ATC_N06 = {
    "title": "WHO ATC/DDD Index N06 Psychoanaleptics",
    "url": "https://atcddd.fhi.no/atc_ddd_index/?code=N06&showdescription=yes",
    "sourceType": "classification",
    "accessedDate": ACCESSED_DATE,
    "evidenceLevel": "REFERENCE_DATABASE",
}
SOURCE_NEML_CN = {
    "title": "国家基本药物目录 2018",
    "url": "https://www.nhc.gov.cn/wjw/jbywml/201810/8b68d28bd3754898b339e06da8c7d907/files/1733375109455_35909.pdf",
    "sourceType": "national-essential-medicines-list",
    "accessedDate": ACCESSED_DATE,
    "evidenceLevel": "REFERENCE_DATABASE",
}


GROUPS = {
    "antipsychotic": {
        "categories": ["clinical-psychiatry", "antipsychotic", "prescription-medicine"],
        "drugClass": ["Antipsychotic"],
        "indications": ["Schizophrenia spectrum disorders", "Bipolar disorder or agitation contexts depending on local labeling"],
        "monitoring": ["EPS/akathisia", "Sedation", "Weight/BMI", "Glucose/HbA1c", "Lipids", "Blood pressure", "QT/ECG when clinically indicated"],
        "warnings": ["Extrapyramidal symptoms, metabolic effects, sedation, QT prolongation, and neuroleptic malignant syndrome are class-relevant safety concerns."],
        "sourceRefs": [SOURCE_ATC_N05, SOURCE_NEML_CN],
    },
    "antidepressant": {
        "categories": ["clinical-psychiatry", "antidepressant", "prescription-medicine"],
        "drugClass": ["Antidepressant"],
        "indications": ["Major depressive disorder and anxiety-related indications depending on local labeling"],
        "monitoring": ["Mood symptoms", "Suicidality especially early in treatment", "Activation/mania switch", "Serotonin syndrome symptoms", "Hyponatremia risk when clinically indicated"],
        "warnings": ["Do not combine serotonergic medicines or MAOIs without label-specific review; monitor for activation, suicidality, and serotonin toxicity."],
        "sourceRefs": [SOURCE_ATC_N06, SOURCE_NEML_CN],
    },
    "mood-stabilizer": {
        "categories": ["clinical-psychiatry", "mood-stabilizer", "prescription-medicine"],
        "drugClass": ["Mood stabilizer", "Antiepileptic used in psychiatric contexts"],
        "indications": ["Bipolar disorder and seizure-related indications depending on local labeling"],
        "monitoring": ["Mood symptoms", "CBC when clinically indicated", "Liver function where relevant", "Renal/thyroid for lithium", "Pregnancy risk review"],
        "warnings": ["Therapeutic monitoring and organ-specific safety checks are drug-specific and must follow local labeling."],
        "sourceRefs": [SOURCE_NEML_CN],
    },
    "anxiolytic-hypnotic": {
        "categories": ["clinical-psychiatry", "anxiolytic", "sedative-hypnotic", "prescription-medicine"],
        "drugClass": ["Anxiolytic or hypnotic"],
        "indications": ["Anxiety or insomnia-related indications depending on local labeling"],
        "monitoring": ["Sedation", "Falls", "Respiratory depression risk", "Dependence/withdrawal risk", "Cognitive impairment"],
        "warnings": ["Sedatives can impair driving and respiration; avoid combining with alcohol, opioids, or other CNS depressants unless specifically supervised."],
        "sourceRefs": [SOURCE_ATC_N05, SOURCE_NEML_CN],
    },
    "adhd": {
        "categories": ["clinical-psychiatry", "adhd-medication", "prescription-medicine"],
        "drugClass": ["ADHD medication"],
        "indications": ["Attention-deficit/hyperactivity disorder depending on local labeling"],
        "monitoring": ["Blood pressure", "Heart rate", "Sleep", "Appetite/weight", "Misuse/diversion risk for stimulants"],
        "warnings": ["Cardiovascular, psychiatric activation, sleep, appetite, and misuse risks are formulation- and patient-specific."],
        "sourceRefs": [SOURCE_ATC_N06],
    },
    "cognitive": {
        "categories": ["clinical-psychiatry", "cognitive-disorder-medication", "prescription-medicine"],
        "drugClass": ["Anti-dementia medicine"],
        "indications": ["Alzheimer disease or cognitive-disorder indications depending on local labeling"],
        "monitoring": ["Cognition/function", "Heart rate/syncope risk", "GI adverse effects", "Weight"],
        "warnings": ["Cholinesterase inhibitors can cause bradycardia, syncope, GI effects, and weight loss; memantine interpretation differs from recreational NMDA-antagonist use."],
        "sourceRefs": [SOURCE_ATC_N06],
    },
    "substance-use": {
        "categories": ["clinical-psychiatry", "substance-use-disorder-treatment", "prescription-medicine"],
        "drugClass": ["Substance-use-disorder treatment"],
        "indications": ["Alcohol, opioid, nicotine, or other substance-use-disorder contexts depending on local labeling"],
        "monitoring": ["Substance use outcomes", "Withdrawal symptoms", "Liver function where relevant", "Adherence", "Overdose risk counseling"],
        "warnings": ["Indication, withdrawal state, opioid exposure, hepatic function, and behavioral supports change interpretation."],
        "sourceRefs": [SOURCE_NEML_CN],
    },
    "gabapentinoid": {
        "categories": ["clinical-psychiatry", "gabapentinoid", "prescription-medicine"],
        "drugClass": ["Gabapentinoid"],
        "indications": ["Neurologic indications and selected psychiatric-adjacent contexts depending on local labeling"],
        "monitoring": ["Sedation", "Dizziness", "Falls", "Misuse risk", "Renal function for dose context"],
        "warnings": ["CNS depression, respiratory depression risk with opioids or sedatives, and misuse potential require context-specific review."],
        "sourceRefs": [SOURCE_NEML_CN],
    },
    "ketamine": {
        "categories": ["clinical-psychiatry", "prescription-medicine"],
        "drugClass": ["NMDA receptor antagonist", "Anesthetic", "Treatment-resistant depression context under supervised protocols"],
        "indications": ["Anesthesia label context; treatment-resistant depression protocols depend on local approval and specialist supervision"],
        "monitoring": ["Blood pressure", "Dissociation/sedation", "Respiratory status", "Misuse risk", "Urinary symptoms with repeated exposure"],
        "warnings": ["Psychiatric use requires protocol-driven specialist supervision; recreational dissociative use must not be inferred from this index."],
        "sourceRefs": [SOURCE_NEML_CN],
    },
    "nac": {
        "categories": ["clinical-psychiatry", "prescription-medicine"],
        "drugClass": ["Mucolytic/antidote with psychiatric research contexts"],
        "indications": ["Acetaminophen poisoning and respiratory indications; psychiatric uses remain context-dependent"],
        "monitoring": ["Indication-specific clinical response", "GI effects", "Hypersensitivity reactions"],
        "warnings": ["Psychiatric evidence and use context are indication-specific; do not infer a treatment recommendation from indexing."],
        "sourceRefs": [SOURCE_NEML_CN],
    },
}


DRUGS = [
    # Antipsychotics
    ("Chlorpromazine", ["氯丙嗪", "冬眠灵"], "antipsychotic", ["N05AA01"]),
    ("Clozapine", ["氯氮平"], "antipsychotic", ["N05AH02"]),
    ("Olanzapine", ["奥氮平", "再普乐"], "antipsychotic", ["N05AH03"]),
    ("Aripiprazole", ["阿立哌唑", "安律凡"], "antipsychotic", ["N05AX12"]),
    ("Paliperidone", ["帕利哌酮"], "antipsychotic", ["N05AX13"]),
    ("Ziprasidone", ["齐拉西酮"], "antipsychotic", ["N05AE04"]),
    ("Lurasidone", ["鲁拉西酮"], "antipsychotic", ["N05AE05"]),
    ("Amisulpride", ["氨磺必利"], "antipsychotic", ["N05AL05"]),
    ("Sulpiride", ["舒必利"], "antipsychotic", ["N05AL01"]),
    ("Perphenazine", ["奋乃静"], "antipsychotic", ["N05AB03"]),
    ("Fluphenazine", ["氟奋乃静"], "antipsychotic", ["N05AB02"]),
    ("Trifluoperazine", ["三氟拉嗪"], "antipsychotic", ["N05AB06"]),
    ("Thioridazine", ["硫利达嗪"], "antipsychotic", ["N05AC02"]),
    ("Droperidol", ["氟哌利多"], "antipsychotic", ["N05AD08"]),
    ("Pimozide", ["匹莫齐特"], "antipsychotic", ["N05AG02"]),
    ("Flupentixol", ["氟哌噻吨"], "antipsychotic", ["N05AF01"]),
    ("Zuclopenthixol", ["珠氯噻醇"], "antipsychotic", ["N05AF05"]),
    ("Loxapine", ["洛沙平"], "antipsychotic", ["N05AH01"]),
    ("Cariprazine", ["卡利拉嗪"], "antipsychotic", ["N05AX15"]),
    ("Brexpiprazole", ["布瑞哌唑"], "antipsychotic", ["N05AX16"]),
    ("Asenapine", ["阿塞那平"], "antipsychotic", ["N05AH05"]),
    ("Pimavanserin", ["匹莫范色林"], "antipsychotic", ["N05AX17"]),
    # Antidepressants
    ("Sertraline", ["舍曲林", "左洛复"], "antidepressant", ["N06AB06"]),
    ("Fluoxetine", ["氟西汀", "百忧解"], "antidepressant", ["N06AB03"]),
    ("Paroxetine", ["帕罗西汀", "赛乐特"], "antidepressant", ["N06AB05"]),
    ("Escitalopram", ["艾司西酞普兰", "来士普"], "antidepressant", ["N06AB10"]),
    ("Citalopram", ["西酞普兰"], "antidepressant", ["N06AB04"]),
    ("Fluvoxamine", ["氟伏沙明"], "antidepressant", ["N06AB08"]),
    ("Venlafaxine", ["文拉法辛", "怡诺思"], "antidepressant", ["N06AX16"]),
    ("Desvenlafaxine", ["去甲文拉法辛"], "antidepressant", ["N06AX23"]),
    ("Duloxetine", ["度洛西汀", "欣百达"], "antidepressant", ["N06AX21"]),
    ("Milnacipran", ["米那普仑"], "antidepressant", ["N06AX17"]),
    ("Bupropion", ["安非他酮"], "antidepressant", ["N06AX12"]),
    ("Trazodone", ["曲唑酮"], "antidepressant", ["N06AX05"]),
    ("Vortioxetine", ["伏硫西汀"], "antidepressant", ["N06AX26"]),
    ("Vilazodone", ["维拉佐酮"], "antidepressant", ["N06AX24"]),
    ("Amitriptyline", ["阿米替林"], "antidepressant", ["N06AA09"]),
    ("Clomipramine", ["氯米帕明"], "antidepressant", ["N06AA04"]),
    ("Imipramine", ["丙米嗪"], "antidepressant", ["N06AA02"]),
    ("Nortriptyline", ["去甲替林"], "antidepressant", ["N06AA10"]),
    ("Doxepin", ["多塞平"], "antidepressant", ["N06AA12"]),
    ("Maprotiline", ["马普替林"], "antidepressant", ["N06AA21"]),
    ("Moclobemide", ["吗氯贝胺"], "antidepressant", ["N06AG02"]),
    ("Phenelzine", ["苯乙肼"], "antidepressant", ["N06AF03"]),
    ("Tranylcypromine", ["反苯环丙胺"], "antidepressant", ["N06AF04"]),
    ("Agomelatine", ["阿戈美拉汀"], "antidepressant", ["N06AX22"]),
    # Mood stabilizers
    ("Lithium Carbonate", ["Lithium", "碳酸锂", "锂盐"], "mood-stabilizer", ["N05AN01"]),
    ("Valproate", ["Valproic acid", "Sodium valproate", "丙戊酸", "丙戊酸钠"], "mood-stabilizer", ["N03AG01"]),
    ("Divalproex", ["双丙戊酸钠"], "mood-stabilizer", ["N03AG01"]),
    ("Carbamazepine", ["卡马西平"], "mood-stabilizer", ["N03AF01"]),
    ("Oxcarbazepine", ["奥卡西平"], "mood-stabilizer", ["N03AF02"]),
    ("Lamotrigine", ["拉莫三嗪"], "mood-stabilizer", ["N03AX09"]),
    # Anxiolytics/hypnotics
    ("Hydroxyzine", ["羟嗪"], "anxiolytic-hypnotic", ["N05BB01"]),
    ("Nitrazepam", ["硝西泮"], "anxiolytic-hypnotic", ["N05CD02"]),
    ("Estazolam", ["艾司唑仑"], "anxiolytic-hypnotic", ["N05CD04"]),
    ("Lormetazepam", ["劳美西泮"], "anxiolytic-hypnotic", ["N05CD06"]),
    ("Flurazepam", ["氟西泮"], "anxiolytic-hypnotic", ["N05CD01"]),
    ("Clorazepate", ["氯拉卓酸"], "anxiolytic-hypnotic", ["N05BA05"]),
    ("Prazepam", ["普拉西泮"], "anxiolytic-hypnotic", ["N05BA11"]),
    ("Clobazam", ["氯巴占"], "anxiolytic-hypnotic", ["N05BA09"]),
    ("Tofisopam", ["托非索泮"], "anxiolytic-hypnotic", ["N05BA23"]),
    ("Zaleplon", ["扎来普隆"], "anxiolytic-hypnotic", ["N05CF03"]),
    ("Ramelteon", ["雷美替胺"], "anxiolytic-hypnotic", ["N05CH02"]),
    ("Suvorexant", ["苏沃雷生"], "anxiolytic-hypnotic", ["N05CJ01"]),
    ("Lemborexant", ["莱博雷生"], "anxiolytic-hypnotic", ["N05CJ02"]),
    ("Daridorexant", ["达利雷生"], "anxiolytic-hypnotic", ["N05CJ03"]),
    # ADHD/wakefulness
    ("Atomoxetine", ["托莫西汀"], "adhd", ["N06BA09"]),
    ("Guanfacine", ["胍法辛"], "adhd", ["C02AC02"]),
    ("Dexmethylphenidate", ["右哌甲酯"], "adhd", ["N06BA04"]),
    ("Viloxazine", ["维洛沙秦"], "adhd", ["N06AX09"]),
    ("Pitolisant", ["匹托利生"], "adhd", ["N07XX11"]),
    ("Solriamfetol", ["索利安非托"], "adhd", ["N06BA14"]),
    # Cognitive disorders
    ("Donepezil", ["多奈哌齐"], "cognitive", ["N06DA02"]),
    ("Rivastigmine", ["利斯的明", "卡巴拉汀"], "cognitive", ["N06DA03"]),
    # Substance-use-disorder
    ("Naltrexone", ["纳曲酮"], "substance-use", ["N07BB04"]),
    ("Acamprosate", ["阿坎酸"], "substance-use", ["N07BB03"]),
    ("Varenicline", ["伐尼克兰"], "substance-use", ["N07BA03"]),
    ("Disulfiram", ["双硫仑"], "substance-use", ["N07BB01"]),
]

EXISTING_PATCHES = [
    ("Haloperidol", [], "antipsychotic", ["N05AD01"]),
    ("Prochlorperazine", [], "antipsychotic", ["N05AB04"]),
    ("Quetiapine", [], "antipsychotic", ["N05AH04"]),
    ("Risperidone", [], "antipsychotic", ["N05AX08"]),
    ("Mirtazapine", [], "antidepressant", ["N06AX11"]),
    ("Tianeptine", [], "antidepressant", ["N06AX14"]),
    ("Buspirone", [], "anxiolytic-hypnotic", ["N05BE01"]),
    ("Alprazolam", [], "anxiolytic-hypnotic", ["N05BA12"]),
    ("Bromazepam", [], "anxiolytic-hypnotic", ["N05BA08"]),
    ("Chlordiazepoxide", [], "anxiolytic-hypnotic", ["N05BA02"]),
    ("Clonazepam", [], "anxiolytic-hypnotic", ["N03AE01"]),
    ("Diazepam", [], "anxiolytic-hypnotic", ["N05BA01"]),
    ("Lorazepam", [], "anxiolytic-hypnotic", ["N05BA06"]),
    ("Midazolam", [], "anxiolytic-hypnotic", ["N05CD08"]),
    ("Oxazepam", [], "anxiolytic-hypnotic", ["N05BA04"]),
    ("Flunitrazepam", [], "anxiolytic-hypnotic", ["N05CD03"]),
    ("Temazepam", [], "anxiolytic-hypnotic", ["N05CD07"]),
    ("Triazolam", [], "anxiolytic-hypnotic", ["N05CD05"]),
    ("Etizolam", [], "anxiolytic-hypnotic", ["N05BA19"]),
    ("Phenobarbital", [], "anxiolytic-hypnotic", ["N03AA02"]),
    ("Pentobarbital", [], "anxiolytic-hypnotic", ["N05CA01"]),
    ("Zolpidem", [], "anxiolytic-hypnotic", ["N05CF02"]),
    ("Zopiclone", [], "anxiolytic-hypnotic", ["N05CF01"]),
    ("Eszopiclone", [], "anxiolytic-hypnotic", ["N05CF04"]),
    ("Melatonin", [], "anxiolytic-hypnotic", ["N05CH01"]),
    ("Amphetamine", [], "adhd", ["N06BA01"]),
    ("Dextroamphetamine", [], "adhd", ["N06BA02"]),
    ("Lisdexamfetamine", [], "adhd", ["N06BA12"]),
    ("Methylphenidate", [], "adhd", ["N06BA04"]),
    ("Modafinil", [], "adhd", ["N06BA07"]),
    ("Armodafinil", [], "adhd", ["N06BA13"]),
    ("Memantine", [], "cognitive", ["N06DX01"]),
    ("Galantamine", [], "cognitive", ["N06DA04"]),
    ("Gabapentin", [], "gabapentinoid", ["N03AX12"]),
    ("Pregabalin", [], "gabapentinoid", ["N03AX16"]),
    ("Methadone", [], "substance-use", ["N07BC02"]),
    ("Buprenorphine", [], "substance-use", ["N07BC01"]),
    ("Naloxone", [], "substance-use", ["V03AB15"]),
    ("N-Acetylcysteine", [], "nac", ["R05CB01", "V03AB23"]),
    ("Ketamine", [], "ketamine", ["N01AX03"]),
]


def dailymed_search(name: str) -> dict:
    query = name.replace(" ", "+")
    return {
        "title": f"DailyMed label search: {name}",
        "url": f"https://dailymed.nlm.nih.gov/dailymed/search.cfm?query={query}",
        "sourceType": "regulatory-label-search",
        "accessedDate": ACCESSED_DATE,
        "evidenceLevel": "REGULATORY_LABEL_SEARCH",
        "note": "Use the current product-specific label before entering numeric PK or regimen values.",
    }


def entry(name: str, aliases: list[str], group_key: str, atc_codes: list[str]) -> dict:
    group = GROUPS[group_key]
    source_refs = group["sourceRefs"] + [dailymed_search(name)]
    route = "oral"
    return {
        "name": name,
        "commonNames": [name] + aliases,
        "url": dailymed_search(name)["url"],
        "isApproved": True,
        "categories": group["categories"],
        "summary": f"{name} is indexed as a clinical psychiatry medicine. This entry is for clinical reference indexing and education; product-specific labeling should be checked before adding exact pharmacokinetic or regimen values.",
        "clinicalInfo": {
            "atcCodes": atc_codes,
            "drugClass": group["drugClass"],
            "indications": group["indications"],
            "contraindications": ["See current product-specific label and local approved labeling."],
            "majorWarnings": group["warnings"],
            "majorInteractions": ["Drug-specific CYP, QT, serotonergic, CNS depressant, or other interaction risks require product-specific label review."],
            "monitoring": group["monitoring"],
            "sourceRefs": source_refs,
        },
        "timeCourse": [
            {
                "route": route,
                "formulation": "product-specific",
                "notes": [
                    "Specific Tmax, half-life, duration, and steady-state values vary by formulation and product label.",
                    "Numeric graph values are intentionally left unfilled until product-specific labels are reviewed.",
                ],
                "sourceRefs": [dailymed_search(name)],
            }
        ],
        "tdm": {
            "isRoutinelyMonitored": group_key == "mood-stabilizer" and name in {"Lithium Carbonate", "Valproate", "Carbamazepine"},
            "monitoringType": "drug-specific clinical/laboratory monitoring; serum concentration only for selected medicines",
            "reason": "Psychiatry medicines are generally monitored by clinical response and safety labs; selected medicines such as lithium and some anticonvulsants may use serum concentration monitoring.",
            "pharmacokineticParametersAvailable": True,
            "interpretationCaveats": [
                "Monitoring type is drug-specific and should not be interpreted as universal TDM.",
                "Interpretation depends on indication, formulation, timing, organ function, comedications, and local laboratory method.",
            ],
            "sourceRefs": source_refs,
        },
        "doseUseReferences": [
            {
                "indication": group["indications"][0],
                "population": "Adults unless local label specifies otherwise",
                "route": route,
                "formulation": "product-specific",
                "amountText": "source needed",
                "scheduleText": "source needed",
                "ranges": [
                    {
                        "min": None,
                        "max": None,
                        "unit": "mg",
                        "basis": "unknown",
                        "frequency": "source needed",
                        "rangeKind": "source-needed",
                        "label": "Label/literature regimen",
                        "note": "No numeric regimen is shown until a current product-specific label or guideline is reviewed.",
                    }
                ],
                "sourceType": "regulatory-label-search",
                "evidenceLevel": "SOURCE_NEEDED",
                "note": "Dose use references are not recommendations and must not be used for self-medication or dose adjustment.",
                "sourceRefs": [],
            }
        ],
        "roas": [],
        "sourceRefs": source_refs,
    }


def patch(name: str, aliases: list[str], group_key: str, atc_codes: list[str]) -> dict:
    generated = entry(name, aliases, group_key, atc_codes)
    return {
        "name": name,
        "fieldsSet": {
            "clinicalInfo": generated["clinicalInfo"],
            "timeCourse": generated["timeCourse"],
            "tdm": generated["tdm"],
            "doseUseReferences": generated["doseUseReferences"],
            "sourceRefs": generated["sourceRefs"],
        },
    }


def main() -> None:
    data = {
        "substances": [entry(*drug) for drug in DRUGS],
        "substancePatches": [patch(*drug) for drug in EXISTING_PATCHES],
    }
    OUTPUT_PATH.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
