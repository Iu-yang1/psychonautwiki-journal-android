package com.isaakhanimann.journal.data.hrt

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

data class TestosteroneReferencePoint(val month: Int, val mean: Double)
data class TestosteroneEstrogenContext(val id: String, val route: HormoneRoute, val timeHours: Double)

data class TestosteroneReference(
    val id: String,
    val label: String,
    val sourceTitle: String,
    val sourceUrl: String,
    val table: String,
    val participants: Int,
    val drug: AntiandrogenDrug,
    val routes: Set<HormoneRoute>,
    val regimen: String,
    val unit: String,
    val points: List<TestosteroneReferencePoint>,
) {
    fun ngDl(point: TestosteroneReferencePoint): Double = if (unit == "nmol/L") point.mean / .0347 else point.mean
}

data class AntiandrogenResidual(
    val drug: AntiandrogenDrug,
    val recordCount: Int,
    val totalRecordedMg: Double,
    val remainingDoseProxyMg: Double,
    val halfLifeHours: Double,
)

data class TestosteroneReferenceResult(
    val scenarios: List<TestosteroneScenario>,
    val notes: List<String>,
    val residuals: List<AntiandrogenResidual>,
)

object TestosteroneReferences {
    const val version = "2026-11-15"
    const val cpaPkUrl = "https://www.medicines.org.uk/emc/product/15996/smpc"
    const val spiroPkUrl = "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=8b76a904-6c99-485a-a05f-13a36c5f720c"
    private const val antiandrogenTitle = "Effect of Spironolactone and Cyproterone Acetate on Breast Growth in Transgender People: A Randomized Clinical Trial"
    private const val routeTitle = "Effectiveness and Safety of Different Estradiol Regimens in Transgender Females: A Randomized Controlled Trial"
    private val combinedRoutes = setOf(HormoneRoute.ORAL, HormoneRoute.GEL, HormoneRoute.PATCH)

    val studies = listOf(
        TestosteroneReference("cpa-2025", "E2＋CPA · 混合途径研究", antiandrogenTitle,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC12086416/", "表2", 32, AntiandrogenDrug.CPA,
            combinedRoutes, "CPA 12.5 mg/天；口服 EV 起始 4 mg/天或等效经皮 E2，按化验调量。研究包含片剂、凝胶、贴片，未分途径报告总 T。",
            "nmol/L", listOf(TestosteroneReferencePoint(0, 17.30), TestosteroneReferencePoint(3, .81), TestosteroneReferencePoint(6, 1.48))),
        TestosteroneReference("spiro-2025", "E2＋螺内酯 · 混合途径研究", antiandrogenTitle,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC12086416/", "表2", 31, AntiandrogenDrug.SPIRONOLACTONE,
            combinedRoutes, "螺内酯 100 mg/天；口服 EV 起始 4 mg/天或等效经皮 E2，按化验调量。研究包含片剂、凝胶、贴片，未分途径报告总 T。",
            "nmol/L", listOf(TestosteroneReferencePoint(0, 17.02), TestosteroneReferencePoint(3, 6.75), TestosteroneReferencePoint(6, 4.15))),
        TestosteroneReference("treat-patch-2024", "贴片 E2＋螺内酯 · TREAT", routeTitle,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC11220669/", "表3", 12, AntiandrogenDrug.SPIRONOLACTONE,
            setOf(HormoneRoute.PATCH), "E2 贴片起始 100 μg/天；螺内酯起始 50 mg/天，每月按化验调量。文中平均剂量分别为 170 μg/天、81 mg/天。",
            "ng/dL", listOf(TestosteroneReferencePoint(0, 498.6), TestosteroneReferencePoint(1, 62.7), TestosteroneReferencePoint(6, 29.4))),
        TestosteroneReference("treat-sl-once-2024", "舌下 E2 每日一次＋螺内酯 · TREAT", routeTitle,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC11220669/", "表3", 13, AntiandrogenDrug.SPIRONOLACTONE,
            setOf(HormoneRoute.SUBLINGUAL), "游离 E2 舌下起始 2 mg/天、每日一次；螺内酯起始 50 mg/天，每月按化验调量。文中平均剂量分别为 6.2 mg/天、129 mg/天。",
            "ng/dL", listOf(TestosteroneReferencePoint(0, 481.3), TestosteroneReferencePoint(1, 278.5), TestosteroneReferencePoint(6, 108.1))),
        TestosteroneReference("treat-sl-twice-2024", "舌下 E2 每日两次＋螺内酯 · TREAT", routeTitle,
            "https://pmc.ncbi.nlm.nih.gov/articles/PMC11220669/", "表3", 14, AntiandrogenDrug.SPIRONOLACTONE,
            setOf(HormoneRoute.SUBLINGUAL), "游离 E2 舌下起始总量 2 mg/天、分两次；螺内酯起始 50 mg/天，每月按化验调量。文中平均剂量分别为 6.2 mg/天、115 mg/天。",
            "ng/dL", listOf(TestosteroneReferencePoint(0, 505.3), TestosteroneReferencePoint(1, 313.8), TestosteroneReferencePoint(6, 121.7))),
    )

    fun halfLifeHours(drug: AntiandrogenDrug): Double = when (drug) {
        AntiandrogenDrug.CPA -> 43.9
        AntiandrogenDrug.SPIRONOLACTONE -> 16.5
    }

    fun estrogenHistory(data: HormoneJournalData, consumer: String?, nowMillis: Long): List<TestosteroneEstrogenContext> {
        val links = data.links.associateBy { it.ingestionId }
        val substances = data.substances.associateBy { it.id }
        return data.records.filter { it.consumerName == consumer && it.time.toEpochMilli() <= nowMillis }.mapNotNull { record ->
            val identity = substances[links[record.id]?.medicationId]?.identity ?: return@mapNotNull null
            if (identity.ingredientId != "ingredient:estradiol") return@mapNotNull null
            val freeE2 = identity.chemicalFormId == "chemical:estradiol"
            val formulation = identity.formulationId ?: MedicationProducts.eventSnapshot(record.id, data.settings)?.product?.formulation
            val route = when (record.administrationRoute.name) {
                "ORAL" -> if (formulation == "form:oral-solid" && (freeE2 || identity.chemicalFormId == "chemical:estradiol-valerate")) HormoneRoute.ORAL else null
                "SUBLINGUAL" -> if (formulation == "form:oral-solid" && freeE2) HormoneRoute.SUBLINGUAL else null
                "TRANSDERMAL" -> if (freeE2) when (formulation) {
                    "form:gel" -> HormoneRoute.GEL
                    "form:patch" -> HormoneRoute.PATCH
                    else -> null
                } else null
                else -> null
            } ?: return@mapNotNull null
            TestosteroneEstrogenContext(record.id.toString(), route, record.time.toEpochMilli() / 3600000.0)
        }
    }

    fun residuals(doses: List<AntiandrogenDose>, now: Double): List<AntiandrogenResidual> =
        AntiandrogenDrug.entries.mapNotNull { drug ->
            val past = doses.filter { it.drug == drug && it.timeHours <= now }
            if (past.isEmpty()) return@mapNotNull null
            val halfLife = halfLifeHours(drug)
            val remaining = past.sumOf { it.doseMg * exp(-ln(2.0) * (now - it.timeHours) / halfLife) }
            AntiandrogenResidual(drug, past.size, past.sumOf { it.doseMg }, remaining, halfLife)
        }

    fun calculate(
        events: List<TestosteroneEstrogenContext>,
        doses: List<AntiandrogenDose>,
        from: Double,
        now: Double,
        through: Double,
        checkActive: () -> Unit = {},
    ): TestosteroneReferenceResult {
        val past = events.filter { it.timeHours <= now }.distinctBy { it.id }
        val residuals = residuals(doses, now)
        val notes = mutableListOf(
            "参考线为两项 HRT 随机试验的组均值，研究对象为开始女性化治疗的成年人；不代表个人预测、受体阻断强度或调量建议。",
            "按最早可用 E2 和对应抗雄记录中较晚的时间对齐。记录起点不一定是实际治疗起点；参考情景假设持续采用研究方案，不能反映个人漏服、停药或换药效果。",
            "随访点之间仅用直线连接；以日历月对齐，六个月之外不外推。未按个人剂量、E2 浓度或总 T 化验缩放；化验点独立显示。",
            "历史 CPA／螺内酯用于选择各自研究对照及对齐起点，两药不合并为一个抑制比例。全部实际记录参与残留代理计算；未补记未来剂量。",
            "CPA 43.9 小时来自 Androcur 5.2；螺内酯采用活性代谢物 canrenone 的 16.5 小时，来自 DailyMed 12.3。瞬时吸收、一阶衰减和代谢物代理是近似，残留 mg 不是血药浓度或总 T 抑制量。",
        )
        val scenarios = studies.mapNotNull { study ->
            checkActive()
            val firstE2 = past.filter { it.route in study.routes }.minOfOrNull { it.timeHours }
                ?: return@mapNotNull null
            val firstDrug = doses.filter { it.drug == study.drug && it.timeHours <= now }.minOfOrNull { it.timeHours }
                ?: return@mapNotNull null
            val start = max(firstE2, firstDrug)
            val anchors = study.points.map { point ->
                val time = java.time.Instant.ofEpochMilli((start * 3600000.0).toLong())
                    .atZone(java.time.ZoneOffset.UTC).plusMonths(point.month.toLong()).toInstant().toEpochMilli() / 3600000.0
                TestosteroneSample(time, study.ngDl(point))
            }
            fun samples(left: Double, right: Double): List<TestosteroneSample> {
                val begin = max(left, anchors.first().timeHours)
                val end = min(right, anchors.last().timeHours)
                if (begin >= end) return emptyList()
                val times = (listOf(begin, end) + anchors.map { it.timeHours }.filter { it in begin..end }).distinct().sorted()
                return times.map { time ->
                    val segment = anchors.zipWithNext().first { (first, second) -> time in first.timeHours..second.timeHours }
                    val (first, second) = segment
                    TestosteroneSample(time, first.ngDl + (second.ngDl - first.ngDl) *
                        ((time - first.timeHours) / (second.timeHours - first.timeHours)))
                }
            }
            val history = samples(from, now)
            val forecast = samples(now, through)
            if (history.isEmpty() && forecast.isEmpty()) return@mapNotNull null
            TestosteroneScenario(study.id, study.label, history, forecast, study.id, start)
        }
        if (scenarios.isEmpty()) notes += "当前范围没有可对齐的文献参考线：需有相应 E2 途径及 CPA／螺内酯历史，且处于研究的六个月范围。仍可查看实测总 T 和全部研究数据。注射、单药 E2、含服 EV 不直接套用这些研究。"
        return TestosteroneReferenceResult(scenarios, notes, residuals)
    }
}
