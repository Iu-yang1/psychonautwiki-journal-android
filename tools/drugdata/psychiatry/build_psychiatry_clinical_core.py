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
            "eliminationHalfLife": (16.7, 20, "h", "label pharmacokinetics", "Active moiety half-life is about 20 hours in the cited FDA label; eMC SmPC reports 16.7 hours in young adults."),
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
            "eliminationHalfLife": (18, 36, "h", "label pharmacokinetics"),
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
            "eliminationHalfLife": (48, 48, "h", "label pharmacokinetics", "Terminal elimination half-life is up to 48 hours in the cited label; active metabolite N-desmethyldiazepam may be up to 100 hours."),
        },
        "doseReference": ("Anxiety / alcohol withdrawal / muscle spasm / seizure adjunct label contexts", "2 to 40 mg/day across cited adult divided-dose label contexts", "2 to 4 divided doses depending on indication in the cited label", 2, 40),
        "doseBar": (2, 5, 10, 20),
    },
    "Buspirone": {
        "openfdaSetId": "02628a0c-bfdb-4a58-8e48-bcd8ca12d53b",
        "timeCourse": {
            "tmax": (40, 90, "min", "label pharmacokinetics"),
            "eliminationHalfLife": (2, 3, "h", "label pharmacokinetics", "Average elimination half-life of unchanged buspirone after single 10 to 40 mg doses."),
        },
        "doseReference": ("Anxiety label context", "15 to 60 mg/day across cited adult divided-dose label context", "divided dosing; commonly 20 to 30 mg/day in cited trials", 15, 60),
        "doseBar": (5, 15, 30, 60),
    },
    "Methylphenidate": {
        "openfdaSetId": "034fb7cd-e183-475e-8beb-64fd88facc8f",
        "timeCourse": {
            "tmax": (1, 3, "h", "label pharmacokinetics", "First peak for methylphenidate extended-release capsules in cited label."),
            "peakEffect": (4.3, 11, "h", "label pharmacokinetics", "Second peak window for methylphenidate extended-release capsules; product-specific bimodal profile."),
            "eliminationHalfLife": (1.5, 4.2, "h", "label pharmacokinetics", "Range across pediatric and adult extended-release capsule pharmacokinetic table in cited label."),
        },
        "doseReference": ("ADHD extended-release capsule label context", "20 to 60 mg/day in cited extended-release capsule label context", "once daily in the morning; titrate weekly in 10 mg increments in the cited label", 20, 60),
        "doseBar": (5, 20, 40, 60),
    },
    "Armodafinil": {
        "openfdaSetId": "23da56b8-7552-4362-a1ab-e794c131194c",
        "timeCourse": {
            "timeToSteadyState": (7, 7, "day", "label pharmacokinetics"),
        },
        "doseReference": ("Narcolepsy / obstructive sleep apnea / shift-work disorder label contexts", "150 to 250 mg/day across cited adult label contexts", "once daily in the morning for narcolepsy/OSA or about 1 hour before shift work in cited label", 150, 250),
        "doseBar": (50, 150, 200, 250),
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
        "extraSourceRefs": [
            {
                "title": "UK eMC SmPC: Trazodone 50 mg Capsules",
                "url": "https://www.medicines.org.uk/emc/product/7186/smpc",
                "sourceType": "regulatory-label",
                "accessedDate": ACCESSED_DATE,
                "evidenceLevel": "REGULATORY_LABEL",
                "labelSection": "5.2 Pharmacokinetic properties",
            }
        ],
        "timeCourse": {
            "tmax": (1, 2, "h", "label pharmacokinetics", "Peak plasma levels occur about 1 hour fasting or 2 hours with food in the cited label."),
            "eliminationHalfLife": (5, 13, "h", "label pharmacokinetics", "UK eMC SmPC describes biphasic elimination with terminal half-life of 5 to 13 hours."),
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
        "extraSourceRefs": [
            {
                "title": "PubMed: Plasma levels and half lives of thioridazine and some of its metabolites",
                "url": "https://pubmed.ncbi.nlm.nih.gov/837967/",
                "sourceType": "pharmacokinetic-study",
                "accessedDate": ACCESSED_DATE,
                "evidenceLevel": "PHARMACOKINETIC_STUDY",
                "labelSection": "Abstract",
                "note": "Older chronic psychotic patient pharmacokinetic study; early and late disappearance half-lives are not ordinary clinical duration.",
            }
        ],
        "timeCourse": {
            "eliminationHalfLife": (5, 26, "h", "literature estimate", "Study reports mean early disappearance half-life of 5 hours and mean late disappearance half-life of 26 hours."),
        },
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
    "Nortriptyline": {
        "openfdaSetId": "017f7717-e160-4142-9a5a-1a2bd9776707",
        "extraSourceRefs": [
            {
                "title": "HPRA SmPC: Nortriptyline 10 mg Film-coated Tablets",
                "url": "https://assets.hpra.ie/products/Human/29663/Licence_PA0599-005-001_20032024151245.pdf",
                "sourceType": "regulatory-label",
                "accessedDate": ACCESSED_DATE,
                "evidenceLevel": "REGULATORY_LABEL",
                "labelSection": "4.2 Posology and method of administration; 5.2 Pharmacokinetic properties",
                "note": "Irish HPRA Summary of Product Characteristics.",
            }
        ],
        "timeCourse": {
            "tmax": (4.0, 8.8, "h", "label pharmacokinetics", "SmPC reports Tmax 5.5 +/- 1.9 hours, range 4.0 to 8.8 hours."),
            "eliminationHalfLife": (16, 38, "h", "label pharmacokinetics", "SmPC reports approximate half-life 26 hours, range 16 to 38 hours."),
            "timeToSteadyState": (7, 7, "day", "label pharmacokinetics", "SmPC states steady-state plasma levels are reached within one week for most patients."),
        },
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
        "extraSourceRefs": [
            {
                "title": "European Medicines Agency product information: Haldol",
                "url": "https://www.ema.europa.eu/en/documents/referral/haldol-article-30-referral-annex-iii_en.pdf",
                "sourceType": "regulatory-label",
                "accessedDate": ACCESSED_DATE,
                "evidenceLevel": "REGULATORY_LABEL",
                "labelSection": "5.2 Pharmacokinetic properties",
                "note": "EMA referral product information; haloperidol terminal elimination half-life range and average.",
            }
        ],
        "timeCourse": {
            "eliminationHalfLife": (15, 37, "h", "label pharmacokinetics", "EMA product information reports average terminal half-life about 24 hours, range 15 to 37 hours."),
        },
        "doseReference": ("Psychotic disorder oral label context", "1 to 100 mg/day across cited adult oral label contexts", "divided twice or three times daily; individualized in the cited label", 1, 100),
        "doseBar": (0.5, 1, 15, 100),
    },
    "Chlorpromazine": {
        "openfdaSetId": "02c56c39-99bd-44a4-8d9d-9d867c323968",
        "extraSourceRefs": [
            {
                "title": "HPRA SmPC: Clonactil 100 mg film-coated tablets",
                "url": "https://assets.hpra.ie/products/Human/14852/Licence_PA0126-026-002_24112019113323.pdf",
                "sourceType": "regulatory-label",
                "accessedDate": ACCESSED_DATE,
                "evidenceLevel": "REGULATORY_LABEL",
                "labelSection": "4.2 Posology and method of administration; 5.2 Pharmacokinetic properties",
                "note": "Irish HPRA Summary of Product Characteristics.",
            }
        ],
        "timeCourse": {
            "eliminationHalfLife": (3, 288, "h", "label pharmacokinetics", "SmPC describes a prolonged biphasic half-life from 3 hours up to 12 days; do not interpret this as ordinary duration of action."),
        },
        "doseReference": ("Psychotic disorder oral label context", "30 to 1000 mg/day across cited outpatient and inpatient label contexts", "divided dosing; individualized and reduced to maintenance in the cited label", 30, 1000),
        "doseBar": (25, 75, 400, 1000),
    },
    "Amphetamine": {
        "openfdaSetId": "00242264-40d8-4267-a91a-727a1f088616",
        "extraOpenfdaSetIds": [
            ("aff45863-ffe1-4d4f-8acf-c7081512a6c0", "openFDA drug label: Adderall XR"),
        ],
        "timeCourse": {
            "tmax": (3, 7, "h", "label pharmacokinetics", "Immediate-release mixed amphetamine salts peak around 3 hours; Adderall XR Tmax about 7 hours in cited label."),
            "eliminationHalfLife": (10, 13, "h", "label pharmacokinetics", "Adult mean half-life: about 10 hours for d-amphetamine and 13 hours for l-amphetamine in cited Adderall XR label."),
        },
        "doseReference": ("ADHD / narcolepsy oral label context", "5 to 60 mg/day across cited ADHD and narcolepsy label contexts", "divided dosing; late evening doses avoided in the cited label", 5, 60),
        "doseBar": (2.5, 5, 20, 40),
    },
    "Amitriptyline": {
        "emcProductId": "14334",
        "emcTitle": "UK eMC SmPC: Amitriptyline 10 mg film-coated tablets",
        "timeCourse": {
            "tmax": (1.93, 7.98, "h", "label pharmacokinetics", "SmPC reports mean Tmax about 3.89 ± 1.87 hours for 10 mg/25 mg film-coated tablets."),
        },
        "doseReference": ("Major depressive disorder adult SmPC context", "50 to 150 mg/day in cited adult depression SmPC context", "divided into two doses; increased gradually in cited SmPC", 50, 150),
        "doseBar": (10, 50, 100, 150),
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
        "doseReference": ("Schizophrenia oral SmPC context", "400 to 2400 mg/day across cited adult SmPC contexts", "one or two tablets twice daily; symptom-pattern-specific ranges in cited SmPC", 400, 2400),
        "doseBar": (200, 400, 800, 2400),
    },
    "Flupentixol": {
        "emcProductId": "998",
        "emcTitle": "UK eMC SmPC: Fluanxol 0.5 mg film-coated tablets",
        "timeCourse": {
            "tmax": (4, 4, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (35, 35, "h", "label pharmacokinetics"),
        },
        "doseReference": ("Low-dose oral flupentixol SmPC context", "1 to 3 mg/day in cited adult SmPC context", "single morning dose or divided doses above 2 mg/day in cited SmPC", 1, 3),
        "doseBar": (0.5, 1, 2, 3),
    },
    "Zuclopenthixol": {
        "emcProductId": "994",
        "emcTitle": "UK eMC SmPC: Clopixol 2 mg film-coated tablets",
        "timeCourse": {
            "tmax": (3, 6, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (1, 1, "day", "label pharmacokinetics"),
        },
        "doseReference": ("Psychotic disorder oral SmPC context", "4 to 150 mg/day in cited adult oral SmPC context", "divided doses; usual initial and maintenance ranges are narrower in cited SmPC", 4, 150),
        "doseBar": (2, 4, 50, 150),
    },
    "Trifluoperazine": {
        "emcProductId": "1546",
        "emcTitle": "UK eMC SmPC: Trifluoperazine 5 mg/5 ml Oral Solution",
        "timeCourse": {
            "eliminationHalfLife": (22, 22, "h", "label pharmacokinetics", "Terminal elimination half-life; alpha phase is about 3.6 hours in cited SmPC."),
        },
        "doseReference": ("Low-dose / high-dose oral SmPC contexts", "2 to 15 mg/day across cited adult low-dose and high-dose starting contexts", "divided doses; further increases only at intervals in cited SmPC", 2, 15),
        "doseBar": (1, 2, 6, 15),
    },
    "Droperidol": {
        "emcProductId": "7310",
        "emcTitle": "UK eMC SmPC: Droperidol 2.5 mg/ml Solution for Injection",
        "route": "intravenous",
        "formulation": "solution for injection",
        "timeCourse": {
            "onset": (2, 3, "min", "clinical effect"),
            "durationOfAction": (2, 4, "h", "pharmacodynamic effect", "Sedative/tranquillising effects tend to persist 2 to 4 hours; alertness may be affected up to 12 hours."),
            "eliminationHalfLife": (121, 147, "min", "label pharmacokinetics", "SmPC reports elimination half-life 134 ± 13 minutes."),
        },
        "customDoseUseReferences": [
            {
                "indication": "Post-operative nausea and vomiting intravenous SmPC context",
                "route": "intravenous",
                "formulation": "solution for injection",
                "amountText": "0.625 to 1.25 mg per adult IV dose in cited SmPC context",
                "scheduleText": "administered around the end of surgery; repeat dosing no more often than every 6 hours as required in cited SmPC",
                "ranges": [
                    {
                        "min": 0.625,
                        "max": 1.25,
                        "unit": "mg",
                        "basis": "per-dose",
                        "frequency": "repeat every 6 hours as required in cited SmPC context",
                        "rangeKind": "label-regimen",
                        "label": "IV per-dose label range",
                        "note": "PONV label context, not a psychiatric dosing recommendation.",
                    }
                ],
            }
        ],
    },
    "Fluphenazine": {
        "openfdaSetId": "0860b3f3-3116-40f8-bcb0-e5c47731bdc8",
        "extraOpenfdaSetIds": [
            ("044f49f3-f980-4ef8-8beb-78412702cd9d", "openFDA drug label: Fluphenazine Decanoate"),
        ],
        "route": "intramuscular or subcutaneous",
        "formulation": "decanoate depot injection",
        "timeCourse": {
            "onset": (24, 72, "h", "clinical effect"),
            "peakEffect": (48, 96, "h", "clinical effect", "Label states effects on psychotic symptoms become significant within 48 to 96 hours after injection."),
            "durationOfAction": (4, 6, "week", "clinical effect", "Maintenance injection may control symptoms up to four weeks or longer; response has lasted as long as six weeks in a few patients."),
        },
        "doseReference": ("Psychotic disorder oral label context", "1 to 40 mg/day across cited oral maintenance, initial, and upper label contexts", "divided every 6 to 8 hours initially; maintenance often lower in cited label", 1, 40),
        "doseRoute": "oral",
        "doseFormulation": "tablet or elixir",
        "doseBar": (1, 2.5, 20, 40),
    },
    "Hydroxyzine": {
        "emcProductId": "100997",
        "emcTitle": "UK eMC SmPC: Hydroxyzine 10 mg film-coated tablets",
        "timeCourse": {
            "tmax": (1.7, 2.5, "h", "label pharmacokinetics", "SmPC reports mean Tmax 2.1 ± 0.4 hours after single oral dose."),
            "eliminationHalfLife": (14, 24.1, "h", "label pharmacokinetics", "SmPC reports 20.0 ± 4.1 hours and 14.0 hours in adults."),
        },
        "doseReference": ("Anxiety adult SmPC context", "50 to 100 mg/day in cited adult anxiety SmPC context", "divided doses; use lowest effective dose for shortest duration in cited SmPC", 50, 100),
        "doseBar": (10, 25, 50, 100),
    },
    "Nitrazepam": {
        "emcProductId": "3901",
        "emcTitle": "UK eMC SmPC: Mogadon 5 mg Tablets",
        "timeCourse": {
            "tmax": (2, 2, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (30, 30, "h", "label pharmacokinetics", "Average plasma half-life; elderly patients may have longer half-life in cited SmPC."),
            "timeToSteadyState": (5, 5, "day", "label pharmacokinetics"),
        },
        "doseReference": ("Insomnia oral SmPC context", "5 to 10 mg at bedtime in cited adult SmPC context", "single dose before retiring; shortest possible duration in cited SmPC", 5, 10),
        "doseBar": (2.5, 5, 10, 10),
    },
    "Imipramine": {
        "emcProductId": "13879",
        "emcTitle": "UK eMC SmPC: Imipramine Hydrochloride 25 mg/5 ml Oral Solution",
        "timeCourse": {
            "eliminationHalfLife": (19, 19, "h", "label pharmacokinetics"),
        },
        "doseReference": ("Depression adult SmPC context", "75 to 300 mg/day across cited adult outpatient and inpatient SmPC contexts", "divided doses; maintenance lower in cited SmPC", 75, 300),
        "doseBar": (25, 75, 150, 300),
    },
    "Ketamine": {
        "openfdaSetId": "14e8f864-8b8a-4e7e-8439-e510d3107063",
        "route": "intravenous",
        "formulation": "injection",
        "timeCourse": {
            "durationOfAction": (10, 15, "min", "label pharmacokinetics", "Initial alpha phase half-life corresponds clinically to anesthetic effect in cited label."),
            "eliminationHalfLife": (2.5, 2.5, "h", "label pharmacokinetics", "Redistribution beta-phase half-life after intravenous administration in cited label."),
        },
        "customDoseUseReferences": [
            {
                "indication": "Anesthesia induction label context",
                "route": "intravenous or intramuscular",
                "formulation": "injection",
                "amountText": "IV 1 to 4.5 mg/kg or IM 6.5 to 13 mg/kg initial induction dose in cited label context",
                "scheduleText": "administered by clinicians experienced in general anesthesia and airway management in cited label",
                "ranges": [
                    {
                        "min": 1,
                        "max": 4.5,
                        "unit": "mg/kg",
                        "basis": "weight-based",
                        "frequency": "single IV induction context",
                        "rangeKind": "label-regimen",
                        "label": "IV induction label range",
                        "note": "Anesthesia label context, not a psychiatric ketamine protocol.",
                    },
                    {
                        "min": 6.5,
                        "max": 13,
                        "unit": "mg/kg",
                        "basis": "weight-based",
                        "frequency": "single IM induction context",
                        "rangeKind": "label-regimen",
                        "label": "IM induction label range",
                        "note": "Anesthesia label context, not a psychiatric ketamine protocol.",
                    },
                ],
            }
        ],
    },
    "Chlordiazepoxide": {
        "emcProductId": "1729",
        "emcTitle": "UK eMC SmPC: Librium 10 mg Capsules",
        "timeCourse": {
            "tmax": (1, 2, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (6, 30, "h", "label pharmacokinetics"),
            "timeToSteadyState": (3, 3, "day", "label pharmacokinetics", "Parent drug steady-state levels usually reached within three days; active metabolites reach steady state later."),
        },
        "doseReference": ("Anxiety / insomnia associated with anxiety / alcohol withdrawal SmPC contexts", "10 to 100 mg/day across cited adult SmPC contexts", "divided doses or bedtime depending on indication in cited SmPC", 10, 100),
        "doseBar": (5, 10, 30, 100),
    },
    "Lormetazepam": {
        "emcProductId": "5269",
        "emcTitle": "UK eMC SmPC: Lormetazepam 0.5 mg Tablets",
        "timeCourse": {
            "eliminationHalfLife": (11, 11, "h", "label pharmacokinetics"),
        },
        "doseReference": ("Insomnia SmPC context", "0.5 to 1.5 mg before retiring in cited SmPC context", "single bedtime dose; shortest possible duration in cited SmPC", 0.5, 1.5),
        "doseBar": (0.5, 0.5, 1, 1.5),
    },
    "Moclobemide": {
        "emcProductId": "7295",
        "emcTitle": "UK eMC SmPC: Moclobemide 150 mg film-coated tablets",
        "timeCourse": {
            "tmax": (1, 1, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (1, 4, "h", "label pharmacokinetics"),
            "timeToSteadyState": (7, 7, "day", "label pharmacokinetics", "SmPC states concentrations increase over the first week and remain stable thereafter."),
        },
        "doseReference": ("Major depression / social phobia SmPC contexts", "150 to 600 mg/day across cited SmPC contexts", "divided doses after meals in cited SmPC", 150, 600),
        "doseBar": (150, 300, 450, 600),
    },
    "N-Acetylcysteine": {
        "openfdaSetId": "2324d60b-4f49-4759-a46e-a06bfce7e8aa",
        "route": "intravenous",
        "formulation": "injection",
        "timeCourse": {
            "eliminationHalfLife": (5.6, 5.6, "h", "label pharmacokinetics", "Acetaminophen-overdose IV label context; psychiatric research use may not share this dosing context."),
        },
        "customDoseUseReferences": [
            {
                "indication": "Acetaminophen overdose intravenous label context",
                "route": "intravenous",
                "formulation": "injection",
                "amountText": "150 mg/kg loading dose followed by 50 mg/kg and 100 mg/kg infusion doses over the cited 21-hour label protocol",
                "scheduleText": "three-dose IV protocol; acetaminophen nomogram and clinical context determine use in cited label",
                "ranges": [
                    {
                        "min": 50,
                        "max": 150,
                        "unit": "mg/kg",
                        "basis": "weight-based",
                        "frequency": "three-dose 21-hour protocol context",
                        "rangeKind": "label-regimen",
                        "label": "IV acetaminophen-overdose protocol dose components",
                        "note": "Antidote label protocol, not a psychiatric NAC regimen.",
                    }
                ],
            }
        ],
    },
    "Prochlorperazine": {
        "emcProductId": "101549",
        "emcTitle": "UK eMC SmPC: Prochlorperazine maleate 3 mg Buccal Tablets",
        "route": "buccal",
        "formulation": "buccal tablet",
        "timeCourse": {
            "eliminationHalfLife": (9, 9, "h", "label pharmacokinetics", "Buccal formulation half-life similar to oral formulation in cited SmPC."),
        },
        "doseReference": ("Migraine/nausea buccal SmPC context", "3 to 12 mg/day in cited adult buccal SmPC context", "one or two 3 mg buccal tablets twice daily for up to two days in cited SmPC", 3, 12),
        "doseBar": (3, 3, 6, 12),
    },
    "Zopiclone": {
        "emcProductId": "2855",
        "emcTitle": "UK eMC SmPC: Zimovane 7.5 mg film-coated tablets",
        "timeCourse": {
            "tmax": (1.5, 2, "h", "label pharmacokinetics"),
            "eliminationHalfLife": (5, 5, "h", "label pharmacokinetics"),
        },
        "doseReference": ("Insomnia oral SmPC context", "3.75 to 7.5 mg at night in cited SmPC context", "single bedtime dose; lower dose in older adults or impaired hepatic/renal/respiratory function in cited SmPC context", 3.75, 7.5),
        "doseBar": (3.75, 3.75, 7.5, 7.5),
    },
    "Acamprosate": {
        "doseReference": ("Alcohol dependence maintenance of abstinence label context", "999 to 1998 mg/day across cited renal-adjusted and usual adult label contexts", "333 to 666 mg three times daily depending on renal function in cited label", 999, 1998),
        "doseBar": (333, 999, 1998, 1998),
    },
    "Aripiprazole": {
        "doseReference": ("Schizophrenia oral adult label context", "10 to 30 mg/day in cited adult schizophrenia label context", "once daily without regard to meals; dose changes no more often than every 2 weeks in cited label", 10, 30),
        "doseBar": (2, 10, 15, 30),
    },
    "Asenapine": {
        "doseReference": ("Bipolar mania sublingual label context", "10 to 20 mg/day in cited adult adjunctive bipolar mania label context", "5 to 10 mg sublingually twice daily; do not swallow tablet in cited label", 10, 20),
        "doseBar": (5, 10, 15, 20),
    },
    "Atomoxetine": {
        "doseReference": ("ADHD adult and >70 kg label context", "40 to 100 mg/day in cited adult and >70 kg label context", "once daily or divided morning and late afternoon/early evening in cited label", 40, 100),
        "doseBar": (10, 40, 80, 100),
    },
    "Brexpiprazole": {
        "doseReference": ("MDD adjunctive / schizophrenia / dementia-agitation label contexts", "0.5 to 4 mg/day across cited adult label contexts", "once daily with or without food; indication-specific titration in cited label", 0.5, 4),
        "doseBar": (0.5, 1, 2, 4),
    },
    "Buprenorphine": {
        "doseReference": ("Opioid use disorder sublingual induction/maintenance label context", "2 to 24 mg/day across cited induction and maintenance label contexts", "single daily sublingual dose; protocol- and withdrawal-state-dependent in cited label", 2, 24),
        "doseBar": (2, 4, 16, 24),
    },
    "Carbamazepine": {
        "doseReference": ("Seizure disorder / neuralgia / bipolar-related label contexts", "200 to 1600 mg/day across cited adult label contexts", "divided dosing with meals; formulation-specific conversion caveats in cited label", 200, 1600),
        "doseBar": (100, 200, 800, 1600),
    },
    "Cariprazine": {
        "doseReference": ("Schizophrenia / bipolar disorder / MDD adjunctive adult label contexts", "1.5 to 6 mg/day across cited adult label contexts", "once daily; dose changes reflect slowly because of long half-life in cited label", 1.5, 6),
        "doseBar": (1.5, 1.5, 3, 6),
    },
    "Clobazam": {
        "doseReference": ("Lennox-Gastaut syndrome adjunctive therapy label context", "5 to 40 mg/day across cited body-weight label contexts", "daily dose above 5 mg divided twice daily; titrate no faster than weekly in cited label", 5, 40),
        "doseBar": (5, 5, 20, 40),
    },
    "Clomipramine": {
        "doseReference": ("Obsessive-compulsive disorder label context", "25 to 250 mg/day across cited adult label titration and maximum contexts", "initially divided with meals; may be given once daily at bedtime after titration in cited label", 25, 250),
        "doseBar": (25, 25, 100, 250),
    },
    "Clonazepam": {
        "doseReference": ("Seizure disorder / panic disorder oral label contexts", "0.5 to 20 mg/day across cited adult panic and seizure label contexts", "divided dosing for seizure context or twice daily for panic context in cited label", 0.5, 20),
        "doseBar": (0.25, 0.5, 4, 20),
    },
    "Clorazepate": {
        "doseReference": ("Anxiety / alcohol withdrawal / seizure-adjunct label contexts", "7.5 to 90 mg/day across cited adult label contexts", "divided dosing or bedtime dosing depending on indication in cited label", 7.5, 90),
        "doseBar": (7.5, 15, 60, 90),
    },
    "Daridorexant": {
        "doseReference": ("Insomnia adult label context", "25 to 50 mg/night in cited adult label context", "once nightly within 30 minutes before bed with at least 7 hours before planned awakening", 25, 50),
        "doseBar": (25, 25, 50, 50),
    },
    "Desvenlafaxine": {
        "doseReference": ("Major depressive disorder adult label context", "25 to 100 mg/day across cited adult, discontinuation, renal, and hepatic label contexts", "once daily; tablets swallowed whole in cited label", 25, 100),
        "doseBar": (25, 50, 50, 100),
    },
    "Dexmethylphenidate": {
        "customDoseUseReferences": [
            {
                "indication": "ADHD Azstarys fixed-combination label context",
                "route": "oral",
                "formulation": "serdexmethylphenidate/dexmethylphenidate capsule",
                "amountText": "7.8 to 10.4 mg/day dexmethylphenidate component in cited fixed-combination label context",
                "scheduleText": "once daily in the morning; do not substitute mg-for-mg with other methylphenidate products in cited label",
                "ranges": [
                    {
                        "min": 7.8,
                        "max": 10.4,
                        "unit": "mg",
                        "basis": "component-dose",
                        "frequency": "once daily",
                        "rangeKind": "label-regimen",
                        "label": "Dexmethylphenidate component",
                        "note": "Component-aware display only; this is not a standalone dexmethylphenidate equivalence.",
                    }
                ],
            }
        ],
    },
    "Dextroamphetamine": {
        "doseReference": ("ADHD / narcolepsy stimulant label contexts", "2.5 to 60 mg/day across cited ADHD and narcolepsy label contexts", "divided dosing; late evening doses avoided in cited label", 2.5, 60),
        "doseBar": (2.5, 5, 20, 40),
    },
    "Divalproex": {
        "customDoseUseReferences": [
            {
                "indication": "Mania / seizure extended-release label contexts",
                "route": "oral",
                "formulation": "extended-release tablet",
                "amountText": "25 to 60 mg/kg/day across cited mania initial and maximum label contexts",
                "scheduleText": "once daily extended-release dosing; plasma-level and indication-specific titration in cited label",
                "ranges": [
                    {
                        "min": 25,
                        "max": 60,
                        "unit": "mg/kg/day",
                        "basis": "weight-based",
                        "frequency": "once daily extended-release context",
                        "rangeKind": "label-regimen",
                        "label": "Weight-based label range",
                        "note": "Weight-based range; do not render as scalar mg/day.",
                    }
                ],
            }
        ],
    },
    "Doxepin": {
        "doseReference": ("Insomnia low-dose tablet label context", "3 to 6 mg/night in cited insomnia label context", "once daily within 30 minutes of bedtime in cited label", 3, 6),
        "doseBar": (3, 3, 6, 6),
    },
    "Escitalopram": {
        "doseReference": ("Major depressive disorder / generalized anxiety disorder label contexts", "10 to 20 mg/day in cited adult label contexts", "once daily morning or evening with or without food in cited label", 10, 20),
        "doseBar": (5, 10, 15, 20),
    },
    "Estazolam": {
        "doseReference": ("Insomnia oral label context", "0.5 to 2 mg at bedtime across cited adult and geriatric label contexts", "single bedtime dose in cited label", 0.5, 2),
        "doseBar": (0.5, 1, 2, 2),
    },
    "Eszopiclone": {
        "doseReference": ("Insomnia oral label context", "1 to 3 mg at bedtime in cited adult label context", "single dose immediately before bedtime with 7 to 8 hours before awakening in cited label", 1, 3),
        "doseBar": (1, 1, 2, 3),
    },
    "Flurazepam": {
        "doseReference": ("Insomnia oral label context", "15 to 30 mg at bedtime in cited adult label context", "single bedtime dose; sex- and geriatric-specific starting caveats in cited label", 15, 30),
        "doseBar": (15, 15, 30, 30),
    },
    "Fluvoxamine": {
        "doseReference": ("Obsessive-compulsive disorder extended-release label context", "100 to 300 mg/day in cited adult label context", "once daily at bedtime; weekly 50 mg increases as tolerated in cited label", 100, 300),
        "doseBar": (50, 100, 200, 300),
    },
    "Gabapentin": {
        "doseReference": ("Postherpetic neuralgia / partial-onset seizure label contexts", "300 to 3600 mg/day across cited adult label contexts", "titrated and divided dosing; renal adjustment required in cited label", 300, 3600),
        "doseBar": (100, 300, 1800, 3600),
    },
    "Galantamine": {
        "doseReference": ("Alzheimer disease extended-release label context", "8 to 24 mg/day in cited label context", "once daily in the morning with food; titrate after minimum 4-week intervals in cited label", 8, 24),
        "doseBar": (4, 8, 16, 24),
    },
    "Guanfacine": {
        "doseReference": ("Hypertension immediate-release label context", "1 to 3 mg/day in cited antihypertensive label context", "once daily at bedtime; higher doses increase adverse reactions in cited label", 1, 3),
        "doseBar": (1, 1, 2, 3),
    },
    "Lamotrigine": {
        "doseReference": ("Epilepsy / bipolar disorder label contexts", "25 to 400 mg/day across cited adult titration and maintenance label contexts", "titration depends strongly on valproate, enzyme inducers, and estrogen-containing products in cited label", 25, 400),
        "doseBar": (25, 25, 200, 400),
    },
    "Lemborexant": {
        "doseReference": ("Insomnia adult label context", "5 to 10 mg/night in cited adult label context", "once nightly immediately before bed with at least 7 hours before awakening in cited label", 5, 10),
        "doseBar": (5, 5, 10, 10),
    },
    "Lisdexamfetamine": {
        "doseReference": ("ADHD / binge eating disorder adult label contexts", "30 to 70 mg/day across cited label contexts", "once daily in the morning with or without food in cited label", 30, 70),
        "doseBar": (10, 30, 50, 70),
    },
    "Lorazepam": {
        "doseReference": ("Anxiety / insomnia due to anxiety oral label contexts", "1 to 10 mg/day across cited adult oral label contexts", "divided dosing for anxiety or single bedtime dose for insomnia context in cited label", 1, 10),
        "doseBar": (0.5, 1, 4, 10),
    },
    "Lurasidone": {
        "doseReference": ("Schizophrenia / bipolar depression adult label contexts", "20 to 160 mg/day across cited adult label contexts", "once daily with at least 350 calories of food in cited label", 20, 160),
        "doseBar": (20, 40, 80, 160),
    },
    "Memantine": {
        "doseReference": ("Alzheimer disease oral label context", "5 to 20 mg/day in cited oral tablet label context", "titrate by 5 mg increments no more often than weekly in cited label", 5, 20),
        "doseBar": (5, 5, 10, 20),
    },
    "Milnacipran": {
        "doseReference": ("Fibromyalgia oral label context", "12.5 to 200 mg/day across cited titration and upper label contexts", "divided twice daily after titration in cited label", 12.5, 200),
        "doseBar": (12.5, 50, 100, 200),
    },
    "Midazolam": {
        "customDoseUseReferences": [
            {
                "indication": "Preoperative sedation/anxiolysis/amnesia injection label context",
                "route": "intramuscular or intravenous",
                "formulation": "injection",
                "amountText": "IM 0.07 to 0.08 mg/kg or IV titrated doses usually not exceeding 5 mg in cited adult label contexts",
                "scheduleText": "requires continuous respiratory and cardiac monitoring in cited label",
                "ranges": [
                    {
                        "min": 0.07,
                        "max": 0.08,
                        "unit": "mg/kg",
                        "basis": "weight-based",
                        "frequency": "single IM premedication context",
                        "rangeKind": "label-regimen",
                        "label": "IM premedication label range",
                        "note": "Procedural/anesthesia context, not outpatient hypnotic use.",
                    },
                    {
                        "min": 1,
                        "max": 5,
                        "unit": "mg",
                        "basis": "per-dose",
                        "frequency": "IV titration context",
                        "rangeKind": "label-regimen",
                        "label": "IV titration label context",
                        "note": "Procedural/anesthesia context with monitoring requirements.",
                    },
                ],
            }
        ],
    },
    "Mirtazapine": {
        "doseReference": ("Major depressive disorder oral label context", "15 to 45 mg/day in cited adult label context", "once daily, preferably in the evening before sleep, in cited label", 15, 45),
        "doseBar": (7.5, 15, 30, 45),
    },
    "Modafinil": {
        "doseReference": ("Narcolepsy / obstructive sleep apnea / shift-work disorder label contexts", "200 to 400 mg/day across cited adult label contexts", "once daily in the morning for narcolepsy/OSA or about 1 hour before shift work in cited label", 200, 400),
        "doseBar": (100, 200, 200, 400),
    },
    "Methadone": {
        "openfdaSetId": "14f7b1d8-344a-4eaa-a1bf-bc1a00c3ab58",
        "customDoseUseReferences": [
            {
                "indication": "Opioid use disorder oral concentrate label context",
                "route": "oral",
                "formulation": "oral concentrate",
                "amountText": "20 to 30 mg initial single dose and 80 to 120 mg/day common maintenance-stability range in cited label context",
                "scheduleText": "dispensed under opioid-treatment-program requirements; first-day total ordinarily not above 40 mg in cited label",
                "ranges": [
                    {
                        "min": 20,
                        "max": 30,
                        "unit": "mg",
                        "basis": "per-dose",
                        "frequency": "initial single-dose context",
                        "rangeKind": "label-regimen",
                        "label": "Initial single-dose label context",
                        "note": "OUD protocol context; not for self-adjustment.",
                    },
                    {
                        "min": 80,
                        "max": 120,
                        "unit": "mg",
                        "basis": "daily-total",
                        "frequency": "maintenance context",
                        "rangeKind": "label-regimen",
                        "label": "Maintenance-stability label context",
                        "note": "OUD protocol context; clinical stability range is not an initiation target.",
                    },
                ],
            }
        ],
    },
    "Naltrexone": {
        "doseReference": ("Alcohol dependence oral label context", "50 mg/day in cited alcoholism label context", "once daily as part of broader treatment context in cited label", 50, 50),
        "doseBar": (25, 50, 50, 50),
    },
    "Naloxone": {
        "openfdaSetId": "139c30b9-5600-48f5-8610-0cacd2782398",
        "customDoseUseReferences": [
            {
                "indication": "Known or suspected opioid overdose nasal spray label context",
                "route": "intranasal",
                "formulation": "nasal spray",
                "amountText": "4 mg single intranasal spray; additional sprays may be given every 2 to 3 minutes until emergency help arrives in cited label",
                "scheduleText": "seek emergency medical care immediately after use in cited label",
                "ranges": [
                    {
                        "min": 4,
                        "max": 4,
                        "unit": "mg",
                        "basis": "per-dose",
                        "frequency": "repeat every 2 to 3 minutes as needed in overdose emergency context",
                        "rangeKind": "label-regimen",
                        "label": "Nasal spray per-dose label context",
                        "note": "Emergency overdose reversal context.",
                    }
                ],
            }
        ],
    },
    "Oxazepam": {
        "doseReference": ("Anxiety / alcohol withdrawal oral label contexts", "30 to 120 mg/day across cited adult label contexts", "three or four divided doses in cited label", 30, 120),
        "doseBar": (10, 30, 60, 120),
    },
    "Oxcarbazepine": {
        "doseReference": ("Partial-onset seizure adult oral label context", "600 to 2400 mg/day across cited adult monotherapy/adjunctive label contexts", "twice daily; titration and renal adjustment in cited label", 600, 2400),
        "doseBar": (300, 600, 1200, 2400),
    },
    "Paliperidone": {
        "doseReference": ("Schizophrenia / schizoaffective disorder extended-release label contexts", "3 to 12 mg/day in cited adult label contexts", "once daily; tablets swallowed whole in cited label", 3, 12),
        "doseBar": (1.5, 3, 6, 12),
    },
    "Pentobarbital": {
        "customDoseUseReferences": [
            {
                "indication": "Sedative-hypnotic parenteral label context",
                "route": "intramuscular",
                "formulation": "injection",
                "amountText": "150 to 200 mg as a single adult IM hypnotic dose in cited label context",
                "scheduleText": "single IM dose; parenteral route only when oral administration is impossible or impractical in cited label",
                "ranges": [
                    {
                        "min": 150,
                        "max": 200,
                        "unit": "mg",
                        "basis": "per-dose",
                        "frequency": "single IM hypnotic dose context",
                        "rangeKind": "label-regimen",
                        "label": "IM hypnotic per-dose label context",
                        "note": "Parenteral barbiturate label context with monitoring requirements.",
                    }
                ],
            }
        ],
    },
    "Perphenazine": {
        "doseReference": ("Schizophrenia oral label context", "12 to 64 mg/day across cited adult schizophrenia label contexts", "divided dosing; prolonged doses above 24 mg/day reserved for monitored contexts in cited label", 12, 64),
        "doseBar": (4, 12, 24, 64),
    },
    "Phenelzine": {
        "doseReference": ("Depression MAOI oral label context", "15 to 90 mg/day across cited initial, early-phase, and maintenance label contexts", "divided dosing; maintenance may be reduced after maximum benefit in cited label", 15, 90),
        "doseBar": (15, 45, 60, 90),
    },
    "Phenobarbital": {
        "openfdaSetId": "057992c1-29a0-4229-9c30-d787cb632eac",
        "doseReference": ("Sedation / hypnotic / anticonvulsant oral label contexts", "30 to 400 mg/day across cited adult oral label contexts", "divided daytime sedation, oral hypnotic single-dose, or anticonvulsant contexts in cited label", 30, 400),
        "doseBar": (30, 60, 200, 400),
    },
    "Pimavanserin": {
        "doseReference": ("Parkinson disease psychosis oral label context", "34 mg/day in cited label context", "once daily with or without food in cited label", 34, 34),
        "doseBar": (10, 34, 34, 34),
    },
    "Pimozide": {
        "doseReference": ("Tourette disorder oral label context", "1 to 10 mg/day across cited adult/pediatric tic-suppression label context", "divided dosing; ECG and CYP2D6 caveats in cited label", 1, 10),
        "doseBar": (1, 2, 4, 10),
    },
    "Pitolisant": {
        "doseReference": ("Narcolepsy adult oral label context", "8.9 to 35.6 mg/day across cited adult label titration context", "once daily in the morning upon awakening in cited label", 8.9, 35.6),
        "doseBar": (4.45, 8.9, 17.8, 35.6),
    },
    "Pregabalin": {
        "doseReference": ("Neuropathic pain / fibromyalgia / seizure-adjunct label contexts", "150 to 600 mg/day across cited adult label contexts", "two or three divided doses; renal adjustment required in cited label", 150, 600),
        "doseBar": (75, 150, 300, 600),
    },
    "Ramelteon": {
        "doseReference": ("Insomnia adult label context", "8 mg/day in cited adult label context", "within 30 minutes of going to bed; avoid high-fat meal timing in cited label", 8, 8),
        "doseBar": (4, 8, 8, 8),
    },
    "Rivastigmine": {
        "doseReference": ("Alzheimer disease / Parkinson disease dementia oral capsule label contexts", "3 to 12 mg/day across cited oral capsule label contexts", "twice daily with meals in cited label", 3, 12),
        "doseBar": (1.5, 3, 6, 12),
    },
    "Solriamfetol": {
        "doseReference": ("Narcolepsy / obstructive sleep apnea adult label contexts", "37.5 to 150 mg/day across cited adult label contexts", "once daily upon awakening; avoid within 9 hours of bedtime in cited label", 37.5, 150),
        "doseBar": (37.5, 75, 150, 150),
    },
    "Suvorexant": {
        "doseReference": ("Insomnia adult label context", "10 to 20 mg/night in cited adult label context", "once nightly within 30 minutes before bed with at least 7 hours before awakening in cited label", 10, 20),
        "doseBar": (5, 10, 20, 20),
    },
    "Triazolam": {
        "doseReference": ("Insomnia oral label context", "0.125 to 0.5 mg/night across cited adult and geriatric label contexts", "single bedtime dose; quantities should not exceed 1-month supply in cited label", 0.125, 0.5),
        "doseBar": (0.125, 0.25, 0.5, 0.5),
    },
    "Valproate": {
        "customDoseUseReferences": [
            {
                "indication": "Seizure disorder oral solution label context",
                "route": "oral",
                "formulation": "oral solution",
                "amountText": "10 to 60 mg/kg/day across cited initial and upper label contexts",
                "scheduleText": "increase by 5 to 10 mg/kg/week; plasma-level and clinical-response context in cited label",
                "ranges": [
                    {
                        "min": 10,
                        "max": 60,
                        "unit": "mg/kg/day",
                        "basis": "weight-based",
                        "frequency": "daily total weight-based context",
                        "rangeKind": "label-regimen",
                        "label": "Weight-based label range",
                        "note": "Weight-based range; do not render as scalar mg/day.",
                    }
                ],
            }
        ],
    },
    "Varenicline": {
        "doseReference": ("Smoking cessation oral label context", "0.5 to 2 mg/day across cited starting-week and continuing-week label contexts", "titrated from once daily to twice daily in cited label", 0.5, 2),
        "doseBar": (0.5, 0.5, 1, 2),
    },
    "Viloxazine": {
        "doseReference": ("ADHD adult extended-release label context", "200 to 600 mg/day in cited adult label context", "once daily; weekly 200 mg titration increments in cited label", 200, 600),
        "doseBar": (100, 200, 400, 600),
    },
    "Vortioxetine": {
        "doseReference": ("Major depressive disorder adult label context", "5 to 20 mg/day across cited adult label contexts", "once daily without regard to meals in cited label", 5, 20),
        "doseBar": (5, 10, 15, 20),
    },
    "Zaleplon": {
        "doseReference": ("Insomnia oral label context", "5 to 20 mg/night across cited adult label contexts", "immediately before bedtime or after difficulty falling asleep in cited label", 5, 20),
        "doseBar": (5, 10, 20, 20),
    },
    "Ziprasidone": {
        "doseReference": ("Schizophrenia / bipolar mania oral capsule label contexts", "40 to 160 mg/day across cited adult label contexts", "twice daily with food in cited label", 40, 160),
        "doseBar": (20, 40, 80, 160),
    },
    "Bromazepam": {
        "extraSourceRefs": [
            {
                "title": "HPRA SmPC: Lexotan 1.5 mg Tablets",
                "url": "https://assets.hpra.ie/products/Human/30761/Licence_PA2239-008-001_07052020082419.pdf",
                "sourceType": "regulatory-label",
                "accessedDate": ACCESSED_DATE,
                "evidenceLevel": "REGULATORY_LABEL",
                "labelSection": "4.2 Posology and method of administration; 5.2 Pharmacokinetic properties",
                "note": "Irish HPRA Summary of Product Characteristics.",
            }
        ],
        "timeCourse": {
            "tmax": (2, 2, "h", "label pharmacokinetics", "SmPC states peak plasma concentrations are reached within 2 hours after oral administration."),
            "eliminationHalfLife": (20, 20, "h", "label pharmacokinetics", "SmPC states elimination half-life is about 20 hours."),
            "timeToSteadyState": (5, 9, "day", "label pharmacokinetics", "SmPC states steady-state plasma concentrations are reached in around 5 to 9 days."),
        },
        "doseReference": ("Anxiety oral SmPC context", "3 to 18 mg/day usual general-practice range; up to 60 mg/day only in exceptional hospitalised-patient SmPC context", "divided doses in cited SmPC context", 3, 60),
        "doseBar": (1.5, 3, 18, 60),
    },
    "Etizolam": {
        "extraSourceRefs": [
            {
                "title": "DEA Drug & Chemical Evaluation Section: Etizolam",
                "url": "https://www.deadiversion.usdoj.gov/drug_chem_info/etizolam.pdf",
                "sourceType": "government-safety-monograph",
                "accessedDate": ACCESSED_DATE,
                "evidenceLevel": "GOVERNMENT_SAFETY_MONOGRAPH",
                "labelSection": "Pharmacology",
                "note": "US DEA pharmacology summary; etizolam is not approved for medical use in the United States.",
            },
            {
                "title": "PMDA report: Benzodiazepine receptor agonists package-insert revisions",
                "url": "https://www.pmda.go.jp/files/000217229.pdf",
                "sourceType": "regulatory-safety-report",
                "accessedDate": ACCESSED_DATE,
                "evidenceLevel": "REGULATORY_SAFETY_REPORT",
                "labelSection": "Appendix etizolam dosing table",
                "note": "Japanese PMDA package-insert revision report with labeled adult dosage contexts.",
            },
        ],
        "timeCourse": {
            "tmax": (0.5, 2, "h", "literature estimate", "DEA summary cites a single-dose human PK study with maximum plasma concentration within 0.5 to 2 hours."),
            "eliminationHalfLife": (3.4, 3.4, "h", "literature estimate", "DEA summary cites a mean elimination half-life averaging 3.4 hours."),
        },
        "doseReference": ("Japanese labeled anxiety/depression/insomnia contexts", "1.5 to 3 mg/day across cited PMDA package-insert contexts", "divided three times daily for neurosis/depression or once nightly for sleep-disorder context in cited PMDA report", 1.5, 3),
        "doseSourceType": "regulatory-safety-report",
        "doseEvidenceLevel": "REGULATORY_SAFETY_REPORT",
        "doseRangeKind": "label-regimen",
        "doseBar": (0.5, 1.5, 3, 3),
    },
    "Flunitrazepam": {
        "extraSourceRefs": [
            {
                "title": "HPRA SmPC: Rohypnol 1 mg film-coated tablets",
                "url": "https://assets.hpra.ie/products/Human/14097/LicenseSPC_PA0050-008-001_11042011141024.pdf",
                "sourceType": "regulatory-label",
                "accessedDate": ACCESSED_DATE,
                "evidenceLevel": "REGULATORY_LABEL",
                "labelSection": "4.2 Posology and method of administration; 5.2 Pharmacokinetic properties",
                "note": "Irish HPRA Summary of Product Characteristics.",
            }
        ],
        "timeCourse": {
            "eliminationHalfLife": (16, 35, "h", "label pharmacokinetics", "SmPC states elimination half-life may be between 16 and 35 hours and onset is rapid but not numerically specified."),
        },
        "doseReference": ("Short-term insomnia oral SmPC context", "0.5 to 2 mg at bedtime in cited adult SmPC context", "single dose just before going to bed in cited SmPC context", 0.5, 2),
        "doseBar": (0.5, 0.5, 1, 2),
    },
    "Maprotiline": {
        "extraSourceRefs": [
            {
                "title": "FDA label: Maprotiline hydrochloride tablets, USP",
                "url": "https://www.accessdata.fda.gov/drugsatfda_docs/label/2014/072285s021lbl.pdf",
                "sourceType": "regulatory-label",
                "accessedDate": ACCESSED_DATE,
                "evidenceLevel": "REGULATORY_LABEL",
                "labelSection": "Clinical Pharmacology; Dosage and Administration",
                "note": "FDA label available through Drugs@FDA/accessdata.",
            }
        ],
        "timeCourse": {
            "tmax": (12, 12, "h", "label pharmacokinetics", "FDA label states mean time to peak is 12 hours."),
            "eliminationHalfLife": (51, 51, "h", "label pharmacokinetics", "FDA label states elimination half-life averages 51 hours."),
            "timeToSteadyState": (14, 14, "day", "label pharmacokinetics", "FDA label states steady state is reached in the second week."),
        },
        "doseReference": ("Depressive illness oral FDA label context", "75 to 225 mg/day across cited outpatient, hospitalized, and maximum label contexts", "single daily or divided dosing in cited label context", 75, 225),
        "doseBar": (25, 75, 150, 225),
    },
    "Prazepam": {
        "extraSourceRefs": [
            {
                "title": "HPRA SmPC: Centrax 10 mg Tablets",
                "url": "https://assets.hpra.ie/products/Human/21640/Licence_PA0822-010-001_25052023120251.pdf",
                "sourceType": "regulatory-label",
                "accessedDate": ACCESSED_DATE,
                "evidenceLevel": "REGULATORY_LABEL",
                "labelSection": "4.2 Posology and method of administration; 5.2 Pharmacokinetic properties",
                "note": "Irish HPRA Summary of Product Characteristics.",
            }
        ],
        "timeCourse": {
            "eliminationHalfLife": (63, 70, "h", "label pharmacokinetics", "SmPC reports mean half-life of the principal active metabolite norprazepam as 63 hours before and 70 hours after multiple dosing."),
        },
        "doseReference": ("Anxiety oral SmPC context", "10 to 60 mg/day in cited adult SmPC context", "single or divided doses in cited SmPC context", 10, 60),
        "doseBar": (10, 10, 30, 60),
    },
    "Tianeptine": {
        "extraSourceRefs": [
            {
                "title": "PubMed: The pharmacokinetics of the antidepressant tianeptine and its main metabolite in healthy humans",
                "url": "https://pubmed.ncbi.nlm.nih.gov/2341111/",
                "sourceType": "pharmacokinetic-study",
                "accessedDate": ACCESSED_DATE,
                "evidenceLevel": "PHARMACOKINETIC_STUDY",
                "labelSection": "Abstract",
                "note": "Human pharmacokinetic study after oral dosing.",
            },
            {
                "title": "UIC Drug Information Group FAQ: tianeptine misuse/withdrawal",
                "url": "https://dig.pharmacy.uic.edu/faqs/2024-2/june-2024-faqs/what-is-tianeptine-and-are-there-recommendations-for-managing-tianeptine-misuse-withdrawal-in-the-medical-setting/",
                "sourceType": "clinical-review",
                "accessedDate": ACCESSED_DATE,
                "evidenceLevel": "CLINICAL_REVIEW",
                "labelSection": "Background",
                "note": "Review used only to index broad medical-use dose context and misuse caveats, not as a dosing recommendation.",
            },
        ],
        "timeCourse": {
            "tmax": (0.47, 1.41, "h", "literature estimate", "Human PK study reports tmax 0.94 +/- 0.47 hours after oral dosing."),
            "eliminationHalfLife": (1.4, 3.6, "h", "literature estimate", "Human PK study reports plasma half-life 2.5 +/- 1.1 hours."),
        },
        "doseReference": ("Medical-use literature context", "25 to 50 mg/day medical-use context cited in review literature", "product- and country-specific schedule; often divided dosing in clinical references", 25, 50),
        "doseSourceType": "clinical-review",
        "doseEvidenceLevel": "CLINICAL_REVIEW",
        "doseRangeKind": "literature-regimen",
        "doseBar": (12.5, 25, 37.5, 50),
    },
    "Tofisopam": {
        "extraSourceRefs": [
            {
                "title": "PubMed: Pharmacokinetics and metabolism of tofisopam (Grandaxin)",
                "url": "https://pubmed.ncbi.nlm.nih.gov/8100113/",
                "sourceType": "pharmacokinetic-study",
                "accessedDate": ACCESSED_DATE,
                "evidenceLevel": "PHARMACOKINETIC_STUDY",
                "labelSection": "Abstract",
                "note": "Human pharmacokinetic and metabolism study.",
            },
            {
                "title": "Kusuri-no-Shiori: GRANDAXIN Tablets 50",
                "url": "https://www.rad-ar.or.jp/siori/english/search/result?n=44482",
                "sourceType": "drug-information-sheet",
                "accessedDate": ACCESSED_DATE,
                "evidenceLevel": "DRUG_INFORMATION_SHEET",
                "labelSection": "Dosage regimen",
                "note": "Japanese patient drug information sheet used for labeled dosage context.",
            },
        ],
        "timeCourse": {
            "eliminationHalfLife": (2.7, 3.5, "h", "literature estimate", "Human PK study abstract reports unchanged tofisopam plasma half-life of 2.7 to 3.5 hours; total radioactivity declines more slowly."),
        },
        "doseReference": ("Japanese labeled autonomic/anxiety symptom context", "150 mg/day in cited Japanese drug-information sheet context", "50 mg three times daily; adjusted by age or symptoms in cited sheet", 150, 150),
        "doseSourceType": "drug-information-sheet",
        "doseEvidenceLevel": "DRUG_INFORMATION_SHEET",
        "doseRangeKind": "label-regimen",
        "doseBar": (50, 150, 150, 150),
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


CURATED_LABEL_DATA["Amitriptyline"]["timeCourse"]["eliminationHalfLife"] = (
    16.49,
    40.36,
    "h",
    "label pharmacokinetics",
    "SmPC reports mean plasma half-life about 25 hours with this range.",
)

CURATED_LABEL_DATA.setdefault("Pimavanserin", {}).setdefault("timeCourse", {}).update(
    {
        "tmax": (4, 24, "h", "label pharmacokinetics", "Median Tmax is 6 hours with a 4 to 24 hour range."),
        "eliminationHalfLife": (
            57,
            57,
            "h",
            "label pharmacokinetics",
            "Mean pimavanserin half-life; active metabolite mean half-life is about 200 hours.",
        ),
    }
)

CURATED_LABEL_DATA.setdefault("Clobazam", {}).setdefault("timeCourse", {}).update(
    {
        "tmax": (0.5, 4, "h", "label pharmacokinetics"),
        "eliminationHalfLife": (
            36,
            42,
            "h",
            "label pharmacokinetics",
            "Estimated mean clobazam half-life; N-desmethylclobazam half-life is about 71 to 82 hours.",
        ),
    }
)

CURATED_LABEL_DATA.setdefault("Viloxazine", {}).setdefault("timeCourse", {}).update(
    {
        "tmax": (3, 9, "h", "label pharmacokinetics", "Median Tmax about 5 hours with a 3 to 9 hour range."),
        "eliminationHalfLife": (
            2.28,
            11.76,
            "h",
            "label pharmacokinetics",
            "Mean half-life 7.02 +/- 4.74 hours in the cited label.",
        ),
    }
)

CURATED_LABEL_DATA.setdefault("Solriamfetol", {}).setdefault("timeCourse", {}).update(
    {
        "tmax": (1.25, 3, "h", "label pharmacokinetics"),
        "eliminationHalfLife": (7.1, 7.1, "h", "label pharmacokinetics"),
        "timeToSteadyState": (3, 3, "day", "label pharmacokinetics"),
    }
)


def _load_auto_label_data() -> dict:
    if not AUTO_LABEL_DATA_PATH.exists():
        return {}
    return json.loads(AUTO_LABEL_DATA_PATH.read_text(encoding="utf-8"))


# 手工核对数据优先；批量 openFDA 缓存只补充尚未手工覆盖的条目。
MANUAL_CURATED_LABEL_DATA = CURATED_LABEL_DATA


def merge_curated_label_data(auto_data: dict, manual_data: dict) -> dict:
    merged = dict(auto_data)
    for name, manual in manual_data.items():
        base = dict(merged.get(name, {}))
        base.update(manual)
        merged[name] = base
    return merged


CURATED_LABEL_DATA = merge_curated_label_data(_load_auto_label_data(), MANUAL_CURATED_LABEL_DATA)


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


def curated_regulatory_sources(name: str, curated: dict | None) -> list[dict]:
    sources = [curated_regulatory_source(name, curated)]
    if curated:
        for set_id, title in curated.get("extraOpenfdaSetIds", []):
            sources.append(openfda_source(title.replace("openFDA drug label: ", ""), set_id))
            sources[-1]["title"] = title
        sources.extend(curated.get("extraSourceRefs", []))
    return sources


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
        "route": curated.get("route", "oral"),
        "formulation": curated.get("formulation", "product-specific oral formulation"),
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
    if not curated:
        return None
    if "customDoseUseReferences" in curated:
        refs = []
        for ref in curated["customDoseUseReferences"]:
            ref_copy = dict(ref)
            ref_copy.setdefault("population", "Adults unless product-specific label states otherwise")
            ref_copy.setdefault("sourceType", ref_copy.get("sourceType", "regulatory-label"))
            ref_copy.setdefault("evidenceLevel", ref_copy.get("evidenceLevel", "REGULATORY_LABEL"))
            ref_copy.setdefault("note", "Dose use references are not recommendations and must not be used for self-medication or dose adjustment.")
            ref_copy["sourceRefs"] = source_refs
            refs.append(ref_copy)
        return refs
    if "doseReference" not in curated:
        return None
    indication, amount_text, schedule_text, range_min, range_max = curated["doseReference"]
    source_type = curated.get("doseSourceType", "regulatory-label")
    evidence_level = curated.get("doseEvidenceLevel", "REGULATORY_LABEL")
    range_kind = curated.get("doseRangeKind", "label-regimen")
    return [
        {
            "indication": indication,
            "population": "Adults unless product-specific label states otherwise",
            "route": curated.get("doseRoute", curated.get("route", "oral")),
            "formulation": curated.get("doseFormulation", curated.get("formulation", "product-specific oral formulation")),
            "amountText": amount_text,
            "scheduleText": schedule_text,
            "ranges": [
                {
                    "min": range_min,
                    "max": range_max,
                    "unit": "mg",
                    "basis": "daily-total",
                    "frequency": schedule_text,
                    "rangeKind": range_kind,
                    "label": curated.get("doseRangeLabel", "Label/literature regimen range"),
                    "note": "Label/literature regimen range for reference indexing; not a dosing recommendation.",
                }
            ],
            "sourceType": source_type,
            "evidenceLevel": evidence_level,
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
    source_refs = group["sourceRefs"] + curated_regulatory_sources(name, curated)
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
