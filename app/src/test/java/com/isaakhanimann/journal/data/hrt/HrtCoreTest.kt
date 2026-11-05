package com.isaakhanimann.journal.data.hrt

import org.junit.Assert.*
import org.junit.Test

class HrtCoreTest {
    private val catalog = listOf(IdentityEntry("oral", "Oral Estradiol", listOf("E2"), setOf("ORAL")), IdentityEntry("gel", "Estradiol Gel", listOf("E2"), setOf("TRANSDERMAL")))
    @Test fun ambiguousAliasIsNotResolvedUsingRoute() { assertNull(MedicationIdentity.resolve("E2", "ORAL", catalog, emptySet())) }
    @Test fun customNameCollisionRemainsUnresolved() { assertNull(MedicationIdentity.resolve("Oral Estradiol", "ORAL", catalog, setOf("oral estradiol"))) }
    @Test fun exactNameRequiresMatchingRoute() {
        assertEquals("oral", MedicationIdentity.resolve("ORAL ESTRADIOL", "ORAL", catalog, emptySet()))
        assertNull(MedicationIdentity.resolve("Oral Estradiol", "SUBLINGUAL", catalog, emptySet()))
    }
    @Test fun unsupportedModelDoesNotDrawZeroCurve() {
        val result = HormoneSimulationEngine().simulate(listOf(HormoneEvent("a", "unknown", HormoneRoute.ORAL, 0.0, 1.0)), emptyList(), 0.0, 24.0, 24.0)
        assertTrue(result.samples.isEmpty()); assertEquals("unsupported_parameter_set", result.excluded["a"])
    }
    @Test fun patchRemovalAndFutureEvents() {
        val p = HormoneParameters("patch", HormoneRoute.PATCH, "synthetic-test", "analytic fixture", true, 1.0, 0.1, 1.0)
        val result = HormoneSimulationEngine().simulate(listOf(HormoneEvent("a", "patch", HormoneRoute.PATCH, 0.0, null, 10.0, 1.0), HormoneEvent("b", "patch", HormoneRoute.PATCH, 30.0, null, 40.0, 1.0)), listOf(p), 0.0, 20.0, 20.0, 3)
        assertEquals(listOf("a"), result.included)
        assertTrue(result.samples[1].pgMl > result.samples[2].pgMl)
        assertEquals("future_or_invalid_time", result.excluded["b"])
    }
    @Test fun units() { assertEquals(100.0, LabUnits.estradiolPgMl(367.1, "pmol/L")!!, 0.001); assertNull(LabUnits.estradiolPgMl(1.0, "mg")) }
}
