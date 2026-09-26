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
- `*Fragment.kt`：页面级逻辑；当前底部导航的账本、统计、资产、AI 助手、我的分别由 `LedgerFragment`、`StatisticsFragment`、`AssetFragment`、`AgentFragment`、`SettingsFragment` 承载
- `adapter/`：RecyclerView 相关适配器；当前除通用列表外，也包含录入页抽屉相关适配器，如 `RecordAssetSheetAdapter`、`RecordCategoryTreeAdapter`
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
- `DESIGN.md`：当前视觉和交互设计约束
- `docs/requirements/decisions/`：业务规则决策文档
- `docs/archive/`：历史执行计划与归档草案
- `docs/design/assets/primitive-showcase/`：当前设计原语与状态展示

## 当前架构现实

修改前先接受这些现实：

- 当前应用以 `Fragment + XML + 手工导航/切换` 为主，不要默认已经接入 Navigation Component。
- 当前数据层是 `SQLite + DatabaseHelper`，不要默认已经迁移到 Room。
- 当前语言切换基于应用级 locale，不要再额外引入第二套手写国际化状态。
- 当前 AI 助手已经不是静态示例页，而是带 SQLite 持久化多会话的 MiniMax BYOK 聊天页；不要再按“单会话内存态”理解 `AgentFragment`。
- 当前运行时只使用浅色外观；`ThemeHelper` 会把旧主题偏好归一为浅色，视觉约束以根目录 `DESIGN.md` 为准。
- 当前底部导航由 `MainActivity` 统一控制一级 / 二级页显隐，不要在各个二级页里继续各自维护一套 hide/show 规则；`AgentFragment` 的会话抽屉显隐也应通过 `MainActivity` 的导航壳控制链路协同。
- 当前新增与编辑记录共用 `fragment_add_record_quick.xml`；日期和资产使用底部选择器，分类在页内按标准或快速模式选择，金额键盘常驻。
- README 和规划文档里出现的目标架构，不等于当前代码已经实现。
- 若只是在修 bug 或补局部功能，优先沿用现有模式，避免顺手做大规模架构迁移。
