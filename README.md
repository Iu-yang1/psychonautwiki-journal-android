# PsychonautWiki Journal

PsychonautWiki Journal is an app to make recreational drug users safer. The aim is to provide features that are attractive to users as well as useful from a harm-reduction perspective.
This app is built natively with [Jetpack Compose](https://developer.android.com/jetpack/compose).

💻 Requirements
------------
To try out this app, clone this repository and open the root directory in [Android Studio](https://developer.android.com/studio).

## Cardiovascular data

The bundled offline substance database includes cardiovascular reference entries covering:

- cardiac glycosides, antiarrhythmics, antianginal medicines, antihypertensives, heart-failure therapies, and diuretics;
- antiplatelets, anticoagulants, thrombolytics, and GP IIb/IIIa inhibitors;
- statins and non-statin lipid-lowering medicines;
- peripheral-circulation and pulmonary-hypertension medicines.

Some less commonly used regional products and formulation-specific records still require additional label review.

Clinical entries use these optional fields:

- `clinicalInfo` for drug class, indications, contraindications, warnings, interactions, routine clinical monitoring, and sources;
- `timeCourse` for onset, Tmax, peak effect, duration of action, elimination half-life, steady state, washout, notes, and formulation-specific sources;
- `tdm` for true concentration monitoring or for explaining why clinical, ECG, coagulation, or laboratory markers are used instead;
- `doseUseReferences` for label-, guideline-, protocol-, or literature-reported regimens;
- `sourceRefs` for traceable regulatory labels, guidelines, laboratory catalogs, pharmacokinetic studies, and reviews.

Monitoring is not the same as therapeutic drug monitoring. Blood pressure, heart rate, ECG, INR, aPTT, anti-Xa activity, electrolytes, renal function, liver enzymes, CK, and lipid panels are clinical or laboratory monitoring markers. They must not be represented as routine plasma drug concentrations unless the medicine has a supported TDM use.

`doseUseReferences` is a source index, not a dose recommendation. It must not be used for prescribing, self-medication, or dose adjustment.

`timeCourse` is not automatically a clinical-effect curve. Tmax describes peak plasma concentration; peak clinical effect may occur at another time. Duration of action and washout must not be inferred directly from elimination half-life.

Source priority is:

1. Current DailyMed, FDA, EMA, or official local product labeling.
2. ACC/AHA, ESC, CHEST, ASH, KDIGO, and other relevant clinical guidelines.
3. Clinical laboratory catalogs for TDM and coagulation-assay interpretation.
4. Human pharmacokinetic studies, systematic reviews, and authoritative reviews.

All clinical information is provided for education and data indexing. It is not medical advice.

## License
```
Licensed under the GNU GENERAL PUBLIC LICENCE, Version 3.
You may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.gnu.org/licenses/gpl-3.0.en.html

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
