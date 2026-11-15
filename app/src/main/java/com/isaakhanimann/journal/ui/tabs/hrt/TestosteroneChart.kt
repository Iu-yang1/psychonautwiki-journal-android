package com.isaakhanimann.journal.ui.tabs.hrt

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.data.hrt.HormoneChartData
import com.isaakhanimann.journal.data.hrt.TestosteroneSample
import com.isaakhanimann.journal.data.hrt.TestosteroneReferences
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class TestosteroneHit(val time: Double, val value: Double, val label: String)

private fun testosteroneTime(hours: Double): String = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli((hours * 3600000).toLong()))

@Composable
fun TestosteroneChart(data: HormoneChartData) {
    var molar by remember { mutableStateOf(false) }
    var selected by remember(data, molar) { mutableStateOf<TestosteroneHit?>(null) }
    var sourcesOpen by remember { mutableStateOf(false) }
    val factor = if (molar) .0347 else 1.0
    val unit = if (molar) "nmol/L" else "ng/dL"
    val colors = listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary)
    val axis = MaterialTheme.colorScheme.outline
    val labColor = MaterialTheme.colorScheme.primary
    val top = maxOf(1.0,
        data.testosteroneScenarios.maxOfOrNull { scenario -> (scenario.history + scenario.forecast).maxOfOrNull { it.ngDl } ?: 0.0 } ?: 0.0,
        data.measuredTestosterone.maxOfOrNull { it.value } ?: 0.0)
    val span = data.throughHours - data.fromHours
    Row {
        Text(when {
            data.testosteroneScenarios.isNotEmpty() -> "总睾酮 · 文献参考与实测"
            data.measuredTestosterone.isNotEmpty() -> "总睾酮 · 实测"
            else -> "总睾酮 · 文献参考"
        }, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.weight(1f))
        TextButton({ molar = !molar }) { Text(unit) }
    }
    if (data.testosteroneReferenceNotes.isNotEmpty()) {
        Text("内置研究组均值 · 未经个人预测验证 · 个人化验独立显示", style = MaterialTheme.typography.bodySmall)
        Text("连线仅为随访点之间的参考；未来虚线假设持续研究方案，不随个人剂量或计划改变。", style = MaterialTheme.typography.bodySmall)
        if (data.testosteroneScenarios.isEmpty()) Text(data.testosteroneReferenceNotes.last(), style = MaterialTheme.typography.bodySmall)
    }
    TextButton({ sourcesOpen = true }) { Text("查看研究数据与来源") }
    if (sourcesOpen) TestosteroneReferenceSources { sourcesOpen = false }
    if (data.testosteroneScenarios.isEmpty() && data.measuredTestosterone.isEmpty()) return
    Text("0 – ${"%.4g".format(top * factor)} $unit", style = MaterialTheme.typography.labelSmall)
    Canvas(Modifier.fillMaxWidth().height(200.dp)
        .semantics { contentDescription = "总睾酮文献参考线和独立化验点，可点按查看数值" }
        .pointerInput(data, molar) {
            detectTapGestures { tap ->
                val inset = 10.dp.toPx()
                val width = size.width - 2 * inset
                val height = size.height - 2 * inset
                val time = data.fromHours + ((tap.x - inset) / width).coerceIn(0f, 1f) * span
                val labs = data.measuredTestosterone.map {
                    TestosteroneHit(it.original.time / 3600000.0, it.value, "实测 ${it.original.comparison}")
                }
                val curves = data.testosteroneScenarios.flatMap { scenario ->
                    (if (time <= data.nowHours) scenario.history else scenario.forecast).map {
                        TestosteroneHit(it.timeHours, it.ngDl, scenario.label)
                    }
                }
                selected = (labs + curves).minByOrNull {
                    val horizontal = inset + ((it.time - data.fromHours) / span * width).toFloat() - tap.x
                    val vertical = inset + ((1 - it.value / top) * height).toFloat() - tap.y
                    horizontal * horizontal + vertical * vertical
                }
            }
        }) {
        val inset = 10.dp.toPx()
        val width = size.width - 2 * inset
        val height = size.height - 2 * inset
        fun position(time: Double, value: Double) = Offset(
            inset + ((time - data.fromHours) / span * width).toFloat(),
            inset + ((1 - value / top) * height).toFloat())
        drawLine(axis, Offset(inset, inset), Offset(inset, inset + height))
        drawLine(axis, Offset(inset, inset + height), Offset(inset + width, inset + height))
        val nowX = position(data.nowHours, 0.0).x
        drawLine(axis, Offset(nowX, inset), Offset(nowX, inset + height),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())))
        fun path(samples: List<TestosteroneSample>) = Path().apply {
            samples.forEachIndexed { index, sample ->
                val point = position(sample.timeHours, sample.ngDl)
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        }
        data.testosteroneScenarios.forEachIndexed { index, scenario ->
            val color = colors[index % colors.size]
            drawPath(path(scenario.history), color, style = Stroke(2.dp.toPx()))
            drawPath(path(scenario.forecast), color, style = Stroke(2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))))
        }
        data.measuredTestosterone.forEach { lab ->
            val position = position(lab.original.time / 3600000.0, lab.value)
            if (lab.original.comparison == "=") drawCircle(labColor, 4.dp.toPx(), position)
            else drawCircle(labColor, 5.dp.toPx(), position, style = Stroke(2.dp.toPx()))
        }
        selected?.let { drawCircle(axis, 6.dp.toPx(), position(it.time, it.value), style = Stroke(1.dp.toPx())) }
    }
    Row {
        Text(testosteroneTime(data.fromHours), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.weight(1f))
        Text(testosteroneTime(data.throughHours), style = MaterialTheme.typography.labelSmall)
    }
    data.testosteroneScenarios.forEachIndexed { index, scenario ->
        Text(scenario.label, color = colors[index % colors.size], style = MaterialTheme.typography.labelSmall)
        scenario.referenceId?.let { id ->
            val reference = TestosteroneReferences.studies.single { it.id == id }
            Text("${reference.table} · n=${reference.participants} · 组均值",
                style = MaterialTheme.typography.labelSmall)
        }
        scenario.referenceStartHours?.let { start ->
            Text("按首条 E2／抗雄记录对齐：${testosteroneTime(start)}", style = MaterialTheme.typography.labelSmall)
        }
    }
    selected?.let {
        Text("${testosteroneTime(it.time)} · ${it.label} ${"%.6g".format(it.value * factor)} $unit")
    }
}
