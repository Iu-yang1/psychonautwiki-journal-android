package com.isaakhanimann.journal.data.hrt

/** Explicitly labeled extrapolation, not a validated sublingual-valerate parameter set. */
object ValerateSublingualBridge {
    const val MASS_FRACTION = 272.38 / 356.50
    const val EVIDENCE_URL = "https://pmc.ncbi.nlm.nih.gov/articles/PMC6333764/"

    fun events(data: HormoneJournalData, catalog: HormoneReferenceCatalog, consumer: String?, nowHours: Double, planned: Boolean = false): List<HormoneEvent> {
        val profile = catalog.models.single { it.id == "e2-sl-reference-v1" }
        val medications = data.substances.associateBy { it.id }
        val links = data.links.associateBy { it.ingestionId }
        val details = data.details.associateBy { it.ingestionId }
        return data.records.filter { it.consumerName == consumer && it.administrationRoute.name == "SUBLINGUAL" }.mapNotNull { record ->
            val medication = medications[links[record.id]?.medicationId] ?: return@mapNotNull null
            if (medication.identity?.chemicalFormId != "chemical:estradiol-valerate" || medication.identity.formulationId != "form:oral-solid") return@mapNotNull null
            val time = record.time.toEpochMilli()/3600000.0
            if (!planned && time > nowHours) return@mapNotNull null
            val snapshot = MedicationProducts.eventSnapshot(record.id, data.settings)
            val basis = snapshot?.doseBasis ?: details[record.id]?.doseBasis?.let { value -> DoseBasis.entries.singleOrNull { it.name == value } }
            val resolved = MedicationProducts.resolve(record.dose, record.units, medication.id, snapshot?.product, basis)
            val mass = resolved.activeE2Mg ?: resolved.chemicalFormMg?.times(MASS_FRACTION) ?: return@mapNotNull null
            if (!mass.isFinite() || mass <= 0) return@mapNotNull null
            // Only the in-memory driver uses the E2 reference identity. Stored medication IDs/routes remain intact.
            HormoneEvent(record.id.toString(), profile.medicationId, profile.route, time, mass, profileId = profile.id, planned = planned)
        }
    }
}
