"""Cache primary-source candidates for a review batch without approving any fields."""

import argparse
import concurrent.futures
import hashlib
import json
import re
from html import unescape
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def fetch(source, directory, refresh=False):
    original_url = source["url"]
    query = urllib.parse.parse_qs(urllib.parse.urlparse(original_url).query)
    set_id = query.get("setid", [None])[0]
    if urllib.parse.urlparse(original_url).hostname == "api.fda.gov":
        match = re.fullmatch(
            r'set_id:"?([0-9a-fA-F-]{36})"?', query.get("search", [""])[0]
        )
        set_id = match.group(1) if match else None
    use_spl = bool(
        set_id
        and urllib.parse.urlparse(original_url).hostname
        in {"dailymed.nlm.nih.gov", "www.dailymed.nlm.nih.gov", "api.fda.gov"}
    )
    url = (
        f"https://dailymed.nlm.nih.gov/dailymed/services/v2/spls/{set_id}.xml"
        if use_spl
        else original_url
    )
    key = hashlib.sha256(original_url.encode()).hexdigest()[:20]
    path = directory / f"{key}.txt"
    result = {
        "key": key,
        "url": original_url,
        "retrievalUrl": url,
        "title": source.get("title", ""),
    }
    if path.exists() and not refresh:
        metadata = path.with_suffix(".json")
        if metadata.exists():
            result = json.loads(metadata.read_text(encoding="utf-8"))
            if result.get("status") in {"downloaded", "cached"} and result.get(
                "sha256"
            ):
                result["status"] = "cached"
                return result
            result = {
                "key": key,
                "url": original_url,
                "retrievalUrl": url,
                "title": source.get("title", ""),
            }
    try:
        request = urllib.request.Request(
            url, headers={"User-Agent": "Journal-drug-library-review/1.0"}
        )
        with urllib.request.urlopen(request, timeout=30) as response:
            data = response.read()
            result["httpStatus"] = response.status
            result["resolvedUrl"] = response.geturl()
        if data.startswith(b"%PDF"):
            text = None
            result["requiresPdfReview"] = True
        elif use_spl:
            root = ET.fromstring(data)
            ns = {"s": "urn:hl7-org:v3"}
            products = []
            for product in root.findall(
                ".//s:manufacturedProduct/s:manufacturedProduct", ns
            ):
                name = product.find("s:name", ns)
                form = product.find("s:formCode", ns)
                ingredients = []
                for ingredient in product.findall(".//s:ingredient", ns):
                    if not ingredient.get("classCode", "").startswith("ACTI"):
                        continue
                    substance = ingredient.find("s:ingredientSubstance/s:name", ns)
                    numerator = ingredient.find("s:quantity/s:numerator", ns)
                    denominator = ingredient.find("s:quantity/s:denominator", ns)
                    basis = ingredient.find(
                        "s:ingredientSubstance/s:asEquivalentEntity/s:definingSubstance/s:name",
                        ns,
                    )
                    ingredients.append(
                        {
                            "name": substance.text if substance is not None else None,
                            "numerator": dict(numerator.attrib)
                            if numerator is not None
                            else None,
                            "denominator": dict(denominator.attrib)
                            if denominator is not None
                            else None,
                            "strengthBasis": basis.text if basis is not None else None,
                        }
                    )
                if ingredients:
                    item = {
                        "name": name.text if name is not None else None,
                        "form": form.get("displayName") if form is not None else None,
                        "ingredients": ingredients,
                    }
                    if item not in products:
                        products.append(item)
            result["products"] = products
            chunks = []
            for section in root.findall(".//s:section", ns):
                title = section.find("s:title", ns)
                body = section.find("s:text", ns)
                code = section.find("s:code", ns)
                heading = (
                    " ".join(title.itertext())
                    if title is not None
                    else (code.get("displayName", "") if code is not None else "")
                )
                if body is not None:
                    chunks.append(
                        "\n## "
                        + " ".join(heading.split())
                        + "\n"
                        + " ".join(" ".join(body.itertext()).split())
                    )
            if not chunks:
                raise ValueError("SPL did not contain readable label sections")
            text = "\n".join(chunks)
            version = root.find("s:versionNumber", ns)
            effective = root.find("s:effectiveTime", ns)
            result["version"] = version.get("value") if version is not None else None
            result["effectiveDate"] = (
                effective.get("value") if effective is not None else None
            )
        else:
            text = data.decode("utf-8", errors="replace")
            if any(
                marker in text
                for marker in (
                    "body.cookies-required",
                    "cf-chl-",
                    "Checking your browser",
                )
            ):
                raise ValueError("Access challenge returned instead of source content")
            text = re.sub(
                r"<(script|style|noscript)\b[^>]*>.*?</\1>", "", text, flags=re.S | re.I
            )
            text = re.sub(
                r"<(?:/p|/div|/h[1-6]|br|/li|/tr)\b[^>]*>", "\n", text, flags=re.I
            )
            text = unescape(re.sub(r"<[^>]+>", " ", text))
            text = "\n".join(
                " ".join(line.split()) for line in text.splitlines() if line.strip()
            )
        result["sha256"] = hashlib.sha256(data).hexdigest()
        snapshot = directory / f"{key}-{result['sha256'][:16]}.source"
        snapshot.write_bytes(data)
        result["snapshotFile"] = snapshot.as_posix()
        result["status"] = "requires_pdf_review" if text is None else "downloaded"
        if text is not None:
            result["textFile"] = path.as_posix()
            path.write_text(text, encoding="utf-8")
    except (OSError, ValueError, ET.ParseError) as error:
        result["status"] = "failed"
        result["error"] = str(error)
    path.with_suffix(".json").write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--batch", required=True, action="append")
    parser.add_argument("--labels-only", action="store_true")
    parser.add_argument(
        "--output-dir", type=Path, default=ROOT / "build/library-sources"
    )
    parser.add_argument(
        "--proxy", help="Optional explicit proxy; otherwise use the environment"
    )
    parser.add_argument("--refresh", action="store_true")
    args = parser.parse_args()
    if args.proxy:
        urllib.request.install_opener(
            urllib.request.build_opener(
                urllib.request.ProxyHandler({"http": args.proxy, "https": args.proxy})
            )
        )
    manifest = json.loads(
        (ROOT / "tools/drugdata/review_manifest.json").read_text(encoding="utf-8")
    )["entries"]
    substances = json.loads(
        (ROOT / "app/src/main/res/raw/substances.json").read_text(encoding="utf-8")
    )["substances"]
    batches = {batch: {} for batch in args.batch}
    members = [s for s in substances if manifest[s["id"]]["batch"] in batches]
    assert set(args.batch) <= {manifest[s["id"]]["batch"] for s in members}, (
        "Unknown batch"
    )
    sources = {}
    for substance in members:
        refs = substance.get("sourceRefs", []) + substance.get("clinicalInfo", {}).get(
            "sourceRefs", []
        )
        membership = batches[manifest[substance["id"]]["batch"]]
        membership[substance["id"]] = []
        for source in refs:
            if args.labels_only and source.get("sourceType") != "regulatory-label":
                continue
            sources[source["url"]] = source
            if source["url"] not in membership[substance["id"]]:
                membership[substance["id"]].append(source["url"])
    args.output_dir.mkdir(parents=True, exist_ok=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
        results = list(
            executor.map(
                lambda source: fetch(source, args.output_dir, args.refresh),
                sources.values(),
            )
        )
    by_url = {source["url"]: source for source in results}
    for batch, membership in batches.items():
        urls = dict.fromkeys(
            url for candidates in membership.values() for url in candidates
        )
        batch_sources = [by_url[url] for url in urls]
        report = {"batch": batch, "membership": membership, "sources": batch_sources}
        (args.output_dir / f"{batch}.json").write_text(
            json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
        readable = sum(source["status"] != "failed" for source in batch_sources)
        print(
            f"{batch}: {len(membership)} entries, {readable}/{len(batch_sources)} candidate documents retrieved"
        )


if __name__ == "__main__":
    main()
