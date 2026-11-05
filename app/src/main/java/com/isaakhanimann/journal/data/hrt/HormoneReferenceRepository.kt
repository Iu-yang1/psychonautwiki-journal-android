package com.isaakhanimann.journal.data.hrt

import android.content.Context
import com.isaakhanimann.journal.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class HormoneReferenceSource(val url: String? = null, val urls: List<String> = emptyList(), val kind: String, val population: String? = null)
@Serializable enum class HormoneReferenceDriver { ACTIVE_E2_MG, NOMINAL_E2_MG_PER_HOUR, CHEMICAL_FORM_MG }

@Serializable
data class HormoneReferenceProfile(
    val id: String, val name: String, val medicationId: String, val route: HormoneRoute,
    val administrationRoutes: List<String>, val chemicalFormId: String, val formulationId: String,
    val kernel: HormoneKernel, val absorptionPerHour: Double, val eliminationPerHour: Double,
    val driverUnit: HormoneReferenceDriver,
    val transferPerHour: Double? = null, val responseScale: Double, val activeFraction: Double,
    val version: String, val sources: List<String>, val assumptions: List<String>, val reviewed: Boolean,
    val analyteId: String = "E2", val requiresProduct: Boolean = false,
    val allowedSites: List<String> = emptyList(), val compatibleMedicationIds: List<String> = emptyList(),
    val referenceFamily: String? = null, val referenceDoseMg: Double? = null,
    val lagHours: Double = 0.0, val fastEliminationPerHour: Double? = null, val fastFraction: Double? = null,
) {
    fun acceptsMedication(id: String) = id == medicationId || id in compatibleMedicationIds
    fun parameters() = HormoneParameters(medicationId, route, version, sources.joinToString(), reviewed,
        absorptionPerHour, eliminationPerHour, responseScale, kernel = kernel, transferPerHour = transferPerHour, id = id,
        lagHours = lagHours, fastEliminationPerHour = fastEliminationPerHour, fastFraction = fastFraction)
}

@Serializable
data class HormoneReferenceCatalog(
    val version: String, val sources: Map<String, HormoneReferenceSource>, val assumptions: List<String>,
    val models: List<HormoneReferenceProfile>,
) {
    fun validate() {
        require(models.isNotEmpty() && models.map { it.id }.distinct().size == models.size)
        models.forEach { model ->
            require(model.version.isNotBlank() && model.reviewed && model.administrationRoutes.isNotEmpty())
            require(model.activeFraction.isFinite() && model.activeFraction > 0 && model.activeFraction <= 1)
            require((model.driverUnit == HormoneReferenceDriver.NOMINAL_E2_MG_PER_HOUR) == (model.route == HormoneRoute.PATCH))
            require(model.analyteId in setOf("E2", "EE", "E4", "E1", "EQUILIN"))
            require(model.referenceDoseMg == null || model.referenceDoseMg.isFinite() && model.referenceDoseMg > 0)
            require(model.lagHours.isFinite() && model.lagHours >= 0)
            require(model.sources.isNotEmpty() && model.sources.all { it in sources })
            require(model.assumptions.isNotEmpty())
            require(model.absorptionPerHour > 0 && model.eliminationPerHour > 0 && model.responseScale > 0)
        }
    }

    companion object {
        private val codec = Json { ignoreUnknownKeys = true }
        fun parse(text: String): HormoneReferenceCatalog = codec.decodeFromString<HormoneReferenceCatalog>(text).also { it.validate() }
    }
}

@Singleton
class HormoneReferenceRepository @Inject constructor(@param:ApplicationContext context: Context) {
    val catalog = context.resources.openRawResource(R.raw.hrt_reference_models).bufferedReader().use { HormoneReferenceCatalog.parse(it.readText()) }
}
