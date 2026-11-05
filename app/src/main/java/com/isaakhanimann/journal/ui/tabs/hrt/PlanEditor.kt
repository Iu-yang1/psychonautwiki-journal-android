package com.isaakhanimann.journal.ui.tabs.hrt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.data.hrt.*
import com.isaakhanimann.journal.data.substances.classes.Substance
import com.isaakhanimann.journal.data.substances.classes.availableRoutesForIngestion
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanEditor(
    medications: List<Substance>,
    existing: HrtPlan?,
    initialRule: PlanSchedule,
    consumer: String?,
    stock: List<HrtStock>,
    settings: List<HrtModelSetting>,
    search: (String) -> List<Substance>,
    operationError: String?,
    onClose: () -> Unit,
    onSave: (HrtPlan, PlanSchedule) -> Unit,
) {
    val zone = if (initialRule.zone == "local") ZoneId.systemDefault() else ZoneId.of(initialRule.zone)
    val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    var medicationId by remember { mutableStateOf(existing?.medicationId ?: "") }
    var route by remember { mutableStateOf(existing?.route ?: "") }
    var dose by remember { mutableStateOf(existing?.dose?.takeIf { it.isFinite() }?.toString() ?: "") }
    var unit by remember { mutableStateOf(existing?.unit ?: "mg") }
    var mode by remember { mutableStateOf(initialRule.mode) }
    var interval by remember { mutableStateOf(initialRule.interval.toString()) }
    var times by remember { mutableStateOf(initialRule.times.joinToString(",")) }
    var weekdays by remember { mutableStateOf(initialRule.weekdays) }
    var start by remember { mutableStateOf(Instant.ofEpochMilli(existing?.start ?: System.currentTimeMillis()).atZone(zone).format(format)) }
    var end by remember { mutableStateOf(initialRule.end?.let { Instant.ofEpochMilli(it).atZone(zone).format(format) } ?: "") }
    var notifications by remember { mutableStateOf(initialRule.notifications) }
    var stockId by remember { mutableStateOf(initialRule.stockId) }
    var productId by remember { mutableStateOf(initialRule.productId) }
    var doseBasis by remember { mutableStateOf(initialRule.doseBasis) }
    var applicationSite by remember { mutableStateOf(initialRule.applicationSite.orEmpty()) }
    var patchWearHours by remember { mutableStateOf(initialRule.patchWearHours?.toString() ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    val medication = medications.singleOrNull { it.id == medicationId }
    AlertDialog(onDismissRequest = onClose, title = { Text(if (existing == null) "添加用药计划" else "编辑计划") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (existing?.route == "") Text("从旧提醒转换：请重新确认频率、途径和时点。保存后停用旧提醒。")
            Text(medication?.name ?: "选择药物")
            OutlinedTextField(query, { query = it }, label = { Text("查找药物") }, singleLine = true)
            if (medication == null || query.isNotBlank()) {
                search(query).take(12).forEach { sub ->
                    TextButton({ medicationId = sub.id; route = ""; stockId = null; productId = null; doseBasis = null; query = "" }) { Text(sub.name) }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                medication?.availableRoutesForIngestion()?.forEach { value -> FilterChip(route == value.name, { route = value.name }, label = { Text(value.displayText) }) }
            }
            Row {
                OutlinedTextField(dose, { dose = it }, label = { Text("数量") }, modifier = Modifier.weight(1f))
                OutlinedTextField(unit, { unit = it; stockId = null }, label = { Text("单位") }, modifier = Modifier.weight(1f))
            }
            FlowRow {
                listOf(ScheduleMode.HOURS to "每 N 小时", ScheduleMode.DAYS to "每 N 天", ScheduleMode.WEEKDAYS to "指定星期").forEach { (value, title) ->
                    FilterChip(mode == value, { mode = value; interval = if (value == ScheduleMode.HOURS) "24" else "1" }, label = { Text(title) })
                }
            }
            if (mode != ScheduleMode.WEEKDAYS) OutlinedTextField(interval, { interval = it }, label = { Text("间隔") })
            if (mode != ScheduleMode.HOURS) OutlinedTextField(times, { times = it }, label = { Text("时点，用逗号分隔，如 08:00,20:00") })
            if (mode == ScheduleMode.WEEKDAYS) FlowRow {
                (1..7).forEach { day -> FilterChip(day in weekdays, { weekdays = if (day in weekdays) weekdays - day else weekdays + day }, label = { Text(listOf("一", "二", "三", "四", "五", "六", "日")[day - 1]) }) }
            }
            OutlinedTextField(start, { start = it }, label = { Text("开始 · yyyy-MM-ddTHH:mm") })
            OutlinedTextField(end, { end = it }, label = { Text("结束（可选）· yyyy-MM-ddTHH:mm") })
            Row { Checkbox(notifications, { notifications = it }); Text("提醒") }
            Text("药品规格")
            FilterChip(productId == null, { productId = null }, label = { Text("暂不指定") })
            MedicationProducts.all(settings).filter { !it.archived && it.medicationId == medicationId }.forEach { product ->
                FilterChip(productId == product.id && stockId == null, { productId = product.id; stockId = null }, label = { Text(product.name) })
            }
            Text("默认扣减库存")
            FilterChip(stockId == null, { stockId = null }, label = { Text("不扣减") })
            stock.filter { it.medicationId == medicationId && settings.none { setting -> setting.id == "stock-status:${it.id}" && setting.payload == "archived" } }.forEach { item ->
                FilterChip(stockId == item.id, { stockId = item.id; productId = null }, label = { Text("${item.name} · ${item.unit} · ${item.id.take(6)}") })
            }
            val chosenProduct = stockId?.let { MedicationProducts.forStock(it, settings) }
                ?: MedicationProducts.all(settings).singleOrNull { it.id == productId }
            Text("剂量口径")
            FilterChip(doseBasis == null, { doseBasis = null }, label = { Text("按单位与规格判断") })
            FilterChip(doseBasis == DoseBasis.PRODUCT, { doseBasis = DoseBasis.PRODUCT }, label = { Text("制剂用量") })
            FilterChip(doseBasis == DoseBasis.CHEMICAL_FORM, { doseBasis = DoseBasis.CHEMICAL_FORM }, label = { Text("所选药物质量") })
            if (medication?.identity?.ingredientId == "ingredient:estradiol") {
                FilterChip(doseBasis == DoseBasis.ACTIVE_E2, { doseBasis = DoseBasis.ACTIVE_E2 }, label = { Text("母体雌二醇当量（质量单位）") })
            }
            val resolved = MedicationProducts.resolve(dose.toDoubleOrNull(), unit, medicationId, chosenProduct, doseBasis)
            Text(resolved.missing ?: resolved.activeE2Mg?.let { "雌二醇当量：$it mg E2" } ?: resolved.chemicalFormMg?.let { "所选药物含量：$it mg" } ?: "释放速率：${resolved.patchReleaseMcgDay} μg/天")
            if (medication?.identity?.formulationId in setOf("form:gel", "form:patch", "form:oil-injection", "form:spray")) {
                OutlinedTextField(applicationSite, { applicationSite = it }, label = { Text("给药部位") })
            }
            if (medication?.identity?.formulationId == "form:patch") {
                OutlinedTextField(patchWearHours, { patchWearHours = it }, label = { Text("计划模拟佩戴时长（小时）") })
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            operationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton({
            try {
                val sub = requireNotNull(medication) { "请选择药物" }
                require(sub.availableRoutesForIngestion().any { it.name == route }) { "请选择给药途径" }
                val amount = dose.toDouble()
                require(amount.isFinite() && amount > 0 && unit.isNotBlank()) { "数量或单位无效" }
                val startMs = LocalDateTime.parse(start, format).atZone(zone).toInstant().toEpochMilli()
                val rule = PlanSchedule(mode, interval.toInt(), times.split(',').map(String::trim).filter(String::isNotEmpty), weekdays,
                    end.takeIf(String::isNotBlank)?.let { LocalDateTime.parse(it, format).atZone(zone).toInstant().toEpochMilli() }, stockId = stockId, notifications = notifications,
                    anchorDate = LocalDateTime.parse(start, format).toLocalDate().toString(), productId = productId, zone = initialRule.zone,
                    applicationSite = applicationSite.ifBlank { null }, patchWearHours = patchWearHours.takeIf(String::isNotBlank)?.toDouble(), doseBasis = doseBasis)
                rule.validate(startMs)
                val plan = HrtPlan(existing?.id ?: UUID.randomUUID().toString(), sub.id, sub.name, existing?.consumer ?: consumer, route, amount, unit, startMs,
                    if (mode == ScheduleMode.HOURS) rule.interval else 24, existing?.paused ?: false)
                onSave(plan, rule)
            } catch (exception: RuntimeException) { error = exception.message ?: "输入无效" }
        }) { Text("保存") }
    }, dismissButton = { TextButton(onClose) { Text("取消") } })
}
