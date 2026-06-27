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

fun TimeValue.representativeHours(): Float? {
    val minHours = min?.toHours(unit)
    val maxHours = max?.toHours(unit)
    return when {
        minHours != null && maxHours != null -> ((minHours + maxHours) / 2.0).toFloat()
        minHours != null -> minHours.toFloat()
        maxHours != null -> maxHours.toFloat()
        else -> null
    }
}

fun TimeValue.endpointHours(): Float? {
    val minHours = min?.toHours(unit)
    val maxHours = max?.toHours(unit)
    return when {
        maxHours != null -> maxHours.toFloat()
        minHours != null -> minHours.toFloat()
        else -> null
    }
}

private fun Double.toHours(unit: String): Double? {
    return when (unit.lowercase()) {
        "s", "sec", "second", "seconds" -> this / 3600.0
        "m", "min", "minute", "minutes" -> this / 60.0
        "h", "hr", "hour", "hours" -> this
        "d", "day", "days" -> this * 24.0
        "wk", "week", "weeks" -> this * 24.0 * 7.0
        "mo", "month", "months" -> this * 24.0 * 30.0
        else -> null
    }
}
