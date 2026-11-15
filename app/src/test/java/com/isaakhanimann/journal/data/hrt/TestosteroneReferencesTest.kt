package com.isaakhanimann.journal.data.hrt

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class TestosteroneReferencesTest {
    private val start = Instant.parse("2026-01-15T08:00:00Z").toEpochMilli() / 3600000.0
    private val now = start + 7 * 24
    private fun estrogen(route: HormoneRoute = HormoneRoute.ORAL, time: Double = start) =
        TestosteroneEstrogenContext("e2", route, time)
    private fun dose(drug: AntiandrogenDrug = AntiandrogenDrug.CPA, time: Double = start, mg: Double = 12.5) =
        AntiandrogenDose(drug.name, drug, time, mg)
    private fun calculate(events: List<TestosteroneEstrogenContext> = listOf(estrogen()),
        doses: List<AntiandrogenDose> = listOf(dose()), end: Double = now + 24) =
        TestosteroneReferences.calculate(events, doses, now - 24, now, end)

    @Test fun publishedMeansRetainOriginalUnitsStatisticsAndRandomizedGroupSizes() {
        val cpa = TestosteroneReferences.studies.single { it.id == "cpa-2025" }
        assertEquals(listOf(0, 3, 6), cpa.points.map { it.month })
        assertEquals(listOf(17.30, .81, 1.48), cpa.points.map { it.mean })
        assertEquals(32, cpa.participants)
        assertEquals(1.48 / .0347, cpa.ngDl(cpa.points.last()), 1e-12)
        val spiro = TestosteroneReferences.studies.single { it.id == "spiro-2025" }
        assertEquals(listOf(17.02, 6.75, 4.15), spiro.points.map { it.mean })
        assertEquals(31, spiro.participants)
        val treat = TestosteroneReferences.studies.filter { it.id.startsWith("treat") }
        assertEquals(listOf(listOf(498.6, 62.7, 29.4), listOf(481.3, 278.5, 108.1),
            listOf(505.3, 313.8, 121.7)), treat.map { study -> study.points.map { it.mean } })
        assertEquals(listOf(12, 13, 14), treat.map { it.participants })
        assertTrue(treat.all { it.table == "表3" && it.unit == "ng/dL" })
        assertTrue(TestosteroneReferences.studies.all { it.sourceTitle.isNotBlank() && it.sourceUrl.startsWith("https://pmc.ncbi.nlm.nih.gov/") })
    }

    @Test fun automaticReferenceUsesOldRecordsOutsideVisibleWindow() {
        val result = calculate()
        val reference = result.scenarios.single()
        assertEquals("cpa-2025", reference.referenceId)
        assertEquals(start, reference.referenceStartHours!!, 0.0)
        assertEquals(now - 24, reference.history.first().timeHours, 0.0)
        assertEquals(reference.history.last(), reference.forecast.first())
        val later = calculate(doses = listOf(dose(time = start + 24))).scenarios.single()
        assertEquals(start + 24, later.referenceStartHours!!, 0.0)
        assertTrue(later.history.first().ngDl > reference.history.first().ngDl)
    }

    @Test fun changingDosesChangesResidualsButDoesNotInventADoseResponse() {
        val small = calculate()
        val large = calculate(doses = listOf(dose(mg = 50.0)))
        assertEquals(small.scenarios, large.scenarios)
        assertEquals(small.residuals.single().remainingDoseProxyMg * 4,
            large.residuals.single().remainingDoseProxyMg, 1e-12)
    }

    @Test fun twoDrugsRemainSeparateResearchComparisons() {
        val result = calculate(doses = listOf(dose(), dose(AntiandrogenDrug.SPIRONOLACTONE, mg = 100.0)))
        assertEquals(setOf("cpa-2025", "spiro-2025"), result.scenarios.map { it.referenceId }.toSet())
        assertEquals(2, result.residuals.size)
        assertTrue(result.notes.any { it.contains("不合并") })
    }

    @Test fun sublingualFrequencyIsShownAsTwoStudyComparisonsRatherThanGuessedFromOneDose() {
        val result = calculate(events = listOf(estrogen(HormoneRoute.SUBLINGUAL)),
            doses = listOf(dose(AntiandrogenDrug.SPIRONOLACTONE)))
        assertEquals(setOf("treat-sl-once-2024", "treat-sl-twice-2024"), result.scenarios.map { it.referenceId }.toSet())
    }

    @Test fun unstudiedInjectionAndMissingAntiadrogenHaveNoInventedReference() {
        assertTrue(calculate(events = listOf(estrogen(HormoneRoute.INJECTION))).scenarios.isEmpty())
        assertTrue(calculate(doses = emptyList()).scenarios.isEmpty())
        assertTrue(calculate(events = emptyList()).scenarios.isEmpty())
    }

    @Test fun publishedFollowupDatesAreExactCalendarMonthsAndNotExtrapolatedPastSixMonths() {
        val end = Instant.parse("2026-07-15T08:00:00Z").toEpochMilli() / 3600000.0
        val result = calculate(end = end + 100).scenarios.single()
        assertEquals(end, result.forecast.last().timeHours, 0.0)
        assertEquals(1.48 / .0347, result.forecast.last().ngDl, 1e-12)
        val after = TestosteroneReferences.calculate(listOf(estrogen()), listOf(dose()), end + 1, end + 2, end + 3)
        assertTrue(after.scenarios.isEmpty())
        assertTrue(after.notes.last().contains("六个月"))
    }

    @Test fun cpaReferenceRetainsReportedRiseBetweenThreeAndSixMonths() {
        val middle = Instant.parse("2026-04-15T08:00:00Z").toEpochMilli() / 3600000.0
        val result = TestosteroneReferences.calculate(listOf(estrogen()), listOf(dose()), middle, middle + 24, middle + 48)
        val history = result.scenarios.single().history
        assertEquals(.81 / .0347, history.first().ngDl, 1e-12)
        assertTrue(history.last().ngDl > history.first().ngDl)
    }

    @Test fun residualUsesEveryActualHistoricalDoseWithDrugSpecificSourcedHalfLife() {
        val doses = listOf(dose(time = now - 43.9, mg = 50.0), dose(time = now, mg = 12.5),
            dose(AntiandrogenDrug.SPIRONOLACTONE, now - 16.5, 100.0), dose(time = now + 1, mg = 1000.0))
        val residuals = TestosteroneReferences.residuals(doses, now)
        val cpa = residuals.single { it.drug == AntiandrogenDrug.CPA }
        assertEquals(2, cpa.recordCount)
        assertEquals(62.5, cpa.totalRecordedMg, 0.0)
        assertEquals(37.5, cpa.remainingDoseProxyMg, 1e-9)
        assertEquals(50.0, residuals.single { it.drug == AntiandrogenDrug.SPIRONOLACTONE }.remainingDoseProxyMg, 1e-9)
        assertEquals(cpa.remainingDoseProxyMg / 2,
            TestosteroneReferences.residuals(doses.filter { it.timeHours <= now }, now + 43.9)
                .single { it.drug == AntiandrogenDrug.CPA }.remainingDoseProxyMg, 1e-9)
    }

    @Test fun referenceDefaultsAndLegacySettingsRemainCompatibleAndConsumerSpecific() {
        val legacy = HrtModelSetting("hormone-model:null", """{"baselineE2PgMl":12.0,"testosterone":null}""")
        assertTrue(HormoneModelOptions.read(listOf(legacy), null).showTestosteroneReference)
        val disabled = HormoneModelOptions(showTestosteroneReference = false)
        val settings = listOf(disabled.setting("B"), legacy)
        assertEquals(disabled, HormoneModelOptions.read(settings, "B"))
        assertTrue(HormoneModelOptions.read(settings, "").showTestosteroneReference)
        assertTrue(HormoneModelOptions.read(settings, null).showTestosteroneReference)
    }
}
