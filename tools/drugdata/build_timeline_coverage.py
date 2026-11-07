#!/usr/bin/env python3
"""Build the post-review timeline coverage pack.

The pack contains approximate pharmacokinetic timelines for substances whose
reviewed records do not have a numeric clinical duration.  The generator keeps
route-specific values compact and lets the Android resources merge them without
overwriting existing clinical time-course fields.
"""

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "tools/drugdata/timeline_coverage.json"


def pk(half_life, tmax=None, note=None):
    result = {"halfLife": list(half_life)}
    if tmax is not None:
        result["tmax"] = list(tmax)
    if note:
        result["note"] = note
    return result


def time_spec(value, basis):
    return {
        "min": value[0],
        "max": value[1],
        "unit": value[2],
        "basis": basis,
    }


def window(value, tmax=None, note=None):
    result = {
        "status": "available",
        "effectTimelineWindow": time_spec(
            value, "Dosing or application interval; plot range only"
        ),
        "note": note
        or "Plot window only; the interval is not a clinical-effect duration.",
    }
    if tmax is not None:
        result["tmax"] = list(tmax)
    return result


def duration(value, tmax=None, note=None, basis="Label-reported clinical duration"):
    result = {
        "status": "available",
        "durationOfAction": time_spec(value, basis),
        "note": note or "Label-reported clinical duration; not derived from half-life.",
    }
    if tmax is not None:
        result["tmax"] = list(tmax)
    return result


PK_ROUTES = {
    "25B-NBOMe": {
        "INSUFFLATED": pk((3, 3, "h"), (0.08, 0.08, "h")),
    },
    "5-Hydroxytryptophan": {
        "ORAL": pk((2, 4, "h"), (0.5, 1, "h")),
    },
    "5F-PB-22": {
        "SMOKED": pk((1, 2, "h"), (0.1, 0.25, "h")),
    },
    "Apomorphine": {
        "SUBCUTANEOUS": pk((0.5, 1, "h"), (0.15, 0.3, "h")),
    },
    "Ketamine": {
        "SUBLINGUAL": pk((2, 3, "h"), (0.5, 0.75, "h")),
        "INTRAVENOUS": pk((2, 3, "h"), (0.1, 0.25, "h")),
    },
    "LSA": {
        "ORAL": pk((4, 6, "h"), (1, 1.5, "h")),
        "SUBLINGUAL": pk((4, 6, "h"), (0.75, 1, "h")),
    },
    "Meclofenoxate": {
        "ORAL": pk((0.5, 2, "h"), (0.5, 0.5, "h")),
    },
    "Methiopropamine": {
        "INSUFFLATED": pk((2, 3, "h"), (0.15, 0.5, "h")),
    },
    "MPT": {
        "ORAL": pk((1.5, 3, "h"), (0.5, 1, "h")),
    },
    "PMA": {
        "ORAL": pk((6, 10, "h"), (1, 2, "h")),
    },
    "PMMA": {
        "ORAL": pk((6, 10, "h"), (1, 2, "h")),
    },
    "Poppers": {
        "INHALED": pk((0.03, 0.08, "h"), (0.01, 0.03, "h")),
    },
    "Acetazolamide": {
        "ORAL": pk((10, 16, "h"), (1, 3, "h")),
    },
    "Lidocaine": {
        "INTRAVENOUS": pk((1.5, 2, "h"), (0.05, 0.15, "h")),
    },
    "Dopamine": {
        "INTRAVENOUS": pk((0.02, 0.05, "h"), (0.05, 0.1, "h")),
    },
    "Moxonidine": {
        "ORAL": pk((2, 3, "h"), (0.5, 1.5, "h")),
    },
    "Metolazone": {
        "ORAL": pk((8, 14, "h"), (1, 3, "h")),
    },
    "Ethacrynic Acid": {
        "ORAL": pk((1, 4, "h"), (0.5, 1.5, "h")),
    },
    "Canrenone": {
        "ORAL": pk((10, 22, "h"), (2, 4, "h")),
    },
    "Finerenone": {
        "ORAL": pk((2, 3, "h"), (1, 2, "h")),
    },
    "Nicergoline": {
        "ORAL": pk((2, 4, "h"), (0.75, 1.5, "h")),
    },
    "Nicotinyl Alcohol": {
        "ORAL": pk((1, 2, "h"), (0.25, 0.75, "h")),
    },
    "Diosmin": {
        "ORAL": pk((11, 11, "h"), (2, 4, "h")),
    },
    "Hesperidin": {
        "ORAL": pk((3, 6, "h"), (4, 8, "h")),
    },
    "Calcium Dobesilate": {
        "ORAL": pk((5, 5, "h"), (4, 6, "h")),
    },
    "Aliskiren": {
        "ORAL": pk((24, 40, "h"), (1, 3, "h")),
    },
    "Moexipril": {
        "ORAL": pk((2, 9, "h"), (1, 2, "h")),
    },
    "Lovastatin": {
        "ORAL": pk((1, 2, "h"), (2, 4, "h")),
    },
    "Isosorbide Mononitrate": {
        "ORAL": pk((4, 6, "h"), (0.5, 1.5, "h")),
    },
    "Nebivolol": {
        "ORAL": pk((12, 19, "h"), (1, 3, "h")),
    },
    "Icosapent Ethyl": {
        "ORAL": pk((89, 89, "h"), (4, 8, "h")),
    },
    "Alprostadil": {
        "INTRAVENOUS": pk((0.01, 0.03, "h"), (0.01, 0.03, "h")),
    },
    "Medroxyprogesterone Acetate": {
        "ORAL": pk((12, 17, "h"), (1.5, 3.5, "h")),
    },
    "Estradiol Cypionate Injection": {
        "INTRAMUSCULAR": window(
            (21, 28, "day"),
            (2, 5, "day"),
            "Injection interval shown as the plot window; not an effect duration.",
        ),
    },
    "Estradiol Transdermal Patch": {
        "TRANSDERMAL": window(
            (7, 7, "day"),
            (1, 1, "day"),
            "Patch wear interval shown as the plot window; not an effect duration.",
        ),
    },
    "Estradiol Spray": {
        "TRANSDERMAL": window(
            (24, 24, "h"),
            (6, 10, "h"),
            "Daily application interval shown as the plot window; not an effect duration.",
        ),
    },
    "Estradiol Valerate Injection": {
        "INTRAMUSCULAR": window(
            (28, 28, "day"),
            (1, 2, "day"),
            "Injection interval shown as the plot window; not an effect duration.",
        ),
    },
    "Ethinylestradiol": {
        "ORAL": pk((13, 27, "h"), (1, 2, "h")),
    },
    "Leuprolide Acetate": {
        "INTRAMUSCULAR": window(
            (28, 28, "day"),
            (2, 6, "h"),
            "Monthly depot interval shown as the plot window; not an effect duration.",
        ),
    },
    "Triptorelin": {
        "INTRAMUSCULAR": window(
            (168, 168, "day"),
            (2, 6, "h"),
            "24-week depot interval shown as the plot window; not an effect duration.",
        ),
    },
    "Micronized Progesterone": {
        "ORAL": pk((12, 20, "h"), (1, 2, "h")),
    },
    "Trihexyphenidyl Hydrochloride": {
        "ORAL": pk((5, 10, "h"), (1, 2, "h")),
    },
    "Fluphenazine": {
        "ORAL": pk((15, 30, "h"), (1, 3, "h")),
    },
    "Thioridazine": {
        "ORAL": pk((10, 36, "h"), (2, 4, "h")),
    },
    "Droperidol": {
        "INTRAVENOUS": pk((2, 3, "h"), (0.05, 0.15, "h")),
    },
    "Loxapine": {
        "ORAL": pk((4, 12, "h"), (0.5, 2, "h")),
    },
    "Cariprazine": {
        "ORAL": pk((48, 96, "h"), (3, 6, "h")),
    },
    "Promazine": {
        "ORAL": pk((12, 24, "h"), (2, 4, "h")),
    },
    "Levosulpiride": {
        "ORAL": pk((6, 8, "h"), (2, 4, "h")),
    },
    "Lumateperone": {
        "ORAL": pk((18, 18, "h"), (1, 2, "h")),
    },
    "Desvenlafaxine": {
        "ORAL": pk((10, 12, "h"), (6, 8, "h")),
    },
    "Trazodone": {
        "ORAL": pk((7, 10, "h"), (1, 2, "h")),
    },
    "Nortriptyline": {
        "ORAL": pk((18, 44, "h"), (4, 8, "h")),
    },
    "Tranylcypromine": {
        "ORAL": pk((2, 3, "h"), (1, 2, "h")),
    },
    "Trimipramine": {
        "ORAL": pk((16, 40, "h"), (2, 4, "h")),
    },
    "Protriptyline": {
        "ORAL": pk((54, 92, "h"), (4, 8, "h")),
    },
    "Isocarboxazid": {
        "ORAL": pk((2, 4, "h"), (1, 2, "h")),
    },
    "Selegiline": {
        "TRANSDERMAL": pk((18, 25, "h"), (4, 8, "h")),
    },
    "Suvorexant": {
        "ORAL": pk((12, 12, "h"), (1, 3, "h")),
    },
    "Clomethiazole": {
        "ORAL": pk((4, 6, "h"), (0.5, 1.5, "h")),
    },
    "Meprobamate": {
        "ORAL": pk((8, 12, "h"), (1, 3, "h")),
    },
    "Guanfacine": {
        "ORAL": pk((18, 18, "h"), (4, 6, "h")),
    },
    "Disulfiram": {
        "ORAL": duration(
            (14, 14, "day"),
            (6, 12, "h"),
            "Label-reported clinical duration after discontinuation.",
            "Label-reported clinical duration after discontinuation",
        ),
    },
}


HISTORICAL_DURATIONS = {
    "Lisinopril": {
        "ORAL": window(
            (24, 24, "h"),
            (1, 2, "h"),
            "Once-daily dosing interval shown as the plot window; not an effect duration.",
        ),
    },
    "Losartan": {
        "ORAL": window(
            (24, 24, "h"),
            (1, 2, "h"),
            "Once-daily dosing interval shown as the plot window; not an effect duration.",
        ),
    },
    "Carvedilol": {
        "ORAL": window(
            (12, 12, "h"),
            (1, 2, "h"),
            "Twice-daily dosing interval shown as the plot window; not an effect duration.",
        ),
    },
    "Olmesartan": {
        "ORAL": window(
            (24, 24, "h"),
            (1, 3, "h"),
            "Once-daily dosing interval shown as the plot window; not an effect duration.",
        ),
    },
    "Irbesartan": {
        "ORAL": window(
            (24, 24, "h"),
            (1, 3, "h"),
            "Once-daily dosing interval shown as the plot window; not an effect duration.",
        ),
    },
    "Hydralazine": {
        "ORAL": duration(
            (2, 4, "h"),
            (0.5, 1, "h"),
            basis="Label-reported clinical duration",
        ),
        "INTRAVENOUS": duration(
            (2, 6, "h"),
            (0.1, 0.3, "h"),
            basis="Label-reported clinical duration",
        ),
    },
    "Bisoprolol": {
        "ORAL": window(
            (24, 24, "h"),
            (1, 4, "h"),
            "Once-daily dosing interval shown as the plot window; not an effect duration.",
        ),
    },
    "Labetalol": {
        "ORAL": duration(
            (8, 12, "h"),
            (1, 2, "h"),
            basis="Label-reported clinical duration",
        ),
        "INTRAVENOUS": duration(
            (2, 6, "h"),
            (0.1, 0.3, "h"),
            basis="Label-reported clinical duration",
        ),
    },
    "Enalapril": {
        "ORAL": window(
            (24, 24, "h"),
            (1, 2, "h"),
            "Once-daily dosing interval shown as the plot window; not an effect duration.",
        ),
    },
    "Nifedipine": {
        "ORAL": duration(
            (6, 8, "h"),
            (0.5, 2, "h"),
            "Label-reported immediate-release clinical duration.",
            "Label-reported clinical duration",
        ),
    },
    "Droxidopa": {
        "ORAL": duration(
            (6, 6, "h"),
            (1, 2, "h"),
            basis="Label-reported clinical duration",
        ),
    },
    "Nitroglycerin": {
        "TRANSDERMAL": window(
            (12, 14, "h"),
            (2, 4, "h"),
            "Nitroglycerin patches are recommended for 12-14 hours of daily wear; this is an application/dosing window, not a measured single-dose effect duration, so it is shown as the plot window only.",
        ),
    },
}


def route_patch(route, values):
    result = {"route": route, **values}
    if "status" not in result:
        result["status"] = "pk-estimated"
    if "note" not in result:
        result["note"] = (
            "Approximate pharmacokinetic timeline for effect visualization."
        )
    return result


def main():
    names = set(PK_ROUTES) | set(HISTORICAL_DURATIONS)
    patches = []
    for name in sorted(names):
        routes = []
        for route, values in PK_ROUTES.get(name, {}).items():
            routes.append(route_patch(route, values))
        for route, values in HISTORICAL_DURATIONS.get(name, {}).items():
            routes.append(route_patch(route, values))
        patches.append({"name": name, "routes": routes})
    data = {
        "schemaVersion": 1,
        "generatedAt": "2026-11-07",
        "timelineCoverage": patches,
    }
    OUTPUT.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Wrote {OUTPUT} ({len(patches)} substances)")


if __name__ == "__main__":
    main()
