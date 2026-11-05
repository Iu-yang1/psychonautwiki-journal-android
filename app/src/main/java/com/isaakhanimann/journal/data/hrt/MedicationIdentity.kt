package com.isaakhanimann.journal.data.hrt

import java.text.Normalizer
import java.util.Locale

data class IdentityEntry(val id: String, val name: String, val aliases: List<String>, val routes: Set<String>)
object MedicationIdentity {
    private val separators = Regex("[\\s_-]+")
    fun normalize(value: String) = Normalizer.normalize(value, Normalizer.Form.NFKC).trim().lowercase(Locale.ROOT).replace(separators, "")
    fun resolve(name: String, route: String, catalog: List<IdentityEntry>, customNames: Set<String>): String? {
        return MedicationIdentityIndex(catalog, customNames).resolve(name, route)
    }
}

class MedicationIdentityIndex(catalog: List<IdentityEntry>, customNames: Set<String> = emptySet()) {
    private val custom = customNames.map(MedicationIdentity::normalize).toSet()
    private val canonical = catalog.groupBy { MedicationIdentity.normalize(it.name) }
    private val aliases = catalog.flatMap { entry -> entry.aliases.map { MedicationIdentity.normalize(it) to entry } }
        .groupBy({ it.first }, { it.second }).mapValues { (_, candidates) -> candidates.distinctBy { it.id } }

    fun resolve(name: String, route: String): String? {
        val key = MedicationIdentity.normalize(name)
        if (key in custom) return null
        val candidates = canonical[key] ?: aliases[key].orEmpty()
        // A route cannot disambiguate an otherwise ambiguous ingredient or brand name.
        return candidates.singleOrNull()?.takeIf { route in it.routes }?.id
    }
}
