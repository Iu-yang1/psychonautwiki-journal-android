Copyright (C) 2022 Isaak Hanimann.

See the end of the file for license conditions.

# PsychonautWiki Journal

PsychonautWiki Journal is an Android app to make recreational drug users safer. The aim is to provide features that are attractive to users as well as useful from a harm-reduction perspective.
This app is built natively with [Jetpack Compose](https://developer.android.com/jetpack/compose).

This is an open source fork that maintains development after the project closed.

![A presentation of the App](https://github.com/huanli233/psychonautwiki-journal-android/blob/main/metadata/en-US/images/Google%20Pixel%204%20XL%20Presentation.png?raw=true)

Download the latest version from [GitHub releases](https://github.com/huanli233/psychonautwiki-journal-android/releases/latest).

## Clinical Pharmacology Data

The local `app/src/main/res/raw/substances.json` file also supports optional clinical pharmacology fields for cardiovascular and peripheral-circulation medicines. These fields are additive and old PsychonautWiki-style entries remain valid when they do not define them.

New optional fields on a substance:

```json
{
  "clinicalInfo": {
    "atcCodes": ["C08CA01"],
    "drugClass": ["Calcium channel blocker", "CCB"],
    "indications": ["Hypertension"],
    "contraindications": [],
    "majorWarnings": [],
    "majorInteractions": [],
    "monitoring": [],
    "sourceRefs": [
      {
        "title": "DailyMed label",
        "url": "https://dailymed.nlm.nih.gov/",
        "sourceType": "regulatory-label",
        "accessedDate": "2026-06-23"
      }
    ]
  },
  "timeCourse": [
    {
      "route": "oral",
      "formulation": "tablet",
      "tmax": {
        "min": 6,
        "max": 12,
        "unit": "h",
        "basis": "plasma concentration"
      },
      "durationOfAction": {
        "min": 24,
        "max": null,
        "unit": "h",
        "basis": "clinical effect"
      },
      "notes": ["Tmax and peak clinical effect may not be identical."],
      "sourceRefs": []
    }
  ],
  "tdm": {
    "isRoutinelyMonitored": true,
    "monitoringType": "serum concentration",
    "analytes": ["digoxin"],
    "specimen": "serum",
    "samplingTime": "At least 6 hours after the last dose; preferably trough at steady state when clinically feasible.",
    "therapeuticRanges": [
      {
        "indication": "heart failure literature-oriented lower range",
        "range": "0.5-0.9",
        "unit": "ng/mL",
        "note": "Example laboratory reference; local policy may differ."
      }
    ],
    "toxicityThresholds": [
      {
        "threshold": ">2.0",
        "unit": "ng/mL",
        "note": "Interpret with clinical context."
      }
    ],
    "criticalValues": [],
    "interpretationCaveats": [
      "Interpret with sampling time, renal function, electrolytes, ECG, symptoms, and interacting drugs."
    ],
    "sourceRefs": []
  }
}
```

`timeCourse` is for pharmacokinetic/pharmacodynamic time-course data such as onset, Tmax, peak effect, duration of action, elimination half-life, time to steady state, and washout. Do not mix this with therapeutic drug monitoring, toxic concentration, or treatment-window data.

`tdm` is for therapeutic drug monitoring and concentration interpretation. It is intentionally separate from `timeCourse`: PK describes average drug behavior, while TDM describes how a measured patient sample may be interpreted. Non-routine examples, such as amlodipine or metoprolol, should set `isRoutinelyMonitored` to `false` and explain that monitoring is usually based on clinical response or safety labs rather than plasma concentration. Warfarin should be represented as INR/PT effect monitoring, not routine plasma concentration TDM.

Medical disclaimer: This information is for educational reference and data indexing only. It is not medical advice and must not be used for diagnosis, prescribing, self-medication, or dose adjustment. Always consult qualified medical professionals and local approved labeling.

本资料仅用于学习和资料索引，不构成医疗建议，不用于诊断、处方、自行用药或调整剂量。实际用药必须咨询具备资质的医疗专业人员，并遵循当地批准说明书。

Recommended sources:

- DailyMed for labels, clinical pharmacology, pharmacokinetics, Tmax, Cmax, AUC, half-life, and steady state.
- openFDA drug label API for bulk screening and label-section extraction, with final verification against DailyMed or FDA source labels.
- Drugs@FDA for FDA-approved labels, application numbers, and label history.
- RxNorm / RxNav for normalized names, brand names, RxCUI, and NDC relationships.
- PubMed / NCBI E-utilities for reviews and pharmacokinetic literature.
- ESC, ACC/AHA, Chinese guidelines, Goodman & Gilman, AHFS, Martindale, and clinical pharmacology reviews for context.
- Laboratory TDM catalogs only for true TDM, therapeutic-window, or toxic-concentration fields; do not mix those values into `timeCourse`.

Cardiovascular example fragments live under `tools/drugdata/cardiovascular/` and are split by maintenance category, for example `cardiac_glycosides.json`, `antiarrhythmics.json`, `antithrombotics.json`, and `beta_blockers.json`.

## Endocrine / HRT data pack

The local Endocrine / HRT data pack contains route- and formulation-specific estradiol entries, conjugated estrogens, ethinylestradiol, estetrol, cyproterone acetate (CPA), antiandrogenic progestins, and other progestogens. Source files live under:

```text
tools/drugdata/endocrine/
  estrogens/
  antiandrogenic_progestins/
  progestogens/
```

The module is for education, source indexing, personal-record support, and preparation for possible future HRT trend-model research. It does not provide medical advice, recommend a dose, replace a clinician, predict E2 or testosterone, or adjust treatment.

New optional fields include:

- `endocrineInfo`: hormone class, mechanisms, affected hormones, monitoring labs, assay caveats, safety signals, model roles, and sources.
- `doseUseReferences`: label, guideline, or literature use references. These are not recommended doses. A `source needed` value means that no numeric regimen has been transcribed into the data pack.
- `hrtModelInfo`: a future-model compatibility and data-requirement marker only. It does not mean that the current app can predict hormone levels or guide treatment.
- Extended `timeCourse` flags for depot release, peak/trough windows, injection-interval sensitivity, and assay-timing sensitivity.

Estradiol and testosterone results can be affected by sampling time, route, formulation, injection interval, assay method, SHBG, albumin, liver or kidney function, and interacting medicines. Immunoassays and LC-MS/MS methods may not agree. Ethinylestradiol and conjugated estrogens must not be interpreted as ordinary serum 17beta-estradiol exposure and are marked incompatible with a standard serum E2 model.

Recommended endocrine sources include:

- Endocrine Society 2017 Clinical Practice Guideline.
- WPATH Standards of Care Version 8.
- UCSF Transgender Care Guidelines.
- DailyMed and FDA labels.
- EMA, MHRA, and UK EMC product information and safety communications.
- PubMed pharmacokinetic studies.
- Estradiol assay-accuracy literature comparing LC-MS/MS and immunoassays.
- CPA testosterone-suppression studies and regulatory meningioma safety communications.
- Systematic reviews of progestogen use in feminizing hormone therapy.

All generated raw resources use stable base files:

```text
app/src/main/res/raw/substances_base.json
app/src/main/res/raw-zh-rCN/substances_base.json
```

The generator recursively merges JSON data packs under `tools/drugdata/` into the application resources. Output files are never used as their own build input. Rebuild the default and localized raw resources with:

```bash
python tools/drugdata/build_endocrine_pack.py
python tools/drugdata/check_endocrine_pack.py
python tools/drugdata/build_substances_json.py
```

The base files normally do not need to be regenerated. `tools/drugdata/initialize_substances_base.py` is a migration utility for deliberately rebuilding the base boundary from an existing generated resource.

## License

    This file is part of PsychonautWiki Journal.
    
    PsychonautWiki Journal is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.
    
    PsychonautWiki Journal is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with PsychonautWiki Journal.  If not, see https://www.gnu.org/licenses/gpl-3.0.en.html.
