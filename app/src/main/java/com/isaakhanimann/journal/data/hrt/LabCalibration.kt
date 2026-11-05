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

data class LabAmplitudeFit(val scale: Double = 1.0, val labIds: List<String> = emptyList())

/** One identifiable amplitude only; no inference of clearance, absorption, endogenous production or T suppression. */
object LabCalibration {
    fun fit(data: HormoneJournalData, consumer: String?, analyteId: String, events: List<HormoneEvent>, parameters: List<HormoneParameters>, nowMillis: Long): LabAmplitudeFit {
        if (analyteId != "E2" || events.isEmpty()) return LabAmplitudeFit()
        val eventIds = events.map { it.id }.toSet()
        val medicines = data.substances.associateBy { it.id }
        val links = data.links.associateBy { it.ingestionId }
        val doses = data.records.filter { it.consumerName == consumer && medicines[links[it.id]?.medicationId]?.identity?.ingredientId == "ingredient:estradiol" }
        val pairs = data.labs.mapNotNull { lab ->
            if (lab.consumer != consumer || lab.time > nowMillis || lab.comparison != "=" || LabTrends.analyteKey(lab.analyte) != analyteId) return@mapNotNull null
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
            Triple(lab.id, predicted, measured.value)
        }
        if (pairs.isEmpty()) return LabAmplitudeFit()
        val maxP = pairs.maxOf { it.second }
        val numerator = pairs.sumOf { (it.second/maxP)*it.third }
        val denominator = pairs.sumOf { (it.second/maxP)*(it.second/maxP) } * maxP
        val scale = numerator/denominator
        return if (scale.isFinite() && scale >= 0) LabAmplitudeFit(scale, pairs.map { it.first }) else LabAmplitudeFit()
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
