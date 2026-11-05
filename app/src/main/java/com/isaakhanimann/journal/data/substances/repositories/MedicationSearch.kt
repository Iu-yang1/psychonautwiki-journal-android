package com.isaakhanimann.journal.data.substances.repositories

import com.isaakhanimann.journal.data.hrt.MedicationIdentity
import java.text.Collator
import java.util.Locale

data class MedicationSearchEntry(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val categories: Set<String> = emptySet(),
    val routes: Set<String> = emptySet(),
    val formulations: Set<String> = emptySet(),
    val referenceText: String = "",
    val sortName: String = name,
    val domains: Set<String> = categories.filter { it.startsWith("domain:") }.toSet(),
    val mechanisms: Set<String> = emptySet(),
    val keywords: List<String> = emptyList(),
)

data class MedicationSearchRequest(
    val text: String = "",
    val categories: Set<String> = emptySet(),
    val routes: Set<String> = emptySet(),
    val formulations: Set<String> = emptySet(),
    val includeReferences: Boolean = false,
    val sort: String = "default",
    val domains: Set<String> = emptySet(),
    val mechanisms: Set<String> = emptySet(),
)

data class MedicationSearchRanking(
    val favorites: Set<String> = emptySet(),
    val activePlans: Set<String> = emptySet(),
    val recentIds: List<String> = emptyList(),
)

object MedicationSearch {
    fun rank(entries: List<MedicationSearchEntry>, request: MedicationSearchRequest, ranking: MedicationSearchRanking = MedicationSearchRanking(), locale: Locale = Locale.getDefault()): List<String> {
        return MedicationSearchIndex(entries, locale).rank(request, ranking)
    }
}

/** Immutable catalog fields are prepared once; filter changes reuse names and collation keys. */
class MedicationSearchIndex(entries: List<MedicationSearchEntry>, locale: Locale = Locale.getDefault()) {
    private class IndexedEntry(val entry: MedicationSearchEntry, collator: Collator) {
        val name = MedicationIdentity.normalize(entry.name)
        val aliases = entry.aliases.map(MedicationIdentity::normalize).distinct()
        val keywords = entry.keywords.map(MedicationIdentity::normalize).distinct()
        val references by lazy(LazyThreadSafetyMode.PUBLICATION) { MedicationIdentity.normalize(entry.referenceText) }
        val sortKey = collator.getCollationKey(entry.sortName)
    }
    private val indexed = Collator.getInstance(locale).let { collator -> entries.distinctBy { it.id }.map { IndexedEntry(it, collator) } }

    fun rank(request: MedicationSearchRequest, ranking: MedicationSearchRanking = MedicationSearchRanking(), checkActive: () -> Unit = {}): List<String> {
        val query = MedicationIdentity.normalize(request.text)
        val recency = ranking.recentIds.withIndex().associate { it.value to it.index }
        val matched = indexed.mapNotNull { item ->
            checkActive()
            val entry = item.entry
            if (request.categories.isNotEmpty() && entry.categories.none { it in request.categories }) return@mapNotNull null
            if (request.domains.isNotEmpty() && entry.domains.none { it in request.domains }) return@mapNotNull null
            if (request.mechanisms.isNotEmpty() && entry.mechanisms.none { it in request.mechanisms }) return@mapNotNull null
            if (request.routes.isNotEmpty() && entry.routes.none { it in request.routes }) return@mapNotNull null
            if (request.formulations.isNotEmpty() && entry.formulations.none { it in request.formulations }) return@mapNotNull null
            val name = item.name
            val aliases = item.aliases
            val relevance = when {
                query.isEmpty() || name == query -> 0
                query in aliases -> 1
                name.startsWith(query) -> 2
                aliases.any { it.startsWith(query) } -> 3
                name.contains(query) || aliases.any { it.contains(query) } -> 4
                item.keywords.any { it == query || it.startsWith(query) } -> 5
                item.keywords.any { it.contains(query) } -> 6
                request.includeReferences && item.references.contains(query) -> 7
                else -> return@mapNotNull null
            }
            item to relevance
        }
        return matched.sortedWith { (indexedA, matchA), (indexedB, matchB) ->
            checkActive()
            val a = indexedA.entry
            val b = indexedB.entry
            var result = matchA.compareTo(matchB)
            if (result == 0 && request.sort == "default") {
                result = (b.id in ranking.favorites).compareTo(a.id in ranking.favorites)
                if (result == 0) result = (b.id in ranking.activePlans).compareTo(a.id in ranking.activePlans)
            }
            if (result == 0 && request.sort != "name") result = (recency[a.id] ?: Int.MAX_VALUE).compareTo(recency[b.id] ?: Int.MAX_VALUE)
            if (result == 0 && request.sort == "default") result = ("hrt-related" in b.categories).compareTo("hrt-related" in a.categories)
            if (result == 0) result = indexedA.sortKey.compareTo(indexedB.sortKey)
            if (result == 0) result = a.id.compareTo(b.id)
            result
        }.map { it.first.entry.id }
    }
}
