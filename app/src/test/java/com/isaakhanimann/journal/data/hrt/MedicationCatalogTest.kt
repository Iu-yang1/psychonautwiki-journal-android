package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.classes.*
import org.junit.Assert.*
import org.junit.Test

class MedicationCatalogTest {
    private fun substance(id: String, name: String, aliases: List<String> = emptyList(), courses: List<TimeCourse> = emptyList()) = Substance(
        name, aliases, "https://example.test", false, null, emptyList(), null, emptyList(), emptyList(), null, null, null, null, null,
        emptyList(), null, emptyList(), timeCourse = courses, id = id)

    @Test fun clinicalAndLegacyPacksMergeByStableIdWithoutDroppingOldEntries() {
        val current = substance("same", "Current", listOf("new alias"))
        val old = substance("same", "Old", listOf("old alias"))
        val legacyOnly = substance("legacy", "Legacy")
        val merged = mergeMedicationFiles(SubstanceFile(emptyList(), listOf(current)), SubstanceFile(emptyList(), listOf(old, legacyOnly)))
        assertEquals(listOf("same", "legacy"), merged.substances.map { it.id })
        assertEquals("Current", merged.substances.first().name)
        assertTrue(merged.substances.first().commonNames.containsAll(listOf("new alias", "old alias", "Old")))
    }

    @Test fun hrtRoutesCanComeFromTimeCourseWithoutRecreationalDoseEntries() {
        val injectable = substance("ev", "EV injection", courses = listOf(TimeCourse("intramuscular or subcutaneous", formulation = "oil depot injection")))
        assertEquals(setOf(AdministrationRoute.INTRAMUSCULAR, AdministrationRoute.SUBCUTANEOUS), injectable.availableRoutesForIngestion().toSet())
    }

    @Test fun abbreviationsDoNotMatchInsideUnrelatedWords() {
        assertFalse(AdministrationRoute.ORAL in TimeCourse("depot").matchingAdministrationRoutes())
        assertFalse(AdministrationRoute.INTRAMUSCULAR in TimeCourse("implant").matchingAdministrationRoutes())
        assertTrue(AdministrationRoute.SUBLINGUAL in TimeCourse("sublingual/buccal").matchingAdministrationRoutes())
        assertTrue(AdministrationRoute.BUCCAL in TimeCourse("sublingual/buccal").matchingAdministrationRoutes())
    }
}
