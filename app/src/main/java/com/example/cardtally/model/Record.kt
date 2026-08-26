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
    var assetId: Long? = null,
    var destinationAssetId: Long? = null,
    var assetSource: String? = null,
    var destinationAssetSource: String? = null,
    var photoUri: String? = null,
    var photoUris: List<String> = emptyList(),
    var sortOrder: Int = 0
)
