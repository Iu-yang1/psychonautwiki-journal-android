package com.isaakhanimann.journal.ui.tabs.hrt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.isaakhanimann.journal.data.hrt.AntiandrogenOptions
import com.isaakhanimann.journal.data.hrt.HrtLab
import com.isaakhanimann.journal.data.hrt.LabTrends
import com.isaakhanimann.journal.data.hrt.TestosteroneOptions
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
    labs: List<HrtLab>,
    onSave: (HormoneModelOptions) -> Unit,
    onClose: () -> Unit,
) {
    var baseline by remember { mutableStateOf(options.baselineE2PgMl.toString()) }
    var calibrationStart by remember { mutableStateOf(options.calibrationStartMillis?.let(::formatModelDate).orEmpty()) }
    val current = options.testosterone
    var enabled by remember { mutableStateOf(current != null) }
    var start by remember { mutableStateOf(formatModelDate(current?.startMillis ?: System.currentTimeMillis())) }
    var exactStart by remember { mutableStateOf(current?.startMillis) }
    var initial by remember { mutableStateOf(current?.initialNgDl?.toString().orEmpty()) }
    var unsuppressed by remember { mutableStateOf(current?.unsuppressedNgDl?.toString().orEmpty()) }
    var floor by remember { mutableStateOf(current?.floorNgDl?.toString().orEmpty()) }
    var ec50 by remember { mutableStateOf(current?.ec50PgMl?.toString().orEmpty()) }
    var effect by remember { mutableStateOf(current?.effectHours?.toString().orEmpty()) }
    var response by remember { mutableStateOf(current?.responseHours?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val cyproterone = remember { AntiandrogenInputs(current?.cyproterone, 43.9) }
    val spironolactone = remember { AntiandrogenInputs(current?.spironolactone, 16.5) }
    val latest = remember(labs) {
        labs.filter { it.time <= System.currentTimeMillis() && it.comparison == "=" && LabTrends.analyteKey(it.analyte) == "总睾酮" }
            .mapNotNull { runCatching { LabTrends.normalize(it) }.getOrNull()?.takeIf { point -> point.unit == "ng/dL" } }
            .maxByOrNull { it.original.time }
    }
    fun number(text: String, label: String) = text.trim().toDoubleOrNull()
        ?.takeIf { it.isFinite() } ?: throw IllegalArgumentException("请填写有效的$label")
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.padding(horizontal = 16.dp).widthIn(max = 560.dp), shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("模型参数", style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(baseline, { baseline = it }, label = { Text("个人 E2 基线（pg/mL）") }, singleLine = true)
                    OutlinedTextField(calibrationStart, { calibrationStart = it }, label = { Text("本阶段校准起点（可留空）") },
                        placeholder = { Text("2026-11-15 08:00") }, singleLine = true)
                    Text("填写本地时间。校准只使用此后明确选用的化验，较早曲线保留研究幅度。", style = MaterialTheme.typography.bodySmall)
                    Row {
                        Checkbox(enabled, { enabled = it })
                        Text("显示总睾酮情景")
                    }
                    if (enabled) {
                        Text("以下为手动情景参数，尚无自动个体拟合。起算值可取自化验；未抑制水平和下限需反映个人性腺状态。", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(start, { start = it; exactStart = null }, label = { Text("起算时间") }, singleLine = true)
                        OutlinedTextField(initial, { initial = it }, label = { Text("起算总 T（ng/dL）") }, singleLine = true)
                        latest?.let { lab ->
                            TextButton({
                                start = formatModelDate(lab.original.time)
                                exactStart = lab.original.time
                                initial = lab.value.toString()
                            }) { Text("使用最近的总睾酮化验") }
                        }
                        OutlinedTextField(unsuppressed, { unsuppressed = it }, label = { Text("未抑制总 T（ng/dL）") }, singleLine = true)
                        OutlinedTextField(floor, { floor = it }, label = { Text("抑制下限（ng/dL）") }, singleLine = true)
                        OutlinedTextField(ec50, { ec50 = it }, label = { Text("E2 半最大抑制浓度（pg/mL）") }, singleLine = true)
                        OutlinedTextField(effect, { effect = it }, label = { Text("E2 反馈时间常数（小时）") }, singleLine = true)
                        OutlinedTextField(response, { response = it }, label = { Text("总 T 响应时间常数（小时）") }, singleLine = true)
                        Text("历史抗雄用药按实际剂量、时间及残留计算，包括显示范围之前的记录。未填写的药物会在纳入详情中标注。", style = MaterialTheme.typography.bodySmall)
                        AntiandrogenFields("CPA", cyproterone)
                        AntiandrogenFields("螺内酯", spironolactone)
                        Text("半衰期参考说明书：CPA 43.9小时；螺内酯以活性代谢物canrenone的16.5小时作为残留代理。并非经验证的抗雄药效半衰期。半最大暴露与抑制强度无通用默认值；螺内酯受体阻断不代表总T下降，可将总T最大抑制比例设为0。", style = MaterialTheme.typography.bodySmall)
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClose) { Text("取消") }
                    TextButton({
                        try {
                            val config = if (enabled) TestosteroneOptions(
                                exactStart ?: parseModelDate(start), number(initial, "起算总 T"), number(unsuppressed, "未抑制总 T"),
                                number(floor, "抑制下限"), number(ec50, "半最大抑制浓度"),
                                number(effect, "反馈时间常数"), number(response, "响应时间常数"),
                                cyproterone.options("CPA"), spironolactone.options("螺内酯"),
                            ) else null
                            require(config == null || config.startMillis <= System.currentTimeMillis()) { "起算时间应早于当前时间" }
                            val cutoff = if (calibrationStart == options.calibrationStartMillis?.let(::formatModelDate).orEmpty()) {
                                options.calibrationStartMillis
                            } else calibrationStart.takeIf { it.isNotBlank() }?.let(::parseModelDate)
                            val result = HormoneModelOptions(number(baseline, "E2 基线"), cutoff, config)
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

private class AntiandrogenInputs(current: AntiandrogenOptions?, referenceHalfLife: Double) {
    var enabled by mutableStateOf(current != null)
    var halfLife by mutableStateOf((current?.halfLifeHours ?: referenceHalfLife).toString())
    var halfSuppression by mutableStateOf(current?.halfSuppressionMgDay?.toString().orEmpty())
    var maximum by mutableStateOf(current?.maximumSuppression?.toString().orEmpty())

    fun options(label: String): AntiandrogenOptions? {
        if (!enabled) return null
        fun parse(text: String) = text.trim().toDoubleOrNull() ?: throw IllegalArgumentException("请填写有效的$label 情景参数")
        return AntiandrogenOptions(parse(halfLife), parse(halfSuppression), parse(maximum)).also { it.validate() }
    }
}

@Composable
private fun AntiandrogenFields(label: String, fields: AntiandrogenInputs) {
    Row {
        Checkbox(fields.enabled, { fields.enabled = it })
        Text("纳入${label}历史记录")
    }
    if (fields.enabled) {
        OutlinedTextField(fields.halfLife, { fields.halfLife = it }, label = { Text("$label 残留半衰期（小时）") }, singleLine = true)
        OutlinedTextField(fields.halfSuppression, { fields.halfSuppression = it }, label = { Text("$label 半最大抑制等效暴露（mg/天）") }, singleLine = true)
        OutlinedTextField(fields.maximum, { fields.maximum = it }, label = { Text("$label 总 T 最大抑制比例（0–1）") }, singleLine = true)
    }
}
