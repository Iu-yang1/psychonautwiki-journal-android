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

import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.classes.roa.DurationRange
import com.isaakhanimann.journal.data.substances.classes.roa.DurationUnits
import com.isaakhanimann.journal.data.substances.classes.roa.RoaDuration
import kotlin.math.min

fun Substance.availableRoutesForIngestion(): List<AdministrationRoute> {
    val roaRoutes = roas.map { it.route }
    val timeCourseRoutes = timeCourse.flatMap { it.matchingAdministrationRoutes() }
    return (roaRoutes + timeCourseRoutes).distinct()
}

fun Substance.derivedRoaDurationForRoute(route: AdministrationRoute): RoaDuration? {
    return timeCourse.firstOrNull { it.matchesRoute(route) }?.toRoaDurationForTimeline()
}

private fun TimeCourse.matchesRoute(route: AdministrationRoute): Boolean {
    return route in matchingAdministrationRoutes()
}

fun TimeCourse.matchingAdministrationRoutes(): Set<AdministrationRoute> {
    val normalizedRoute = normalizedRouteText()
    val routeWords = normalizedRoute
        .replace("/", " ")
        .replace(",", " ")
        .replace(";", " ")
        .split(" ")
        .filter { it.isNotBlank() }
        .toSet()
    return AdministrationRoute.entries.filter { route ->
        val routeText = route.normalizedRouteText()
        val aliases = routeAliases.getValue(route)
        normalizedRoute == routeText ||
            normalizedRoute in aliases ||
            aliases.any { alias -> alias in routeWords || normalizedRoute.contains(alias) }
    }.toSet()
}

internal fun TimeCourse.toRoaDurationForTimeline(): RoaDuration? {
    var totalEnd = totalEndHours() ?: return null
    val onsetEnd = (onset?.representativeHours() ?: estimateOnsetHours(totalEnd))
        .coerceAtLeast(0f)
    var peakAt = (peakEffect?.representativeHours() ?: tmax?.representativeHours())
        ?: estimatePeakHours(onsetEnd, totalEnd)

    val minPhase = min(0.1f, totalEnd * 0.05f).coerceAtLeast(1f / 60f)
    peakAt = peakAt.coerceAtLeast(onsetEnd + minPhase)
    if (totalEnd <= peakAt + minPhase) {
        totalEnd = peakAt + minPhase * 2f
    }

    val comeup = (peakAt - onsetEnd).coerceAtLeast(minPhase)
    val maxPeakPlateau = (totalEnd - peakAt - minPhase).coerceAtLeast(minPhase)
    val peak = (totalEnd * 0.08f).coerceIn(minPhase, maxPeakPlateau)
    val offset = (totalEnd - onsetEnd - comeup - peak).coerceAtLeast(minPhase)

    return RoaDuration(
        onset = onsetEnd.toHourDurationRange(),
        comeup = comeup.toHourDurationRange(),
        peak = peak.toHourDurationRange(),
        offset = offset.toHourDurationRange(),
        total = totalEnd.toHourDurationRange(),
        afterglow = null
    )
}

private fun TimeCourse.totalEndHours(): Float? {
    val clinicalEnd = durationOfAction?.endpointHours()
    val washoutEnd = washout?.endpointHours()
    return clinicalEnd ?: washoutEnd
}

private fun estimateOnsetHours(totalEnd: Float): Float {
    return min(0.5f, totalEnd * 0.1f).coerceAtLeast(1f / 60f)
}

private fun estimatePeakHours(onsetEnd: Float, totalEnd: Float): Float {
    return onsetEnd + (totalEnd - onsetEnd).coerceAtLeast(0.25f) * 0.25f
}

private fun Float.toHourDurationRange(): DurationRange {
    val safeValue = coerceAtLeast(1f / 60f)
    return DurationRange(
        min = safeValue,
        max = safeValue,
        units = DurationUnits.HOURS
    )
}

private fun AdministrationRoute.normalizedRouteText(): String = name.lowercase().replace("_", " ")

private fun TimeCourse.normalizedRouteText(): String = route.trim().lowercase().replace("_", " ")

private val routeAliases = mapOf(
    AdministrationRoute.ORAL to setOf("oral", "po", "by mouth"),
    AdministrationRoute.SUBLINGUAL to setOf("sublingual", "sl"),
    AdministrationRoute.BUCCAL to setOf("buccal"),
    AdministrationRoute.INSUFFLATED to setOf("intranasal", "insufflated", "nasal", "nasal spray"),
    AdministrationRoute.RECTAL to setOf("rectal"),
    AdministrationRoute.TRANSDERMAL to setOf("transdermal", "topical patch", "patch"),
    AdministrationRoute.SUBCUTANEOUS to setOf("subcutaneous", "sc", "subcut", "subcutaneous injection"),
    AdministrationRoute.INTRAMUSCULAR to setOf("intramuscular", "im", "intramuscular injection"),
    AdministrationRoute.INTRAVENOUS to setOf("intravenous", "iv", "iv infusion", "infusion", "intravenous infusion"),
    AdministrationRoute.SMOKED to setOf("smoked", "vaporized"),
    AdministrationRoute.INHALED to setOf("inhaled")
)
