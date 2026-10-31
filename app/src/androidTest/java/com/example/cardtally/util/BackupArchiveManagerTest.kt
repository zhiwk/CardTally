package com.example.cardtally.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Record
import org.json.JSONObject
import com.example.cardtally.testing.IsolatedTestGuard
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

@RunWith(AndroidJUnit4::class)
class BackupArchiveManagerTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val password = "test-backup-password".toCharArray()

    @Before fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        resetInstallation()
    }

    @After fun tearDown() {
        if (IsolatedTestGuard.isIsolatedBuild) resetInstallation()
    }

    @Test fun freshRestoreAndIncrementalMergeKeepOneMasterLedgerAndCorrectBalance() {
        DatabaseHelper(context).use { db ->
            val asset = db.addAsset(Asset(name = "Wallet", amount = 1000.0))
            db.addRecord(Record(date = "2026-09-27", amount = 100.0,
                category = "餐饮", type = 0, assetId = asset, assetSource = "Wallet"))
        }
        val first = backup()

        resetInstallation()
        restore(first)
        assertEquals(1, count("ledgers"))
        assertEquals(1, count("assets"))
        assertEquals(1, count("records"))
        assertEquals(90_000L, balance())

        DatabaseHelper(context).use { db ->
            val asset = db.writableDatabase.rawQuery("SELECT id FROM assets", null).use {
                it.moveToFirst()
                it.getLong(0)
            }
            db.addRecord(Record(date = "2026-09-28", amount = 50.0,
                category = "餐饮", type = 0, assetId = asset, assetSource = "Wallet"))
        }
        val second = backup()

        resetInstallation()
        restore(first)
        restore(second)
        restore(second)
        assertEquals(1, count("ledgers"))
        assertEquals(2, count("records"))
        assertEquals(85_000L, balance())
    }

    @Test fun wrongPasswordCannotImportAnyRows() {
        val archive = backup()
        resetInstallation()
        assertThrows(BackupArchiveManager.BackupIntegrityException::class.java) {
            BackupArchiveManager(context).importFrom(ByteArrayInputStream(archive), "wrong-password".toCharArray())
        }
        assertEquals(1, count("ledgers"))
    }

    @Test fun changedLocalAndBackupBalancesRequireAnExplicitChoice() {
        DatabaseHelper(context).use { it.addAsset(Asset(name = "Wallet", amount = 1000.0)) }
        val first = backup()
        DatabaseHelper(context).use { helper ->
            val asset = helper.readableDatabase.rawQuery("SELECT id FROM assets", null).use {
                it.moveToFirst(); it.getLong(0)
            }
            helper.addRecord(Record(date = "2026-09-28", amount = 50.0,
                category = "餐饮", type = 0, assetId = asset, assetSource = "Wallet"))
        }
        val second = backup()
        resetInstallation()
        restore(first)
        DatabaseHelper(context).use { helper ->
            val asset = helper.readableDatabase.rawQuery("SELECT id FROM assets", null).use {
                it.moveToFirst(); it.getLong(0)
            }
            helper.updateAsset(Asset(id = asset, name = "Wallet", amount = 800.0))
        }
        assertThrows(BackupArchiveManager.AssetBalanceConflictException::class.java) {
            BackupArchiveManager(context).importFrom(ByteArrayInputStream(second), password)
        }
        assertEquals(0, count("records"))
        assertEquals(80_000L, balance())
        val preview = BackupArchiveManager(context).importFrom(
            ByteArrayInputStream(second), password, dryRun = true, useBackupBalances = true)
        assertEquals(1, preview.balanceConflicts)
        BackupArchiveManager(context).importFrom(ByteArrayInputStream(second), password,
            useBackupBalances = true)
        assertEquals(1, count("records"))
        assertEquals(95_000L, balance())
    }

    @Test fun balanceEventsRoundTripIncrementallyWithoutReapplyingAmountsOrDuplicatingRows() {
        DatabaseHelper(context).use { helper ->
            helper.addAsset(Asset(name = "Wallet", amount = 500.0))
            helper.updateAsset(helper.getAllAssets().single().copy(amount = 600.0))
        }
        val first = backup()
        DatabaseHelper(context).use { helper -> helper.updateAsset(helper.getAllAssets().single().copy(amount = 400.0)) }
        val second = backup()
        resetInstallation()
        // Force the restored asset to receive a different local ID.
        DatabaseHelper(context).use { it.addAsset(Asset(name = "Unrelated", amount = 10.0)) }
        val preview = BackupArchiveManager(context).importFrom(ByteArrayInputStream(first), password, dryRun = true)
        assertEquals(2, preview.balanceEvents)
        assertEquals(1, count("asset_balance_events"))
        restore(first)
        restore(second)
        restore(second)
        assertEquals(4, count("asset_balance_events")) // one unrelated event plus three imported ones
        DatabaseHelper(context).use { helper ->
            val ledger = helper.readableDatabase.rawQuery("SELECT ledger_id FROM assets WHERE name = 'Wallet'", null).use {
                check(it.moveToFirst()); it.getLong(0)
            }
            val wallet = DatabaseHelper(context, ledger).use { it.getAllAssets().single { asset -> asset.name == "Wallet" } }
            org.junit.Assert.assertTrue(wallet.id != 1L)
            assertEquals(400.0, wallet.amount, 0.0)
            val history = helper.getAssetHistoryPage(wallet.id, wallet.name).entries
                .filterIsInstance<com.example.cardtally.model.AssetHistoryEntry.Balance>()
            assertEquals(listOf(-20_000L, 10_000L, 50_000L), history.map { it.event.deltaMinor })
            assertEquals(setOf(wallet.id), history.map { it.event.assetId }.toSet())
            assertTrueNoBills(helper)
        }
        // Export/import the merged installation too; forwarded aliases must still deduplicate events.
        restore(backup())
        assertEquals(4, count("asset_balance_events"))
    }

    @Test fun v3WithoutBalanceEventsRestoresWithoutInventingAnInitialization() {
        DatabaseHelper(context).use { it.addAsset(Asset(name = "Legacy wallet", amount = 500.0)) }
        val archive = zipManifest { root ->
            root.put("version", 3)
            root.getJSONObject("tables").remove("asset_balance_events")
            val aliases = root.getJSONArray("aliases")
            for (index in aliases.length() - 1 downTo 0) {
                if (aliases.getJSONObject(index).getString("table") == "asset_balance_events") aliases.remove(index)
            }
        }
        resetInstallation()
        restore(archive)
        assertEquals(50_000L, balance())
        assertEquals(0, count("asset_balance_events"))
    }

    @Test fun v4MissingBalanceEventsTableFailsWithoutImportingAssets() {
        DatabaseHelper(context).use { it.addAsset(Asset(name = "Wallet", amount = 500.0)) }
        val archive = zipManifest { it.getJSONObject("tables").remove("asset_balance_events") }
        resetInstallation()
        assertThrows(IllegalArgumentException::class.java) {
            BackupArchiveManager(context).importFrom(ByteArrayInputStream(archive), password)
        }
        assertEquals(0, count("assets"))
        assertEquals(0, count("asset_balance_events"))
    }

    private fun assertTrueNoBills(helper: DatabaseHelper) {
        assertEquals(0, helper.getAllRecords().size)
        assertEquals(0L, helper.getTotalByTypeMinor(0))
        assertEquals(0L, helper.getTotalByTypeMinor(1))
    }

    private fun zipManifest(change: (JSONObject) -> Unit): ByteArray {
        val zip = ByteArrayOutputStream().use { output ->
            BackupArchiveManager(context).exportZipTo(output)
            output.toByteArray()
        }
        val entries = ZipInputStream(ByteArrayInputStream(zip)).use { input ->
            buildList {
                while (true) {
                    val entry = input.nextEntry ?: break
                    add(entry.name to input.readBytes())
                    input.closeEntry()
                }
            }
        }
        check(entries.first().first == "manifest.json")
        val root = JSONObject(entries.first().second.toString(Charsets.UTF_8))
        change(root)
        return ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { result ->
                entries.forEach { (name, bytes) ->
                    result.putNextEntry(ZipEntry(name))
                    result.write(if (name == "manifest.json") root.toString().toByteArray(Charsets.UTF_8) else bytes)
                    result.closeEntry()
                }
            }
            output.toByteArray()
        }
    }

    private fun backup(): ByteArray = ByteArrayOutputStream().use { output ->
        BackupArchiveManager(context).exportTo(output, password)
        output.toByteArray()
    }

    private fun restore(archive: ByteArray) {
        val preview = BackupArchiveManager(context).importFrom(
            ByteArrayInputStream(archive), password, dryRun = true)
        assertEquals(0, preview.different)
        BackupArchiveManager(context).importFrom(ByteArrayInputStream(archive), password)
    }

    private fun count(table: String): Int = DatabaseHelper(context).use { helper ->
        helper.readableDatabase.rawQuery("SELECT COUNT(*) FROM $table", null).use {
            it.moveToFirst()
            it.getInt(0)
        }
    }

    private fun balance(): Long = DatabaseHelper(context).use { helper ->
        helper.readableDatabase.rawQuery("SELECT amount FROM assets", null).use {
            it.moveToFirst()
            it.getLong(0)
        }
    }

    private fun resetInstallation() {
        context.deleteDatabase("CardTally.db")
        context.getSharedPreferences("backup_internal", 0).edit().clear().commit()
    }
}
