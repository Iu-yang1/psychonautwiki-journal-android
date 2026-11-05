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

package com.isaakhanimann.journal.data.substances.repositories

import com.isaakhanimann.journal.data.hrt.withHrtTaxonomy
import com.isaakhanimann.journal.data.substances.classes.MedicationCatalogFile
import com.isaakhanimann.journal.data.substances.classes.mergeMedicationFiles
import kotlinx.serialization.json.Json
import android.content.Context
import com.isaakhanimann.journal.R
import com.isaakhanimann.journal.data.substances.classes.Category
import com.isaakhanimann.journal.data.substances.classes.Substance
import com.isaakhanimann.journal.data.substances.classes.SubstanceFile
import com.isaakhanimann.journal.data.substances.classes.SubstanceWithCategories
import com.isaakhanimann.journal.data.substances.classes.MedicationTaxonomy
import com.isaakhanimann.journal.data.substances.parse.SubstanceParserInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubstanceRepository @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val substanceParser: SubstanceParserInterface,
) : SubstanceRepositoryInterface {

    val taxonomy = appContext.resources.openRawResource(R.raw.medication_taxonomy).bufferedReader().use { MedicationTaxonomy.parse(it.readText()) }

    private val _substanceFileFlow = MutableStateFlow(parseSubstanceFile())

    fun refreshSubstance() {
        _substanceFileFlow.value = parseSubstanceFile()
    }

    private fun parseSubstanceFile(): SubstanceFile {
        val primary = substanceParser.parseSubstanceFile(string = getAssetsSubstanceFileContent())
        val base = appContext.resources.openRawResource(R.raw.substances_base).bufferedReader().use { substanceParser.parseSubstanceFile(it.readText()) }
        val catalog = appContext.resources.openRawResource(R.raw.medication_catalog).bufferedReader().use { Json.decodeFromString<MedicationCatalogFile>(it.readText()) }
        require(catalog.version == 1 && catalog.entries.map { it.id }.distinct().size == catalog.entries.size)
        val identities = catalog.entries.associateBy { it.id }
        val combined = taxonomy.apply(mergeMedicationFiles(primary, base).withHrtTaxonomy(), appContext.resources.configuration.locales[0])
        return SubstanceFile(combined.categories, combined.substances.map { substance ->
            val identity = identities[substance.id]
            substance.copy(identity = identity, commonNames = (substance.commonNames + identity?.aliases.orEmpty()).distinct())
        })
    }

    private fun getAssetsSubstanceFileContent(): String {
        return appContext.resources.openRawResource(R.raw.substances).bufferedReader().use { it.readText() }
    }

    override fun getAllSubstances(): List<Substance> {
        return _substanceFileFlow.value.substances
    }

    override fun getAllSubstancesFlow(): Flow<List<Substance>> {
        return _substanceFileFlow.map {
            it.substances
        }
    }

    override fun getAllSubstancesWithCategories(): List<SubstanceWithCategories> {
        return _substanceFileFlow.value.withCategories
    }

    fun getAllSubstancesWithCategoriesFlow(): Flow<List<SubstanceWithCategories>> {
        return _substanceFileFlow.map { it.withCategories }
    }

    override fun getAllCategories(): List<Category> {
        return _substanceFileFlow.value.categories
    }

    override fun getSubstance(substanceName: String): Substance? {
        return _substanceFileFlow.value.find(substanceName)
    }

    override fun getCategory(categoryName: String): Category? {
        return _substanceFileFlow.value.categories.firstOrNull { it.name == categoryName }
    }

    override fun getSubstanceWithCategories(substanceName: String): SubstanceWithCategories? {
        val substance = getSubstance(substanceName) ?: return null
        return _substanceFileFlow.value.withCategories.firstOrNull { it.substance.id == substance.id }
    }
}
