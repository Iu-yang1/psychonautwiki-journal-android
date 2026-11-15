package com.isaakhanimann.journal.data.hrt

import kotlin.math.exp
import kotlin.math.ln

enum class AntiandrogenDrug(val ingredientId: String, val label: String) {
    CPA("ingredient:cyproterone-acetate", "CPA"),
    SPIRONOLACTONE("ingredient:spironolactone", "螺内酯");

    fun options(config: TestosteroneOptions): AntiandrogenOptions? = when (this) {
        CPA -> config.cyproterone
        SPIRONOLACTONE -> config.spironolactone
    }
}

data class AntiandrogenDose(val id: String, val drug: AntiandrogenDrug, val timeHours: Double, val doseMg: Double)
data class AntiandrogenRecord(val id: String, val name: String, val timeMillis: Long, val doseMg: Double?, val reason: String?)
data class AntiandrogenHistory(val doses: List<AntiandrogenDose>, val records: List<AntiandrogenRecord>) {
    companion object {
        fun read(data: HormoneJournalData, consumer: String?, nowMillis: Long, config: TestosteroneOptions?): AntiandrogenHistory {
            val links = data.links.associateBy { it.ingestionId }
            val substances = data.substances.associateBy { it.id }
            val details = data.details.associateBy { it.ingestionId }
            val doses = mutableListOf<AntiandrogenDose>()
            val records = data.records.filter { it.consumerName == consumer }.mapNotNull { record ->
                val medicationId = links[record.id]?.medicationId ?: return@mapNotNull null
                val ingredient = substances[medicationId]?.identity?.ingredientId ?: return@mapNotNull null
                val drug = AntiandrogenDrug.entries.singleOrNull { it.ingredientId == ingredient } ?: return@mapNotNull null
                val snapshot = MedicationProducts.eventSnapshot(record.id, data.settings)
                val basisText = details[record.id]?.doseBasis
                val basis = snapshot?.doseBasis ?: basisText?.let { text -> DoseBasis.entries.singleOrNull { it.name == text } }
                val resolved = MedicationProducts.resolve(record.dose, record.units, medicationId, snapshot?.product, basis)
                val mass = resolved.chemicalFormMg
                val reason = when {
                    record.time.toEpochMilli() > nowMillis -> "未来实际记录未纳入"
                    record.administrationRoute.name != "ORAL" -> "尚无此途径的抗雄暴露模型"
                    basisText != null && basis == null -> "未知剂量口径"
                    resolved.missing != null -> resolved.missing
                    mass == null || !mass.isFinite() || mass <= 0 -> "需要明确有效成分质量"
                    config == null || drug.options(config) == null -> "需填写${drug.label}总 T 情景参数"
                    else -> null
                }
                if (reason == null) doses += AntiandrogenDose(record.id.toString(), drug, record.time.toEpochMilli() / 3600000.0, requireNotNull(mass))
                AntiandrogenRecord(record.id.toString(), drug.label, record.time.toEpochMilli(), mass, reason)
            }.sortedByDescending { it.timeMillis }
            return AntiandrogenHistory(doses.sortedBy { it.timeHours }, records)
        }
    }
}

class AntiandrogenExposure(doses: List<AntiandrogenDose>, private val options: AntiandrogenOptions) {
    private val events = doses.sortedBy { it.timeHours }
    private val elimination = ln(2.0) / options.halfLifeHours
    private var index = 0
    private var remainingMg = 0.0
    private var previous: Double? = null

    init {
        options.validate()
        require(events.all { it.timeHours.isFinite() && it.doseMg.isFinite() && it.doseMg > 0 })
    }

    fun remainingProduction(timeHours: Double): Double {
        val last = previous
        require(timeHours.isFinite() && (last == null || timeHours >= last))
        if (last != null) remainingMg *= exp(-elimination * (timeHours - last))
        while (index < events.size && events[index].timeHours <= timeHours) {
            val event = events[index++]
            remainingMg += event.doseMg * exp(-elimination * (timeHours - event.timeHours))
        }
        previous = timeHours
        val equivalentMgDay = remainingMg * elimination * 24
        val occupancy = if (equivalentMgDay <= options.halfSuppressionMgDay) {
            val ratio = equivalentMgDay / options.halfSuppressionMgDay
            ratio / (1 + ratio)
        } else 1 / (1 + options.halfSuppressionMgDay / equivalentMgDay)
        return 1 - options.maximumSuppression * occupancy
    }
}
