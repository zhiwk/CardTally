package com.example.cardtally.util

import com.example.cardtally.model.Asset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultAssetPickerOptionsTest {
    @Test fun optionalEntryCanOfferNoAssetEvenWithAnEmptyPool() {
        assertEquals(listOf(DefaultAssetPickerOptions.Row.NoAsset),
            DefaultAssetPickerOptions.groupedRows(emptyList(), "", allowNoAsset = true))
        val rows = DefaultAssetPickerOptions.groupedRows(listOf(Asset(id = 9)), "", allowNoAsset = true)
        assertEquals(DefaultAssetPickerOptions.Row.NoAsset, rows.first())
        assertEquals(listOf(9L), rows.filterIsInstance<DefaultAssetPickerOptions.Row.Account>().map { it.asset.id })
    }

    @Test fun defaultTransferAndRecurringModesDoNotExposeNoAssetUnlessExplicitlyAllowed() {
        assertTrue(DefaultAssetPickerOptions.groupedRows(emptyList(), "").isEmpty())
        val rows = DefaultAssetPickerOptions.groupedRows(listOf(Asset(id = 1)), "")
        assertTrue(rows.none { it is DefaultAssetPickerOptions.Row.NoAsset })
        assertTrue(DefaultAssetPickerOptions.groupedRows(emptyList(), "search", allowNoAsset = true).isEmpty())
    }

    @Test fun groupsFollowAssetPageOrderInsteadOfGlobalPinOrder() {
        val assets = listOf(Asset(id = 6, categoryLabel = "借入", isPinned = true),
            Asset(id = 5, categoryLabel = "借出"), Asset(id = 4, categoryLabel = "基金"),
            Asset(id = 3, categoryLabel = "交通卡"), Asset(id = 2, categoryLabel = "信用卡"),
            Asset(id = 1, categoryLabel = "现金"))
        val rows = DefaultAssetPickerOptions.groupedRows(assets, "")
        assertEquals(DefaultAssetPickerOptions.Group.entries.toList(),
            rows.filterIsInstance<DefaultAssetPickerOptions.Row.Header>().map { it.group })
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L),
            rows.filterIsInstance<DefaultAssetPickerOptions.Row.Account>().map { it.asset.id })
    }

    @Test fun pinsStayWithinTheirGroupWithoutDuplicatingAccounts() {
        val assets = listOf(Asset(id = 1, categoryLabel = "储蓄卡"),
            Asset(id = 2, categoryLabel = "信用卡", isPinned = true),
            Asset(id = 3, categoryLabel = "现金", isPinned = true),
            Asset(id = 4, categoryLabel = "微信钱包"), Asset(id = 5, categoryLabel = "信用卡"))
        val rows = DefaultAssetPickerOptions.groupedRows(assets, "")
        assertEquals(listOf(3L, 1L, 4L, 2L, 5L),
            rows.filterIsInstance<DefaultAssetPickerOptions.Row.Account>().map { it.asset.id })
        assertEquals(2, rows.count { it is DefaultAssetPickerOptions.Row.Header })
    }

    @Test fun filteredResultsKeepOnlyNonemptyGroupsAndNeverStandaloneHeaders() {
        val assets = listOf(Asset(id = 1, name = "日常现金", categoryLabel = "现金"),
            Asset(id = 2, name = "日常基金", categoryLabel = "基金"),
            Asset(id = 3, name = "日常信用卡", categoryLabel = "信用卡", isArchived = true))
        val rows = DefaultAssetPickerOptions.groupedRows(assets, "日常")
        assertEquals(listOf(DefaultAssetPickerOptions.Group.FUNDS, DefaultAssetPickerOptions.Group.INVESTMENT),
            rows.filterIsInstance<DefaultAssetPickerOptions.Row.Header>().map { it.group })
        assertTrue(DefaultAssetPickerOptions.groupedRows(assets, "无匹配").isEmpty())
        assertTrue(DefaultAssetPickerOptions.groupedRows(emptyList(), "").isEmpty())
        rows.forEachIndexed { index, row ->
            if (row is DefaultAssetPickerOptions.Row.Header) {
                assertTrue(rows.getOrNull(index + 1) is DefaultAssetPickerOptions.Row.Account)
            }
        }
    }

    @Test fun assetCategoryFamiliesAndUnknownLabelsMatchAssetPageClassification() {
        val categories = mapOf(
            DefaultAssetPickerOptions.Group.CREDIT to listOf("信用卡", "花呗", "白条", "借呗", "其他信用"),
            DefaultAssetPickerOptions.Group.TOPUP to listOf("交通卡", "饭卡", "话费", "会员卡", "押金", "其他充值卡"),
            DefaultAssetPickerOptions.Group.INVESTMENT to listOf("股票", "基金", "黄金", "其他理财"),
            DefaultAssetPickerOptions.Group.RECEIVABLE to listOf("借出", "其他应收"),
            DefaultAssetPickerOptions.Group.PAYABLE to listOf("借入", "其他应付"),
            DefaultAssetPickerOptions.Group.FUNDS to listOf("", "现金", "储蓄卡", "支付宝", "微信钱包", "自定义"))
        categories.forEach { (group, labels) ->
            labels.forEach { assertEquals(group, DefaultAssetPickerOptions.groupOf(Asset(categoryLabel = it))) }
        }
    }

    @Test fun blankSearchPreservesOrderWithinPinnedAndRegularAccounts() {
        val assets = listOf(Asset(id = 1), Asset(id = 2, isPinned = true),
            Asset(id = 3), Asset(id = 4, isPinned = true))
        assertEquals(listOf(2L, 4L, 1L, 3L), DefaultAssetPickerOptions.filter(assets, " ").map { it.id })
        assertEquals(listOf(1L, 2L, 3L, 4L), assets.map { it.id })
    }

    @Test fun archivedAccountsAreNotSelectableEvenWhenTheirNameMatches() {
        val assets = listOf(Asset(id = 1, name = "日常"), Asset(id = 2, name = "日常", isArchived = true))
        assertEquals(listOf(1L), DefaultAssetPickerOptions.filter(assets, "日常").map { it.id })
    }

    @Test fun searchMatchesTrimmedNameCategoryOrLocalizedBankIgnoringCase() {
        val assets = listOf(Asset(id = 1, name = "Everyday"),
            Asset(id = 2, name = "备用", categoryLabel = "微信钱包"), Asset(id = 3, name = "储蓄卡"))
        assertEquals(listOf(1L), DefaultAssetPickerOptions.filter(assets, " EVERY ").map { it.id })
        assertEquals(listOf(2L), DefaultAssetPickerOptions.filter(assets, "微信").map { it.id })
        assertEquals(listOf(3L), DefaultAssetPickerOptions.filter(assets, "招商") {
            if (it.id == 3L) "招商银行 · 储蓄卡" else ""
        }.map { it.id })
    }

    @Test fun noMatchesAndEmptyPoolProduceEmptyResults() {
        assertTrue(DefaultAssetPickerOptions.filter(emptyList(), "").isEmpty())
        assertTrue(DefaultAssetPickerOptions.filter(listOf(Asset(name = "现金")), "银行").isEmpty())
    }

    @Test fun identicalNamesKeepTheirStableAssetIds() {
        val assets = listOf(Asset(id = 10, name = "日常"), Asset(id = 20, name = "日常"))
        assertEquals(listOf(10L, 20L), DefaultAssetPickerOptions.filter(assets, "日常").map { it.id })
    }
}
