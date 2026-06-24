/*
 * Copyright (c) 2023. Isaak Hanimann.
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

import com.isaakhanimann.journal.data.substances.classes.SubstanceWithCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    val substanceRepo: SubstanceRepository
) : SearchRepositoryInterface {

    override fun getMatchingSubstances(
        searchText: String,
        filterCategories: List<String>,
        recentlyUsedSubstanceNamesSorted: List<String>,
    ): List<SubstanceWithCategories> {
        val substancesMatchingCategories = getSubstancesMatchingCategories(filterCategories)
        val substancesFilteredWithText = getSubstancesMatchingSearchText(searchText, prefilteredSubstances = substancesMatchingCategories)
        return getSubstancesSorted(prefilteredSubstances = substancesFilteredWithText, recentlyUsedSubstanceNamesSorted = recentlyUsedSubstanceNamesSorted)
    }

    // --- NEW METHOD ADDED ---
    /**
     * A reactive version of getMatchingSubstances that returns a Flow.
     * This is ideal for observing substance data changes in a UI.
     * Note: This implementation does not include category filtering or recent-based sorting
     * for simplicity, as it's primarily used for the substance selector which needs all substances.
     *
     * @param searchText The text to filter substances by.
     * @return A Flow emitting the list of matching substances.
     */
    fun getMatchingSubstancesFlow(searchText: String): Flow<List<SubstanceWithCategories>> {
        // Assumption: substanceRepo provides a method to get all substances as a Flow.
        // This is a common pattern with Room DB.
        val allSubstancesFlow = substanceRepo.getAllSubstancesWithCategoriesFlow()

        return allSubstancesFlow.map { allSubstances ->
            // Reuse the existing text filtering logic within the flow's map operator.
            getSubstancesMatchingSearchText(searchText, allSubstances)
        }
    }

    private fun getSubstancesMatchingCategories(filterCategories: List<String>): List<SubstanceWithCategories> {
        return substanceRepo.getAllSubstancesWithCategories().filter { substanceWithCategories ->
            filterCategories.all { substanceWithCategories.substance.categories.contains(it) }
        }
    }

    private fun getSubstancesMatchingSearchText(searchText: String, prefilteredSubstances: List<SubstanceWithCategories>): List<SubstanceWithCategories> {
        return if (searchText.isEmpty()) {
            prefilteredSubstances
        } else {
            val searchString = searchText.normalizedForSearch()
            // substances whose primary name begins with the search string
            val mainPrefixMatches = prefilteredSubstances.filter { substanceWithCategories ->
                substanceWithCategories.substance.name.normalizedForSearch().startsWith(
                    prefix = searchString, ignoreCase = true
                )
            }
            // substances with any searchable field beginning with the search string
            val prefixMatches = prefilteredSubstances.filter { substanceWithCategories ->
                substanceWithCategories.searchableTerms().any { term ->
                    term.normalizedForSearch().startsWith(
                        prefix = searchString, ignoreCase = true
                    )
                }
            }
            // substances containing the search string in any searchable field
            val matches = prefilteredSubstances.filter { substanceWithCategories ->
                substanceWithCategories.searchableTerms().any { term ->
                    term.normalizedForSearch().contains(
                        other = searchString, ignoreCase = true
                    )
                }
            }
            return (mainPrefixMatches + prefixMatches + matches).distinctBy { it.substance.name }
        }
    }

    private fun getSubstancesSorted(
        prefilteredSubstances: List<SubstanceWithCategories>,
        recentlyUsedSubstanceNamesSorted: List<String>
    ): List<SubstanceWithCategories> {
        val recentNames = recentlyUsedSubstanceNamesSorted.distinct()
        val recentlyUsedMatches =
            recentNames.filter { recent -> prefilteredSubstances.any { it.substance.name == recent } }
                .mapNotNull {
                    substanceRepo.getSubstanceWithCategories(
                        substanceName = it
                    )
                }
        val commonSubstanceMatches =
            prefilteredSubstances.filter { sub -> sub.categories.any { cat -> cat.name == "common" } }
        return (recentlyUsedMatches + commonSubstanceMatches + prefilteredSubstances).distinctBy { it.substance.name }
    }

    private fun SubstanceWithCategories.searchableTerms(): List<String> {
        val currentSubstance = substance
        val clinicalInfo = currentSubstance.clinicalInfo
        val tdm = currentSubstance.tdm
        val endocrineInfo = currentSubstance.endocrineInfo
        val hrtModelInfo = currentSubstance.hrtModelInfo
        return currentSubstance.commonNames +
            currentSubstance.name +
            currentSubstance.categories +
            categories.map { it.name } +
            clinicalInfo?.atcCodes.orEmpty() +
            clinicalInfo?.drugClass.orEmpty() +
            clinicalInfo?.indications.orEmpty() +
            clinicalInfo?.monitoring.orEmpty() +
            listOfNotNull(tdm?.monitoringType, tdm?.reason, tdm?.specimen, tdm?.samplingTime, tdm?.assayMethod) +
            tdm?.analytes.orEmpty() +
            tdm?.therapeuticRanges.orEmpty().flatMap { range ->
                listOfNotNull(range.indication, range.range, range.unit, range.note)
            } +
            tdm?.toxicityThresholds.orEmpty().flatMap { threshold ->
                listOfNotNull(threshold.threshold, threshold.unit, threshold.note)
            } +
            tdm?.criticalValues.orEmpty().flatMap { threshold ->
                listOfNotNull(threshold.threshold, threshold.unit, threshold.note)
            } +
            tdm?.interpretationCaveats.orEmpty() +
            currentSubstance.timeCourse.flatMap { timeCourse ->
                listOfNotNull(timeCourse.route, timeCourse.formulation) +
                    listOf("Tmax", "half-life").filter { term ->
                        when (term) {
                            "Tmax" -> timeCourse.tmax != null
                            else -> timeCourse.eliminationHalfLife != null
                        }
                    } +
                    timeCourse.notes
            } +
            endocrineInfo?.hormoneClass.orEmpty() +
            endocrineInfo?.mechanisms.orEmpty() +
            endocrineInfo?.affectedHormones.orEmpty() +
            endocrineInfo?.monitoringLabs.orEmpty() +
            endocrineInfo?.assayCaveats.orEmpty() +
            endocrineInfo?.safetySignals.orEmpty() +
            endocrineInfo?.modelRoles.orEmpty() +
            currentSubstance.doseUseReferences.flatMap { reference ->
                listOfNotNull(
                    reference.indication,
                    reference.population,
                    reference.route,
                    reference.formulation,
                    reference.amountText,
                    reference.scheduleText,
                    reference.sourceType,
                    reference.evidenceLevel,
                    reference.note
                ) +
                    reference.ranges.flatMap { range ->
                        listOfNotNull(
                            range.unit,
                            range.basis,
                            range.frequency,
                            range.rangeKind,
                            range.label,
                            range.note
                        ) + range.components.flatMap { component ->
                            listOf(component.substance, component.unit)
                        }
                    } +
                    reference.sourceRefs.flatMap { source ->
                        listOf(source.title, source.sourceType)
                    }
            } +
            hrtModelInfo?.modelRoles.orEmpty() +
            hrtModelInfo?.primaryModeledAnalytes.orEmpty() +
            hrtModelInfo?.requiredEventFields.orEmpty() +
            hrtModelInfo?.requiredLabFields.orEmpty() +
            hrtModelInfo?.caveats.orEmpty()
    }

    private fun String.normalizedForSearch(): String {
        return replace(Regex("[- ]"), "")
    }
}
