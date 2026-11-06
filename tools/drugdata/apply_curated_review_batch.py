#!/usr/bin/env python3
"""Merge a complete, manually curated review batch into the library ledgers."""

import argparse
import json
from datetime import date
from pathlib import Path
from tempfile import NamedTemporaryFile


ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "tools/drugdata"
FIELDS = (
    "identity",
    "commonNames",
    "categories",
    "roas",
    "summary",
    "clinicalInfo.indications",
    "clinicalInfo.contraindications",
    "clinicalInfo.majorWarnings",
    "clinicalInfo.majorInteractions",
    "clinicalInfo.monitoring",
    "doseUseReferences",
    "timeCourse",
    "sourceRefs",
    "translation",
    "interactions",
)
STATES = {"verified", "not_applicable", "evidence_unavailable"}


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def write(path, value):
    temporary = NamedTemporaryFile(
        mode="w",
        encoding="utf-8",
        newline="\n",
        dir=path.parent,
        delete=False,
    )
    try:
        with temporary:
            json.dump(value, temporary, ensure_ascii=False, indent=2)
            temporary.write("\n")
        Path(temporary.name).replace(path)
    finally:
        Path(temporary.name).unlink(missing_ok=True)


def validate_date(value, context):
    assert isinstance(value, str), f"{context}: reviewedDate must be a string"
    date.fromisoformat(value)


def validate_source(source, context):
    assert isinstance(source, dict), f"{context}: source must be an object"
    for key in ("id", "url", "title", "region", "version", "accessedDate"):
        assert source.get(key), f"{context}: source missing {key}"
    validate_date(source["accessedDate"], f"{context}.accessedDate")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("batch_file", type=Path)
    args = parser.parse_args()

    batch_data = read(args.batch_file)
    assert batch_data.get("schemaVersion") == 1, "Unsupported batch schema"
    batch_name = batch_data["batch"]
    assert batch_name and batch_data.get("entries"), "Batch has no entries"

    manifest_path = DATA / "review_manifest.json"
    overrides_path = DATA / "library_overrides.json"
    manifest = read(manifest_path)
    overrides = read(overrides_path)
    reviewed_ids = set()

    for identifier, decision in batch_data["entries"].items():
        assert identifier not in reviewed_ids, f"Duplicate identity {identifier}"
        reviewed_ids.add(identifier)
        row = manifest["entries"].get(identifier)
        assert row, f"Unknown identity {identifier}"
        assert row["batch"] == batch_name, (
            f"{identifier}: manifest batch {row['batch']} != {batch_name}"
        )
        assert not any(
            field.get("status") != "pending" for field in row["fields"].values()
        ), f"{identifier}: refusing to overwrite a closed review"

        sources = decision.get("sources", [])
        assert sources, f"{identifier}: missing sources"
        source_ids = [source.get("id") for source in sources]
        assert len(source_ids) == len(set(source_ids)), (
            f"{identifier}: duplicate source"
        )
        for source in sources:
            validate_source(source, f"{identifier}.source")

        fields = decision.get("fields", {})
        assert set(fields) == set(FIELDS), f"{identifier}: expected all review fields"
        for field_name, review in fields.items():
            assert review.get("status") in STATES, (
                f"{identifier}.{field_name}: invalid status"
            )
            validate_date(review.get("reviewedDate"), f"{identifier}.{field_name}")
            refs = review.get("sourceIds", [])
            assert all(source_id in source_ids for source_id in refs), (
                f"{identifier}.{field_name}: unknown source reference"
            )
            if review["status"] == "verified":
                assert refs, f"{identifier}.{field_name}: verified without source"
            else:
                assert review.get("reason"), (
                    f"{identifier}.{field_name}: missing reason"
                )
            if review["status"] == "evidence_unavailable":
                assert decision.get("searches"), (
                    f"{identifier}.{field_name}: missing search record"
                )

        override = decision.get("override")
        assert isinstance(override, dict), f"{identifier}: missing content override"
        assert "fields" in override and "locales" in override, (
            f"{identifier}: incomplete content override"
        )
        assert "name" not in override["fields"] and "id" not in override["fields"], (
            f"{identifier}: immutable identity in override"
        )
        for locale in ("zh-CN", "en"):
            locale_override = override["locales"].get(locale)
            assert isinstance(locale_override, dict), (
                f"{identifier}: missing {locale} content"
            )
            assert locale_override.get("displayName"), (
                f"{identifier}: missing {locale} display name"
            )

        row["fields"] = fields
        row["sources"] = sources
        row["searches"] = decision.get("searches", [])
        row["aliasDecisions"] = decision.get("aliasDecisions", [])
        overrides["entries"][identifier] = override

    expected = {
        identifier
        for identifier, row in manifest["entries"].items()
        if row["batch"] == batch_name
    }
    assert expected == reviewed_ids, (
        f"{batch_name}: expected {len(expected)} identities, got {len(reviewed_ids)}"
    )

    write(manifest_path, manifest)
    write(overrides_path, overrides)
    print(f"Merged {len(reviewed_ids)} curated identities from {batch_name}")


if __name__ == "__main__":
    main()
