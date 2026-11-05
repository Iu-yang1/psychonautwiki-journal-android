package com.isaakhanimann.journal.data.hrt

import org.junit.Assert.*
import org.junit.Test

class LabTrendsTest {
    private fun lab(name: String, value: Double, unit: String, consumer: String? = null, time: Long = 1) = HrtLab("$name-$time-$unit", consumer, name, time, value, unit)

    @Test fun hormoneUnitsNormalizeToSeparateAnalytes() {
        assertEquals(100.0, LabTrends.normalize(lab("estradiol", 367.1, "pmol/L")).value, 1e-9)
        assertEquals(100.0, LabTrends.normalize(lab("T", 3.47, "nmol/L")).value, 1e-9)
        assertEquals(1.0, LabTrends.normalize(lab("P4", 3.18, "nmol/L")).value, 1e-9)
        assertEquals("free testosterone", LabTrends.normalize(lab("free testosterone", 1.0, "ng/dL")).analyte)
    }

    @Test fun prolactinAssayUnitsRemainSeparate() {
        val data = listOf(lab("PRL", 10.0, "ng/mL"), lab("PRL", 200.0, "mIU/L"))
        assertEquals(2, LabTrends.series(data, null).size)
    }

    @Test fun consumerFilteringDoesNotMergeNullBlankOrNamedPeople() {
        val data = listOf(lab("E2", 100.0, "pg/mL"), lab("E2", 200.0, "pg/mL", ""), lab("E2", 300.0, "pg/mL", "person B"))
        assertEquals(100.0, LabTrends.series(data, null).values.single().single().value, 0.0)
        assertEquals(200.0, LabTrends.series(data, "").values.single().single().value, 0.0)
        assertEquals(300.0, LabTrends.series(data, "person B").values.single().single().value, 0.0)
    }

    @Test fun censoredValuesKeepComparisonAndOriginalReport() {
        val original = lab("E2", 36.71, "pmol/L").copy(comparison = "<=", method = "LC-MS/MS", referenceRange = "report range")
        val normalized = LabTrends.normalize(original)
        assertEquals("<=", normalized.original.comparison)
        assertEquals(10.0, normalized.value, 1e-9)
        assertEquals(original, normalized.original)
    }

    @Test fun compatibleUnitsSortBySampleTime() {
        val data = listOf(lab("E2", 367.1, "pmol/L", time = 2), lab("E2", 80.0, "pg/mL", time = 1))
        assertArrayEquals(doubleArrayOf(80.0, 100.0), LabTrends.series(data, null).values.single().map { it.value }.toDoubleArray(), 1e-9)
    }

    @Test(expected = IllegalArgumentException::class) fun invalidValuesAreRejected() { LabTrends.validate(lab("E2", Double.NaN, "pg/mL")) }
    @Test(expected = IllegalArgumentException::class) fun conversionOverflowIsRejected() { LabTrends.normalize(lab("T", Double.MAX_VALUE, "nmol/L")) }
}
