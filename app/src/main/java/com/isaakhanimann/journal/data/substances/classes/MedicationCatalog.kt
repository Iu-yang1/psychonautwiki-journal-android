package com.isaakhanimann.journal.data.substances.classes

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class MedicationCatalogIdentity(
    val id: String,
    val aliases: List<String> = emptyList(),
    val ingredientId: String? = null,
    val chemicalFormId: String? = null,
    val formulationId: String? = null,
    val sourceUrls: List<String> = emptyList(),
    val administrationRoutes: List<String> = emptyList(),
)

@Serializable
data class MedicationCatalogFile(val version: Int, val entries: List<MedicationCatalogIdentity>)

fun Substance.searchFormulations(): Set<String> = (listOfNotNull(identity?.formulationId) + timeCourse.mapNotNull { it.formulation }).toSet()

fun Substance.localizedSortName(locale: Locale = Locale.getDefault()): String =
    if (locale.language == "zh") commonNames.firstOrNull { name -> name.any { it in '\u4e00'..'\u9fff' } } ?: name else name

fun formulationLabel(value: String): String = when (value) {
    "form:oral-solid" -> "口服固体制剂"
    "form:oil-injection" -> "油剂注射剂"
    "form:depot-injection" -> "长效注射剂"
    "form:gel" -> "凝胶"
    "form:patch" -> "贴片"
    "form:spray" -> "喷雾"
    "form:implant" -> "植入剂"
    else -> value
}

fun mergeMedicationFiles(primary: SubstanceFile, baseline: SubstanceFile): SubstanceFile {
    val old = baseline.substances.associateBy { it.id }
    val merged = primary.substances.map { current ->
        val previous = old[current.id] ?: return@map current
        current.copy(commonNames = (current.commonNames + previous.commonNames + previous.name).filter { it != current.name }.distinct(),
            categories = (current.categories + previous.categories).distinct(),
            roas = current.roas.ifEmpty { previous.roas }, interactions = current.interactions ?: previous.interactions)
    }
    return SubstanceFile((primary.categories + baseline.categories).distinctBy { it.name },
        (merged + baseline.substances).distinctBy { it.id })
}
