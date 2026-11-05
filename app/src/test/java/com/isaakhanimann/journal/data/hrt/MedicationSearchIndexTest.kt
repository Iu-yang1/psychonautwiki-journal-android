package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.data.substances.classes.*
import com.isaakhanimann.journal.data.substances.parse.SubstanceParser
import com.isaakhanimann.journal.data.substances.repositories.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale
import kotlin.system.measureNanoTime

class MedicationSearchIndexTest {
    @Test fun filterAndReferenceSwitchesDoNotChangeMatchingOrRanking() {
        val entries = listOf(
            MedicationSearchEntry("oral", "Oral E2", listOf("补佳乐"), routes = setOf("ORAL"), formulations = setOf("tablet"), referenceText = "potassium monitoring"),
            MedicationSearchEntry("gel", "Estradiol Gel", listOf("E2"), routes = setOf("TRANSDERMAL"), formulations = setOf("gel")),
        )
        val index = MedicationSearchIndex(entries)
        repeat(20) {
            assertEquals(listOf("gel"), index.rank(MedicationSearchRequest(routes = setOf("TRANSDERMAL"), formulations = setOf("gel"))))
            assertTrue(index.rank(MedicationSearchRequest("potassium")).isEmpty())
            assertEquals(listOf("oral"), index.rank(MedicationSearchRequest("potassium", includeReferences = true)))
            assertEquals(listOf("oral"), index.rank(MedicationSearchRequest("补佳乐")))
            assertEquals(listOf("gel", "oral"), index.rank(MedicationSearchRequest(), MedicationSearchRanking(favorites = setOf("gel"))))
        }
    }

    @Test(expected = CancellationException::class) fun outdatedCpuSearchCanBeCancelled() {
        MedicationSearchIndex(listOf(MedicationSearchEntry("a", "A"))).rank(MedicationSearchRequest()) { throw CancellationException("newer request") }
    }

    @Test fun actualCatalogReportsCostOfRebuildingVersusReusingTheIndex() {
        fun raw(name: String) = requireNotNull(javaClass.classLoader!!.getResourceAsStream(name)).bufferedReader().use { it.readText() }
        val parser = SubstanceParser()
        val identities = Json.decodeFromString<MedicationCatalogFile>(raw("medication_catalog.json")).entries.associateBy { it.id }
        val entries = mergeMedicationFiles(parser.parseSubstanceFile(raw("substances.json")), parser.parseSubstanceFile(raw("substances_base.json"))).withHrtTaxonomy().substances.map { original ->
            val sub = original.withCatalogIdentity(identities[original.id])
            MedicationSearchEntry(sub.id, sub.name, sub.commonNames, sub.categories.toSet(), sub.availableRoutesForIngestion().map { it.name }.toSet(), sub.searchFormulations(), sub.referenceSearchText(), sub.localizedSortName(Locale.ENGLISH), keywords = sub.searchTerms)
        }
        assertEquals(595, entries.size)
        val trihexyphenidyl = entries.single { it.name == "Trihexyphenidyl Hydrochloride" }
        assertEquals(listOf(trihexyphenidyl.id), MedicationSearchIndex(entries).rank(MedicationSearchRequest("盐酸苯海索")))
        assertEquals(setOf("ORAL"), trihexyphenidyl.routes)
        val requests = listOf(
            MedicationSearchRequest(routes = setOf("ORAL")),
            MedicationSearchRequest(formulations = setOf("form:gel")),
            MedicationSearchRequest("monitoring", includeReferences = true),
            MedicationSearchRequest(includeReferences = true),
        )
        val index = MedicationSearchIndex(entries, Locale.ENGLISH)
        requests.forEach { request -> assertEquals(MedicationSearch.rank(entries, request, locale = Locale.ENGLISH), index.rank(request)) }
        repeat(5) { requests.forEach { index.rank(it) } }
        val repeatCount = 10
        val rebuild = measureNanoTime { repeat(repeatCount) { requests.forEach { MedicationSearch.rank(entries, it, locale = Locale.ENGLISH) } } }
        val reuse = measureNanoTime { repeat(repeatCount) { requests.forEach { index.rank(it) } } }
        println("SEARCH_INDEX_595: rebuild=%.3f ms/query, reuse=%.3f ms/query; JVM microbenchmark, not device frame timing".format(rebuild / (repeatCount * requests.size * 1e6), reuse / (repeatCount * requests.size * 1e6)))
    }
}
