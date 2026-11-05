package com.isaakhanimann.journal.data.hrt

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class MedicationProductTest {
    private val tablet = MedicationProduct("p", "med", "Tablet", "tablet", "tablet", 2.0)
    private val gel = tablet.copy(unit = "g", formulation = "gel", strengthMgPerUnit = 0.6)
    private fun resolve(amount: Double, unit: String, product: MedicationProduct?) = MedicationProducts.resolve(amount, unit, "med", product)

    @Test fun countVolumeAndPumpUseExplicitStrength() {
        assertEquals(4.0, resolve(2.0, "片", tablet).chemicalFormMg!!, 0.0)
        assertEquals(4.0, resolve(0.2, "mL", tablet.copy(unit = "mL", strengthMgPerUnit = 20.0)).chemicalFormMg!!, 1e-12)
        assertEquals(1.5, resolve(2.0, "pump", tablet.copy(unit = "pump", strengthMgPerUnit = 0.75)).chemicalFormMg!!, 0.0)
    }

    @Test fun gelMassIsNotActiveDrugMass() {
        assertEquals(0.6, resolve(1.0, "g", gel).chemicalFormMg!!, 0.0)
        assertEquals(1000.0, MedicationProducts.resolve(1.0, "g", "med", gel, DoseBasis.CHEMICAL_FORM).chemicalFormMg!!, 0.0)
    }

    @Test fun patchRateDoesNotBecomeMass() {
        val result = resolve(2.0, "patch", tablet.copy(unit = "patch", patchReleaseMcgDay = 100.0))
        assertNull(result.chemicalFormMg)
        assertEquals(200.0, result.patchReleaseMcgDay!!, 0.0)
    }

    @Test fun explicitReleaseRateUnitsAreNotTreatedAsPatchCountsOrDrugLoading() {
        val rate = MedicationProducts.resolve(50.0, "μg/day", "med", null)
        assertEquals(50.0, rate.patchReleaseMcgDay!!, 0.0)
        assertNull(rate.chemicalFormMg)
        assertEquals(50.0, MedicationProducts.resolve(.05, "mg/day", "med", null).patchReleaseMcgDay!!, 1e-9)
        assertNull(MedicationProducts.resolve(4.0, "mg", "med", null).patchReleaseMcgDay)
    }

    @Test fun missingAndMismatchedSpecificationsAreRejected() {
        assertNotNull(resolve(1.0, "pump", null).missing)
        assertNotNull(resolve(1.0, "tablet", tablet.copy(medicationId = "other")).missing)
        assertNotNull(resolve(1.0, "mL", tablet).missing)
        assertNotNull(resolve(Double.NaN, "mg", null).missing)
        assertNotNull(resolve(Double.MAX_VALUE, "g", null).missing)
    }

    @Test fun activeMassCanConsumeVolumeOrGelMassStock() {
        val volume = HrtStock("s", "med", "Vial", "mL")
        assertEquals(0.2, MedicationProducts.stockAmount(4.0, "mg", volume, tablet.copy(unit = "mL", strengthMgPerUnit = 20.0)), 1e-12)
        assertEquals(1.0, MedicationProducts.stockAmount(0.6, "mg", volume.copy(unit = "g"), gel), 1e-12)
    }

    @Test fun decimalStrengthDoesNotCreatePhantomInventoryShortage() {
        val product = tablet.copy(unit = "pump", strengthMgPerUnit = 0.1)
        assertEquals(0.3, resolve(3.0, "pump", product).chemicalFormMg!!, 0.0)
    }

    @Test fun snapshotDoesNotFollowLaterProductEdits() {
        val encoded = Json.encodeToString(EventProductSnapshot(tablet, 1234))
        val revised = tablet.copy(strengthMgPerUnit = 3.0)
        val snapshot = Json.decodeFromString<EventProductSnapshot>(encoded)
        assertEquals(4.0, resolve(2.0, "tablet", snapshot.product).chemicalFormMg!!, 0.0)
        assertEquals(6.0, resolve(2.0, "tablet", revised).chemicalFormMg!!, 0.0)
    }

    @Test fun decimalLedgerRefundsAndEditsRemainBalanced() {
        fun entry(amount: Double) = StockMovement("x", "s", amount, "event", "test", 0)
        assertEquals(0.0, InventoryLedger.balance(listOf(entry(0.3), entry(-0.1), entry(-0.2))), 0.0)
        assertEquals(-1.0, InventoryLedger.consumptionDelta(3.0, listOf(entry(-2.0))), 0.0)
        assertEquals(3.0, InventoryLedger.consumptionDelta(0.0, listOf(entry(-2.0), entry(-1.0))), 0.0)
    }

    @Test(expected = IllegalArgumentException::class) fun invalidSpecificationCannotBeSaved() { tablet.copy(strengthMgPerUnit = -1.0).validate() }
    @Test(expected = IllegalArgumentException::class) fun releaseRateRequiresPatchUnit() { tablet.copy(patchReleaseMcgDay = 50.0).validate() }
}
