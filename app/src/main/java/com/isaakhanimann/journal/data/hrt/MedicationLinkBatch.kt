package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.data.room.experiences.entities.Ingestion
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.isaakhanimann.journal.data.substances.AdministrationRoute

@Serializable
data class LinkEventFingerprint(
    val id: Int, val name: String, val time: Long, val end: Long?, val route: String,
    val dose: Double?, val units: String?, val consumer: String?, val customUnitId: Int?,
    val customRecipeId: Int?, val recipeGroupId: String?, val estimated: Boolean,
) {
    companion object {
        fun of(event: Ingestion) = LinkEventFingerprint(event.id, event.substanceName, event.time.toEpochMilli(), event.endTime?.toEpochMilli(),
            event.administrationRoute.name, event.dose, event.units, event.consumerName, event.customUnitId, event.customRecipeId, event.recipeGroupId, event.isDoseAnEstimate)
    }
}

@Serializable
data class MedicationLinkChange(
    val event: LinkEventFingerprint,
    val before: MedicationLink?, val after: MedicationLink,
    val beforeDetail: HrtEventDetail?, val afterDetail: HrtEventDetail?,
    val beforeProduct: HrtModelSetting?, val afterProduct: HrtModelSetting?,
)

@Serializable
data class MedicationLinkBatch(val id: String, val createdAt: Long, val changes: List<MedicationLinkChange>, val undone: Boolean = false) {
    fun validate() {
        require(id.isNotBlank() && changes.isNotEmpty() && changes.size <= 5000 && changes.map { it.event.id }.distinct().size == changes.size) { "批次记录无效" }
        changes.forEach { change ->
            val eventId = change.event.id
            require(eventId > 0 && change.after.ingestionId == eventId && (change.before == null || change.before.ingestionId == eventId))
            require(change.after.originalName == change.event.name)
            AdministrationRoute.valueOf(change.event.route)
            require(change.beforeDetail == null || change.beforeDetail.ingestionId == eventId)
            require(change.afterDetail == null || change.afterDetail.ingestionId == eventId)
            listOfNotNull(change.beforeProduct, change.afterProduct).forEach { snapshot ->
                require(snapshot.id == "event-product:$eventId")
                Json.decodeFromString<EventProductSnapshot>(snapshot.payload).product.validate()
            }
        }
    }
}

object MedicationLinkPreview {
    fun matchingEvents(anchor: Ingestion, events: List<Ingestion>): List<Ingestion> = events.filter {
        it.substanceName == anchor.substanceName && it.administrationRoute == anchor.administrationRoute && it.consumerName == anchor.consumerName
    }.sortedByDescending { it.time }

    fun canUndo(change: MedicationLinkChange, event: Ingestion?, link: MedicationLink?, detail: HrtEventDetail?, product: HrtModelSetting?): Boolean =
        event != null && LinkEventFingerprint.of(event) == change.event && link == change.after && detail == change.afterDetail && product == change.afterProduct
}
