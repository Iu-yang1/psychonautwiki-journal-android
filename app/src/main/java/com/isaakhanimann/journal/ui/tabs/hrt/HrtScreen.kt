package com.isaakhanimann.journal.ui.tabs.hrt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.isaakhanimann.journal.data.hrt.HrtLab
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HrtScreen(onOpenJournal: () -> Unit, viewModel: HrtViewModel = hiltViewModel()) {
    val dashboard by viewModel.dashboard.collectAsState()
    val consumer by viewModel.consumer.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var consumerMenu by remember { mutableStateOf(false) }
    var managementMenu by remember { mutableStateOf(false) }
    var showPlans by remember { mutableStateOf(false) }
    var showReview by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var associationId by remember { mutableStateOf<Int?>(null) }
    var productId by remember { mutableStateOf<Int?>(null) }
    val preferences = LocalContext.current.getSharedPreferences("hrt_navigation", 0)
    var journalFirst by remember { mutableStateOf(preferences.getBoolean("journalFirst", false)) }
    LaunchedEffect(consumer) { associationId = null; productId = null; showReview = false }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("HRT", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onOpenJournal) { Text("打开日记") }
            Box {
                IconButton({ managementMenu = true }) { Icon(Icons.Outlined.MoreVert, "HRT 管理") }
                DropdownMenu(managementMenu, { managementMenu = false }) {
                    DropdownMenuItem(text = { Text("计划设置") }, onClick = { managementMenu = false; showPlans = true })
                    DropdownMenuItem(text = { Text("历史数据适配") }, onClick = { managementMenu = false; showReview = true })
                    DropdownMenuItem(text = { Text("关联操作历史") }, onClick = { managementMenu = false; showHistory = true })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text(if (journalFirst) "设为启动页" else "启动时打开日记") }, onClick = {
                        journalFirst = !journalFirst
                        preferences.edit().putBoolean("journalFirst", journalFirst).apply()
                        managementMenu = false
                    })
                }
            }
        }
        Box(Modifier.padding(horizontal = 20.dp)) {
            TextButton({ consumerMenu = true }, contentPadding = PaddingValues(end = 8.dp)) {
                Icon(Icons.Outlined.PersonOutline, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(subjectLabel(consumer))
                Icon(Icons.Outlined.ArrowDropDown, null)
            }
            DropdownMenu(consumerMenu, { consumerMenu = false }) {
                dashboard.consumers.forEach { subject -> DropdownMenuItem(text = { Text(subjectLabel(subject)) }, onClick = {
                    viewModel.consumer.value = subject; consumerMenu = false
                }) }
            }
        }
        PrimaryTabRow(tab) {
            listOf("趋势", "化验", "库存").forEachIndexed { index, label ->
                Tab(tab == index, { tab = index }, text = { Text(label) })
            }
        }
        viewModel.message?.let { Text(it, Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.primary) }
        key(tab, consumer) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (tab) {
                    0 -> {
                        if (dashboard.estradiol != null || dashboard.testosterone != null) item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LatestLabCard("雌二醇 · E2", dashboard.estradiol, Modifier.weight(1f)) { tab = 1 }
                                LatestLabCard("总睾酮", dashboard.testosterone, Modifier.weight(1f)) { tab = 1 }
                            }
                        }
                        item {
                            ElevatedCard(elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)) {
                                Column(Modifier.padding(16.dp)) {
                                    HormoneSimulationPanel(viewModel, consumer, { showReview = true }, { associationId = it })
                                }
                            }
                        }
                        if (dashboard.pending > 0) item {
                            OutlinedCard(onClick = { showReview = true }) {
                                Text("${dashboard.pending} 条历史数据待确认", Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    1 -> item { LabPanel(viewModel, consumer) }
                    2 -> item { InventoryPanel(viewModel) }
                }
            }
        }
    }
    if (showPlans) ModalBottomSheet(onDismissRequest = { showPlans = false }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("计划设置", style = MaterialTheme.typography.titleLarge)
            HrtPlans(viewModel, consumer)
        }
    }
    if (showReview) HrtDataReviewDialog(viewModel, onClose = { showReview = false }, onAssociate = { associationId = it }, onProduct = { productId = it })
    if (showHistory) AssociationHistoryDialog(viewModel) { showHistory = false }
    associationId?.let { id ->
        val events by viewModel.events.collectAsState()
        events.singleOrNull { it.id == id && it.consumerName == consumer }?.let { event ->
            MedicationAssociationDialog(viewModel, event) { associationId = null }
        }
    }
    productId?.let { id ->
        val events by viewModel.events.collectAsState()
        val links by viewModel.links.collectAsState()
        val settings by viewModel.settings.collectAsState()
        val event = events.singleOrNull { it.id == id && it.consumerName == consumer }
        val link = links.singleOrNull { it.ingestionId == id }
        if (event != null && link?.medicationId != null) EventProductEditor(viewModel, event, link.medicationId, settings) { productId = null }
    }
}

private fun subjectLabel(consumer: String?): String = consumer?.ifEmpty { "历史空名称对象" } ?: "默认对象"

@Composable
private fun LatestLabCard(title: String, lab: HrtLab?, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier, onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(lab?.let { "${it.comparison.takeUnless { value -> value == "=" }.orEmpty()}${"%.5g".format(it.value)}" } ?: "—", style = MaterialTheme.typography.headlineSmall)
            Text(lab?.unit ?: "暂无化验", style = MaterialTheme.typography.bodySmall)
            lab?.let { Text(remember(it.time) { DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it.time)) }, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun HrtDataReviewDialog(viewModel: HrtViewModel, onClose: () -> Unit, onAssociate: (Int) -> Unit, onProduct: (Int) -> Unit) {
    val records by viewModel.reviewRecords.collectAsState()
    var page by remember { mutableIntStateOf(0) }
    val currentPage = page.coerceAtMost((records.size - 1).coerceAtLeast(0) / 20)
    AlertDialog(onDismissRequest = onClose, title = { Text("历史数据适配") }, text = {
        Column {
            Text("关联身份与补全规格；用量、时间和备注在日记中编辑。", style = MaterialTheme.typography.bodySmall)
            if (records.isEmpty()) Text("暂无需要适配的历史数据", Modifier.padding(vertical = 16.dp))
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                items(records.drop(currentPage * 20).take(20), key = { it.event.id }) { record ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text(record.event.substanceName, style = MaterialTheme.typography.titleSmall)
                        Text(record.category?.let { "已识别 · $it" } ?: "待确认身份", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text("${record.event.dose ?: "?"} ${record.event.units.orEmpty()} · ${record.event.time}", style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton({ onAssociate(record.event.id) }) { Text(if (record.link?.medicationId == null) "确认身份" else "更正关联") }
                            TextButton({ onProduct(record.event.id) }, enabled = record.link?.medicationId != null) { Text("补全规格 / 部位") }
                        }
                    }
                    HorizontalDivider()
                }
            }
            if (records.size > 20) Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton({ page = currentPage - 1 }, enabled = currentPage > 0) { Text("上一页") }
                Text("${currentPage + 1} / ${(records.size + 19) / 20}")
                TextButton({ page = currentPage + 1 }, enabled = (currentPage + 1) * 20 < records.size) { Text("下一页") }
            }
        }
    }, confirmButton = { TextButton(onClose) { Text("完成") } })
}
