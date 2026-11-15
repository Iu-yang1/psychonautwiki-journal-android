package com.isaakhanimann.journal.ui.tabs.hrt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.isaakhanimann.journal.data.hrt.HormoneModelOptions
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

private val modelDateFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT)

private fun formatModelDate(time: Long): String =
    modelDateFormat.format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()))

private fun parseModelDate(text: String): Long =
    LocalDateTime.parse(text.trim(), modelDateFormat).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

@Composable
fun HormoneModelEditor(
    options: HormoneModelOptions,
    onSave: (HormoneModelOptions) -> Unit,
    onClose: () -> Unit,
) {
    var baseline by remember { mutableStateOf(options.baselineE2PgMl.toString()) }
    var calibrationStart by remember { mutableStateOf(options.calibrationStartMillis?.let(::formatModelDate).orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.padding(horizontal = 16.dp).widthIn(max = 560.dp), shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("E2 校准设置", style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("以下为可选的个人 E2 校准设置。保留默认值即可使用原有参考曲线。", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(baseline, { baseline = it }, label = { Text("个人 E2 基线（pg/mL）") }, singleLine = true,
                        supportingText = { Text("默认 0 表示不额外添加基线，不代表实测 E2 为 0。") })
                    OutlinedTextField(calibrationStart, { calibrationStart = it }, label = { Text("本阶段校准起点（可留空）") },
                        placeholder = { Text("2026-11-15 08:00") }, singleLine = true)
                    Text("填写本地时间。校准只使用此后明确选用的化验，较早曲线保留研究幅度。", style = MaterialTheme.typography.bodySmall)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClose) { Text("取消") }
                    TextButton({
                        try {
                            val cutoff = if (calibrationStart == options.calibrationStartMillis?.let(::formatModelDate).orEmpty()) {
                                options.calibrationStartMillis
                            } else calibrationStart.takeIf { it.isNotBlank() }?.let(::parseModelDate)
                            val value = baseline.trim().toDoubleOrNull() ?: throw IllegalArgumentException("请填写有效的 E2 基线")
                            val result = options.copy(baselineE2PgMl = value, calibrationStartMillis = cutoff)
                            result.validate()
                            onSave(result)
                        } catch (exception: IllegalArgumentException) {
                            error = exception.message
                        } catch (_: java.time.DateTimeException) {
                            error = "时间格式应为 yyyy-MM-dd HH:mm"
                        }
                    }) { Text("保存") }
                }
            }
        }
    }
}
