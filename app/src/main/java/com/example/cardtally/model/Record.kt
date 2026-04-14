package com.example.cardtally.model

data class Record(
    var id: Long = 0,
    var date: String = "",
    var amount: Double = 0.0,
    var category: String = "",
    var categoryId: Long? = null,
    var categoryNameSnapshot: String? = null,
    var categoryPathSnapshot: String? = null,
    var type: Int = 0,
    var description: String? = null,
    var assetSource: String? = null,
    var sortOrder: Int = 0
)
