/*
 * Copyright (c) 2024. Isaak Hanimann.
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

package com.isaakhanimann.journal.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles scheduled reminder alarms
 */
@AndroidEntryPoint
class ReminderBroadcastReceiver : BroadcastReceiver() {
    @Inject lateinit var database: com.isaakhanimann.journal.data.room.AppDatabase
    @Inject lateinit var startup: com.isaakhanimann.journal.data.hrt.DatabaseStartup
    @Inject lateinit var coordinator: LegacyReminderCoordinator

    @Inject
    lateinit var notificationManager: IngestionReminderNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra("reminder_id", -1)
        val pending = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                if (kotlinx.coroutines.withTimeoutOrNull(8000) { startup.awaitReady() } != true) return@launch
                if (reminderId == -1) { coordinator.refresh(); return@launch }
                val reminder = database.experienceDao().getReminderById(reminderId) ?: return@launch
                if (!reminder.isEnabled) return@launch
                notificationManager.showReminderNotification(reminder.id, reminder.substanceName, reminder.dose, reminder.units, reminder.note)
                notificationManager.scheduleReminder(reminder)
            } catch (exception: Exception) { android.util.Log.e("LegacyReminders", "Reminder delivery failed", exception) }
            finally { pending.finish() }
        }
    }
}
