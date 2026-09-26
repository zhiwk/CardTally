package com.example.cardtally.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/** Creates the canonical initial income/expense category tree for a new local database. */
internal class CategoryDefaultsSeeder(private val columns: Columns) {
    data class Columns(
        val table: String,
        val id: String,
        val name: String,
        val type: String,
        val icon: String,
        val parentId: String,
        val sortOrder: String,
        val ledgerId: String
    )

    fun seed(db: SQLiteDatabase, ledgerId: Long) {
        val parents = listOf(
            "购物" to "ic_category_shopping",
            "餐饮" to "ic_category_food",
            "居住" to "ic_category_housing",
            "交通" to "ic_category_transport"
        )
        val parentIds = mutableMapOf<String, Long>()
        parents.forEachIndexed { index, (name, icon) ->
            parentIds[name] = db.insertOrThrow(columns.table, null, ContentValues().apply {
                put(columns.name, name)
                put(columns.type, 0)
                put(columns.icon, icon)
                put(columns.sortOrder, index)
                put(columns.ledgerId, 1L)
            })
        }
        val children = mapOf(
            "购物" to listOf("服饰" to "tabler_shirt", "家电" to "tabler_devices", "数码" to "tabler_device_laptop"),
            "餐饮" to listOf("早午晚餐" to "tabler_tools_kitchen"),
            "居住" to listOf("房租" to "tabler_home", "酒店" to "tabler_hotel_service"),
            "交通" to listOf("短途" to "tabler_car", "飞机高铁" to "tabler_plane")
        )
        children.forEach { (parentName, items) ->
            val parentId = parentIds[parentName] ?: return@forEach
            items.forEach { (name, icon) ->
                db.insertOrThrow(columns.table, null, ContentValues().apply {
                    put(columns.name, name)
                    put(columns.type, 0)
                    put(columns.icon, icon)
                    put(columns.parentId, parentId)
                    put(columns.ledgerId, 1L)
                })
            }
        }
        seedIncome(db, ledgerId)
    }

    private fun seedIncome(db: SQLiteDatabase, ledgerId: Long) {
        val parents = listOf("工作" to "tabler_briefcase", "理财" to "tabler_building_bank")
        val parentIds = mutableMapOf<String, Long>()
        parents.forEachIndexed { index, (name, icon) ->
            val parentId = findCategoryId(db, name, 1, null) ?: db.insertOrThrow(
                columns.table,
                null,
                ContentValues().apply {
                    put(columns.name, name)
                    put(columns.type, 1)
                    put(columns.icon, icon)
                    put(columns.sortOrder, index)
                    put(columns.ledgerId, 1L)
                }
            )
            parentIds[name] = parentId
        }
        val children = mapOf(
            "工作" to listOf("工资" to "ic_category_salary", "报销" to "tabler_receipt"),
            "理财" to listOf("股票" to "tabler_chart_line", "基金" to "tabler_chart_donut", "黄金" to "tabler_pig_money")
        )
        children.forEach { (parentName, items) ->
            items.forEachIndexed { index, (name, icon) ->
                val parentId = parentIds[parentName] ?: return@forEachIndexed
                if (findCategoryId(db, name, 1, parentId) == null) {
                    db.insertOrThrow(columns.table, null, ContentValues().apply {
                        put(columns.name, name)
                        put(columns.type, 1)
                        put(columns.icon, icon)
                        put(columns.parentId, parentId)
                        put(columns.sortOrder, index)
                        put(columns.ledgerId, 1L)
                    })
                }
            }
        }
    }

    private fun findCategoryId(db: SQLiteDatabase, name: String, type: Int, parentId: Long?): Long? {
        val where = if (parentId == null) {
            "${columns.name} = ? AND ${columns.type} = ? AND ${columns.parentId} IS NULL"
        } else {
            "${columns.name} = ? AND ${columns.type} = ? AND ${columns.parentId} = ?"
        }
        val args = if (parentId == null) arrayOf(name, type.toString())
        else arrayOf(name, type.toString(), parentId.toString())
        return db.query(columns.table, arrayOf(columns.id), where, args, null, null, null, "1")
            .use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
    }
}
