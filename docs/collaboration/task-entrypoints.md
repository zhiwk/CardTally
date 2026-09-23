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

## 涉及视觉设计 / UI 风格时再读

1. `DESIGN.md`
2. 当前页面对应的布局、Fragment、Adapter 和资源文件

## 涉及历史计划 / 过程文档时再读

- `docs/requirements/plans/*.md`
这些文件可用于理解历史执行意图或阶段性方案，但不能直接当成当前代码现状的权威来源，必须回到代码与高可信文档验证。

## 常见任务入口

### 改账本 / 记录 / 设置等页面

先读对应 Fragment，再读关联布局、Adapter、资源文件。

### 改“记一笔” / “编辑记录”录入页

先读：

1. `app/src/main/java/com/example/cardtally/AddRecordFragment.kt`
2. `app/src/main/java/com/example/cardtally/EditRecordFragment.kt`
3. `app/src/main/res/layout/fragment_add_record_quick.xml`（当前两种模式共用的录入壳）
4. `app/src/main/java/com/example/cardtally/adapter/RecordAssetSheetAdapter.kt`
5. `app/src/main/java/com/example/cardtally/adapter/RecordCategoryTreeAdapter.kt`
6. `app/src/main/res/layout/bottom_sheet_record_*.xml`
7. `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`

注意：两种模式共用 `fragment_add_record_quick.xml`，区别在页内分类列表；金额键盘常驻，日期和资产仍使用底部选择器。

### 改重复记账 / 转账资产选择

先读：

1. `app/src/main/java/com/example/cardtally/RecurringRecordEditFragment.kt`
2. `app/src/main/java/com/example/cardtally/RecurringRecordsFragment.kt`
3. `app/src/main/java/com/example/cardtally/RecordAssetPickerBottomSheetFragment.kt`
4. `app/src/main/java/com/example/cardtally/AssetFragment.kt`
5. `app/src/main/java/com/example/cardtally/adapter/AssetAdapter.kt`
6. `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`
7. `docs/requirements/decisions/business_rules.md`

注意：普通「记一笔」和重复记账共用资产选择器，通过 `FragmentResult` 回传资产 ID 与是否为转入资产；分别验证两个宿主的结果处理及弹层关闭行为。任务账本通过 `pickerLedgerId` 限制资产列表，不应改变当前账本。默认资产设置使用独立的 `DefaultRecordAssetPickerBottomSheetFragment` 结果契约。

### 改数据库或数据展示

先读：

1. `DatabaseHelper.kt`
2. 相关 `model/*.kt`
3. 发起查询或渲染数据的 Fragment / Adapter

数据协作者边界：

- 记录行映射：`database/RecordSqlMapper.kt`
- 重复记账模板读写：`database/RecurringRecordRepository.kt`
- 分类只读查询及路径：`database/CategoryReadRepository.kt`
- 分类写入与排序：`database/CategoryWriteRepository.kt`
- 分类父子关系校验：`database/CategoryHierarchyValidator.kt`
- 分类树展示顺序：`database/CategoryTreeOrdering.kt`

这些类由 `DatabaseHelper` 保持兼容门面调用；Schema、资产／账本写入、记录余额事务、统计聚合和 AI 持久化仍在 `DatabaseHelper`，后续职责拆分需分别保持原事务与作用域语义。

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

注意：当前仓库内 Gradle 构建与测试默认串行；安装前先确认 `app/build/outputs/apk/debug/app-debug.apk` 已生成。

注意：`verification` flavor 会把变体名拆开，日常命令写作 `:app:assembleDebug` / `:app:testDebugUnitTest` 仍可用（同 `:app:assembleEverydayDebug` / `:app:testEverydayUnitTest`）；会重置数据库的 device 测试只能跑 `:app:connectedVerificationDebugAndroidTest`，它以 `.verification` applicationId 安装并与日常数据隔离。项目只保留 `dev`、`verification` flavor，release 通过 build type 使用 `.release` applicationId。

### 改主题 / 样式 / 视觉一致性

先读：

1. `util/ThemeHelper.kt`
2. `ThemeSettingsFragment.kt` 与 `fragment_theme_settings.xml`
3. `res/values/*.xml` 与 `res/values-night/*.xml`
4. `util/ThemeColorHelper.kt`（如果涉及 Kotlin 运行时取色）
5. 受影响页面布局 / Adapter / Fragment
6. `DESIGN.md`

注意：当前视觉方向以 `DESIGN.md` 为准，不要继续沿用已删除的历史设计指南或 Stitch 导出。

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
