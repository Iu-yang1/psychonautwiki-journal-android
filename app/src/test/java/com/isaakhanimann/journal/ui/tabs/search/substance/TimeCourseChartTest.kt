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

package com.isaakhanimann.journal.ui.tabs.search.substance

import com.isaakhanimann.journal.data.substances.classes.EFFECT_TIMELINE_NOT_RECOMMENDED
import com.isaakhanimann.journal.data.substances.classes.TimeCourse
import com.isaakhanimann.journal.data.substances.classes.TimeValue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeCourseChartTest {

    @Test
    fun peakIsClampedAfterOnsetWhenTmaxPrecedesClinicalOnset() {
        val chart = TimeCourse(
            route = "oral",
            effectTimelineStatus = "available",
            onset = TimeValue(min = 3.0, max = 3.0, unit = "h"),
            tmax = TimeValue(min = 0.5, max = 1.0, unit = "h"),
            effectTimelineWindow = TimeValue(min = 24.0, max = 24.0, unit = "h")
        ).toChartModel()

        assertNotNull(chart)
        requireNotNull(chart)
        assertTrue("peak must follow onset", chart.peakHour > chart.onsetStartHour)
        assertTrue("comeup must not be negative", chart.peakHour >= chart.riseControlHour)
        assertTrue("peak must precede the chart end", chart.peakHour < chart.endHour)
        assertTrue("fall control must follow the peak", chart.fallControlHour > chart.peakHour)
    }

    @Test
    fun notRecommendedTimelineProducesNoChart() {
        val chart = TimeCourse(
            route = "intravenous",
            effectTimelineStatus = EFFECT_TIMELINE_NOT_RECOMMENDED,
            onset = TimeValue(min = 5.0, max = 10.0, unit = "min"),
            eliminationHalfLife = TimeValue(min = 2.0, max = 2.0, unit = "h")
        ).toChartModel()

        assertNull(chart)
    }
}
