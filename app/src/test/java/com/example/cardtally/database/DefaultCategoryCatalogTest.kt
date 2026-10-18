package com.example.cardtally.database

import org.junit.Assert.*
import org.junit.Test

class DefaultCategoryCatalogTest {
    @Test fun defaultGroupsHaveTheRequestedOrderAndCoverage() {
        val entries = DefaultCategoryCatalog.entries
        assertEquals(listOf("餐饮", "零食", "水果", "饮品", "糕点", "做饭食材", "购物", "宠物",
            "交通", "汽车", "服饰", "日用品", "教育", "投资亏损", "娱乐", "游戏", "保健品",
            "订阅服务", "运动", "住房", "居家", "美容"),
            entries.filter { it.type == 0 && it.parent == null }.map { it.name })
        assertEquals(listOf("工资", "理财", "红包", "奖金", "报销", "兼职", "礼金", "利息", "退款",
            "投资收益", "二手交易", "社会福利", "退税", "公积金"),
            entries.filter { it.type == 1 && it.parent == null }.map { it.name })
        assertEquals(101, entries.count { it.type == 0 && it.parent != null })
        assertEquals(40, entries.count { it.type == 1 && it.parent != null })
        assertEquals(listOf("早餐", "午餐", "晚餐", "美团外卖", "饿了么外卖", "京东外卖", "餐厅", "美食"),
            entries.filter { it.type == 0 && it.parent == "餐饮" }.map { it.name })
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
