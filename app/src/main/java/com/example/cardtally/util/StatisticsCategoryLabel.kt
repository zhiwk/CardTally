package com.example.cardtally.util

/** Removes presentation-only prefixes while keeping the stored category name intact. */
fun normalizeStatisticsCategoryLabel(label: String): String {
    return label.substringAfter("· ").substringAfter(": ")
}
