# Clinical Psychiatry Index Audit

This report separates clinical psychiatry / neuropsychiatry medicines from the original recreational psychoactive taxonomy.

## Scope

Current pass only changes index fields:

- `commonNames` Chinese aliases and common brand aliases
- `categories` for clinical psychiatry search/filtering

It does not rewrite substance summaries, dosage, ROA, toxicity, interactions, or time-course content.

## Source Frameworks

Primary classification references:

- WHO ATC/DDD N05 Psycholeptics: antipsychotics, anxiolytics, hypnotics/sedatives.
  https://atcddd.fhi.no/atc_ddd_index/?code=N05&showdescription=yes
- WHO ATC/DDD N06 Psychoanaleptics: antidepressants, ADHD psychostimulants/nootropics, anti-dementia medicines.
  https://atcddd.fhi.no/atc_ddd_index/?code=N06&showdescription=yes
- WHO Model Lists of Essential Medicines.
  https://www.who.int/groups/expert-committee-on-selection-and-use-of-essential-medicines/essential-medicines-lists
- Chinese National Essential Medicines List, 2018 edition.
  https://www.nhc.gov.cn/wjw/jbywml/201810/8b68d28bd3754898b339e06da8c7d907/files/1733375109455_35909.pdf

## Indexed Existing Entries

The generated app data now marks the following existing entries with `clinical-psychiatry` where appropriate:

- Antipsychotic: Haloperidol, Prochlorperazine, Quetiapine, Risperidone
- Antidepressant: Mirtazapine, Tianeptine
- Anxiolytic / sedative-hypnotic: Alprazolam, Bromazepam, Buspirone, Chlordiazepoxide, Clonazepam, Diazepam, Etizolam, Flunitrazepam, Lorazepam, Midazolam, Oxazepam, Temazepam, Triazolam
- Z-drug / sleep: Eszopiclone, Melatonin, Zolpidem, Zopiclone
- ADHD / stimulant medicine: Amphetamine, Dextroamphetamine, Lisdexamfetamine, Methylphenidate
- ADHD / alpha-2 agonist context: Clonidine
- Wakefulness-promoting: Armodafinil, Modafinil
- Cognitive-disorder medicine: Galantamine, Memantine
- Gabapentinoid / psychiatric-adjacent: Gabapentin, Pregabalin
- Substance-use-disorder or overdose context: Buprenorphine, Methadone, Naloxone, N-Acetylcysteine
- Supervised treatment-resistant depression / anesthesia context: Ketamine

## Data Corrections

- `Buspirone` previously carried DMT aliases and `psychedelic`; this pass removes those index values and replaces them with buspirone / 丁螺环酮 aliases plus `anxiolytic`.

## Removal Candidates

The following groups should not be part of a clinical psychiatry medicine index unless a later medical-data pack explicitly justifies them:

- Novel psychedelics and lysergamide / phenethylamine / tryptamine research chemicals.
- Synthetic cannabinoids.
- Recreational entactogens and cathinones.
- Novel dissociatives and arylcyclohexylamine research chemicals.
- Deliriant plants or non-clinical intoxication entries.

They are not deleted automatically in this pass because deletion changes the product scope of the original PsychonautWiki app. Use `python tools/drugdata/check_psychiatry_index.py` to list concrete candidates from the current generated dataset.

## High-Value Missing Medicines

These are not added yet because the user requested classification/index work first and no content rewrite.

Antipsychotics:

- Chlorpromazine
- Clozapine
- Olanzapine
- Aripiprazole
- Paliperidone
- Ziprasidone
- Lurasidone
- Amisulpride
- Sulpiride
- Perphenazine

Antidepressants:

- Sertraline
- Fluoxetine
- Paroxetine
- Escitalopram
- Citalopram
- Fluvoxamine
- Venlafaxine
- Duloxetine
- Bupropion
- Trazodone
- Vortioxetine
- Amitriptyline
- Clomipramine

Mood stabilizers:

- Lithium
- Valproate
- Carbamazepine
- Lamotrigine

Anxiolytics / hypnotics:

- Hydroxyzine
- Zaleplon
- Ramelteon
- Suvorexant
- Lemborexant

ADHD:

- Atomoxetine
- Guanfacine
- Clonidine

Cognitive disorders:

- Donepezil
- Rivastigmine

Substance-use-disorder treatment:

- Naltrexone
- Acamprosate
- Varenicline
- Disulfiram
