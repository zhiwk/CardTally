package com.example.cardtally.model

data class Asset(
    var id: Long = 0,
    var name: String = "",
    var amount: Double = 0.0,
    var type: Int = 0,
    var isArchived: Boolean = false
)
