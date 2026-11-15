# Hormone research-reference models

Bundle `2026-11-05.2` contains 16 profiles for the 13 estrogen catalog entries.
A catalog identity is not a complete product specification: generic estradiol still
requires confirmation of chemical form, formulation, route and any needed strength/site.
The 6 released v1 profiles retain their IDs, numerical values and individual versions.

These are population-reference reconstructions for a journal, not validated individual
concentration predictions or dosing advice. Numerical checks establish implementation
and stated-anchor agreement; they do not establish clinical validity.

## Support matrix

| Catalog entry / formulation | Supported reference and output |
| --- | --- |
| Estradiol (ingredient only) | Explicitly confirmed oral, sublingual, gel, patch or spray reference; E2. No automatic choice of ester or vehicle. |
| Oral Estradiol | Micronized E2 oral reference, E2 |
| Sublingual/Buccal Estradiol | Micronized E2 **sublingual** reference, E2; the combined catalog name does not make buccal tablets equivalent |
| Oral Estradiol Valerate | 4mg EV reference-group single-dose study, E2 from administered ester mg |
| Estradiol Valerate Injection | Oil IM EV, separate three-stage reference, E2 |
| Estradiol Cypionate Injection | Oil IM EC, separate three-stage reference, E2 |
| Estradiol Enanthate Injection | EN alone IM, reported apparent absorption/elimination/volume parameters, E2 |
| Estradiol Gel | EstroGel 0.06% arm and separate Divigel 0.1% upper-thigh references, E2 |
| Estradiol Spray | Evamist forearm, separate 1/2/3-spray responses, E2; other doses are rejected |
| Estradiol Transdermal Patch | Original twice-weekly abdomen reference, nominal release and actual wear interval, E2 |
| Ethinylestradiol | Microgynon EE component reference, **EE** |
| Estetrol | NEXTSTELLIS anhydrous E4 component reference, **E4** |
| Conjugated Estrogens | Confirmed Premarin mixture, separate baseline-adjusted unconjugated **E1** and **Equilin** responses |

EE/E4/CEE components are separate analytes; they are never summed into an E2 curve or
converted to “E2 potency”. Laboratory overlays must match the selected analyte. CEE
component curves are not a complete mixture/activity model. Antiestrogens, progestogens
and GnRH drugs remain recognized records. Their individual pharmacodynamic effects
require additional parameterization.

## Interactive model settings

Chart sampling adds event onset, lag, peak neighborhoods, characteristic time scales
and patch removal to the display grid for every administration family.
The original fixed-grid simulation API remains available for numerical comparisons.

Each consumer can specify an E2 baseline and a calibration phase start. The fitted
amplitude minimizes squared residuals after subtracting that baseline. The baseline
is added once to the final curve. Calibration uses eligible labs in the selected
phase, and the fitted amplitude applies from that phase start. The diagnostics show
fit MAE and forward validation MAE obtained by fitting only strictly earlier labs.
Unseparated baselines embedded in published reference anchors remain a limitation.

The optional total-testosterone scenario uses explicit user-entered parameters,
with concentrations in ng/dL and time constants in hours:

```
dE_effect/dt = (E2 - E_effect) / tau_E
T_target = T_floor + (T_unsuppressed - T_floor) / (1 + E_effect / EC50) * R_CPA * R_spiro
dT/dt = (T_target - T) / tau_T
```

The starting T and timestamp can be copied from the latest exact T lab. Effective
E2 is warmed up from earlier recorded exposure, looking back up to eight feedback
time constants. Each gel reference drives its
own T scenario, including future estrogen plans. Integration uses linear E2
interpolation and exponential updates with steps no longer than 0.25 h.
The T parameters are scenario inputs without fitted population defaults. Gonadal
feedback, SHBG and free T remain outside this model.
Measured T points preserve their comparison operators and use an independent axis.

Historical oral CPA and spironolactone records use linked ingredient identities and
the existing event product snapshot/dose-basis conversions. All earlier actual doses
contribute residual exposure, including doses preceding the display window or T anchor.
Other consumers, future actual records, unsupported routes and unresolved doses are
excluded with an explicit reason in the antiandrogen record details. The T anchor
remains exactly the specified starting value. Future antiandrogen exposure currently
contains only residual actual doses; antiandrogen plans are not projected.

The exposure proxy for each drug uses `k = ln(2)/halfLifeHours`,
`A(t) = sum(doseMg * exp(-k * ageHours))`, and `D(t) = 24 * k * A(t)` in equivalent
mg/day. It assumes instantaneous oral input, is not a plasma concentration, and does
not model absorption or metabolite formation. Each drug has separately entered
`D50` and `Smax`: `R = 1 - Smax * D / (D50 + D)`.
The multiplicative combination above is a scenario assumption, not a clinically
validated drug interaction or individualized response model.

CPA's 43.9-hour terminal half-life is documented in the
[50mg tablet SmPC](https://www.medicines.org.uk/emc/product/15996/smpc), section 5.2.
For spironolactone the editor suggests the active metabolite canrenone's 16.5 hours
as a residual proxy rather than the parent's 1.4 hours; the
[DailyMed label](https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=8b76a904-6c99-485a-a05f-13a36c5f720c)
also reports TMS 13.8 hours and HTMS 15 hours. These values do not establish effective
antiandrogen half-lives or suppression strength. D50 and Smax have no fabricated
defaults. In particular receptor antagonism does not imply lower serum total T;
spironolactone can use Smax = 0. The model does not output receptor blockade or free T.

Unconfirmed vehicles, SC injection, buccal formulations, other application sites and
unreviewed patch types are not silently substituted. They remain recognized records
with an explicit modeling state. “All catalog entries have a reference” does not mean
all possible products or routes share that reference.

## Sources and reconstruction boundaries

- [Doll 2022](https://pubmed.ncbi.nlm.nih.gov/34781041/), oral/sublingual E2, and the fixed v1 sources in `evidence.json`.
- [Kuhnz 1998](https://pubmed.ncbi.nlm.nih.gov/9793623/), oral EV: reference-group peak/time and terminal time; simple response does not reproduce every enterohepatic secondary peak.
- [Wiemeyer 1986](https://pubmed.ncbi.nlm.nih.gov/3814225/), EN alone: reconstructed from abstract-reported apparent parameters using administered EN mg. Apparent volume is not measured physiological volume, and the peak was not independently redigitized from the original article. Do not mix in DHPA combination studies.
- [Divigel label](https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=59e610dc-883e-4f07-9ec6-a5841b0bf9fb), 1mg day-14 unadjusted exposure and roughly 10h terminal time. Effective absorption and phase delay are fitted, not independently measured skin transport constants.
- [Sirvio 2026](https://link.springer.com/article/10.1007/s40268-026-00552-x) reports a much longer terminal time in a different 400cm² cohort. Its parameters are not pooled with the label's 10h reference. Individual variability and unadjusted baseline bias remain.
- [Evamist label](https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=9a0aa631-133d-406b-9d32-8a1a99af4e50) and [FDA review](https://www.accessdata.fda.gov/drugsatfda_docs/nda/2007/022014s000_PharmR.pdf): dose is not proportional to exposure. Each of 1/2/3 sprays has its own fitted response. `Ka=2*Ke` is an explicit effective-shape constraint, not a published physiological measurement; phase delay fits the reported steady-state peak time. Single-dose and non-daily curves extrapolate from steady-state data and lack independent clinical validation.
- [Microgynon SmPC](https://www.medicines.org.uk/emc/product/1131/smpc), EE component: two-phase response constrained by reported fast/slow times and peak/accumulation summaries. The label notes that phase times were measured at higher doses. Do not confuse the LNG half-life with the EE half-life. The coformulated progestin and interaction effects are not modeled.
- [NEXTSTELLIS label](https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=c5270073-d083-4109-ae4b-156986175e0a), fasting E4 component: fitted fast-distribution fraction plus reported 27h terminal response. Fast parameters are effective fit parameters, not independently measured clearance. Dose basis is anhydrous E4; monohydrate conversion requires explicit specification. DRSP is not part of the E4 curve.
- [Premarin label](https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=1fcd61a8-1a67-4785-a51a-45fca6fbb32b): CEE product-dose drives separate E1/Equilin concentration models. AUC and peak time are calibration targets; peaks are compared within reported CV. The CV is not an individual prediction interval, and other CEE brands are not presumed equivalent.

The legacy `activeFraction` field is used only for explicit E2 ester/equivalent conversion;
its placeholder value for other analytes is not an E2 potency equivalence.

Linear superposition and any dose scaling beyond a measured reference remain assumptions.
The model does not infer endogenous secretion, individual clearance, efficacy, risk or dose changes.

## Reproduce

```powershell
python tools/drugdata/hrt_models/expand_reference_models.py
python tools/drugdata/hrt_models/check_reference_models.py
```

Requires existing NumPy/SciPy. `fit_reference_models.py` is the low-level v1 fitter;
run the expansion builder for the complete bundle. `evidence.json` / `anchor_validation.json`
preserve v1 evidence. Expansion sources and 18 extra checks are in `evidence_expansion.json`
and `expansion_anchor_validation.json`; 14 original checks remain, total 32.

`numerical_fixtures.json` and `expansion_numerical_fixtures.json` use independent SciPy
matrix exponentials, including the biphasic response and delayed skin responses. Kotlin
tests compare all 16 profiles and enforce analyte separation, spray dose bands, actual
site/route requirements, explicit hydrate conversion and preservation of original records.
New or modified response parameters require a new profile ID/version.

## Attribution

Only v1 injection transfer/turnover priors refer to MIT estrannaise.js data at fixed commit
`47198c4e0738849e9ac5b51605a477a5a6a1e814`. Amplitude/absorption fits and implementations
are independent; no code from unlicensed HRT-Recorder is copied. MIT notice remains
bundled as `hrt_model_notices.txt`.

## Explicit sublingual-EV bridge and gel scenarios

Journals may contain EV tablets administered sublingually. This route is now
recognized without rewriting the drug identity or actual administration route.
[Lim 2019](https://pmc.ncbi.nlm.nih.gov/articles/PMC6333764/) documents clinical use,
but is not a dedicated single-dose EV sublingual PK validation. When enabled, the chart applies a separately labeled **bridge extrapolation**:
administered EV mass ×272.38/356.50 supplies parent E2 mass to the existing sublingual
E2 reference time response. This hypothesis is not a seventeenth validated profile,
and does not turn the journal entry into free E2. It is visibly labeled and can be
disabled. The bridge itself provides no validated pharmacodynamic suppression
relationship or dose recommendation.

When legacy gel records already state active E2 mg, the chart uses that mass directly.
Missing vehicle/site information can be compared under two named published study
conditions, without writing a guessed vehicle/site into the journal. These scenarios
combine the other admitted estrogen events and clearly label any EV bridge component;
they are neither bounds on personal exposure nor confidence intervals. Original dose
timelines remain separate from concentration-reference series.

## Lab amplitude calibration

New E2 lab entries can store explicit consent, sampling time and the reported interval
since the most recent actual estrogen dose. This metadata is separate from the raw lab
and ingestion rows. It is included in the existing backup settings module without a
schema change. Old labs lacking this context do not silently become calibration targets.

For each displayed reference/scenario, eligible exact E2 observations are evaluated at
their actual sampling time, using only prior actual doses. A non-negative one-parameter
least-squares amplitude is calculated: sum(predicted*observed)/sum(predicted²). The
published reference parameters themselves remain unchanged. This is not Bayesian
individual PK estimation and does not identify absorption, clearance or endogenous
baseline from a single point. T measurements remain independent measurements; they
do not automatically fit the optional manual T scenario. No dose recommendation is generated.

Other patients, future samples, censored results, unconvertible units, missing dosing
context or intervals inconsistent with the actual dose timestamps are excluded from
this amplitude fit. Each reference scenario is fitted separately. Changing or deleting
labs/doses recalculates the fit. Raw assay values and recorded doses are not rewritten.
Sampling-time sensitivity is discussed in the primary simulation study
https://pmc.ncbi.nlm.nih.gov/articles/PMC7063976/ ; its results are not asserted to
validate an estradiol patient model. The source dialog reports calibration diagnostics.

## Optional manual testosterone scenario

The chart can add a separate total-testosterone scenario in ng/dL (nmol/L measurements
are converted using 0.0347). This is an exploratory model with explicitly entered
parameters, not a clinically validated prediction or dose calculator. The initial T,
unsuppressed level, floor, E2 EC50 and two response time constants must be supplied.
E2 measurements remain independent calibration observations; T observations are not
silently used to estimate suppression parameters.

The model uses `dE_effect/dt = (E2 - E_effect)/effectHours` and
`dT/dt = (T_target - T)/responseHours`, with
`T_target = floor + (unsuppressed-floor)/(1+E_effect/EC50) * R_CPA * R_spiro`.
The E2 effect compartment is propagated analytically for each linear input segment;
the T response uses exponential midpoint integration with a step no greater than
15 minutes or one eighth of the shortest configured time scale. Integration splits
at every included antiandrogen dose time, even with a coarse E2 input grid. The
E2 compartment warms up for up to eight effect time constants before the T anchor,
while preserving the entered initial T exactly.

All linked oral CPA and spironolactone records for the selected consumer are read,
including records before the visible window or T anchor. Product snapshots and
explicit dose bases resolve ingredient mass. Missing units, incompatible routes,
future actual records and missing drug parameters have visible exclusion reasons.
Each drug uses `A = sum(doseMg * exp(-k*ageHours))`, `k = ln(2)/halfLifeHours`,
`D = 24*k*A`, and `R = 1 - Smax*D/(D50+D)`. D is a dose-exposure proxy with
instantaneous absorption, not a plasma concentration or measured daily intake.
D50 and Smax are manual scenario parameters; no clinical values are invented.
Spironolactone receptor blockade does not imply reduced total T: Smax can be zero.
The multiplicative combination is an explicit, unvalidated modeling assumption.
Future estrogen plans are included; future antiandrogen plans are not projected.
Only the residual exposure of recorded antiandrogen doses continues into the forecast.

A manually supplied E2 baseline is subtracted before amplitude fitting and added
once afterward. A calibration phase start limits eligible labs and scaling to that
phase. Both sides of its time boundary are sampled to avoid interpolating the new
scale into earlier exposure. Training MAE and chronological forward-validation MAE
describe calibration error, not prediction intervals or clinical validation. Study
anchors may themselves contain unseparated endogenous baseline. Reference PK kernels
and coefficients are unchanged; event-aware plotting adds onset, peak, characteristic
time-scale and patch-removal samples to the regular grid.
