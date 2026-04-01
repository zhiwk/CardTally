# CardTally V1 完整规划文档

> 创建时间：2026-04-01
> 最后更新：2026-04-01
> 状态：规划阶段，已对齐

---

## 一、产品信息架构

### 1.1 六大一页能力区域

```
CardTally 6个一级能力区域
│
├── 首页 (Home)      - 陪伴感 + 轻量概览（日记风格）
├── 记录 (Records)   - 历史账目管理（独立标签页）
├── 记一笔 (Add)     - 高频录入（首页/记录页FAB入口）
├── 资产 (Assets)    - 账户与余额状态
├── Agent           - 协作助手（固定底部标签）
└── 我的 (Me)       - 低频能力收纳
```

### 1.2 底部导航结构（5-tab）

| 标签 | 图标 | 状态 |
|------|------|------|
| 首页 | ic_home | ✅ 重新设计为日记风格 |
| 记录 | ic_records | 🆕 新增（从HomeFragment剥离） |
| 资产 | ic_asset | 🔄 独立二级页面 |
| Agent | ic_agent | 🆕 新增（替换"统计"） |
| 我的 | ic_settings | 🔄 重建 |

**注意**："记一笔"不做底部导航tab，通过首页/记录页的FAB按钮进入。

---

## 二、已确认的业务规则

### 2.1 删除记录资产处理（Q1-A1）

**规则**：删除支出记录时，自动将对应金额加回资产余额，无需用户二次确认。

**代码逻辑**：
```kotlin
fun deleteRecord(record: Record) {
    if (record.type == EXPENSE) {
        assetBalance += record.amount // 自动回滚资产
    }
    // 删除记录本身
    database.delete(record.id)
}
```

**测试用例**：
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

**影响范围**：
- `DatabaseHelper.kt` - 删除逻辑
- `AssetFragment.kt` - 需监听记录删除事件并刷新资产显示

---

### 2.2 分类统计规则（Q2-A2）

**规则**：分类统计为扁平结构，子分类（如"餐饮-早餐"）不自动归入父类"餐饮"，独立统计。

**代码逻辑**：
```kotlin
fun getCategoryTotal(category: String): Double {
    return database.query {
        // 精确匹配，不含子类
        it.where("category = '$category'").sum("amount")
    }
}
```

**测试用例**：
```kotlin
fun testCategoryStatsFlat() {
    addRecord("餐饮-早餐", 20.0)
    addRecord("餐饮-午餐", 30.0)
    assertTotal("餐饮-早餐", 20.0) // 仅精确匹配
    assertTotal("餐饮", 0.0)      // 父类不含子类
}
```

---

### 2.3 Agent权限规则（Q3-B3）

**规则**：Agent可直接执行业务操作，无需用户二次确认，但必须生成完整的审计日志。

**审计日志字段**：
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

**日志存储路径**：`/data/audit/yyyy-MM-dd.log`（追加写入）

**操作类型**：
| 操作 | 风险级别 | 日志记录 |
|------|---------|---------|
| 新增记录 | 低 | ✅ |
| 修改备注 | 低 | ✅ |
| 修改金额 | 中 | ✅ |
| 删除记录 | 高 | ✅ |
| 转账 | 高 | ✅ |

---

### 2.4 离线模式（Q4）

**状态**：当前版本无云端服务器，所有数据本地存储。

**TODO**：未来版本如需云端同步，需补充：
- 本地草稿状态保留
- 冲突解决策略（待定）

---

### 2.5 性能优化策略（Q5-B5）

**规则**：按时间范围动态加载记录，默认加载最近3个月，最多200条。

**代码逻辑**：
```kotlin
fun loadRecords(range: TimeRange = TimeRange.last3Months()): List<Record> {
    return database.query {
        it.where("date BETWEEN '${range.start}' AND '${range.end}'")
         .orderBy("date DESC", "sort_order ASC")
         .limit(200)
    }
}
```

**SQL示例**：
```sql
SELECT * FROM records
WHERE date > date('now', '-3 months')
ORDER BY date DESC, sort_order ASC
LIMIT 200;
```

---

## 三、页面框架计划

### 3.1 计划清单

| 计划文件 | 状态 | 任务数 | 优先级 |
|---------|------|--------|--------|
| `records-page-framework.md` | ✅ 完成 | 8 | P0 |
| `home-page-framework.md` | ✅ 完成 | 6 | P0 |
| `assets-my-page-framework.md` | ✅ 完成 | 7 | P1 |
| `add-agent-page-framework.md` | ✅ 完成 | 8 | P1 |

---

### 3.2 records-page-framework.md

**目标**：创建独立的`RecordsFragment`作为记录标签页

**核心变更**：
- 新增 `nav_records` 到底部导航（在首页和资产之间）
- `HomeFragment` 剥离所有记录管理代码
- 新建 `RecordsFragment` 处理记录相关功能

**任务列表**：
| # | 任务 | 波次 | 依赖 |
|---|------|------|------|
| 1 | 建立Android测试基础设施 | Wave 1 | - |
| 2 | 添加数据库-backed搜索和过滤查询 | Wave 1 | 1 |
| 3 | 引入Fragment作用域的记录状态管理 | Wave 1 | 1, 2 |
| 4 | Fork并简化分组记录适配器 | Wave 1 | 1 |
| 5 | 创建`RecordsFragment` + 更新底部导航 | Wave 2 | 1, 2, 3, 4 |
| 6 | 现代化`SearchFragment` | Wave 2 | 1, 2, 3, 4 |
| 7 | 加固记录交互 + 保护遗留兼容性 | Wave 2 | 1, 4, 5, 6 |
| 8 | 最终确定CI、覆盖率和完整验证 | Wave 2 | 1, 5, 6, 7 |

**关键路径**：1 → 2 → 3 → 5 → 6 → 8

---

### 3.3 home-page-framework.md

**目标**：将`HomeFragment`重新设计为日记风格首页

**设计规格**（来自 `docs/stitch-guidance/home-page-spec.md`）：
| 模块 | 说明 | 优先级 |
|------|------|--------|
| 顶部日期 | 3月27日 周四 | P0 |
| 本月概览卡片 | 收入+支出+余额 | P0 |
| 最近记录 | 3条精选，点击进入编辑页 | P0 |
| 记一笔入口 | FAB悬浮按钮 | P0 |
| Agent入口卡片 | 首页卡片，名片/字条风格 | P0 |
| 空状态 | 方案二+管家风格文案 | P0 |

**明确不出现的内容**：
- 月份切换
- 复杂统计图表
- 周/月/年筛选按钮
- 资产快捷入口
- 搜索框
- 过长的记录列表

**任务列表**：
| # | 任务 | 波次 | 依赖 |
|---|------|------|------|
| 1 | 添加`HomeViewModel`状态管理 | Wave 1 | - |
| 2 | 创建首页布局（适配`fragment_home.xml`） | Wave 1 | 1 |
| 3 | 创建简化版首页记录适配器 | Wave 1 | 1 |
| 4 | 组装首页UI + 剥离记录管理代码 | Wave 2 | 1, 2, 3 |
| 5 | 与add-agent-plan协调`navigateToAgent()` | Wave 2 | 4 |
| 6 | 最终确定首页集成 + 完整验证 | Wave 2 | 4, 5 |

**关键路径**：1 → 2 → 4 → 5 → 6

---

### 3.4 assets-my-page-framework.md

**目标**：清理资产页 + 重建"我的"页

**核心变更**：
- 资产页：移除内联对话框，改为独立二级页面
- 资产详情：账户特定记录列表
- 我的页：从`SettingsFragment`重建，移除"统计"标签

**任务列表**：
| # | 任务 | 波次 | 依赖 |
|---|------|------|------|
| 1 | 创建资产添加/编辑的独立二级页面 | Wave 1 | - |
| 2 | 创建资产存档页面 | Wave 1 | - |
| 3 | 更新底部导航菜单（移除"统计"，添加"Agent"） | Wave 1 | - |
| 4 | 重构`AssetFragment`为纯列表 | Wave 1 | 1, 2 |
| 5 | 更新`MainActivity`导航处理 | Wave 2 | 3 |
| 6 | 重建"我的"页面 | Wave 2 | 3, 5 |
| 7 | 最终确定 + 验证 | Wave 2 | 4, 6 |

**关键路径**：1 → 2 → 4 → 6 → 7

---

### 3.5 add-agent-page-framework.md

**目标**：开发Agent标签页 + 对话功能 + 审计追踪

**核心变更**：
- Agent为固定底部标签（替换"统计"）
- 纯对话入口页面
- 支持完整业务写入
- 操作确认 + 审计日志
- 提供者失败时优雅降级

**任务列表**：
| # | 任务 | 波次 | 依赖 |
|---|------|------|------|
| 1 | 重建AddRecordFragment为单主保存按钮 | Wave 1 | - |
| 2 | 添加共享能力层 | Wave 1 | - |
| 3 | 硬化稳定引用 | Wave 1 | - |
| 4 | 创建Agent Fragment + 对话界面 | Wave 2 | 1, 2, 3 |
| 5 | 实现Agent对话逻辑 | Wave 2 | 4 |
| 6 | 实现操作确认流程 | Wave 2 | 4, 5 |
| 7 | 实现审计日志 | Wave 2 | 5, 6 |
| 8 | 最终确定 + 幂等性保证 | Wave 2 | 7 |

**关键路径**：1 → 2 → 3 → 4 → 5 → 6 → 7 → 8

---

## 四、执行顺序

### Phase 1：基础层（records-page + home-page）
```
records-page-framework (Task 1-4) ─┬─→ records-page-framework (Task 5-8)
                                   │
home-page-framework (Task 1-3) ────┴─→ home-page-framework (Task 4-6)
```

### Phase 2：功能层（assets-my + add-agent）
```
assets-my-page-framework (并行 Wave 1)
add-agent-page-framework (并行 Wave 1)
                              ↓
            assets-my-page-framework (Wave 2)
            add-agent-page-framework (Wave 2)
```

---

## 五、测试策略

### 5.1 测试基础设施
- JUnit 4（与现有`app/build.gradle:50`一致）
- Robolectric（单元测试）
- Espresso（UI测试）
- Jacoco（覆盖率）

### 5.2 覆盖率目标
| 层级 | 覆盖率要求 |
|------|-----------|
| 核心业务逻辑 | ≥80% |
| 数据库Helper | ≥70% |
| UI组件 | 关键路径覆盖 |

### 5.3 CI/CD
- GitHub Actions 工作流
- 触发条件：push + pull request
- 执行步骤：`assembleDebug` → `testDebugUnitTest` → `connectedDebugAndroidTest` → `jacocoTestReport`

---

## 六、已废弃/延期功能

| 功能 | 状态 | 原因 |
|------|------|------|
| 统计页（StatisticsFragment） | 延期v2 | 当前版本聚焦核心记账流程 |
| 分类层级继承 | 不采用 | Q2-A2确认：扁平统计 |
| 云端同步 | 暂不支持 | Q4确认：离线本地存储 |
| 导航组件迁移 | 暂不引入 | 保持现有Fragment+Helper结构 |

---

## 七、文档索引

| 文档 | 路径 | 用途 |
|------|------|------|
| 品牌设计指南 | `docs/stitch-guidance/brand-design-guide.md` | 产品定位、设计禁区 |
| 视觉设计指南 | `docs/stitch-guidance/visual-design-guide.md` | 色彩、字体、间距 |
| 页面设计指南 | `docs/stitch-guidance/page-design-guide.md` | 6个一级页面布局规范 |
| 首页设计规格 | `docs/stitch-guidance/home-page-spec.md` | 首页模块清单 |
| v1范围冻结 | `docs/plans/2026-03-26-cardtally-v1-scope-freeze.md` | 范围决策 |
| IA导航规范 | `docs/plans/2026-03-26-cardtally-ia-navigation-spec.md` | 导航结构 |
| Agent集成原则 | `docs/plans/2026-03-26-cardtally-agent-integration-principles.md` | Agent设计原则 |
| Agent能力定义 | `docs/plans/2026-03-26-cardtally-agent-capabilities.md` | Agent功能范围 |
| 业务规则 | `.sisyphus/decisions/business_rules.md` | 业务逻辑决策 |

---

**下一步**：开始执行 `records-page-framework.md`
