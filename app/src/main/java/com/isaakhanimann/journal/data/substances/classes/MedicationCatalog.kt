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
    val searchTerms: List<String> = emptyList(),
    val excludedAliases: List<String> = emptyList(),
)

@Serializable
data class MedicationCatalogFile(val version: Int, val entries: List<MedicationCatalogIdentity>)

fun Substance.withCatalogIdentity(catalogIdentity: MedicationCatalogIdentity?): Substance {
    require(catalogIdentity == null || catalogIdentity.id == id)
    val excluded = catalogIdentity?.excludedAliases.orEmpty()
        .map(com.isaakhanimann.journal.data.hrt.MedicationIdentity::normalize).toSet()
    return copy(identity = catalogIdentity,
        commonNames = (commonNames + catalogIdentity?.aliases.orEmpty()).distinct()
            .filter { com.isaakhanimann.journal.data.hrt.MedicationIdentity.normalize(it) !in excluded },
        searchTerms = (searchTerms + catalogIdentity?.searchTerms.orEmpty()).distinct())
}

fun Substance.searchFormulations(): Set<String> = (listOfNotNull(identity?.formulationId) + formulationIds).toSet()

fun Substance.localizedSortName(locale: Locale = Locale.getDefault()): String =
    if (displayName != name) displayName else if (locale.language == "zh") commonNames.firstOrNull { name -> name.any { it in '\u4e00'..'\u9fff' } } ?: name else name

fun formulationLabel(value: String): String = when (value) {
    "form:oral-solid" -> "口服固体制剂"
    "form:tablet" -> "片剂"
    "form:capsule" -> "胶囊剂"
    "form:oral-liquid" -> "口服液体制剂"
    "form:injection" -> "注射剂"
    "form:powder" -> "散剂 / 粉剂"
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
        current.copy(
            commonNames = if ("commonNames" in current.reviewedFields) current.commonNames else
                (current.commonNames + previous.commonNames + previous.name).filter { it != current.name }.distinct(),
            categories = if ("categories" in current.reviewedFields) current.categories else
                (current.categories + previous.categories).distinct(),
            roas = if ("roas" in current.reviewedFields) current.roas else current.roas.ifEmpty { previous.roas },
            interactions = if ("interactions" in current.reviewedFields) current.interactions else current.interactions ?: previous.interactions
        )
    }
    return SubstanceFile((primary.categories + baseline.categories).distinctBy { it.name },
        (merged + baseline.substances).distinctBy { it.id })
}
