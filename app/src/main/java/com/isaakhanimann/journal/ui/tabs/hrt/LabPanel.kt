package com.isaakhanimann.journal.ui.tabs.hrt

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.data.hrt.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabPanel(viewModel: HrtViewModel, consumer: String?) {
    val labs by viewModel.labs.collectAsState()
    val decoded = remember(labs, consumer) { runCatching { LabTrends.series(labs, consumer) } }
    val series = decoded.getOrNull().orEmpty()
    var selected by remember(consumer) { mutableStateOf<Pair<String, String>?>(null) }
    val key = selected?.takeIf { it in series } ?: series.keys.firstOrNull()
    var editing by remember { mutableStateOf<HrtLab?>(null) }
    var form by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<HrtLab?>(null) }
    val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton({ editing = null; form = true }) { Text("添加化验") }
        if (decoded.isFailure) {
            Text("化验数据无法换算：${decoded.exceptionOrNull()?.message}", color = MaterialTheme.colorScheme.error)
            labs.filter { it.consumer == consumer }.forEach { lab -> TextButton({ editing = lab; form = true }) { Text("修正：${lab.analyte} · ${lab.value} ${lab.unit}") } }
        }
        if (series.isEmpty()) Text("暂无化验结果")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            series.keys.forEach { group -> FilterChip(group == key, { selected = group }, label = { Text("${group.first} · ${group.second}") }) }
        }
        if (key != null) {
            val points = series.getValue(key)
            Text("${key.first}（${key.second}）", style = MaterialTheme.typography.titleMedium)
            LabTrendChart(points)
            points.asReversed().forEach { point ->
                val lab = point.original
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                    Text("${format.format(Instant.ofEpochMilli(lab.time))} · ${lab.comparison}${lab.value} ${lab.unit}")
                    if (lab.unit != point.unit) Text("换算：${lab.comparison}${"%.6g".format(point.value)} ${point.unit}")
                    if (lab.method.isNotBlank()) Text("检测方法：${lab.method}")
                    if (lab.referenceRange.isNotBlank()) Text("报告参考区间：${lab.referenceRange}")
                    Row {
                        TextButton({ editing = lab; form = true }) { Text("编辑") }
                        TextButton({ deleting = lab }) { Text("删除") }
                    }
                } }
            }
        }
    }
    if (form) LabEditor(viewModel, editing, consumer, { form = false })
    deleting?.let { lab -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除这条化验结果？") }, confirmButton = {
        TextButton({ viewModel.run { viewModel.repo.deleteLab(lab); deleting = null } }) { Text("删除") }
    }, dismissButton = { TextButton({ deleting = null }) { Text("取消") } }) }
}

@Composable
fun LabTrendChart(points: List<NormalizedLab>) {
    if (points.isEmpty()) return
    val color = MaterialTheme.colorScheme.primary
    val boundsColor = MaterialTheme.colorScheme.tertiary
    val axis = MaterialTheme.colorScheme.outline
    val minTime = points.minOf { it.original.time }
    val maxTime = points.maxOf { it.original.time }
    val top = points.maxOf { it.value }.coerceAtLeast(1.0)
    Text("0 – ${"%.2f".format(top)} ${points.first().unit}", style = MaterialTheme.typography.labelSmall)
    Canvas(Modifier.fillMaxWidth().height(160.dp).semantics { contentDescription = "化验趋势，${points.size} 个实测点；不等式结果以空心圆标示，不参与连线" }) {
        val inset = 10.dp.toPx()
        val width = size.width - 2 * inset
        val height = size.height - 2 * inset
        drawLine(axis, Offset(inset, inset), Offset(inset, inset + height))
        drawLine(axis, Offset(inset, inset + height), Offset(inset + width, inset + height))
        var previous: Offset? = null
        points.forEach { point ->
            val x = if (maxTime == minTime) width / 2 else width * ((point.original.time - minTime).toDouble() / (maxTime - minTime)).toFloat()
            val position = Offset(inset + x, inset + height * (1 - point.value / top).toFloat())
            if (point.original.comparison == "=") {
                previous?.let { drawLine(color, it, position, 2.dp.toPx()) }
                drawCircle(color, 4.dp.toPx(), position)
                previous = position
            } else {
                drawCircle(boundsColor, 5.dp.toPx(), position, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                previous = null
            }
        }
    }
    val format = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault())
    Row { Text(format.format(Instant.ofEpochMilli(minTime)), style = MaterialTheme.typography.labelSmall); Spacer(Modifier.weight(1f)); Text(format.format(Instant.ofEpochMilli(maxTime)), style = MaterialTheme.typography.labelSmall) }
}

data class LabInputDraft(val id: String = UUID.randomUUID().toString(), val value: String = "", val unit: String = "pg/mL", val method: String = "", val range: String = "")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabEditor(viewModel: HrtViewModel, existing: HrtLab?, consumer: String?, onClose: () -> Unit) {
    val events by viewModel.events.collectAsState()
    val links by viewModel.links.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var analyte by remember(existing?.id) { mutableStateOf(existing?.analyte ?: "E2") }
    val drafts = remember(existing?.id) { mutableStateMapOf<String, LabInputDraft>().apply {
        if (existing != null) put(existing.analyte, LabInputDraft(existing.id, "${if (existing.comparison == "=") "" else existing.comparison}${existing.value}", existing.unit, existing.method, existing.referenceRange))
    } }
    val draft = drafts.getOrPut(analyte) { LabInputDraft(unit = labUnitPresets(analyte).first()) }
    var sample by remember(existing?.id) { mutableStateOf(Instant.ofEpochMilli(existing?.time ?: System.currentTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDateTime().withSecond(0).withNano(0)) }
    val sampleMillis = sample.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val effectiveConsumer = if (existing != null) existing.consumer else consumer
    val ids = viewModel.medications.filter { it.identity?.ingredientId == "ingredient:estradiol" }.map { it.id }.toSet()
    val linkMap = links.associateBy { it.ingestionId }
    val lastDose = events.filter { it.consumerName == effectiveConsumer && it.time.toEpochMilli() <= sampleMillis && linkMap[it.id]?.medicationId in ids }.maxByOrNull { it.time }
    val previous = existing?.let { LabDoseContext.read(it.id, settings) }
    var elapsed by remember(sampleMillis, lastDose?.id) { mutableStateOf(previous?.takeIf { existing?.time == sampleMillis && it.lastDoseId == lastDose?.id }?.elapsedHours?.toString() ?: lastDose?.let { "%.3f".format(java.util.Locale.ROOT, (sampleMillis-it.time.toEpochMilli())/3600000.0) }.orEmpty()) }
    var calibrate by remember(existing?.id) { mutableStateOf(previous?.calibrate ?: true) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onClose, title = { Text("化验结果") }, text = {
        Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabResultFields(analyte, { analyte = it }, drafts)
            Text("采样时间", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                com.isaakhanimann.journal.ui.tabs.journal.addingestion.time.DatePickerButton(sample, { sample = it.withSecond(0).withNano(0) }, sample.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                com.isaakhanimann.journal.ui.tabs.journal.addingestion.time.TimePickerButton(sample, { sample = it.withSecond(0).withNano(0) }, sample.format(DateTimeFormatter.ofPattern("HH:mm")))
            }
            if (LabTrends.analyteKey(analyte) == "E2") {
                Row { Checkbox(calibrate, { calibrate = it }, enabled = lastDose != null); Text("按化验校准曲线") }
                OutlinedTextField(elapsed, { elapsed = it }, label = { Text("距上次雌激素用药（小时）") })
                if (lastDose != null) Text("上次用药：${lastDose.substanceName} · ${lastDose.time.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))}", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(draft.method, { drafts[analyte] = draft.copy(method = it) }, label = { Text("检测方法（可选）") })
            OutlinedTextField(draft.range, { drafts[analyte] = draft.copy(range = it) }, label = { Text("报告参考区间（可选）") })
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            viewModel.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton({
        try {
            val labs = drafts.filterValues { it.value.isNotBlank() }.map { (name, input) ->
                val text = input.value.trim().replace("≤", "<=").replace("≥", ">=")
                val comparison = listOf("<=", ">=", "<", ">", "=").firstOrNull { text.startsWith(it) } ?: "="
                val number = if (text.startsWith(comparison)) text.removePrefix(comparison).trim() else text
                HrtLab(input.id, effectiveConsumer, name.trim(), sampleMillis, number.replace(',', '.').toDouble(), input.unit, comparison, input.method, input.range).also(LabTrends::validate)
            }
            require(labs.isNotEmpty()) { "请输入检测结果" }
            val contexts = labs.map { lab ->
                val enabled = LabTrends.analyteKey(lab.analyte) == "E2" && calibrate && lastDose != null && lab.comparison == "="
                val hours = elapsed.takeIf { enabled }?.replace(',', '.')?.toDouble()
                if (enabled) {
                    require(hours != null && hours.isFinite() && hours >= 0) { "请填写距上次用药时间" }
                    require(kotlin.math.abs(sampleMillis - requireNotNull(lastDose).time.toEpochMilli() - hours * 3600000) <= 60000) { "间隔与用药记录不一致，请核对时间" }
                }
                LabDoseContext(lab.id, hours, if (enabled) lastDose?.id else null, if (enabled) lastDose?.time?.toEpochMilli() else null, enabled)
            }
            viewModel.run { viewModel.repo.saveLabs(labs, contexts); onClose() }
        } catch (exception: RuntimeException) { error = exception.message ?: "输入无效" }
    }) { Text(if (existing == null) "保存已填写项" else "保存") } }, dismissButton = { TextButton(onClose) { Text("取消") } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabResultFields(analyte: String, onAnalyte: (String) -> Unit, drafts: androidx.compose.runtime.snapshots.SnapshotStateMap<String, LabInputDraft>) {
    val draft = drafts.getOrPut(analyte) { LabInputDraft(unit = labUnitPresets(analyte).first()) }
            FlowRow { LabTrends.analytes.forEach { name -> FilterChip(analyte == name, { onAnalyte(name) }, label = { Text(name) }) } }
            OutlinedTextField(analyte, { onAnalyte(it) }, label = { Text("指标") })
            OutlinedTextField(draft.value, { drafts[analyte] = draft.copy(value = it) }, label = { Text("结果") }, supportingText = { Text("支持 <、>、≤、≥") })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) { labUnitPresets(analyte).forEach { preset -> FilterChip(draft.unit == preset, { drafts[analyte] = draft.copy(unit = preset) }, label = { Text(preset) }) } }
            OutlinedTextField(draft.unit, { drafts[analyte] = draft.copy(unit = it) }, label = { Text("报告单位") })
}
