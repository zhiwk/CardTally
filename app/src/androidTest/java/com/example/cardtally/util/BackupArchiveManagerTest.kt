package com.example.cardtally.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Record
import com.example.cardtally.testing.IsolatedTestGuard
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

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
