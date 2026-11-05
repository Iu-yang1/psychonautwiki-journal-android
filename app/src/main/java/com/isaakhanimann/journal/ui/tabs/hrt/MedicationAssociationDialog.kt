package com.isaakhanimann.journal.ui.tabs.hrt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.data.hrt.*
import com.isaakhanimann.journal.data.room.experiences.entities.Ingestion
import com.isaakhanimann.journal.data.substances.classes.availableRoutesForIngestion
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MedicationAssociationDialog(viewModel: HrtViewModel, anchor: Ingestion, onClose: () -> Unit) {
    val events by viewModel.events.collectAsState()
    val links by viewModel.links.collectAsState()
    val ranking by viewModel.search.ranking.collectAsState()
    val sort by viewModel.search.sortMode.collectAsState()
    val known = remember(anchor.id) { viewModel.repo.substances.getSubstance(anchor.substanceName) }
    val needsFormulation = known?.identity?.ingredientId == "ingredient:estradiol" && known.identity.formulationId == null
    var query by remember(anchor.id) { mutableStateOf(anchor.substanceName) }
    var targetId by remember(anchor.id) { mutableStateOf(known?.takeIf { !needsFormulation && anchor.administrationRoute in it.availableRoutesForIngestion() }?.id) }
    var batch by remember { mutableStateOf(false) }
    var previewPage by remember { mutableIntStateOf(0) }
    val matches = remember(query, ranking, sort) {
        viewModel.search.getMatchingSubstances(query, emptyList(), emptyList()).map { it.substance }
            .filter { anchor.administrationRoute in it.availableRoutesForIngestion() && (!needsFormulation || it.identity?.ingredientId == "ingredient:estradiol" && it.identity.formulationId != null) }
    }
    val candidates = MedicationLinkPreview.matchingEvents(anchor, events)
    val selected = if (batch) candidates else listOf(anchor)
    val target = viewModel.repo.substances.getAllSubstances().singleOrNull { it.id == targetId }
    AlertDialog(onDismissRequest = onClose, title = { Text("关联药物") }, text = {
        Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${anchor.substanceName} · ${anchor.administrationRoute.displayText} · ${anchor.consumerName ?: "默认对象"}")
            if (links.singleOrNull { it.ingestionId == anchor.id }?.origin == "custom-collision") Text("存在同名自定义药物，请确认是否关联到内置资料。")
            if (needsFormulation) Text("成分已识别，请选择实际使用的制剂。")
            OutlinedTextField(query, { query = it }, label = { Text("查找名称 / 商品名 / 别名") })
            target?.let { Text("关联到：${it.name}", style = MaterialTheme.typography.titleMedium) }
            matches.take(12).forEach { substance ->
                FilterChip(targetId == substance.id, { targetId = substance.id }, label = { Text(substance.name) })
            }
            if (matches.isEmpty()) Text("没有支持此给药途径的匹配药物")
            Row {
                Checkbox(batch, { batch = it })
                Text("同时处理同名、同途径、同对象记录（${candidates.size} 条）")
            }
            Text("预览：${selected.size} 条；原始名称、用量和时间保留，规格单独补全。")
            val page = previewPage.coerceIn(0, (selected.size - 1).coerceAtLeast(0) / 20)
            selected.drop(page * 20).take(20).forEach { event -> Text("${event.time} · ${event.dose ?: "?"} ${event.units.orEmpty()}") }
            if (selected.size > 20) Row {
                TextButton({ previewPage = page - 1 }, enabled = page > 0) { Text("上一页") }
                Text("${page + 1} / ${(selected.size + 19) / 20}")
                TextButton({ previewPage = page + 1 }, enabled = (page + 1) * 20 < selected.size) { Text("下一页") }
            }
            viewModel.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton({ viewModel.run { viewModel.repo.associateBatch(selected, requireNotNull(targetId)); onClose() } }, enabled = targetId != null && selected.isNotEmpty() && selected.size <= 5000) { Text("确认关联 ${selected.size} 条") }
    }, dismissButton = {
        Row {
            TextButton({ viewModel.run { viewModel.repo.associate(anchor, null); onClose() } }) { Text("取消此条关联") }
            TextButton(onClose) { Text("关闭") }
        }
    })
}

@Composable
fun AssociationHistoryDialog(viewModel: HrtViewModel, onClose: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val decoded = runCatching { settings.filter { it.id.startsWith("link-batch:") }.map { Json.decodeFromString<MedicationLinkBatch>(it.payload) }.sortedByDescending { it.createdAt } }
    val format = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault())
    AlertDialog(onDismissRequest = onClose, title = { Text("批量关联历史") }, text = {
        Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (decoded.isFailure) Text("历史数据无法读取：${decoded.exceptionOrNull()?.message}", color = MaterialTheme.colorScheme.error)
            val batches = decoded.getOrNull().orEmpty()
            if (batches.isEmpty()) Text("暂无批量操作")
            batches.forEach { batch ->
                Text("${format.format(Instant.ofEpochMilli(batch.createdAt))} · ${batch.changes.size} 条 · ${batch.changes.firstOrNull()?.event?.name.orEmpty()}")
                if (batch.undone) Text("已撤销") else TextButton({ viewModel.run { viewModel.repo.undoAssociationBatch(batch.id) } }) { Text("撤销此批次") }
            }
            viewModel.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClose) { Text("关闭") } })
}
