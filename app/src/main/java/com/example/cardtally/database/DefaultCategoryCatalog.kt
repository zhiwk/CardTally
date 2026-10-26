package com.example.cardtally.database

/** Owner-approved merged directory; historical paths live separately for migration/backup recognition. */
internal object DefaultCategoryCatalog {
    data class Entry(val type: Int, val parent: String?, val name: String, val icon: String, val sortOrder: Int) {
        val pathKey: String get() = "$type|${parent.orEmpty()}|$name"
    }
    private data class Group(val type: Int, val name: String, val icon: String, val children: String)
    private val groups = listOf(
        Group(0, "餐饮食品", "tabler_tools_kitchen", "餐饮、外卖、零食、水果、饮品、糕点、食材"),
        Group(0, "日用购物", "tabler_shopping_bag", "日用百货、清洁用品、纸品、厨房用品、其他购物"),
        Group(0, "服饰美容", "tabler_shirt", "衣服、鞋帽、箱包、配饰、洗护护肤、化妆品、美容美发、美甲"),
        Group(0, "数码电子", "tabler_device_laptop", "手机、电脑、数码设备、数码配件、数码维修"),
        Group(0, "居住家庭", "tabler_home", "房租、房贷、物业、水费、电费、燃气、家具、家电、家居用品、装修"),
        Group(0, "交通出行", "tabler_bus", "公交、地铁、打车、客运、高铁、飞机、共享单车"),
        Group(0, "汽车用车", "tabler_car", "加油、充电、停车、通行费、保养、维修、车险、违章罚款"),
        Group(0, "通信订阅", "tabler_phone", "话费、宽带、视频会员、音乐会员、软件服务、云存储、其他订阅"),
        Group(0, "医疗健康", "tabler_pill", "挂号问诊、药品、检查化验、体检、牙科、保健品"),
        Group(0, "教育学习", "tabler_school", "学费、培训、书籍、文具、办公用品"),
        Group(0, "休闲运动", "tabler_barbell", "电影演出、聚会娱乐、游戏、健身、运动装备、运动课程、户外活动、其他娱乐"),
        Group(0, "旅行度假", "tabler_plane", "酒店住宿、景点门票、旅行服务、其他旅行"),
        Group(0, "宠物", "tabler_paw", "宠物食品、宠物用品、宠物医疗、宠物美容"),
        Group(0, "人情往来", "tabler_gift", "红包、礼金、礼物、捐赠"),
        Group(0, "其他支出", "tabler_category", "保险保费、投资亏损、其他支出"),
        Group(1, "工资奖金", "tabler_briefcase", "基本工资、绩效奖金、年终奖、项目奖金、加班费"),
        Group(1, "兼职副业", "tabler_clock", "兼职收入、副业收入、其他劳务收入"),
        Group(1, "投资理财", "tabler_building_bank", "银行利息、基金收益、股票收益、黄金收益、其他理财收益"),
        Group(1, "红包礼金", "tabler_gift", "红包、礼金、其他赠与"),
        Group(1, "报销", "tabler_receipt", "差旅报销、餐费报销、其他报销"),
        Group(1, "退款", "tabler_arrow_back_up", "购物退款、服务退款"),
        Group(1, "二手交易", "tabler_recycle", "闲置物品、其他二手收入"),
        Group(1, "福利补贴", "tabler_shield", "社会补贴、生育补贴、退税、公积金提取"),
        Group(1, "其他收入", "tabler_category", "赔偿收入、其他收入")
    )
    val entries: List<Entry> = buildList {
        groups.groupBy { it.type }.forEach { (type, items) ->
            items.forEachIndexed { order, group ->
                add(Entry(type, null, group.name, group.icon, order))
                group.children.split('、').forEachIndexed { index, child ->
                    add(Entry(type, group.name, child, group.icon, index))
                }
            }
        }
    }
}
