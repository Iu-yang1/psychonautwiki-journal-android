package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.data.substances.classes.availableRoutesForIngestion
import androidx.room.withTransaction
import com.isaakhanimann.journal.data.room.AppDatabase
import com.isaakhanimann.journal.data.room.experiences.entities.*
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.repositories.SubstanceRepository
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HrtRepository @Inject constructor(val database: AppDatabase, val substances: SubstanceRepository) {
    val dao get() = database.hrtDao()
    suspend fun savePlan(plan: HrtPlan, schedule: PlanSchedule) = database.withTransaction {
        require(plan.dose.isFinite() && plan.dose > 0 && plan.unit.isNotBlank())
        schedule.validate(plan.start)
        require(substances.getAllSubstances().any { it.id == plan.medicationId && it.availableRoutesForIngestion().any { route -> route.name == plan.route } }) { "药物或途径无效" }
        val settings = dao.exportSettings()
        val product = MedicationProducts.forPlan(schedule, settings)
        require(product == null || product.medicationId == plan.medicationId) { "药品规格与药物不匹配" }
        require(schedule.productId == null || MedicationProducts.all(settings).any { it.id == schedule.productId }) { "药品规格不存在" }
        schedule.stockId?.let { id ->
            val stock = dao.exportStock().single { it.id == id && it.medicationId == plan.medicationId }
            MedicationProducts.stockAmount(plan.dose, plan.unit, stock, product, schedule.doseBasis, EstradiolMass.fraction(substances.getAllSubstances().singleOrNull { it.id == plan.medicationId }?.identity?.chemicalFormId))
        }
        val old = dao.plan(plan.id)
        if (old != null) {
            val revision = PlanRevision(System.currentTimeMillis(), old, PlanSchedule.read(old, dao.exportSettings()))
            dao.save(HrtModelSetting("plan-revision:${plan.id}:${UUID.randomUUID()}", Json.encodeToString(revision)))
        }
        dao.save(plan)
        dao.save((if (old == null) schedule else schedule.copy(effectiveFrom = System.currentTimeMillis())).setting(plan.id))
    }

    suspend fun skip(plan: HrtPlan, occurrence: Long) = database.withTransaction {
        validateOccurrence(plan, occurrence, allowFuture = true)
        val key = PlanOccurrences.key(plan.id, occurrence)
        require(dao.setting(key) == null) { "本次用药已有处理结果" }
        dao.save(HrtModelSetting(key, "skipped"))
    }

    private suspend fun validateOccurrence(plan: HrtPlan, occurrence: Long, allowFuture: Boolean = false) {
        require(dao.plan(plan.id) == plan) { "计划已变更，请刷新后重试" }
        val schedule = PlanSchedule.read(plan, dao.exportSettings())
        require(PlanOccurrences.between(plan, schedule, occurrence, occurrence).singleOrNull() == occurrence) { "不是有效的计划时点" }
        require(allowFuture || occurrence <= System.currentTimeMillis()) { "不能提前确认未来用药" }
    }
    suspend fun identify(ingestions: List<Ingestion>, customNames: Set<String>) {
        val catalog = substances.getAllSubstances().map { IdentityEntry(it.id, it.name, it.commonNames, it.availableRoutesForIngestion().map { route -> route.name }.toSet()) }
        val index = MedicationIdentityIndex(catalog, customNames)
        for (batch in ingestions.chunked(128)) database.withTransaction { batch.forEach { event ->
            if (dao.event(event.id) != event) return@forEach
            val old = dao.link(event.id)
            if (old?.origin in setOf("manual", "blocked", "custom-collision") && old?.originalName == event.substanceName) return@forEach
            val id = index.resolve(event.substanceName, event.administrationRoute.name)
            val collision = customNames.any { MedicationIdentity.normalize(it) == MedicationIdentity.normalize(event.substanceName) }
            val link = MedicationLink(event.id, id, if (id == null) "needs_confirmation" else "identified", event.substanceName, if (collision) "custom-collision" else "automatic")
            if (link != old) dao.save(link)
        } }
    }
    suspend fun associate(event: Ingestion, medicationId: String?) = database.withTransaction {
        require(dao.event(event.id) == event) { "记录已变更，请刷新后重试" }
        if (medicationId != null) require(substances.getAllSubstances().any { it.id == medicationId && event.administrationRoute in it.availableRoutesForIngestion() }) { "药物不支持这条记录的给药途径" }
        val old = dao.link(event.id)
        if (old?.medicationId != medicationId) {
            require(InventoryLedger.forEvent(dao.exportMovements(), event.id).none { it.amount < 0 }) { "该记录关联库存，请先在日记中撤销该记录后重新录入" }
            dao.setting("event-product:${event.id}")?.let { snapshot ->
                dao.save(HrtModelSetting("event-product-revision:${event.id}:${UUID.randomUUID()}", snapshot.payload))
                dao.deleteSetting(snapshot.id)
            }
            dao.detail(event.id)?.let { detail -> dao.save(detail.copy(formulation = null, strengthMg = null, patchRateMcgDay = null, patchRemovedAt = null, doseBasis = null)) }
        }
        dao.save(MedicationLink(event.id, medicationId, if (medicationId == null) "needs_confirmation" else "identified", event.substanceName, if (medicationId == null) "blocked" else "manual"))
    }
    suspend fun associateBatch(events: List<Ingestion>, medicationId: String): String = database.withTransaction {
        require(events.isNotEmpty() && events.size <= 5000 && events.map { it.id }.distinct().size == events.size) { "请选择 1–5000 条不同的记录" }
        val anchor = events.first()
        require(MedicationLinkPreview.matchingEvents(anchor, events).size == events.size) { "批量操作仅限同名、同途径和同对象的记录" }
        val changes = events.map { event ->
            val before = dao.link(event.id)
            val detail = dao.detail(event.id)
            val product = dao.setting("event-product:${event.id}")
            associate(event, medicationId)
            MedicationLinkChange(LinkEventFingerprint.of(event), before, requireNotNull(dao.link(event.id)), detail, dao.detail(event.id), product, dao.setting("event-product:${event.id}"))
        }
        val id = UUID.randomUUID().toString()
        dao.save(HrtModelSetting("link-batch:$id", Json.encodeToString(MedicationLinkBatch(id, System.currentTimeMillis(), changes))))
        id
    }

    suspend fun undoAssociationBatch(id: String) = database.withTransaction {
        val key = "link-batch:$id"
        val batch = Json.decodeFromString<MedicationLinkBatch>(requireNotNull(dao.setting(key)) { "批次不存在" }.payload)
        require(!batch.undone) { "已撤销此批次" }
        batch.changes.forEach { change ->
            val eventId = change.event.id
            require(MedicationLinkPreview.canUndo(change, dao.event(eventId), dao.link(eventId), dao.detail(eventId), dao.setting("event-product:$eventId"))) { "记录或补全信息已在之后变更，不能覆盖这些修改" }
        }
        batch.changes.forEach { change ->
            if (change.before == null) dao.unlink(change.event.id) else dao.save(change.before)
            if (change.beforeDetail == null) dao.deleteDetail(change.event.id) else dao.save(change.beforeDetail)
            if (change.beforeProduct == null) dao.deleteSetting("event-product:${change.event.id}") else dao.save(change.beforeProduct)
        }
        dao.save(HrtModelSetting(key, Json.encodeToString(batch.copy(undone = true))))
    }
    suspend fun export() = HrtBackup(dao.exportPlans(), dao.exportStock(), dao.exportMovements(), dao.exportLabs(), dao.exportSettings())
    suspend fun saveProduct(product: MedicationProduct) = database.withTransaction {
        product.validate()
        require(substances.getAllSubstances().any { it.id == product.medicationId }) { "药物不存在" }
        val old = MedicationProducts.all(dao.exportSettings()).singleOrNull { it.id == product.id }
        require(old == null || old.medicationId == product.medicationId && MedicationProducts.unitKey(old.unit) == MedicationProducts.unitKey(product.unit)) { "已有规格不能改成另一种药物或计数单位，请新建规格" }
        dao.save(product.setting())
    }
    suspend fun saveLab(lab: HrtLab) {
        LabTrends.normalize(lab)
        dao.save(lab)
    }
    suspend fun saveLabs(labs: List<HrtLab>, contexts: List<LabDoseContext>) = database.withTransaction {
        require(labs.isNotEmpty() && labs.map { it.id }.distinct().size == labs.size)
        labs.forEach { LabTrends.normalize(it) }
        contexts.forEach { require(it.labId in labs.map { lab -> lab.id }); require(it.elapsedHours == null || it.elapsedHours.isFinite() && it.elapsedHours >= 0) }
        labs.forEach { dao.save(it) }
        contexts.forEach { dao.save(it.setting()) }
    }
    suspend fun deleteLab(lab: HrtLab) = database.withTransaction {
        dao.delete(lab); dao.deleteSetting("lab-dose:${lab.id}")
    }

    suspend fun saveStock(stock: HrtStock, product: MedicationProduct?, opening: Double?) = database.withTransaction {
        require(stock.name.isNotBlank() && stock.unit.isNotBlank()) { "名称和单位不能为空" }
        require(substances.getAllSubstances().any { it.id == stock.medicationId })
        val old = dao.exportStock().singleOrNull { it.id == stock.id }
        if (old != null) require(old.medicationId == stock.medicationId && MedicationProducts.unitKey(old.unit) == MedicationProducts.unitKey(stock.unit)) { "已有库存不能更改药物和计数单位，请新建一项" }
        if (product != null) {
            product.validate()
            require(product.medicationId == stock.medicationId && MedicationProducts.unitKey(product.unit) == MedicationProducts.unitKey(stock.unit)) { "药品规格与库存不匹配" }
        }
        dao.save(stock)
        if (product == null) dao.deleteSetting("stock-product:${stock.id}")
        else dao.save(HrtModelSetting("stock-product:${stock.id}", Json.encodeToString(product)))
        if (old == null) {
            val quantity = requireNotNull(opening)
            require(quantity.isFinite() && quantity >= 0)
            dao.save(StockMovement(UUID.randomUUID().toString(), stock.id, quantity, "opening:${stock.id}", "opening", System.currentTimeMillis()))
        }
    }

    suspend fun adjustStock(stockId: String, delta: Double, reason: String) = database.withTransaction {
        require(dao.exportStock().any { it.id == stockId })
        require(delta.isFinite() && delta != 0.0 && reason.isNotBlank()) { "请填写有效的变化数量和原因" }
        require(InventoryLedger.balance(dao.exportMovements().filter { it.stockId == stockId }) + delta >= 0) { "库存不足" }
        val id = UUID.randomUUID().toString()
        dao.save(StockMovement(id, stockId, delta, "manual:$id", reason, System.currentTimeMillis()))
    }

    suspend fun saveEventProduct(event: Ingestion, product: MedicationProduct?, site: String?, removedAt: Long?, basis: DoseBasis?, patchOngoing: Boolean = false) = database.withTransaction {
        require(!patchOngoing || product?.patchReleaseMcgDay != null && removedAt == null) { "仍在佩戴的贴片不能同时填写移除时间" }
        require(dao.event(event.id) == event) { "记录已变更，请刷新后重试" }
        require(removedAt == null || removedAt >= event.time.toEpochMilli()) { "移除时间早于贴上时间" }
        val link = requireNotNull(dao.link(event.id)) { "请先关联药物" }
        if (product != null) require(product.medicationId == link.medicationId) { "药品规格与药物不匹配" }
        val existing = dao.detail(event.id)
        val previousSnapshot = dao.setting("event-product:${event.id}")
        if (previousSnapshot != null) dao.save(HrtModelSetting("event-product-revision:${event.id}:${UUID.randomUUID()}", previousSnapshot.payload))
        dao.save((existing ?: HrtEventDetail(event.id)).copy(formulation = product?.formulation, strengthMg = product?.strengthMgPerUnit, site = site, patchRateMcgDay = product?.patchReleaseMcgDay, patchRemovedAt = removedAt, doseBasis = basis?.name))
        if (product == null) dao.deleteSetting("event-product:${event.id}")
        else dao.save(EventProductSnapshot(product, System.currentTimeMillis(), basis, patchOngoing).setting(event.id))
        reconcileStock(event)
    }
    suspend fun take(plan: HrtPlan, occurrence: Long, stockId: String? = null) = database.withTransaction {
        validateOccurrence(plan, occurrence)
        val key = PlanOccurrences.key(plan.id, occurrence)
        require(dao.setting(key) == null) { "本次用药已有处理结果" }
        val schedule = PlanSchedule.read(plan, dao.exportSettings())
        recordDose(plan, schedule, occurrence, key, stockId)
    }

    private suspend fun recordDose(plan: HrtPlan, schedule: PlanSchedule, occurrence: Long, key: String, stockId: String?) {
        val selectedStockId = stockId ?: schedule.stockId
        val settings = dao.exportSettings()
        val product = if (selectedStockId == null) MedicationProducts.forPlan(schedule, settings) else MedicationProducts.forStock(selectedStockId, settings)
        require(product == null || product.medicationId == plan.medicationId) { "药品规格与计划不匹配" }
        var consumed: Double? = null
        if (selectedStockId != null) {
            val stock = dao.exportStock().single { it.id == selectedStockId }
            require(stock.medicationId == plan.medicationId)
            require(dao.setting("stock-status:$selectedStockId")?.payload != "archived") { "库存已归档" }
            require(stock.expires == null || stock.expires >= System.currentTimeMillis()) { "库存已过期" }
            consumed = MedicationProducts.stockAmount(plan.dose, plan.unit, stock, product, schedule.doseBasis, EstradiolMass.fraction(substances.getAllSubstances().singleOrNull { it.id == plan.medicationId }?.identity?.chemicalFormId))
            require(InventoryLedger.balance(dao.exportMovements().filter { it.stockId == selectedStockId }) >= consumed) { "Insufficient stock" }
        }
        val time = Instant.ofEpochMilli(occurrence)
        val experienceId = database.experienceDao().insert(Experience(0, "HRT", "", sortDate = time, location = null)).toInt()
        val ingestion = Ingestion(substanceName = plan.name, time = time, administrationRoute = AdministrationRoute.valueOf(plan.route), dose = plan.dose, isDoseAnEstimate = false, estimatedDoseStandardDeviation = null, units = plan.unit, experienceId = experienceId, notes = null, stomachFullness = null, consumerName = plan.consumer, customUnitId = null)
        val companions = database.experienceDao().getAllSubstanceCompanions()
        if (companions.none { it.substanceName == plan.name }) {
            val oldColor = MedicationPreferences.color(plan.name, companions, database.experienceDao().getAllCustomSubstances().map { it.name }.toSet()) { name -> substances.getSubstance(name)?.id }
            database.experienceDao().insert(SubstanceCompanion(plan.name, oldColor?.color, oldColor?.customColor))
        }
        val id = database.experienceDao().insert(ingestion).toInt()
        if (selectedStockId != null) dao.save(StockMovement(UUID.randomUUID().toString(), selectedStockId, -requireNotNull(consumed), "$key:$id", "intake:$id", occurrence))
        dao.save(MedicationLink(id, plan.medicationId, "identified", plan.name))
        if (product != null) {
            dao.save(EventProductSnapshot(product, System.currentTimeMillis(), doseBasis = schedule.doseBasis, patchOngoing = product.patchReleaseMcgDay != null).setting(id))
            dao.save(HrtEventDetail(id, formulation = product.formulation, strengthMg = product.strengthMgPerUnit, patchRateMcgDay = product.patchReleaseMcgDay,
                site = schedule.applicationSite, doseBasis = schedule.doseBasis?.name))
        }
        if (product == null && (schedule.doseBasis != null || schedule.applicationSite != null)) dao.save(HrtEventDetail(id, site = schedule.applicationSite, doseBasis = schedule.doseBasis?.name))
        dao.save(HrtModelSetting(key, "taken:$id"))
    }
    suspend fun reconcileStock(event: Ingestion, deleting: Boolean = false) {
        val snapshot = MedicationProducts.eventSnapshot(event.id, dao.exportSettings())
        if (deleting) {
            dao.exportSettings().filter { it.id.startsWith("plan:") && it.payload == "taken:${event.id}" }.forEach { dao.deleteSetting(it.id) }
            dao.deleteSetting("event-product:${event.id}")
        }
        val movements = InventoryLedger.forEvent(dao.exportMovements(), event.id)
        if (movements.isEmpty()) return
        val stockId = movements.map { it.stockId }.distinct().single()
        val stock = dao.exportStock().single { it.id == stockId }
        val link = dao.link(event.id)
        require(deleting || link?.originalName == event.substanceName && link.medicationId == stock.medicationId) { "请先撤销库存关联，再更改药物" }
        val quantity = if (deleting) 0.0 else MedicationProducts.stockAmount(event.dose ?: error("Stock-linked dose is required"), event.units ?: error("Missing dose unit"), stock, snapshot?.product, snapshot?.doseBasis ?: dao.detail(event.id)?.doseBasis?.let(DoseBasis::valueOf), EstradiolMass.fraction(substances.getAllSubstances().singleOrNull { it.id == stock.medicationId }?.identity?.chemicalFormId))
        val delta = InventoryLedger.consumptionDelta(quantity, movements)
        if (delta != 0.0) {
            require(InventoryLedger.balance(dao.exportMovements().filter { it.stockId == stockId }) + delta >= 0) { "Insufficient stock" }
            val id = UUID.randomUUID().toString()
            dao.save(StockMovement(id, stockId, delta, "adjust:$id", "intake:${event.id}", System.currentTimeMillis()))
        }
    }

}
