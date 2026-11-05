package com.isaakhanimann.journal.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.isaakhanimann.journal.R
import com.isaakhanimann.journal.data.hrt.*
import com.isaakhanimann.journal.data.room.AppDatabase
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HrtPlanNotifications @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val preferences = context.getSharedPreferences("hrt_alarms", 0)
    val error = MutableStateFlow<String?>(null)
    private var started = false

    @Synchronized fun start() {
        if (started) return
        started = true
        notifications.createNotificationChannel(NotificationChannel(CHANNEL, "HRT 用药计划", NotificationManager.IMPORTANCE_DEFAULT))
        scope.launch {
            combine(database.hrtDao().plans(), database.hrtDao().settings()) { plans, settings -> plans to settings }
                .collect { (plans, settings) -> safely { reconcile(plans, settings) } }
        }
    }

    suspend fun refresh() = safely { reconcile(database.hrtDao().exportPlans(), database.hrtDao().exportSettings()) }

    private fun pending(id: String, occurrence: Long): PendingIntent {
        val intent = Intent(context, HrtPlanAlarmReceiver::class.java)
            .setData(Uri.parse("journal://hrt-plan/$id"))
            .putExtra("plan_id", id).putExtra("occurrence", occurrence)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun reconcile(plans: List<HrtPlan>, settings: List<HrtModelSetting>) {
        val previous = preferences.getStringSet("scheduled", emptySet()).orEmpty().toSet()
        preferences.edit().putStringSet("scheduled", previous + plans.map { it.id }).apply()
        previous.forEach { alarms.cancel(pending(it, 0)) }
        val now = System.currentTimeMillis()
        val active = mutableSetOf<String>()
        plans.forEach { plan ->
            if (plan.paused) return@forEach
            val rule = PlanSchedule.read(plan, settings)
            if (!rule.notifications) return@forEach
            val next = PlanOccurrences.nextUnprocessed(plan, rule, now, settings.map { it.id }.toSet()) ?: return@forEach
            val intent = pending(plan.id, next)
            if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, intent)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, intent)
            }
            active += plan.id
        }
        preferences.edit().putStringSet("scheduled", active).apply()
        error.value = null
    }

    suspend fun deliver(id: String, occurrence: Long) = safely {
        val plan = database.hrtDao().plan(id) ?: return@safely
        val settings = database.hrtDao().exportSettings()
        val rule = PlanSchedule.read(plan, settings)
        val now = System.currentTimeMillis()
        val valid = occurrence <= now && now - occurrence <= 12 * 3600000L &&
            PlanOccurrences.between(plan, rule, occurrence, occurrence).singleOrNull() == occurrence
        if (valid && rule.notifications && database.hrtDao().setting(PlanOccurrences.key(id, occurrence)) == null &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)) {
            val launch = requireNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
            val open = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            notifications.notify(id, 0, NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle(plan.name)
                .setContentText("${plan.dose} ${plan.unit} · 请在应用中确认或跳过")
                .setContentIntent(open).setAutoCancel(true).build())
        }
        refresh()
    }

    private suspend fun safely(action: suspend () -> Unit) {
        try { action() } catch (error: Exception) {
            if (error is CancellationException) throw error
            this.error.value = "HRT 提醒调度失败：${error.message}"
            Log.e("HrtPlans", "Cannot schedule HRT reminders", error)
        }
    }

    companion object { private const val CHANNEL = "hrt_plans" }
}

@AndroidEntryPoint
class HrtPlanAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: HrtPlanNotifications
    @Inject lateinit var databaseStartup: DatabaseStartup
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (kotlinx.coroutines.withTimeoutOrNull(8000) { databaseStartup.awaitReady() } != true) return@launch
                val id = intent.getStringExtra("plan_id")
                if (id != null) scheduler.deliver(id, intent.getLongExtra("occurrence", -1))
                else scheduler.refresh()
            } finally { result.finish() }
        }
    }
}
