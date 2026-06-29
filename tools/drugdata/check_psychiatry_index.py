#!/usr/bin/env python3
import json
import sys
from pathlib import Path


SUBSTANCES_PATH = Path("app/src/main/res/raw/substances.json")

CLINICAL_CATEGORY = "clinical-psychiatry"
RETAINED_CLINICAL_CATEGORIES = {
    "clinical-psychiatry",
    "cardiovascular",
    "antithrombotic",
    "peripheral-circulation",
    "pulmonary-hypertension",
    "heart-failure",
    "lipid-lowering",
    "prescription-medicine",
    "endocrine",
    "hrt-related",
}

HIGH_VALUE_MISSING = {
    "antipsychotics": [
        "Chlorpromazine",
        "Clozapine",
        "Olanzapine",
        "Aripiprazole",
        "Paliperidone",
        "Ziprasidone",
        "Lurasidone",
        "Amisulpride",
        "Sulpiride",
        "Perphenazine",
        "Fluphenazine",
        "Trifluoperazine",
        "Thioridazine",
        "Droperidol",
        "Pimozide",
        "Flupentixol",
        "Zuclopenthixol",
        "Loxapine",
        "Cariprazine",
        "Brexpiprazole",
        "Asenapine",
        "Pimavanserin",
        "Levomepromazine",
        "Promazine",
        "Melperone",
        "Sertindole",
        "Chlorprothixene",
        "Penfluridol",
        "Tiapride",
        "Levosulpiride",
        "Zotepine",
        "Iloperidone",
        "Lumateperone",
        "Blonanserin",
    ],
    "antidepressants": [
        "Sertraline",
        "Fluoxetine",
        "Paroxetine",
        "Escitalopram",
        "Citalopram",
        "Fluvoxamine",
        "Venlafaxine",
        "Duloxetine",
        "Bupropion",
        "Trazodone",
        "Vortioxetine",
        "Amitriptyline",
        "Clomipramine",
        "Desvenlafaxine",
        "Milnacipran",
        "Vilazodone",
        "Imipramine",
        "Nortriptyline",
        "Doxepin",
        "Maprotiline",
        "Moclobemide",
        "Phenelzine",
        "Tranylcypromine",
        "Agomelatine",
        "Mianserin",
        "Reboxetine",
        "Levomilnacipran",
        "Nefazodone",
        "Trimipramine",
        "Dosulepin",
        "Protriptyline",
        "Isocarboxazid",
        "Selegiline",
        "Esketamine",
    ],
    "mood_stabilizers": [
        "Lithium Carbonate",
        "Valproate",
        "Divalproex",
        "Carbamazepine",
        "Oxcarbazepine",
        "Lamotrigine",
    ],
    "anxiolytics_and_hypnotics": [
        "Hydroxyzine",
        "Nitrazepam",
        "Estazolam",
        "Lormetazepam",
        "Flurazepam",
        "Clorazepate",
        "Prazepam",
        "Clobazam",
        "Tofisopam",
        "Zaleplon",
        "Ramelteon",
        "Suvorexant",
        "Lemborexant",
        "Daridorexant",
        "Brotizolam",
        "Quazepam",
        "Loprazolam",
        "Chloral Hydrate",
        "Clomethiazole",
        "Meprobamate",
        "Tasimelteon",
    ],
    "adhd": [
        "Atomoxetine",
        "Guanfacine",
        "Dexmethylphenidate",
        "Viloxazine",
        "Pitolisant",
        "Solriamfetol",
    ],
    "cognitive_disorders": [
        "Donepezil",
        "Rivastigmine",
    ],
    "substance_use_disorder": [
        "Naltrexone",
        "Acamprosate",
        "Varenicline",
        "Disulfiram",
        "Nalmefene",
    ],
}

RECREATIONAL_ONLY_CATEGORY_MARKERS = {
    "common",
    "habit-forming",
    "psychedelic",
    "stimulant",
    "depressant",
    "opioid",
    "entactogen",
    "benzodiazepine",
    "barbiturate",
    "eugeroic",
    "gabapentinoid",
    "hypnotic",
    "z-drug",
    "苯二氮䓬类",
    "巴比妥类",
    "促醒剂",
    "催眠药",
    "Z 类催眠药",
    "Z类催眠药",
    "加巴喷丁类",
    "dissociative",
    "deliriant",
    "hallucinogen",
    "oneirogen",
    "nootropic",
    "cannabinoid",
    "research-chemical",
    "tentative",
    "常见的",
    "易成瘾",
    "迷幻剂",
    "兴奋剂",
    "抑制剂",
    "阿片类物质",
    "同感剂",
    "分离剂",
    "谵妄剂",
    "致幻剂",
    "致梦剂",
    "益智药",
    "大麻素",
    "研究性化学品",
    "信息不确定",
}


def load_substances() -> list[dict]:
    data = json.loads(SUBSTANCES_PATH.read_text(encoding="utf-8"))
    return data.get("substances", [])


def has_chinese_alias(substance: dict) -> bool:
    return any(
        any("\u4e00" <= char <= "\u9fff" for char in alias)
        for alias in substance.get("commonNames", [])
    )


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    substances = load_substances()
    by_name = {substance.get("name", "").lower(): substance for substance in substances}
    clinical_entries = [
        substance
        for substance in substances
        if CLINICAL_CATEGORY in substance.get("categories", [])
    ]
    missing_chinese = [
        substance.get("name")
        for substance in clinical_entries
        if not has_chinese_alias(substance)
    ]
    missing_by_group = {
        group: [name for name in names if name.lower() not in by_name]
        for group, names in HIGH_VALUE_MISSING.items()
    }
    non_clinical_entries = [
        substance.get("name")
        for substance in substances
        if not RETAINED_CLINICAL_CATEGORIES.intersection(substance.get("categories", []))
    ]
    clinical_recreational_category_residue = {
        category
        for substance in clinical_entries
        for category in substance.get("categories", [])
        if category in RECREATIONAL_ONLY_CATEGORY_MARKERS
    }
    recreational_candidates = [
        substance.get("name")
        for substance in substances
        if CLINICAL_CATEGORY not in substance.get("categories", [])
        and RECREATIONAL_ONLY_CATEGORY_MARKERS.intersection(substance.get("categories", []))
    ]

    print(f"Clinical psychiatry indexed entries: {len(clinical_entries)}")
    for substance in sorted(clinical_entries, key=lambda item: item.get("name", "")):
        print(
            json.dumps(
                {
                    "name": substance.get("name"),
                    "categories": substance.get("categories", []),
                    "commonNames": substance.get("commonNames", []),
                },
                ensure_ascii=False,
            )
        )

    if missing_chinese:
        raise SystemExit(
            "Clinical psychiatry entries without Chinese alias: "
            + ", ".join(missing_chinese)
        )
    if non_clinical_entries:
        raise SystemExit(
            "Non-clinical substances remained after generation: "
            + ", ".join(non_clinical_entries[:80])
        )
    if clinical_recreational_category_residue:
        raise SystemExit(
            "Clinical psychiatry entries still contain recreational categories: "
            + ", ".join(sorted(clinical_recreational_category_residue))
        )

    print("\nHigh-value missing clinical psychiatry medicines:")
    for group, names in missing_by_group.items():
        print(f"{group}: {', '.join(names) if names else 'none'}")

    print("\nRecreational-only removal candidates, not deleted automatically:")
    print(", ".join(recreational_candidates[:120]))
    if len(recreational_candidates) > 120:
        print(f"... and {len(recreational_candidates) - 120} more")


if __name__ == "__main__":
    main()
