package com.example.cardtally.util

import com.example.cardtally.model.Asset

/** Presentation-only filtering; the caller supplies the current ledger's available asset pool. */
internal object DefaultAssetPickerOptions {
    enum class Group { FUNDS, CREDIT, TOPUP, INVESTMENT, RECEIVABLE, PAYABLE }

    sealed class Row {
        data class Header(val group: Group) : Row()
        data class Account(val asset: Asset) : Row()
        data object NoAsset : Row()
    }

    // Same category-label families as AssetFragment; legacy/unknown labels remain funds.
    fun groupOf(asset: Asset): Group = when (asset.categoryLabel) {
        "信用卡", "花呗", "白条", "借呗", "其他信用" -> Group.CREDIT
        "交通卡", "饭卡", "话费", "会员卡", "押金", "其他充值卡" -> Group.TOPUP
        "股票", "基金", "黄金", "其他理财" -> Group.INVESTMENT
        "借出", "其他应收" -> Group.RECEIVABLE
        "借入", "其他应付" -> Group.PAYABLE
        else -> Group.FUNDS
    }

    fun groupedRows(assets: List<Asset>, query: String, allowNoAsset: Boolean = false,
        bankLabel: (Asset) -> String = { "" }): List<Row> {
        val groups = filter(assets, query, bankLabel).groupBy(::groupOf)
        return buildList {
            if (allowNoAsset && query.isBlank()) add(Row.NoAsset)
            for (group in Group.entries) {
                val accounts = groups[group].orEmpty()
                if (accounts.isEmpty()) continue
                add(Row.Header(group))
                accounts.forEach { add(Row.Account(it)) }
            }
        }
    }

    fun filter(assets: List<Asset>, query: String, bankLabel: (Asset) -> String = { "" }): List<Asset> {
        val search = query.trim()
        return assets.filter { asset ->
            !asset.isArchived && (search.isEmpty() ||
                asset.name.contains(search, ignoreCase = true) ||
                asset.categoryLabel.contains(search, ignoreCase = true) ||
                bankLabel(asset).contains(search, ignoreCase = true))
        }.sortedByDescending { it.isPinned }
    }
}
