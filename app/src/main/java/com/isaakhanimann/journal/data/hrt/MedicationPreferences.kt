package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.data.room.experiences.entities.SubstanceCompanion

object MedicationPreferences {
    fun sameIdentity(a: String, b: String, customNames: Set<String>, resolve: (String) -> String?): Boolean {
        if (a == b) return true
        val custom = customNames.map(MedicationIdentity::normalize).toSet()
        if (MedicationIdentity.normalize(a) in custom || MedicationIdentity.normalize(b) in custom) return false
        val id = resolve(a) ?: return false
        return resolve(b) == id
    }

    fun color(name: String, companions: List<SubstanceCompanion>, customNames: Set<String>, resolve: (String) -> String?): SubstanceCompanion? {
        companions.singleOrNull { it.substanceName == name }?.let { return it }
        val candidates = companions.filter { sameIdentity(name, it.substanceName, customNames, resolve) }
        return if (candidates.map { it.color to it.customColor }.distinct().size == 1) candidates.firstOrNull() else null
    }
}
