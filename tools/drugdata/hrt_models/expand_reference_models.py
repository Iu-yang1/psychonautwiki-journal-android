"""Extend immutable v1 profiles with independently fitted, analyte-specific references."""

import json
import math
from copy import deepcopy
import numpy as np
from scipy.integrate import quad
from scipy.linalg import expm
from scipy.optimize import brentq, least_squares, minimize_scalar
from fit_reference_models import (
    ROOT,
    HERE,
    REGISTRY,
    EVIDENCE,
    build as original_models,
    validate_models,
)

VERSION = "2026-11-05.2"
SOURCES = {
    "kuhnz1998": {
        "url": "https://pubmed.ncbi.nlm.nih.gov/9793623/",
        "kind": "human-single-dose-pk",
        "population": "绝经后女性，4mg口服EV交叉试验；参考制剂组。",
    },
    "wiemeyer1986": {
        "url": "https://pubmed.ncbi.nlm.nih.gov/3814225/",
        "kind": "published-apparent-pk-parameters",
        "population": "绝经后女性，单次10mg EN肌注，RIA；按摘要表观参数重建。",
    },
    "en_mass": {
        "url": "https://comptox.epa.gov/dashboard/chemical/details/DTXSID3023001",
        "kind": "molecular-mass",
    },
    "divigel_label": {
        "url": "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=59e610dc-883e-4f07-9ec6-a5841b0bf9fb",
        "kind": "regulatory-label",
        "population": "0.1% Divigel，上大腿，14天，表2未校正基线。",
    },
    "divigel2026": {
        "url": "https://link.springer.com/article/10.1007/s40268-026-00552-x",
        "kind": "comparison-study-not-pooled",
        "population": "日本绝经后女性10人，400cm²；其70.1h终末时间未与旧标签10h混用。",
    },
    "evamist_label": {
        "url": "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=9a0aa631-133d-406b-9d32-8a1a99af4e50",
        "kind": "regulatory-label",
        "population": "绝经后女性，第14天前臂喷雾，1/2/3喷分别建模。",
    },
    "evamist_fda": {
        "url": "https://www.accessdata.fda.gov/drugsatfda_docs/nda/2007/022014s000_PharmR.pdf",
        "kind": "regulatory-review",
    },
    "ee_smpc": {
        "url": "https://www.medicines.org.uk/emc/product/1131/smpc",
        "kind": "manufacturer-smpc",
        "population": "Microgynon30复方中的EE成分；不推算复方的LNG或抑制作用。",
    },
    "e4_label": {
        "url": "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=c5270073-d083-4109-ae4b-156986175e0a",
        "kind": "regulatory-label",
        "population": "NEXTSTELLIS空腹研究的无水E4成分，非单药研究；不推算DRSP作用。",
    },
    "cee_label": {
        "url": "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=1fcd61a8-1a67-4785-a51a-45fca6fbb32b",
        "kind": "regulatory-label",
        "population": "绝经后女性，Premarin0.625mg混合物；分别输出基线校正E1与未结合Equilin。",
    },
}


def bat(t, a, c):
    if t <= 0:
        return 0.0
    if abs(a - c) < 1e-8 * max(a, c):
        return a * t * math.exp(-a * t)
    return a / (a - c) * (math.exp(-c * t) - math.exp(-a * t))


def steady(t, a, c):
    if abs(a - c) < 1e-8 * max(a, c):
        decay = math.exp(-a * 24)
        return a * math.exp(-a * t) * (t / (1 - decay) + 24 * decay / (1 - decay) ** 2)
    return (
        a
        / (a - c)
        * (
            math.exp(-c * t) / (-math.expm1(-c * 24))
            - math.exp(-a * t) / (-math.expm1(-a * 24))
        )
    )


def peak_of(function):
    result = minimize_scalar(
        lambda t: -function(t),
        bounds=(0, 24),
        method="bounded",
        options={"xatol": 1e-10},
    )
    return result.x, -result.fun


def absorption_for_peak(tmax, elimination):
    return brentq(
        lambda a: math.log(a / elimination) / (a - elimination) - tmax,
        elimination * 1.00001,
        100,
    )


def model(
    identifier, medication, name, route, chemical, form, a, c, scale, source, **extra
):
    result = dict(
        id=identifier,
        name=name,
        medicationId=REGISTRY[medication],
        route=route,
        administrationRoutes=[
            "TRANSDERMAL"
            if route in ("GEL", "SPRAY")
            else "INTRAMUSCULAR"
            if route == "INJECTION"
            else "ORAL"
        ],
        chemicalFormId=chemical,
        formulationId=form,
        kernel="TWO_STAGE",
        absorptionPerHour=a,
        eliminationPerHour=c,
        responseScale=scale,
        activeFraction=1.0,
        driverUnit="CHEMICAL_FORM_MG",
        version=VERSION,
        sources=[source],
        assumptions=["群体研究参考；线性叠加及参考范围外的剂量缩放不是个体临床验证。"],
        reviewed=True,
    )
    result.update(extra)
    return result


def extensions():
    models, checks = [], []

    def check(name, actual, expected, tolerance, source, calibration=False):
        actual = float(actual)
        passed = abs(actual - expected) <= tolerance
        checks.append(
            dict(
                name=name,
                actual=actual,
                expected=expected,
                tolerance=tolerance,
                source=source,
                calibration=calibration,
                passed=bool(passed),
            )
        )
        if not passed:
            raise ValueError(f"{name}: {actual} vs {expected}, tolerance {tolerance}")

    c = math.log(2) / 15
    a = absorption_for_peak(10, c)
    scale = 42.9 / (4 * bat(10, a, c))
    models.append(
        model(
            "e2-oral-ev-reference-v1",
            "Oral Estradiol Valerate",
            "口服EV · Kuhnz1998参考",
            "ORAL",
            "chemical:estradiol-valerate",
            "form:oral-solid",
            a,
            c,
            scale,
            "kuhnz1998",
            activeFraction=272.38 / 356.50,
            assumptions=[
                "以参考组4mg酯剂量的峰值、峰时及15小时终末时间重建，输入为EV酯mg。",
                "有效参考响应未分离个人基线，未复现肠肝循环的每个次峰；不能替代实测。",
                "剂量缩放与多次叠加为研究外推。白蛋白/SHBG和内源分泌未个体化。",
            ],
        )
    )
    check("oral_ev_peak", 4 * scale * bat(10, a, c), 42.9, 1e-6, "kuhnz1998", True)
    check("oral_ev_tmax", math.log(a / c) / (a - c), 10, 0.001, "kuhnz1998", True)
    models.append(
        model(
            "e2-im-en-reference-v1",
            "Estradiol Enanthate Injection",
            "EN单用肌注 · Wiemeyer1986表观参数",
            "INJECTION",
            "chemical:estradiol-enanthate",
            "form:oil-injection",
            1.505 / 24,
            0.12445 / 24,
            1e6 / 5087,
            "wiemeyer1986",
            activeFraction=272.38 / 384.56,
            sources=["wiemeyer1986", "en_mass"],
            assumptions=[
                "按论文摘要给出的吸收、消除速率及5087L表观容积重建，剂量口径为实际EN酯mg。",
                "该容积为给定剂量口径的表观拟合值，不是实测生理分布容积；未从原图独立复核峰值。",
                "研究为EN单用肌注与RIA检测，不套用DHPA复方、皮下途径或自配载体。",
                "基线及个体差异不作自动校正，重复叠加仅为研究外推。",
            ],
        )
    )
    check("en_absorption_half_days", math.log(2) / 1.505, 0.46, 0.005, "wiemeyer1986")
    check("en_terminal_half_days", math.log(2) / 0.12445, 5.57, 0.01, "wiemeyer1986")
    # Fix the label's terminal time, fit the steady peak/area, then infer an effective phase delay.
    c = math.log(2) / 10

    def ratio(a):
        return peak_of(lambda t: steady(t, a, c))[1] / (1 / (c * 24))

    a = brentq(lambda value: ratio(value) - 51.5 / 30.5, c * 1.0001, 100)
    peak_time, _ = peak_of(lambda t: steady(t, a, c))
    lag = (8 - peak_time) % 24
    scale = 732 * c
    models.append(
        model(
            "e2-divigel-reference-v1",
            "Estradiol Gel",
            "Divigel0.1% · 上大腿标签参考",
            "GEL",
            "chemical:estradiol",
            "form:gel",
            a,
            c,
            scale,
            "divigel_label",
            requiresProduct=True,
            lagHours=lag,
            referenceFamily="divigel01",
            allowedSites=[
                "upperthigh",
                "leftupperthigh",
                "rightupperthigh",
                "上大腿",
                "左上大腿",
                "右上大腿",
            ],
            sources=["divigel_label", "divigel2026"],
            assumptions=[
                "采用旧标签1mg、第14天未校正基线的AUC/峰值及约10小时终末时间。",
                "相位延迟由稳态峰时拟合，是经验参数，不是独立测量的皮肤吸收延迟。",
                "2026年另一个400cm²研究报告更长终末时间，未将两组参数拼成同一模型。",
                "限0.1%制剂、上大腿且未清洗条件；线性剂量缩放仅为近似，不能套用EstroGel或其他部位。",
                "单次及非每日给药响应由稳态数据外推，未独立临床验证。",
            ],
        )
    )
    check("divigel_ss_auc", scale / c, 732, 0.001, "divigel_label", True)
    check(
        "divigel_ss_peak",
        scale * peak_of(lambda t: steady(t, a, c))[1],
        51.5,
        0.001,
        "divigel_label",
        True,
    )
    for count, cmax, area, tmax in [
        (1, 36.4, 471, 20),
        (2, 57.4, 736, 18),
        (3, 54.1, 742, 20),
    ]:

        def spray_ratio(rate):
            return peak_of(lambda t: steady(t, 2 * rate, rate))[1] * (rate * 24)

        c = brentq(lambda rate: spray_ratio(rate) - cmax / (area / 24), 0.0001, 5)
        a = 2 * c
        peak_time, _ = peak_of(lambda t: steady(t, a, c))
        lag = (tmax - peak_time) % 24
        dose = count * 1.53
        scale = area * c / dose
        models.append(
            model(
                f"e2-evamist-{count}-reference-v1",
                "Estradiol Spray",
                f"Evamist · {count}喷稳态参考",
                "SPRAY",
                "chemical:estradiol",
                "form:spray",
                a,
                c,
                scale,
                "evamist_label",
                requiresProduct=True,
                referenceFamily="evamist",
                referenceDoseMg=dose,
                lagHours=lag,
                allowedSites=[
                    "forearm",
                    "leftforearm",
                    "rightforearm",
                    "前臂",
                    "左前臂",
                    "右前臂",
                ],
                sources=["evamist_label", "evamist_fda"],
                assumptions=[
                    f"仅核对每日{count}喷、每喷1.53mg、前臂第14天的未校正基线数据。",
                    "吸收/周转速率及延迟为稳态形状的经验拟合，不是独立测量的生理半衰期。",
                    "单次和非每日响应属研究外推；标签与FDA评审不支持按喷数线性增加暴露。",
                    "其他剂量不套用此参数；1、2、3喷分别选择固定剂量响应，不外推到其他部位或载体。",
                ],
            )
        )
        check(f"spray{count}_auc", dose * scale / c, area, 0.001, "evamist_label", True)
        check(
            f"spray{count}_peak",
            dose * scale * peak_of(lambda t: steady(t, a, c))[1],
            cmax,
            0.001,
            "evamist_label",
            True,
        )
    # EE: reported biphasic response; source is the EE component of a combined oral product.
    cf, cs = math.log(2) / 1.5, math.log(2) / 20

    def ee_residual(x):
        a, odds, scale = np.exp(x)
        f = odds / (1 + odds)
        ptime, peak = peak_of(
            lambda t: scale * (f * bat(t, a, cf) + (1 - f) * bat(t, a, cs))
        )
        _, sspeak = peak_of(
            lambda t: scale * (f * steady(t, a, cf) + (1 - f) * steady(t, a, cs))
        )
        return [
            math.log(peak / 100),
            math.log(ptime / 1.5),
            math.log(sspeak / peak / 1.35),
        ]

    fit = least_squares(
        ee_residual,
        np.log([1.3, 2.4, 158]),
        max_nfev=1000,
        xtol=1e-12,
        gtol=1e-12,
        ftol=1e-12,
    )
    a, odds, scaledose = np.exp(fit.x)
    f = odds / (1 + odds)
    models.append(
        model(
            "ee-oral-reference-v1",
            "Ethinylestradiol",
            "炔雌醇EE · 独立分析物参考",
            "ORAL",
            "chemical:ethinylestradiol",
            "form:oral-solid",
            float(a),
            cs,
            float(scaledose / 0.03),
            "ee_smpc",
            analyteId="EE",
            kernel="BIPHASIC",
            fastEliminationPerHour=cf,
            fastFraction=float(f),
            requiresProduct=True,
            assumptions=[
                "输入为EE成分mg，输出为EE的pg/mL；不能与E2浓度相加或按效力换算。",
                "参考Microgynon30的EE成分；快相半衰期取报告1–2小时的中值1.5小时，慢相20小时。",
                "峰时取报告1–2小时的中值，重复峰值增幅以30–40%的中点作有效拟合。",
                "这些快慢相是经验分解；不模拟LNG、SHBG变化、药物相互作用或卵巢抑制。",
            ],
        )
    )
    check(
        "ee_fitted_summary_residual",
        float(np.max(np.abs(fit.fun))),
        0,
        0.001,
        "ee_smpc",
        True,
    )
    # E4: one terminal compartment cannot fit the sharp peak and long tail simultaneously.
    cs = math.log(2) / 27

    def e4_residual(x):
        a, cf, odds, scale = np.exp(x)
        f = odds / (1 + odds)
        peak_time, peak = peak_of(
            lambda t: scale * (f * steady(t, a, cf) + (1 - f) * steady(t, a, cs))
        )
        area = scale * (f / cf + (1 - f) / cs)
        single = quad(
            lambda t: scale * (f * bat(t, a, cf) + (1 - f) * bat(t, a, cs)), 0, 24
        )[0]
        return [
            math.log(peak / 17900),
            math.log(peak_time / 0.5),
            math.log(area / 59100),
            math.log(area / single / 1.6),
        ]

    fit = least_squares(
        e4_residual,
        np.log([2.05, 2.07, 43.4, 44300]),
        max_nfev=1000,
        bounds=(np.log([0.1, 0.05, 0.01, 100]), np.log([100, 10, 10000, 1e7])),
    )
    a, cf, odds, scaledose = np.exp(fit.x)
    f = odds / (1 + odds)
    models.append(
        model(
            "e4-oral-reference-v1",
            "Estetrol",
            "雌四醇E4 · 无水成分参考",
            "ORAL",
            "chemical:estetrol",
            "form:oral-solid",
            float(a),
            cs,
            float(scaledose / 14.2),
            "e4_label",
            analyteId="E4",
            kernel="BIPHASIC",
            fastEliminationPerHour=float(cf),
            fastFraction=float(f),
            requiresProduct=True,
            assumptions=[
                "输入无水E4成分mg，输出E4的pg/mL；15mg单水合物按标签等于14.2mg无水E4，不是E2当量。",
                "采用NEXTSTELLIS空腹多次给药资料；不将DRSP视作E4，也不模拟其作用。",
                "快相与比例由峰值、暴露和蓄积资料拟合；27小时终末时间来自标签，快相不是独立测得的清除率。",
                "按报告均值近似重建，峰时、蓄积及稳态天数只是群体参考，不适合作为个体调量依据。",
            ],
        )
    )
    check(
        "e4_summary_log_residual",
        float(np.max(np.abs(fit.fun))),
        0,
        0.05,
        "e4_label",
        True,
    )
    for analyte, peak, tmax, half, area, cv in [
        ("E1", 64, 9.6, 20.2, 1723, 0.42),
        ("EQUILIN", 31, 7.9, 12.9, 602, 0.38),
    ]:
        c = math.log(2) / half
        a = absorption_for_peak(tmax, c)
        scale = area * c / 0.625
        models.append(
            model(
                f"cee-{analyte.lower()}-reference-v1",
                "Conjugated Estrogens",
                f"Premarin · {analyte}成分响应",
                "ORAL",
                "chemical:conjugated-estrogens",
                "form:oral-solid",
                a,
                c,
                scale,
                "cee_label",
                analyteId=analyte,
                requiresProduct=True,
                referenceFamily="premarin",
                assumptions=[
                    "输入为Premarin标示CEE混合物mg，不假定所有CEE品牌具有相同组成或暴露。",
                    f"输出仅为{analyte}的未结合成分浓度；不是总雌激素、总受体效力或E2浓度。",
                    "E1采用标签基线校正值，Equilin按其独立测量值；以AUC与峰时标定，峰值在报告变异内比较。",
                    "多次给药线性叠加为研究外推，不建模成分间的完整代谢转换。",
                ],
            )
        )
        check(f"cee_{analyte}_auc", 0.625 * scale / c, area, 0.001, "cee_label", True)
        check(
            f"cee_{analyte}_peak_within_cv",
            0.625 * scale * bat(tmax, a, c),
            peak,
            peak * cv,
            "cee_label",
        )
    return models, dict(
        version=VERSION,
        scope="population-reference reconstruction, not personal clinical validation",
        checks=checks,
    )


def matrix_fixture(m):
    times = [0, 0.25, 0.5, 1, 2, 4, 8, 12, 24, 48, 96, 168, 336]
    a, c = m["absorptionPerHour"], m["eliminationPerHour"]
    driver = m.get("referenceDoseMg") or 1.0
    values = []
    for time in times:
        t = time - m.get("lagHours", 0)
        if t <= 0:
            values.append(0.0)
            continue
        if m["kernel"] == "BIPHASIC":
            f = m["fastFraction"]
            cf = m["fastEliminationPerHour"]
            q = np.array([[-a, 0, 0], [a * f, -cf, 0], [a * (1 - f), 0, -c]])
            state = expm(q * t) @ np.array([1, 0, 0])
            v = state[1] + state[2]
        else:
            q = np.array([[-a, 0], [a, -c]])
            v = expm(q * t)[1, 0]
        values.append(float(v * driver * m["responseScale"]))
    return dict(profileId=m["id"], driver=driver, timesHours=times, pgMl=values)


def bundle():
    old = deepcopy(original_models())
    new, report = extensions()
    # Keep each released profile's own version and numerical values intact.
    for m in old:
        if m["route"] in ("ORAL", "SUBLINGUAL", "GEL", "PATCH"):
            m["compatibleMedicationIds"] = [REGISTRY["Estradiol"]]
    for m in new:
        if m["chemicalFormId"] == "chemical:estradiol" and m["route"] in (
            "GEL",
            "SPRAY",
        ):
            m["compatibleMedicationIds"] = [REGISTRY["Estradiol"]]
    return dict(
        version=VERSION,
        sources={**EVIDENCE["sources"], **SOURCES},
        assumptions=EVIDENCE["assumptions"],
        models=old + new,
    ), report


if __name__ == "__main__":
    result, report = bundle()
    validate_models(original_models())
    (ROOT / "app/src/main/res/raw/hrt_reference_models.json").write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (HERE / "expansion_anchor_validation.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    new = result["models"][6:]
    (HERE / "expansion_numerical_fixtures.json").write_text(
        json.dumps(
            dict(version=VERSION, fixtures=[matrix_fixture(m) for m in new]), indent=2
        )
        + "\n",
        encoding="utf-8",
    )
    (HERE / "evidence_expansion.json").write_text(
        json.dumps(
            dict(
                version=VERSION,
                sources=SOURCES,
                method="Fixed literature anchors and explicitly documented effective fits; preserve all legacy profile parameters.",
            ),
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    print(
        f"Built {len(result['models'])} profiles, {len(report['checks'])} expansion anchors, {len(new)} independent matrix fixtures"
    )
