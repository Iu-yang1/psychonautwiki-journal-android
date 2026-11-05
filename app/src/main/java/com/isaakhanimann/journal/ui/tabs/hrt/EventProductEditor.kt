package com.isaakhanimann.journal.ui.tabs.hrt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import com.isaakhanimann.journal.data.hrt.*
import com.isaakhanimann.journal.data.room.experiences.entities.Ingestion
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventProductEditor(viewModel: HrtViewModel, event: Ingestion, medicationId: String, settings: List<HrtModelSetting>, onClose: () -> Unit) {
    val details by viewModel.details.collectAsState()
    val detail = details.singleOrNull { it.ingestionId == event.id }
    val medication = viewModel.medications.singleOrNull { it.id == medicationId }
    val isE2 = medication?.identity?.ingredientId == "ingredient:estradiol"
    val identityForm = medication?.identity?.formulationId
    val originalSnapshot = MedicationProducts.eventSnapshot(event.id, settings)
    var product by remember(event.id, originalSnapshot) { mutableStateOf(originalSnapshot?.product) }
    val form = identityForm ?: product?.formulation
    val compatibleModels = viewModel.references.catalog.models.filter { it.acceptsMedication(medicationId) && event.administrationRoute.name in it.administrationRoutes }
    val previewAnalyte = product?.modelProfileId?.let { id -> viewModel.references.catalog.models.singleOrNull { it.id == id }?.analyteId } ?: compatibleModels.firstOrNull()?.analyteId ?: "E2"
    val hasModel = compatibleModels.isNotEmpty()
    val activeGelMg = form == "form:gel" && event.units?.let(MedicationProducts::unitKey) == "mg"
    var showAdvanced by remember(event.id) { mutableStateOf(originalSnapshot?.doseBasis == DoseBasis.PRODUCT && originalSnapshot.product.strengthMgPerUnit != null) }
    var basis by remember(event.id, originalSnapshot, detail) {
        mutableStateOf(originalSnapshot?.doseBasis ?: detail?.doseBasis?.let { value -> DoseBasis.entries.singleOrNull { it.name == value } }
            ?: if (originalSnapshot?.product?.unit?.let(MedicationProducts::unitKey) == event.units?.let(MedicationProducts::unitKey) && originalSnapshot != null
                || form == "form:gel" && event.units?.let(MedicationProducts::unitKey) == "g") DoseBasis.PRODUCT
            else if (event.units?.let { MedicationProducts.massMg(1.0, it) } != null) DoseBasis.CHEMICAL_FORM else DoseBasis.PRODUCT)
    }
    var ongoing by remember(event.id, originalSnapshot) { mutableStateOf(originalSnapshot?.patchOngoing ?: false) }
    var site by remember(event.id, detail) { mutableStateOf(detail?.site.orEmpty()) }
    val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    var removal by remember(event.id, detail) { mutableStateOf(detail?.patchRemovedAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(format) } ?: "") }
    var error by remember(event.id) { mutableStateOf<String?>(null) }
    var createProduct by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    val products = remember(settings, medicationId) { MedicationProducts.all(settings).filter { it.medicationId == medicationId && !it.archived } }
    val templates = remember(medicationId) { HrtReferenceProducts.forMedication(medicationId, viewModel.references.catalog).filter { candidate -> viewModel.references.catalog.models.any { it.id == candidate.modelProfileId && event.administrationRoute.name in it.administrationRoutes } } }
    val result = MedicationProducts.resolve(event.dose, event.units, medicationId, product, basis)
    val removalMillis = runCatching { removal.takeIf(String::isNotBlank)?.let { LocalDateTime.parse(it, format).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() } }
    val now = remember { System.currentTimeMillis() / 3600000.0 }
    val preview = if (hasModel && removalMillis.isSuccess) {
        val previewSettings = product?.let { listOf(EventProductSnapshot(it, System.currentTimeMillis(), basis, ongoing).setting(event.id)) }.orEmpty()
        HormoneRecordNormalizer.normalize(listOf(event), listOf(MedicationLink(event.id, medicationId, "identified", event.substanceName)),
            listOf(HrtEventDetail(event.id, site = site.ifBlank { null }, patchRemovedAt = removalMillis.getOrNull(), doseBasis = basis.name)),
            previewSettings, viewModel.medications, viewModel.references.catalog, event.consumerName, now, now + 7 * 24, analyteId = previewAnalyte)
    } else null

    AlertDialog(onDismissRequest = onClose, title = { Text("补全这次用药") }, text = {
        Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${event.substanceName} · ${event.dose ?: "?"} ${event.units.orEmpty()}", style = MaterialTheme.typography.titleSmall)
            Text("${event.administrationRoute.displayText} · 原始记录保留", style = MaterialTheme.typography.bodySmall)
            Text("已识别 · ${medication?.hrtCategoryLabel() ?: "药物"}", color = MaterialTheme.colorScheme.primary)
            if (activeGelMg) {
                Text("${event.dose ?: "?"} mg 已是有效雌二醇质量，无需补商品规格。", color = MaterialTheme.colorScheme.primary)
                Text("图表统一选择文献参考；未知制剂和部位保持留空。", style = MaterialTheme.typography.bodySmall)
                TextButton({ showAdvanced = !showAdvanced }) { Text(if (showAdvanced) "收起其他解释" else "其他剂量口径 / 制剂信息") }
            }
            if (!activeGelMg || showAdvanced) {
            Text("记录中的数量是", style = MaterialTheme.typography.titleSmall)
            DoseMeaningChoice(basis == DoseBasis.CHEMICAL_FORM, { basis = DoseBasis.CHEMICAL_FORM },
                if (isE2 && medication?.identity?.chemicalFormId == "chemical:estradiol") "有效雌二醇的质量" else if (medication?.identity?.ingredientId == "ingredient:estetrol") "无水雌四醇质量" else "药物本身的质量（含酯时按酯质量）")
            DoseMeaningChoice(basis == DoseBasis.PRODUCT, { basis = DoseBasis.PRODUCT },
                if (form == "form:gel") "凝胶总用量 / 泵数" else if (medication?.identity?.ingredientId == "ingredient:estetrol") "片数或单水合物质量（需对应规格）" else "片数、毫升或其他制剂用量")
            if (isE2 && medication?.identity?.chemicalFormId != "chemical:estradiol") {
                DoseMeaningChoice(basis == DoseBasis.ACTIVE_E2, { basis = DoseBasis.ACTIVE_E2 }, "折算后的雌二醇当量")
            }
            HorizontalDivider()
            Text("所用制剂", style = MaterialTheme.typography.titleSmall)
            Text(product?.name ?: "尚未选择", style = MaterialTheme.typography.bodyMedium)
            if (products.isEmpty() && templates.isEmpty()) Text("没有现成规格，可在这里新建。", style = MaterialTheme.typography.bodySmall)
            originalSnapshot?.let { snapshot ->
                FilterChip(product == snapshot.product, { product = snapshot.product }, label = { Text("保留当时规格") })
            }
            products.forEach { item -> FilterChip(product == item, { product = item }, label = { Text(item.name) }) }
            if (templates.isNotEmpty()) {
                Text("已核对的制剂规格，仅在与实际药品相同时选择", style = MaterialTheme.typography.bodySmall)
                templates.forEach { item -> FilterChip(product == item, { product = item }, label = { Text(item.name) }) }

            }
            Row {
                TextButton({ createProduct = true }) { Text("新建规格") }
                TextButton({ product = null; ongoing = false }) { Text("暂不指定") }
            }
            product?.sourceUrl?.let { source -> TextButton({ try { uri.openUri(source) } catch (_: Exception) { error = "无法打开规格来源" } }) { Text("规格来源") } }
            if (product != null && hasModel) {
                val compatible = compatibleModels.distinctBy { it.referenceFamily ?: it.id }
                if (compatible.isNotEmpty()) {
                    Text("制剂对应的研究参考", style = MaterialTheme.typography.titleSmall)
                    compatible.forEach { profile ->
                        FilterChip(product?.modelProfileId == profile.id, { product = product?.copy(modelProfileId = profile.id, formulation = profile.formulationId) }, label = { Text(profile.name) })
                    }
                }
            }
            Text(result.missing ?: result.activeE2Mg?.let { "雌二醇当量：$it mg" } ?: result.chemicalFormMg?.let { "药物质量：$it mg" } ?: "释放速率：${result.patchReleaseMcgDay} μg/天")
            if (form in setOf("form:gel", "form:patch", "form:oil-injection", "form:depot-injection", "form:implant", "form:spray")) {
                Text(if (form in setOf("form:gel", "form:patch", "form:spray")) "实际给药部位（曲线需要）" else "给药部位（可选）", style = MaterialTheme.typography.titleSmall)
                if (form in setOf("form:gel", "form:patch", "form:spray")) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("上臂", "上大腿", "前臂", "腹部").forEach { value -> FilterChip(site == value, { site = value }, label = { Text(value) }) }
                }
                OutlinedTextField(site, { site = it }, label = { Text("实际部位，可自行填写") })
            }
            if (form == "form:patch") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(ongoing, { ongoing = it; if (it) removal = "" }, enabled = product?.patchReleaseMcgDay != null)
                    Text("仍在佩戴")
                }
                OutlinedTextField(removal, { removal = it; if (it.isNotBlank()) ongoing = false }, label = { Text("实际移除时间 · yyyy-MM-ddTHH:mm") })
            }
            if (hasModel) {
                Text(if (preview?.events?.isNotEmpty() == true) "已补全，可纳入 ${hormoneAnalyteLabel(previewAnalyte)} 曲线" else "尚未纳入：${preview?.excluded?.values?.firstOrNull()?.let(::hormoneReason) ?: "检查移除时间"}",
                    color = if (preview?.events?.isNotEmpty() == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            } else Text("这条 HRT 用药已识别并保留；不用于计算雌二醇浓度。", style = MaterialTheme.typography.bodySmall)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            viewModel.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton({
        try {
            val removed = removalMillis.getOrThrow()
            viewModel.run { viewModel.repo.saveEventProduct(event, product, site.ifBlank { null }, removed, if (activeGelMg && !showAdvanced) DoseBasis.CHEMICAL_FORM else basis, ongoing); onClose() }
        } catch (exception: RuntimeException) { error = exception.message ?: "输入无效" }
    }) { Text("保存补全信息") } }, dismissButton = { TextButton(onClose) { Text("取消") } })
    if (createProduct) ProductEditor(viewModel, null, { createProduct = false }, medicationIdHint = medicationId,
        unitHint = when (form) { "form:gel" -> if (event.units?.let(MedicationProducts::unitKey) == "pump") "pump" else "g"; "form:oil-injection" -> "mL"; "form:patch" -> "patch"; "form:spray" -> "spray"; else -> "tablet" }) { created ->
        viewModel.run { viewModel.repo.saveProduct(created); product = created; createProduct = false }
    }
}

@Composable
private fun DoseMeaningChoice(selected: Boolean, onClick: () -> Unit, label: String) {
    Row(Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(vertical = 6.dp))
    }
}
