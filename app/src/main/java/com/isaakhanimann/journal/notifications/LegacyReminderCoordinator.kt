package com.isaakhanimann.journal.notifications

import android.util.Log
import com.isaakhanimann.journal.applicationScope
import com.isaakhanimann.journal.data.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacyReminderCoordinator @Inject constructor(private val database: AppDatabase, private val manager: IngestionReminderNotificationManager) {
    val error = MutableStateFlow<String?>(null)
    private var started = false
    suspend fun refresh() {
        var failure: String? = null
        database.experienceDao().getAllReminders().forEach { reminder ->
            try {
                manager.cancelReminder(reminder.id)
                if (reminder.isEnabled) manager.scheduleReminder(reminder)
            } catch (exception: Exception) {
                failure = "旧提醒 ${reminder.substanceName}：${exception.message}"
                Log.e("LegacyReminders", "Cannot reschedule legacy reminder", exception)
            }
        }
        error.value = failure
    }
    @Synchronized fun start() {
        if (started) return
        started = true
        applicationScope.launch(Dispatchers.IO) {
            var previous = emptySet<Int>()
            database.experienceDao().getAllRemindersFlow().collect { reminders ->
                (previous + reminders.map { it.id }).forEach(manager::cancelReminder)
                var failure: String? = null
                reminders.filter { it.isEnabled }.forEach { reminder ->
                    try { manager.scheduleReminder(reminder) }
                    catch (exception: Exception) { failure = "旧提醒 ${reminder.substanceName}：${exception.message}"; Log.e("LegacyReminders", "Cannot schedule legacy reminder", exception) }
                }
                error.value = failure
                previous = reminders.map { it.id }.toSet()
            }
        }
    }
}
