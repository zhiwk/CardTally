# Project Overview

## 项目定位

CardTally 是一个原生 Android 记账应用，当前仓库中的工程事实如下：

- 语言：Kotlin
- 构建：Gradle
- Android Gradle Plugin：`8.3.0`
- Gradle Wrapper：`8.13`
- 模块：单模块 `:app`
- 最低 SDK：24
- 目标 / 编译 SDK：34
- UI：XML 布局 + Material Components + Fragment
- 数据层：SQLite，核心入口为 `DatabaseHelper.kt`
- 国际化：应用级 locale 切换，当前支持中文与英文，入口在 `SettingsFragment`

> 修改时应优先相信仓库中的构建脚本和源码，不要拿 README 或规划文档替代代码事实。

## 仓库地图

### 代码

- `app/src/main/java/com/example/cardtally/`
- `MainActivity.kt`：主 Activity
- `*Fragment.kt`：页面级逻辑，例如 `HomeFragment`、`AddRecordFragment`、`EditRecordFragment`、`AddAssetFragment`、`EditAssetFragment`、`StatisticsFragment`、`SettingsFragment`、`SearchFragment`、`AgentFragment`、`AssetFragment`；其中当前“账本”一级页仍由 `StatisticsFragment` 承载，并在页内切换 `统计 / 明细` 两种视图
- `adapter/`：RecyclerView 相关适配器
- `database/`：SQLite 数据访问，核心在 `DatabaseHelper.kt`
- `model/`：数据模型，包括记录、资产，以及 AI 助手会话 / 消息模型
- `network/`：网络请求与 MiniMax BYOK 聊天相关客户端逻辑
- `util/`：工具类，例如主题、快捷记账、列表交互辅助、AI 设置与会话默认命名辅助，以及主题颜色解析 / 底部导航 FAB 定位辅助

### 资源

- `app/src/main/res/layout/`：页面和列表项布局
- `app/src/main/res/values/`：颜色、尺寸、样式、字符串等设计系统资源
- `app/src/main/res/drawable/`：图标、背景、形状
- `app/src/main/res/menu/`：导航与菜单

### 文档与规划

- `docs/requirements/plans/`：产品重开、信息架构、实现计划
- `docs/design/guidelines/`：视觉和 Stitch 设计约束
- `docs/requirements/decisions/`：业务规则决策文档
- `docs/archive/`：历史执行计划与归档草案
- `docs/design/assets/stitch/`：Stitch 导出参考

## 当前架构现实

修改前先接受这些现实：

- 当前应用以 `Fragment + XML + 手工导航/切换` 为主，不要默认已经接入 Navigation Component。
- 当前数据层是 `SQLite + DatabaseHelper`，不要默认已经迁移到 Room。
- 当前语言切换基于应用级 locale，不要再额外引入第二套手写国际化状态。
- 当前 AI 助手已经不是静态示例页，而是带 SQLite 持久化多会话的 MiniMax BYOK 聊天页；不要再按“单会话内存态”理解 `AgentFragment`。
- 当前主题设置现实是 `浅色 / 深色 / 跟随系统` 三档，不要再按旧的蓝 / 绿 / 橙彩色主题理解设置页。
- 当前深色模式适配依赖 `ThemeHelper`、主题属性与 `values-night` 覆盖；不要继续往布局和 Kotlin 里新增 `*_light` 直接引用。
- 当前底部导航由 `MainActivity` 统一控制一级 / 二级页显隐，不要在各个二级页里继续各自维护一套 hide/show 规则；`AgentFragment` 的会话抽屉显隐也应通过 `MainActivity` 的导航壳控制链路协同。
- README 和规划文档里出现的目标架构，不等于当前代码已经实现。
- 若只是在修 bug 或补局部功能，优先沿用现有模式，避免顺手做大规模架构迁移。
