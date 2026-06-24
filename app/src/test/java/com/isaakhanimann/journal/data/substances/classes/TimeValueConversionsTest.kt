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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeValueConversionsTest {

    @Test
    fun representativeHoursUsesRangeMidpoint() {
        assertEquals(
            4f,
            TimeValue(min = 2.0, max = 6.0, unit = "h").representativeHours()
        )
        assertEquals(
            1f,
            TimeValue(min = 30.0, max = 90.0, unit = "min").representativeHours()
        )
    }

    @Test
    fun representativeHoursSupportsSingleBoundaryAndUnknownUnit() {
        assertEquals(
            3f,
            TimeValue(min = 3.0, max = null, unit = "h").representativeHours()
        )
        assertEquals(
            5f,
            TimeValue(min = null, max = 5.0, unit = "h").representativeHours()
        )
        assertNull(
            TimeValue(min = 1.0, max = 2.0, unit = "unknown").representativeHours()
        )
    }

    @Test
    fun timelineUsesMidpointsForEveryTimeRange() {
        val duration = TimeCourse(
            route = "oral",
            onset = TimeValue(min = 1.0, max = 3.0, unit = "h"),
            peakEffect = TimeValue(min = 4.0, max = 6.0, unit = "h"),
            durationOfAction = TimeValue(min = 8.0, max = 12.0, unit = "h")
        ).toRoaDurationForTimeline()

        assertEquals(2f, duration?.onset?.min)
        assertEquals(3f, duration?.comeup?.min)
        assertEquals(10f, duration?.total?.min)
    }
}
