package com.isaakhanimann.journal.data.hrt

import com.isaakhanimann.journal.ui.tabs.settings.JournalExport
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class HrtBackupTest {
    private val product = MedicationProduct("p", "med", "Gel", "gel", "g", 0.6)
    private val stock = HrtStock("s", "med", "Batch", "g", 123456789)

    @Test fun oldBackupStillDecodesWithoutNewFields() {
        val backup = Json.decodeFromString<JournalExport>("{}")
        backup.validateForReplacement()
        assertEquals(1, backup.formatVersion)
        assertTrue(backup.hrt.stock.isEmpty())
    }

    @Test fun productAndFrozenStockSpecificationRoundTrip() {
        val metadata = listOf(product.setting(), HrtModelSetting("stock-product:s", Json.encodeToString(product)))
        val backup = JournalExport(formatVersion = 2, hrt = HrtBackup(stock = listOf(stock), settings = metadata))
        val restored = Json.decodeFromString<JournalExport>(Json.encodeToString(backup))
        restored.validateForReplacement()
        assertEquals(product, MedicationProducts.forStock("s", restored.hrt.settings))
        assertEquals(backup, restored)
    }

    @Test(expected = IllegalArgumentException::class) fun duplicateMetadataKeysCannotOverwriteSnapshots() {
        JournalExport(hrt = HrtBackup(settings = listOf(product.setting(), product.setting()))).validateForReplacement()
    }

    @Test(expected = IllegalStateException::class) fun missingStockCannotReceiveSpecification() {
        JournalExport(hrt = HrtBackup(settings = listOf(HrtModelSetting("stock-product:s", Json.encodeToString(product))))).validateForReplacement()
    }

    @Test(expected = IllegalArgumentException::class) fun mismatchedStockMedicationCannotBeImported() {
        JournalExport(hrt = HrtBackup(stock = listOf(stock.copy(medicationId = "other")), settings = listOf(HrtModelSetting("stock-product:s", Json.encodeToString(product))))).validateForReplacement()
    }
}
