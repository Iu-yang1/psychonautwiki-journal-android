"""Verify bundled model parameters against the frozen evidence and numerical fit."""
import json
import math

from fit_reference_models import ROOT, HERE, VERSION, EVIDENCE, build, independent_values, validate_models
from expand_reference_models import bundle as expanded_bundle, matrix_fixture, SOURCES


def close_tree(actual, expected, location="root"):
    if isinstance(expected, dict):
        assert isinstance(actual, dict) and actual.keys() == expected.keys(), location
        for key in expected:
            close_tree(actual[key], expected[key], f"{location}.{key}")
    elif isinstance(expected, list):
        assert isinstance(actual, list) and len(actual) == len(expected), location
        for index, value in enumerate(expected):
            close_tree(actual[index], value, f"{location}[{index}]")
    elif isinstance(expected, (int, float)) and not isinstance(expected, bool):
        assert math.isclose(actual, expected, rel_tol=1e-8, abs_tol=1e-7), location
    else:
        assert actual == expected, location


if __name__ == "__main__":
    models = build()
    report = validate_models(models)
    bundle = json.loads((ROOT / "app/src/main/res/raw/hrt_reference_models.json").read_text(encoding="utf-8"))
    expanded, expansion_report = expanded_bundle()
    close_tree(bundle, expanded)
    close_tree([{key: item[key] for key in expected} for item, expected in zip(bundle["models"][:6], models)], models)
    close_tree({key: bundle["sources"][key] for key in EVIDENCE["sources"]}, EVIDENCE["sources"])
    close_tree({key: bundle["sources"][key] for key in SOURCES}, SOURCES)
    close_tree(bundle["assumptions"], EVIDENCE["assumptions"])
    close_tree(json.loads((HERE / "anchor_validation.json").read_text(encoding="utf-8")), report)
    fixtures = json.loads((HERE / "numerical_fixtures.json").read_text(encoding="utf-8"))
    close_tree(fixtures, dict(version=VERSION, fixtures=[independent_values(model) for model in models]))
    close_tree(json.loads((HERE / "expansion_anchor_validation.json").read_text(encoding="utf-8")), expansion_report)
    close_tree(json.loads((HERE / "expansion_numerical_fixtures.json").read_text(encoding="utf-8")), dict(version=expanded["version"], fixtures=[matrix_fixture(model) for model in expanded["models"][6:]]))
    print(f"Verified {len(expanded['models'])} profiles, {len(report['checks']) + len(expansion_report['checks'])} study-anchor checks and independent matrix fixtures")
