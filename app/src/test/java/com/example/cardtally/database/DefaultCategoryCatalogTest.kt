package com.example.cardtally.database

import org.junit.Assert.*
import org.junit.Test

class DefaultCategoryCatalogTest {
    @Test fun defaultGroupsHaveTheRequestedOrderAndCoverage() {
        val entries = DefaultCategoryCatalog.entries
        assertEquals(listOf("餐饮食品", "日用购物", "服饰美容", "数码电子", "居住家庭", "交通出行",
            "汽车用车", "通信订阅", "医疗健康", "教育学习", "休闲运动", "旅行度假", "宠物", "人情往来", "其他支出"),
            entries.filter { it.type == 0 && it.parent == null }.map { it.name })
        assertEquals(listOf("工资奖金", "兼职副业", "投资理财", "红包礼金", "报销", "退款", "二手交易", "福利补贴", "其他收入"),
            entries.filter { it.type == 1 && it.parent == null }.map { it.name })
        assertEquals(91, entries.count { it.type == 0 && it.parent != null })
        assertEquals(29, entries.count { it.type == 1 && it.parent != null })
        assertEquals(listOf("餐饮", "外卖", "零食", "水果", "饮品", "糕点", "食材"),
            entries.filter { it.type == 0 && it.parent == "餐饮食品" }.map { it.name })
    }

    @Test fun pathsAreUniqueParentsPrecedeChildrenAndSiblingOrdersAreContinuous() {
        val entries = DefaultCategoryCatalog.entries
        assertEquals(entries.size, entries.map { it.pathKey }.toSet().size)
        val parents = mutableSetOf<Pair<Int, String>>()
        entries.forEach { entry ->
            assertTrue(entry.type == 0 || entry.type == 1)
            assertTrue(entry.name.isNotBlank())
            assertTrue(entry.icon.startsWith("tabler_"))
            if (entry.parent == null) parents.add(entry.type to entry.name)
            else assertTrue(parents.contains(entry.type to entry.parent))
        }
        entries.groupBy { it.type to it.parent }.values.forEach { siblings ->
            assertEquals(siblings.indices.toList(), siblings.map { it.sortOrder })
        }
        parents.forEach { (type, name) ->
            assertTrue(entries.any { it.type == type && it.parent == name })
        }
    }
}
