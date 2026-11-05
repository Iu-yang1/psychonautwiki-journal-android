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
and GnRH drugs remain recognized records without fabricated E2 contributions or
predicted testosterone suppression.

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
disabled. No pharmacodynamic suppression or dose recommendation is inferred.

When legacy gel records already state active E2 mg, the chart uses that mass directly.
Missing vehicle/site information can be compared under two named published study
conditions, without writing a guessed vehicle/site into the journal. These scenarios
combine the other admitted estrogen events and clearly label any EV bridge component;
they are neither bounds on personal exposure nor confidence intervals. Original dose
timelines remain separate from concentration-reference series.
