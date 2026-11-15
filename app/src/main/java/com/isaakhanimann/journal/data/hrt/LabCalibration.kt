package com.isaakhanimann.journal.data.hrt

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs

@Serializable
data class LabDoseContext(val labId: String, val elapsedHours: Double?, val lastDoseId: Int?, val lastDoseTime: Long?, val calibrate: Boolean) {
    fun setting() = HrtModelSetting("lab-dose:$labId", Json.encodeToString(this))
    companion object {
        fun read(id: String, settings: List<HrtModelSetting>): LabDoseContext? = settings.singleOrNull { it.id == "lab-dose:$id" }?.let { runCatching { Json.decodeFromString<LabDoseContext>(it.payload) }.getOrNull() }
    }
}

data class LabAmplitudeFit(
    val scale: Double = 1.0,
    val labIds: List<String> = emptyList(),
    val baselinePgMl: Double = 0.0,
    val meanAbsoluteErrorPgMl: Double? = null,
    val forwardValidationErrorPgMl: Double? = null,
)

/** One identifiable amplitude only; no inference of clearance, absorption, endogenous production or T suppression. */
object LabCalibration {
    fun fit(data: HormoneJournalData, consumer: String?, analyteId: String, events: List<HormoneEvent>, parameters: List<HormoneParameters>, nowMillis: Long): LabAmplitudeFit {
        if (analyteId != "E2") return LabAmplitudeFit()
        val options = HormoneModelOptions.read(data.settings, consumer)
        val baseline = options.baselineE2PgMl
        if (events.isEmpty()) return LabAmplitudeFit(baselinePgMl = baseline)
        val eventIds = events.map { it.id }.toSet()
        val medicines = data.substances.associateBy { it.id }
        val links = data.links.associateBy { it.ingestionId }
        val doses = data.records.filter { it.consumerName == consumer && medicines[links[it.id]?.medicationId]?.identity?.ingredientId == "ingredient:estradiol" }
        val pairs = data.labs.mapNotNull { lab ->
            if (lab.consumer != consumer || lab.time > nowMillis || lab.comparison != "=" || LabTrends.analyteKey(lab.analyte) != analyteId) return@mapNotNull null
            if (options.calibrationStartMillis?.let { lab.time < it } == true) return@mapNotNull null
            val context = LabDoseContext.read(lab.id, data.settings) ?: return@mapNotNull null
            val elapsed = context.elapsedHours ?: return@mapNotNull null
            if (!context.calibrate || !elapsed.isFinite() || elapsed < 0) return@mapNotNull null
            val last = doses.filter { it.time.toEpochMilli() <= lab.time }.maxByOrNull { it.time } ?: return@mapNotNull null
            if (last.id != context.lastDoseId || last.time.toEpochMilli() != context.lastDoseTime || last.id.toString() !in eventIds) return@mapNotNull null
            if (abs(lab.time - last.time.toEpochMilli() - elapsed * 3600000) > 60000) return@mapNotNull null
            val measured = runCatching { LabTrends.normalize(lab) }.getOrNull() ?: return@mapNotNull null
            if (measured.unit != "pg/mL") return@mapNotNull null
            val t = lab.time/3600000.0
            val predicted = HormoneSimulationEngine().simulate(events.filter { !it.planned && it.timeHours <= t }, parameters, t, t + .000001, t, 2).samples.firstOrNull()?.pgMl ?: return@mapNotNull null
            if (!predicted.isFinite() || predicted <= 1e-9) return@mapNotNull null
            Triple(lab, predicted, measured.value)
        }.sortedBy { it.first.time }
        if (pairs.isEmpty()) return LabAmplitudeFit(baselinePgMl = baseline)
        fun fitScale(points: List<Triple<HrtLab, Double, Double>>): Double {
            val maximum = points.maxOf { it.second }
            val numerator = points.sumOf { (it.second / maximum) * (it.third - baseline) }
            val denominator = points.sumOf { (it.second / maximum) * (it.second / maximum) } * maximum
            return (numerator / denominator).coerceAtLeast(0.0)
        }
        val scale = fitScale(pairs)
        if (!scale.isFinite()) return LabAmplitudeFit(baselinePgMl = baseline)
        val error = pairs.map { abs(baseline + scale * it.second - it.third) }.average()
        val validationErrors = pairs.mapNotNull { point ->
            val earlier = pairs.filter { it.first.time < point.first.time }
            if (earlier.isEmpty()) null else abs(baseline + fitScale(earlier) * point.second - point.third)
        }
        return LabAmplitudeFit(scale, pairs.map { it.first.id }, baseline, error,
            validationErrors.takeIf { it.isNotEmpty() }?.average())
    }
}

fun labUnitPresets(analyte: String): List<String> = when (LabTrends.analyteKey(analyte)) {
    "E2" -> listOf("pg/mL", "pmol/L", "ng/L")
    "总睾酮" -> listOf("ng/dL", "nmol/L", "ng/mL")
    "孕酮" -> listOf("ng/mL", "nmol/L")
    "泌乳素" -> listOf("ng/mL", "mIU/L", "μg/L")
    "LH", "FSH" -> listOf("mIU/mL", "IU/L")
    "SHBG" -> listOf("nmol/L")
    else -> listOf("pg/mL", "ng/mL", "nmol/L")
}
