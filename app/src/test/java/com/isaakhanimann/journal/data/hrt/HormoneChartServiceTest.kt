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
        assertTrue(chart.forecast.isNotEmpty()) // Existing doses still have a hypothetical decay tail.
    }

    @Test fun measuredPointsKeepComparisonAreIndependentAndCannotFitTheModel() {
        val a = HrtLab("a", null, "E2", now-1000, 367.1, "pmol/L", "<")
        val other = a.copy(id = "other", consumer = "person B", value = 999.0)
        val future = a.copy(id = "future", time = now+1000)
        val unknown = a.copy(id = "unknown", unit = "arbitrary unit")
        val base = service.calculate(data(), HormoneChartRequest(), now)
        val with = service.calculate(data(labs = listOf(a, other, future, unknown)), HormoneChartRequest(), now)
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
}
