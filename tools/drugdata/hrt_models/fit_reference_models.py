"""Generate research-reference E2 kernels and independent numerical fixtures.

Fits are to explicitly listed literature anchors, not individual patients.
Requires numpy/scipy. Clinical validation is not asserted by this program.
"""
import json
import math
from pathlib import Path

import numpy as np
from scipy.integrate import quad
from scipy.linalg import expm
from scipy.optimize import brentq, minimize_scalar

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
REGISTRY = json.loads((ROOT / "tools/drugdata/medication_ids.json").read_text(encoding="utf-8"))
EVIDENCE = json.loads((HERE / "evidence.json").read_text(encoding="utf-8"))
VERSION = EVIDENCE["version"]


def matrix_response(t, absorption, elimination, transfer=None):
    if t < 0:
        return 0.0
    if transfer is None:
        matrix = np.array([[-absorption, 0], [absorption, -elimination]])
        return float(expm(matrix * t)[1, 0])
    matrix = np.array([[-absorption, 0, 0], [absorption, -transfer, 0], [0, transfer, -elimination]])
    return float(expm(matrix * t)[2, 0])


def fast_response(t, a, c):
    return a / (a - c) * (math.exp(-c * t) - math.exp(-a * t)) if t >= 0 else 0.0


def three_response(t, a, b, c):
    return a * b * (math.exp(-a * t) / ((a-b)*(a-c)) - math.exp(-b*t) / ((a-b)*(b-c)) + math.exp(-c*t) / ((a-c)*(b-c))) if t >= 0 else 0.0


def profile(identifier, name, medication, route, admin, chemical, form, kernel, a, c, scale, fraction, sources, assumptions, b=None):
    return dict(id=identifier, name=name, medicationId=REGISTRY[medication], route=route, administrationRoutes=admin,
                chemicalFormId=chemical, formulationId=form, kernel=kernel, absorptionPerHour=a, eliminationPerHour=c,
                transferPerHour=b, responseScale=scale, activeFraction=fraction, version=VERSION,
                sources=sources, assumptions=assumptions, reviewed=True,
                driverUnit="NOMINAL_E2_MG_PER_HOUR" if route=="PATCH" else "ACTIVE_E2_MG")


def build():
    models = []
    for ester, medication, mw, peak, peak_h, b, c in [
        ("ev", "Estradiol Valerate Injection", 356.50, 667.0, 52.8, 4.85/24, 1.24/24),
        ("ec", "Estradiol Cypionate Injection", 396.5622, 337.0, 93.6, 3.57/24, .669/24),
    ]:
        def peak_time(a):
            return minimize_scalar(lambda t: -three_response(t, a, b, c), bounds=(0, 720), method="bounded", options={"xatol": 1e-10}).x
        a = brentq(lambda value: peak_time(value) - peak_h, 1e-5, min(b, c)*.99, xtol=1e-14)
        fraction = 272.38/mw
        scale = peak/(5*fraction*three_response(peak_h, a, b, c))
        models.append(profile("e2-im-"+ester+"-reference-v1", ester.upper()+" 油剂肌注研究参考", medication, "INJECTION", ["INTRAMUSCULAR"],
            "chemical:estradiol-"+("valerate" if ester=="ev" else "cypionate"), "form:oil-injection", "THREE_STAGE", a, c, scale, fraction,
            ["oriowo1980", "rothman2024", "estrannaise_temporal_priors", "molecular_weights"],
            ["油剂肌注；不外推皮下注射或水性悬浮剂。", "转运/周转时间常数为开放模型先验；吸收率与幅度按报告峰值重拟合，不能解释为生理清除率。", "峰值资料未分离个体基线；重复给药为线性研究外推。"], b))
    c = .05
    a = brentq(lambda value: math.log(value/c)/(value-c)-8, c*1.0001, 3)
    scale = 35/fast_response(8, a, c)
    oral_auc = quad(lambda t: scale*fast_response(t, a, c), 0, 8)[0]
    models.append(profile("e2-oral-reference-v1", "微粒化 E2 口服研究参考", "Oral Estradiol", "ORAL", ["ORAL"], "chemical:estradiol", "form:oral-solid",
        "TWO_STAGE", a, c, scale, 1, ["doll2022", "oral_smpc"], ["以单次 1 mg 研究观测峰值拟合；表观半衰期先验约 13.9 小时。", "8 小时是该研究最后采样点，不能据此断言真实峰值恰在 8 小时。", "未建模内源性分泌或个体首过代谢。"] ))
    sa = 1.8
    def sl_ratio(clearance):
        scalar = 144/fast_response(1, sa, clearance)
        return quad(lambda t: scalar*fast_response(t, sa, clearance), 0, 8)[0]/oral_auc
    sc = brentq(lambda value: sl_ratio(value)-1.8, .6, 1)
    ss = 144/fast_response(1, sa, sc)
    models.append(profile("e2-sl-reference-v1", "微粒化 E2 舌下研究参考", "Sublingual/Buccal Estradiol", "SUBLINGUAL", ["SUBLINGUAL"], "chemical:estradiol", "form:oral-solid",
        "TWO_STAGE", sa, sc, ss, 1, ["doll2022"], ["按 1 小时采样值及 0–8 小时暴露比拟合，不将连续峰值时间冒充实测时间。", "口腔颊黏膜、吞咽比例与含服时长不作自动等效换算。"] ))
    ga, gc = math.log(2)/36, math.log(2)
    def gel_day14(t):
        return .75*sum(fast_response(t+i*24, ga, gc) for i in range(14))
    gs = 572.48/quad(gel_day14, 0, 24)[0]
    models.append(profile("e2-gel006-reference-v1", "EstroGel 0.06% 上臂研究参考", "Estradiol Gel", "GEL", ["TRANSDERMAL"], "chemical:estradiol", "form:gel",
        "TWO_STAGE", ga, gc, gs, 1, ["estrogel_fda", "estrogel_label"], ["按 0.75 mg、上臂、不清洗的基线校正资料拟合。", "假设终末 36 小时主要反映皮肤吸收，系统周转先验为 1 小时；不是独立测得的清除率。", "研究变异很大；部位、清洗、防晒、其他浓度或载体均不自动等效。"] ))
    pc, wear = math.log(2)/7, 80.0
    def patch_ss(t, a):
        return fast_response(t, a, pc)+fast_response(wear, a, pc)*math.exp(-pc*t)/(1-math.exp(-pc*wear))
    def ratio(a):
        average = quad(lambda t: patch_ss(t, a), 0, wear)[0]/wear
        return patch_ss(wear, a)/average
    pa = brentq(lambda a: ratio(a)-(41-11.7)/(57-11.7), .001, .08)
    ps = (57-11.7)/(quad(lambda t: patch_ss(t, pa), 0, wear)[0]/wear)/(50/24000)
    models.append(profile("e2-patch-tw-reference-v1", "原始每周两次贴片腹部研究参考", "Estradiol Transdermal Patch", "PATCH", ["TRANSDERMAL"], "chemical:estradiol", "form:patch",
        "PATCH_DEPOT", pa, pc, ps, 1, ["patch_label"], ["名义释放率为经验储库驱动，不使用贴片总装载量。", "50 μg/天参考组的平均值和谷值按标签脚注 80 小时处理；已减去研究基线 11.7 pg/mL。", "实际贴上/移除区间决定输入；不能自动等同每周一次贴片或不同部位。"] ))
    return models


def independent_values(model):
    times = [0.0, .25, 1.0, 4.0, 8.0, 24.0, 48.0, 80.0, 96.0, 168.0, 336.0]
    driver = (50/24000) if model["route"]=="PATCH" else 1.0
    values = []
    for t in times:
        if model["route"]=="PATCH" and t>80:
            raw = matrix_response(80, model["absorptionPerHour"], model["eliminationPerHour"])*math.exp(-model["eliminationPerHour"]*(t-80))
        else:
            raw = matrix_response(t, model["absorptionPerHour"], model["eliminationPerHour"], model["transferPerHour"])
        values.append(raw*driver*model["responseScale"])
    return dict(profileId=model["id"], driver=driver, timesHours=times, pgMl=values, removedAtHours=80 if model["route"]=="PATCH" else None)


def validate_models(models):
    by_id = {model["id"]: model for model in models}
    checks = []
    def check(name, actual, expected, tolerance, source):
        actual = float(actual)
        passed = bool(abs(actual-expected) <= tolerance)
        checks.append(dict(name=name, actual=actual, expected=expected, tolerance=tolerance, source=source, passed=passed))
        if not passed:
            raise ValueError(f"Reference check failed: {name}: {actual} vs {expected}")
    def response(model, t):
        return matrix_response(t, model["absorptionPerHour"], model["eliminationPerHour"], model["transferPerHour"])*model["responseScale"]
    for suffix, peak, time_h in [("ev",667,52.8),("ec",337,93.6)]:
        model = by_id[f"e2-im-{suffix}-reference-v1"]
        def value(t):
            return response(model,t)*5*model["activeFraction"]
        maximum = minimize_scalar(lambda t:-value(t),bounds=(0,720),method="bounded").x
        check(suffix+"_peak",value(maximum),peak,0.001,"rothman2024")
        check(suffix+"_tmax",maximum,time_h,0.001,"rothman2024")
    oral = by_id["e2-oral-reference-v1"]
    sl = by_id["e2-sl-reference-v1"]
    check("oral_observed_peak",response(oral,8),35,1e-8,"doll2022")
    check("sl_observed_peak",response(sl,1),144,1e-8,"doll2022")
    ratio = quad(lambda t:response(sl,t),0,8)[0]/quad(lambda t:response(oral,t),0,8)[0]
    check("sl_oral_auc8_ratio",ratio,1.8,1e-7,"doll2022")
    gel = by_id["e2-gel006-reference-v1"]
    def day14(t):
        return .75*sum(response(gel,t+i*24) for i in range(14))
    check("gel_auc_day14",quad(day14,0,24)[0],572.48,0.001,"estrogel_fda")
    check("gel_auc_day1_within_reported_sd",quad(lambda t:.75*response(gel,t),0,24)[0],97.71,112.27,"estrogel_fda")
    max14 = -minimize_scalar(lambda t:-day14(t),bounds=(0,24),method="bounded").fun
    check("gel_peak_day14_within_reported_sd",max14,42.83,26.95,"estrogel_fda")
    patch = by_id["e2-patch-tw-reference-v1"]
    pc = patch["eliminationPerHour"]
    def steady(t):
        return 50/24000*(response(patch,t)+response(patch,80)*math.exp(-pc*t)/(1-math.exp(-pc*80)))
    check("patch_baseline_adjusted_average",quad(steady,0,80)[0]/80,57-11.7,0.001,"patch_label")
    check("patch_baseline_adjusted_trough",steady(80),41-11.7,0.001,"patch_label")
    maximum = -minimize_scalar(lambda t:-steady(t),bounds=(0,80),method="bounded").fun
    check("patch_peak_within_reported_sd",maximum,83-11.7,41,"patch_label")
    check("patch_post_removal_half_life",math.log(2)/pc,7,0.001,"patch_label")
    return dict(version=VERSION, scope="numerical and population-reference anchors; not individual clinical validation",checks=checks)


if __name__ == "__main__":
    models = build()
    report = validate_models(models)
    (HERE / "anchor_validation.json").write_text(json.dumps(report, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    output = dict(version=VERSION, sources=EVIDENCE["sources"], assumptions=EVIDENCE["assumptions"], models=models)
    (ROOT / "app/src/main/res/raw/hrt_reference_models.json").write_text(json.dumps(output, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    (HERE / "numerical_fixtures.json").write_text(json.dumps(dict(version=VERSION, fixtures=[independent_values(model) for model in models]), indent=2)+"\n", encoding="utf-8")
    print(f"Generated {len(models)} reference models across {len(set(m['route'] for m in models))} administration families")
