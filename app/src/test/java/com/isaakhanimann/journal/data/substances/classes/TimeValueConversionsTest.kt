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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun timelineUsesMidpointsForPhasesAndUpperBoundForEnd() {
        val duration = TimeCourse(
            route = "oral",
            onset = TimeValue(min = 1.0, max = 3.0, unit = "h"),
            peakEffect = TimeValue(min = 4.0, max = 6.0, unit = "h"),
            durationOfAction = TimeValue(min = 8.0, max = 12.0, unit = "h")
        ).toRoaDurationForTimeline()

        assertEquals(2f, duration?.onset?.min)
        assertEquals(3f, duration?.comeup?.min)
        assertEquals(12f, duration?.total?.min)
    }

    @Test
    fun pkEstimateUsesEffectiveHalfLifeWhenEliminationHalfLifeIsMissing() {
        val duration = TimeCourse(
            route = "oral",
            effectTimelineStatus = "pk-estimated",
            effectiveHalfLife = TimeValue(min = 12.0, max = 12.0, unit = "h")
        ).toRoaDurationForTimeline()

        assertNotNull(duration)
        assertEquals(61f, duration?.total?.min)
    }

    @Test
    fun steadyStateTimeIsNotUsedAsThePeakTime() {
        val duration = TimeCourse(
            route = "transdermal",
            effectTimelineStatus = "pk-estimated",
            timeToSteadyState = TimeValue(min = 12.0, max = 12.0, unit = "day"),
            eliminationHalfLife = TimeValue(min = 10.0, max = 10.0, unit = "h")
        ).toRoaDurationForTimeline()

        assertNotNull(duration)
        assertTrue((duration?.total?.max ?: 0f) < 100f)
    }

    @Test
    fun notRecommendedTimelineDoesNotProduceADuration() {
        val duration = TimeCourse(
            route = "sublingual",
            effectTimelineStatus = EFFECT_TIMELINE_NOT_RECOMMENDED,
            tmax = TimeValue(min = 1.0, max = 1.0, unit = "h"),
            durationOfAction = TimeValue(min = 24.0, max = 24.0, unit = "h")
        ).toRoaDurationForTimeline()

        assertNull(duration)
    }

    @Test
    fun explicitTimelineWindowSetsTheModeledEnd() {
        val duration = TimeCourse(
            route = "oral",
            effectTimelineStatus = "pk-estimated",
            tmax = TimeValue(min = 6.0, max = 6.0, unit = "h"),
            eliminationHalfLife = TimeValue(min = 13.0, max = 13.0, unit = "h"),
            effectTimelineWindow = TimeValue(min = 24.0, max = 24.0, unit = "h")
        ).toRoaDurationForTimeline()

        assertNotNull(duration)
        assertEquals(24f, duration?.total?.max)
    }

    @Test
    fun onsetLaterThanTmaxStillProducesAPositiveComeup() {
        val duration = TimeCourse(
            route = "oral",
            effectTimelineStatus = "available",
            onset = TimeValue(min = 3.0, max = 3.0, unit = "h"),
            tmax = TimeValue(min = 0.5, max = 1.0, unit = "h"),
            effectTimelineWindow = TimeValue(min = 24.0, max = 24.0, unit = "h")
        ).toRoaDurationForTimeline()

        assertNotNull(duration)
        assertEquals(3f, duration?.onset?.max)
        assertTrue("comeup must not be negative", (duration?.comeup?.min ?: 0f) > 0f)
        assertTrue("peak plateau must not be negative", (duration?.peak?.min ?: 0f) > 0f)
    }
}
