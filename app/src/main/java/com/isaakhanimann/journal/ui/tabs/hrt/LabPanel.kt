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
    if (form) LabEditor(editing, consumer, viewModel.errorMessage, { form = false }) { lab -> viewModel.run { viewModel.repo.saveLab(lab); form = false } }
    deleting?.let { lab -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除这条化验结果？") }, confirmButton = {
        TextButton({ viewModel.run { viewModel.repo.dao.delete(lab); deleting = null } }) { Text("删除") }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabEditor(existing: HrtLab?, consumer: String?, operationError: String?, onClose: () -> Unit, onSave: (HrtLab) -> Unit) {
    val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    var analyte by remember { mutableStateOf(existing?.analyte ?: "E2") }
    var value by remember { mutableStateOf(existing?.let { "${if (it.comparison == "=") "" else it.comparison}${it.value}" } ?: "") }
    var unit by remember { mutableStateOf(existing?.unit ?: "pg/mL") }
    var time by remember { mutableStateOf(Instant.ofEpochMilli(existing?.time ?: System.currentTimeMillis()).atZone(ZoneId.systemDefault()).format(format)) }
    var method by remember { mutableStateOf(existing?.method ?: "") }
    var range by remember { mutableStateOf(existing?.referenceRange ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onClose, title = { Text("化验结果") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow { LabTrends.analytes.forEach { name -> FilterChip(analyte == name, { analyte = name }, label = { Text(name) }) } }
            OutlinedTextField(analyte, { analyte = it }, label = { Text("指标（可自定义）") })
            OutlinedTextField(value, { value = it }, label = { Text("结果，支持 <、>、<=、>=") })
            OutlinedTextField(unit, { unit = it }, label = { Text("报告单位") })
            OutlinedTextField(time, { time = it }, label = { Text("采样时间 · yyyy-MM-ddTHH:mm") })
            OutlinedTextField(method, { method = it }, label = { Text("检测方法（可选）") })
            OutlinedTextField(range, { range = it }, label = { Text("报告参考区间（可选）") })
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            operationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton({
        try {
            val text = value.trim()
            val comparison = listOf("<=", ">=", "<", ">", "=").firstOrNull { text.startsWith(it) } ?: "="
            val number = if (text.startsWith(comparison)) text.removePrefix(comparison).trim() else text
            val lab = HrtLab(existing?.id ?: UUID.randomUUID().toString(), existing?.consumer ?: consumer, analyte,
                LocalDateTime.parse(time, format).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), number.toDouble(), unit, comparison, method, range)
            LabTrends.validate(lab); onSave(lab)
        } catch (exception: RuntimeException) { error = exception.message ?: "输入无效" }
    }) { Text("保存") } }, dismissButton = { TextButton(onClose) { Text("取消") } })
}
