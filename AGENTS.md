# AGENTS.md

CardTally 仓库的 AI 协作入口文件。开始任何修改前，先读本文件，再按任务类型读取对应文档。

## 1. 项目定位

CardTally 是一个 **原生 Android 记账应用**，当前技术栈与工程事实如下：

- 语言：Kotlin
- 构建：Gradle（仓库中实际可见为 Android Gradle Plugin `8.3.0`，Gradle wrapper `8.13`）
- 模块：单模块 `:app`
- 最低 SDK：24
- 目标 / 编译 SDK：34
- UI：XML 布局 + Material Components + Fragment
- 数据层：SQLite（`DatabaseHelper.kt`）

> 注意：修改时应优先相信 **仓库中的构建脚本和源码**，不要以 README 或规划文档的描述替代代码事实。

## 2. 先读什么：按任务类型选择

### 所有任务都先读

1. `README.md`
2. 本文件 `AGENTS.md`

### 涉及业务规则 / 数据逻辑时再读

1. `.sisyphus/decisions/business_rules.md`
2. `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`
3. 受影响页面对应的 Fragment / Adapter / Model

### 涉及页面重构 / 信息架构 / 首版产品方向时再读

1. `docs/plans/2026-03-26-cardtally-implementation-plan.md`
2. `docs/plans/2026-03-26-cardtally-product-restart-design.md`
3. 相关 `docs/plans/*.md`

### 涉及视觉设计 / UI 风格 / Stitch 设计稿时再读

1. `docs/stitch-guidance/README.md`
2. `docs/stitch-guidance/brand-design-guide.md`
3. `docs/stitch-guidance/visual-design-guide.md`
4. `docs/stitch-guidance/page-design-guide.md`
5. `docs/stitch-guidance/home-page-spec.md`
6. 必要时查看 `design/stitch_extracted/`

### 涉及历史计划 / 过程文档时再读

- `.sisyphus/plans/*.md`
- `design/stitch_extracted/`

这些文件可用于理解历史执行意图、阶段性方案或外部设计产物，但**不能直接当成当前代码现状的权威来源**。必须回到代码与权威文档验证。

## 3. 仓库地图

### 代码

- `app/src/main/java/com/example/cardtally/`
  - `MainActivity.kt`：主 Activity
  - `*Fragment.kt`：页面级逻辑，如 `HomeFragment`、`AddRecordFragment`、`StatisticsFragment`、`SettingsFragment`、`AssetFragment`
  - `adapter/`：RecyclerView 相关适配器
  - `database/`：SQLite 数据访问，核心在 `DatabaseHelper.kt`
  - `model/`：数据模型
  - `util/`：工具类，例如主题、快捷记账、列表交互辅助

### 资源

- `app/src/main/res/layout/`：页面和列表项布局
- `app/src/main/res/values/`：颜色、尺寸、样式、字符串等设计系统资源
- `app/src/main/res/drawable/`：图标、背景、形状
- `app/src/main/res/menu/`：导航与菜单

### 文档与规划

- `docs/plans/`：产品重开、信息架构、页面职责、实现计划
- `docs/stitch-guidance/`：给 Stitch / 视觉生成使用的设计约束
- `.sisyphus/decisions/`：业务决策文档
- `.sisyphus/plans/`：历史执行计划与一次性实现方案
- `design/stitch_extracted/`：Stitch 产出的设计结果参考

## 4. 当前架构现实

修改前先接受以下现实，而不是按想象中的“更现代架构”做：

- 当前应用以 **Fragment + XML + 手工导航/切换** 为主，不要默认项目已经接入 Navigation Component。
- 当前数据层是 **SQLite + `DatabaseHelper`**，不要默认已经迁移到 Room。
- README 和规划文档里出现的目标架构，不等于当前代码已经实现。
- 若只是在修 bug 或补局部功能，优先沿用现有模式，避免顺手做大规模架构迁移。

## 5. 修改原则

### 通用原则

- 先验证再修改；先读受影响文件簇，不要只看单文件。
- 优先做 **最小必要改动**，尤其是 bugfix。
- 不要把“计划中的新产品结构”混入一个本应局部修复的小任务里。
- 不要用注释、文档或 README 替代真实实现。
- 不要编造未读代码的行为。

### Kotlin / Android 约束

- 延续现有命名方式：页面使用 `*Fragment.kt`，适配器在 `adapter/`，模型在 `model/`，工具在 `util/`。
- 布局命名遵循现有模式：`fragment_*.xml`、`item_*.xml`、`dialog_*.xml`。
- 颜色、尺寸、样式优先落到 `res/values/` 的资源文件中，避免把设计 token 再次硬编码进布局或 Kotlin。
- 涉及主题、卡片、排版等 UI 变更时，优先复用已有资源文件，例如：
  - `colors_light.xml`
  - `colors_dark.xml`
  - `colors_gradients.xml`
  - `styles.xml`
  - `styles_cards.xml`
  - `styles_typography.xml`
  - `dimens.xml`

### 业务规则约束

如果修改以下行为，必须先对照 `.sisyphus/decisions/business_rules.md`：

- 删除记录与资产余额回滚
- 分类统计口径
- Agent 操作审计要求
- 离线模式假设
- 记录分页 / 时间范围加载策略

如果代码与文档冲突，先明确当前代码行为，再在修改说明中指出冲突，不要默默“顺手修正”规则。

## 6. 视觉与产品气质约束

CardTally 当前明确的产品气质是：**静奢理财日记**。

进行 UI / 视觉调整时，应优先符合这些方向：

- 安静、不喧嚣、无压迫感
- 高级感、品质感、克制的细节
- 纯净留白，不做拥挤 dashboard
- 有温度，但不要过度卡通化

明确避免：

- 高饱和主色大面积铺陈
- 数据密集型后台风 UI
- 模板化记账 App 观感
- 过重的阴影、分隔线和装饰

如果是视觉工作，先读 `docs/stitch-guidance/README.md` 及其关联文件，再动资源与布局。

## 7. 命令与验证

优先使用仓库自带 wrapper。

### Windows

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat clean
```

### macOS / Linux

```bash
./gradlew assembleDebug
./gradlew clean
```

### 产物位置

`app/build/outputs/apk/debug/CardTally-debug.apk`

### 测试现状

- `app/build.gradle` 已配置 JUnit4、AndroidX Test、Espresso 依赖。
- 当前仓库中 **未发现** `app/src/test/` 或 `app/src/androidTest/` 下的实际测试文件。
- 这意味着很多改动最终只能依靠构建通过 + 受影响路径的人工验证。

## 8. 做任务时怎么选入口

### 改首页 / 记录 / 统计 / 设置等页面

先读对应 Fragment，再读关联布局、Adapter、资源文件。

### 改数据库或数据展示

先读：

1. `DatabaseHelper.kt`
2. 相关 `model/*.kt`
3. 发起查询或渲染数据的 Fragment / Adapter

### 改主题 / 样式 / 视觉一致性

先读：

1. `util/ThemeHelper.kt`
2. `res/values/*.xml`
3. 受影响页面布局
4. `docs/stitch-guidance/*`

### 改产品结构 / 新页面框架

先读：

1. `docs/plans/2026-03-26-cardtally-implementation-plan.md`
2. 相关页面职责和 IA 文档
3. 当前旧页面实现

不要直接按旧页面一比一延续；这个仓库已经明确存在“旧结构”到“新骨架”的过渡阶段。

## 9. 文档可信度排序

当多个来源冲突时，默认按这个顺序判断：

1. **当前源码与 Gradle 配置**
2. `.sisyphus/decisions/*.md` 中的明确业务规则
3. `docs/plans/*.md` 中已确认的产品/结构方向
4. `docs/stitch-guidance/*.md` 中的视觉约束
5. `README.md`
6. `.sisyphus/plans/*.md`、`design/stitch_extracted/*` 等历史 / 过程性文件

## 10. 提交前最低自检

如果你修改了代码或资源，而不是只改文档：

1. 重新阅读所有改动文件，确认风格一致
2. 执行 `assembleDebug`
3. 检查受影响页面是否还符合“静奢理财日记”方向
4. 如果动了业务规则，核对 `.sisyphus/decisions/business_rules.md`
5. 如果动了视觉设计，核对 `docs/stitch-guidance/*`

如果你只修改文档：

- 保证文档内容与仓库现状一致
- 不要把愿景文档写成当前已实现事实

## 11. 对 AI 的最终要求

- 先基于仓库事实工作，再基于愿景文档规划
- 先最小化破坏，再追求理想结构
- 任何不确定的行为，都必须通过读代码确认
- 在这个仓库里，**“规划中” 与 “已实现” 是两回事**，不要混淆
