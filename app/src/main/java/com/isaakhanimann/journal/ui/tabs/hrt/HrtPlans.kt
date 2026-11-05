package com.isaakhanimann.journal.ui.tabs.hrt

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.isaakhanimann.journal.data.hrt.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import androidx.room.withTransaction
import org.json.JSONObject

@Composable
fun HrtPlans(viewModel: HrtViewModel, consumer: String?) {
    val context = LocalContext.current
    val plans by viewModel.plans.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val stocks by viewModel.stock.collectAsState()
    val legacy by viewModel.legacyReminders.collectAsState()
    val legacyAlarmError by viewModel.legacyAlarms.error.collectAsState()
    val schedulerError by viewModel.notificationScheduler.error.collectAsState()
    var editor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<HrtPlan?>(null) }
    var migrationId by remember { mutableStateOf<Int?>(null) }
    var archiveKey by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf<String?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var notificationsEnabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(); delay(30000) } }
    val format = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton({ editing = null; migrationId = null; archiveKey = null; editor = true }) { Text("添加计划") }
        if (!notificationsEnabled) TextButton({
            if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }) { Text("开启通知") }
        schedulerError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        legacyAlarmError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (plans.none { it.consumer == consumer }) Text("暂无用药计划")
        plans.filter { it.consumer == consumer }.forEach { plan ->
            val decoded = runCatching { PlanSchedule.read(plan, settings) }
            val rule = decoded.getOrNull()
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(plan.name, style = MaterialTheme.typography.titleMedium)
                    Text("${plan.dose} ${plan.unit} · ${plan.route}")
                    if (rule == null) Text("计划配置无效，请编辑修复", color = MaterialTheme.colorScheme.error)
                    Row {
                        TextButton({ editing = plan; migrationId = null; archiveKey = null; editor = true }) { Text("编辑") }
                        if (rule != null) TextButton({ viewModel.run { viewModel.repo.savePlan(plan.copy(paused = !plan.paused), rule) } }) { Text(if (plan.paused) "恢复" else "暂停") }
                        TextButton({ history = plan.id }) { Text("历史") }
                    }
                    if (rule != null && !plan.paused) {
                        val occurrences = PlanOccurrences.between(plan, rule, now - 7 * 86400000L, now + 8 * 86400000L, 500)
                        val due = occurrences.filter { it <= now && settings.none { setting -> setting.id == PlanOccurrences.key(plan.id, it) } }.takeLast(5)
                        val next = PlanOccurrences.nextUnprocessed(plan, rule, now, settings.map { it.id }.toSet())
                        if (due.isEmpty()) Text("没有待确认用药")
                        due.forEach { occurrence ->
                            Row {
                                Text(format.format(Instant.ofEpochMilli(occurrence)), modifier = Modifier.weight(1f))
                                TextButton({ viewModel.run { viewModel.repo.take(plan, occurrence) } }) { Text(if (rule.stockId == null) "已用药" else "已用药并扣库存") }
                                TextButton({ viewModel.run { viewModel.repo.skip(plan, occurrence) } }) { Text("跳过") }
                            }
                        }
                        next?.let { occurrence ->
                            Text("下次：${format.format(Instant.ofEpochMilli(occurrence))}")
                            TextButton({ viewModel.run { viewModel.repo.skip(plan, occurrence) } }) { Text("跳过下次") }
                        }
                        val processed = occurrences.filter { occurrence -> settings.any { it.id == PlanOccurrences.key(plan.id, occurrence) } }.takeLast(3)
                        processed.forEach { occurrence ->
                            val result = settings.single { it.id == PlanOccurrences.key(plan.id, occurrence) }.payload
                            Text("${format.format(Instant.ofEpochMilli(occurrence))} · ${if (result == "skipped") "已跳过" else "已记录"}")
                        }
                    }
                }
            }
        }
        legacy.filter { it.isEnabled }.forEach { reminder ->
            TextButton({
                val sub = viewModel.repo.substances.getSubstance(reminder.substanceName)
                editing = HrtPlan(UUID.randomUUID().toString(), sub?.id.orEmpty(), reminder.substanceName, consumer, "", reminder.dose ?: Double.NaN, reminder.units.orEmpty(), now, 24)
                migrationId = reminder.id
                archiveKey = null
                editor = true
            }) { Text("从旧提醒建立计划：${reminder.substanceName}") }
        }
        settings.filter { setting -> setting.id.startsWith("legacy:reminder:") && settings.none { it.id == "legacy-reviewed:${setting.id}" } }.forEach { archived ->
            val decoded = runCatching { JSONObject(archived.payload).getJSONObject("record") }
            val record = decoded.getOrNull()
            if (record == null) Text("旧提醒归档无法读取", color = MaterialTheme.colorScheme.error)
            else {
                Text("旧提醒待复核：${record.optString("substanceName")}")
                Text("原始时点值：${record.optLong("reminderTime")} · ${record.optString("repeatMode")}")
                TextButton({
                    val sub = viewModel.repo.substances.getSubstance(record.optString("substanceName"))
                    editing = HrtPlan(UUID.randomUUID().toString(), sub?.id.orEmpty(), record.optString("substanceName"), consumer, "",
                        record.optDouble("dose"), if (record.isNull("units")) "" else record.optString("units"), now, 24)
                    migrationId = null; archiveKey = archived.id; editor = true
                }) { Text("确认时点并建立计划") }
            }
        }
    }
    if (editor) {
        val previous = editing
        val oldReminder = legacy.singleOrNull { it.id == migrationId }
        val rule = if (oldReminder != null) PlanSchedule(mode = ScheduleMode.DAYS, interval = 1, times = listOf(oldReminder.reminderTime.toString()))
        else previous?.let { runCatching { PlanSchedule.read(it, settings) }.getOrNull() } ?: PlanSchedule()
        PlanEditor(viewModel.medications, previous, rule, consumer, stocks, settings, viewModel::matchingMedications, viewModel.errorMessage, { editor = false }) { plan, schedule ->
            val sourceArchive = archiveKey
            if (sourceArchive != null) viewModel.run {
                viewModel.repo.database.withTransaction {
                    viewModel.repo.savePlan(plan, schedule)
                    viewModel.repo.dao.save(HrtModelSetting("legacy-reviewed:$sourceArchive", plan.id))
                }
                editor = false
            }
            else if (oldReminder == null) viewModel.savePlan(plan, schedule) { editor = false }
            else {
                viewModel.migrateReminder(oldReminder, plan, schedule) { editor = false }
            }
        }
    }
    history?.let { id ->
        val revisions = settings.filter { it.id.startsWith("plan-revision:$id:") }.mapNotNull { runCatching { Json.decodeFromString<PlanRevision>(it.payload) }.getOrNull() }.sortedByDescending { it.time }
        AlertDialog(onDismissRequest = { history = null }, title = { Text("计划变更历史") }, text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                if (revisions.isEmpty()) Text("尚无变更")
                revisions.forEach { revision -> Text("${format.format(Instant.ofEpochMilli(revision.time))} · ${revision.plan.dose} ${revision.plan.unit} · ${revision.schedule.mode} · ${if (revision.plan.paused) "暂停" else "启用"}") }
            }
        }, confirmButton = { TextButton({ history = null }) { Text("关闭") } })
    }
}
