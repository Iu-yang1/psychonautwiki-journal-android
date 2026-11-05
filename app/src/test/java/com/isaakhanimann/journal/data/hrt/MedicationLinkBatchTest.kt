package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.data.room.experiences.entities.Ingestion
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class MedicationLinkBatchTest {
    private fun event(id: Int, consumer: String? = null) = Ingestion(id, "EV", Instant.ofEpochMilli(id.toLong()), administrationRoute = AdministrationRoute.INTRAMUSCULAR,
        dose = 2.0, isDoseAnEstimate = false, estimatedDoseStandardDeviation = null, units = "mg", experienceId = 1, notes = null, stomachFullness = null, consumerName = consumer, customUnitId = null)

    @Test fun batchPreviewSeparatesConsumersRoutesAndRawNames() {
        val anchor = event(1)
        val entries = listOf(anchor, event(2), event(3, ""), event(4, "person B"), event(5).copy(administrationRoute = AdministrationRoute.ORAL), event(6).copy(substanceName = "ev"))
        assertEquals(listOf(2, 1), MedicationLinkPreview.matchingEvents(anchor, entries).map { it.id })
    }

    @Test fun undoRequiresUnchangedDoseAndSupplementalData() {
        val event = event(1)
        val link = MedicationLink(1, "med", "identified", "EV", "manual")
        val detail = HrtEventDetail(1, site = "left")
        val change = MedicationLinkChange(LinkEventFingerprint.of(event), null, link, null, detail, null, null)
        assertTrue(MedicationLinkPreview.canUndo(change, event, link, detail, null))
        assertFalse(MedicationLinkPreview.canUndo(change, event.copy(dose = 3.0), link, detail, null))
        assertFalse(MedicationLinkPreview.canUndo(change, event, link, detail.copy(site = "right"), null))
        assertFalse(MedicationLinkPreview.canUndo(change, null, link, detail, null))
        assertTrue(MedicationLinkPreview.canUndo(change, event.copy(notes = "new note"), link, detail, null))
    }

    @Test fun persistedBatchPreservesPriorIdentityAndOriginalSubject() {
        val event = event(1, "")
        val old = MedicationLink(1, null, "needs_confirmation", "EV")
        val next = old.copy(medicationId = "med", status = "identified", origin = "manual")
        val batch = MedicationLinkBatch("b", 123, listOf(MedicationLinkChange(LinkEventFingerprint.of(event), old, next, null, null, null, null)))
        val decoded = Json.decodeFromString<MedicationLinkBatch>(Json.encodeToString(batch))
        assertEquals(batch, decoded)
        assertEquals("", decoded.changes.single().event.consumer)
        decoded.validate()
    }

    @Test(expected = IllegalArgumentException::class) fun batchCannotReferToAnotherEventsLink() {
        val event = event(1)
        MedicationLinkBatch("bad", 1, listOf(MedicationLinkChange(LinkEventFingerprint.of(event), null, MedicationLink(2, "med", "identified", "EV"), null, null, null, null))).validate()
    }

    @Test fun manualInventoryReasonCannotImpersonateDoseConsumption() {
        val manual = StockMovement("m", "s", -1.0, "manual:one", "intake:1", 0)
        val system = manual.copy(id = "sys", eventKey = "plan:p:123:1")
        assertEquals(listOf(system), InventoryLedger.forEvent(listOf(manual, system), 1))
    }
}
