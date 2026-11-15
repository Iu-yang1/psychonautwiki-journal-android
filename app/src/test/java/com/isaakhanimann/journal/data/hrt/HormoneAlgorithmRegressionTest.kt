package com.isaakhanimann.journal.data.hrt

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.ln

class HormoneAlgorithmRegressionTest {
    private val parameters = HormoneParameters("e2", HormoneRoute.SUBLINGUAL, "test", "analytic test",
        true, 1.8, .7246, 269.5)
    private val event = HormoneEvent("dose", "e2", HormoneRoute.SUBLINGUAL, 5000.13, 1.0)

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
    fun calibrationStepPreservesItsLeftLimitAndEarlierE2() {
        val boundaryMillis = (10.5 * 3600000).toLong()
        val model = HormoneModelOptions(calibrationStartMillis = boundaryMillis)
        val samples = model.calibrate(model.calibrationTimes().map { HormoneSample(it, 100.0) },
            LabAmplitudeFit(scale = 3.0))
        assertEquals(100.0, samples.first().pgMl, 0.0)
        assertEquals(300.0, samples.last().pgMl, 0.0)
        assertTrue(samples.first().timeHours < 10.5)
        assertEquals(10.5, samples.last().timeHours, 0.0)
    }

    @Test
    fun modelSettingsRemainSeparateForNullEmptyAndNamedConsumers() {
        val own = HormoneModelOptions(12.0, 100)
        val empty = HormoneModelOptions(20.0)
        val other = HormoneModelOptions(30.0)
        val settings = listOf(own.setting(null), empty.setting(""), other.setting("person"))
        assertEquals(own, HormoneModelOptions.read(settings, null))
        assertEquals(empty, HormoneModelOptions.read(settings, ""))
        assertEquals(other, HormoneModelOptions.read(settings, "person"))
    }

    @Test
    fun legacyTestosteroneSettingsCannotBreakOrChangeE2Settings() {
        val expected = HormoneModelOptions(12.0, 12345L)
        val payload = """{"baselineE2PgMl":12.0,"calibrationStartMillis":12345,"testosterone":{"startMillis":0,"initialNgDl":500.0,"unsuppressedNgDl":500.0,"floorNgDl":20.0,"ec50PgMl":100.0,"effectHours":24.0,"responseHours":168.0,"cyproterone":{"halfLifeHours":43.9,"halfSuppressionMgDay":10.0,"maximumSuppression":0.9}},"showTestosteroneReference":true}"""
        val setting = expected.setting("person").copy(payload = payload)
        val settings = listOf(setting)
        val loaded = HormoneModelOptions.read(settings, "person")
        assertEquals(expected, loaded)
        assertEquals(payload, settings.single().payload)
        assertFalse(loaded.setting("person").payload.contains("testosterone", ignoreCase = true))
        assertEquals(expected, HormoneModelOptions.read(listOf(loaded.setting("person")), "person"))
        val oldDefault = HormoneModelOptions().setting(null).copy(
            payload = """{"testosterone":null,"showTestosteroneReference":false}""")
        assertEquals(HormoneModelOptions(), HormoneModelOptions.read(listOf(oldDefault), null))
    }

    @Test
    fun invalidE2BaselineIsStillRejectedInLegacySettings() {
        val setting = HormoneModelOptions().setting(null).copy(
            payload = """{"baselineE2PgMl":-1.0,"testosterone":{},"showTestosteroneReference":true}""")
        assertThrows(IllegalArgumentException::class.java) { HormoneModelOptions.read(listOf(setting), null) }
        listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { baseline ->
            assertThrows(IllegalArgumentException::class.java) { HormoneModelOptions(baseline).validate() }
        }
    }
}
