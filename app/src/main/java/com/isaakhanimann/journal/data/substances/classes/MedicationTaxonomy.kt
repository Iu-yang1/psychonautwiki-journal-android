package com.isaakhanimann.journal.data.substances.classes

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

@Serializable
data class MedicationTaxonomyFile(val version: Int, val nodes: List<MedicationTaxonomyNode>)

@Serializable
data class MedicationTaxonomyNode(
    val id: String,
    val labelZh: String,
    val labelEn: String,
    val parentId: String? = null,
    val kind: String,
    val order: Int,
    val legacyIds: List<String> = emptyList(),
    val atcCodes: List<String> = emptyList(),
) {
    fun label(locale: Locale) = if (locale.language == "zh") labelZh else labelEn
}

class MedicationTaxonomy(file: MedicationTaxonomyFile) {
    val nodes = file.nodes.sortedBy { it.order }
    private val byId = nodes.associateBy { it.id }
    private val byLegacy = nodes.flatMap { node -> (node.legacyIds + node.id).map { it to node } }.toMap()

    init {
        require(file.version == 1 && nodes.size == byId.size)
        require(nodes.all { it.parentId == null || it.parentId in byId })
        require(nodes.flatMap { it.legacyIds + it.id }.distinct().size == nodes.sumOf { it.legacyIds.size + 1 })
        nodes.forEach { node ->
            val seen = mutableSetOf(node.id)
            var parent = node.parentId
            while (parent != null) {
                require(seen.add(parent)) { "Taxonomy cycle at ${node.id}" }
                parent = byId.getValue(parent).parentId
            }
        }
    }

    fun node(id: String) = byLegacy[id]

    fun tags(categories: Collection<String>): Set<String> = buildSet {
        addAll(categories)
        categories.forEach { category ->
            var node = byLegacy[category]
            while (node != null) {
                add(node.id)
                node = node.parentId?.let(byId::get)
            }
        }
        if (none { byId[it]?.kind == "domain" }) add("domain:other")
    }

    fun apply(file: SubstanceFile, locale: Locale): SubstanceFile {
        val categories = file.categories.map { category ->
            val node = node(category.name)
            category.copy(description = node?.label(locale) ?: category.description,
                displayName = node?.label(locale), kind = node?.kind ?: "attribute")
        } + nodes.map { node ->
            Category(node.id, node.label(locale), null, Color(0xFF9575CD), node.label(locale), node.kind)
        }
        return SubstanceFile(categories.distinctBy { it.name }, file.substances.map { sub ->
            sub.copy(categories = tags(sub.categories).toList())
        })
    }

    companion object {
        fun parse(text: String) = MedicationTaxonomy(Json.decodeFromString<MedicationTaxonomyFile>(text))
    }
}
