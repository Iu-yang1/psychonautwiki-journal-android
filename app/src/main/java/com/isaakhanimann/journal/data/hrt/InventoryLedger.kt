package com.isaakhanimann.journal.data.hrt

import java.math.BigDecimal

object InventoryLedger {
    fun forEvent(movements: List<StockMovement>, eventId: Int): List<StockMovement> = movements.filter {
        it.reason == "intake:$eventId" && (it.eventKey.startsWith("plan:") || it.eventKey.startsWith("adjust:"))
    }
    fun balance(movements: List<StockMovement>): Double = sum(movements).toDouble()
    fun consumptionDelta(quantity: Double, movements: List<StockMovement>): Double =
        BigDecimal.valueOf(-quantity).subtract(sum(movements)).toDouble()

    private fun sum(movements: List<StockMovement>): BigDecimal = movements.fold(BigDecimal.ZERO) { total, entry ->
        require(entry.amount.isFinite())
        total.add(BigDecimal.valueOf(entry.amount))
    }
}
