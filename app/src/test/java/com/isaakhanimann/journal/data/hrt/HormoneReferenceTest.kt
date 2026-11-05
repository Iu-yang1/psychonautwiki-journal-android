package com.isaakhanimann.journal.data.hrt

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

@Serializable private data class ReferenceFixture(val profileId: String, val driver: Double, val timesHours: List<Double>, val pgMl: List<Double>, val removedAtHours: Double? = null)
@Serializable private data class ReferenceFixtures(val version: String, val fixtures: List<ReferenceFixture>)

class HormoneReferenceTest {
    private fun resource(name: String) = requireNotNull(javaClass.classLoader!!.getResourceAsStream(name)).bufferedReader().use { it.readText() }
    private val catalog get() = HormoneReferenceCatalog.parse(resource("hrt_reference_models.json"))

    @Test fun allFiveFamiliesHaveExplicitSourcesAndParameters() {
        val references = catalog
        assertEquals(HormoneRoute.entries.toSet(), references.models.map { it.route }.toSet())
        assertEquals(16, references.models.size)
        references.models.forEach { assertTrue(it.sources.all { source -> references.sources.getValue(source).let { info -> info.url != null || info.urls.isNotEmpty() } }) }
    }

    @Test fun kotlinResponsesMatchIndependentScipyMatrixExponentialFixtures() {
        val references = catalog
        val original = Json.decodeFromString<ReferenceFixtures>(resource("numerical_fixtures.json"))
        val expanded = Json.decodeFromString<ReferenceFixtures>(resource("expansion_numerical_fixtures.json"))
        val fixtures = ReferenceFixtures(expanded.version, original.fixtures + expanded.fixtures)
        assertEquals(references.models.map { it.id }.toSet(), fixtures.fixtures.map { it.profileId }.toSet())
        assertEquals(references.version, fixtures.version)
        fixtures.fixtures.forEach { fixture ->
            val profile = references.models.single { it.id == fixture.profileId }
            val event = HormoneEvent("event", profile.medicationId, profile.route, 0.0,
                if (profile.route == HormoneRoute.PATCH) null else fixture.driver,
                fixture.removedAtHours, if (profile.route == HormoneRoute.PATCH) fixture.driver else null, profile.id)
            fixture.timesHours.zip(fixture.pgMl).forEach { (time, expected) ->
                val actual = HormoneSimulationEngine().simulate(listOf(event), listOf(profile.parameters()), time, time + 0.001, 1000.0, 2).samples.first().pgMl
                assertEquals("${profile.id} at $time", expected, actual, max(1e-7, abs(expected) * 1e-8))
            }
        }
    }

    @Test fun equalAndNearlyEqualThreeStageRatesStayFinite() {
        val p = HormoneParameters("m", HormoneRoute.INJECTION, "test", "analytic limit", true, .5, .5, 1.0, kernel = HormoneKernel.THREE_STAGE, transferPerHour = .5)
        val event = HormoneEvent("e", "m", HormoneRoute.INJECTION, 0.0, 1.0)
        val sample = HormoneSimulationEngine().simulate(listOf(event), listOf(p), 1.0, 2.0, 2.0, 2).samples.first()
        assertEquals(.5*.5*exp(-.5)/2, sample.pgMl, 1e-12)
        val near = HormoneSimulationEngine().simulate(listOf(event), listOf(p.copy(transferPerHour = .500000001)), 1.0, 2.0, 2.0, 2).samples.first()
        assertEquals(sample.pgMl, near.pgMl, 1e-9)
    }

    @Test fun tinyThreeStageTimesDoNotProduceNegativeCancellationArtifacts() {
        val p = catalog.models.first { it.kernel == HormoneKernel.THREE_STAGE }.parameters()
        val event = HormoneEvent("e", p.medicationId, p.route, 0.0, 1.0)
        val sample = HormoneSimulationEngine().simulate(listOf(event), listOf(p), 1e-9, 1e-8, 1.0, 2).samples.first()
        val leading = p.absorptionPerHour * p.transferPerHour!! * 1e-18 * p.responseScale / 2
        assertEquals(leading, sample.pgMl, 1e-26)
        assertTrue(sample.pgMl > 0)
    }

    @Test fun patchRemovalIsContinuousAndStopsReservoirInput() {
        val profile = catalog.models.single { it.route == HormoneRoute.PATCH }
        val event = HormoneEvent("p", profile.medicationId, HormoneRoute.PATCH, 0.0, null, 80.0, 50.0/24000)
        val engine = HormoneSimulationEngine()
        val samples = engine.simulate(listOf(event), listOf(profile.parameters()), 79.999999, 80.000001, 100.0, 3).samples
        assertEquals(samples[0].pgMl, samples[2].pgMl, 1e-4)
        val later = engine.simulate(listOf(event), listOf(profile.parameters()), 87.0, 88.0, 100.0, 2).samples.first()
        assertEquals(samples[1].pgMl / 2, later.pgMl, 1e-8)
    }

    @Test fun plannedForecastIsExplicitAndNeverAdmitsFutureActualRecord() {
        val p = catalog.models.first().parameters()
        val future = HormoneEvent("f", p.medicationId, p.route, 10.0, 1.0)
        assertTrue(HormoneSimulationEngine().simulate(listOf(future), listOf(p), 0.0, 24.0, 1.0, 3, includePlanned = true).included.isEmpty())
        val forecast = HormoneSimulationEngine().simulate(listOf(future.copy(planned = true)), listOf(p), 0.0, 24.0, 1.0, 3, includePlanned = true)
        assertEquals(listOf("f"), forecast.included)
        assertTrue(forecast.samples.last().pgMl > 0)
    }

    @Test fun explicitUnknownProfileDoesNotFallBackToAnotherModel() {
        val p = catalog.models.first().parameters()
        val event = HormoneEvent("e", p.medicationId, p.route, 0.0, 1.0, profileId = "unknown")
        assertEquals("unsupported_parameter_set", HormoneSimulationEngine().simulate(listOf(event), listOf(p), 0.0, 24.0, 24.0).excluded["e"])
    }
}
