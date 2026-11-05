package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.data.room.experiences.entities.Ingestion
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.classes.*
import com.isaakhanimann.journal.data.substances.parse.SubstanceParser
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class HrtCompletionRegressionTest {
    private fun raw(name: String) = requireNotNull(javaClass.classLoader!!.getResourceAsStream(name)).bufferedReader().use { it.readText() }
    private val catalog = HormoneReferenceCatalog.parse(raw("hrt_reference_models.json"))
    private val substances = run {
        val parser = SubstanceParser()
        val identities = Json.decodeFromString<MedicationCatalogFile>(raw("medication_catalog.json")).entries.associateBy { it.id }
        mergeMedicationFiles(parser.parseSubstanceFile(raw("substances.json")), parser.parseSubstanceFile(raw("substances_base.json"))).withHrtTaxonomy().substances.map {
            it.copy(identity = identities[it.id], commonNames = (it.commonNames + identities[it.id]?.aliases.orEmpty()).distinct())
        }
    }
    private val identities = MedicationIdentityIndex(substances.map { IdentityEntry(it.id, it.name, it.commonNames, it.availableRoutesForIngestion().map { route -> route.name }.toSet()) })
    private fun event(name: String, amount: Double = 3.0, unit: String = "mg", route: AdministrationRoute = AdministrationRoute.TRANSDERMAL) =
        Ingestion(1, name, Instant.EPOCH, administrationRoute = route, dose = amount, isDoseAnEstimate = false,
            estimatedDoseStandardDeviation = null, units = unit, experienceId = 1, notes = "retained", stomachFullness = null, consumerName = null, customUnitId = null)

    private fun normalize(record: Ingestion, product: MedicationProduct?, basis: DoseBasis, site: String = "上臂", analyteId: String = "E2"): NormalizedHormoneRecords {
        val id = requireNotNull(identities.resolve(record.substanceName, record.administrationRoute.name))
        return HormoneRecordNormalizer.normalize(listOf(record), listOf(MedicationLink(1, id, "identified", record.substanceName)),
            listOf(HrtEventDetail(1, site = site, doseBasis = basis.name)), product?.let { listOf(EventProductSnapshot(it, 0, basis).setting(1)) }.orEmpty(),
            substances, catalog, null, 24.0, 48.0, analyteId = analyteId)
    }

    @Test fun canonicalAndChineseCpaAreRecognizedAsAntiandrogenWithoutAnE2Failure() {
        val cpa = substances.single { it.name == "Cyproterone Acetate" }
        listOf("Cyproterone Acetate", "醋酸环丙孕酮", "CPA", "Androcur").forEach { assertEquals(cpa.id, identities.resolve(it, "ORAL")) }
        val record = event("Cyproterone Acetate", 12.5, route = AdministrationRoute.ORAL)
        val chart = HormoneChartService(catalog).calculate(HormoneJournalData(listOf(record), listOf(MedicationLink(1, cpa.id, "identified", record.substanceName)), emptyList(), emptyList(), emptyList(), substances = substances),
            HormoneChartRequest(forecastDays = 0), 24 * 3600000L)
        assertTrue(chart.excluded.isEmpty())
        assertTrue(chart.history.isEmpty())
        assertEquals("抗雄激素", chart.recognizedHrt.single().category)
        assertEquals(12.5, chart.recognizedHrt.single().dose!!, 0.0)
        assertEquals("retained", record.notes)
    }

    @Test fun existingEstrogensAndAntiandrogensHaveRecordableCanonicalIdentities() {
        substances.filter { "hrt-estrogens" in it.categories || "hrt-antiandrogens" in it.categories }.forEach { medication ->
            val routes = medication.availableRoutesForIngestion()
            assertTrue("No route: ${medication.name}", routes.isNotEmpty())
            routes.forEach { assertEquals(medication.name, medication.id, identities.resolve(medication.name, it.name)) }
            val allEntries = substances.map { IdentityEntry(it.id, it.name, it.commonNames, it.availableRoutesForIngestion().map { route -> route.name }.toSet()) }
            medication.commonNames.forEach { alias ->
                val key = MedicationIdentity.normalize(alias)
                val exact = allEntries.filter { MedicationIdentity.normalize(it.name) == key }
                val candidates = if (exact.isNotEmpty()) exact else allEntries.filter { entry -> entry.aliases.any { MedicationIdentity.normalize(it) == key } }
                if (candidates.distinctBy { it.id }.singleOrNull()?.id == medication.id) routes.forEach { route ->
                    assertEquals("${medication.name}: $alias", medication.id, identities.resolve(alias, route.name))
                }
            }
        }
        val generic = event("Estradiol", route = AdministrationRoute.ORAL)
        val normalized = normalize(generic, null, DoseBasis.CHEMICAL_FORM)
        assertEquals("needs_formulation", normalized.excluded["1"])
    }

    @Test fun gelScreenshotCanBeCompletedWithActiveMassTemplateAndActualSite() {
        val record = event("Estradiol Gel")
        val id = requireNotNull(identities.resolve(record.substanceName, "TRANSDERMAL"))
        val product = HrtReferenceProducts.forMedication(id, catalog).single { it.unit == "g" && it.modelProfileId == "e2-gel006-reference-v1" }
        assertEquals(3.0, normalize(record, product, DoseBasis.CHEMICAL_FORM).events.single().doseMg!!, 1e-12)
        assertEquals("reference_application_site_mismatch", normalize(record, product, DoseBasis.CHEMICAL_FORM, "大腿").excluded["1"])
        assertEquals("select_reference_profile", normalize(record, null, DoseBasis.CHEMICAL_FORM).excluded["1"])
        assertEquals(3.0, record.dose!!, 0.0)
        assertEquals("mg", record.units)
    }

    @Test fun explicitGelMassAndPumpCountsUseTheChosenSpecificationOnly() {
        val gel = substances.single { it.name == "Estradiol Gel" }
        val templates = HrtReferenceProducts.forMedication(gel.id, catalog).filter { it.modelProfileId == "e2-gel006-reference-v1" }
        val grams = templates.single { it.unit == "g" && it.modelProfileId == "e2-gel006-reference-v1" }
        assertEquals(1.8, normalize(event("Estradiol Gel", unit = "g"), grams, DoseBasis.PRODUCT).events.single().doseMg!!, 1e-12)
        assertEquals(0.0018, normalize(event("Estradiol Gel"), grams, DoseBasis.PRODUCT).events.single().doseMg!!, 1e-12)
        assertEquals(2.25, normalize(event("Estradiol Gel", unit = "pump"), templates.single { it.unit == "pump" }, DoseBasis.PRODUCT).events.single().doseMg!!, 1e-12)
    }

    @Test fun oralE2CountSpecificationCanUseTheExistingRouteProfile() {
        val oral = substances.single { it.name == "Oral Estradiol" }
        val product = MedicationProduct("oral", oral.id, "user tablets", "form:oral-solid", "tablet", 2.0)
        val record = event("Oral Estradiol", 2.0, "tablet", AdministrationRoute.ORAL)
        assertEquals(4.0, normalize(record, product, DoseBasis.PRODUCT).events.single().doseMg!!, 1e-12)
    }

    @Test fun recognizedEstrogenWithIncompleteSpecRemainsRecognized() {
        val medication = substances.single { it.name == "Estradiol Gel" }
        val record = event(medication.name)
        val chart = HormoneChartService(catalog).calculate(HormoneJournalData(listOf(record), listOf(MedicationLink(1, medication.id, "identified", record.substanceName)), emptyList(), emptyList(), emptyList(), substances = substances), HormoneChartRequest(forecastDays = 0), 24 * 3600000L)
        assertEquals("雌激素", chart.recognizedHrt.single().category)
        assertEquals("gel_study_scenario", chart.recognizedHrt.single().modelReason)
        assertTrue(chart.includedActual.isEmpty())
    }

    @Test fun everyBundledEstrogenHasAReferenceAndCanNormalizeWithItsConfirmedFormulation() {
        val estrogens = substances.filter { "hrt-estrogens" in it.categories }
        assertEquals(13, estrogens.size)
        estrogens.forEach { medication ->
            val profile = requireNotNull(catalog.models.firstOrNull { it.acceptsMedication(medication.id) }) { medication.name }
            val patch = profile.route == HormoneRoute.PATCH
            val amount = profile.referenceDoseMg ?: when (profile.analyteId) { "EE" -> 0.03; "E4" -> 14.2; "E1", "EQUILIN" -> 0.625; else -> 1.0 }
            val record = event(medication.name, amount, if (patch) "patch" else "mg", AdministrationRoute.valueOf(profile.administrationRoutes.first()))
            val product = MedicationProduct("confirmed", medication.id, "confirmed reference", profile.formulationId, if (patch) "patch" else "mg", if (patch) null else 1.0, if (patch) 50.0 else null, modelProfileId = profile.id)
            val site = profile.allowedSites.firstOrNull() ?: if (patch) "腹部" else "上臂"
            val result = if (patch) HormoneRecordNormalizer.normalize(listOf(record), listOf(MedicationLink(1, medication.id, "identified", record.substanceName)), listOf(HrtEventDetail(1, site = site, patchRemovedAt = 24 * 3600000L)), listOf(EventProductSnapshot(product, 0, DoseBasis.PRODUCT).setting(1)), substances, catalog, null, 24.0, 48.0, analyteId = profile.analyteId)
                else normalize(record, product, DoseBasis.CHEMICAL_FORM, site, profile.analyteId)
            assertTrue("${medication.name}: ${result.excluded}", result.excluded.isEmpty())
            assertTrue(HormoneSimulationEngine().simulate(result.events, listOf(profile.parameters()), 0.0, 24.0, 24.0, 30).samples.any { it.pgMl > 0 })
        }
    }

    @Test fun eeCannotMixWithE2OrUseE2LaboratoryPoints() {
        val oral = substances.single { it.name == "Oral Estradiol" }
        val ee = substances.single { it.name == "Ethinylestradiol" }
        val e2Record = event(oral.name, 1.0, route = AdministrationRoute.ORAL)
        val eeRecord = event(ee.name, 0.03, route = AdministrationRoute.ORAL).copy(id = 2)
        val product = HrtReferenceProducts.forMedication(ee.id, catalog).single()
        val data = HormoneJournalData(listOf(e2Record, eeRecord), listOf(MedicationLink(1, oral.id, "identified", oral.name), MedicationLink(2, ee.id, "identified", ee.name)), emptyList(), listOf(EventProductSnapshot(product, 0, DoseBasis.CHEMICAL_FORM).setting(2)), emptyList(),
            listOf(HrtLab("e2", null, "E2", 0, 100.0, "pg/mL"), HrtLab("ee", null, "EE", 0, 100.0, "pg/mL")), substances)
        val chart = HormoneChartService(catalog).calculate(data, HormoneChartRequest(forecastDays = 0, analyteId = "EE"), 24 * 3600000L)
        assertEquals(listOf("2"), chart.includedActual)
        assertEquals(listOf("ee"), chart.measured.map { it.original.id })
        assertTrue(chart.excluded.isEmpty())
    }

    @Test fun sprayDoseBandsUseTheirOwnExposureAndRejectUnsupportedMultiples() {
        val spray = substances.single { it.name == "Estradiol Spray" }
        val product = HrtReferenceProducts.forMedication(spray.id, catalog).single()
        listOf(471.0, 736.0, 742.0).forEachIndexed { index, expected ->
            val result = normalize(event(spray.name, index + 1.0, "spray"), product, DoseBasis.PRODUCT, "前臂")
            val dose = result.events.single()
            val profile = catalog.models.single { it.id == dose.profileId }
            assertEquals(expected, dose.doseMg!! * profile.responseScale / profile.eliminationPerHour, .001)
        }
        assertEquals("unsupported_reference_dose", normalize(event(spray.name, 4.0, "spray"), product, DoseBasis.PRODUCT, "前臂").excluded["1"])
    }

    @Test fun e4HydrateMassIsConvertedOnlyWhenTheRecordedBasisIsExplicit() {
        val e4 = substances.single { it.name == "Estetrol" }
        val product = HrtReferenceProducts.forMedication(e4.id, catalog).single { it.unit == "mg" }
        val normalized = normalize(event(e4.name, 15.0, route = AdministrationRoute.ORAL), product, DoseBasis.PRODUCT, analyteId = "E4")
        assertEquals(14.2, normalized.events.single().doseMg!!, 1e-12)
    }
}
