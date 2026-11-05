package com.isaakhanimann.journal.notifications

import com.isaakhanimann.journal.data.room.experiences.entities.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object LegacyReminderSchedule {
    fun next(reminder: IngestionReminder, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (!reminder.isEnabled) return null
        val created = Instant.ofEpochMilli(reminder.createdAt).atZone(zone)
        val anchor = created.toLocalDate()
        val custom = if (reminder.repeatMode == RepeatMode.CUSTOM) requireNotNull(CustomRepeatData.fromJson(reminder.customRepeatData)) { "自定义提醒配置无效" } else null
        require(custom?.intervalDays == null || custom.intervalDays > 0)
        require(custom?.maxOccurrences == null || custom.maxOccurrences > 0)
        val end = custom?.endDate?.let(LocalDate::parse)
        var day = anchor
        var ordinal = 0
        val horizon = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(36600)
        while (!day.isAfter(horizon) && (end == null || !day.isAfter(end))) {
            val difference = ChronoUnit.DAYS.between(anchor, day)
            val matches = when (reminder.repeatMode) {
                RepeatMode.ONCE, RepeatMode.DAILY -> true
                RepeatMode.WEEKLY -> difference % 7 == 0L
                RepeatMode.CUSTOM -> when {
                    custom!!.daysOfWeek.isNotEmpty() -> day.dayOfWeek.value in custom.daysOfWeek
                    custom.intervalDays != null -> difference % custom.intervalDays == 0L
                    else -> error("自定义提醒缺少星期或间隔")
                }
            }
            val instant = day.atTime(reminder.reminderTime).atZone(zone).toInstant().toEpochMilli()
            if (matches && instant > reminder.createdAt) {
                ordinal++
                if (custom?.maxOccurrences != null && ordinal > custom.maxOccurrences) return null
                if (instant > now) return instant
                if (reminder.repeatMode == RepeatMode.ONCE) return null
            }
            day = day.plusDays(1)
        }
        return null
    }
}
