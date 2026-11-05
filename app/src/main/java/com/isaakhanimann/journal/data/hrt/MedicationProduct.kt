package com.isaakhanimann.journal.data.hrt

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import java.math.BigDecimal
import java.math.MathContext

@Serializable
data class MedicationProduct(
    val id: String,
    val medicationId: String,
    val name: String,
    val formulation: String,
    val unit: String,
    val strengthMgPerUnit: Double? = null,
    val patchReleaseMcgDay: Double? = null,
    val archived: Boolean = false,
    val modelProfileId: String? = null,
    val sourceUrl: String? = null,
) {
    fun validate() {
        require(id.isNotBlank() && medicationId.isNotBlank() && name.isNotBlank() && unit.isNotBlank()) { "药品名称、药物和单位不能为空" }
        require(strengthMgPerUnit == null || strengthMgPerUnit.isFinite() && strengthMgPerUnit > 0) { "每单位含量必须大于零" }
        require(patchReleaseMcgDay == null || patchReleaseMcgDay.isFinite() && patchReleaseMcgDay > 0) { "贴片释放速率无效" }
        require(patchReleaseMcgDay == null || MedicationProducts.unitKey(unit) == "patch") { "释放速率仅可用于贴片计数单位" }
        require(sourceUrl == null || sourceUrl.startsWith("https://")) { "规格来源链接无效" }
    }
    fun setting() = HrtModelSetting("product:$id", Json.encodeToString(this))
}

@Serializable
data class EventProductSnapshot(val product: MedicationProduct, val recordedAt: Long, val doseBasis: DoseBasis? = null, val patchOngoing: Boolean = false) {
    fun setting(eventId: Int) = HrtModelSetting("event-product:$eventId", Json.encodeToString(this))
}

data class ResolvedDose(val chemicalFormMg: Double? = null, val patchReleaseMcgDay: Double? = null, val missing: String? = null, val activeE2Mg: Double? = null)

@Serializable
enum class DoseBasis { PRODUCT, CHEMICAL_FORM, ACTIVE_E2 }

object EstradiolMass {
    fun fraction(chemicalFormId: String?): Double? = when (chemicalFormId) {
        "chemical:estradiol" -> 1.0
        "chemical:estradiol-valerate" -> 272.38 / 356.50
        "chemical:estradiol-cypionate" -> 272.38 / 396.5622
        "chemical:estradiol-enanthate" -> 272.38 / 384.56
        else -> null
    }
}

object MedicationProducts {
    fun all(settings: List<HrtModelSetting>): List<MedicationProduct> = settings.filter { it.id.startsWith("product:") }.map {
        val product = Json.decodeFromString<MedicationProduct>(it.payload).also(MedicationProduct::validate)
        require(it.id == "product:${product.id}") { "药品规格 ID 不匹配" }
        product
    }.also { products -> require(products.map { it.id }.distinct().size == products.size) { "重复的药品规格" } }

    fun forStock(stockId: String, settings: List<HrtModelSetting>): MedicationProduct? =
        settings.singleOrNull { it.id == "stock-product:$stockId" }?.let { Json.decodeFromString<MedicationProduct>(it.payload).also(MedicationProduct::validate) }

    fun forEvent(eventId: Int, settings: List<HrtModelSetting>): MedicationProduct? =
        eventSnapshot(eventId, settings)?.product

    fun eventSnapshot(eventId: Int, settings: List<HrtModelSetting>): EventProductSnapshot? =
        settings.singleOrNull { it.id == "event-product:$eventId" }?.let { Json.decodeFromString<EventProductSnapshot>(it.payload).also { snapshot -> snapshot.product.validate() } }

    fun forPlan(schedule: PlanSchedule, settings: List<HrtModelSetting>): MedicationProduct? {
        if (schedule.stockId != null) return forStock(schedule.stockId, settings)
        return schedule.productId?.let { id -> all(settings).singleOrNull { it.id == id } }
    }

    fun unitKey(unit: String): String = when (val key = unit.trim().lowercase(Locale.ROOT)) {
        "片", "tablet", "tablets", "pill", "pills" -> "tablet"
        "泵", "pump", "pumps" -> "pump"
        "毫升", "ml" -> "ml"
        "贴", "patch", "patches" -> "patch"
        "喷", "spray", "sprays" -> "spray"
        "毫克", "mg" -> "mg"
        "微克", "mcg", "ug", "μg", "µg" -> "mcg"
        "克", "g" -> "g"
        "mg e2", "mg e2 eq", "mg e2eq", "mg e2-equivalent", "mg e2 equivalent" -> "mg-e2"
        "mcg/day", "ug/day", "μg/day", "µg/day", "mcg/d", "ug/d", "μg/d", "µg/d", "mcg/24h", "μg/24h", "µg/24h", "微克/天" -> "mcg/day"
        "mg/day", "mg/d", "mg/24h", "毫克/天" -> "mg/day"
        else -> key
    }

    fun massMg(amount: Double, unit: String): Double? = when (unitKey(unit)) {
        "mg" -> amount
        "mcg" -> amount / 1000.0
        "g" -> amount * 1000.0
        else -> null
    }

    fun resolve(amount: Double?, unit: String?, medicationId: String, product: MedicationProduct?, basis: DoseBasis? = null): ResolvedDose {
        if (amount == null || !amount.isFinite() || amount <= 0) return ResolvedDose(missing = "缺少有效剂量")
        if (unit == null) return ResolvedDose(missing = "缺少单位")
        if (product != null && product.medicationId != medicationId) return ResolvedDose(missing = "药品规格与药物不匹配")
        if (basis == DoseBasis.ACTIVE_E2 || unitKey(unit) == "mg-e2") {
            val mass = if (unitKey(unit) == "mg-e2") amount else massMg(amount, unit)
            return if (mass != null && mass.isFinite()) ResolvedDose(activeE2Mg = mass) else ResolvedDose(missing = "E2 当量需要质量单位")
        }
        if (unitKey(unit) == "mcg/day") return ResolvedDose(patchReleaseMcgDay = amount)
        if (unitKey(unit) == "mg/day") return if ((amount * 1000).isFinite()) ResolvedDose(patchReleaseMcgDay = amount * 1000) else ResolvedDose(missing = "释放速率超出范围")
        if (product?.strengthMgPerUnit?.let { !it.isFinite() || it <= 0 } == true || product?.patchReleaseMcgDay?.let { !it.isFinite() || it <= 0 } == true) return ResolvedDose(missing = "规格含量无效")
        val usesProduct = basis == DoseBasis.PRODUCT || basis == null && product != null && unitKey(product.unit) == unitKey(unit)
        if (!usesProduct) {
            val mass = massMg(amount, unit)
            return if (mass != null && mass.isFinite()) ResolvedDose(chemicalFormMg = mass) else ResolvedDose(missing = "需要补充药品规格或明确剂量口径")
        }
        if (product == null) return ResolvedDose(missing = "需要补充药品规格")
        val productAmount = if (unitKey(product.unit) == unitKey(unit)) amount else {
            val unitMass = massMg(1.0, product.unit)
            val recordedMass = massMg(amount, unit)
            if (basis != DoseBasis.PRODUCT || unitMass == null || recordedMass == null) return ResolvedDose(missing = "记录单位与规格不一致")
            recordedMass / unitMass
        }
        val patch = product.patchReleaseMcgDay
        if (patch != null) {
            val rate = BigDecimal.valueOf(patch).multiply(BigDecimal.valueOf(productAmount)).toDouble()
            return if (rate.isFinite()) ResolvedDose(patchReleaseMcgDay = rate) else ResolvedDose(missing = "释放速率超出范围")
        }
        val strength = product.strengthMgPerUnit ?: return ResolvedDose(missing = "需要补充每单位含量")
        val result = BigDecimal.valueOf(productAmount).multiply(BigDecimal.valueOf(strength)).toDouble()
        return if (result.isFinite()) ResolvedDose(chemicalFormMg = result) else ResolvedDose(missing = "剂量超出范围")
    }

    fun stockAmount(amount: Double, unit: String, stock: HrtStock, product: MedicationProduct?, basis: DoseBasis? = null, activeFraction: Double? = null): Double {
        require(amount.isFinite() && amount > 0)
        if (unitKey(unit) == unitKey(stock.unit) && basis !in setOf(DoseBasis.CHEMICAL_FORM, DoseBasis.ACTIVE_E2)) return amount
        val resolved = resolve(amount, unit, stock.medicationId, product, basis)
        val mg = if (resolved.activeE2Mg != null) resolved.activeE2Mg / requireNotNull(activeFraction) { "该化学形式尚不支持 E2 当量库存换算" }
            else resolved.chemicalFormMg ?: error("库存扣减需要明确的药品含量和单位")
        val perUnitMg = product?.takeIf { unitKey(it.unit) == unitKey(stock.unit) }?.strengthMgPerUnit ?: massMg(1.0, stock.unit)
            ?: error("库存单位不能换算")
        return BigDecimal.valueOf(mg).divide(BigDecimal.valueOf(perUnitMg), MathContext.DECIMAL128).toDouble().also { require(it.isFinite() && it > 0) }
    }
}
