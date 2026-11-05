package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.data.room.experiences.entities.Ingestion
import com.isaakhanimann.journal.data.substances.classes.Substance

data class NormalizedHormoneRecords(val events: List<HormoneEvent>, val excluded: Map<String, String>, val assumptions: Set<String>)

/** Original doses and names are never rewritten by modelling eligibility checks. */
object HormoneRecordNormalizer {
    fun normalize(
        records: List<Ingestion>, links: List<MedicationLink>, details: List<HrtEventDetail>, settings: List<HrtModelSetting>,
        substances: List<Substance>, catalog: HormoneReferenceCatalog, consumer: String?, nowHours: Double, throughHours: Double,
        plannedRecords: Boolean = false, analyteId: String = "E2",
    ): NormalizedHormoneRecords {
        val linkMap = links.associateBy { it.ingestionId }
        val detailMap = details.associateBy { it.ingestionId }
        val substanceMap = substances.associateBy { it.id }
        val events = mutableListOf<HormoneEvent>()
        val excluded = linkedMapOf<String, String>()
        val assumptions = linkedSetOf<String>()
        records.filter { it.consumerName == consumer }.forEach { record ->
            val id = record.id.toString()
            val medicationId = linkMap[record.id]?.medicationId
            val substance = substanceMap[medicationId]
            val time = record.time.toEpochMilli() / 3600000.0
            fun reject(reason: String) { excluded[id] = reason }
            if (time > nowHours && !plannedRecords) { reject("future_actual_record"); return@forEach }
            if (medicationId == null || substance == null) { reject("needs_identity"); return@forEach }
            val snapshot = MedicationProducts.eventSnapshot(record.id, settings)
            val basisText = detailMap[record.id]?.doseBasis
            val basis = snapshot?.doseBasis ?: basisText?.let { text -> DoseBasis.entries.singleOrNull { it.name == text } }
            if (basisText != null && basis == null) { reject("unknown_dose_basis"); return@forEach }
            val product = snapshot?.product
            val selected = product?.modelProfileId?.let { id -> catalog.models.singleOrNull { it.id == id } }
            if (product?.modelProfileId != null && selected == null) { reject("select_reference_profile"); return@forEach }
            if (selected != null && (!selected.acceptsMedication(medicationId) || record.administrationRoute.name !in selected.administrationRoutes)) { reject("profile_form_or_route_mismatch"); return@forEach }
            val allProfiles = catalog.models.filter { it.acceptsMedication(medicationId) && record.administrationRoute.name in it.administrationRoutes }
            val compatible = allProfiles.filter { it.analyteId == analyteId }
            if (compatible.isEmpty()) {
                reject(if (allProfiles.isNotEmpty()) "other_analyte_record" else if (substance.identity?.ingredientId != "ingredient:estradiol") "non_e2_record" else if (catalog.models.any { it.acceptsMedication(medicationId) }) "route_model_unverified" else "unsupported_parameter_set")
                return@forEach
            }
            if (substance.identity?.formulationId == null && product == null) { reject("needs_formulation"); return@forEach }
            if (basis == DoseBasis.ACTIVE_E2 && substance.identity?.ingredientId != "ingredient:estradiol") { reject("dose_basis_not_supported"); return@forEach }
            val resolved = MedicationProducts.resolve(record.dose, record.units, medicationId, product, basis)
            if (resolved.missing != null) { reject("missing_dose_spec:${resolved.missing}"); return@forEach }
            val choices = compatible.filter { candidate ->
                selected == null || candidate.id == selected.id || selected.referenceFamily != null && candidate.referenceFamily == selected.referenceFamily
            }
            fun driver(profile: HormoneReferenceProfile): Double? {
                return when (profile.driverUnit) {
                    HormoneReferenceDriver.ACTIVE_E2_MG -> resolved.activeE2Mg ?: resolved.chemicalFormMg?.times(profile.activeFraction)
                    HormoneReferenceDriver.CHEMICAL_FORM_MG -> resolved.chemicalFormMg ?: resolved.activeE2Mg?.div(profile.activeFraction)
                    HormoneReferenceDriver.NOMINAL_E2_MG_PER_HOUR -> resolved.patchReleaseMcgDay?.div(24000.0)
                }
            }
            val dosed = choices.filter { candidate -> candidate.referenceDoseMg == null || driver(candidate)?.let { kotlin.math.abs(it-candidate.referenceDoseMg) <= 1e-6 } == true }
            val profile = dosed.singleOrNull()
            if (profile == null) {
                reject(if (choices.isNotEmpty() && choices.all { it.referenceDoseMg != null }) "unsupported_reference_dose" else "select_reference_profile"); return@forEach
            }
            if (selected == null && (profile.requiresProduct || profile.route in setOf(HormoneRoute.GEL, HormoneRoute.PATCH, HormoneRoute.SPRAY))) {
                reject("select_reference_profile"); return@forEach
            }
            val form = substance.identity?.formulationId ?: product?.formulation
            if (!profile.acceptsMedication(medicationId) || substance.identity?.chemicalFormId != profile.chemicalFormId || form != profile.formulationId || !profile.reviewed) {
                reject("profile_form_or_route_mismatch"); return@forEach
            }
            if (profile.route in setOf(HormoneRoute.GEL, HormoneRoute.PATCH, HormoneRoute.SPRAY)) {
                val site = detailMap[record.id]?.site
                if (site.isNullOrBlank()) { reject("missing_application_site"); return@forEach }
                val key = MedicationIdentity.normalize(site)
                val legacySites = if (profile.route == HormoneRoute.GEL) setOf("arm", "upperarm", "forearm", "leftarm", "rightarm", "leftupperarm", "rightupperarm", "上臂", "手臂", "前臂", "左臂", "右臂", "左上臂", "右上臂", "左前臂", "右前臂")
                    else setOf("abdomen", "lowerabdomen", "leftabdomen", "rightabdomen", "腹部", "下腹", "左腹部", "右腹部", "左下腹", "右下腹")
                val allowed = if (profile.allowedSites.isNotEmpty()) profile.allowedSites.map(MedicationIdentity::normalize).toSet() else legacySites
                if (key !in allowed) { reject("reference_application_site_mismatch"); return@forEach }
            }
            if (profile.route == HormoneRoute.PATCH) {
                val rate = resolved.patchReleaseMcgDay
                if (rate == null) { reject("missing_patch_release_rate"); return@forEach }
                val recordedEnd = detailMap[record.id]?.patchRemovedAt?.div(3600000.0) ?: record.endTime?.toEpochMilli()?.div(3600000.0)
                val end = recordedEnd ?: if (snapshot?.patchOngoing == true) throughHours else null
                if (end == null || end <= time) { reject("missing_patch_wear_interval"); return@forEach }
                if (recordedEnd == null) assumptions += "仍在佩戴的贴片假定持续到显示范围末尾；未来部分为情景估算。"
                if (recordedEnd != null && recordedEnd > nowHours) assumptions += "未来的贴片结束时间按计划移除情景处理。"
                events += HormoneEvent(id, profile.medicationId, profile.route, time, null, end, rate/24000.0, profile.id, planned = plannedRecords)
            } else {
                val mass = driver(profile)
                if (mass == null) { reject("missing_chemical_mass"); return@forEach }
                if (!mass.isFinite() || mass <= 0) { reject("dose_out_of_range"); return@forEach }
                events += HormoneEvent(id, profile.medicationId, profile.route, time, mass, profileId = profile.id, planned = plannedRecords)
                assumptions += profile.assumptions

            }
        }
        return NormalizedHormoneRecords(events, excluded, assumptions)
    }
}
