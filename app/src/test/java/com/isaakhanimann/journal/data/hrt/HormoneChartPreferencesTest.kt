package com.isaakhanimann.journal.data.hrt

import android.app.Application
import com.isaakhanimann.journal.ui.tabs.hrt.HormoneChartOptions
import com.isaakhanimann.journal.ui.tabs.hrt.HormoneChartPreferences
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class HormoneChartPreferencesTest {
    @Test fun completeOptionsSurviveScreenRecreationAndRetainUnitsPerAnalyte() {
        val storage = RuntimeEnvironment.getApplication().getSharedPreferences("chart-test", 0)
        storage.edit().clear().commit()
        val e2 = HormoneChartOptions(7, 0, "E2", "pmol/L", "estrogel", false)
        HormoneChartPreferences(storage).save(e2)
        assertEquals(e2, HormoneChartPreferences(storage).load())
        val e4 = e2.copy(analyteId = "E4", unit = "ng/mL")
        HormoneChartPreferences(storage).save(e4)
        assertEquals(e4, HormoneChartPreferences(storage).load())
        assertEquals("pmol/L", HormoneChartPreferences(storage).unitFor("E2"))
        assertEquals("ng/mL", HormoneChartPreferences(storage).unitFor("E4"))
    }
}
