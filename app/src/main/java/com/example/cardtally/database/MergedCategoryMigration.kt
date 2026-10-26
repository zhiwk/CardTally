package com.example.cardtally.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/** Explicit historical paths only. Unknown names/deeper custom subtrees are not guessed. */
internal object MergedCategoryMigration {
    const val ALIASES = "category_merge_aliases"
    data class Target(val type: Int, val parent: String, val name: String)

    private val expenseRoots = mapOf(
        "餐饮" to "餐饮食品", "零食" to "餐饮食品", "水果" to "餐饮食品", "饮品" to "餐饮食品",
        "糕点" to "餐饮食品", "做饭食材" to "餐饮食品", "购物" to "日用购物", "日用品" to "日用购物",
        "服饰" to "服饰美容", "美容" to "服饰美容", "居住" to "居住家庭", "住房" to "居住家庭",
        "居家" to "居住家庭", "交通" to "交通出行", "汽车" to "汽车用车", "订阅服务" to "通信订阅",
        "保健品" to "医疗健康", "教育" to "教育学习", "娱乐" to "休闲运动", "游戏" to "休闲运动",
        "运动" to "休闲运动", "投资亏损" to "其他支出", "宠物" to "宠物")
    private val incomeRoots = mapOf("工作" to "工资奖金", "工资" to "工资奖金", "奖金" to "工资奖金",
        "理财" to "投资理财", "投资收益" to "投资理财", "利息" to "投资理财", "兼职" to "兼职副业",
        "红包" to "红包礼金", "礼金" to "红包礼金", "社会福利" to "福利补贴", "退税" to "福利补贴",
        "公积金" to "福利补贴", "报销" to "报销", "退款" to "退款", "二手交易" to "二手交易")

    fun target(type: Int, parent: String, name: String): Target? {
        val root = (if (type == 0) expenseRoots else incomeRoots)[parent] ?: return null
        val known = ReferencedCategoryCatalog.entries.any { it.type == type && it.parent == parent && it.name == name }
        val old = if (type == 0) mapOf("购物" to setOf("服饰", "家电", "数码"), "餐饮" to setOf("早午晚餐", "早中午餐"),
            "居住" to setOf("房租", "酒店"), "交通" to setOf("短途", "飞机高铁"))
        else mapOf("工作" to setOf("工资", "报销"), "理财" to setOf("股票", "基金", "黄金"))
        if (!known && name !in old[parent].orEmpty()) return null
        if (type == 0) {
            if (parent in setOf("零食", "水果", "饮品", "糕点")) return Target(type, root, parent)
            if (parent == "做饭食材") return Target(type, root, "食材")
            if (parent == "保健品") return Target(type, root, "保健品")
            if (parent == "投资亏损") return Target(type, root, "投资亏损")
            if (parent == "游戏") return Target(type, root, "游戏")
            return when (name) {
                "早餐", "午餐", "晚餐", "早午晚餐", "早中午餐", "餐厅", "美食" -> Target(type, "餐饮食品", "餐饮")
                "美团外卖", "饿了么外卖", "京东外卖" -> Target(type, "餐饮食品", "外卖")
                "家电" -> Target(type, "居住家庭", "家电")
                "数码" -> Target(type, "数码电子", "数码设备")
                "服饰" -> Target(type, "服饰美容", "服饰")
                "酒店" -> Target(type, "旅行度假", "酒店住宿")
                "服装", "上衣", "裤子", "裙子" -> Target(type, "服饰美容", "衣服")
                "鞋子", "鞋帽" -> Target(type, "服饰美容", "鞋帽")
                "包包" -> Target(type, "服饰美容", "箱包")
                "配饰", "服饰配件" -> Target(type, "服饰美容", "配饰")
                "洗护用品", "护肤品" -> Target(type, "服饰美容", "洗护护肤")
                "出租车", "网约车" -> Target(type, root, "打车")
                "停车费", "加油" -> Target(type, "汽车用车", if (name == "停车费") "停车" else name)
                "汽车保养" -> Target(type, root, "保养")
                "汽车维修" -> Target(type, root, "维修")
                "汽车保险" -> Target(type, root, "车险")
                "物业费" -> Target(type, root, "物业")
                "装饰品", "床上用品" -> Target(type, root, "家居用品")
                "培训费" -> Target(type, root, "培训")
                "电影" -> Target(type, root, "电影演出")
                "健身房" -> Target(type, root, "健身")
                else -> Target(type, root, name) // Preserve broad/ambiguous old labels, e.g. 短途/飞机高铁.
            }
        }
        return when (parent) {
            "红包" -> Target(type, root, if (name == "随礼回礼") "礼金" else "红包")
            "礼金" -> Target(type, root, "礼金")
            "退税" -> Target(type, root, "退税")
            "社会福利" -> Target(type, root, if (name == "生育津贴") "生育补贴" else "社会补贴")
            else -> when (name) {
                "股票", "股票分红" -> Target(type, root, "股票收益")
                "基金", "基金投资" -> Target(type, root, "基金收益")
                "黄金" -> Target(type, root, "黄金收益")
                "其他理财", "理财产品", "其他投资收益", "其他利息", "公积金利息" -> Target(type, "投资理财", "其他理财收益")
                "外快" -> Target(type, root, "副业收入")
                "二手商品" -> Target(type, root, "其他二手收入")
                // Do not guess whether old 工资 means 基本工资, or 报销 means 差旅报销.
                "报销" -> Target(type, "报销", "报销")
                else -> Target(type, root, name)
            }
        }
    }

    fun apply(db: SQLiteDatabase, completeCatalog: () -> Unit) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $ALIASES (old_id INTEGER PRIMARY KEY, new_id INTEGER NOT NULL)")
        data class Row(val id: Long, val type: Int, val parent: Long?, val name: String)
        val rows = db.rawQuery("SELECT id, type, parent_id, name FROM categories ORDER BY sort_order, id", null).use { c ->
            buildList { while (c.moveToNext()) add(Row(c.getLong(0), c.getInt(1), if (c.isNull(2)) null else c.getLong(2), c.getString(3))) }
        }
        val byId = rows.associateBy { it.id }
        fun hasChildren(id: Long) = count(db, "SELECT COUNT(*) FROM categories WHERE parent_id = ?", id) > 0
        fun referenced(id: Long) = listOf("records", "recurring_records", "record_deletion_undo").any {
            count(db, "SELECT COUNT(*) FROM $it WHERE category_id = ?", id) > 0
        }
        fun root(type: Int, name: String): Long {
            db.rawQuery("SELECT id FROM categories WHERE type = ? AND parent_id IS NULL AND name = ? " +
                "AND EXISTS (SELECT 1 FROM categories child WHERE child.parent_id = categories.id) ORDER BY sort_order, id",
                arrayOf(type.toString(), name)).use { if (it.moveToFirst()) return it.getLong(0) }
            val entry = DefaultCategoryCatalog.entries.first { it.type == type && it.parent == null && it.name == name }
            return db.insertOrThrow("categories", null, ContentValues().apply {
                put("type", type); put("name", name); put("icon", entry.icon); put("ledger_id", 1)
                put("sort_order", nextOrder(db, type, null))
            })
        }
        val roots = mutableMapOf<Pair<Int, String>, Long>()
        fun targetRoot(type: Int, name: String) = roots.getOrPut(type to name) { root(type, name) }
        val sources = rows.mapNotNull { row ->
            val parent = row.parent?.let(byId::get) ?: return@mapNotNull null
            if (parent.parent != null || parent.type != row.type || hasChildren(row.id)) return@mapNotNull null
            target(row.type, parent.name, row.name)?.let { it to row }
        }.groupBy({ it.first }, { it.second })
        for ((target, candidates) in sources) {
            val parentId = targetRoot(target.type, target.parent)
            val existing = db.rawQuery("SELECT id FROM categories WHERE type = ? AND parent_id = ? AND name = ? " +
                "AND NOT EXISTS (SELECT 1 FROM categories child WHERE child.parent_id = categories.id) ORDER BY sort_order, id",
                arrayOf(target.type.toString(), parentId.toString(), target.name)).use { if (it.moveToFirst()) it.getLong(0) else null }
            val retained = existing ?: candidates.first().id
            if (existing == null) db.update("categories", ContentValues().apply {
                put("parent_id", parentId); put("name", target.name); put("sort_order", nextOrder(db, target.type, parentId))
            }, "id = ?", arrayOf(retained.toString()))
            candidates.filter { it.id != retained }.forEach { merge(db, it.id, retained) }
        }
        // Only mapped groups have been created so far, and each already contains a moved leaf.
        // Filling other groups now avoids empty placeholder roots being duplicated by the appender.
        completeCatalog()
        for (row in rows.filter { it.parent == null }) {
            // An original root leaf is not an obsolete emptied group, even without current references.
            if (rows.none { it.parent == row.id }) continue
            val name = (if (row.type == 0) expenseRoots else incomeRoots)[row.name] ?: continue
            if (!hasChildren(row.id) && !referenced(row.id)) {
                val replacement = targetRoot(row.type, name)
                if (row.id != replacement) merge(db, row.id, replacement)
            }
        }
    }

    private fun merge(db: SQLiteDatabase, old: Long, retained: Long) {
        for (table in listOf("records", "recurring_records", "record_deletion_undo"))
            db.execSQL("UPDATE $table SET category_id = ? WHERE category_id = ?", arrayOf(retained, old))
        db.execSQL("UPDATE backup_import_map SET local_row_id = ? WHERE table_name = 'categories' AND local_row_id = ?", arrayOf(retained, old))
        db.execSQL("UPDATE $ALIASES SET new_id = ? WHERE new_id = ?", arrayOf(retained, old))
        db.execSQL("INSERT OR REPLACE INTO $ALIASES VALUES (?, ?)", arrayOf(old, retained))
        db.delete("categories", "id = ?", arrayOf(old.toString()))
    }

    private fun count(db: SQLiteDatabase, sql: String, id: Long) = db.rawQuery(sql, arrayOf(id.toString())).use { it.moveToFirst(); it.getInt(0) }
    private fun nextOrder(db: SQLiteDatabase, type: Int, parent: Long?): Int = db.rawQuery(
        "SELECT COALESCE(MAX(sort_order), -1) + 1 FROM categories WHERE type = ? AND " +
            if (parent == null) "parent_id IS NULL" else "parent_id = ?",
        if (parent == null) arrayOf(type.toString()) else arrayOf(type.toString(), parent.toString())
    ).use { it.moveToFirst(); it.getInt(0) }
}
