package com.example.cardtally.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Record
import com.example.cardtally.testing.IsolatedTestGuard
import com.example.cardtally.util.LedgerSession
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Business tests for ledger asset groups (UX14): independent vs shared group
 * creation, same-group-only atomic merge, and cross-ledger asset history.
 *
 * Runs only on the isolated verification flavor; the database is a scratch one and
 * no user data is read or written.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseHelperAssetGroupTest {

    private lateinit var context: android.content.Context
    private lateinit var databaseHelper: DatabaseHelper

    @Before
    fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DATABASE_NAME)
        databaseHelper = DatabaseHelper(context)
    }

    @After
    fun tearDown() {
        databaseHelper.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    private fun masterId(): Long = databaseHelper.getMasterLedgerId()

    /** Ledger A sharing the master group, plus a ledger C that also shares it. */
    private fun createSharedLedger(name: String, groupRootId: Long): Long {
        val id = databaseHelper.createLedgerInAssetGroup(name, groupRootId)
        assertNotNull("ledger $name must be created", id)
        return id!!
    }

    private fun addAssetForCurrentLedger(name: String, amount: Double): Long {
        val id = databaseHelper.addAsset(
            Asset(name = name, amount = amount, type = 0, categoryLabel = "Cash")
        )
        assertTrue("asset $name must be created", id > 0)
        return id
    }

    private fun switchLedger(id: Long) {
        LedgerSession.setCurrentId(context, id)
    }

    @Test
    fun independentGroup_startsWithNoAssetsAndSharesNothing() {
        switchLedger(masterId())
        addAssetForCurrentLedger("Master cash", 100.0)

        val independentId = databaseHelper.createLedgerInAssetGroup("独立账本", sharedSourceLedgerId = null)!!
        switchLedger(independentId)

        assertEquals("a new independent group must not copy assets", 0, databaseHelper.getAllAssets().size)
        assertEquals(
            "an independent group must not reference the master group",
            independentId,
            databaseHelper.getAssetGroupRootId(independentId)
        )
    }

    @Test
    fun sharedGroup_reusesTheSameAssetIdsAndBalances() {
        switchLedger(masterId())
        val assetId = addAssetForCurrentLedger("Shared cash", 250.0)

        val sharedId = createSharedLedger("共享账本", masterId())
        switchLedger(sharedId)

        val assets = databaseHelper.getAllAssets()
        assertEquals("the shared group must expose exactly the same assets", 1, assets.size)
        assertEquals(assetId, assets.first().id)
        assertEquals(250.0, assets.first().amount, 0.0)
    }

    @Test
    fun assetGroups_areDeduplicatedByStableRootNotByMemberName() {
        createSharedLedger("共享一号", masterId())
        createSharedLedger("共享二号", masterId())

        val groups = databaseHelper.getAssetGroups()
        val masterGroup = groups.first { it.isMaster }

        assertEquals("one group row per pool root", 1, groups.size)
        assertEquals(3, masterGroup.memberLedgerIds.size)
        assertTrue(masterGroup.memberLedgerIds.contains(masterId()))

        val independent = databaseHelper.createLedgerInAssetGroup("独立", null)!!
        assertEquals("an independent group must appear as its own row", 2, databaseHelper.getAssetGroups().size)
        assertFalse(databaseHelper.getAssetGroups().first { it.rootLedgerId == independent }.isMaster)
    }

    @Test
    fun merge_sameGroup_movesRecordsAndKeepsAssetIdsAndBalances() {
        switchLedger(masterId())
        val assetId = addAssetForCurrentLedger("Shared cash", 100.0)
        // An asset that belongs to a *different* group must never move.
        switchLedger(masterId())
        val otherGroupLedger = databaseHelper.createLedgerInAssetGroup("旁支", null)!!
        switchLedger(otherGroupLedger)
        val otherGroupAssetId = addAssetForCurrentLedger("旁支资产", 55.0)

        val sourceId = createSharedLedger("源账本", masterId())
        val targetId = createSharedLedger("目标账本", masterId())

        switchLedger(sourceId)
        val sourceRecordId = databaseHelper.addRecord(
            Record(date = "2026-09-01", amount = 40.0, category = "餐饮", type = 0, assetId = assetId)
        )
        switchLedger(targetId)
        val targetRecordId = databaseHelper.addRecord(
            Record(date = "2026-09-02", amount = 60.0, category = "餐饮", type = 0, assetId = assetId)
        )

        assertEquals(
            DatabaseHelper.LedgerMergeFailure.NONE,
            databaseHelper.validateLedgerMerge(sourceId, targetId)
        )
        // Records already applied their asset effect, so capture the real balance
        // and prove the merge itself does not touch it.
        switchLedger(targetId)
        val balanceBeforeMerge = databaseHelper.getAllAssets().first().amount
        switchLedger(sourceId)

        assertTrue(databaseHelper.mergeLedgerInto(sourceId, targetId))

        assertNull("source ledger must be gone", databaseHelper.getLedgers().firstOrNull { it.id == sourceId })
        assertEquals("target must keep both records", 2, databaseHelper.getLedgerRecordCount(targetId))

        // Locate the merged record by its immutable id and confirm it now belongs to
        // the target, with its amount and category intact.
        val mergedRecord = databaseHelper.getAllRecordsByAssetId(assetId).firstOrNull { it.id == sourceRecordId }
        assertNotNull("the merged record must still exist under its original id", mergedRecord)
        assertEquals("the merged record must now belong to the target", targetId, mergedRecord?.ledgerId)
        assertEquals(40.0, mergedRecord?.amount ?: 0.0, 0.0)
        assertEquals("餐饮", mergedRecord?.category)
        assertEquals(targetRecordId, databaseHelper.getAllRecordsByAssetId(assetId).first { it.amount == 60.0 }.id)

        switchLedger(targetId)
        val assets = databaseHelper.getAllAssets()
        assertEquals("asset count must not change", 1, assets.size)
        assertEquals("asset id must not change", assetId, assets.first().id)
        assertEquals("balance must not change", balanceBeforeMerge, assets.first().amount, 0.0)

        // The unrelated group keeps its own asset and its own root.
        switchLedger(otherGroupLedger)
        assertEquals(1, databaseHelper.getAllAssets().size)
        assertEquals(otherGroupAssetId, databaseHelper.getAllAssets().first().id)
        assertEquals(otherGroupLedger, databaseHelper.getAssetGroupRootId(otherGroupLedger))
    }

    @Test
    fun merge_rejectionsDoNotWriteAnything() {
        val independentId = databaseHelper.createLedgerInAssetGroup("独立", null)!!
        val master = masterId()

        assertEquals(
            DatabaseHelper.LedgerMergeFailure.CROSS_GROUP,
            databaseHelper.validateLedgerMerge(independentId, master)
        )
        assertEquals(
            DatabaseHelper.LedgerMergeFailure.MASTER_AS_SOURCE,
            databaseHelper.validateLedgerMerge(master, independentId)
        )
        assertEquals(
            DatabaseHelper.LedgerMergeFailure.SAME_LEDGER,
            databaseHelper.validateLedgerMerge(master, master)
        )
        assertEquals(
            DatabaseHelper.LedgerMergeFailure.MISSING_LEDGER,
            databaseHelper.validateLedgerMerge(9999L, master)
        )

        assertFalse(databaseHelper.mergeLedgerInto(independentId, master))
        assertFalse(databaseHelper.mergeLedgerInto(master, independentId))
        assertEquals("no ledger may be removed by a rejected merge", 2, databaseHelper.getLedgers().size)
    }

    @Test
    fun merge_nonRootSource_keepsAThirdLedgerInTheSameGroup() {
        switchLedger(masterId())
        val assetId = addAssetForCurrentLedger("Shared cash", 500.0)

        val sourceId = createSharedLedger("源账本", masterId())
        val targetId = createSharedLedger("目标账本", masterId())
        val thirdId = createSharedLedger("第三账本", masterId())

        switchLedger(sourceId)
        databaseHelper.addRecord(
            Record(date = "2026-09-03", amount = 10.0, category = "餐饮", type = 0, assetId = assetId)
        )

        assertTrue(databaseHelper.mergeLedgerInto(sourceId, targetId))

        assertEquals("the third ledger must still see the same pool", masterId(), databaseHelper.getAssetGroupRootId(thirdId))
        assertEquals(
            listOf(thirdId),
            databaseHelper.getAssetGroupMemberIds(thirdId).filter { it != masterId() && it != targetId }
        )
        switchLedger(thirdId)
        assertEquals(1, databaseHelper.getAllAssets().size)
        assertEquals(assetId, databaseHelper.getAllAssets().first().id)
    }

    @Test
    fun merge_rootSourceWithAThirdMember_repointsSurvivingReferences() {
        switchLedger(masterId())
        val assetId = addAssetForCurrentLedger("Shared cash", 700.0)
        // The master ledger is the permanent pool root, so exercise the root-source
        // path with a non-master root group instead.
        val rootId = databaseHelper.createLedgerInAssetGroup("自建根", null)!!
        switchLedger(rootId)
        val ownAssetId = addAssetForCurrentLedger("自建资产", 300.0)

        val memberB = createSharedLedger("成员B", rootId)
        val memberC = createSharedLedger("成员C", rootId)

        assertTrue(databaseHelper.mergeLedgerInto(rootId, memberB))

        assertNull(databaseHelper.getLedgers().firstOrNull { it.id == rootId })
        assertEquals("B becomes the kept root", memberB, databaseHelper.getAssetGroupRootId(memberB))
        assertEquals("C must still resolve into the same group", memberB, databaseHelper.getAssetGroupRootId(memberC))
        switchLedger(memberC)
        val assets = databaseHelper.getAllAssets()
        assertEquals(1, assets.size)
        assertEquals(ownAssetId, assets.first().id)
        assertEquals(300.0, assets.first().amount, 0.0)
        // The master group is untouched by that merge.
        switchLedger(masterId())
        val masterAssets = databaseHelper.getAllAssets()
        assertEquals(1, masterAssets.size)
        assertEquals(assetId, masterAssets.first().id)
        assertEquals(masterId(), databaseHelper.getAssetGroupRootId(masterId()))
    }

    @Test
    fun repeatedMerge_isRejectedWithoutExtraWrites() {
        val sourceId = createSharedLedger("源账本", masterId())
        val targetId = createSharedLedger("目标账本", masterId())
        switchLedger(sourceId)
        databaseHelper.addRecord(Record(date = "2026-09-04", amount = 12.0, category = "餐饮", type = 0))

        assertTrue(databaseHelper.mergeLedgerInto(sourceId, targetId))
        val recordsAfterFirst = databaseHelper.getLedgerRecordCount(targetId)

        assertFalse("a repeated merge must be rejected", databaseHelper.mergeLedgerInto(sourceId, targetId))
        assertEquals(recordsAfterFirst, databaseHelper.getLedgerRecordCount(targetId))
    }

    @Test
    fun allRecordsByAssetId_collectsEveryLedgerOnceAndIgnoresSameNamedAssets() {
        switchLedger(masterId())
        val sharedAssetId = addAssetForCurrentLedger("现金", 0.0)
        val otherGroupLedger = databaseHelper.createLedgerInAssetGroup("另一组", null)!!
        switchLedger(otherGroupLedger)
        val sameNamedAssetId = addAssetForCurrentLedger("现金", 0.0)
        assertFalse(sharedAssetId == sameNamedAssetId)

        val ledgerA = createSharedLedger("A账本", masterId())
        val ledgerB = createSharedLedger("B账本", masterId())

        switchLedger(ledgerA)
        databaseHelper.addRecord(
            Record(date = "2026-09-05", amount = 10.0, category = "餐饮", type = 0, assetId = sharedAssetId)
        )
        switchLedger(ledgerB)
        databaseHelper.addRecord(
            Record(date = "2026-09-06", amount = 20.0, category = "餐饮", type = 1, assetId = sharedAssetId)
        )
        // A transfer between two assets inside the shared group: it must appear once.
        switchLedger(ledgerB)
        databaseHelper.addRecord(
            Record(
                date = "2026-09-07",
                amount = 30.0,
                category = "转账",
                type = 2,
                assetId = sharedAssetId,
                destinationAssetId = sharedAssetId
            )
        )
        switchLedger(otherGroupLedger)
        databaseHelper.addRecord(
            Record(date = "2026-09-08", amount = 99.0, category = "餐饮", type = 0, assetId = sameNamedAssetId)
        )

        switchLedger(masterId())
        val history = databaseHelper.getAllRecordsByAssetId(sharedAssetId)

        assertEquals("same-asset transfers are rejected, so each valid record appears once", 2, history.size)
        assertEquals(
            "records must not be filtered by the current ledger",
            setOf(ledgerA, ledgerB),
            history.mapNotNull { it.ledgerId }.toSet()
        )
        assertFalse(
            "the same-named asset from another group must not leak in",
            history.any { it.amount == 99.0 }
        )

        val names = databaseHelper.getLedgerNamesByIds(history.mapNotNull { it.ledgerId }.toSet())
        assertEquals("A账本", names[ledgerA])
        assertEquals("B账本", names[ledgerB])
    }

    @Test
    fun assetHistory_localFallbackStillScopesToTheCurrentLedger() {
        switchLedger(masterId())
        val assetId = addAssetForCurrentLedger("现金", 0.0)
        val otherLedger = createSharedLedger("另一账本", masterId())

        switchLedger(masterId())
        databaseHelper.addRecord(
            Record(date = "2026-09-09", amount = 5.0, category = "餐饮", type = 0, assetId = assetId)
        )
        switchLedger(otherLedger)
        databaseHelper.addRecord(
            Record(date = "2026-09-10", amount = 7.0, category = "餐饮", type = 0, assetId = assetId)
        )

        switchLedger(masterId())
        assertEquals("the legacy scoped query keeps its ledger filter", 1, databaseHelper.getRecordsByAssetId(assetId).size)
        assertEquals("the cross-ledger query sees both", 2, databaseHelper.getAllRecordsByAssetId(assetId).size)
    }

    @Test
    fun createLedgerInAssetGroup_failsForAnUnknownSourceWithoutLeavingALedger() {
        val before = databaseHelper.getLedgers().size
        val result = databaseHelper.createLedgerInAssetGroup("坏账本", sharedSourceLedgerId = 424242L)

        assertNull(result)
        assertEquals("a failed create must not leave a ledger behind", before, databaseHelper.getLedgers().size)
    }

    companion object {
        private const val DATABASE_NAME = "CardTally.db"
    }
}
