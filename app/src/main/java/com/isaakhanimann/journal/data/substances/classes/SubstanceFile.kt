/*
 * Copyright (c) 2022. Isaak Hanimann.
 * This file is part of PsychonautWiki Journal.
 *
 * PsychonautWiki Journal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * PsychonautWiki Journal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PsychonautWiki Journal.  If not, see https://www.gnu.org/licenses/gpl-3.0.en.html.
 */

package com.isaakhanimann.journal.data.substances.classes

class SubstanceFile(
    val categories: List<Category>,
    val substances: List<Substance>
) {
    val substancesMap: Map<String, Substance> = substances.associateBy { it.name }
    val withCategories: List<SubstanceWithCategories> by lazy {
        val byName = categories.associateBy { it.name }
        substances.map { substance ->
            val matched = substance.categories.mapNotNull(byName::get)
            val visible = matched.filter { it.kind == null || it.kind == "category" }
                .distinctBy { it.displayName ?: it.name }
            SubstanceWithCategories(substance, visible.ifEmpty { matched.filter { it.kind == "domain" } })
        }
    }
    private val names by lazy { substances.groupBy { com.isaakhanimann.journal.data.hrt.MedicationIdentity.normalize(it.name) } }
    private val aliases by lazy {
        substances.flatMap { substance -> substance.commonNames.map { com.isaakhanimann.journal.data.hrt.MedicationIdentity.normalize(it) to substance } }
            .groupBy({ it.first }, { it.second }).mapValues { (_, items) -> items.distinctBy { it.id } }
    }
    fun find(name: String): Substance? {
        substancesMap[name]?.let { return it }
        val key = com.isaakhanimann.journal.data.hrt.MedicationIdentity.normalize(name)
        return (names[key] ?: aliases[key]).orEmpty().singleOrNull()
    }
}
