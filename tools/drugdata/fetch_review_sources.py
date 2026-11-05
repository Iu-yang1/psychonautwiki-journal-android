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
    url = (
        f"https://dailymed.nlm.nih.gov/dailymed/services/v2/spls/{set_id}.xml"
        if set_id and "dailymed.nlm.nih.gov" in original_url
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
        elif set_id and "dailymed.nlm.nih.gov" in original_url:
            root = ET.fromstring(data)
            ns = {"s": "urn:hl7-org:v3"}
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
    parser.add_argument("--batch", required=True)
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
    members = [s for s in substances if manifest[s["id"]]["batch"] == args.batch]
    assert members, "Unknown batch"
    sources = {}
    membership = {}
    for substance in members:
        refs = substance.get("sourceRefs", []) + substance.get("clinicalInfo", {}).get(
            "sourceRefs", []
        )
        membership[substance["id"]] = []
        for source in refs:
            sources[source["url"]] = source
            membership[substance["id"]].append(source["url"])
    args.output_dir.mkdir(parents=True, exist_ok=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
        results = list(
            executor.map(
                lambda source: fetch(source, args.output_dir, args.refresh),
                sources.values(),
            )
        )
    report = {"batch": args.batch, "membership": membership, "sources": results}
    (args.output_dir / f"{args.batch}.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    for result in results:
        print(result["status"], result["url"])


if __name__ == "__main__":
    main()
