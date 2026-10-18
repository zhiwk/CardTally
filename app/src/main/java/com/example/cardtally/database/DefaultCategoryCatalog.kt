package com.example.cardtally.database

/** Category labels/grouping reference BeeCount; initialization and icons are CardTally's own. */
internal object DefaultCategoryCatalog {
    private data class Group(val type: Int, val name: String, val icon: String, val children: List<String>)
    data class Entry(val type: Int, val parent: String?, val name: String, val icon: String, val sortOrder: Int) {
        val pathKey: String get() = "$type|${parent.orEmpty()}|$name"
    }
    private val groups = listOf(
        Group(0, "餐饮", "tabler_tools_kitchen", listOf("早餐", "午餐", "晚餐", "美团外卖", "饿了么外卖", "京东外卖", "餐厅", "美食")),
        Group(0, "零食", "tabler_cookie", listOf("饼干", "薯片", "糖果", "巧克力", "坚果")),
        Group(0, "水果", "tabler_apple", listOf("苹果", "香蕉", "橙子", "葡萄", "西瓜", "其他水果")),
        Group(0, "饮品", "tabler_cup", listOf("奶茶", "咖啡", "果汁", "汽水", "矿泉水")),
        Group(0, "糕点", "tabler_cake", listOf("蛋糕", "面包", "甜点", "曲奇")),
        Group(0, "做饭食材", "tabler_carrot", listOf("蔬菜", "肉类", "水产", "调料", "粮油")),
        Group(0, "购物", "tabler_shopping_bag", listOf("服装", "鞋帽", "包包", "配饰", "日用百货")),
        Group(0, "宠物", "tabler_paw", listOf("宠物食品", "宠物用品", "宠物医疗", "宠物美容")),
        Group(0, "交通", "tabler_bus", listOf("地铁", "公交", "出租车", "网约车", "停车费", "加油")),
        Group(0, "汽车", "tabler_car", listOf("汽车保养", "汽车维修", "汽车保险", "洗车", "违章罚款")),
        Group(0, "服饰", "tabler_shirt", listOf("上衣", "裤子", "裙子", "鞋子", "服饰配件")),
        Group(0, "日用品", "tabler_wash", listOf("洗护用品", "纸品", "清洁用品", "厨房用品")),
        Group(0, "教育", "tabler_school", listOf("学费", "培训费", "书籍", "文具", "办公用品")),
        Group(0, "投资亏损", "tabler_trending_down", listOf("股票亏损", "基金亏损", "其他投资亏损")),
        Group(0, "娱乐", "tabler_movie", listOf("电影", "KTV", "游乐场", "酒吧", "其他娱乐")),
        Group(0, "游戏", "tabler_device_gamepad_2", listOf("游戏充值", "游戏装备", "游戏会员")),
        Group(0, "保健品", "tabler_pill", listOf("维生素", "保健食品", "营养品")),
        Group(0, "订阅服务", "tabler_repeat", listOf("视频会员", "音乐会员", "云存储", "其他订阅")),
        Group(0, "运动", "tabler_barbell", listOf("健身房", "运动装备", "运动课程", "户外活动")),
        Group(0, "住房", "tabler_home", listOf("房租", "物业费", "房贷", "装修")),
        Group(0, "居家", "tabler_sofa", listOf("家具", "家电", "装饰品", "床上用品")),
        Group(0, "美容", "tabler_brush", listOf("护肤品", "化妆品", "美容美发", "美甲")),
        Group(1, "工资", "tabler_briefcase", listOf("基本工资", "绩效奖金", "年终奖", "加班费")),
        Group(1, "理财", "tabler_building_bank", listOf("基金收益", "股票分红", "理财产品", "其他理财")),
        Group(1, "红包", "tabler_gift", listOf("节日红包", "生日红包", "随礼回礼")),
        Group(1, "奖金", "tabler_award", listOf("年度奖金", "季度奖", "项目奖金", "其他奖金")),
        Group(1, "报销", "tabler_receipt", listOf("差旅报销", "餐费报销", "其他报销")),
        Group(1, "兼职", "tabler_clock", listOf("兼职收入", "外快")),
        Group(1, "礼金", "tabler_heart", listOf("结婚礼金", "生日礼金", "其他礼金")),
        Group(1, "利息", "tabler_percentage", listOf("银行利息", "其他利息")),
        Group(1, "退款", "tabler_arrow_back_up", listOf("购物退款", "服务退款", "其他退款")),
        Group(1, "投资收益", "tabler_chart_line", listOf("股票收益", "基金投资", "其他投资收益")),
        Group(1, "二手交易", "tabler_recycle", listOf("闲置物品", "二手商品")),
        Group(1, "社会福利", "tabler_shield", listOf("失业保险", "生育津贴", "其他补贴")),
        Group(1, "退税", "tabler_receipt_tax", listOf("个税退税", "其他退费")),
        Group(1, "公积金", "tabler_pig_money", listOf("公积金提取", "公积金利息")),
    )
    val entries: List<Entry> = buildList {
        for (type in listOf(0, 1)) {
            groups.filter { it.type == type }.forEachIndexed { order, group ->
                add(Entry(type, null, group.name, group.icon, order))
                group.children.forEachIndexed { childOrder, name ->
                    add(Entry(type, group.name, name, group.icon, childOrder))
                }
            }
        }
    }
}
