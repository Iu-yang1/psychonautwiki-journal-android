package com.isaakhanimann.journal.ui.tabs.hrt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.data.hrt.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun InventoryPanel(viewModel: HrtViewModel) {
    val stocks by viewModel.stock.collectAsState()
    val movements by viewModel.movements.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val decoded = remember(settings) { runCatching { MedicationProducts.all(settings) } }
    val products = decoded.getOrNull().orEmpty()
    var editingProduct by remember { mutableStateOf<MedicationProduct?>(null) }
    var productForm by remember { mutableStateOf(false) }
    var editingStock by remember { mutableStateOf<HrtStock?>(null) }
    var stockForm by remember { mutableStateOf(false) }
    var adjust by remember { mutableStateOf<HrtStock?>(null) }
    var ledger by remember { mutableStateOf<HrtStock?>(null) }
    val format = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            TextButton({ editingStock = null; stockForm = true }) { Text("添加库存") }
            TextButton({ editingProduct = null; productForm = true }) { Text("添加药品规格") }
        }
        if (decoded.isFailure) Text("规格数据无法读取：${decoded.exceptionOrNull()?.message}", color = MaterialTheme.colorScheme.error)
        if (stocks.isEmpty()) Text("暂无库存")
        stocks.forEach { stock ->
            val archived = settings.any { it.id == "stock-status:${stock.id}" && it.payload == "archived" }
            val product = MedicationProducts.forStock(stock.id, settings)
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text(stock.name, style = MaterialTheme.typography.titleMedium)
                Text("${InventoryLedger.balance(movements.filter { it.stockId == stock.id })} ${stock.unit}${if (archived) " · 已归档" else ""}")
                product?.let { Text("${it.name} · ${it.strengthMgPerUnit?.let { strength -> "$strength mg/${it.unit}" } ?: "含量待补充"}") }
                stock.expires?.let { Text("有效期：${format.format(Instant.ofEpochMilli(it))}${if (it < System.currentTimeMillis()) " · 已过期" else ""}") }
                Row {
                    TextButton({ editingStock = stock; stockForm = true }) { Text("编辑") }
                    TextButton({ adjust = stock }) { Text("入库 / 损耗 / 调整") }
                    TextButton({ ledger = stock }) { Text("流水") }
                }
                TextButton({ viewModel.run { viewModel.repo.dao.save(HrtModelSetting("stock-status:${stock.id}", if (archived) "active" else "archived")) } }) { Text(if (archived) "取消归档" else "归档") }
            } }
        }
        Text("药品规格", style = MaterialTheme.typography.titleMedium)
        products.forEach { product ->
            TextButton({ editingProduct = product; productForm = true }) { Text("${product.name} · ${product.formulation} · ${product.strengthMgPerUnit?.toString() ?: "?"} mg/${product.unit}${if (product.archived) " · 已归档" else ""}") }
        }
    }
    if (productForm) ProductEditor(viewModel, editingProduct, { productForm = false }) { product ->
        viewModel.run { viewModel.repo.saveProduct(product); productForm = false }
    }
    if (stockForm) StockEditor(viewModel, editingStock, products, settings, { stockForm = false }) { stock, product, amount ->
        viewModel.run { viewModel.repo.saveStock(stock, product, amount); stockForm = false }
    }
    adjust?.let { stock ->
        var delta by remember(stock.id) { mutableStateOf("") }
        var reason by remember(stock.id) { mutableStateOf("") }
        AlertDialog(onDismissRequest = { adjust = null }, title = { Text(stock.name) }, text = {
            Column {
                OutlinedTextField(delta, { delta = it }, label = { Text("变化数量（入库为正、损耗为负）· ${stock.unit}") })
                OutlinedTextField(reason, { reason = it }, label = { Text("原因") })
                viewModel.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton({ viewModel.run { viewModel.repo.adjustStock(stock.id, delta.toDouble(), reason); adjust = null } }) { Text("保存") } }, dismissButton = { TextButton({ adjust = null }) { Text("取消") } })
    }
    ledger?.let { stock ->
        val entries = movements.filter { it.stockId == stock.id }.sortedByDescending { it.time }
        AlertDialog(onDismissRequest = { ledger = null }, title = { Text("${stock.name} · 流水") }, text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                entries.forEach { entry ->
                    val reason = when {
                        entry.eventKey.startsWith("manual:") -> entry.reason
                        entry.reason == "opening" -> "初始入库"
                        entry.reason.startsWith("intake:") -> if (entry.amount < 0) "用药扣减" else "用药更正 / 撤销"
                        else -> entry.reason
                    }
                    Text("${format.format(Instant.ofEpochMilli(entry.time))} · ${entry.amount} ${stock.unit} · $reason")
                }
            }
        }, confirmButton = { TextButton({ ledger = null }) { Text("关闭") } })
    }
}

@Composable
fun ProductEditor(viewModel: HrtViewModel, existing: MedicationProduct?, onClose: () -> Unit, medicationIdHint: String? = null, unitHint: String? = null, onSave: (MedicationProduct) -> Unit) {
    var medicationId by remember { mutableStateOf(existing?.medicationId ?: medicationIdHint.orEmpty()) }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var formulation by remember { mutableStateOf(existing?.formulation ?: viewModel.medications.singleOrNull { it.id == medicationIdHint }?.identity?.formulationId.orEmpty()) }
    var unit by remember { mutableStateOf(existing?.unit ?: unitHint ?: "tablet") }
    var strength by remember { mutableStateOf(existing?.strengthMgPerUnit?.toString() ?: "") }
    var release by remember { mutableStateOf(existing?.patchReleaseMcgDay?.toString() ?: "") }
    var archived by remember { mutableStateOf(existing?.archived ?: false) }
    var profileId by remember { mutableStateOf(existing?.modelProfileId) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onClose, title = { Text("药品规格") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (medicationIdHint == null) MedicationPicker(viewModel, medicationId) { medicationId = it; profileId = null }
            else Text(viewModel.medications.singleOrNull { it.id == medicationId }?.name ?: medicationId)
            OutlinedTextField(name, { name = it }, label = { Text("商品名 / 规格名称") })
            OutlinedTextField(formulation, { formulation = it }, label = { Text("剂型，如片剂、凝胶、油剂注射液") })
            OutlinedTextField(unit, { unit = it }, label = { Text("计数单位，如 tablet、pump、mL、g、patch") })
            OutlinedTextField(strength, { strength = it }, label = { Text("每 $unit 含药物质量（mg）") })
            OutlinedTextField(release, { release = it }, label = { Text("每贴释放速率（μg/天，仅贴片）") })
            Row { Checkbox(archived, { archived = it }); Text("归档") }
            Text("研究参考模型")
            FilterChip(profileId == null, { profileId = null }, label = { Text("暂不指定") })
            viewModel.references.catalog.models.filter { it.acceptsMedication(medicationId) }.distinctBy { it.referenceFamily ?: it.id }.forEach { profile ->
                FilterChip(profileId == profile.id, { profileId = profile.id; formulation = profile.formulationId }, label = { Text(profile.name) })
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            viewModel.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton({
        try {
            val product = MedicationProduct(existing?.id ?: UUID.randomUUID().toString(), medicationId, name, formulation, unit,
                strength.takeIf(String::isNotBlank)?.toDouble(), release.takeIf(String::isNotBlank)?.toDouble(), archived, profileId)
            product.validate(); onSave(product)
        } catch (exception: RuntimeException) { error = exception.message ?: "输入无效" }
    }) { Text("保存") } }, dismissButton = { TextButton(onClose) { Text("取消") } })
}

@Composable
private fun StockEditor(viewModel: HrtViewModel, existing: HrtStock?, products: List<MedicationProduct>, settings: List<HrtModelSetting>, onClose: () -> Unit, onSave: (HrtStock, MedicationProduct?, Double?) -> Unit) {
    var medicationId by remember { mutableStateOf(existing?.medicationId ?: "") }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var unit by remember { mutableStateOf(existing?.unit ?: "tablet") }
    var amount by remember { mutableStateOf("") }
    val initialProduct = existing?.let { MedicationProducts.forStock(it.id, settings) }
    var selectedProduct by remember { mutableStateOf(initialProduct) }
    var expiry by remember { mutableStateOf(existing?.expires?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString() } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onClose, title = { Text(if (existing == null) "添加库存" else "编辑库存") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (existing == null) MedicationPicker(viewModel, medicationId) { medicationId = it; selectedProduct = null }
            else Text(viewModel.medications.singleOrNull { it.id == medicationId }?.name ?: medicationId)
            OutlinedTextField(name, { name = it }, label = { Text("库存名称 / 批次") })
            OutlinedTextField(unit, { unit = it; selectedProduct = null }, enabled = existing == null, label = { Text("库存单位") })
            FilterChip(selectedProduct == null, { selectedProduct = null }, label = { Text("暂无明确规格") })
            if (initialProduct != null) FilterChip(selectedProduct == initialProduct, { selectedProduct = initialProduct }, label = { Text("保留原规格：${initialProduct.name}") })
            products.filter { !it.archived && it.medicationId == medicationId && MedicationProducts.unitKey(it.unit) == MedicationProducts.unitKey(unit) }.forEach { product ->
                FilterChip(selectedProduct == product, { selectedProduct = product }, label = { Text(product.name) })
            }
            if (existing == null) OutlinedTextField(amount, { amount = it }, label = { Text("初始数量") })
            OutlinedTextField(expiry, { expiry = it }, label = { Text("有效期（可选，yyyy-MM-dd）") })
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            viewModel.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton({
        try {
            val expiration = expiry.takeIf(String::isNotBlank)?.let { LocalDate.parse(it).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1 }
            onSave(HrtStock(existing?.id ?: UUID.randomUUID().toString(), medicationId, name, unit, expiration), selectedProduct, if (existing == null) amount.toDouble() else null)
        } catch (exception: RuntimeException) { error = exception.message ?: "输入无效" }
    }) { Text("保存") } }, dismissButton = { TextButton(onClose) { Text("取消") } })
}

@Composable
fun MedicationPicker(viewModel: HrtViewModel, selectedId: String, onSelect: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val ranking by viewModel.search.ranking.collectAsState()
    val sort by viewModel.search.sortMode.collectAsState()
    val matches = remember(query, ranking, sort) { viewModel.matchingMedications(query) }
    Text(viewModel.medications.singleOrNull { it.id == selectedId }?.name ?: "选择药物")
    OutlinedTextField(query, { query = it }, label = { Text("查找药物") })
    if (selectedId.isBlank() || query.isNotBlank()) matches.take(12).forEach { sub ->
        TextButton({ onSelect(sub.id); query = "" }) { Text(sub.name) }
    }
}
