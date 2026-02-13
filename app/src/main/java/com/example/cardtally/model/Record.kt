package com.example.cardtally.model

data class Record(
    var id: Long = 0,
    var date: String = "",
    var amount: Double = 0.0,
    var category: String = "",
    var type: Int = 0,
    var description: String? = null
)
