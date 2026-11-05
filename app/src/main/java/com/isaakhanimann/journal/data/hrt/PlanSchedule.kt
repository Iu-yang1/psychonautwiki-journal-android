package com.isaakhanimann.journal.data.hrt

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalTime
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Serializable
enum class ScheduleMode { HOURS, DAYS, WEEKDAYS }

@Serializable
data class PlanSchedule(
    val mode: ScheduleMode = ScheduleMode.HOURS,
    val interval: Int = if (mode == ScheduleMode.HOURS) 24 else 1,
    val times: List<String> = listOf("08:00"),
    val weekdays: Set<Int> = (1..7).toSet(),
    val end: Long? = null,
    val zone: String = "local",
    val stockId: String? = null,
    val notifications: Boolean = true,
    val anchorDate: String? = null,
    val effectiveFrom: Long? = null,
    val productId: String? = null,
    val applicationSite: String? = null,
    val patchWearHours: Double? = null,
    val doseBasis: DoseBasis? = null,
) {
    fun validate(start: Long) {
        require(interval in 1..87600) { "间隔须为 1–87600" }
        require(end == null || end >= start) { "结束时间早于开始时间" }
        require(patchWearHours == null || patchWearHours.isFinite() && patchWearHours > 0) { "贴片佩戴时长无效" }
        if (zone != "local") ZoneId.of(zone)
        anchorDate?.let(LocalDate::parse)
        if (mode != ScheduleMode.HOURS) {
            require(times.isNotEmpty() && times.size <= 24 && times.distinct().size == times.size) { "请填写不重复的服药时间" }
            times.forEach { LocalTime.parse(it) }
        }
        if (mode == ScheduleMode.WEEKDAYS) require(weekdays.isNotEmpty() && weekdays.all { it in 1..7 }) { "请选择星期" }
    }

    companion object {
        fun key(id: String) = "schedule:$id"
        fun read(plan: HrtPlan, settings: List<HrtModelSetting>): PlanSchedule {
            val saved = settings.singleOrNull { it.id == key(plan.id) }
            return if (saved == null) PlanSchedule(interval = plan.intervalHours)
            else Json.decodeFromString<PlanSchedule>(saved.payload).also { it.validate(plan.start) }
        }
    }

    fun setting(planId: String) = HrtModelSetting(key(planId), Json.encodeToString(this))
}

object PlanOccurrences {
    fun nextUnprocessed(plan: HrtPlan, schedule: PlanSchedule, after: Long, processed: Set<String>): Long? {
        if (after == Long.MAX_VALUE) return null
        val span = 36600L * 86400000L
        val through = if (after > Long.MAX_VALUE - span) Long.MAX_VALUE else after + span
        var from = after + 1
        for (attempt in 0..10000) {
            val candidate = between(plan, schedule, from, through, limit = 1).firstOrNull() ?: return null
            if (key(plan.id, candidate) !in processed) return candidate
            if (candidate == Long.MAX_VALUE) return null
            from = candidate + 1
        }
        return null
    }
    fun between(plan: HrtPlan, schedule: PlanSchedule, from: Long, through: Long, limit: Int = 100, deviceZone: ZoneId = ZoneId.systemDefault()): List<Long> {
        schedule.validate(plan.start)
        require(from <= through && limit in 1..10000)
        if (plan.paused) return emptyList()
        val low = maxOf(from, plan.start, schedule.effectiveFrom ?: plan.start)
        val high = minOf(through, schedule.end ?: Long.MAX_VALUE)
        if (low > high) return emptyList()
        if (schedule.mode == ScheduleMode.HOURS) {
            val interval = schedule.interval * 3600000L
            val offset = low - plan.start
            val first = plan.start + (offset / interval + if (offset % interval == 0L) 0 else 1) * interval
            return generateSequence(first) { previous ->
                if (previous > Long.MAX_VALUE - interval) null else previous + interval
            }.takeWhile { it <= high }.take(limit).toList()
        }
        val zone = if (schedule.zone == "local") deviceZone else ZoneId.of(schedule.zone)
        val anchor = schedule.anchorDate?.let(LocalDate::parse) ?: Instant.ofEpochMilli(plan.start).atZone(zone).toLocalDate()
        var day = Instant.ofEpochMilli(low).atZone(zone).toLocalDate()
        val last = Instant.ofEpochMilli(high).atZone(zone).toLocalDate()
        require(ChronoUnit.DAYS.between(day, last) <= 36602) { "查询日期范围过大" }
        val result = mutableListOf<Long>()
        val times = schedule.times.map(LocalTime::parse).sorted()
        while (!day.isAfter(last) && result.size < limit) {
            val matches = when (schedule.mode) {
                ScheduleMode.DAYS -> ChronoUnit.DAYS.between(anchor, day) % schedule.interval == 0L
                ScheduleMode.WEEKDAYS -> day.dayOfWeek.value in schedule.weekdays
                else -> false
            }
            if (matches) times.map { time ->
                // Zone rules move a nonexistent clock time forward and select the earlier overlap.
                day.atTime(time).atZone(zone).toInstant().toEpochMilli()
            }.distinct().sorted().forEach { instant ->
                if (instant in low..high && instant !in result && result.size < limit) result += instant
            }
            day = day.plusDays(1)
        }
        return result.sorted()
    }

    fun key(planId: String, occurrence: Long) = "plan:$planId:$occurrence"
}

@Serializable
data class PlanRevision(val time: Long, val plan: HrtPlan, val schedule: PlanSchedule)
