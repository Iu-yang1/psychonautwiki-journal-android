package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.data.room.experiences.entities.Ingestion
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.classes.MedicationCatalogFile
import com.isaakhanimann.journal.data.substances.parse.SubstanceParser
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class HormoneChartServiceTest {
    private fun resource(name: String) = requireNotNull(javaClass.classLoader!!.getResourceAsStream(name)).bufferedReader().use { it.readText() }
    private val catalog = HormoneReferenceCatalog.parse(resource("hrt_reference_models.json"))
    private val substances = run {
        val identities = Json.decodeFromString<MedicationCatalogFile>(resource("medication_catalog.json")).entries.associateBy { it.id }
        SubstanceParser().parseSubstanceFile(resource("substances.json")).substances.map { it.copy(identity = identities[it.id]) }
    }
    private val oral = catalog.models.single { it.id == "e2-oral-reference-v1" }
    private val now = 1800000000000L
    private val record = Ingestion(1, "original", Instant.ofEpochMilli(now-3600000), administrationRoute = AdministrationRoute.ORAL,
        dose = 1.0, isDoseAnEstimate = false, estimatedDoseStandardDeviation = null, units = "mg", experienceId = 1,
        notes = "retained", stomachFullness = null, consumerName = null, customUnitId = null)
    private val plan = HrtPlan("p", oral.medicationId, "Oral Estradiol", null, "ORAL", 1.0, "mg", now-24*3600000L, 24)
    private fun data(records: List<Ingestion> = listOf(record), plans: List<HrtPlan> = listOf(plan), settings: List<HrtModelSetting> = emptyList(), labs: List<HrtLab> = emptyList()) =
        HormoneJournalData(records, records.map { MedicationLink(it.id, oral.medicationId, "identified", it.substanceName) }, emptyList(), settings, plans, labs, substances)
    private val service = HormoneChartService(catalog)

    @Test fun forecastNeverBackfillsPastAndDoesNotChangeHistoricalResponse() {
        val journal = data()
        val without = service.calculate(journal, HormoneChartRequest(historyDays = 7, forecastDays = 0), now)
        val with = service.calculate(journal, HormoneChartRequest(historyDays = 7, forecastDays = 3), now)
        assertEquals(without.history, with.history)
        assertEquals(3, with.includedPlanned.size)
        assertTrue(with.includedPlanned.all { it.substringAfterLast(':').toLong() > now })
        assertEquals(with.history.last().pgMl, with.forecast.first().pgMl, 1e-9)
        assertEquals(1.0, record.dose!!, 0.0)
        assertTrue(journal.settings.isEmpty())
        assertEquals(listOf(plan), journal.plans)
    }

    @Test fun skippedFutureSlotsAndOtherSubjectsAreNotProjected() {
        val skipped = HrtModelSetting(PlanOccurrences.key(plan.id, now+24*3600000L), "skipped")
        val journal = data(plans = listOf(plan, plan.copy(id = "other", consumer = "")), settings = listOf(skipped))
        val chart = service.calculate(journal, HormoneChartRequest(forecastDays = 3), now)
        assertEquals(2, chart.includedPlanned.size)
        assertTrue(chart.includedPlanned.none { it.startsWith("plan:other:") || it.endsWith((now+24*3600000L).toString()) })
    }

    @Test fun pausedPlansCannotCreateFutureCurveEvents() {
        val chart = service.calculate(data(plans = listOf(plan.copy(paused = true))), HormoneChartRequest(forecastDays = 3), now)
        assertTrue(chart.includedPlanned.isEmpty())
        assertTrue(chart.forecast.isNotEmpty())
    }

    @Test fun measuredPointsKeepComparisonAreIndependentAndCannotFitTheModel() {
        val lab = HrtLab("a", null, "E2", now-1000, 367.1, "pmol/L", "<")
        val other = lab.copy(id = "other", consumer = "person B", value = 999.0)
        val future = lab.copy(id = "future", time = now+1000)
        val unknown = lab.copy(id = "unknown", unit = "arbitrary unit")
        val base = service.calculate(data(), HormoneChartRequest(), now)
        val with = service.calculate(data(labs = listOf(lab, other, future, unknown)), HormoneChartRequest(), now)
        assertEquals(base.history, with.history)
        assertEquals(listOf("a"), with.measured.map { it.original.id })
        assertEquals("<", with.measured.single().original.comparison)
        assertEquals(100.0, with.measured.single().value, 1e-9)
        assertTrue(with.excluded.any { it.id == "lab:future" && it.reason == "future_lab_record" })
        assertTrue(with.excluded.any { it.id == "lab:unknown" && it.reason == "unsupported_lab_unit" })
    }

    @Test fun futureDatedActualRecordIsNotTurnedIntoPlannedInput() {
        val future = record.copy(id = 2, time = Instant.ofEpochMilli(now+3600000))
        val chart = service.calculate(data(records = listOf(future), plans = emptyList()), HormoneChartRequest(), now)
        assertTrue(chart.history.isEmpty())
        assertTrue(chart.forecast.isEmpty())
        assertTrue(chart.excluded.any { it.id == "2" && it.reason == "future_actual_record" })
    }

    @Test fun patchPlanRequiresExplicitWearAndDoesNotInferItFromFrequency() {
        val patch = catalog.models.single { it.route == HormoneRoute.PATCH }
        val product = MedicationProduct("patch", patch.medicationId, "patch", "patch", "patch", patchReleaseMcgDay = 50.0, modelProfileId = patch.id)
        val plan = plan.copy(medicationId = patch.medicationId, route = "TRANSDERMAL", dose = 1.0, unit = "patch", name = "patch")
        val rule = PlanSchedule(productId = product.id, applicationSite = "腹部")
        val journal = data(records = emptyList(), plans = listOf(plan), settings = listOf(product.setting(), rule.setting(plan.id)))
        val missing = service.calculate(journal, HormoneChartRequest(forecastDays = 3), now)
        assertTrue(missing.includedPlanned.isEmpty())
        assertTrue(missing.excluded.any { it.reason == "missing_planned_patch_wear" })
        val ready = service.calculate(journal.copy(settings = listOf(product.setting(), rule.copy(patchWearHours = 80.0).setting(plan.id))), HormoneChartRequest(forecastDays = 3), now)
        assertEquals(3, ready.includedPlanned.size)
        assertTrue(ready.forecast.last().pgMl > 0)
    }

    @Test fun estimatedInputsAreNotPresentedAsConfidenceIntervals() {
        val chart = service.calculate(data(records = listOf(record.copy(isDoseAnEstimate = true))), HormoneChartRequest(), now)
        assertTrue(chart.assumptions.any { it.contains("估计值") && it.contains("置信区间") })
    }

    @Test fun testosteroneLabsRemainUnmodifiedAndCannotAffectE2() {
        val labs = listOf(
            HrtLab("t", null, "testosterone", now - 1000, 3.47, "nmol/L"),
            HrtLab("other", "person B", "testosterone", now - 1000, 999.0, "ng/dL"))
        val journal = data(labs = labs)
        val baseline = service.calculate(data(), HormoneChartRequest(), now)
        val chart = service.calculate(journal, HormoneChartRequest(), now)
        assertEquals(baseline, chart)
        assertEquals(labs, journal.labs)
        val normalized = LabTrends.normalize(journal.labs.first())
        assertEquals(100.0, normalized.value, 1e-9)
        assertEquals("ng/dL", normalized.unit)
    }

    @Test fun gelForecastRetainsContinuityAndIncludesFutureEstrogenPlans() {
        val gel = catalog.models.single { it.id == "e2-gel006-reference-v1" }
        val gelRecord = record.copy(substanceName = "Estradiol Gel", administrationRoute = AdministrationRoute.TRANSDERMAL)
        val journal = HormoneJournalData(listOf(gelRecord),
            listOf(MedicationLink(1, gel.medicationId, "identified", gelRecord.substanceName)),
            emptyList(), emptyList(), listOf(plan), substances = substances)
        val chart = service.calculate(journal, HormoneChartRequest(historyDays = 1), now)
        val noPlans = service.calculate(journal.copy(plans = emptyList()), HormoneChartRequest(historyDays = 1), now)
        assertEquals(2, chart.gelScenarios.size)
        chart.gelScenarios.forEach { scenario ->
            val decay = noPlans.gelScenarios.single { it.id == scenario.id }
            assertEquals(scenario.history, decay.history)
            assertEquals(scenario.history.last().pgMl, scenario.forecast.first().pgMl, 1e-8)
            assertTrue(scenario.forecast.last().pgMl > decay.forecast.last().pgMl)
        }
    }

    @Test fun historicalAntiandrogensRemainStoredAndCpaRemainsRecognizedWithoutChangingE2() {
        val cpaId = substances.single { it.identity?.ingredientId == "ingredient:cyproterone-acetate" }.id
        val spiroId = substances.single { it.identity?.ingredientId == "ingredient:spironolactone" }.id
        val old = record.copy(id = 11, time = Instant.ofEpochMilli(now - 96 * 3600000L),
            dose = 50000.0, units = "mcg")
        val tablet = record.copy(id = 12, dose = .5, units = "片")
        val other = old.copy(id = 13, consumerName = "other")
        val future = old.copy(id = 14, time = Instant.ofEpochMilli(now + 3600000))
        val base = data()
        val records = listOf(old, tablet, other, future)
        val journal = base.copy(records = base.records + records, links = base.links +
            records.map { MedicationLink(it.id, if (it.id == tablet.id) spiroId else cpaId, "identified", it.substanceName) })
        val chart = service.calculate(journal, HormoneChartRequest(historyDays = 1), now)
        val baseline = service.calculate(base, HormoneChartRequest(historyDays = 1), now)
        assertEquals(baseline.history, chart.history)
        assertEquals(baseline.forecast, chart.forecast)
        assertEquals(baseline.assumptions, chart.assumptions)
        assertEquals(setOf(1, 11), chart.recognizedHrt.map { it.id }.toSet())
        val cpa = chart.recognizedHrt.single { it.id == old.id }
        assertFalse(cpa.inVisibleRange)
        assertEquals("non_e2_record", cpa.modelReason)
        assertEquals(old.dose, cpa.dose)
        assertEquals("mcg", cpa.unit)
        assertEquals(tablet, journal.records.single { it.id == tablet.id })
        assertEquals(spiroId, journal.links.single { it.ingestionId == tablet.id }.medicationId)
        assertEquals(base.records + records, journal.records)
        assertTrue(journal.settings.isEmpty())
    }

    @Test fun legacyTestosteroneSettingsLeaveTheE2ChartUnchanged() {
        val options = HormoneModelOptions(12.0, now - 24 * 3600000L)
        val legacy = options.setting(null).let {
            it.copy(payload = it.payload.dropLast(1) +
                ""","testosterone":{"ec50PgMl":0.0},"showTestosteroneReference":true}""")
        }
        val expected = service.calculate(data(settings = listOf(options.setting(null))), HormoneChartRequest(), now)
        val journal = data(settings = listOf(legacy))
        assertEquals(expected, service.calculate(journal, HormoneChartRequest(), now))
        assertEquals(listOf(legacy), journal.settings)
    }

    @Test fun explicitE2BaselineRemainsVisibleWithoutModeledDosesAndDoesNotAffectOtherAnalytes() {
        val journal = data(records = emptyList(), plans = emptyList(),
            settings = listOf(HormoneModelOptions(baselineE2PgMl = 25.0).setting(null)))
        val chart = service.calculate(journal, HormoneChartRequest(forecastDays = 1), now)
        assertTrue(chart.history.isNotEmpty())
        assertTrue(chart.history.all { it.pgMl == 25.0 })
        assertTrue(chart.forecast.all { it.pgMl == 25.0 })
        assertEquals(chart.history.last(), chart.forecast.first())
        assertTrue(chart.includedActual.isEmpty())
        val withoutForecast = service.calculate(journal, HormoneChartRequest(forecastDays = 0), now)
        assertTrue(withoutForecast.forecast.isEmpty())
        val other = service.calculate(journal, HormoneChartRequest(analyteId = "E4"), now)
        assertTrue(other.history.isEmpty())
        assertTrue(other.forecast.isEmpty())
    }
}
