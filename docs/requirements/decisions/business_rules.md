# 业务规则决策文档

> 创建时间：2026-04-01
> 最后更新：2026-04-01
> 决策者：产品+研发对齐

---

## 1. 删除记录资产处理（Q1-A1）

### 规则
删除支出记录时，自动将对应金额加回资产余额，无需用户二次确认。

### 代码逻辑
```kotlin
// DatabaseHelper.kt
/**
 * 删除记录并自动更新资产（基于规则 Q1-A1）
 * @warning 支出删除会立即回滚资产，无需确认
 */
fun deleteRecord(record: Record) {
    if (record.type == EXPENSE) {
        assetBalance += record.amount // 自动回滚资产
    }
    database.delete(record.id)
}
```

### 测试用例
```kotlin
fun testDeleteExpenseRefundsAsset() {
    val initialBalance = getAssetBalance()
    addExpenseRecord(100.0)
    deleteLastRecord()
    assertBalance(initialBalance) // 应等于初始值
}

fun testDeleteIdempotency() {
    deleteRecord(recordId)
    deleteRecord(recordId) // 第二次删除应无资产变动
    assertAssetUnchanged()
}
```

### 影响范围
- `DatabaseHelper.kt` - 删除逻辑
- `AssetFragment.kt` - 需监听记录删除事件并刷新资产显示

---

## 2. 分类统计规则（Q2-A2）

### 规则
分类统计为扁平结构，子分类（如"餐饮-早餐"）不自动归入父类"餐饮"，独立统计。

### 代码逻辑
```kotlin
// 精确匹配，不含子类
fun getCategoryTotal(category: String): Double {
    return database.query {
        it.where("category = '$category'").sum("amount")
    }
}
```

### 测试用例
```kotlin
fun testCategoryStatsFlat() {
    addRecord("餐饮-早餐", 20.0)
    addRecord("餐饮-午餐", 30.0)
    assertTotal("餐饮-早餐", 20.0) // 仅精确匹配
    assertTotal("餐饮", 0.0)      // 父类不含子类
}
```

### 影响范围
- `StatisticsFragment.kt` - 分类统计计算
- `DatabaseHelper.kt` - 查询逻辑

---

## 3. Agent权限规则（Q3-B3，已废弃）

> 本节为历史规则，已被 `docs/requirements/decisions/2026-08-12-light-ux-redesign.md` 第 4 节取代。当前 Agent 只能进行用户主动触发的 BYOK 文本对话，不得直接创建、修改、删除或转账。

以下内容仅保留作历史记录，不得作为当前实现依据。

### 规则
Agent可直接执行业务操作，无需用户二次确认，但必须生成完整的审计日志。

### 审计日志格式
```json
{
  "timestamp": "2026-04-01T10:30:00",
  "agent_session_id": "session_xxx",
  "operation": "edit_amount",
  "record_id": 123,
  "old_value": 100.0,
  "new_value": 200.0,
  "status": "success"
}
```

### 日志存储
- 路径：`/data/audit/yyyy-MM-dd.log`
- 格式：JSON Lines（追加写入）
- 保留：永久保留（暂不设清理策略）

### 操作风险级别
| 操作 | 风险级别 | 日志记录 |
|------|---------|---------|
| 新增记录 | 低 | ✅ |
| 修改备注 | 低 | ✅ |
| 修改金额 | 中 | ✅ |
| 删除记录 | 高 | ✅ |
| 转账 | 高 | ✅ |

### 影响范围
- `AgentFragment.kt` - 对话处理入口
- `DatabaseHelper.kt` - 如后续落地审计日志，可作为持久化扩展点
- `model/` - 如需独立日志模型，应在当前模型目录下新增

---

## 4. 离线模式（Q4）

### 状态
当前版本无云端服务器，所有数据本地存储。

### TODO（未来版本）
- 本地草稿状态保留
- 冲突解决策略（待定）

---

## 5. 性能优化策略（Q5-B5）

### 规则
按时间范围动态加载记录，默认加载最近3个月，最多200条。

### 代码逻辑
```kotlin
fun loadRecords(range: TimeRange = TimeRange.last3Months()): List<Record> {
    return database.query {
        it.where("date BETWEEN '${range.start}' AND '${range.end}'")
         .orderBy("date DESC", "sort_order ASC")
         .limit(200)
    }
}
```

### SQL示例
```sql
SELECT * FROM records
WHERE date > date('now', '-3 months')
ORDER BY date DESC, sort_order ASC
LIMIT 200;
```

### 影响范围
- `DatabaseHelper.kt` - 查询方法
- `HomeFragment.kt` - 首页最近记录
- `SearchFragment.kt` - 记录检索与结果展示

---

## 6. 变更记录

| 日期 | 变更内容 | 决策者 |
|------|---------|--------|
| 2026-04-01 | 初始业务规则对齐完成 | 产品+研发 |

## 7. 账本与资产关系（Q5-C1）

### 规则
- 资产通过不可变资产 ID 绑定记录，不通过名称绑定。
- 新建账本默认不共享资产；用户可选择与另一个账本共用完整资产集合，或复制该账本的完整资产集合为目标账本的独立资产。
- 共用资产共享余额和资产所有权，非所有者账本不可编辑、置顶、归档或删除该资产。
- 复制资产生成新的资产 ID，初始余额复制当时的余额，后续余额和操作完全独立。
- 合并账本必须二次确认；记录、分类和资产迁移至当前账本，源账本删除，资产 ID 与历史记录保持不变。

## 8. 全局分类

### 规则
- 分类是应用级唯一数据集，不隶属于账本、资产或其他业务对象。
- 新建账本不会复制或创建分类；所有账本使用同一份一级、二级分类树。
- 记录只能选择叶子分类；分类的父子关系、排序和图标在全局范围内生效。
- 从旧版本升级时，按“收支类型 + 父分类 + 名称”合并重复分类，并将记录引用迁移到保留的分类 ID。
