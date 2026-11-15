package com.isaakhanimann.journal.data.hrt

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.exp
import kotlin.math.ln

class HormoneAlgorithmRegressionTest {
    private val parameters = HormoneParameters("e2", HormoneRoute.SUBLINGUAL, "test", "analytic test",
        true, 1.8, .7246, 269.5)
    private val event = HormoneEvent("dose", "e2", HormoneRoute.SUBLINGUAL, 5000.13, 1.0)
    private val options = TestosteroneOptions(0, 500.0, 500.0, 20.0, 100.0, 24.0, 168.0)

    @Test
    fun yearLongChartSamplesActualSublingualPeak() {
        val result = HormoneSimulationEngine().simulate(listOf(event), listOf(parameters), 0.0, 8784.0,
            8784.0, points = 601, includeEventTimes = true)
        val peakOffset = ln(parameters.absorptionPerHour / parameters.eliminationPerHour) /
            (parameters.absorptionPerHour - parameters.eliminationPerHour)
        val peak = result.samples.maxBy { it.pgMl }
        assertEquals(event.timeHours + peakOffset, peak.timeHours, 1e-5)
        assertTrue(peak.pgMl > 140)
        assertTrue(result.samples.any { it.timeHours == event.timeHours })
    }

    @Test
    fun patchRemovalAndLagAreSampledAtTheirActualTimes() {
        val patch = parameters.copy(route = HormoneRoute.PATCH, kernel = HormoneKernel.PATCH_ZERO_ORDER, lagHours = 2.0)
        val dose = event.copy(route = HormoneRoute.PATCH, timeHours = 10.0, endHours = 90.0,
            doseMg = null, releaseMgHour = .002)
        val result = HormoneSimulationEngine().simulate(listOf(dose), listOf(patch), 0.0, 100.0,
            100.0, points = 2, includeEventTimes = true)
        assertTrue(result.samples.any { it.timeHours == 12.0 })
        assertTrue(result.samples.any { it.timeHours == 92.0 })
    }

    @Test
    fun constantE2FollowsAnalyticTestosteroneResponse() {
        val times = (0..240).map { HormoneSample(it.toDouble(), 100.0) }
        val result = TestosteroneSimulationEngine().simulate(times, options, 100.0)
        val target = 20.0 + (500.0 - 20.0) / 2
        assertEquals(500.0, result.first().ngDl, 0.0)
        assertEquals(target + (500.0 - target) * exp(-240.0 / 168), result.last().ngDl, 1e-9)
        assertTrue(result.zipWithNext().all { (first, second) -> first.ngDl >= second.ngDl })
    }

    @Test
    fun briefPeakDoesNotInstantlyCollapseTestosterone() {
        val exposure = listOf(HormoneSample(0.0, 0.0), HormoneSample(.5, 500.0),
            HormoneSample(1.0, 0.0), HormoneSample(24.0, 0.0))
        val result = TestosteroneSimulationEngine().simulate(exposure, options, 0.0)
        assertTrue(result[1].ngDl > 499)
        assertTrue(result.all { it.ngDl in 20.0..500.0 })
    }

    @Test
    fun zeroE2RecoversTowardTheConfiguredUnsuppressedLevel() {
        val result = TestosteroneSimulationEngine().simulate(
            listOf(HormoneSample(0.0, 0.0), HormoneSample(168.0, 0.0)),
            options.copy(initialNgDl = 20.0), 0.0)
        assertEquals(500 + (20 - 500) * exp(-1.0), result.last().ngDl, 1e-9)
    }

    @Test
    fun feedbackWarmupRetainsTheStartingTestosteroneAnchor() {
        val exposure = (-192..24).map { HormoneSample(it.toDouble(), 100.0) }
        val result = TestosteroneSimulationEngine().simulate(exposure, options, 0.0)
        assertEquals(0.0, result.first().timeHours, 0.0)
        assertEquals(500.0, result.first().ngDl, 0.0)
        assertTrue(result.last().ngDl < 500)
        assertTrue(result.all { it.timeHours >= 0 })
    }

    @Test
    fun linearFeedbackMatchesRampSolutionAndRemainsStableForTinyIntervals() {
        val engine = TestosteroneSimulationEngine()
        val initial = 30.0
        val inputStart = 50.0
        val slope = 8.0
        val hours = 12.0
        val timeConstant = 3.0
        val expected = inputStart + slope * (hours - timeConstant) +
            (initial - inputStart + slope * timeConstant) * exp(-hours / timeConstant)
        assertEquals(expected, engine.linearFeedback(initial, inputStart, inputStart + slope * hours,
            hours, timeConstant), 1e-12)
        assertEquals(0.0, engine.linearFeedback(0.0, 0.0, 100.0, 0.0, 24.0), 0.0)
        val tiny = engine.linearFeedback(0.0, 0.0, 100.0, 1e-10, 24.0)
        assertEquals(100.0 * 1e-10 / 48, tiny, 1e-20)
    }

    @Test
    fun irregularDrugTimeIsIntegratedExactlyAsAnExplicitExposureBoundary() {
        val drug = AntiandrogenDose("irregular", AntiandrogenDrug.CPA, .13, 50.0)
        val config = options.copy(responseHours = 2.0, cyproterone = AntiandrogenOptions(48.0, 10.0, .8))
        val engine = TestosteroneSimulationEngine()
        val coarse = engine.simulate(listOf(HormoneSample(0.0, 0.0), HormoneSample(1.0, 0.0)),
            config, 0.0, antiandrogens = listOf(drug))
        val split = engine.simulate(listOf(HormoneSample(0.0, 0.0), HormoneSample(.13, 0.0), HormoneSample(1.0, 0.0)),
            config, 0.0, antiandrogens = listOf(drug))
        assertEquals(coarse.last(), split.last())
        assertEquals(500.0, split[1].ngDl, 0.0)
        assertTrue(split.last().ngDl < 500)
    }

    @Test
    fun changingE2AgreesWithIndependentFineStepRungeKuttaSolution() {
        val config = options.copy(effectHours = .5, responseHours = 2.0)
        val result = TestosteroneSimulationEngine().simulate(
            listOf(HormoneSample(0.0, 0.0), HormoneSample(4.0, 200.0)), config, 0.0)
        var reference = 500.0
        val step = 4.0 / 16000
        fun derivative(time: Double, value: Double): Double {
            val effective = 50 * (time - .5 * (1 - exp(-time / .5)))
            val target = 20 + 480 / (1 + effective / 100)
            return (target - value) / 2
        }
        repeat(16000) { index ->
            val time = index * step
            val first = derivative(time, reference)
            val second = derivative(time + step / 2, reference + step * first / 2)
            val third = derivative(time + step / 2, reference + step * second / 2)
            val fourth = derivative(time + step, reference + step * third)
            reference += step * (first + 2 * second + 2 * third + fourth) / 6
        }
        assertEquals(reference, result.last().ngDl, .02)
    }

    @Test
    fun calibrationStepPreservesItsLeftLimitAndCannotAffectEarlierTestosterone() {
        val boundaryMillis = (10.5 * 3600000).toLong()
        val model = HormoneModelOptions(calibrationStartMillis = boundaryMillis, testosterone = options)
        val times = model.calibrationTimes()
        val samples = model.calibrate(times.map { HormoneSample(it, 100.0) }, LabAmplitudeFit(scale = 3.0))
        assertEquals(100.0, samples.first().pgMl, 0.0)
        assertEquals(300.0, samples.last().pgMl, 0.0)
        val events = listOf(event.copy(timeHours = 0.0))
        fun project(scale: Double) = TestosteroneProjection.calculate(HormoneChartRequest(), model, emptyList(),
            events, emptyList(), listOf(parameters), LabAmplitudeFit(scale = scale), emptyMap(),
            0.0, 12.0, 24.0, {}).single()
        val reference = project(1.0)
        val calibrated = project(3.0)
        assertEquals(reference.history.filter { it.timeHours < 10.5 }, calibrated.history.filter { it.timeHours < 10.5 })
        assertTrue(calibrated.forecast.last().ngDl < reference.forecast.last().ngDl)
    }

    @Test
    fun modelSettingsRemainSeparateForNullEmptyAndNamedConsumers() {
        val own = HormoneModelOptions(12.0, 100, options)
        val empty = HormoneModelOptions(20.0)
        val other = HormoneModelOptions(30.0)
        val settings = listOf(own.setting(null), empty.setting(""), other.setting("person"))
        assertEquals(own, HormoneModelOptions.read(settings, null))
        assertEquals(empty, HormoneModelOptions.read(settings, ""))
        assertEquals(other, HormoneModelOptions.read(settings, "person"))
    }

    @Test
    fun invalidTestosteroneValuesAreRejectedBeforeSimulation() {
        listOf(options.copy(ec50PgMl = 0.0), options.copy(responseHours = Double.NaN),
            options.copy(floorNgDl = 600.0)).forEach { config ->
            assertThrows(IllegalArgumentException::class.java) { config.validate() }
        }
    }

    @Test
    fun oldAntiandrogenDoseRetainsAnAnalyticTailAndDoesNotChangeTheAnchor() {
        val drug = AntiandrogenOptions(48.0, 10.0, .8)
        val dose = AntiandrogenDose("old", AntiandrogenDrug.CPA, -96.0, 40.0)
        val proxy = AntiandrogenExposure(listOf(dose), drug)
        val equivalentAtStart = 10.0 * ln(2.0) / 48 * 24
        assertEquals(1 - .8 * equivalentAtStart / (10 + equivalentAtStart), proxy.remainingProduction(0.0), 1e-12)
        assertTrue(proxy.remainingProduction(96.0) > 1 - .8 * equivalentAtStart / (10 + equivalentAtStart))
        val exposure = (0..240).map { HormoneSample(it.toDouble(), 0.0) }
        val result = TestosteroneSimulationEngine().simulate(exposure, options.copy(cyproterone = drug), 0.0,
            antiandrogens = listOf(dose))
        assertEquals(options.initialNgDl, result.first().ngDl, 0.0)
        assertTrue(result.last().ngDl < options.initialNgDl)
    }

    @Test
    fun cpaAndSpironolactoneUseIndependentEffectsAndFutureDosesCannotChangeThePast() {
        val exposure = (0..120).map { HormoneSample(it.toDouble(), 0.0) }
        val cpa = AntiandrogenDose("cpa", AntiandrogenDrug.CPA, 0.0, 50.0)
        val spiro = AntiandrogenDose("spiro", AntiandrogenDrug.SPIRONOLACTONE, 0.0, 100.0)
        val config = options.copy(cyproterone = AntiandrogenOptions(43.9, 10.0, .9),
            spironolactone = AntiandrogenOptions(16.5, 100.0, 0.0))
        val engine = TestosteroneSimulationEngine()
        val without = engine.simulate(exposure, config, 0.0)
        assertEquals(without, engine.simulate(exposure, config, 0.0, antiandrogens = listOf(spiro)))
        val spiroEffect = engine.simulate(exposure,
            config.copy(spironolactone = config.spironolactone!!.copy(maximumSuppression = .4)), 0.0,
            antiandrogens = listOf(spiro))
        assertTrue(spiroEffect.last().ngDl < without.last().ngDl)
        val combined = engine.simulate(exposure, config, 0.0, antiandrogens = listOf(cpa, spiro))
        assertTrue(combined.last().ngDl < without.last().ngDl)
        assertEquals(combined, engine.simulate(exposure, config, 0.0,
            antiandrogens = listOf(cpa, spiro, cpa.copy(id = "future", timeHours = 150.0))))
        assertTrue(combined.all { it.ngDl in options.floorNgDl..options.unsuppressedNgDl })
    }

    @Test
    fun repeatingAntiandrogenDosesAccumulateAndWashOutAfterStopping() {
        val config = AntiandrogenOptions(48.0, 10.0, .8)
        val doses = (0..5).map { AntiandrogenDose("$it", AntiandrogenDrug.CPA, it * 24.0, 10.0) }
        val proxy = AntiandrogenExposure(doses, config)
        val first = proxy.remainingProduction(0.0)
        val accumulated = proxy.remainingProduction(120.0)
        assertTrue(accumulated < first)
        assertTrue(proxy.remainingProduction(600.0) > first)
    }

    @Test
    fun drugParametersRejectInvalidTimeScalesAndSuppressionFractions() {
        listOf(AntiandrogenOptions(0.0, 10.0, .8), AntiandrogenOptions(48.0, -1.0, .8),
            AntiandrogenOptions(48.0, 10.0, 1.1), AntiandrogenOptions(48.0, 10.0, Double.NaN)).forEach {
            assertThrows(IllegalArgumentException::class.java) { it.validate() }
        }
        val config = options.copy(cyproterone = AntiandrogenOptions(43.9, 10.0, .9),
            spironolactone = AntiandrogenOptions(16.5, 100.0, 0.0))
        val setting = HormoneModelOptions(testosterone = config).setting("person")
        assertEquals(config, HormoneModelOptions.read(listOf(setting), "person").testosterone)
    }
}
