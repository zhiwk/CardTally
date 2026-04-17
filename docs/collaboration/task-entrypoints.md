# Task Entrypoints

## 所有任务都先读

1. `README.md`
2. `AGENTS.md`
3. `docs/collaboration/README.md`

## 涉及业务规则 / 数据逻辑时再读

1. `docs/requirements/decisions/business_rules.md`
2. `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`
3. 受影响页面对应的 Fragment / Adapter / Model

## 涉及页面重构 / 信息架构 / 首版产品方向时再读

1. `docs/requirements/plans/2026-03-26-cardtally-implementation-plan.md`
2. `docs/requirements/plans/2026-03-26-cardtally-product-restart-design.md`
3. `docs/requirements/plans/*.md`

## 涉及视觉设计 / UI 风格 / Stitch 设计稿时再读

1. `docs/design/guidelines/README.md`
2. `docs/design/guidelines/brand-design-guide.md`
3. `docs/design/guidelines/visual-design-guide.md`
4. `docs/design/guidelines/page-design-guide.md`
5. `docs/design/guidelines/home-page-spec.md`
6. 必要时查看 `docs/design/assets/stitch/`

## 涉及历史计划 / 过程文档时再读

- `docs/requirements/plans/*.md`
- `docs/design/assets/stitch/`

这些文件可用于理解历史执行意图、阶段性方案或外部设计产物，但不能直接当成当前代码现状的权威来源，必须回到代码与高可信文档验证。

## 常见任务入口

### 改首页 / 记录 / 统计 / 设置等页面

先读对应 Fragment，再读关联布局、Adapter、资源文件。

### 改“记一笔” / “编辑记录”录入页

先读：

1. `app/src/main/java/com/example/cardtally/AddRecordFragment.kt`
2. `app/src/main/java/com/example/cardtally/EditRecordFragment.kt`
3. `app/src/main/res/layout/fragment_add_record.xml`
4. `app/src/main/java/com/example/cardtally/adapter/RecordAssetSheetAdapter.kt`
5. `app/src/main/java/com/example/cardtally/adapter/RecordCategoryTreeAdapter.kt`
6. `app/src/main/res/layout/bottom_sheet_record_*.xml`
7. `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`

注意：当前录入页现实已经是“主页面极简壳 + 底部抽屉交互”，不要按旧的 `Spinner + 分类网格` 页面假设继续改。

### 改数据库或数据展示

先读：

1. `DatabaseHelper.kt`
2. 相关 `model/*.kt`
3. 发起查询或渲染数据的 Fragment / Adapter

### 改 AI 助手 / MiniMax 对话 / 会话持久化

先读：

1. `app/src/main/java/com/example/cardtally/AgentFragment.kt`
2. `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`
3. `app/src/main/java/com/example/cardtally/adapter/AgentChatAdapter.kt`
4. `app/src/main/java/com/example/cardtally/adapter/AgentSessionAdapter.kt`
5. `app/src/main/java/com/example/cardtally/model/AiChatMessage.kt`
6. `app/src/main/java/com/example/cardtally/model/AiChatSession.kt`
7. `app/src/main/java/com/example/cardtally/util/AiAssistantSettingsHelper.kt`
8. `app/src/main/java/com/example/cardtally/network/MiniMaxClient.kt`
9. `app/src/main/res/layout/fragment_agent.xml`
10. `docs/collaboration/skills/android-gradle-serial-verification.md`（如果需要跑构建 / 单测 / 真机验证）

### 做真机截图 / 页面取证 / UI 回归留档

先读：

1. `skills/adb-current-screen-screenshot.md`
2. 如同时涉及构建或测试，再读 `docs/collaboration/skills/android-gradle-serial-verification.md`

注意：当前仓库推荐先用 `adb devices` 确认设备状态；如果有多台设备，后续截图命令必须带 `-s <serial>`。如果设备提示存在多个 display，或默认截图出现黑图，先执行 `adb shell dumpsys SurfaceFlinger --display-id`，再改用 `screencap -d <display-id>`；需要留档时默认保存到根目录 `screenshot/`。

### 做 Debug 编译或 APK 安装

先读：

1. `skills/android-build-debug.md`
2. `skills/android-install-debug-apk.md`
3. 如同时涉及测试或整轮验证，再读 `docs/collaboration/skills/android-gradle-serial-verification.md`

注意：当前仓库内 Gradle 构建与测试默认串行；安装前先确认 `app/build/outputs/apk/debug/CardTally-debug.apk` 已生成。

### 改主题 / 样式 / 视觉一致性

先读：

1. `util/ThemeHelper.kt`
2. `ThemeSettingsFragment.kt` 与 `fragment_theme_settings.xml`
3. `res/values/*.xml` 与 `res/values-night/*.xml`
4. `util/ThemeColorHelper.kt`（如果涉及 Kotlin 运行时取色）
5. 受影响页面布局 / Adapter / Fragment
6. `docs/design/guidelines/*`

注意：当前仓库主题现实只有 `浅色 / 深色 / 跟随系统` 三档；如果看到 `*_light` 直接引用，默认应视为待迁移对象，而不是可继续沿用的模式。

### 改文案 / 国际化 / 语言切换

先读：

1. `app/src/main/java/com/example/cardtally/util/LanguageHelper.kt`
2. `app/src/main/res/values/strings.xml`
3. `app/src/main/res/values-en/strings.xml`
4. 受影响页面对应的 `Fragment` 和 `layout`
5. 如涉及设置入口，再读 `SettingsFragment.kt` 和 `fragment_settings.xml`

补充现实：

- 语言切换入口在“我的”页 `SettingsFragment`
- 当前已做过一轮减闪处理，但真机切换中英文仍会轻微闪屏
- 后续目标是更平滑的淡入淡出过渡，而不是维持当前闪动效果
- 如需继续排查，连同 `MainActivity`、`activity_main.xml` 与主题 / 窗口动画资源一起看

### 改产品结构 / 新页面框架

先读：

1. `docs/requirements/plans/2026-03-26-cardtally-implementation-plan.md`
2. 相关页面职责和 IA 文档
3. 当前旧页面实现

不要直接按旧页面一比一延续；这个仓库仍处于“旧结构”向“新骨架”过渡阶段。
