package com.example.cardtally.model

/** A saved record template; generated records remain ordinary Record rows. */
data class RecurringRecord(
    var id: Long = 0,
    var ledgerId: Long = 0,
    var type: Int = 0,
    var name: String = "",
    var amountMinor: Long = 0,
    var categoryId: Long? = null,
    var categoryName: String = "",
    var categoryPath: String? = null,
    var assetId: Long? = null,
    var assetSource: String? = null,
    var destinationAssetId: Long? = null,
    var destinationAssetSource: String? = null,
    var note: String? = null,
    var frequency: String = DAILY,
    var weeklyDay: Int? = null,
    var monthlyDay: Int? = null,
    var yearlyMonth: Int? = null,
    var yearlyDay: Int? = null,
    var intervalDays: Int? = null,
    var startDate: String = "",
    var endDate: String? = null,
    var enabled: Boolean = true,
    var nextDueDate: String = ""
) {
    companion object {
        const val DAILY = "daily"
        const val WEEKLY = "weekly"
        const val MONTHLY = "monthly"
        const val YEARLY = "yearly"
        const val INTERVAL = "interval"
    }
}
