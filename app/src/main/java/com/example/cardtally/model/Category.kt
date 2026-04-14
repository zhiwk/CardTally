package com.example.cardtally.model

data class Category(
    var id: Long = 0,
    var name: String = "",
    var type: Int = 0,
    var icon: String? = null,
    var parentId: Long? = null
)
