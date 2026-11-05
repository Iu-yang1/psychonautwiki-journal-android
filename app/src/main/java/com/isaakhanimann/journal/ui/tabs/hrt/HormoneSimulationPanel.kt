package com.isaakhanimann.journal.ui.tabs.hrt

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.isaakhanimann.journal.R
import com.isaakhanimann.journal.data.hrt.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private fun modelTime(hours: Double): String = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli((hours * 3600000).toLong()))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HormoneSimulationPanel(parent: HrtViewModel, consumer: String?, onOpenRecords: () -> Unit, onAssociate: (Int) -> Unit, viewModel: HormoneSimulationViewModel = hiltViewModel()) {
    val preferences = LocalContext.current.getSharedPreferences("hormone_chart", 0)
    val chartPreferences = remember(preferences) { HormoneChartPreferences(preferences) }
    val saved = remember { chartPreferences.load() }
    var historyDays by remember { mutableIntStateOf(saved.historyDays) }
    var analyteId by remember { mutableStateOf(saved.analyteId) }
    var analyteMenu by remember { mutableStateOf(false) }
    var optionsOpen by remember { mutableStateOf(false) }
    var gelReference by remember { mutableStateOf(saved.gelReference) }
    var bridgeValerate by remember { mutableStateOf(saved.bridgeValerate) }
    var forecastDays by remember { mutableIntStateOf(saved.forecastDays) }
    var unit by remember { mutableStateOf(saved.unit) }
    var editId by remember { mutableStateOf<Int?>(null) }
    val state by viewModel.state.collectAsState()
    val records by parent.events.collectAsState()
    val links by parent.links.collectAsState()
    val settings by parent.settings.collectAsState()
    LaunchedEffect(consumer, historyDays, forecastDays, analyteId, gelReference, bridgeValerate, unit) {
        if (analyteId != "E2" && unit == "pmol/L" || analyteId == "E2" && unit == "ng/mL") unit = "pg/mL"
        chartPreferences.save(HormoneChartOptions(historyDays, forecastDays, analyteId, unit, gelReference, bridgeValerate))
        viewModel.configure(consumer, historyDays, forecastDays, analyteId, gelReference, bridgeValerate)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("激素曲线 · 估算", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton({ optionsOpen = true }) { Icon(Icons.Outlined.Tune, "曲线设置") }
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            TextButton({ analyteMenu = true }) { Text("${hormoneAnalyteLabel(analyteId)} ▾") }
            DropdownMenu(analyteMenu, { analyteMenu = false }) {
                viewModel.references.catalog.models.map { it.analyteId }.distinct().forEach { id ->
                    DropdownMenuItem(text = { Text(hormoneAnalyteLabel(id)) }, onClick = { analyteId = id; unit = chartPreferences.unitFor(id); analyteMenu = false })
                }
            }
        }
        TextButton({ optionsOpen = true }) { Text("近 $historyDays 天") }
        }
        when (val current = state) {
            HormoneChartState.Loading -> CircularProgressIndicator()
            is HormoneChartState.Failed -> { Text(current.message, color = MaterialTheme.colorScheme.error); TextButton(viewModel::retry) { Text("重新计算") } }
            is HormoneChartState.Ready -> {
                val data = current.data
                if (data.request != HormoneChartRequest(consumer, historyDays, forecastDays, analyteId, gelReference, bridgeValerate)) CircularProgressIndicator()
                else {
                    HormoneSimulationSummary(data, unit, viewModel.references.catalog, onOpenRecords) { id ->
                        if (links.singleOrNull { it.ingestionId == id }?.medicationId == null || data.excluded.any { it.id == id.toString() && it.reason == "needs_formulation" }) onAssociate(id)
                        else editId = id
                    }
                }
            }
        }
    }
    if (optionsOpen) AlertDialog(onDismissRequest = { optionsOpen = false }, title = { Text("曲线设置") }, text = {
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("历史范围")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1, 7, 30, 90, 180, 365).forEach { days -> FilterChip(historyDays == days, { historyDays = days }, label = { Text("近 $days 天") }) }
            }
            Text("未来情景")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(0, 3, 7, 30).forEach { days -> FilterChip(forecastDays == days, { forecastDays = days }, label = { Text(if (days == 0) "不显示" else "$days 天") }) }
            }
            Text("单位")
            Row { (if (analyteId == "E2") listOf("pg/mL", "pmol/L") else listOf("pg/mL", "ng/mL")).forEach { label -> FilterChip(unit == label, { unit = label }, label = { Text(label) }) } }
            if (analyteId == "E2") {
                Row { Checkbox(bridgeValerate, { bridgeValerate = it }); Text("含服戊酸雌二醇估算") }
                Text("凝胶参考")
                listOf("compare" to "比较两种凝胶参考", "estrogel" to "EstroGel上臂资料情景", "divigel" to "Divigel上大腿资料情景", "confirmed" to "仅使用逐条明确的信息").forEach { (key, label) ->
                    FilterChip(gelReference == key, { gelReference = key }, label = { Text(label) })
                }
            }
        }
    }, confirmButton = { TextButton({ optionsOpen = false }) { Text("完成") } })
    editId?.let { id ->
        val record = records.singleOrNull { it.id == id && it.consumerName == consumer }
        val link = links.singleOrNull { it.ingestionId == id }
        if (record != null && link?.medicationId != null) EventProductEditor(parent, record, link.medicationId, settings) { editId = null }
    }
}

@Composable
fun HormoneSimulationSummary(data: HormoneChartData, unit: String, catalog: HormoneReferenceCatalog, onOpenRecords: () -> Unit, onEdit: (Int) -> Unit) {
    var sourcesOpen by remember { mutableStateOf(false) }
    var excludedOpen by remember { mutableStateOf(false) }
    var tableOpen by remember { mutableStateOf(false) }
    var otherHrtOpen by remember { mutableStateOf(false) }
    var detailsMenu by remember { mutableStateOf(false) }
    var timelineOpen by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (data.history.isEmpty() && data.gelScenarios.isEmpty()) Text("暂无可纳入 ${data.request.analyteId} 模型的历史用药记录")
        if (data.history.isNotEmpty() || data.forecast.isNotEmpty() || data.measured.isNotEmpty() || data.gelScenarios.isNotEmpty()) HormoneChart(data, unit)
        val scenarioCount = data.gelScenarios.flatMap { it.recordIds }.distinct().size
        Text("${data.request.analyteId} 用药 ${data.includedActual.size + scenarioCount} 条${if (scenarioCount > 0) "" else ""} · 计划情景 ${data.includedPlanned.size} 次 · 实测 ${data.measured.size} 点")
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextButton({ otherHrtOpen = true }) { Text("已识别 ${data.recognizedHrt.size} 条") }
            Spacer(Modifier.weight(1f))
            Box {
                IconButton({ detailsMenu = true }) { Icon(Icons.Outlined.MoreVert, "曲线详情") }
                DropdownMenu(detailsMenu, { detailsMenu = false }) {
                    DropdownMenuItem(text = { Text("查看未纳入项（${data.excluded.size}）") }, onClick = { detailsMenu = false; excludedOpen = true })
                    DropdownMenuItem(text = { Text("数值表") }, onClick = { detailsMenu = false; tableOpen = true })
                    DropdownMenuItem(text = { Text("原始剂量时间线") }, onClick = { detailsMenu = false; timelineOpen = true })
                    DropdownMenuItem(text = { Text("历史数据适配") }, onClick = { detailsMenu = false; onOpenRecords() })
                }
            }
        }
    }
    if (timelineOpen) AlertDialog(onDismissRequest = { timelineOpen = false }, title = { Text("原始剂量时间线") }, text = {
        Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) { HormoneDoseTimeline(data) }
    }, confirmButton = { TextButton({ timelineOpen = false }) { Text("关闭") } })
    if (excludedOpen) HormoneExclusions(data.excluded, { excludedOpen = false }) { id -> onEdit(id); excludedOpen = false }
    if (sourcesOpen) HormoneSources(catalog, data.profileIds, data.assumptions) { sourcesOpen = false }
    if (tableOpen) HormoneValueTable(data, unit) { tableOpen = false }
    if (otherHrtOpen) AlertDialog(onDismissRequest = { otherHrtOpen = false }, title = { Text("已识别的 HRT 用药") }, text = {
        Column {
            Text("全部历史的已识别HRT用药；不受近7天等曲线范围限制。", style = MaterialTheme.typography.bodySmall)
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                data.recognizedHrt.forEach { record -> item(key = record.id) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text(record.name, style = MaterialTheme.typography.titleSmall)
                        Text("已识别 · ${record.category}")
                        Text("${record.dose ?: "?"} ${record.unit.orEmpty()} · ${modelTime(record.time / 3600000.0)}")
                        if (!record.inVisibleRange) Text("当前图窗之外", style = MaterialTheme.typography.labelSmall)
                        Text(when (record.modelReason) {
                            null -> "已纳入 ${data.request.analyteId} 曲线"
                            "gel_study_scenario" -> "已按有效mg计入曲线"
                            "sublingual_valerate_bridge" -> "已计入含服雌二醇估算"
                            "non_e2_record" -> "已保存，不计入当前曲线"
                            "other_analyte_record" -> "已识别，请切换到对应成分曲线"
                            else -> "${hormoneReason(record.modelReason)}"
                        }, style = MaterialTheme.typography.bodySmall)
                        if (record.modelReason in setOf("needs_formulation", "select_reference_profile", "missing_application_site", "missing_patch_release_rate", "missing_patch_wear_interval", "ambiguous_gel_mass_basis") || record.modelReason?.startsWith("missing_dose_spec:") == true) {
                            TextButton({ otherHrtOpen = false; onEdit(record.id) }) { Text("补全模拟信息") }
                        }
                    }
                    HorizontalDivider()
                } }
            }
        }
    }, confirmButton = { TextButton({ otherHrtOpen = false }) { Text("关闭") } })
}

private data class HormoneHit(val time: Double, val value: Double, val label: String)

@Composable
private fun HormoneDoseTimeline(data: HormoneChartData) {
    val groups = remember(data.doseRecords) { data.doseRecords.groupBy { Triple(it.name, it.route, it.unit) } }
    val color = MaterialTheme.colorScheme.primary
    val axis = MaterialTheme.colorScheme.outline
    Text("原始剂量时间线", style = MaterialTheme.typography.titleSmall)
    Text("下图为实际记录的药物用量，不是血药浓度。", style = MaterialTheme.typography.bodySmall)
    groups.forEach { (key, records) ->
        val route = when (key.second) { "SUBLINGUAL" -> "舌下"; "BUCCAL" -> "颊含"; "TRANSDERMAL" -> "经皮"; "ORAL" -> "口服"; else -> key.second }
        Text("${key.first} · $route · ${key.third.orEmpty()}", style = MaterialTheme.typography.labelLarge)
        val top = maxOf(records.maxOfOrNull { it.dose ?: 0.0 } ?: 0.0, 1.0)
        Canvas(Modifier.fillMaxWidth().height(100.dp).semantics { contentDescription = "原始剂量时间线：${key.first}，${records.size}条，不是血药浓度" }) {
            val inset = 8.dp.toPx()
            val width = size.width - 2 * inset
            val height = size.height - 2 * inset
            drawLine(axis, Offset(inset, inset + height), Offset(inset + width, inset + height))
            records.forEach { record -> record.dose?.takeIf { it.isFinite() && it > 0 }?.let { dose ->
                val x = inset + ((record.time/3600000.0 - data.fromHours)/(data.nowHours - data.fromHours)*width).toFloat()
                val y = inset + ((1 - dose/top)*height).toFloat()
                drawLine(color.copy(alpha = .4f), Offset(x, inset + height), Offset(x, y), 1.dp.toPx())
                drawCircle(color, 3.dp.toPx(), Offset(x, y))
            } }
        }
        Text("${records.size} 条 · 纵轴 0–${"%.4g".format(top)} ${key.third.orEmpty()}", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun HormoneChart(data: HormoneChartData, unit: String) {
    val actualColor = MaterialTheme.colorScheme.primary
    val forecastColor = MaterialTheme.colorScheme.secondary
    val labColor = MaterialTheme.colorScheme.tertiary
    val axis = MaterialTheme.colorScheme.outline
    val factor = when (unit) { "pmol/L" -> 3.671; "ng/mL" -> 0.001; else -> 1.0 }
    val top = maxOf(data.history.maxOfOrNull { it.pgMl } ?: 0.0, data.forecast.maxOfOrNull { it.pgMl } ?: 0.0, data.measured.maxOfOrNull { it.value } ?: 0.0, data.gelScenarios.flatMap { it.history + it.forecast }.maxOfOrNull { it.pgMl } ?: 0.0, 1.0)
    val scenarioColors = listOf(MaterialTheme.colorScheme.primary, if (isSystemInDarkTheme()) Color(0xFF80CBC4) else Color(0xFF00897B))
    val span = data.throughHours - data.fromHours
    var selected by remember(data) { mutableStateOf<HormoneHit?>(null) }
    Text("0 – ${"%.4g".format(top * factor)} $unit", style = MaterialTheme.typography.labelSmall)
    Canvas(Modifier.fillMaxWidth().height(240.dp).semantics { contentDescription = "历史模型实线、未来计划虚线、独立化验点；可点按查看时点" }
        .pointerInput(data, unit) {
            detectTapGestures { tap ->
                val inset = 10.dp.toPx()
                val width = size.width - 2 * inset; val height = size.height - 2 * inset
                val time = data.fromHours + ((tap.x - inset) / width).coerceIn(0f, 1f) * span
                val closeLab = data.measured.minByOrNull { point ->
                    val x = inset + ((point.original.time/3600000.0 - data.fromHours)/span*width).toFloat()
                    val y = inset + ((1 - point.value/top)*height).toFloat()
                    (x-tap.x)*(x-tap.x) + (y-tap.y)*(y-tap.y)
                }?.takeIf { point ->
                    val x = inset + ((point.original.time/3600000.0 - data.fromHours)/span*width).toFloat()
                    val y = inset + ((1 - point.value/top)*height).toFloat()
                    abs(x - tap.x) <= 12.dp.toPx() && abs(y-tap.y) <= 12.dp.toPx()
                }
                selected = if (closeLab != null) HormoneHit(closeLab.original.time/3600000.0, closeLab.value, "实测 ${closeLab.original.comparison}")
                else {
                    val hits = if (data.gelScenarios.isNotEmpty()) data.gelScenarios.flatMap { scenario ->
                        (if (time <= data.nowHours) scenario.history else scenario.forecast).map { HormoneHit(it.timeHours, it.pgMl, scenario.label) }
                    } else (if (time <= data.nowHours) data.history else data.forecast).map { HormoneHit(it.timeHours, it.pgMl, "研究参考采样值") }
                    hits.minByOrNull { hit ->
                        val dx = inset + ((hit.time-data.fromHours)/span*width).toFloat()-tap.x
                        val dy = inset + ((1-hit.value/top)*height).toFloat()-tap.y
                        dx*dx+dy*dy
                    }
                }
            }
        }) {
        val inset = 10.dp.toPx(); val width = size.width - 2*inset; val height = size.height - 2*inset
        fun position(time: Double, value: Double) = Offset(inset + ((time-data.fromHours)/span*width).toFloat(), inset + ((1-value/top)*height).toFloat())
        drawLine(axis, Offset(inset, inset), Offset(inset, inset+height))
        drawLine(axis, Offset(inset, inset+height), Offset(inset+width, inset+height))
        val nowX = position(data.nowHours, 0.0).x
        drawLine(axis, Offset(nowX, inset), Offset(nowX, inset+height), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())))
        fun path(samples: List<HormoneSample>): Path = Path().apply {
            samples.forEachIndexed { index, sample -> val point = position(sample.timeHours, sample.pgMl); if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y) }
        }
        if (data.gelScenarios.isEmpty() && data.history.isNotEmpty()) drawPath(path(data.history), actualColor, style = Stroke(2.dp.toPx()))
        if (data.gelScenarios.isEmpty() && data.forecast.isNotEmpty()) drawPath(path(data.forecast), forecastColor, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))))
        data.gelScenarios.forEachIndexed { index, scenario ->
            val color = scenarioColors[index % scenarioColors.size]
            drawPath(path(scenario.history), color, style = Stroke(2.dp.toPx()))
            if (scenario.forecast.isNotEmpty()) drawPath(path(scenario.forecast), color, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))))
        }
        data.measured.forEach { lab ->
            val point = position(lab.original.time/3600000.0, lab.value)
            if (lab.original.comparison == "=") drawCircle(labColor, 4.dp.toPx(), point)
            else drawCircle(labColor, 5.dp.toPx(), point, style = Stroke(2.dp.toPx()))
        }
        selected?.let { hit -> drawCircle(axis, 6.dp.toPx(), position(hit.time, hit.value), style = Stroke(1.dp.toPx())) }
    }
    Row { Text(modelTime(data.fromHours), style = MaterialTheme.typography.labelSmall); Spacer(Modifier.weight(1f)); Text(modelTime(data.throughHours), style = MaterialTheme.typography.labelSmall) }
    data.gelScenarios.forEachIndexed { index, scenario -> Text(scenario.label.replace("资料情景", "参考"), color = scenarioColors[index % scenarioColors.size], style = MaterialTheme.typography.labelSmall) }

    selected?.let { Text("${modelTime(it.time)} · ${it.label} ${"%.6g".format(it.value*factor)} $unit") }
}

@Composable
private fun HormoneValueTable(data: HormoneChartData, unit: String, onClose: () -> Unit) {
    val rows = remember(data) {
        (data.history.map { HormoneHit(it.timeHours, it.pgMl, if (data.gelScenarios.isEmpty()) "历史研究估算" else "非凝胶贡献（含桥接）") } +
            data.forecast.filter { it.timeHours > data.nowHours }.map { HormoneHit(it.timeHours, it.pgMl, "未来情景") } +
            data.gelScenarios.flatMap { scenario -> (scenario.history + scenario.forecast).map { HormoneHit(it.timeHours, it.pgMl, scenario.label) } } +
            data.measured.map { HormoneHit(it.original.time/3600000.0, it.value, "实测 ${it.original.comparison}") }).sortedBy { it.time }
    }
    var page by remember(rows) { mutableIntStateOf(0) }
    val factor = when (unit) { "pmol/L" -> 3.671; "ng/mL" -> 0.001; else -> 1.0 }
    AlertDialog(onDismissRequest = onClose, title = { Text("数值表 · $unit") }, text = {
        Column {
            if (rows.isEmpty()) Text("暂无数据")
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                rows.drop(page*20).take(20).forEach { row -> item {
                    SelectionContainer { Text("${modelTime(row.time)} · ${row.label} · ${"%.6g".format(row.value*factor)} $unit") }
                } }
            }
            if (rows.size > 20) Row {
                TextButton({ page-- }, enabled = page > 0) { Text("上一页") }
                Text("${page+1} / ${(rows.size+19)/20}")
                TextButton({ page++ }, enabled = (page+1)*20 < rows.size) { Text("下一页") }
            }
        }
    }, confirmButton = { TextButton(onClose) { Text("关闭") } })
}

fun hormoneReason(reason: String): String = when {
    reason.startsWith("missing_dose_spec:") -> reason.substringAfter(':')
    else -> when (reason) {
        "route_model_unverified" -> "药物已识别；实际途径缺少专属药代参数，未套用口服或其他制剂曲线"
        "gel_study_scenario" -> "有效mg已用于凝胶文献情景"
        "needs_identity" -> "需要关联药物身份"
        "needs_formulation" -> "药物成分已识别，请确认具体制剂"
        "ambiguous_gel_mass_basis" -> "请确认记录的是凝胶总质量还是有效雌二醇质量"
        "non_e2_record" -> "不是 E2 来源，不参与此模型"
        "future_actual_record", "future_lab_record" -> "时间位于未来，不能当作实测历史"
        "select_reference_profile" -> "请在药品规格中选择研究参考模型"
        "unsupported_parameter_set" -> "药物已识别，当前途径或制剂尚无核对参数"
        "other_analyte_record" -> "已识别，请切换到对应成分曲线"
        "unsupported_reference_dose" -> "该喷雾仅核对1、2、3喷，不能按其他剂量线性外推"
        "dose_basis_not_supported" -> "该成分不能按雌二醇当量换算"
        "profile_form_or_route_mismatch" -> "研究模型与化学形式或途径不一致"
        "missing_application_site" -> "需要补充给药部位"
        "reference_application_site_mismatch" -> "研究未覆盖这个给药部位"
        "missing_patch_release_rate" -> "需要明确贴片释放速率"
        "missing_patch_wear_interval" -> "需要贴片移除时间或仍在佩戴标记"
        "missing_planned_patch_wear" -> "计划模拟需要佩戴时长"
        "missing_chemical_mass" -> "不能确定所选药物的质量"
        "unknown_dose_basis" -> "剂量口径未知，请重新补全"
        "dose_out_of_range" -> "数值超出模型计算范围"
        "unsupported_lab_unit" -> "化验单位不能与 E2 曲线换算"
        "forecast_event_limit" -> "计划过于密集，请缩短未来显示范围"
        "invalid_plan", "invalid_plan_route" -> "计划配置无效，请编辑修复"
        else -> "未纳入：$reason"
    }
}

@Composable
private fun HormoneExclusions(items: List<HormoneExclusion>, onClose: () -> Unit, onEdit: (Int) -> Unit) {
    var page by remember(items) { mutableIntStateOf(0) }
    AlertDialog(onDismissRequest = onClose, title = { Text("未纳入项") }, text = {
        Column {
            if (items.isEmpty()) Text("没有未纳入项")
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items.drop(page*20).take(20).forEach { excluded -> item {
                    Text("${if (excluded.planned) "计划 · " else ""}${excluded.name}", style = MaterialTheme.typography.titleSmall)
                    excluded.time?.let { Text(modelTime(it/3600000.0)) }
                    Text(hormoneReason(excluded.reason))
                    val correctable = excluded.reason.startsWith("missing_dose_spec:") || excluded.reason in setOf("needs_identity", "needs_formulation", "ambiguous_gel_mass_basis", "select_reference_profile", "missing_application_site", "missing_patch_release_rate", "missing_patch_wear_interval", "missing_chemical_mass", "profile_form_or_route_mismatch")
                    if (correctable) excluded.id.toIntOrNull()?.takeIf { it > 0 }?.let { id -> TextButton({ onEdit(id) }) { Text("补全此记录") } }
                    HorizontalDivider()
                } }
            }
            if (items.size > 20) Row {
                TextButton({ page-- }, enabled = page>0) { Text("上一页") }
                Text("${page+1} / ${(items.size+19)/20}")
                TextButton({ page++ }, enabled = (page+1)*20<items.size) { Text("下一页") }
            }
        }
    }, confirmButton = { TextButton(onClose) { Text("关闭") } })
}

@Composable
private fun HormoneSources(catalog: HormoneReferenceCatalog, used: Set<String>, assumptions: Set<String>, onClose: () -> Unit) {
    val uri = LocalUriHandler.current
    val context = LocalContext.current
    var notice by remember { mutableStateOf<String?>(null) }
    var license by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onClose, title = { Text("研究参考与支持范围") }, text = {
        Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("版本 ${catalog.version} · 数值和研究锚点核对，不等同个体临床验证。")
            Text("采用线性叠加和剂量缩放；不推断内源分泌、抗雄抑制或个人清除率。研究基线不会自动加到个人曲线。")
            assumptions.forEach { Text(it) }
            catalog.models.forEach { profile ->
                Text("${if (profile.id in used) "当前使用 · " else ""}${profile.name}", style = MaterialTheme.typography.titleSmall)
                Text("途径：${profile.administrationRoutes.joinToString()}")
                profile.assumptions.forEach { Text(it) }
                profile.sources.forEach { sourceId ->
                    val source = catalog.sources.getValue(sourceId)
                    source.population?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    val title = when (sourceId) {
                        "doll2022" -> "Doll 等，2022"
                        "oriowo1980" -> "Oriowo 等，1980"
                        "rothman2024" -> "Rothman 等，2024"
                        "oral_smpc" -> "口服 E2 说明书"
                        "estrogel_fda" -> "FDA EstroGel 审评"
                        "estrogel_label" -> "EstroGel 说明书"
                        "patch_label" -> "贴片说明书"
                        "estrannaise_temporal_priors" -> "开放模型时间先验"
                        "molecular_weights" -> "分子量来源"
                        else -> "研究来源"
                    }
                    (listOfNotNull(source.url) + source.urls).forEach { url -> TextButton({ try { uri.openUri(url) } catch (_: Exception) { notice = "无法打开来源链接" } }) { Text(title) } }
                }
            }
            Text("未列出的酯、载体、途径或部位不会自动套用其他模型。")
            TextButton({ license = !license }) { Text("开源模型声明") }
            if (license) SelectionContainer { Text(remember { context.resources.openRawResource(R.raw.hrt_model_notices).bufferedReader().use { it.readText() } }) }
            notice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClose) { Text("关闭") } })
}
