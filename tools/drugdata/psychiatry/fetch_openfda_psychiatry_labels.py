#!/usr/bin/env python3
import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(REPO_ROOT))

from tools.drugdata.psychiatry.build_psychiatry_clinical_core import (  # noqa: E402
    DRUGS,
    EXISTING_PATCHES,
    MANUAL_CURATED_LABEL_DATA,
)


OUTPUT_PATH = REPO_ROOT / "tools/drugdata/psychiatry/openfda_psychiatry_label_data.json"
OPENFDA_LABEL_URL = "https://api.fda.gov/drug/label.json"

SEARCH_TERMS = {
    "Valproate": ["valproic acid", "sodium valproate"],
    "Divalproex": ["divalproex sodium"],
    "N-Acetylcysteine": ["acetylcysteine"],
}

# 这些美国标签常见适应证与本项目精神科索引语境不一致，先保留来源检索，不自动写数值。
SKIP_AUTO_EXTRACTION = {
    "Amisulpride",  # 美国标签主要为术后恶心呕吐用 IV 制剂。
    "Bupropion",  # 标签同时描述多个活性代谢物，简单正则容易误抓代谢物 Tmax/半衰期。
    "Ketamine",  # 美国通用标签多为麻醉用法；精神科语境需单独核对 esketamine/指南。
    "N-Acetylcysteine",  # 标签多为解毒/呼吸道适应证。
    "Temazepam",  # 当前自动抽取会误抓非半衰期片段。
    "Vilazodone",  # 当前自动抽取会误抓半衰期数值作为 Tmax。
    "Armodafinil",  # 当前自动抽取会误抓非终末半衰期片段。
}


def fetch_json(url: str) -> dict | None:
    req = urllib.request.Request(url, headers={"User-Agent": "clinical-data-indexer/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            return json.loads(response.read().decode("utf-8"))
    except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError, json.JSONDecodeError):
        return None


def clean_text(parts: list[str] | str | None) -> str:
    if not parts:
        return ""
    if isinstance(parts, str):
        text = parts
    else:
        text = " ".join(parts)
    return re.sub(r"\s+", " ", text).strip()


def normalize_unit(unit: str) -> str:
    unit = unit.lower()
    if unit.startswith("min"):
        return "min"
    if unit.startswith("day"):
        return "day"
    if unit.startswith("week"):
        return "week"
    return "h"


def num(value: str) -> float:
    parsed = float(value)
    return int(parsed) if parsed.is_integer() else parsed


def first_range(patterns: list[str], text: str) -> tuple[float, float, str] | None:
    for pattern in patterns:
        match = re.search(pattern, text, flags=re.IGNORECASE)
        if not match:
            continue
        first = num(match.group("first"))
        second = match.groupdict().get("second")
        unit = normalize_unit(match.group("unit"))
        return first, num(second) if second else first, unit
    return None


def extract_time_course(label: dict) -> dict:
    text = clean_text(label.get("pharmacokinetics")) + " " + clean_text(label.get("clinical_pharmacology"))
    if not text:
        return {}

    time_course = {}
    tmax = first_range(
        [
            r"\bT\s*max\b[^.]{0,120}?(?P<first>\d+(?:\.\d+)?)\s*(?:to|-|–)\s*(?P<second>\d+(?:\.\d+)?)\s*(?P<unit>hours?|hrs?|h|minutes?|mins?|min|days?|weeks?)",
            r"\bT\s*max\b[^.]{0,120}?(?P<first>\d+(?:\.\d+)?)\s*(?P<unit>hours?|hrs?|h|minutes?|mins?|min|days?|weeks?)",
            r"peak plasma concentrations?[^.]{0,120}?(?:within|at|about|approximately|in)\s*(?P<first>\d+(?:\.\d+)?)\s*(?:to|-|–)?\s*(?P<second>\d+(?:\.\d+)?)?\s*(?P<unit>hours?|hrs?|h|minutes?|mins?|min|days?|weeks?)",
            r"peak concentrations?[^.]{0,120}?(?:within|at|about|approximately|in)\s*(?P<first>\d+(?:\.\d+)?)\s*(?:to|-|–)?\s*(?P<second>\d+(?:\.\d+)?)?\s*(?P<unit>hours?|hrs?|h|minutes?|mins?|min|days?|weeks?)",
        ],
        text,
    )
    if tmax:
        time_course["tmax"] = [tmax[0], tmax[1], tmax[2], "label pharmacokinetics"]

    half_life = first_range(
        [
            r"(?:terminal\s+)?half-life[^.]{0,140}?(?P<first>\d+(?:\.\d+)?)\s*(?:to|-|–)\s*(?P<second>\d+(?:\.\d+)?)\s*(?P<unit>hours?|hrs?|h|minutes?|mins?|min|days?|weeks?)",
            r"(?:terminal\s+)?half-life[^.]{0,140}?(?:about|approximately|is|was|of|averages?|mean)?\s*(?P<first>\d+(?:\.\d+)?)\s*(?P<unit>hours?|hrs?|h|minutes?|mins?|min|days?|weeks?)",
        ],
        text,
    )
    if half_life:
        time_course["eliminationHalfLife"] = [half_life[0], half_life[1], half_life[2], "label pharmacokinetics"]

    steady = first_range(
        [
            r"steady[- ]state[^.]{0,140}?(?:within|after|in|approximately|about)?\s*(?P<first>\d+(?:\.\d+)?)\s*(?:to|-|–)?\s*(?P<second>\d+(?:\.\d+)?)?\s*(?P<unit>days?|weeks?)",
        ],
        text,
    )
    if steady:
        time_course["timeToSteadyState"] = [steady[0], steady[1], steady[2], "label pharmacokinetics"]

    return time_course


def label_for_name(name: str) -> tuple[dict | None, str | None]:
    for term in SEARCH_TERMS.get(name, [name]):
        quoted = urllib.parse.quote(f'openfda.generic_name:"{term.upper()}"')
        url = f"{OPENFDA_LABEL_URL}?search={quoted}&limit=1"
        data = fetch_json(url)
        time.sleep(0.12)
        results = data.get("results") if data else None
        if results:
            return results[0], term
    return None, None


def main() -> None:
    names = []
    for drug in DRUGS + EXISTING_PATCHES:
        name = drug[0]
        if name not in names:
            names.append(name)

    output = {}
    for name in names:
        if name in MANUAL_CURATED_LABEL_DATA or name in SKIP_AUTO_EXTRACTION:
            continue
        label, term = label_for_name(name)
        if not label:
            continue

        time_course = extract_time_course(label)
        if not time_course:
            continue

        set_id = label.get("set_id")
        if isinstance(set_id, list):
            set_id = set_id[0] if set_id else None
        if not set_id:
            continue

        entry = {
            "openfdaSetId": set_id,
            "timeCourse": time_course,
        }
        entry["autoExtractionNote"] = (
            f"Conservative openFDA extraction from generic-name search term '{term}'. "
            "Verify product-specific label before clinical interpretation."
        )
        output[name] = entry

    OUTPUT_PATH.write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {len(output)} auto-curated psychiatry label records to {OUTPUT_PATH}")


if __name__ == "__main__":
    main()
