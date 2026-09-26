package com.example.cardtally.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.model.Ledger

/** Ledger and asset-pool mutation boundary. All multi-row pool changes remain transactional. */
internal class LedgerWriteRepository(
    private val writableDatabase: () -> SQLiteDatabase,
    private val currentLedgerId: () -> Long,
    private val getLedgers: () -> List<Ledger>,
    private val getMasterLedgerId: () -> Long,
    private val getSharedSourceLedgerId: (Long) -> Long?,
    private val columns: Columns
) {
    data class Columns(
        val ledgersTable: String,
        val ledgerId: String,
        val ledgerName: String,
        val ledgerSubtitle: String,
        val ledgerSortOrder: String,
        val ledgerIcon: String,
        val sharedLedgersTable: String,
        val sharedLedgerId: String,
        val sharedSourceLedgerId: String,
        val sharedAssetsTable: String,
        val sharedAssetId: String,
        val assetsTable: String,
        val assetId: String,
        val assetLedgerId: String,
        val assetName: String,
        val assetAmount: String,
        val assetType: String,
        val assetCategoryLabel: String,
        val assetCategoryIcon: String,
        val assetArchived: String,
        val assetPinned: String,
        val assetIncludeInTotal: String,
        val recordsTable: String,
        val recordLedgerId: String
    )

    fun addLedger(name: String, subtitle: String = "", iconName: String = "tabler_book"): Long =
        writableDatabase().insertOrThrow(columns.ledgersTable, null, ContentValues().apply {
            put(columns.ledgerName, name)
            put(columns.ledgerSubtitle, subtitle.ifBlank { "${name}账本" })
            put(columns.ledgerSortOrder, getLedgers().size)
            put(columns.ledgerIcon, iconName)
        })

    fun addLedgerWithAssetPool(
        name: String,
        sharedSourceLedgerId: Long?,
        copyFromLedgerId: Long?,
        iconName: String
    ): Long {
        val id = addLedger(name, iconName = iconName)
        val db = writableDatabase()
        sharedSourceLedgerId?.let { source ->
            db.insertOrThrow(columns.sharedLedgersTable, null, ContentValues().apply {
                put(columns.sharedLedgerId, id)
                put(columns.sharedSourceLedgerId, source)
            })
        }
        copyFromLedgerId?.let { source ->
            db.query(
                columns.assetsTable,
                arrayOf(columns.assetId),
                "${columns.assetLedgerId} = ?",
                arrayOf(source.toString()),
                null,
                null,
                null
            ).use { cursor -> while (cursor.moveToNext()) copyAsset(db, cursor.getLong(0), id) }
        }
        return id
    }

    fun forkAssetPool(ledgerId: Long): Boolean {
        val root = poolRoot(ledgerId)
        if (root == ledgerId) return false
        val members = getLedgers().map { it.id }.filter { poolRoot(it) == root }
        val db = writableDatabase()
        db.beginTransaction()
        return try {
            val poolAssetIds = members.flatMapTo(mutableSetOf()) { memberId ->
                db.query(
                    columns.assetsTable,
                    arrayOf(columns.assetId),
                    "${columns.assetLedgerId} = ?",
                    arrayOf(memberId.toString()),
                    null,
                    null,
                    null
                ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) } }
            }
            poolAssetIds.forEach { assetId ->
                if (!assetExists(db, ledgerId, assetId)) copyAsset(db, assetId, ledgerId)
            }
            db.delete(columns.sharedLedgersTable, "${columns.sharedLedgerId} = ?", arrayOf(ledgerId.toString()))
            val remaining = members.filter { it != ledgerId }
            if (remaining.size == 1) {
                val onlyLedger = remaining.single()
                poolAssetIds.forEach { assetId ->
                    if (!assetExists(db, onlyLedger, assetId)) copyAsset(db, assetId, onlyLedger)
                }
                db.delete(columns.sharedLedgersTable, "${columns.sharedLedgerId} = ?", arrayOf(onlyLedger.toString()))
            }
            db.setTransactionSuccessful()
            true
        } finally {
            db.endTransaction()
        }
    }

    fun updateAssetPool(ledgerId: Long, sharedSourceLedgerId: Long?, iconName: String?, name: String?): Boolean {
        if (ledgerId != currentLedgerId()) return false
        val db = writableDatabase()
        if (iconName != null || name != null) {
            db.update(columns.ledgersTable, ledgerValues(iconName, name), "${columns.ledgerId} = ?", arrayOf(ledgerId.toString()))
        }
        db.delete(columns.sharedLedgersTable, "${columns.sharedLedgerId} = ?", arrayOf(ledgerId.toString()))
        if (sharedSourceLedgerId != null && sharedSourceLedgerId != ledgerId) {
            db.insertOrThrow(columns.sharedLedgersTable, null, ContentValues().apply {
                put(columns.sharedLedgerId, ledgerId)
                put(columns.sharedSourceLedgerId, sharedSourceLedgerId)
            })
        }
        return true
    }

    fun updateLedgerDetails(ledgerId: Long, iconName: String?, name: String?): Boolean {
        if (ledgerId != currentLedgerId()) return false
        if (iconName == null && name == null) return true
        return writableDatabase().update(
            columns.ledgersTable,
            ledgerValues(iconName, name),
            "${columns.ledgerId} = ?",
            arrayOf(ledgerId.toString())
        ) > 0
    }

    fun addLedgerWithAssetLinks(name: String, subtitle: String, sharedAssetIds: List<Long>, copiedAssetIds: List<Long>): Long {
        val id = addLedger(name, subtitle)
        val db = writableDatabase()
        sharedAssetIds.distinct().forEach { assetId ->
            db.insertWithOnConflict(columns.sharedAssetsTable, null, ContentValues().apply {
                put(columns.sharedLedgerId, id)
                put(columns.sharedAssetId, assetId)
            }, SQLiteDatabase.CONFLICT_IGNORE)
        }
        copiedAssetIds.distinct().forEach { copyAsset(db, it, id) }
        return id
    }

    fun copyAssetToLedger(assetId: Long, targetLedgerId: Long): Long =
        copyAsset(writableDatabase(), assetId, targetLedgerId)

    fun updateLegacyLedger(ledgerId: Long, name: String, sharedAssetIds: List<Long>): Boolean {
        if (ledgerId != currentLedgerId()) return false
        val db = writableDatabase()
        db.beginTransaction()
        return try {
            val updated = db.update(
                columns.ledgersTable,
                ContentValues().apply {
                    put(columns.ledgerName, name)
                    put(columns.ledgerSubtitle, "${name}账本")
                },
                "${columns.ledgerId} = ?",
                arrayOf(ledgerId.toString())
            )
            db.delete(columns.sharedAssetsTable, "${columns.sharedLedgerId} = ?", arrayOf(ledgerId.toString()))
            sharedAssetIds.distinct().forEach { assetId ->
                db.insertWithOnConflict(columns.sharedAssetsTable, null, ContentValues().apply {
                    put(columns.sharedLedgerId, ledgerId)
                    put(columns.sharedAssetId, assetId)
                }, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
            updated > 0
        } finally {
            db.endTransaction()
        }
    }

    fun deleteLedgers(ledgerIds: Set<Long>): Boolean {
        val allLedgers = getLedgers()
        if (
            ledgerIds.isEmpty() || getMasterLedgerId() in ledgerIds || ledgerIds.size >= allLedgers.size ||
            !ledgerIds.all { id -> allLedgers.any { it.id == id } }
        ) return false
        val roots = allLedgers.associate { it.id to poolRoot(it.id) }
        val db = writableDatabase()
        db.beginTransaction()
        return try {
            ledgerIds.forEach { db.delete(columns.recordsTable, "${columns.recordLedgerId} = ?", arrayOf(it.toString())) }
            roots.values.distinct()
                .filter { root -> roots.any { it.value == root && it.key in ledgerIds } }
                .forEach { root ->
                    val members = roots.filterValues { it == root }.keys
                    val survivors = members - ledgerIds
                    val keeper = survivors.minOrNull()
                    if (keeper == null) {
                        db.delete(columns.assetsTable, "${columns.assetLedgerId} = ?", arrayOf(root.toString()))
                    } else {
                        members.filter { it in ledgerIds }.forEach { deletedId ->
                            db.update(
                                columns.assetsTable,
                                ContentValues().apply { put(columns.assetLedgerId, keeper) },
                                "${columns.assetLedgerId} = ?",
                                arrayOf(deletedId.toString())
                            )
                        }
                        deleteSharedLedgers(db, survivors)
                        survivors.filter { it != keeper }.forEach { survivor ->
                            db.insertWithOnConflict(columns.sharedLedgersTable, null, ContentValues().apply {
                                put(columns.sharedLedgerId, survivor)
                                put(columns.sharedSourceLedgerId, keeper)
                            }, SQLiteDatabase.CONFLICT_IGNORE)
                        }
                    }
                }
            deleteSharedLedgers(db, ledgerIds)
            db.delete(
                columns.sharedLedgersTable,
                "${columns.sharedSourceLedgerId} IN (${ledgerIds.joinToString { "?" }})",
                ledgerIds.map(Long::toString).toTypedArray()
            )
            ledgerIds.forEach { db.delete(columns.assetsTable, "${columns.assetLedgerId} = ?", arrayOf(it.toString())) }
            db.delete(
                columns.ledgersTable,
                "${columns.ledgerId} IN (${ledgerIds.joinToString { "?" }})",
                ledgerIds.map(Long::toString).toTypedArray()
            )
            db.setTransactionSuccessful()
            true
        } finally {
            db.endTransaction()
        }
    }

    fun createLedgerInAssetGroup(name: String, sharedSourceLedgerId: Long?, iconName: String): Long? {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return null
        val resolvedRoot = sharedSourceLedgerId?.let { sourceId ->
            if (getLedgers().none { it.id == sourceId }) return null
            poolRoot(sourceId)
        }
        val db = writableDatabase()
        db.beginTransaction()
        return try {
            val id = db.insertOrThrow(columns.ledgersTable, null, ContentValues().apply {
                put(columns.ledgerName, trimmedName)
                put(columns.ledgerSubtitle, "${trimmedName}账本")
                put(columns.ledgerSortOrder, getLedgers().size)
                put(columns.ledgerIcon, iconName)
            })
            if (resolvedRoot != null && resolvedRoot != id) {
                db.insertOrThrow(columns.sharedLedgersTable, null, ContentValues().apply {
                    put(columns.sharedLedgerId, id)
                    put(columns.sharedSourceLedgerId, resolvedRoot)
                })
            }
            db.setTransactionSuccessful()
            id
        } catch (_: Exception) {
            null
        } finally {
            db.endTransaction()
        }
    }

    fun validateMerge(sourceLedgerId: Long, targetLedgerId: Long): DatabaseHelper.LedgerMergeFailure {
        if (sourceLedgerId == targetLedgerId) return DatabaseHelper.LedgerMergeFailure.SAME_LEDGER
        val ids = getLedgers().map { it.id }.toSet()
        if (sourceLedgerId !in ids || targetLedgerId !in ids) return DatabaseHelper.LedgerMergeFailure.MISSING_LEDGER
        if (sourceLedgerId == getMasterLedgerId()) return DatabaseHelper.LedgerMergeFailure.MASTER_AS_SOURCE
        if (poolRoot(sourceLedgerId) != poolRoot(targetLedgerId)) return DatabaseHelper.LedgerMergeFailure.CROSS_GROUP
        return DatabaseHelper.LedgerMergeFailure.NONE
    }

    fun mergeLedgerInto(sourceLedgerId: Long, targetLedgerId: Long): Boolean {
        if (validateMerge(sourceLedgerId, targetLedgerId) != DatabaseHelper.LedgerMergeFailure.NONE) return false
        val root = poolRoot(sourceLedgerId)
        val members = getLedgers().map { it.id }.filter { poolRoot(it) == root }
        val db = writableDatabase()
        db.beginTransaction()
        return try {
            if (validateMerge(sourceLedgerId, targetLedgerId) != DatabaseHelper.LedgerMergeFailure.NONE) return false
            db.update(
                columns.recordsTable,
                ContentValues().apply { put(columns.recordLedgerId, targetLedgerId) },
                "${columns.recordLedgerId} = ?",
                arrayOf(sourceLedgerId.toString())
            )
            if (root == sourceLedgerId) {
                db.update(
                    columns.assetsTable,
                    ContentValues().apply { put(columns.assetLedgerId, targetLedgerId) },
                    "${columns.assetLedgerId} = ?",
                    arrayOf(sourceLedgerId.toString())
                )
            }
            db.delete(columns.sharedLedgersTable, "${columns.sharedLedgerId} = ?", arrayOf(sourceLedgerId.toString()))
            db.delete(columns.ledgersTable, "${columns.ledgerId} = ?", arrayOf(sourceLedgerId.toString()))
            repointSharedReferences(db, members, sourceLedgerId, targetLedgerId)
            db.setTransactionSuccessful()
            true
        } catch (_: Exception) {
            false
        } finally {
            db.endTransaction()
        }
    }

    private fun poolRoot(ledgerId: Long): Long {
        val visited = mutableSetOf<Long>()
        var root = ledgerId
        while (visited.add(root)) root = getSharedSourceLedgerId(root) ?: break
        return root
    }

    private fun copyAsset(db: SQLiteDatabase, assetId: Long, targetLedgerId: Long): Long {
        val values = db.query(
            columns.assetsTable,
            null,
            "${columns.assetId} = ?",
            arrayOf(assetId.toString()),
            null,
            null,
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return -1L
            ContentValues().apply {
                put(columns.assetLedgerId, targetLedgerId)
                put(columns.assetName, cursor.getString(cursor.getColumnIndexOrThrow(columns.assetName)))
                put(columns.assetAmount, cursor.getLong(cursor.getColumnIndexOrThrow(columns.assetAmount)))
                put(columns.assetType, cursor.getInt(cursor.getColumnIndexOrThrow(columns.assetType)))
                put(columns.assetCategoryLabel, cursor.getString(cursor.getColumnIndexOrThrow(columns.assetCategoryLabel)))
                put(columns.assetCategoryIcon, cursor.getString(cursor.getColumnIndexOrThrow(columns.assetCategoryIcon)))
                put(columns.assetArchived, 0)
                put(columns.assetPinned, 0)
                put(columns.assetIncludeInTotal, cursor.getInt(cursor.getColumnIndexOrThrow(columns.assetIncludeInTotal)))
            }
        }
        return db.insertOrThrow(columns.assetsTable, null, values)
    }

    private fun assetExists(db: SQLiteDatabase, ledgerId: Long, assetId: Long) = db.query(
        columns.assetsTable,
        arrayOf(columns.assetId),
        "${columns.assetLedgerId} = ? AND ${columns.assetId} = ?",
        arrayOf(ledgerId.toString(), assetId.toString()),
        null,
        null,
        null
    ).use { it.moveToFirst() }

    private fun deleteSharedLedgers(db: SQLiteDatabase, ledgerIds: Set<Long>) {
        if (ledgerIds.isEmpty()) return
        db.delete(
            columns.sharedLedgersTable,
            "${columns.sharedLedgerId} IN (${ledgerIds.joinToString { "?" }})",
            ledgerIds.map(Long::toString).toTypedArray()
        )
    }

    private fun repointSharedReferences(db: SQLiteDatabase, members: List<Long>, removedSourceId: Long, keptLedgerId: Long) {
        val survivingIds = getLedgers().map { it.id }.toSet()
        val survivors = members.filter { it != removedSourceId && it in survivingIds }
        if (survivors.isEmpty()) return
        val oldRoot = db.rawQuery(
            "SELECT ${columns.sharedSourceLedgerId} FROM ${columns.sharedLedgersTable} " +
                "WHERE ${columns.sharedLedgerId} = ?",
            arrayOf(keptLedgerId.toString())
        ).use { if (it.moveToFirst()) it.getLong(0) else null }
        val newRoot = if (oldRoot == removedSourceId || oldRoot == null || oldRoot !in survivingIds) keptLedgerId else oldRoot
        deleteSharedLedgers(db, members.toSet())
        survivors.filter { it != newRoot }.forEach { survivor ->
            db.insertOrThrow(columns.sharedLedgersTable, null, ContentValues().apply {
                put(columns.sharedLedgerId, survivor)
                put(columns.sharedSourceLedgerId, newRoot)
            })
        }
    }

    private fun ledgerValues(iconName: String?, name: String?) = ContentValues().apply {
        iconName?.let { put(columns.ledgerIcon, it) }
        name?.let {
            put(columns.ledgerName, it)
            put(columns.ledgerSubtitle, "${it}账本")
        }
    }
}
