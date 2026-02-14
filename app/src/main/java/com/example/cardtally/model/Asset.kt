package com.example.cardtally.model

data class Asset(
    var id: Long = 0,
    var name: String = "",
    var amount: Double = 0.0,
    var type: Int = 0 // 0: 现金, 1: 银行卡, 2: 支付宝, 3: 微信, 4: 其他
)
