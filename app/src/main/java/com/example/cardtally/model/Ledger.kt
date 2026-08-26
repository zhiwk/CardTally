package com.example.cardtally.model

data class Ledger(
    val id: Long,
    val name: String,
    val subtitle: String = "",
    val sortOrder: Int = 0,
    val iconName: String = "ms_rounded_book"
)
