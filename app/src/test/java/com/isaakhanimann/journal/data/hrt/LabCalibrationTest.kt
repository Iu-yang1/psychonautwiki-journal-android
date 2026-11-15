package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.data.room.experiences.entities.Ingestion
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.classes.MedicationCatalogFile
import com.isaakhanimann.journal.data.substances.parse.SubstanceParser
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class LabCalibrationTest {
    private fun raw(name: String) = javaClass.classLoader!!.getResourceAsStream(name)!!.bufferedReader().use { it.readText() }
    private val catalog = HormoneReferenceCatalog.parse(raw("hrt_reference_models.json"))
    private val profile = catalog.models.single { it.id == "e2-oral-reference-v1" }
    private val substances = run {
        val ids = Json.decodeFromString<MedicationCatalogFile>(raw("medication_catalog.json")).entries.associateBy { it.id }
        SubstanceParser().parseSubstanceFile(raw("substances.json")).substances.map { it.copy(identity = ids[it.id]) }
    }
    private val record = Ingestion(1, "Oral Estradiol", Instant.EPOCH, administrationRoute = AdministrationRoute.ORAL, dose = 1.0, isDoseAnEstimate = false, estimatedDoseStandardDeviation = null, units = "mg", experienceId = 1, notes = null, stomachFullness = null, consumerName = null, customUnitId = null)
    private val log = HormoneJournalData(listOf(record), listOf(MedicationLink(1, profile.medicationId, "identified", record.substanceName)), emptyList(), emptyList(), emptyList(), substances = substances)
    @Test fun exactMeasuredE2ChangesHistoryAndForecastWithoutChangingRecordedDose() {
        val predicted = HormoneSimulationEngine().simulate(listOf(HormoneEvent("1", profile.medicationId, profile.route, 0.0, 1.0, profileId = profile.id)), listOf(profile.parameters()), 4.0, 4.1, 24.0, 2).samples.first().pgMl
        val lab = HrtLab("lab", null, "E2", 4 * 3600000L, 2 * predicted * 3.671, "pmol/L")
        val ctx = LabDoseContext("lab", 4.0, 1, 0, true)
        val service = HormoneChartService(catalog)
        val baseline = service.calculate(log, HormoneChartRequest(), 24 * 3600000L)
        val fitted = service.calculate(log.copy(labs = listOf(lab), settings = listOf(ctx.setting())), HormoneChartRequest(), 24 * 3600000L)
        assertEquals(2.0, fitted.calibration.getValue("history").scale, 1e-10)
        assertEquals(listOf("lab"), fitted.calibration.getValue("history").labIds)
        assertEquals(baseline.history.last().pgMl * 2, fitted.history.last().pgMl, 1e-10)
        assertEquals(baseline.forecast.first().pgMl * 2, fitted.forecast.first().pgMl, 1e-10)
        assertEquals(1.0, record.dose!!, 0.0)
        val mismatch = service.calculate(log.copy(labs = listOf(lab), settings = listOf(ctx.copy(elapsedHours = 1.0).setting())), HormoneChartRequest(), 24 * 3600000L)
        assertEquals(1.0, mismatch.calibration.getValue("history").scale, 0.0)
        val censored = service.calculate(log.copy(labs = listOf(lab.copy(comparison = "<")), settings = listOf(ctx.setting())), HormoneChartRequest(), 24 * 3600000L)
        assertEquals(1.0, censored.calibration.getValue("history").scale, 0.0)
    }

    @Test fun individualBaselineIsAddedOnceAndSubtractedBeforeAmplitudeCalibration() {
        val model = HormoneEvent("1", profile.medicationId, profile.route, 0.0, 1.0, profileId = profile.id)
        val predicted = HormoneSimulationEngine().simulate(listOf(model), listOf(profile.parameters()), 4.0, 4.1, 24.0, 2).samples.first().pgMl
        val lab = HrtLab("lab", null, "E2", 4 * 3600000L, 12 + 2 * predicted, "pg/mL")
        val settings = listOf(HormoneModelOptions(12.0).setting(null), LabDoseContext("lab", 4.0, 1, 0, true).setting())
        val fitted = HormoneChartService(catalog).calculate(log.copy(labs = listOf(lab), settings = settings), HormoneChartRequest(), 24 * 3600000L)
        val fit = fitted.calibration.getValue("history")
        assertEquals(2.0, fit.scale, 1e-10)
        assertEquals(12.0, fitted.history.first().pgMl, 1e-10)
        assertEquals(0.0, fit.meanAbsoluteErrorPgMl!!, 1e-10)
    }

    @Test fun calibrationPhaseFiltersOlderLabsAndReportsForwardError() {
        val model = HormoneEvent("1", profile.medicationId, profile.route, 0.0, 1.0, profileId = profile.id)
        val labs = listOf(2, 4, 8).map { hour ->
            val predicted = HormoneSimulationEngine().simulate(listOf(model), listOf(profile.parameters()),
                hour.toDouble(), hour + .1, 24.0, 2).samples.first().pgMl
            HrtLab("lab$hour", null, "E2", hour * 3600000L, (if (hour == 2) 9 else 2) * predicted, "pg/mL")
        }
        val settings = labs.map { LabDoseContext(it.id, it.time / 3600000.0, 1, 0, true).setting() } +
            HormoneModelOptions(calibrationStartMillis = 3 * 3600000L).setting(null)
        val fitted = LabCalibration.fit(log.copy(labs = labs, settings = settings), null, "E2", listOf(model), listOf(profile.parameters()), 24 * 3600000L)
        assertEquals(listOf("lab4", "lab8"), fitted.labIds)
        assertEquals(2.0, fitted.scale, 1e-10)
        assertEquals(0.0, fitted.forwardValidationErrorPgMl!!, 1e-10)
    }
}
