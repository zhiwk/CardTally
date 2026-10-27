package com.example.cardtally.util

/** Tail only: floating pages still extend and draw behind the navigation dock. */
internal object NavigationScrollTail {
    fun resolve(floatingInset: Int, shadowRoom: Int, gap: Int): Int =
        (floatingInset - shadowRoom).coerceAtLeast(0) + gap
}
