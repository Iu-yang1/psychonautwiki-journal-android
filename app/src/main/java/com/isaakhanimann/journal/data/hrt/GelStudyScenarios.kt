package com.isaakhanimann.journal.data.hrt

/** A research scenario never supplies an unknown vehicle/site to the stored journal. */
data class GelStudyScenario(val id: String, val label: String, val history: List<HormoneSample>, val forecast: List<HormoneSample>, val recordIds: List<String>, val profileIds: Set<String>, val modelEvents: List<HormoneEvent> = emptyList())

object GelStudyScenarios {
    fun calculate(data: HormoneJournalData, request: HormoneChartRequest, excluded: Map<String, String>, from: Double, now: Double, through: Double, catalog: HormoneReferenceCatalog, checkActive: () -> Unit, confirmedEvents: List<HormoneEvent> = emptyList()): List<GelStudyScenario> {
        if (request.analyteId != "E2" || request.gelReference == "confirmed") return emptyList()
        val medications = data.substances.associateBy { it.id }
        val links = data.links.associateBy { it.ingestionId }
        val details = data.details.associateBy { it.ingestionId }
        val candidates = data.records.filter { record ->
            val medication = medications[links[record.id]?.medicationId]
            val reason = excluded[record.id.toString()]
            record.consumerName == request.consumer && record.time.toEpochMilli()/3600000.0 <= now && record.dose?.let { it.isFinite() && it > 0 } == true &&
                record.units?.let(MedicationProducts::unitKey) == "mg" && record.administrationRoute.name == "TRANSDERMAL" &&
                medication?.identity?.ingredientId == "ingredient:estradiol" && medication.identity.formulationId == "form:gel" &&
                (reason in setOf("select_reference_profile", "missing_application_site") || reason?.startsWith("missing_dose_spec:") == true)
        }
        if (candidates.isEmpty()) return emptyList()
        val requested = when (request.gelReference) {
            "estrogel" -> listOf("e2-gel006-reference-v1")
            "divigel" -> listOf("e2-divigel-reference-v1")
            else -> listOf("e2-gel006-reference-v1", "e2-divigel-reference-v1")
        }
        val parameters = catalog.models.map { it.parameters() }
        return requested.mapNotNull { target ->
            checkActive()
            val fallback = catalog.models.single { it.id == target }
            val events = candidates.mapNotNull { record ->
                val snapshot = MedicationProducts.eventSnapshot(record.id, data.settings)
                val selected = snapshot?.product?.modelProfileId?.let { id -> catalog.models.singleOrNull { it.id == id && it.route == HormoneRoute.GEL } }
                val site = details[record.id]?.site
                val profiles = catalog.models.filter { it.route == HormoneRoute.GEL && it.acceptsMedication(requireNotNull(links[record.id]?.medicationId)) }
                fun permits(profile: HormoneReferenceProfile): Boolean {
                    if (site.isNullOrBlank()) return true
                    val allowed = profile.allowedSites.ifEmpty { listOf("arm", "upperarm", "forearm", "leftarm", "rightarm", "上臂", "手臂", "前臂", "左上臂", "右上臂", "左臂", "右臂") }
                    return allowed.any { MedicationIdentity.normalize(it) == MedicationIdentity.normalize(site) }
                }
                val profile = if (selected != null) selected.takeIf(::permits)
                    else fallback.takeIf(::permits) ?: profiles.filter(::permits).singleOrNull()
                profile ?: return@mapNotNull null
                // Legacy gel mg is already active estradiol mass. No concentration/count conversion.
                HormoneEvent(record.id.toString(), profile.medicationId, profile.route, record.time.toEpochMilli()/3600000.0,
                    record.dose, profileId = profile.id)
            }
            if (events.isEmpty()) return@mapNotNull null
            val engine = HormoneSimulationEngine()
            val fastTimes = confirmedEvents.filter { it.route == HormoneRoute.SUBLINGUAL && it.timeHours in (from - 24)..now }.flatMap { event -> listOf(.1, .25, .5, .75, 1.0, 1.5, 2.0, 3.0, 4.0, 8.0, 12.0).map { event.timeHours + it } }.distinct().take(5000)
            val history = engine.simulate(confirmedEvents + events, parameters, from, now, now, points = 601, additionalTimes = fastTimes, checkActive = checkActive)
            val forecast = if (through > now) engine.simulate(confirmedEvents + events, parameters, now, through, now, points = 301, checkActive = checkActive).samples else emptyList()
            GelStudyScenario(target, if (target.contains("gel006")) "EstroGel上臂资料情景" else "Divigel上大腿资料情景", history.samples, forecast,
                events.map { it.id }, events.mapNotNull { it.profileId }.toSet(), confirmedEvents + events)
        }
    }
}
