/*
 * Copyright (c) 2026. Isaak Hanimann.
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

data class SourceRef(
    val title: String,
    val url: String,
    val sourceType: String,
    val accessedDate: String
)

data class TimeValue(
    val min: Double?,
    val max: Double?,
    val unit: String,
    val basis: String? = null,
    val note: String? = null
)

data class TimeCourse(
    val route: String,
    val formulation: String? = null,
    val onset: TimeValue? = null,
    val tmax: TimeValue? = null,
    val peakEffect: TimeValue? = null,
    val durationOfAction: TimeValue? = null,
    val eliminationHalfLife: TimeValue? = null,
    val timeToSteadyState: TimeValue? = null,
    val washout: TimeValue? = null,
    val notes: List<String> = emptyList(),
    val sourceRefs: List<SourceRef> = emptyList()
)

data class ClinicalInfo(
    val atcCodes: List<String> = emptyList(),
    val drugClass: List<String> = emptyList(),
    val indications: List<String> = emptyList(),
    val contraindications: List<String> = emptyList(),
    val majorWarnings: List<String> = emptyList(),
    val majorInteractions: List<String> = emptyList(),
    val monitoring: List<String> = emptyList(),
    val sourceRefs: List<SourceRef> = emptyList()
)

data class TherapeuticRange(
    val indication: String? = null,
    val range: String,
    val unit: String,
    val note: String? = null
)

data class ToxicityThreshold(
    val threshold: String,
    val unit: String,
    val note: String? = null
)

data class TherapeuticDrugMonitoring(
    val isRoutinelyMonitored: Boolean,
    val monitoringType: String,
    val reason: String? = null,
    val pharmacokineticParametersAvailable: Boolean = false,
    val analytes: List<String> = emptyList(),
    val specimen: String? = null,
    val samplingTime: String? = null,
    val therapeuticRanges: List<TherapeuticRange> = emptyList(),
    val toxicityThresholds: List<ToxicityThreshold> = emptyList(),
    val criticalValues: List<ToxicityThreshold> = emptyList(),
    val assayMethod: String? = null,
    val interpretationCaveats: List<String> = emptyList(),
    val sourceRefs: List<SourceRef> = emptyList()
)
