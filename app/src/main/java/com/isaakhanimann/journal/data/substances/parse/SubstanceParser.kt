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

package com.isaakhanimann.journal.data.substances.parse

import androidx.compose.ui.graphics.Color
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.classes.*
import com.isaakhanimann.journal.data.substances.classes.roa.*
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubstanceParser @Inject constructor() : SubstanceParserInterface {

    override fun parseSubstanceFile(string: String): SubstanceFile {
        return try {
            val wholeFile = JSONObject(string)
            SubstanceFile(
                categories = parseCategories(wholeFile),
                substances = parseSubstances(wholeFile)
            )
        } catch (e: Exception) {
            SubstanceFile(
                categories = emptyList(),
                substances = emptyList()
            )
        }
    }

    override fun extractSubstanceString(string: String): String? {
        return try {
            val wholeFile = JSONObject(string)
            val data = wholeFile.getJSONObject("data")
            val substances = data.getJSONArray("substances")
            substances.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun parseCategories(wholeFile: JSONObject): List<Category> {
        val jsonCategories = wholeFile.getOptionalJSONArray("categories") ?: return emptyList()
        val categories: MutableList<Category> = mutableListOf()
        for (i in 0 until jsonCategories.length()) {
            val jsonCategory = jsonCategories.getOptionalJSONObject(i) ?: continue
            val newCategory = parseCategory(jsonCategory) ?: continue
            categories.add(newCategory)
        }
        return categories
    }

    private fun parseSubstances(wholeFile: JSONObject): List<Substance> {
        val jsonSubstances = wholeFile.getOptionalJSONArray("substances") ?: return emptyList()
        val substances: MutableList<Substance> = mutableListOf()
        for (i in 0 until jsonSubstances.length()) {
            val jsonCategory = jsonSubstances.getOptionalJSONObject(i) ?: continue
            val newSubstance = parseSubstance(jsonCategory)
            substances.add(newSubstance)
        }
        return substances
    }

    private fun parseCategory(jsonCategory: JSONObject): Category? {
        val name = jsonCategory.getOptionalString("name") ?: return null
        val description = jsonCategory.getOptionalString("description") ?: return null
        val url = jsonCategory.getOptionalString("url")
        val colorDecimal = jsonCategory.getOptionalLong("color") ?: 4278876927
        val color = Color(colorDecimal)
        return Category(name, description, url, color)
    }

    private fun parseSubstance(jsonSubstance: JSONObject): Substance {
        val name = jsonSubstance.getString("name")
        val jsonCommonNames = jsonSubstance.getOptionalJSONArray("commonNames")
        val commonNames = parseCommonNames(jsonCommonNames, removeName = name)
        val url = jsonSubstance.getString("url")
        val isApproved = jsonSubstance.getOptionalBoolean("isApproved") ?: false
        val jsonTolerance = jsonSubstance.getOptionalJSONObject("tolerance")
        val tolerance = parseTolerance(jsonTolerance)
        val jsonTolerances = jsonSubstance.getOptionalJSONArray("crossTolerances")
        val crossTolerances = parseCrossTolerances(jsonTolerances)
        val addictionPotential = jsonSubstance.getOptionalString("addictionPotential")
        val jsonToxicities = jsonSubstance.getOptionalJSONArray("toxicities")
        val toxicities = parseJsonArrayToStringArray(jsonToxicities)
        val jsonCategory = jsonSubstance.getOptionalJSONArray("categories")
        val categories = parseJsonArrayToStringArray(jsonCategory)
        val summary = jsonSubstance.getOptionalString("summary")
        val effectsSummary = jsonSubstance.getOptionalString("effectsSummary")
        val dosageRemark = jsonSubstance.getOptionalString("dosageRemark")
        val generalRisks = jsonSubstance.getOptionalString("generalRisks")
        val longtermRisks = jsonSubstance.getOptionalString("longtermRisks")
        val jsonSaferUse = jsonSubstance.getOptionalJSONArray("saferUse")
        val saferUse = parseJsonArrayToStringArray(jsonSaferUse)
        val jsonInteractions = jsonSubstance.getOptionalJSONObject("interactions")
        val interactions = parseInteractions(jsonInteractions)
        val jsonRoas = jsonSubstance.getOptionalJSONArray("roas")
        val roas = parseRoas(jsonRoas)
        val clinicalInfo = parseClinicalInfo(jsonSubstance.getOptionalJSONObject("clinicalInfo"))
        val timeCourse = parseTimeCourses(jsonSubstance.getOptionalJSONArray("timeCourse"))
        val tdm = parseTherapeuticDrugMonitoring(jsonSubstance.getOptionalJSONObject("tdm"))
        return Substance(
            name = name,
            commonNames = commonNames,
            url = url,
            isApproved = isApproved,
            tolerance = tolerance,
            crossTolerances = crossTolerances,
            addictionPotential = addictionPotential,
            toxicities = toxicities,
            categories = categories,
            summary = summary,
            effectsSummary = effectsSummary,
            dosageRemark = dosageRemark,
            generalRisks = generalRisks,
            longtermRisks = longtermRisks,
            saferUse = saferUse,
            interactions = interactions,
            roas = roas,
            clinicalInfo = clinicalInfo,
            timeCourse = timeCourse,
            tdm = tdm,
        )
    }

    private fun parseClinicalInfo(jsonClinicalInfo: JSONObject?): ClinicalInfo? {
        if (jsonClinicalInfo == null) return null
        val atcCodes = parseJsonArrayToStringArray(jsonClinicalInfo.getOptionalJSONArray("atcCodes"))
        val drugClass = parseJsonArrayToStringArray(jsonClinicalInfo.getOptionalJSONArray("drugClass"))
        val indications = parseJsonArrayToStringArray(jsonClinicalInfo.getOptionalJSONArray("indications"))
        val contraindications = parseJsonArrayToStringArray(jsonClinicalInfo.getOptionalJSONArray("contraindications"))
        val majorWarnings = parseJsonArrayToStringArray(jsonClinicalInfo.getOptionalJSONArray("majorWarnings"))
        val majorInteractions = parseJsonArrayToStringArray(jsonClinicalInfo.getOptionalJSONArray("majorInteractions"))
        val monitoring = parseJsonArrayToStringArray(jsonClinicalInfo.getOptionalJSONArray("monitoring"))
        val sourceRefs = parseSourceRefs(jsonClinicalInfo.getOptionalJSONArray("sourceRefs"))
        val hasAnyContent = listOf(
            atcCodes,
            drugClass,
            indications,
            contraindications,
            majorWarnings,
            majorInteractions,
            monitoring,
            sourceRefs
        ).any { it.isNotEmpty() }
        if (!hasAnyContent) return null
        return ClinicalInfo(
            atcCodes = atcCodes,
            drugClass = drugClass,
            indications = indications,
            contraindications = contraindications,
            majorWarnings = majorWarnings,
            majorInteractions = majorInteractions,
            monitoring = monitoring,
            sourceRefs = sourceRefs
        )
    }

    private fun parseTimeCourses(jsonTimeCourses: JSONArray?): List<TimeCourse> {
        if (jsonTimeCourses == null) return emptyList()
        val timeCourses: MutableList<TimeCourse> = mutableListOf()
        for (i in 0 until jsonTimeCourses.length()) {
            val jsonTimeCourse = jsonTimeCourses.getOptionalJSONObject(i) ?: continue
            val route = jsonTimeCourse.getOptionalString("route") ?: continue
            timeCourses.add(
                TimeCourse(
                    route = route,
                    formulation = jsonTimeCourse.getOptionalString("formulation"),
                    onset = parseTimeValue(jsonTimeCourse.getOptionalJSONObject("onset")),
                    tmax = parseTimeValue(jsonTimeCourse.getOptionalJSONObject("tmax")),
                    peakEffect = parseTimeValue(jsonTimeCourse.getOptionalJSONObject("peakEffect")),
                    durationOfAction = parseTimeValue(jsonTimeCourse.getOptionalJSONObject("durationOfAction")),
                    eliminationHalfLife = parseTimeValue(jsonTimeCourse.getOptionalJSONObject("eliminationHalfLife")),
                    timeToSteadyState = parseTimeValue(jsonTimeCourse.getOptionalJSONObject("timeToSteadyState")),
                    washout = parseTimeValue(jsonTimeCourse.getOptionalJSONObject("washout")),
                    notes = parseJsonArrayToStringArray(jsonTimeCourse.getOptionalJSONArray("notes")),
                    sourceRefs = parseSourceRefs(jsonTimeCourse.getOptionalJSONArray("sourceRefs"))
                )
            )
        }
        return timeCourses
    }

    private fun parseTherapeuticDrugMonitoring(jsonTdm: JSONObject?): TherapeuticDrugMonitoring? {
        if (jsonTdm == null) return null
        val isRoutinelyMonitored = jsonTdm.getOptionalBoolean("isRoutinelyMonitored")
        val monitoringType = jsonTdm.getOptionalString("monitoringType")
        val reason = jsonTdm.getOptionalString("reason")
        val pharmacokineticParametersAvailable =
            jsonTdm.getOptionalBoolean("pharmacokineticParametersAvailable") ?: false
        val analytes = parseJsonArrayToStringArray(jsonTdm.getOptionalJSONArray("analytes"))
        val specimen = jsonTdm.getOptionalString("specimen")
        val samplingTime = jsonTdm.getOptionalString("samplingTime")
        val therapeuticRanges = parseTherapeuticRanges(jsonTdm.getOptionalJSONArray("therapeuticRanges"))
        val toxicityThresholds = parseToxicityThresholds(jsonTdm.getOptionalJSONArray("toxicityThresholds"))
        val criticalValues = parseToxicityThresholds(jsonTdm.getOptionalJSONArray("criticalValues"))
        val assayMethod = jsonTdm.getOptionalString("assayMethod")
        val interpretationCaveats =
            parseJsonArrayToStringArray(jsonTdm.getOptionalJSONArray("interpretationCaveats"))
        val sourceRefs = parseSourceRefs(jsonTdm.getOptionalJSONArray("sourceRefs"))

        val hasAnyContent = listOf(
            monitoringType,
            reason,
            specimen,
            samplingTime,
            assayMethod
        ).any { it != null } || listOf(
            analytes,
            therapeuticRanges,
            toxicityThresholds,
            criticalValues,
            interpretationCaveats,
            sourceRefs
        ).any { it.isNotEmpty() } || isRoutinelyMonitored != null || pharmacokineticParametersAvailable
        if (!hasAnyContent) return null

        return TherapeuticDrugMonitoring(
            isRoutinelyMonitored = isRoutinelyMonitored ?: false,
            monitoringType = monitoringType ?: "",
            reason = reason,
            pharmacokineticParametersAvailable = pharmacokineticParametersAvailable,
            analytes = analytes,
            specimen = specimen,
            samplingTime = samplingTime,
            therapeuticRanges = therapeuticRanges,
            toxicityThresholds = toxicityThresholds,
            criticalValues = criticalValues,
            assayMethod = assayMethod,
            interpretationCaveats = interpretationCaveats,
            sourceRefs = sourceRefs
        )
    }

    private fun parseTherapeuticRanges(jsonRanges: JSONArray?): List<TherapeuticRange> {
        if (jsonRanges == null) return emptyList()
        val ranges: MutableList<TherapeuticRange> = mutableListOf()
        for (i in 0 until jsonRanges.length()) {
            val jsonRange = jsonRanges.getOptionalJSONObject(i) ?: continue
            val range = jsonRange.getOptionalString("range") ?: continue
            val unit = jsonRange.getOptionalString("unit") ?: continue
            ranges.add(
                TherapeuticRange(
                    indication = jsonRange.getOptionalString("indication"),
                    range = range,
                    unit = unit,
                    note = jsonRange.getOptionalString("note")
                )
            )
        }
        return ranges
    }

    private fun parseToxicityThresholds(jsonThresholds: JSONArray?): List<ToxicityThreshold> {
        if (jsonThresholds == null) return emptyList()
        val thresholds: MutableList<ToxicityThreshold> = mutableListOf()
        for (i in 0 until jsonThresholds.length()) {
            val jsonThreshold = jsonThresholds.getOptionalJSONObject(i) ?: continue
            val threshold = jsonThreshold.getOptionalString("threshold") ?: continue
            val unit = jsonThreshold.getOptionalString("unit") ?: continue
            thresholds.add(
                ToxicityThreshold(
                    threshold = threshold,
                    unit = unit,
                    note = jsonThreshold.getOptionalString("note")
                )
            )
        }
        return thresholds
    }

    private fun parseTimeValue(jsonTimeValue: JSONObject?): TimeValue? {
        if (jsonTimeValue == null) return null
        val unit = jsonTimeValue.getOptionalString("unit") ?: return null
        val min = jsonTimeValue.getOptionalDouble("min")
        val max = jsonTimeValue.getOptionalDouble("max")
        val basis = jsonTimeValue.getOptionalString("basis")
        val note = jsonTimeValue.getOptionalString("note")
        return if (min == null && max == null && basis == null && note == null) {
            null
        } else {
            TimeValue(
                min = min,
                max = max,
                unit = unit,
                basis = basis,
                note = note
            )
        }
    }

    private fun parseSourceRefs(jsonSourceRefs: JSONArray?): List<SourceRef> {
        if (jsonSourceRefs == null) return emptyList()
        val sourceRefs: MutableList<SourceRef> = mutableListOf()
        for (i in 0 until jsonSourceRefs.length()) {
            val jsonSourceRef = jsonSourceRefs.getOptionalJSONObject(i) ?: continue
            val title = jsonSourceRef.getOptionalString("title") ?: continue
            val url = jsonSourceRef.getOptionalString("url") ?: continue
            val sourceType = jsonSourceRef.getOptionalString("sourceType") ?: continue
            val accessedDate = jsonSourceRef.getOptionalString("accessedDate") ?: continue
            sourceRefs.add(
                SourceRef(
                    title = title,
                    url = url,
                    sourceType = sourceType,
                    accessedDate = accessedDate
                )
            )
        }
        return sourceRefs
    }

    private fun parseInteractions(jsonInteractions: JSONObject?): Interactions? {
        if (jsonInteractions == null) return null
        val jsonUncertain = jsonInteractions.getOptionalJSONArray("uncertain")
        val uncertainInteractions = parseJsonArrayToStringArray(jsonUncertain)
        val jsonUnsafe = jsonInteractions.getOptionalJSONArray("unsafe")
        val unsafeInteractions = parseJsonArrayToStringArray(jsonUnsafe)
        val jsonDangerous = jsonInteractions.getOptionalJSONArray("dangerous")
        val dangerousInteractions = parseJsonArrayToStringArray(jsonDangerous)
        return Interactions(
            dangerous = dangerousInteractions,
            unsafe = unsafeInteractions,
            uncertain = uncertainInteractions
        )
    }

    private fun parseCommonNames(jsonNames: JSONArray?, removeName: String): List<String> {
        if (jsonNames == null) return emptyList()
        val commonNames: MutableList<String> = mutableListOf()
        for (i in 0 until jsonNames.length()) {
            val commonName = jsonNames.getOptionalString(i) ?: continue
            if (commonName != removeName) {
                commonNames.add(commonName)
            }
        }
        return commonNames
    }

    private fun parseJsonArrayToStringArray(jsonArray: JSONArray?): List<String> {
        if (jsonArray == null) return emptyList()
        val result: MutableList<String> = mutableListOf()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getOptionalString(i) ?: continue
            result.add(item)
        }
        return result
    }

    private fun parseTolerance(jsonTolerance: JSONObject?): Tolerance? {
        if (jsonTolerance == null) return null
        val full = jsonTolerance.getOptionalString("full")
        val half = jsonTolerance.getOptionalString("half")
        val zero = jsonTolerance.getOptionalString("zero")
        return if (full == null && half == null && zero == null) {
            null
        } else {
            Tolerance(full, half, zero)
        }
    }

    private fun parseRoas(jsonRoas: JSONArray?): List<Roa> {
        if (jsonRoas == null) return emptyList()
        val roas: MutableList<Roa> = mutableListOf()
        for (i in 0 until jsonRoas.length()) {
            val oneJsonRoa = jsonRoas.getOptionalJSONObject(i) ?: continue
            val roa = parseRoa(oneJsonRoa)
            if (roa != null) {
                roas.add(roa)
            }
        }
        return roas
    }

    private fun parseRoa(oneJsonRoa: JSONObject): Roa? {
        val routeName = oneJsonRoa.getOptionalString("name")?.uppercase() ?: return null
        val route = when (routeName) {
            AdministrationRoute.ORAL.name -> AdministrationRoute.ORAL
            AdministrationRoute.SUBLINGUAL.name -> AdministrationRoute.SUBLINGUAL
            AdministrationRoute.BUCCAL.name -> AdministrationRoute.BUCCAL
            AdministrationRoute.INSUFFLATED.name -> AdministrationRoute.INSUFFLATED
            AdministrationRoute.RECTAL.name -> AdministrationRoute.RECTAL
            AdministrationRoute.TRANSDERMAL.name -> AdministrationRoute.TRANSDERMAL
            AdministrationRoute.SUBCUTANEOUS.name -> AdministrationRoute.SUBCUTANEOUS
            AdministrationRoute.INTRAMUSCULAR.name -> AdministrationRoute.INTRAMUSCULAR
            AdministrationRoute.INTRAVENOUS.name -> AdministrationRoute.INTRAVENOUS
            AdministrationRoute.SMOKED.name -> AdministrationRoute.SMOKED
            AdministrationRoute.INHALED.name -> AdministrationRoute.INHALED
            else -> return null
        }
        val jsonRoaDose = oneJsonRoa.getOptionalJSONObject("dose")
        val roaDose = parseRoaDose(jsonRoaDose)
        val jsonRoaDuration = oneJsonRoa.getOptionalJSONObject("duration")
        val roaDuration = parseRoaDuration(jsonRoaDuration)
        val jsonBio = oneJsonRoa.getOptionalJSONObject("bioavailability")
        val bioavailability = parseBioavailability(jsonBio)
        return Roa(
            route = route,
            roaDose = roaDose,
            roaDuration = roaDuration,
            bioavailability = bioavailability
        )
    }

    private fun parseRoaDose(jsonDose: JSONObject?): RoaDose? {
        if (jsonDose == null) return null
        val units = jsonDose.getString("units")
        val lightMin = jsonDose.getOptionalDouble("lightMin")
        val commonMin = jsonDose.getOptionalDouble("commonMin")
        val strongMin = jsonDose.getOptionalDouble("strongMin")
        val heavyMin = jsonDose.getOptionalDouble("heavyMin")
        return RoaDose(
            units = units,
            lightMin = lightMin,
            commonMin = commonMin,
            strongMin = strongMin,
            heavyMin = heavyMin,
        )
    }

    private fun parseRoaDuration(jsonRoaDuration: JSONObject?): RoaDuration? {
        if (jsonRoaDuration == null) return null
        val jsonOnset = jsonRoaDuration.getOptionalJSONObject("onset")
        val onset = parseDurationRange(jsonOnset)
        val jsonComeup = jsonRoaDuration.getOptionalJSONObject("comeup")
        val comeup = parseDurationRange(jsonComeup)
        val jsonPeak = jsonRoaDuration.getOptionalJSONObject("peak")
        val peak = parseDurationRange(jsonPeak)
        val jsonOffset = jsonRoaDuration.getOptionalJSONObject("offset")
        val offset = parseDurationRange(jsonOffset)
        val jsonTotal = jsonRoaDuration.getOptionalJSONObject("total")
        val total = parseDurationRange(jsonTotal)
        val jsonAfterglow = jsonRoaDuration.getOptionalJSONObject("afterglow")
        val afterglow = parseDurationRange(jsonAfterglow)
        return if (onset == null && comeup == null && peak == null && offset == null && total == null && afterglow == null) {
            null
        } else {
            RoaDuration(
                onset = onset,
                comeup = comeup,
                peak = peak,
                offset = offset,
                total = total,
                afterglow = afterglow
            )
        }
    }

    private fun parseDurationRange(jsonDurationRange: JSONObject?): DurationRange? {
        if (jsonDurationRange == null) return null
        val units = jsonDurationRange.getOptionalString("units")
        val min = jsonDurationRange.getOptionalDouble("min")
        val max = jsonDurationRange.getOptionalDouble("max")
        if ((min == null && max == null) || units == null) {
            return null
        }
        val durationUnits = when (units) {
            DurationUnits.SECONDS.text -> DurationUnits.SECONDS
            DurationUnits.MINUTES.text -> DurationUnits.MINUTES
            DurationUnits.HOURS.text -> DurationUnits.HOURS
            DurationUnits.DAYS.text -> DurationUnits.DAYS
            else -> null
        }
        return DurationRange(
            min?.toFloat(),
            max?.toFloat(),
            durationUnits
        )
    }

    private fun parseBioavailability(jsonBio: JSONObject?): Bioavailability? {
        if (jsonBio == null) return null
        val min = jsonBio.getOptionalDouble("min")
        val max = jsonBio.getOptionalDouble("max")
        return if (min == null && max == null) {
            null
        } else {
            Bioavailability(min, max)
        }
    }

    private fun parseCrossTolerances(jsonTolerances: JSONArray?): List<String> {
        if (jsonTolerances == null) return emptyList()
        val tolNames: MutableList<String> = mutableListOf()
        for (i in 0 until jsonTolerances.length()) {
            val tolName = jsonTolerances.getOptionalString(i) ?: continue
            tolNames.add(tolName)
        }
        return tolNames
    }
}
