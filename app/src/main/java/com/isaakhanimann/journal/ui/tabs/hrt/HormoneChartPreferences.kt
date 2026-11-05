package com.isaakhanimann.journal.ui.tabs.hrt

import android.content.SharedPreferences

data class HormoneChartOptions(
    val historyDays: Int = 30,
    val forecastDays: Int = 3,
    val analyteId: String = "E2",
    val unit: String = "pg/mL",
    val gelReference: String = "compare",
    val bridgeValerate: Boolean = true,
)

/** Keep chart display choices when the composable leaves navigation or the process restarts. */
class HormoneChartPreferences(private val preferences: SharedPreferences) {
    private val analytes = setOf("E2", "EE", "E4", "E1", "EQUILIN")
    fun unitFor(analyte: String): String {
        val saved = preferences.getString("unit:$analyte", "pg/mL")
        return saved?.takeIf { it in if (analyte == "E2") setOf("pg/mL", "pmol/L") else setOf("pg/mL", "ng/mL") } ?: "pg/mL"
    }
    fun load(): HormoneChartOptions {
        val analyte = preferences.getString("analyteId", "E2")?.takeIf { it in analytes } ?: "E2"
        return HormoneChartOptions(
            historyDays = preferences.getInt("historyDays", 30).takeIf { it in setOf(1, 7, 30, 90, 180, 365) } ?: 30,
            forecastDays = preferences.getInt("forecastDays", 3).takeIf { it in setOf(0, 3, 7, 30) } ?: 3,
            analyteId = analyte, unit = unitFor(analyte),
            gelReference = preferences.getString("gelReference", "compare")?.takeIf { it in setOf("compare", "estrogel", "divigel", "confirmed") } ?: "compare",
            bridgeValerate = preferences.getBoolean("bridgeValerate", true),
        )
    }
    fun save(options: HormoneChartOptions) {
        preferences.edit().putInt("historyDays", options.historyDays).putInt("forecastDays", options.forecastDays)
            .putString("analyteId", options.analyteId).putString("unit:${options.analyteId}", options.unit)
            .putString("gelReference", options.gelReference).putBoolean("bridgeValerate", options.bridgeValerate).apply()
    }
}
