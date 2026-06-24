#!/usr/bin/env python3
import json
import re
from pathlib import Path


SOURCE_DIR = Path("tools/drugdata/cardiovascular")
RAW_FILES = (
    Path("app/src/main/res/raw/substances.json"),
    Path("app/src/main/res/raw-zh-rCN/substances.json"),
)
REQUIRED_SOURCE_FIELDS = ("title", "url", "sourceType", "accessedDate", "evidenceLevel")

FIRST_BATCH = {
    "Amlodipine", "Nifedipine ER", "Diltiazem", "Verapamil",
    "Enalapril", "Lisinopril", "Ramipril", "Losartan", "Valsartan",
    "Candesartan", "Sacubitril/Valsartan", "Spironolactone", "Eplerenone",
    "Dapagliflozin", "Empagliflozin", "Furosemide", "Torsemide",
    "Hydrochlorothiazide", "Indapamide", "Chlorthalidone", "Amiloride",
    "Amiodarone", "Sotalol", "Dofetilide", "Adenosine", "Procainamide",
    "Mexiletine", "Propafenone", "Nitroglycerin", "Isosorbide Mononitrate",
    "Isosorbide Dinitrate", "Ranolazine", "Aspirin", "Clopidogrel",
    "Ticagrelor", "Warfarin", "Apixaban", "Rivaroxaban", "Dabigatran",
    "Enoxaparin", "Heparin", "Alteplase", "Atorvastatin", "Rosuvastatin",
    "Ezetimibe", "Evolocumab", "Alirocumab", "Bempedoic Acid",
    "Fenofibrate", "Pentoxifylline", "Cilostazol", "Midodrine", "Droxidopa",
}

SECOND_BATCH = {
    "Bisoprolol", "Carvedilol", "Nebivolol", "Atenolol", "Propranolol",
    "Irbesartan", "Telmisartan", "Olmesartan", "Perindopril", "Captopril",
    "Bumetanide", "Metolazone", "Acetazolamide", "Triamterene",
    "Quinidine", "Disopyramide", "Dronedarone", "Ibutilide", "Prasugrel",
    "Cangrelor", "Dipyridamole", "Cilostazol", "Fondaparinux", "Dalteparin",
    "Edoxaban", "Argatroban", "Bivalirudin", "Tenecteplase", "Eptifibatide",
    "Tirofiban", "Simvastatin", "Pravastatin", "Pitavastatin", "Fluvastatin",
    "Inclisiran", "Icosapent Ethyl", "Gemfibrozil", "Cholestyramine",
    "Colesevelam", "Sildenafil", "Tadalafil", "Riociguat", "Bosentan",
    "Ambrisentan", "Macitentan", "Epoprostenol", "Iloprost", "Treprostinil",
    "Selexipag", "Alprostadil", "Naftidrofuryl",
}

DOACS = {"Apixaban", "Rivaroxaban", "Dabigatran", "Edoxaban", "Betrixaban"}
STATINS = {"Atorvastatin", "Rosuvastatin", "Simvastatin", "Pravastatin", "Pitavastatin", "Fluvastatin"}
RAAS = {
    "Enalapril", "Lisinopril", "Ramipril", "Losartan", "Valsartan",
    "Candesartan", "Sacubitril/Valsartan", "Irbesartan", "Telmisartan",
    "Olmesartan", "Perindopril", "Captopril",
}
DIURETICS = {
    "Furosemide", "Torsemide", "Hydrochlorothiazide", "Indapamide",
    "Chlorthalidone", "Amiloride", "Bumetanide", "Metolazone",
    "Acetazolamide", "Triamterene",
}
QT_DRUGS = {
    "Amiodarone", "Sotalol", "Dofetilide", "Procainamide", "Quinidine",
    "Disopyramide", "Dronedarone", "Ibutilide",
}


def flatten(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for child in value.values():
            yield from flatten(child)
    elif isinstance(value, list):
        for child in value:
            yield from flatten(child)


def text(substance):
    return " ".join(flatten(substance)).lower()


def source_refs(value):
    if isinstance(value, dict):
        if "sourceRefs" in value:
            yield from value.get("sourceRefs") or []
        for child in value.values():
            yield from source_refs(child)
    elif isinstance(value, list):
        for child in value:
            yield from source_refs(child)


def has_numeric_range(reference):
    return any(
        isinstance(dose_range.get("min"), (int, float))
        or isinstance(dose_range.get("max"), (int, float))
        or any(
            isinstance(component.get("min"), (int, float))
            or isinstance(component.get("max"), (int, float))
            for component in dose_range.get("components", [])
        )
        for dose_range in reference.get("ranges", [])
    )


def require_terms(errors, substance, terms, label):
    haystack = text(substance)
    missing = [term for term in terms if term.lower() not in haystack]
    if missing:
        errors.append(f"{substance['name']}: {label} missing {missing}")


def main():
    errors = []
    source_names = []
    for path in sorted(SOURCE_DIR.rglob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception as error:
            errors.append(f"{path}: invalid JSON: {error}")
            continue
        for substance in data.get("substances", []):
            source_names.append(substance.get("name"))
    duplicates = sorted({name for name in source_names if source_names.count(name) > 1})
    if duplicates:
        errors.append(f"Duplicate cardiovascular source names: {duplicates}")

    for raw_path in RAW_FILES:
        data = json.loads(raw_path.read_text(encoding="utf-8"))
        substances = {
            substance["name"]: substance
            for substance in data.get("substances", [])
            if "cardiovascular" in substance.get("categories", [])
        }
        missing_targets = sorted((FIRST_BATCH | SECOND_BATCH) - set(substances))
        if missing_targets:
            errors.append(f"{raw_path}: missing requested drugs: {missing_targets}")

        for name, substance in substances.items():
            prefix = f"{raw_path}: {name}"
            for field in ("name", "commonNames", "categories"):
                if not substance.get(field):
                    errors.append(f"{prefix}: missing {field}")
            if not substance.get("clinicalInfo"):
                errors.append(f"{prefix}: missing clinicalInfo")
            if not substance.get("timeCourse"):
                errors.append(f"{prefix}: missing timeCourse")
            if substance.get("tdm") is None:
                errors.append(f"{prefix}: missing tdm/monitoring object")
            if not substance.get("doseUseReferences"):
                errors.append(f"{prefix}: missing doseUseReferences")
            refs = list(source_refs(substance))
            if not refs:
                errors.append(f"{prefix}: missing sourceRefs")
            for ref in refs:
                missing = [field for field in REQUIRED_SOURCE_FIELDS if not ref.get(field)]
                if missing:
                    errors.append(f"{prefix}: sourceRef missing {missing}")
            for reference in substance.get("doseUseReferences", []):
                if has_numeric_range(reference) and not reference.get("sourceRefs"):
                    errors.append(f"{prefix}: numeric doseUseReference missing sourceRefs")
            if any("source needed" in value.lower() for value in flatten(substance)):
                errors.append(f"{prefix}: unresolved source needed")

        special = substances
        require_terms(errors, special["Warfarin"], ["inr", "clotting-factor"], "warfarin monitoring")
        require_terms(errors, special["Heparin"], ["aptt", "anti-xa", "platelet"], "UFH monitoring")
        require_terms(errors, special["Enoxaparin"], ["anti-xa", "selected"], "LMWH monitoring")
        for name in DOACS:
            require_terms(errors, special[name], ["routine plasma concentration monitoring is not standard"], "DOAC assay caveat")
        for name in STATINS:
            require_terms(errors, special[name], ["ldl-c", "half-life"], "statin response/PK distinction")
        require_terms(errors, special["Amiodarone"], ["tissue accumulation", "very long", "terminal half-life"], "amiodarone long-tail caveat")
        nitro_courses = special["Nitroglycerin"].get("timeCourse", [])
        nitro_routes = " ".join(
            f"{course.get('route', '')} {course.get('formulation', '')}"
            for course in nitro_courses
        ).lower()
        for route in ("sublingual", "transdermal", "intravenous"):
            if route not in nitro_routes:
                errors.append(f"Nitroglycerin: missing {route} timeCourse")
        for name in RAAS:
            require_terms(errors, special[name], ["potassium", "renal", "pregnancy"], "RAAS safety")
        for name in DIURETICS:
            require_terms(errors, special[name], ["potassium", "sodium", "renal", "volume"], "diuretic monitoring")
        for name in QT_DRUGS:
            require_terms(errors, special[name], ["ecg", "qt"], "antiarrhythmic ECG/QT caveat")

        print(f"{raw_path}: checked {len(substances)} cardiovascular entries")

    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print("Cardiovascular data-pack validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
