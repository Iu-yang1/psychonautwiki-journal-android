package com.isaakhanimann.journal.data.hrt

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class PlanScheduleTest {
    private fun time(text: String) = Instant.parse(text).toEpochMilli()
    private val plan = HrtPlan("p", "med", "Medication", null, "ORAL", 1.0, "mg", time("2026-11-01T00:00:00Z"), 24)

    @Test fun hourlyAnchorIsElapsedTimeAndBounded() {
        val rule = PlanSchedule(interval = 6)
        val points = PlanOccurrences.between(plan, rule, time("2026-11-01T01:00:00Z"), time("2026-11-01T13:00:00Z"))
        assertEquals(listOf(time("2026-11-01T06:00:00Z"), time("2026-11-01T12:00:00Z")), points)
    }

    @Test fun dailyMultiTimeAndEveryOtherDay() {
        val rule = PlanSchedule(ScheduleMode.DAYS, 2, listOf("08:00", "20:00"), zone = "UTC")
        val points = PlanOccurrences.between(plan, rule, plan.start, time("2026-11-04T00:00:00Z"))
        assertEquals(listOf("2026-11-01T08:00:00Z", "2026-11-01T20:00:00Z", "2026-11-03T08:00:00Z", "2026-11-03T20:00:00Z").map(::time), points)
    }

    @Test fun weekdaysAndInclusiveEnd() {
        val rule = PlanSchedule(ScheduleMode.WEEKDAYS, times = listOf("09:00"), weekdays = setOf(1, 3), end = time("2026-11-04T09:00:00Z"), zone = "UTC")
        val points = PlanOccurrences.between(plan, rule, plan.start, time("2026-11-10T00:00:00Z"))
        assertEquals(listOf(time("2026-11-02T09:00:00Z"), time("2026-11-04T09:00:00Z")), points)
    }

    @Test fun dstGapMovesForwardAndOverlapDoesNotDuplicate() {
        val spring = plan.copy(start = time("2026-03-07T00:00:00Z"))
        val rule = PlanSchedule(ScheduleMode.DAYS, 1, listOf("02:30"), zone = "America/New_York")
        assertEquals(listOf(time("2026-03-08T07:30:00Z")), PlanOccurrences.between(spring, rule, time("2026-03-08T00:00:00Z"), time("2026-03-09T00:00:00Z")))
        val fall = rule.copy(times = listOf("01:30"))
        assertEquals(listOf(time("2026-11-01T05:30:00Z")), PlanOccurrences.between(plan, fall, time("2026-11-01T00:00:00Z"), time("2026-11-02T00:00:00Z")))
    }

    @Test fun localCalendarRespondsToDeviceZoneButElapsedScheduleDoesNot() {
        val rule = PlanSchedule(ScheduleMode.DAYS, 1, listOf("08:00"))
        val from = time("2026-11-02T00:00:00Z")
        val to = time("2026-11-03T00:00:00Z") - 1
        assertNotEquals(PlanOccurrences.between(plan, rule, from, to, deviceZone = ZoneId.of("UTC")), PlanOccurrences.between(plan, rule, from, to, deviceZone = ZoneId.of("Asia/Singapore")))
        assertEquals(PlanOccurrences.between(plan, PlanSchedule(), from, to, deviceZone = ZoneId.of("UTC")), PlanOccurrences.between(plan, PlanSchedule(), from, to, deviceZone = ZoneId.of("Asia/Singapore")))
    }

    @Test fun pausedAndFuturePlansDoNotProduceDueEvents() {
        assertTrue(PlanOccurrences.between(plan.copy(paused = true), PlanSchedule(), plan.start, plan.start + 86400000L).isEmpty())
        assertTrue(PlanOccurrences.between(plan, PlanSchedule(), plan.start - 86400000L, plan.start - 1).isEmpty())
    }

    @Test fun editedRulesDoNotGenerateRetroactiveOccurrences() {
        val effective = time("2026-11-02T12:00:00Z")
        val rule = PlanSchedule(interval = 6, effectiveFrom = effective)
        assertEquals(listOf(effective, time("2026-11-02T18:00:00Z")), PlanOccurrences.between(plan, rule, plan.start, time("2026-11-02T18:00:00Z")))
    }

    @Test fun dstGapMustSortBeforeApplyingLimit() {
        val spring = plan.copy(start = time("2026-03-07T00:00:00Z"))
        val rule = PlanSchedule(ScheduleMode.DAYS, times = listOf("02:30", "03:00"), zone = "America/New_York")
        assertEquals(listOf(time("2026-03-08T07:00:00Z")), PlanOccurrences.between(spring, rule, time("2026-03-08T00:00:00Z"), time("2026-03-09T00:00:00Z"), limit = 1))
    }

    @Test(expected = IllegalArgumentException::class) fun invalidTimesAreRejected() { PlanSchedule(ScheduleMode.DAYS, times = listOf("08:00", "08:00")).validate(plan.start) }
    @Test fun nextReminderSkipsResolvedSlotsAndHandlesLongIntervals() {
        val rule = PlanSchedule(interval = 24*90)
        val first = plan.start + 90*86400000L
        val result = PlanOccurrences.nextUnprocessed(plan, rule, plan.start, setOf(PlanOccurrences.key(plan.id, first)))
        assertEquals(plan.start + 180*86400000L, result)
    }
}
