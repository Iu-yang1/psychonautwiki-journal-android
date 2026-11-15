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
        Text(if (data.testosteroneScenarios.isEmpty()) "总睾酮 · 实测" else "总睾酮 · 参数情景", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.weight(1f))
        TextButton({ molar = !molar }) { Text(unit) }
    }
    Text("0 – ${"%.4g".format(top * factor)} $unit", style = MaterialTheme.typography.labelSmall)
    if (data.testosteroneScenarios.isNotEmpty()) {
        Text("E2 延迟反馈 · 纳入 ${data.antiandrogenRecords.count { it.reason == null }} 条历史CPA/螺内酯 · 手动参数情景，未经个体预测验证",
            style = MaterialTheme.typography.bodySmall)
    }
    Canvas(Modifier.fillMaxWidth().height(200.dp)
        .semantics { contentDescription = "总睾酮情景曲线和独立化验点，可点按查看数值" }
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
    if (data.testosteroneScenarios.size > 1) data.testosteroneScenarios.forEachIndexed { index, scenario ->
        Text(scenario.label, color = colors[index % colors.size], style = MaterialTheme.typography.labelSmall)
    }
    selected?.let {
        Text("${testosteroneTime(it.time)} · ${it.label} ${"%.6g".format(it.value * factor)} $unit")
    }
}
