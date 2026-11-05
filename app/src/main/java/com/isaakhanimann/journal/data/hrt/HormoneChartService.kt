package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.data.room.experiences.entities.Ingestion
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.classes.Substance
import java.time.Instant

data class HormoneJournalData(
    val records: List<Ingestion>, val links: List<MedicationLink>, val details: List<HrtEventDetail>,
    val settings: List<HrtModelSetting>, val plans: List<HrtPlan>, val labs: List<HrtLab> = emptyList(), val substances: List<Substance> = emptyList(),
)
data class HormoneChartRequest(val consumer: String? = null, val historyDays: Int = 30, val forecastDays: Int = 3, val analyteId: String = "E2", val gelReference: String = "compare", val bridgeSublingualValerate: Boolean = true) {
    fun validate() { require(historyDays in 1..366 && forecastDays in 0..90 && analyteId in setOf("E2", "EE", "E4", "E1", "EQUILIN") && gelReference in setOf("compare", "estrogel", "divigel", "confirmed")) }
}
data class HormoneExclusion(val id: String, val name: String, val time: Long?, val reason: String, val planned: Boolean = false)
data class HormoneContextRecord(val id: Int, val name: String, val category: String, val dose: Double?, val unit: String?, val time: Long, val modelReason: String? = null, val inVisibleRange: Boolean = true, val route: String = "")
data class HormoneChartData(
    val request: HormoneChartRequest, val fromHours: Double, val nowHours: Double, val throughHours: Double,
    val history: List<HormoneSample>, val forecast: List<HormoneSample>, val measured: List<NormalizedLab>,
    val includedActual: List<String>, val includedPlanned: List<String>, val excluded: List<HormoneExclusion>,
    val profileIds: Set<String>, val assumptions: Set<String>,
    val recognizedHrt: List<HormoneContextRecord> = emptyList(),
    val gelScenarios: List<GelStudyScenario> = emptyList(), val doseRecords: List<HormoneContextRecord> = emptyList(),
    val extrapolatedRecordIds: Set<String> = emptySet(),
)

/** Read-only projection. No DAO or mutation-capable repository enters this service. */
class HormoneChartService(private val catalog: HormoneReferenceCatalog) {
    fun calculate(data: HormoneJournalData, request: HormoneChartRequest, nowMillis: Long, checkActive: () -> Unit = {}): HormoneChartData {
        request.validate()
        val now = nowMillis / 3600000.0
        val from = now - request.historyDays * 24
        val through = now + request.forecastDays * 24
        val actual = HormoneRecordNormalizer.normalize(data.records, data.links, data.details, data.settings, data.substances,
            catalog, request.consumer, now, through, analyteId = request.analyteId)
        val bridge = if (request.analyteId == "E2" && request.bridgeSublingualValerate) ValerateSublingualBridge.events(data, catalog, request.consumer, now) else emptyList()
        val bridgeIds = bridge.map { it.id }.toSet()
        val modeledActual = actual.events.filter { it.id !in bridgeIds } + bridge
        val excluded = mutableListOf<HormoneExclusion>()
        val records = data.records.associateBy { it.id.toString() }
        val medications = data.substances.associateBy { it.id }
        val links = data.links.associateBy { it.ingestionId }
        val recognizedHrt = data.records.filter { it.consumerName == request.consumer && it.time.toEpochMilli() <= nowMillis }.mapNotNull { record ->
            val medication = medications[links[record.id]?.medicationId] ?: return@mapNotNull null
            if ("hrt-related" !in medication.categories) return@mapNotNull null
            HormoneContextRecord(record.id, medication.name, medication.hrtCategoryLabel(), record.dose, record.units, record.time.toEpochMilli(), actual.excluded[record.id.toString()], record.time.toEpochMilli()/3600000.0 >= from, record.administrationRoute.name)
        }
        val scenarios = GelStudyScenarios.calculate(data, request, actual.excluded, from, now, through, catalog, checkActive, modeledActual)
        val scenarioIds = scenarios.flatMap { it.recordIds }.toSet()
        actual.excluded.forEach { (id, reason) ->
            val event = records.getValue(id)
            if (reason in setOf("non_e2_record", "other_analyte_record")) {
                return@forEach
            }
            if (event.time.toEpochMilli()/3600000.0 >= from && id !in scenarioIds && id !in bridgeIds) excluded += HormoneExclusion(id, event.substanceName, event.time.toEpochMilli(), reason)
        }
        val projected = mutableListOf<HormoneEvent>()
        val forecastLabels = linkedMapOf<String, Pair<String, Long>>()
        var forecastCount = 0
        val assumptions = actual.assumptions.toMutableSet()
        if (bridge.isNotEmpty()) assumptions += "含服EV采用已明确标注的桥接外推：酯质量乘272.38/356.50换算母体E2，借用舌下E2时间型。它不是经专属EV含服PK验证的浓度模型，不能作为调量依据；原记录未改为口服或游离E2。"
        if (scenarios.isNotEmpty()) assumptions += "凝胶mg直接按有效雌二醇质量计算。情景线仅比较文献给定的制剂/部位条件；未确认信息不会写入实际记录，不是个体置信区间，也不含无法标定的含服EV贡献。"
        if (request.forecastDays > 0) data.plans.filter { it.consumer == request.consumer && !it.paused }.forEach planLoop@ { plan ->
            checkActive()
            val rule = runCatching { PlanSchedule.read(plan, data.settings) }.getOrElse {
                excluded += HormoneExclusion("plan:${plan.id}", plan.name, null, "invalid_plan", true)
                return@planLoop
            }
            val route = runCatching { AdministrationRoute.valueOf(plan.route) }.getOrElse {
                excluded += HormoneExclusion("plan:${plan.id}", plan.name, null, "invalid_plan_route", true)
                return@planLoop
            }
            val remaining = 5000 - forecastCount
            if (remaining <= 0) {
                excluded += HormoneExclusion("plan:${plan.id}", plan.name, null, "forecast_event_limit", true)
                return@planLoop
            }
            val occurrences = PlanOccurrences.between(plan, rule, nowMillis + 1, (through*3600000).toLong(), limit = remaining + 1)
                .filter { time -> data.settings.none { it.id == PlanOccurrences.key(plan.id, time) } }
            if (occurrences.size > remaining) excluded += HormoneExclusion("plan:${plan.id}:limit", plan.name, null, "forecast_event_limit", true)
            val product = MedicationProducts.forPlan(rule, data.settings)
            forecastCount += occurrences.size.coerceAtMost(remaining)
            occurrences.take(remaining).forEach occurrenceLoop@ { occurrence ->
                val modelId = "plan:${plan.id}:$occurrence"
                forecastLabels[modelId] = plan.name to occurrence
                val synthetic = Ingestion(-1, plan.name, Instant.ofEpochMilli(occurrence), administrationRoute = route,
                    dose = plan.dose, isDoseAnEstimate = false, estimatedDoseStandardDeviation = null, units = plan.unit, experienceId = 0,
                    notes = null, stomachFullness = null, consumerName = plan.consumer, customUnitId = null)
                val snapshot = product?.let { listOf(EventProductSnapshot(it, nowMillis, rule.doseBasis).setting(-1)) }.orEmpty()
                val profile = product?.modelProfileId?.let { id -> catalog.models.singleOrNull { it.id == id } }
                if (profile?.route == HormoneRoute.PATCH && rule.patchWearHours == null) {
                    excluded += HormoneExclusion(modelId, plan.name, occurrence, "missing_planned_patch_wear", true)
                    return@occurrenceLoop
                }
                val removal = rule.patchWearHours?.let { duration ->
                    val end = occurrence + duration * 3600000.0
                    require(end.isFinite() && end < Long.MAX_VALUE) { "计划佩戴时长超出范围" }
                    end.toLong()
                }
                val details = listOf(HrtEventDetail(-1, site = rule.applicationSite, patchRemovedAt = removal, doseBasis = rule.doseBasis?.name))
                val normalized = HormoneRecordNormalizer.normalize(listOf(synthetic), listOf(MedicationLink(-1, plan.medicationId, "identified", plan.name)),
                    details, snapshot, data.substances, catalog, request.consumer, now, through, plannedRecords = true, analyteId = request.analyteId)
                val planBridge = if (request.analyteId == "E2" && request.bridgeSublingualValerate) ValerateSublingualBridge.events(HormoneJournalData(listOf(synthetic), listOf(MedicationLink(-1, plan.medicationId, "identified", plan.name)), details, snapshot, emptyList(), substances = data.substances), catalog, request.consumer, now, planned = true) else emptyList()
                projected += (if (planBridge.isNotEmpty()) planBridge else normalized.events).map { it.copy(id = modelId, planned = true) }
                assumptions += normalized.assumptions
                if (planBridge.isEmpty()) normalized.excluded.values.filter { it !in setOf("non_e2_record", "other_analyte_record") }.forEach { reason -> excluded += HormoneExclusion(modelId, plan.name, occurrence, reason, true) }
            }
        }
        if (projected.isNotEmpty()) assumptions += "未来虚线假设按所选计划执行；不表示已服药，也不扣减库存。"
        if (projected.isNotEmpty() && modeledActual.isEmpty()) assumptions += "暂无可纳入的历史用药，未来情景不包含未知的历史残留。"
        val parameters = catalog.models.map { it.parameters() }
        val fastOffsets = listOf(0.1, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0, 8.0, 12.0)
        val additionalTimes = if (request.analyteId in setOf("EE", "E4") || bridge.isNotEmpty()) (modeledActual + projected)
            .filter { it.timeHours in (from - 24)..through }.flatMap { event -> fastOffsets.map { event.timeHours + it } }.distinct().take(5000) else emptyList()
        val engine = HormoneSimulationEngine()
        val history = engine.simulate(modeledActual, parameters, from, now, now, points = 601, additionalTimes = additionalTimes, checkActive = checkActive)
        val forecast = if (request.forecastDays > 0) engine.simulate(modeledActual + projected, parameters, now, through, now,
            points = 301, includePlanned = true, additionalTimes = additionalTimes, checkActive = checkActive) else null
        (history.excluded + forecast?.excluded.orEmpty()).forEach { (id, reason) ->
            if (excluded.none { it.id == id }) {
                val original = records[id]
                val planned = forecastLabels[id]
                excluded += HormoneExclusion(id, original?.substanceName ?: planned?.first ?: id, original?.time?.toEpochMilli() ?: planned?.second, reason, planned != null)
            }
        }
        val estimated = data.records.count { it.id.toString() in history.included && it.isDoseAnEstimate }
        if (estimated > 0) assumptions += "$estimated 条用量为估计值；曲线使用记录的中心值，未生成置信区间。"
        val measured = data.labs.filter { it.consumer == request.consumer && LabTrends.analyteKey(it.analyte) == request.analyteId && it.time/3600000.0 in from..through }.mapNotNull { lab ->
            if (lab.time > nowMillis) {
                excluded += HormoneExclusion("lab:${lab.id}", lab.analyte, lab.time, "future_lab_record")
                return@mapNotNull null
            }
            val normalized = runCatching { LabTrends.normalize(lab) }
            val point = normalized.getOrNull()
            if (point == null || point.unit != "pg/mL") {
                excluded += HormoneExclusion("lab:${lab.id}", lab.analyte, lab.time, "unsupported_lab_unit")
                null
            } else point
        }.sortedBy { it.original.time }
        val admitted = history.included.toSet()
        val forecastAdmitted = forecast?.included.orEmpty().toSet()
        return HormoneChartData(request, from, now, through, history.samples, forecast?.samples.orEmpty(), measured,
            history.included, projected.filter { it.id in forecastAdmitted }.map { it.id }, excluded,
            ((modeledActual.filter { it.id in admitted } + projected.filter { it.id in forecastAdmitted }).mapNotNull { it.profileId } + scenarios.flatMap { it.profileIds }).toSet(), assumptions,
            recognizedHrt.map { record -> record.copy(modelReason = if (record.id.toString() in bridgeIds) "sublingual_valerate_bridge" else if (record.id.toString() in scenarioIds) "gel_study_scenario" else excluded.firstOrNull { it.id == record.id.toString() }?.reason ?: record.modelReason) }.sortedByDescending { it.time },
            scenarios, recognizedHrt.filter { record -> record.inVisibleRange && catalog.models.any { it.acceptsMedication(links[record.id]?.medicationId.orEmpty()) && it.analyteId == request.analyteId } }, bridgeIds)
    }
}
