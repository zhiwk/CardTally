package com.example.cardtally.util

import java.util.Locale

/** A bounded, stable month index; no page objects are allocated for absent months. */
object LedgerMonthIndex {
    const val COUNT = 9999 * 12
    fun position(month: String): Int = ((month.substring(0, 4).toInt() - 1) * 12 +
        month.substring(5, 7).toInt() - 1).coerceIn(0, COUNT - 1)
    fun month(position: Int): String {
        require(position in 0 until COUNT)
        return String.format(Locale.US, "%04d-%02d", position / 12 + 1, position % 12 + 1)
    }
}
