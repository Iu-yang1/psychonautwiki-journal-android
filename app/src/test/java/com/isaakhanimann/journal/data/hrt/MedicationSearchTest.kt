package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.data.substances.repositories.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class MedicationSearchTest {
    @Test fun exactAliasCannotBeOverriddenByFavoritesOrRecency() {
        val entries = listOf(MedicationSearchEntry("prefix", "EV Injection Extra"), MedicationSearchEntry("exact", "Estradiol Valerate", listOf("EV")))
        val ranking = MedicationSearchRanking(favorites = setOf("prefix"), recentIds = listOf("prefix"))
        assertEquals(listOf("exact", "prefix"), MedicationSearch.rank(entries, MedicationSearchRequest("ev"), ranking))
    }

    @Test fun defaultRankingFollowsFavoriteActiveRecentHrtThenName() {
        val entries = listOf("other", "hrt", "recent", "active", "favorite").map { id -> MedicationSearchEntry(id, id, categories = if (id == "hrt") setOf("hrt-related") else emptySet()) }
        val ranking = MedicationSearchRanking(setOf("favorite"), setOf("active"), listOf("recent"))
        assertEquals(listOf("favorite", "active", "recent", "hrt", "other"), MedicationSearch.rank(entries, MedicationSearchRequest(), ranking))
    }

    @Test fun referenceTextRequiresExplicitOptIn() {
        val entries = listOf(MedicationSearchEntry("a", "Medication", referenceText = "potassium monitoring"))
        assertTrue(MedicationSearch.rank(entries, MedicationSearchRequest("potassium")).isEmpty())
        assertEquals(listOf("a"), MedicationSearch.rank(entries, MedicationSearchRequest("potassium", includeReferences = true)))
    }

    @Test fun routeAndFormAreIndependentDimensions() {
        val entries = listOf(MedicationSearchEntry("a", "A", categories = setOf("hrt"), routes = setOf("ORAL"), formulations = setOf("tablet")), MedicationSearchEntry("b", "B", categories = setOf("hrt"), routes = setOf("TRANSDERMAL"), formulations = setOf("gel")))
        assertEquals(listOf("a"), MedicationSearch.rank(entries, MedicationSearchRequest(categories = setOf("hrt"), routes = setOf("ORAL"), formulations = setOf("tablet"))))
        assertTrue(MedicationSearch.rank(entries, MedicationSearchRequest(routes = setOf("ORAL"), formulations = setOf("gel"))).isEmpty())
    }

    @Test fun unicodeNormalizationAndStableIdDeduplication() {
        val entry = MedicationSearchEntry("one", "Ｅ－Ｖ")
        assertEquals(listOf("one"), MedicationSearch.rank(listOf(entry, entry), MedicationSearchRequest("e_v")))
    }

    @Test fun nameModeUsesLocalizedNameAndRecentModeDoesNotPromoteHrt() {
        val entries = listOf(MedicationSearchEntry("a", "Zulu", sortName = "Alpha"), MedicationSearchEntry("b", "Alpha", categories = setOf("hrt-related"), sortName = "Zulu"))
        val ranking = MedicationSearchRanking(setOf("b"), setOf("b"), listOf("b"))
        assertEquals(listOf("a", "b"), MedicationSearch.rank(entries, MedicationSearchRequest(sort = "name"), ranking, Locale.ENGLISH))
        assertEquals(listOf("b", "a"), MedicationSearch.rank(entries, MedicationSearchRequest(sort = "recent"), ranking))
    }
}
