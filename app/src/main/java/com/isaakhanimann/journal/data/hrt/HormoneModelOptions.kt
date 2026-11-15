package com.isaakhanimann.journal.data.hrt

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class HormoneModelOptions(
    val baselineE2PgMl: Double = 0.0,
    val calibrationStartMillis: Long? = null,
) {
    fun validate() {
        require(baselineE2PgMl.isFinite() && baselineE2PgMl >= 0) { "E2 基线应为非负数" }
    }

    fun setting(consumer: String?) = HrtModelSetting(key(consumer), Json.encodeToString(this))

    fun calibrationTimes(): List<Double> = calibrationStartMillis?.let {
        val boundary = it / 3600000.0
        listOf(Math.nextDown(boundary), boundary)
    }.orEmpty()

    fun calibrate(samples: List<HormoneSample>, fit: LabAmplitudeFit): List<HormoneSample> {
        val boundary = calibrationStartMillis?.div(3600000.0)
        return samples.map {
            val scale = if (boundary != null && it.timeHours < boundary) 1.0 else fit.scale
            it.copy(pgMl = fit.baselinePgMl + it.pgMl * scale)
        }.also { require(it.all { sample -> sample.pgMl.isFinite() }) }
    }

    companion object {
        private val settingsJson = Json { ignoreUnknownKeys = true }
        private fun key(consumer: String?) = "hormone-model:${Json.encodeToString(consumer)}"

        fun read(settings: List<HrtModelSetting>, consumer: String?): HormoneModelOptions {
            val setting = settings.singleOrNull { it.id == key(consumer) } ?: return HormoneModelOptions()
            return settingsJson.decodeFromString<HormoneModelOptions>(setting.payload).also { it.validate() }
        }
    }
}
