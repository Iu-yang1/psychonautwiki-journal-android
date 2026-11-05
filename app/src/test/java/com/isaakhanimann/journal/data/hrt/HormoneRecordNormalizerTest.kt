package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.data.room.experiences.entities.Ingestion
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.classes.MedicationCatalogFile
import com.isaakhanimann.journal.data.substances.parse.SubstanceParser
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class HormoneRecordNormalizerTest {
    private fun resource(name: String) = requireNotNull(javaClass.classLoader!!.getResourceAsStream(name)).bufferedReader().use { it.readText() }
    private val catalog get() = HormoneReferenceCatalog.parse(resource("hrt_reference_models.json"))
    private val substances by lazy {
        val identities = Json.decodeFromString<MedicationCatalogFile>(resource("medication_catalog.json")).entries.associateBy { it.id }
        SubstanceParser().parseSubstanceFile(resource("substances.json")).substances.map { it.copy(identity = identities[it.id]) }
    }
    private fun event(profile: HormoneReferenceProfile, id: Int = 1, dose: Double = 1.0, unit: String = "mg") = Ingestion(id, "original name", Instant.EPOCH,
        administrationRoute = AdministrationRoute.valueOf(profile.administrationRoutes.first()), dose = dose,
        isDoseAnEstimate = false, estimatedDoseStandardDeviation = null, units = unit, experienceId = 1,
        notes = null, stomachFullness = null, consumerName = null, customUnitId = null)
    private fun link(event: Ingestion, profile: HormoneReferenceProfile) = MedicationLink(event.id, profile.medicationId, "identified", event.substanceName)
    private fun normalize(records: List<Ingestion>, links: List<MedicationLink>, details: List<HrtEventDetail> = emptyList(), settings: List<HrtModelSetting> = emptyList(), consumer: String? = null, analyteId: String = "E2") =
        HormoneRecordNormalizer.normalize(records, links, details, settings, substances, catalog, consumer, 336.0, 500.0, analyteId = analyteId)
    private fun product(profile: HormoneReferenceProfile, patch: Boolean = false) = MedicationProduct("p", profile.medicationId, "reference product", profile.formulationId,
        if (patch) "patch" else "mg", if (patch) 4.0 else 1.0, if (patch) 50.0 else null, modelProfileId = profile.id)

    @Test fun esterMassIsConvertedOnceBeforeReferenceResponse() {
        val profile = catalog.models.single { it.id.contains("im-ev") }
        val record = event(profile, dose = 5.0)
        val normalized = normalize(listOf(record), listOf(link(record, profile)))
        assertEquals(5 * 272.38 / 356.50, normalized.events.single().doseMg!!, 1e-12)
        val response = HormoneSimulationEngine().simulate(normalized.events, listOf(profile.parameters()), 52.8, 53.0, 336.0, 2)
        assertEquals(667.0, response.samples.first().pgMl, .001)
        assertEquals(5.0, record.dose!!, 0.0)
        assertEquals("original name", record.substanceName)
    }

    @Test fun allFiveFamiliesNormalizeWithTheirExplicitRequiredFields() {
        val routes = mutableSetOf<HormoneRoute>()
        catalog.models.forEachIndexed { index, profile ->
            val patch = profile.route == HormoneRoute.PATCH
            val record = event(profile, index + 1, dose = profile.referenceDoseMg ?: 1.0, unit = if (patch) "patch" else "mg")
            val site = profile.allowedSites.firstOrNull() ?: when (profile.route) { HormoneRoute.GEL -> "上臂"; HormoneRoute.PATCH -> "腹部"; else -> null }
            val result = normalize(listOf(record), listOf(link(record,profile)),
                listOf(HrtEventDetail(record.id, site = site, patchRemovedAt = if (patch) 80 * 3600000L else null)),
                listOf(EventProductSnapshot(product(profile,patch),1).setting(record.id)), analyteId = profile.analyteId)
            assertTrue("${profile.id}: ${result.excluded}", result.excluded.isEmpty())
            routes += result.events.single().route
        }
        assertEquals(HormoneRoute.entries.toSet(),routes)
    }

    @Test fun imParametersCannotBeAppliedToSubcutaneousRecords() {
        val profile = catalog.models.first { it.route == HormoneRoute.INJECTION }
        val record = event(profile).copy(administrationRoute = AdministrationRoute.SUBCUTANEOUS)
        val result = normalize(listOf(record), listOf(link(record, profile)), settings = listOf(EventProductSnapshot(product(profile), 1).setting(record.id)))
        assertEquals("profile_form_or_route_mismatch", result.excluded[record.id.toString()])
        assertTrue(result.events.isEmpty())
    }

    @Test fun consumerSubjectsIncludingNullAndBlankAreNotMerged() {
        val profile = catalog.models.single { it.id == "e2-oral-reference-v1" }
        val records = listOf(event(profile, 1), event(profile, 2).copy(consumerName = ""), event(profile, 3).copy(consumerName = "person B"))
        val links = records.map { link(it, profile) }
        assertEquals(listOf("1"), normalize(records, links).events.map { it.id })
        assertEquals(listOf("2"), normalize(records, links, consumer = "").events.map { it.id })
    }

    @Test fun gelNeedsStudiedApplicationSiteAndExplicitProfile() {
        val profile = catalog.models.single { it.id == "e2-gel006-reference-v1" }; val record = event(profile)
        val links = listOf(link(record, profile)); val settings = listOf(EventProductSnapshot(product(profile), 1).setting(record.id))
        assertTrue(normalize(listOf(record), links).events.isEmpty())
        assertEquals("missing_application_site", normalize(listOf(record), links, settings = settings).excluded["1"])
        assertEquals("reference_application_site_mismatch", normalize(listOf(record), links, listOf(HrtEventDetail(1, site = "scrotum")), settings).excluded["1"])
    }

    @Test fun patchLoadingIsNeverSubstitutedForReleaseAndUnknownWearIsExcluded() {
        val profile = catalog.models.single { it.route == HormoneRoute.PATCH }; val record = event(profile, unit = "patch")
        val links = listOf(link(record, profile)); val settings = listOf(EventProductSnapshot(product(profile, true), 1).setting(1))
        val result = normalize(listOf(record), links, listOf(HrtEventDetail(1, site = "腹部", patchRemovedAt = 80 * 3600000L)), settings)
        assertEquals(50.0 / 24000, result.events.single().releaseMgHour!!, 1e-12)
        assertNull(result.events.single().doseMg)
        assertEquals("missing_patch_wear_interval", normalize(listOf(record), links, listOf(HrtEventDetail(1, site = "腹部")), settings).excluded["1"])
        val ongoing = listOf(EventProductSnapshot(product(profile, true), 1, patchOngoing = true).setting(1))
        assertEquals(500.0, normalize(listOf(record), links, listOf(HrtEventDetail(1, site = "腹部")), ongoing).events.single().endHours!!, 0.0)
    }
}
