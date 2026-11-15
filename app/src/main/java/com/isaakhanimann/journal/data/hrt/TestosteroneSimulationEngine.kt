package com.isaakhanimann.journal.data.hrt

import kotlin.math.ceil
import kotlin.math.expm1
import kotlin.math.max
import kotlin.math.min

data class TestosteroneSample(val timeHours: Double, val ngDl: Double)

data class TestosteroneScenario(
    val id: String,
    val label: String,
    val history: List<TestosteroneSample>,
    val forecast: List<TestosteroneSample>,
)

class TestosteroneSimulationEngine {
    fun simulate(
        exposure: List<HormoneSample>,
        options: TestosteroneOptions,
        initialEffectiveE2: Double,
        checkActive: () -> Unit = {},
        antiandrogens: List<AntiandrogenDose> = emptyList(),
    ): List<TestosteroneSample> {
        options.validate()
        require(initialEffectiveE2.isFinite() && initialEffectiveE2 >= 0)
        require(exposure.all { it.timeHours.isFinite() && it.pgMl.isFinite() && it.pgMl >= 0 })
        require(exposure.zipWithNext().all { (first, second) -> first.timeHours < second.timeHours })
        if (exposure.isEmpty()) return emptyList()
        val start = options.startMillis / 3600000.0
        require(exposure.first().timeHours <= start && exposure.any { it.timeHours == start })
        var effectiveE2 = initialEffectiveE2
        var testosterone = options.initialNgDl
        val drugExposure = AntiandrogenDrug.entries.mapNotNull { drug ->
            drug.options(options)?.let { config -> AntiandrogenExposure(antiandrogens.filter { it.drug == drug }, config) }
        }
        val timeScales = listOf(options.effectHours, options.responseHours) +
            listOfNotNull(options.cyproterone?.halfLifeHours, options.spironolactone?.halfLifeHours)
        val maximumStep = min(.25, timeScales.min() / 8)
        val boundaries = antiandrogens.filter { it.drug.options(options)?.maximumSuppression?.let { maximum -> maximum > 0 } == true }
            .map { it.timeHours }.filter { it > exposure.first().timeHours && it < exposure.last().timeHours }
            .distinct().sorted()
        var boundaryIndex = 0
        val result = mutableListOf(TestosteroneSample(start, testosterone))
        exposure.zipWithNext().forEach { (first, second) ->
            val cuts = mutableListOf(first.timeHours)
            while (boundaryIndex < boundaries.size && boundaries[boundaryIndex] < second.timeHours) {
                val boundary = boundaries[boundaryIndex++]
                if (boundary > first.timeHours) cuts += boundary
            }
            cuts += second.timeHours
            cuts.zipWithNext().forEach { (left, right) ->
                val steps = ceil((right - left) / maximumStep).toInt().coerceAtLeast(1)
                val step = (right - left) / steps
                fun inputAt(time: Double) = first.pgMl +
                    (second.pgMl - first.pgMl) * ((time - first.timeHours) / (second.timeHours - first.timeHours))
                repeat(steps) { index ->
                    checkActive()
                    val leftTime = left + index * step
                    val middleTime = left + (index + .5) * step
                    val rightTime = left + (index + 1.0) * step
                    val middleE2 = linearFeedback(effectiveE2, inputAt(leftTime), inputAt(middleTime),
                        step / 2, options.effectHours)
                    val suppressedFraction = if (middleE2 <= options.ec50PgMl) {
                        val ratio = middleE2 / options.ec50PgMl
                        ratio / (1 + ratio)
                    } else {
                        1 / (1 + options.ec50PgMl / middleE2)
                    }
                    val remainingProduction = drugExposure.fold(1.0) { fraction, drug -> fraction * drug.remainingProduction(middleTime) }
                    val target = options.floorNgDl +
                        (options.unsuppressedNgDl - options.floorNgDl) * (1 - suppressedFraction) * remainingProduction
                    if (first.timeHours >= start) testosterone += (target - testosterone) * -expm1(-step / options.responseHours)
                    effectiveE2 = linearFeedback(effectiveE2, inputAt(leftTime), inputAt(rightTime),
                        step, options.effectHours)
                }
            }
            check(testosterone.isFinite() && effectiveE2.isFinite())
            if (second.timeHours > start) result += TestosteroneSample(second.timeHours, testosterone)
        }
        return result
    }

    internal fun linearFeedback(initial: Double, inputStart: Double, inputEnd: Double, hours: Double, timeConstant: Double): Double {
        val ratio = hours / timeConstant
        val gain = -expm1(-ratio)
        val rampWeight = if (ratio < 1e-4) ratio * (.5 + ratio * (-1.0 / 6 + ratio * (1.0 / 24 - ratio / 120)))
            else 1 - gain / ratio
        return initial + (inputStart - initial) * gain + (inputEnd - inputStart) * rampWeight
    }
}

object TestosteroneProjection {
    fun calculate(
        request: HormoneChartRequest,
        options: HormoneModelOptions,
        scenarios: List<GelStudyScenario>,
        actual: List<HormoneEvent>,
        planned: List<HormoneEvent>,
        parameters: List<HormoneParameters>,
        mainFit: LabAmplitudeFit,
        fits: Map<String, LabAmplitudeFit>,
        from: Double,
        now: Double,
        through: Double,
        checkActive: () -> Unit,
        antiandrogens: List<AntiandrogenDose> = emptyList(),
    ): List<TestosteroneScenario> {
        val config = options.testosterone ?: return emptyList()
        if (request.analyteId != "E2") return emptyList()
        val start = config.startMillis / 3600000.0
        if (start >= through || start > now) return emptyList()
        val inputs = if (scenarios.isEmpty()) listOf(Triple("history", "E2", actual))
            else scenarios.map { Triple(it.id, it.label, it.modelEvents) }
        return inputs.map { (id, label, events) ->
            checkActive()
            val fit = fits[id] ?: mainFit
            val engine = HormoneSimulationEngine()
            val earliest = min(start, events.minOfOrNull { it.timeHours } ?: start)
            val warmup = max(earliest, start - 8 * config.effectHours)
            val hours = ceil(through - warmup).toInt().coerceAtLeast(1)
            val grid = (0..hours).map { warmup + min(it.toDouble(), through - warmup) } +
                listOf(start, from, now) + options.calibrationTimes() +
                antiandrogens.map { it.timeHours }
            val exposure = engine.simulate(events + planned, parameters, warmup, through, now, points = 2,
                includePlanned = true, additionalTimes = grid, includeEventTimes = true, checkActive = checkActive).samples
            val samples = if (exposure.isEmpty()) grid.filter { it in warmup..through }.distinct().sorted()
                .map { HormoneSample(it, fit.baselinePgMl) }
                else options.calibrate(exposure, fit)
            val initialE2 = samples.first().pgMl
            val projected = TestosteroneSimulationEngine().simulate(samples, config, initialE2, checkActive, antiandrogens)
            TestosteroneScenario(id, label, projected.filter { it.timeHours in from..now },
                projected.filter { it.timeHours in now..through && through > now })
        }
    }
}
