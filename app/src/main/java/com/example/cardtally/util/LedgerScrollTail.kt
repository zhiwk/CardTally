package com.example.cardtally.util

/** Calculate from the original XML padding, never from an already adjusted view. */
internal object LedgerScrollTail {
    fun resolve(baseline: Int, floatingInset: Int, gap: Int, dockedVisible: Boolean, dockedPadding: Int): Int =
        when {
            dockedVisible -> dockedPadding
            floatingInset > 0 -> maxOf(baseline, floatingInset + gap)
            else -> baseline
        }
}
