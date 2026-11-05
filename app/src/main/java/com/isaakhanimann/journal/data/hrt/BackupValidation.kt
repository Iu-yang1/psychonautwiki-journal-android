package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.ui.tabs.settings.JournalExport
import kotlinx.serialization.json.Json

/** Must run before any replacement transaction. */
fun JournalExport.validateForReplacement() {
    require(formatVersion in 1..2) { "Unsupported backup version" }
    require(customUnits.map { it.id }.distinct().size == customUnits.size) { "Duplicate custom unit IDs" }
    require(customRecipes.map { it.id }.distinct().size == customRecipes.size) { "Duplicate recipe IDs" }
    val ingestionIds = experiences.flatMap { it.ingestions }.mapNotNull { it.id }
    require(ingestionIds.size == ingestionIds.distinct().size && ingestionIds.all { it > 0 }) { "Invalid ingestion IDs" }
    if (formatVersion >= 2) require(experiences.flatMap { it.ingestions }.all { it.id != null }) { "Missing ingestion ID" }
    val units = customUnits.map { it.id }.toSet()
    val recipes = customRecipes.map { it.id }.toSet()
    customRecipes.forEach { recipe -> recipe.subcomponents.forEach { component ->
        require(component.customUnitId == null || component.customUnitId in units) { "Missing recipe custom unit" }
        require(component.dose == null || component.dose!!.isFinite()) { "Invalid recipe dose" }
        require(component.customUnitDose == null || component.customUnitDose.isFinite()) { "Invalid recipe custom unit quantity" }
    } }
    customUnits.forEach { unit -> require(unit.dose == null || unit.dose!!.isFinite()) { "Invalid custom unit dose" } }
    experiences.forEach { experience -> experience.ingestions.forEach { ingestion ->
        require(!ingestion.substanceName.isNullOrBlank()) { "Missing medication name" }
        require(ingestion.dose == null || ingestion.dose!!.isFinite()) { "Invalid dose" }
        require(ingestion.customUnitId == null || ingestion.customUnitId in units) { "Missing custom unit" }
        require(ingestion.customRecipeId == null || ingestion.customRecipeId in recipes) { "Missing recipe" }
        require(ingestion.hrtDetail?.strengthMg == null || ingestion.hrtDetail.strengthMg.isFinite() && ingestion.hrtDetail.strengthMg > 0) { "Invalid strength" }
    } }
    experiences.forEach { experience -> experience.timedNotes.forEach { note -> note.photos.forEach { photo ->
        require(photo.imageBase64.isNotBlank()) { "Backup contains a missing photo" }
    } } }
    hrt.plans.forEach {
        require(it.dose.isFinite() && it.dose > 0 && it.intervalHours > 0) { "Invalid HRT plan" }
        PlanSchedule.read(it, hrt.settings).validate(it.start)
    }
    val stockIds = hrt.stock.map { it.id }.toSet()
    require(hrt.settings.map { it.id }.distinct().size == hrt.settings.size) { "Duplicate settings IDs" }
    require(hrt.stock.map { it.id }.distinct().size == hrt.stock.size) { "Duplicate stock IDs" }
    require(hrt.labs.map { it.id }.distinct().size == hrt.labs.size) { "Duplicate lab IDs" }
    require(hrt.plans.map { it.id }.distinct().size == hrt.plans.size) { "Duplicate plan IDs" }
    MedicationProducts.all(hrt.settings).forEach { product -> require(hrt.settings.any { it.id == "product:${product.id}" }) { "Invalid product ID" } }
    hrt.settings.forEach { setting ->
        when {
            setting.id.startsWith("link-batch:") -> {
                val batch = Json.decodeFromString<MedicationLinkBatch>(setting.payload).also(MedicationLinkBatch::validate)
                require(setting.id == "link-batch:${batch.id}") { "Batch ID mismatch" }
            }
            setting.id.startsWith("stock-product:") -> {
                val stock = hrt.stock.singleOrNull { it.id == setting.id.removePrefix("stock-product:") } ?: error("Missing stock for specification")
                val product = Json.decodeFromString<MedicationProduct>(setting.payload).also(MedicationProduct::validate)
                require(product.medicationId == stock.medicationId && MedicationProducts.unitKey(product.unit) == MedicationProducts.unitKey(stock.unit)) { "Stock specification mismatch" }
            }
            setting.id.startsWith("event-product:") -> {
                val id = setting.id.removePrefix("event-product:").toInt()
                require(id in ingestionIds) { "Missing ingestion for specification" }
                Json.decodeFromString<EventProductSnapshot>(setting.payload).product.validate()
            }
        }
    }
    hrt.movements.forEach { require(it.stockId in stockIds && it.amount.isFinite()) { "Invalid inventory movement" } }
    require(hrt.movements.map { it.eventKey }.distinct().size == hrt.movements.size) { "Duplicate inventory movement" }
    hrt.labs.forEach { LabTrends.normalize(it) }
}
