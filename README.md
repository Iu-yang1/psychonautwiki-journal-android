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

Medical disclaimer: This information is for educational reference and data indexing only. It is not medical advice and must not be used for diagnosis, prescribing, self-medication, or dose adjustment. Always consult qualified medical professionals and local approved labeling. 本资料仅用于学习和资料索引，不构成医疗建议，不用于诊断、处方、自行用药或调整剂量。实际用药必须遵循医生医嘱和当地批准说明书。

Recommended sources:

- DailyMed for labels, clinical pharmacology, pharmacokinetics, Tmax, Cmax, AUC, half-life, and steady state.
- openFDA drug label API for bulk screening and label-section extraction, with final verification against DailyMed or FDA source labels.
- Drugs@FDA for FDA-approved labels, application numbers, and label history.
- RxNorm / RxNav for normalized names, brand names, RxCUI, and NDC relationships.
- PubMed / NCBI E-utilities for reviews and pharmacokinetic literature.
- ESC, ACC/AHA, Chinese guidelines, Goodman & Gilman, AHFS, Martindale, and clinical pharmacology reviews for context.
- Laboratory TDM catalogs only for true TDM, therapeutic-window, or toxic-concentration fields; do not mix those values into `timeCourse`.

Cardiovascular example fragments live under `tools/drugdata/cardiovascular/` and are split by maintenance category, for example `cardiac_glycosides.json`, `antiarrhythmics.json`, `antithrombotics.json`, and `beta_blockers.json`. Rebuild the default and localized raw resources with:

```bash
python tools/drugdata/build_substances_json.py
```

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
