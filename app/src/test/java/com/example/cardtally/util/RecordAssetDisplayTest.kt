package com.example.cardtally.util

import com.example.cardtally.model.Asset
import com.example.cardtally.model.Record
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordAssetDisplayTest {
    @Test fun currentNameWinsWithoutChangingHistoricalRecord() {
        val names = RecordAssetDisplay.names(listOf(Asset(id = 1, name = "生活账户")))
        for (type in listOf(0, 1)) {
            val record = Record(type = type, assetId = 1, assetSource = "旧账户", amount = 12.34)
            val before = record.copy()
            assertEquals("生活账户", RecordAssetDisplay.name(record.assetId, record.assetSource, names))
            assertEquals(before, record)
        }
    }

    @Test fun transferResolvesBothIdsWithoutChangingSnapshotsOrAmounts() {
        val names = RecordAssetDisplay.names(listOf(
            Asset(id = 1, name = "转出新名称"), Asset(id = 2, name = "转入新名称")
        ))
        val record = Record(type = 2, assetId = 1, destinationAssetId = 2,
            assetSource = "转出旧名称", destinationAssetSource = "转入旧名称", amount = 20.0, fee = 0.5)
        val before = record.copy()
        assertEquals("转出新名称 → 转入新名称", RecordAssetDisplay.transferRoute(record, names))
        assertEquals(before, record)
    }

    @Test fun missingOrUnboundAssetFallsBackToSnapshot() {
        val names = mapOf(1L to "另一个账户")
        assertEquals("历史账户", RecordAssetDisplay.name(99, "历史账户", names))
        assertEquals("历史账户", RecordAssetDisplay.name(null, "历史账户", names))
        assertEquals("历史账户", RecordAssetDisplay.name(2, "历史账户", mapOf(2L to " ")))
        val record = Record(type = 2, assetId = 1, destinationAssetId = 99,
            assetSource = "旧名称", destinationAssetSource = "历史账户")
        assertEquals("另一个账户 → 历史账户", RecordAssetDisplay.transferRoute(record, names))
    }

    @Test fun noAssetOrBlankSnapshotDoesNotInventAnAssociation() {
        val names = mapOf(1L to "生活账户")
        assertNull(RecordAssetDisplay.name(null, null, names))
        assertNull(RecordAssetDisplay.name(null, " ", names))
        assertNull(RecordAssetDisplay.name(99, "", names))
        assertEquals("", RecordAssetDisplay.transferRoute(Record(type = 2), names))
        assertEquals("生活账户", RecordAssetDisplay.transferRoute(Record(type = 2, assetId = 1), names))
    }

    @Test fun identicalHistoricalNamesStillResolveById() {
        val names = RecordAssetDisplay.names(listOf(
            Asset(id = 1, name = "日用账户"), Asset(id = 2, name = "备用账户")
        ))
        assertEquals("日用账户", RecordAssetDisplay.name(1, "同名账户", names))
        assertEquals("备用账户", RecordAssetDisplay.name(2, "同名账户", names))
        assertEquals("同名账户", RecordAssetDisplay.name(null, "同名账户", names))
    }

    @Test fun archivedAssetStillUsesCurrentName() {
        val names = RecordAssetDisplay.names(listOf(Asset(id = 1, name = "已归档账户", isArchived = true)))
        assertEquals("已归档账户", RecordAssetDisplay.name(1, "旧名称", names))
    }

    @Test fun refreshedNamesReflectRenameForTheSameRecord() {
        val record = Record(assetId = 1, assetSource = "原始名称")
        val oldNames = RecordAssetDisplay.names(listOf(Asset(id = 1, name = "修改前")))
        val newNames = RecordAssetDisplay.names(listOf(Asset(id = 1, name = "修改后")))
        assertEquals("修改前", RecordAssetDisplay.name(record.assetId, record.assetSource, oldNames))
        assertEquals("修改后", RecordAssetDisplay.name(record.assetId, record.assetSource, newNames))
        assertEquals("原始名称", record.assetSource)
    }
}
