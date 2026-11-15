package com.isaakhanimann.journal.data.hrt

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class HormoneModelOptions(
    val baselineE2PgMl: Double = 0.0,
    val calibrationStartMillis: Long? = null,
    val testosterone: TestosteroneOptions? = null,
) {
    fun validate() {
        require(baselineE2PgMl.isFinite() && baselineE2PgMl >= 0) { "E2 基线应为非负数" }
        testosterone?.validate()
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
        private fun key(consumer: String?) = "hormone-model:${Json.encodeToString(consumer)}"

        fun read(settings: List<HrtModelSetting>, consumer: String?): HormoneModelOptions {
            val setting = settings.singleOrNull { it.id == key(consumer) } ?: return HormoneModelOptions()
            return Json.decodeFromString<HormoneModelOptions>(setting.payload).also { it.validate() }
        }
    }
}

@Serializable
data class TestosteroneOptions(
    val startMillis: Long,
    val initialNgDl: Double,
    val unsuppressedNgDl: Double,
    val floorNgDl: Double,
    val ec50PgMl: Double,
    val effectHours: Double,
    val responseHours: Double,
    val cyproterone: AntiandrogenOptions? = null,
    val spironolactone: AntiandrogenOptions? = null,
) {
    fun validate() {
        require(listOf(initialNgDl, unsuppressedNgDl, floorNgDl).all { it.isFinite() && it >= 0 }) { "睾酮浓度应为非负数" }
        require(floorNgDl <= unsuppressedNgDl) { "抑制下限应小于或等于未抑制水平" }
        require(listOf(ec50PgMl, effectHours, responseHours).all { it.isFinite() && it > 0 }) { "EC50 和响应时间应大于零" }
        cyproterone?.validate()
        spironolactone?.validate()
    }
}

@Serializable
data class AntiandrogenOptions(
    val halfLifeHours: Double,
    val halfSuppressionMgDay: Double,
    val maximumSuppression: Double,
) {
    fun validate() {
        require(halfLifeHours.isFinite() && halfLifeHours > 0 &&
            halfSuppressionMgDay.isFinite() && halfSuppressionMgDay > 0) { "抗雄半衰期和半最大抑制暴露应大于零" }
        require(maximumSuppression.isFinite() && maximumSuppression in 0.0..1.0) { "抗雄总 T 最大抑制比例应在 0 到 1 之间" }
    }
}
