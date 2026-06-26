#!/usr/bin/env python3
import json
from pathlib import Path


ACCESSED_DATE = "2026-06-27"
OUTPUT_PATH = Path("tools/drugdata/psychiatry/psychiatry_clinical_core.json")
AUTO_LABEL_DATA_PATH = Path("tools/drugdata/psychiatry/openfda_psychiatry_label_data.json")


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

CURATED_LABEL_DATA = {
    "Sertraline": {
        "openfdaSetId": "00179766-980b-44b0-99d3-1fee2bb27e37",
        "timeCourse": {
            "tmax": (4.5, 8.4, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (26, 26, "h", "label pharmacokinetics"),
            "timeToSteadyState": (7, 7, "day", "label pharmacokinetics"),
        },
        "doseReference": ("Major depressive disorder / anxiety-related label contexts", "50 to 200 mg/day in cited label contexts", "once daily; dose changes no more often than weekly in the cited label", 50, 200),
        "doseBar": (25, 50, 150, 200),
    },
    "Fluoxetine": {
        "openfdaSetId": "02283de9-6087-45f7-a9ce-3b082ce860de",
        "timeCourse": {
            "tmax": (6, 8, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (4, 6, "day", "label pharmacokinetics", "Fluoxetine half-life after chronic administration; norfluoxetine half-life is longer."),
        },
        "doseReference": ("Major depressive disorder / OCD / bulimia / panic disorder label contexts", "20 to 80 mg/day across cited adult label contexts", "once daily or divided depending on product-specific label context", 20, 80),
        "doseBar": (10, 20, 40, 60),
    },
    "Paroxetine": {
        "openfdaSetId": "05ec70fd-4c65-495f-ba12-e8433a10b655",
        "timeCourse": {
            "tmax": (5.2, 5.2, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (21, 21, "h", "label pharmacokinetics"),
            "timeToSteadyState": (10, 10, "day", "label pharmacokinetics"),
        },
        "doseReference": ("MDD / OCD / panic disorder / PTSD / SAD / GAD label contexts", "10 to 60 mg/day across cited adult label contexts", "once daily in the morning in the cited label", 10, 60),
        "doseBar": (10, 20, 40, 60),
    },
    "Citalopram": {
        "openfdaSetId": "0109f365-bebd-4810-a56f-4451e10245db",
        "timeCourse": {
            "tmax": (4, 4, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (35, 35, "h", "label pharmacokinetics"),
            "timeToSteadyState": (7, 7, "day", "label pharmacokinetics"),
        },
        "doseReference": ("Major depressive disorder label context", "20 to 40 mg/day in cited label context", "once daily with or without food", 20, 40),
        "doseBar": (10, 20, 30, 40),
    },
    "Venlafaxine": {
        "openfdaSetId": "017a84aa-0e1f-f560-e063-6294a90a9069",
        "timeCourse": {
            "timeToSteadyState": (3, 3, "day", "label pharmacokinetics"),
        },
        "doseReference": ("Major depressive disorder immediate-release label context", "75 to 375 mg/day across cited label contexts", "two or three divided doses with food in the cited immediate-release label", 75, 375),
        "doseBar": (37.5, 75, 150, 225),
    },
    "Duloxetine": {
        "openfdaSetId": "00628e5e-4c5b-2573-e063-6294a90a0e3b",
        "timeCourse": {
            "tmax": (6, 10, "h", "label pharmacokinetics", "Food may delay Tmax from 6 to 10 hours."),
            "eliminationHalfLife": (8, 17, "h", "label pharmacokinetics"),
            "timeToSteadyState": (3, 3, "day", "label pharmacokinetics"),
        },
        "doseReference": ("MDD / GAD / pain-related label contexts", "30 to 120 mg/day across cited label contexts", "once daily or divided depending on indication in the cited label", 30, 120),
        "doseBar": (20, 40, 60, 120),
    },
    "Olanzapine": {
        "openfdaSetId": "002dd00f-7946-ea4f-e063-6294a90a5916",
        "timeCourse": {
            "tmax": (6, 6, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (21, 54, "h", "label pharmacokinetics", "5th to 95th percentile range; mean about 30 hours."),
        },
        "doseReference": ("Schizophrenia / bipolar disorder oral label contexts", "5 to 20 mg/day across cited adult oral label contexts", "once daily depending on indication in the cited label", 5, 20),
        "doseBar": (2.5, 5, 10, 20),
    },
    "Risperidone": {
        "openfdaSetId": "07d43bf6-a695-453a-a5c7-9581effe585c",
        "timeCourse": {
            "tmax": (1, 1, "h", "label pharmacokinetics", "Risperidone parent compound; active metabolite Tmax depends on CYP2D6 phenotype."),
            "peakEffect": (3, 17, "h", "label pharmacokinetics", "9-hydroxyrisperidone peak occurs around 3 hours in extensive metabolizers and around 17 hours in poor metabolizers."),
        },
        "doseReference": ("Schizophrenia / bipolar mania / irritability in autism label contexts", "0.25 to 16 mg/day across cited label contexts", "once or twice daily depending on indication and product-specific label", 0.25, 16),
        "doseBar": (0.25, 1, 3, 6),
    },
    "Quetiapine": {
        "openfdaSetId": "01261008-5f42-4844-8a65-d4545a67a309",
        "timeCourse": {
            "tmax": (1.5, 1.5, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (6, 6, "h", "label pharmacokinetics"),
            "timeToSteadyState": (2, 2, "day", "label pharmacokinetics"),
        },
        "doseReference": ("Schizophrenia / bipolar disorder immediate-release label contexts", "25 to 800 mg/day across cited label contexts", "once daily at bedtime or divided depending on indication in the cited label", 25, 800),
        "doseBar": (25, 50, 300, 750),
    },
    "Clozapine": {
        "openfdaSetId": "09231d80-6343-4a34-bd5b-100c547fd3c9",
        "timeCourse": {
            "tmax": (1, 6, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (12, 12, "h", "label pharmacokinetics"),
        },
        "doseReference": ("Treatment-resistant schizophrenia label context", "12.5 to 900 mg/day across cited label titration and maximum context", "divided dosing after initial titration in the cited label", 12.5, 900),
        "doseBar": (12.5, 25, 300, 900),
    },
    "Zolpidem": {
        "openfdaSetId": "021153ce-fe27-4ed1-8d88-b4157b0ed734",
        "timeCourse": {
            "tmax": (1.6, 1.6, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (1.4, 4.5, "h", "label pharmacokinetics"),
        },
        "doseReference": ("Insomnia label context", "5 to 10 mg once nightly in cited immediate-release label context", "immediately before bedtime with 7 to 8 hours before awakening", 5, 10),
        "doseBar": (2.5, 5, 10, 10),
    },
    "Donepezil": {
        "openfdaSetId": "03e26183-3e07-45e8-a414-a8bad32483e2",
        "timeCourse": {
            "tmax": (3, 8, "h", "label pharmacokinetics", "About 3 hours for 10 mg tablets and about 8 hours for 23 mg tablets."),
            "eliminationHalfLife": (70, 70, "h", "label pharmacokinetics"),
            "timeToSteadyState": (15, 15, "day", "label pharmacokinetics"),
        },
        "doseReference": ("Alzheimer disease label context", "5 to 23 mg/day across cited label contexts", "once daily in the evening", 5, 23),
        "doseBar": (5, 10, 23, 23),
    },
    "Lithium Carbonate": {
        "openfdaSetId": "01c4facd-ed79-4078-ba33-2044de372d0f",
        "timeCourse": {
            "tmax": (0.25, 6, "h", "label pharmacokinetics", "Immediate-release Tmax 0.25 to 3 hours; sustained-release Tmax 2 to 6 hours."),
        },
        "doseReference": ("Bipolar disorder label context", "300 mg two or three times daily as cited starting regimens; titrate by serum lithium concentration in label context", "serum concentration-guided; 12-hour trough sampling in cited label", 600, 900),
        "doseBar": (300, 600, 900, 1800),
        "tdm": {
            "therapeuticRanges": [
                {"indication": "Acute manic or mixed episodes", "range": "0.8-1.2", "unit": "mEq/L", "note": "12-hour post-dose serum lithium concentration in cited label context."},
                {"indication": "Maintenance treatment", "range": "0.8-1.0", "unit": "mEq/L", "note": "12-hour post-dose serum lithium concentration in cited label context."}
            ],
            "samplingTime": "Draw serum lithium concentration 12 hours after the last oral dose in the cited label context.",
        },
    },
    "Alprazolam": {
        "openfdaSetId": "02840ea6-e4a0-96a0-e063-6394a90a19ae",
        "timeCourse": {
            "tmax": (1, 2, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (6.3, 26.9, "h", "label pharmacokinetics", "Mean about 11.2 hours in healthy adults."),
        },
        "doseReference": ("Generalized anxiety disorder / panic disorder label contexts", "0.75 to 10 mg/day across cited adult divided-dose label contexts", "divided dosing; taper gradually in the cited label", 0.75, 10),
        "doseBar": (0.25, 0.5, 2, 4),
    },
    "Diazepam": {
        "openfdaSetId": "01f04ee4-fff9-499a-8213-984e16368084",
        "timeCourse": {
            "tmax": (0.25, 2.5, "h", "label pharmacokinetics", "Average 1 to 1.5 hours fasting; food can delay Tmax."),
        },
        "doseReference": ("Anxiety / alcohol withdrawal / muscle spasm / seizure adjunct label contexts", "2 to 40 mg/day across cited adult divided-dose label contexts", "2 to 4 divided doses depending on indication in the cited label", 2, 40),
        "doseBar": (2, 5, 10, 20),
    },
    "Buspirone": {
        "openfdaSetId": "02628a0c-bfdb-4a58-8e48-bcd8ca12d53b",
        "timeCourse": {
            "tmax": (40, 90, "min", "label pharmacokinetics"),
        },
        "doseReference": ("Anxiety label context", "15 to 60 mg/day across cited adult divided-dose label context", "divided dosing; commonly 20 to 30 mg/day in cited trials", 15, 60),
        "doseBar": (5, 15, 30, 60),
    },
    "Methylphenidate": {
        "openfdaSetId": "034fb7cd-e183-475e-8beb-64fd88facc8f",
        "timeCourse": {},
        "doseReference": ("ADHD extended-release capsule label context", "20 to 60 mg/day in cited extended-release capsule label context", "once daily in the morning; titrate weekly in 10 mg increments in the cited label", 20, 60),
        "doseBar": (5, 20, 40, 60),
    },
    "Armodafinil": {
        "openfdaSetId": "23da56b8-7552-4362-a1ab-e794c131194c",
        "timeCourse": {
            "timeToSteadyState": (7, 7, "day", "label pharmacokinetics"),
        },
    },
    "Bupropion": {
        "openfdaSetId": "004d8121-59d4-46c4-acb8-b2dd097bf556",
        "timeCourse": {
            "eliminationHalfLife": (12, 30, "h", "label pharmacokinetics", "Label reports mean bupropion half-life 21 ± 9 hours."),
            "timeToSteadyState": (8, 8, "day", "label pharmacokinetics"),
        },
        "doseReference": ("Major depressive disorder / seasonal affective disorder extended-release label context", "150 to 300 mg/day in cited extended-release label context", "once daily; gradual increase to reduce seizure risk in the cited label", 150, 300),
        "doseBar": (150, 150, 300, 300),
    },
    "Trazodone": {
        "openfdaSetId": "007f38e0-653b-43e4-a1c1-b59997b2762a",
        "timeCourse": {
            "tmax": (1, 2, "h", "label pharmacokinetics", "Peak plasma levels occur about 1 hour fasting or 2 hours with food in the cited label."),
        },
        "doseReference": ("Major depressive disorder label context", "150 to 400 mg/day in cited outpatient label context", "divided doses after meal or light snack; gradual changes in the cited label", 150, 400),
        "doseBar": (50, 150, 300, 400),
    },
    "Vilazodone": {
        "openfdaSetId": "0374fbb1-b0fb-4150-b1e7-92e2b0d32ecf",
        "timeCourse": {
            "tmax": (4, 5, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (25, 25, "h", "label pharmacokinetics"),
            "timeToSteadyState": (3, 3, "day", "label pharmacokinetics"),
        },
        "doseReference": ("Major depressive disorder label context", "20 to 40 mg/day in cited target-dose label context", "once daily with food after label titration", 20, 40),
        "doseBar": (10, 20, 40, 40),
    },
    "Temazepam": {
        "openfdaSetId": "066e25b2-8a1b-4b08-b585-ce36753bf104",
        "timeCourse": {
            "eliminationHalfLife": (3.5, 18.4, "h", "label pharmacokinetics", "Label reports terminal half-life range with mean about 8.8 hours."),
        },
        "doseReference": ("Insomnia label context", "7.5 to 30 mg before retiring in cited label context", "single bedtime dose in the cited label", 7.5, 30),
        "doseBar": (7.5, 15, 30, 30),
    },
    "Loxapine": {
        "openfdaSetId": "2ce01a5d-b9bc-af4d-e063-6394a90af9e4",
        "timeCourse": {
            "onset": (20, 30, "min", "pharmacodynamic effect"),
            "peakEffect": (1.5, 3, "h", "pharmacodynamic effect"),
            "durationOfAction": (12, 12, "h", "pharmacodynamic effect", "Normal-volunteer sedation timing in cited label; not equivalent to antipsychotic response."),
        },
        "doseReference": ("Schizophrenia oral capsule label context", "20 to 250 mg/day across cited initial, maintenance, and upper-limit label contexts", "usually divided two to four times daily in the cited label", 20, 250),
        "doseBar": (10, 20, 100, 250),
    },
    "Thioridazine": {
        "openfdaSetId": "1fd16a99-e856-4a37-9dae-c443714fac14",
        "timeCourse": {},
        "doseReference": ("Treatment-refractory schizophrenia label context", "200 to 800 mg/day in cited adult total daily dosage context", "divided two to four times daily in the cited label", 200, 800),
        "doseBar": (50, 200, 400, 800),
    },
    "Disulfiram": {
        "openfdaSetId": "07abe950-3565-418c-8086-b804406a8c87",
        "timeCourse": {
            "durationOfAction": (1, 2, "week", "pharmacodynamic effect", "Label states alcohol reaction may persist one or even two weeks after the last dose."),
        },
        "doseReference": ("Alcohol use disorder aversive-therapy label context", "125 to 500 mg/day across cited maintenance range and upper limit", "single daily dose in the cited label context", 125, 500),
        "doseBar": (125, 250, 500, 500),
    },
    "Tranylcypromine": {
        "openfdaSetId": "3c14a558-2df4-4d93-97dd-640dec6ee785",
        "timeCourse": {
            "durationOfAction": (3, 5, "day", "pharmacodynamic effect", "Label states MAO activity recovery takes up to 3 to 5 days although tranylcypromine is eliminated within 24 hours."),
        },
        "doseReference": ("Major depressive disorder MAOI label context", "30 to 60 mg/day in cited label context", "divided doses; gradual increases in the cited label", 30, 60),
        "doseBar": (10, 30, 40, 60),
    },
    "Hydroxyzine": {
        "openfdaSetId": "02616eee-a1a4-4afc-b84e-69fd28755138",
        "timeCourse": {},
        "doseReference": ("Anxiety/tension symptomatic-relief label context", "50 to 400 mg/day in cited adult anxiety label context", "50 to 100 mg four times daily in the cited label", 50, 400),
        "doseBar": (25, 50, 100, 400),
    },
    "Imipramine": {
        "openfdaSetId": "02c19a0a-3d9d-4881-ad14-1687aafd87b5",
        "timeCourse": {},
        "doseReference": ("Depression label context", "75 to 300 mg/day across cited adult outpatient and hospitalized label contexts", "divided doses; maintenance lower in cited label", 75, 300),
        "doseBar": (25, 75, 150, 300),
    },
    "Nortriptyline": {
        "openfdaSetId": "017f7717-e160-4142-9a5a-1a2bd9776707",
        "timeCourse": {},
        "doseReference": ("Depression label context", "75 to 150 mg/day across cited adult label context", "25 mg three or four times daily or once daily total dose in cited label", 75, 150),
        "doseBar": (25, 75, 100, 150),
        "tdm": {
            "therapeuticRanges": [
                {"indication": "Nortriptyline plasma concentration monitoring in cited label context", "range": "50-150", "unit": "ng/mL", "note": "Label states plasma levels should be monitored above 100 mg/day and maintained in this range."}
            ],
            "samplingTime": "Product- and laboratory-specific; interpret with dose timing, adherence, CYP2D6 status, ECG, symptoms, and local method.",
        },
    },
    "Haloperidol": {
        "openfdaSetId": "00bb61c8-db35-4c04-9ef7-47d447d2496b",
        "timeCourse": {},
        "doseReference": ("Psychotic disorder oral label context", "1 to 100 mg/day across cited adult oral label contexts", "divided twice or three times daily; individualized in the cited label", 1, 100),
        "doseBar": (0.5, 1, 15, 100),
    },
    "Chlorpromazine": {
        "openfdaSetId": "02c56c39-99bd-44a4-8d9d-9d867c323968",
        "timeCourse": {},
        "doseReference": ("Psychotic disorder oral label context", "30 to 1000 mg/day across cited outpatient and inpatient label contexts", "divided dosing; individualized and reduced to maintenance in the cited label", 30, 1000),
        "doseBar": (25, 75, 400, 1000),
    },
    "Prochlorperazine": {
        "openfdaSetId": "02069e43-5245-20dc-e063-6294a90a9dcd",
        "timeCourse": {},
        "doseReference": ("Adult psychiatric disorder oral label context", "15 to 150 mg/day across cited anxiety and psychotic-disorder label contexts", "divided three or four times daily in the cited label", 15, 150),
        "doseBar": (5, 15, 75, 150),
    },
    "Amphetamine": {
        "openfdaSetId": "00242264-40d8-4267-a91a-727a1f088616",
        "timeCourse": {},
        "doseReference": ("ADHD / narcolepsy oral label context", "5 to 60 mg/day across cited ADHD and narcolepsy label contexts", "divided dosing; late evening doses avoided in the cited label", 5, 60),
        "doseBar": (2.5, 5, 20, 40),
    },
    "Agomelatine": {
        "emcProductId": "6564",
        "emcTitle": "UK eMC SmPC: Valdoxan 25 mg film-coated tablets",
        "timeCourse": {
            "tmax": (1, 2, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (1, 2, "h", "label pharmacokinetics"),
        },
        "doseReference": ("Major depressive episodes adult SmPC context", "25 to 50 mg/day in cited SmPC context", "once daily at bedtime; increase after two weeks only in the cited SmPC context", 25, 50),
        "doseBar": (25, 25, 50, 50),
    },
    "Amisulpride": {
        "emcProductId": "548",
        "emcTitle": "UK eMC SmPC: Amisulpride 50 mg Tablets",
        "timeCourse": {
            "eliminationHalfLife": (12, 12, "h", "label pharmacokinetics"),
        },
        "doseReference": ("Schizophrenia oral SmPC/PIL context", "50 to 1200 mg/day across cited oral amisulpride label contexts", "once daily up to 300 mg/day or divided above 300 mg/day in cited label context", 50, 1200),
        "doseBar": (50, 100, 800, 1200),
    },
    "Sulpiride": {
        "emcProductId": "2430",
        "emcTitle": "UK eMC SmPC: Sulpiride 200 mg Tablets",
        "timeCourse": {
            "tmax": (3, 6, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (8, 8, "h", "label pharmacokinetics"),
        },
    },
    "Flupentixol": {
        "emcProductId": "998",
        "emcTitle": "UK eMC SmPC: Fluanxol 0.5 mg film-coated tablets",
        "timeCourse": {
            "tmax": (4, 4, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (35, 35, "h", "label pharmacokinetics"),
        },
    },
    "Zuclopenthixol": {
        "emcProductId": "994",
        "emcTitle": "UK eMC SmPC: Clopixol 2 mg film-coated tablets",
        "timeCourse": {
            "tmax": (3, 6, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (1, 1, "day", "label pharmacokinetics"),
        },
    },
    "Trifluoperazine": {
        "emcProductId": "1546",
        "emcTitle": "UK eMC SmPC: Trifluoperazine 5 mg/5 ml Oral Solution",
        "timeCourse": {
            "eliminationHalfLife": (22, 22, "h", "label pharmacokinetics", "Terminal elimination half-life; alpha phase is about 3.6 hours in cited SmPC."),
        },
    },
    "Nitrazepam": {
        "emcProductId": "3901",
        "emcTitle": "UK eMC SmPC: Mogadon 5 mg Tablets",
        "timeCourse": {
            "eliminationHalfLife": (30, 30, "h", "label pharmacokinetics", "Average plasma half-life; elderly patients may have longer half-life in cited SmPC."),
            "timeToSteadyState": (5, 5, "day", "label pharmacokinetics"),
        },
    },
    "Zopiclone": {
        "emcProductId": "2855",
        "emcTitle": "UK eMC SmPC: Zimovane 7.5 mg film-coated tablets",
        "timeCourse": {
            "eliminationHalfLife": (5, 5, "h", "label pharmacokinetics"),
        },
        "doseReference": ("Insomnia oral SmPC context", "3.75 to 7.5 mg at night in cited SmPC context", "single bedtime dose; lower dose in older adults or impaired hepatic/renal/respiratory function in cited SmPC context", 3.75, 7.5),
        "doseBar": (3.75, 3.75, 7.5, 7.5),
    },
    "Melatonin": {
        "emcProductId": "101120",
        "emcTitle": "UK eMC SmPC: Melatonin 2 mg prolonged-release tablets",
        "timeCourse": {
            "eliminationHalfLife": (3.5, 4, "h", "label pharmacokinetics", "Prolonged-release tablet SmPC; immediate-release products may differ."),
        },
        "doseReference": ("Insomnia prolonged-release SmPC context", "2 mg/day in cited prolonged-release SmPC context", "once daily after food, 1 to 2 hours before bedtime in cited SmPC context", 2, 2),
        "doseBar": (1, 2, 2, 2),
    },
}


def _load_auto_label_data() -> dict:
    if not AUTO_LABEL_DATA_PATH.exists():
        return {}
    return json.loads(AUTO_LABEL_DATA_PATH.read_text(encoding="utf-8"))


# 手工核对数据优先；批量 openFDA 缓存只补充尚未手工覆盖的条目。
MANUAL_CURATED_LABEL_DATA = CURATED_LABEL_DATA
CURATED_LABEL_DATA = {**_load_auto_label_data(), **MANUAL_CURATED_LABEL_DATA}


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


def openfda_source(name: str, set_id: str) -> dict:
    query = name.replace(" ", "+")
    return {
        "title": f"openFDA drug label: {name}",
        "url": f"https://api.fda.gov/drug/label.json?search=set_id:{set_id}",
        "sourceType": "regulatory-label",
        "accessedDate": ACCESSED_DATE,
        "evidenceLevel": "REGULATORY_LABEL",
        "labelSection": "12.3 Pharmacokinetics; 2 Dosage and Administration",
        "note": f"openFDA SPL label record; DailyMed search query: https://dailymed.nlm.nih.gov/dailymed/search.cfm?query={query}",
    }


def emc_source(name: str, product_id: str, title: str | None = None) -> dict:
    return {
        "title": title or f"UK eMC SmPC: {name}",
        "url": f"https://www.medicines.org.uk/emc/product/{product_id}/smpc",
        "sourceType": "regulatory-label",
        "accessedDate": ACCESSED_DATE,
        "evidenceLevel": "REGULATORY_LABEL",
        "labelSection": "4.2 Posology and method of administration; 5.2 Pharmacokinetic properties",
        "note": "UK electronic Medicines Compendium Summary of Product Characteristics.",
    }


def curated_regulatory_source(name: str, curated: dict | None) -> dict:
    if not curated:
        return dailymed_search(name)
    if "openfdaSetId" in curated:
        return openfda_source(name, curated["openfdaSetId"])
    if "emcProductId" in curated:
        return emc_source(name, curated["emcProductId"], curated.get("emcTitle"))
    return dailymed_search(name)


def time_value(value: tuple) -> dict:
    min_value, max_value, unit, basis, *note = value
    result = {
        "min": min_value,
        "max": max_value,
        "unit": unit,
        "basis": basis,
    }
    if note:
        result["note"] = note[0]
    return result


def curated_time_course(name: str, source_refs: list[dict]) -> list[dict] | None:
    curated = CURATED_LABEL_DATA.get(name)
    if not curated:
        return None
    time_course = {
        "route": "oral",
        "formulation": "product-specific oral formulation",
        "notes": [
            "Tmax, half-life, steady-state, and duration are label pharmacokinetic parameters and are not equivalent to clinical response timing.",
            "Values can vary by formulation, population, organ function, and interacting medicines.",
        ],
        "sourceRefs": source_refs,
    }
    for field in [
        "onset",
        "tmax",
        "peakEffect",
        "durationOfAction",
        "eliminationHalfLife",
        "timeToSteadyState",
        "washout",
    ]:
        if field in curated["timeCourse"]:
            time_course[field] = time_value(curated["timeCourse"][field])
    return [time_course]


def curated_dose_use_reference(name: str, source_refs: list[dict]) -> list[dict] | None:
    curated = CURATED_LABEL_DATA.get(name)
    if not curated or "doseReference" not in curated:
        return None
    indication, amount_text, schedule_text, range_min, range_max = curated["doseReference"]
    return [
        {
            "indication": indication,
            "population": "Adults unless product-specific label states otherwise",
            "route": "oral",
            "formulation": "product-specific oral formulation",
            "amountText": amount_text,
            "scheduleText": schedule_text,
            "ranges": [
                {
                    "min": range_min,
                    "max": range_max,
                    "unit": "mg",
                    "basis": "daily-total",
                    "frequency": schedule_text,
                    "rangeKind": "label-regimen",
                    "label": "Label regimen range",
                    "note": "Label/literature regimen range for reference indexing; not a dosing recommendation.",
                }
            ],
            "sourceType": "regulatory-label",
            "evidenceLevel": "REGULATORY_LABEL",
            "note": "Dose use references are not recommendations and must not be used for self-medication or dose adjustment.",
            "sourceRefs": source_refs,
        }
    ]


def curated_roas(name: str) -> list[dict]:
    curated = CURATED_LABEL_DATA.get(name)
    if not curated or "doseBar" not in curated:
        return []
    light, common, strong, heavy = curated["doseBar"]
    return [
        {
            "name": "oral",
            "dose": {
                "units": "mg",
                "lightMin": light,
                "commonMin": common,
                "strongMin": strong,
                "heavyMin": heavy,
            },
        }
    ]


def merge_tdm(base_tdm: dict, name: str, source_refs: list[dict]) -> dict:
    curated = CURATED_LABEL_DATA.get(name, {})
    tdm = dict(base_tdm)
    tdm["sourceRefs"] = source_refs
    if "tdm" in curated:
        tdm["isRoutinelyMonitored"] = True
        tdm["monitoringType"] = "serum concentration monitoring"
        tdm["samplingTime"] = curated["tdm"].get("samplingTime")
        tdm["therapeuticRanges"] = curated["tdm"].get("therapeuticRanges", [])
        tdm["interpretationCaveats"] = [
            "Interpret serum concentration with timing, renal function, sodium/fluid status, interacting medicines, symptoms, and local laboratory method.",
            "Serum concentration targets are label contexts and not self-adjustment instructions.",
        ]
    return tdm


def entry(name: str, aliases: list[str], group_key: str, atc_codes: list[str]) -> dict:
    group = GROUPS[group_key]
    curated = CURATED_LABEL_DATA.get(name)
    source_refs = group["sourceRefs"] + [curated_regulatory_source(name, curated)]
    route = "oral"
    base_tdm = {
        "isRoutinelyMonitored": group_key == "mood-stabilizer" and name in {"Lithium Carbonate", "Valproate", "Carbamazepine"},
        "monitoringType": "drug-specific clinical/laboratory monitoring; serum concentration only for selected medicines",
        "reason": "Psychiatry medicines are generally monitored by clinical response and safety labs; selected medicines such as lithium and some anticonvulsants may use serum concentration monitoring.",
        "pharmacokineticParametersAvailable": True,
        "interpretationCaveats": [
            "Monitoring type is drug-specific and should not be interpreted as universal TDM.",
            "Interpretation depends on indication, formulation, timing, organ function, comedications, and local laboratory method.",
        ],
        "sourceRefs": source_refs,
    }
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
        "timeCourse": curated_time_course(name, source_refs) or [
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
        "tdm": merge_tdm(base_tdm, name, source_refs),
        "doseUseReferences": curated_dose_use_reference(name, source_refs) or [
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
        "roas": curated_roas(name),
        "sourceRefs": source_refs,
    }


def patch(name: str, aliases: list[str], group_key: str, atc_codes: list[str]) -> dict:
    generated = entry(name, aliases, group_key, atc_codes)
    fields_set = {
        "clinicalInfo": generated["clinicalInfo"],
        "timeCourse": generated["timeCourse"],
        "tdm": generated["tdm"],
        "doseUseReferences": generated["doseUseReferences"],
        "sourceRefs": generated["sourceRefs"],
    }
    return {
        "name": name,
        "fieldsSet": fields_set,
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
