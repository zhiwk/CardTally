package com.example.cardtally.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Record
import com.example.cardtally.testing.IsolatedTestGuard
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Only isolated SQLite data; never starts an AI/network request. */
@RunWith(AndroidJUnit4::class)
class AiRecordEngineTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: DatabaseHelper
    private lateinit var engine: AiRecordEngine
    private var clock = 1000L
    private var assetId = 0L
    private var categoryId = 0L

    @Before fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        context.deleteDatabase("CardTally.db")
        db = DatabaseHelper(context)
        assetId = db.addAsset(Asset(name = "Test wallet", amount = 100.0))
        categoryId = db.getLeafCategoriesByType(0).first().id
        engine = AiRecordEngine(context, db) { clock }
    }
    @After fun tearDown() {
        if (IsolatedTestGuard.isIsolatedBuild) {
            if (::db.isInitialized) db.close()
            context.deleteDatabase("CardTally.db")
        }
    }
    private fun plan(name: String, args: JSONObject): AiRecordEngine.Plan =
        engine.prepare(RecordToolProtocol.Call("call", name, args.toString()))
    private fun register(plan: AiRecordEngine.Plan): String = UUID.randomUUID().toString().also {
        AiRecordAuditStore(db.writableDatabase).propose(it, 1, db.getCurrentLedger()!!.id,
            "https://service.example", "test-model", plan.call.name, plan.targetId, plan.fingerprint)
    }
    private fun execute(name: String, args: JSONObject): JSONObject {
        val proposal = plan(name, args)
        return engine.execute(proposal, register(proposal))
    }
    private fun creation(amount: String = "12.50") = JSONObject().put("date", "2026-10-04")
        .put("type", 0).put("amount", amount).put("asset_id", assetId).put("category_id", categoryId)
    private fun balance(id: Long = assetId) = (db.getAllAssets() + db.getArchivedAssets()).first { it.id == id }.amount

    @Test fun confirmedCrud_usesExistingBalanceRules() {
        val proposal = plan("create_record", creation())
        assertTrue(db.getRecordsPage().records.isEmpty()) // Preview has no write effect.
        assertEquals(100.0, balance(), 0.0)
        val id = engine.execute(proposal, register(proposal)).getLong("record_id")
        assertEquals(87.5, balance(), 0.0)
        execute("update_record", JSONObject().put("record_id", id).put("amount", "20.00"))
        assertEquals(80.0, balance(), 0.0)
        execute("delete_record", JSONObject().put("record_id", id))
        assertNull(db.getRecordById(id))
        assertEquals(100.0, balance(), 0.0)

        val unlinkedArgs = creation().apply { remove("asset_id") }
        val unlinked = execute("create_record", unlinkedArgs).getLong("record_id")
        assertNull(db.getRecordById(unlinked)!!.assetId)
        assertNull(db.getRecordById(unlinked)!!.assetSource)
        assertEquals(100.0, balance(), 0.0)
        execute("update_record", JSONObject().put("record_id", unlinked).put("asset_id", assetId))
        assertEquals(87.5, balance(), 0.0)
        execute("update_record", JSONObject().put("record_id", unlinked).put("asset_id", JSONObject.NULL))
        assertNull(db.getRecordById(unlinked)!!.assetId)
        assertNull(db.getRecordById(unlinked)!!.assetSource)
        assertEquals(100.0, balance(), 0.0)
        execute("update_record", JSONObject().put("record_id", unlinked).put("amount", "20.00"))
        assertNull(db.getRecordById(unlinked)!!.assetId) // Omitted asset retains no selection.
        execute("delete_record", JSONObject().put("record_id", unlinked))
        assertEquals(100.0, balance(), 0.0)

        val incomeArgs = creation().put("type", 1).put("asset_id", JSONObject.NULL)
            .put("category_id", db.getLeafCategoriesByType(1).first().id)
        val income = execute("create_record", incomeArgs).getLong("record_id")
        assertNull(db.getRecordById(income)!!.assetId)
        assertEquals(100.0, balance(), 0.0)
        execute("update_record", JSONObject().put("record_id", income).put("asset_id", assetId))
        assertEquals(112.5, balance(), 0.0)
        execute("update_record", JSONObject().put("record_id", income).put("asset_id", JSONObject.NULL))
        assertEquals(100.0, balance(), 0.0)
    }

    @Test fun transferCrud_preservesFeeSymmetry() {
        val destination = db.addAsset(Asset(name = "Destination", amount = 0.0))
        val args = JSONObject().put("date", "2026-10-04").put("type", 2).put("amount", "10.00")
            .put("asset_id", assetId).put("destination_asset_id", destination).put("fee", "1.00")
        assertThrows(IllegalArgumentException::class.java) {
            plan("create_record", JSONObject(args.toString()).put("asset_id", JSONObject.NULL))
        }
        val id = execute("create_record", args).getLong("record_id")
        assertEquals(89.0, balance(), 0.0)
        assertEquals(10.0, balance(destination), 0.0)
        execute("update_record", JSONObject().put("record_id", id).put("amount", "15.00").put("fee", "2.00"))
        assertEquals(83.0, balance(), 0.0)
        assertEquals(15.0, balance(destination), 0.0)
        execute("delete_record", JSONObject().put("record_id", id))
        assertEquals(100.0, balance(), 0.0)
        assertEquals(0.0, balance(destination), 0.0)
    }

    @Test fun changedOrExpiredPreview_cannotApplyABalanceEffect() {
        val expired = plan("create_record", creation())
        val expiredOp = register(expired)
        clock += 300_001L
        assertThrows(IllegalArgumentException::class.java) { engine.execute(expired, expiredOp) }
        val stale = plan("create_record", creation())
        val staleOp = register(stale)
        db.updateAsset(db.getAllAssets().first { it.id == assetId }.copy(amount = 90.0))
        assertThrows(IllegalArgumentException::class.java) { engine.execute(stale, staleOp) }
        assertTrue(db.getRecordsPage().records.isEmpty())
        assertEquals(90.0, balance(), 0.0)
    }

    @Test fun repeatedConfirmation_cannotCreateADuplicate() {
        val proposal = plan("create_record", creation())
        val op = register(proposal)
        engine.execute(proposal, op)
        assertThrows(IllegalStateException::class.java) { engine.execute(proposal, op) }
        assertEquals(1, db.getRecordsPage().records.size)
        assertEquals(87.5, balance(), 0.0)
        assertEquals("success", AiRecordAuditStore(db.readableDatabase).state(op))
    }

    @Test fun queries_areLedgerScopedAndUseContinuousPagination() {
        val category = db.getCategoryById(categoryId)!!
        fun record() = Record(date = "2026-10-04", amount = 1.0, type = 0,
            category = category.name, categoryId = categoryId, assetId = assetId)
        repeat(21) { assertTrue(db.addRecord(record()) > 0) }
        val other = requireNotNull(db.createLedgerInAssetGroup("Other", db.getCurrentLedger()!!.id))
        val otherRecord = DatabaseHelper(context, other).use { it.addRecord(record()) }
        val first = execute("query_records", JSONObject())
        assertEquals(20, first.getJSONArray("records").length())
        val second = execute("query_records", JSONObject().put("after", first.getJSONObject("next_cursor")))
        assertEquals(1, second.getJSONArray("records").length())
        assertTrue(second.isNull("next_cursor"))
        db.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM ai_record_audit WHERE tool = 'query_records' AND state = 'success' AND confirmed_at IS NULL", null
        ).use {
            assertTrue(it.moveToFirst())
            assertEquals(2, it.getInt(0)) // Automatic reads must not claim a native user confirmation.
        }
        val crossLedger = plan("get_record", JSONObject().put("record_id", otherRecord))
        assertThrows(IllegalArgumentException::class.java) { engine.execute(crossLedger, register(crossLedger)) }
        assertThrows(IllegalArgumentException::class.java) { plan("delete_record", JSONObject().put("record_id", otherRecord)) }
    }
}
