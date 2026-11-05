package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.data.substances.classes.*
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.parse.SubstanceParser
import com.isaakhanimann.journal.data.substances.repositories.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class MedicationLibraryTest {
    private fun raw(name: String) = requireNotNull(javaClass.classLoader!!.getResourceAsStream(name)).bufferedReader().use { it.readText() }
    private val taxonomy get() = MedicationTaxonomy.parse(raw("medication_taxonomy.json"))
    private val parser = SubstanceParser()

    @Test fun reviewedEmptyFieldsCannotBeResurrectedFromTheBaseline() {
        val original = parser.parseSubstanceFile(raw("substances.json")).substances.first()
        val baseline = original.copy(commonNames = listOf("wrong alias"), categories = listOf("wrong-category"))
        val primary = original.copy(commonNames = emptyList(), categories = emptyList(), roas = emptyList(), interactions = null,
            reviewedFields = setOf("commonNames", "categories", "roas", "interactions"))
        val result = mergeMedicationFiles(SubstanceFile(emptyList(), listOf(primary)), SubstanceFile(emptyList(), listOf(baseline))).substances.single()
        assertTrue(result.commonNames.isEmpty())
        assertTrue(result.categories.isEmpty())
        assertTrue(result.roas.isEmpty())
        assertNull(result.interactions)
        assertEquals(original.id, result.id)
        assertEquals(original.name, result.name)
    }

    @Test fun unreviewedMissingRoutesStillUseLegacyFallback() {
        val original = parser.parseSubstanceFile(raw("substances.json")).substances.first { it.roas.isNotEmpty() }
        val primary = original.copy(roas = emptyList())
        assertEquals(original.roas, mergeMedicationFiles(SubstanceFile(emptyList(), listOf(primary)), SubstanceFile(emptyList(), listOf(original))).substances.single().roas)
    }

    @Test fun allIdentitiesHaveADomainWithoutTurningEndocrineIntoAnHrtIndication() {
        val file = mergeMedicationFiles(parser.parseSubstanceFile(raw("substances.json")), parser.parseSubstanceFile(raw("substances_base.json"))).withHrtTaxonomy()
        val classified = taxonomy.apply(file, Locale.SIMPLIFIED_CHINESE)
        assertEquals(595, classified.substances.size)
        assertTrue(classified.substances.all { sub -> sub.categories.any { it.startsWith("domain:") } })
        assertFalse("hrt-related" in taxonomy.tags(listOf("endocrine")))
        assertEquals("抗血栓药", taxonomy.node("atc-b01")!!.label(Locale.SIMPLIFIED_CHINESE))
        assertEquals("hrt-antiandrogens", taxonomy.node("antiandrogen")!!.id)
    }

    @Test fun equivalentOldCategoryTagsReturnTheirUnion() {
        val entries = listOf("atc-b01", "antithrombotic").mapIndexed { i, tag ->
            MedicationSearchEntry(i.toString(), "Drug $i", categories = taxonomy.tags(listOf(tag)))
        }
        val selected = taxonomy.node("atc-b01")!!.id
        assertEquals(listOf("0", "1"), MedicationSearch.rank(entries, MedicationSearchRequest(categories = setOf(selected))))
    }

    @Test fun categoriesAreOrWithinTheFacetAndAndAcrossIndependentFacets() {
        val entries = listOf(
            MedicationSearchEntry("a", "A", categories = setOf("one"), routes = setOf("ORAL"), domains = setOf("domain:test"), mechanisms = setOf("mechanism:a")),
            MedicationSearchEntry("b", "B", categories = setOf("two"), routes = setOf("ORAL"), domains = setOf("domain:test")),
            MedicationSearchEntry("c", "C", categories = setOf("one"), routes = setOf("TRANSDERMAL"), domains = setOf("domain:other")),
        )
        val request = MedicationSearchRequest(categories = setOf("one", "two"), routes = setOf("ORAL"), domains = setOf("domain:test"))
        assertEquals(listOf("a", "b"), MedicationSearch.rank(entries, request))
        assertEquals(listOf("a"), MedicationSearch.rank(entries, request.copy(mechanisms = setOf("mechanism:a"))))
    }

    @Test fun localizedDisplayNameDoesNotRenameWhatNavigationAndRecordsUse() {
        val original = parser.parseSubstanceFile(raw("substances.json")).substances.first()
        val translated = original.copy(displayName = "中文显示名")
        val model = SubstanceWithCategories(translated, emptyList()).toSubstanceModel()
        assertEquals(original.name, model.name)
        assertEquals("中文显示名", model.displayName)
        assertEquals("中文显示名", translated.localizedSortName(Locale.SIMPLIFIED_CHINESE))
    }

    @Test fun reviewedClinicalRecordingUnitsDoNotInventRecreationalDoseTiersOrOtherRoutes() {
        val bicalutamide = parser.parseSubstanceFile(raw("substances.json")).substances.single { it.name == "Bicalutamide" }
        assertEquals("mg", bicalutamide.recordingUnit(AdministrationRoute.ORAL))
        assertNull(bicalutamide.getRoa(AdministrationRoute.ORAL)?.roaDose)
        assertEquals("", bicalutamide.recordingUnit(AdministrationRoute.TRANSDERMAL))
        assertEquals(setOf("form:tablet"), bicalutamide.searchFormulations())
        assertFalse(bicalutamide.searchFormulations().any { it.contains("tablets", ignoreCase = true) })
        val translated = bicalutamide.copy(endocrineInfo = bicalutamide.endocrineInfo?.copy(hormoneClass = listOf("非甾体抗雄激素")))
        val classified = SubstanceFile(emptyList(), listOf(translated)).withHrtTaxonomy().substances.single()
        assertTrue("hrt-antiandrogens" in classified.categories)
        assertFalse("hrt-other" in classified.categories)
    }

    @Test fun classKeywordsRemainSearchableButCannotBecomeIngestionIdentitiesAfterFallback() {
        val original = parser.parseSubstanceFile(raw("substances.json")).substances.first()
        val attached = original.copy(commonNames = listOf("ACE inhibitor"))
            .withCatalogIdentity(MedicationCatalogIdentity(original.id, listOf(original.name),
                searchTerms = listOf("ACE inhibitor"), excludedAliases = listOf("ACE inhibitor")))
        assertFalse("ACE inhibitor" in attached.commonNames)
        assertTrue("ACE inhibitor" in attached.searchTerms)
        val index = MedicationIdentityIndex(listOf(IdentityEntry(attached.id, attached.name, attached.commonNames, setOf("ORAL"))))
        assertNull(index.resolve("ACE inhibitor", "ORAL"))
        val search = MedicationSearchEntry(attached.id, attached.name, attached.commonNames, keywords = attached.searchTerms)
        assertEquals(listOf(attached.id), MedicationSearch.rank(listOf(search), MedicationSearchRequest("ACE inhibitor")))
    }

    @Test fun reviewedClassificationDoesNotInferHormoneUseFromClinicalProse() {
        val original = parser.parseSubstanceFile(raw("substances.json")).substances.first()
        val reviewed = original.copy(categories = listOf("cardiovascular"), reviewedFields = setOf("categories"),
            endocrineInfo = EndocrineInfo(hormoneClass = listOf("Estrogen receptor antagonist")))
        val result = SubstanceFile(emptyList(), listOf(reviewed)).withHrtTaxonomy().substances.single()
        assertFalse("hrt-estrogens" in result.categories)
        assertFalse("hrt-related" in result.categories)
    }
}
