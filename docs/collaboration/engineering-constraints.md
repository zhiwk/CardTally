# Engineering Constraints

## 通用原则

- 先验证再修改；先读受影响文件簇，不要只看单文件。
- 优先做最小必要改动，尤其是 bugfix。
- 不要把“计划中的新产品结构”混入一个本应局部修复的小任务里。
- 不要用注释、文档或 README 替代真实实现。
- 不要编造未读代码的行为。

## Kotlin / Android 约束

- 延续现有命名方式：页面使用 `*Fragment.kt`，适配器在 `adapter/`，模型在 `model/`，工具在 `util/`。
- 布局命名遵循现有模式：`fragment_*.xml`、`item_*.xml`、`dialog_*.xml`。
- 颜色、尺寸、样式优先落到 `res/values/` 资源文件中，避免把设计 token 硬编码进布局或 Kotlin。
- 涉及主题、卡片、排版等 UI 变更时，优先复用已有资源文件，例如：
  - `colors_light.xml`
  - `colors_dark.xml`
  - `values-night/*.xml`
  - `styles.xml`
  - `styles_cards.xml`
  - `styles_typography.xml`
  - `dimens.xml`
- 当前主题设置只保留 `浅色 / 深色 / 跟随系统` 三档；不要再新增或恢复蓝 / 绿 / 橙彩色主题分支。
- 当前深色模式修复策略是：优先使用主题属性（如 `?attr/colorOnSurface`），必要时通过 `values-night` 做兼容覆盖；不要继续新增 `@color/*_light` 或 `R.color.*_light` 直接引用。

## 业务规则约束

如果修改以下行为，必须先对照 `docs/requirements/decisions/business_rules.md`：

- 删除记录与资产余额回滚
- 分类统计口径
- Agent 操作审计要求
- 离线模式假设
- 记录分页 / 时间范围加载策略

如果代码与文档冲突，先明确当前代码行为，再在修改说明中指出冲突，不要默默“顺手修正”规则。

## 视觉与产品气质约束

CardTally 当前明确的产品气质是：`静奢理财日记`。

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

如果是视觉工作，先读 `docs/design/guidelines/README.md` 及其关联文件，再动资源与布局。

## 命令与验证

优先使用仓库自带 wrapper。

### Windows

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat clean
```

### macOS / Linux

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew clean
```

### 产物位置

`app/build/outputs/apk/debug/CardTally-debug.apk`

### 测试现状

- `app/build.gradle` 已配置 JUnit4、AndroidX Test、Espresso 依赖。
- 当前仓库已存在实际测试文件，包括：
  - `app/src/test/java/com/example/cardtally/util/AgentSessionTitleHelperTest.kt`
  - `app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperAgentChatSessionTest.kt`
  - `app/src/androidTest/java/com/example/cardtally/util/AiAssistantSettingsHelperTest.kt`
  - `app/src/androidTest/java/com/example/cardtally/MainActivityThemeApplicationTest.kt`
  - `app/src/androidTest/java/com/example/cardtally/util/ThemeHelperTest.kt`
- 涉及 AI 助手多会话持久化时，优先先跑相关单测 / 真机测试，再补 `assembleDebug` 与人工验证。
- 涉及主题 / 深色模式修复时，优先先跑 `ThemeHelperTest`、`MainActivityThemeApplicationTest` 这类主题回归，再补 `assembleDebug` 与人工验证。
- 同一工作区内执行 Android Gradle 验证时，默认串行运行 `assembleDebug`、`testDebugUnitTest`、`connectedDebugAndroidTest`；不要并行跑共享 `app/build/` 产物的任务。详见 `docs/collaboration/skills/android-gradle-serial-verification.md`

## 文档可信度排序

当多个来源冲突时，默认按这个顺序判断：

1. 当前源码与 Gradle 配置
2. `docs/requirements/decisions/*.md` 中的明确业务规则
3. `docs/requirements/plans/*.md` 中已确认的产品/结构方向
4. `docs/design/guidelines/*.md` 中的视觉约束
5. `README.md`
6. `docs/archive/*.md`、`docs/design/assets/stitch/*` 等历史 / 过程性文件

## 提交前最低自检

如果修改了代码或资源，而不是只改文档：

1. 重新阅读所有改动文件，确认风格一致
2. 执行 `assembleDebug`
3. 检查受影响页面是否还符合“静奢理财日记”方向
4. 如果动了业务规则，核对 `docs/requirements/decisions/business_rules.md`
5. 如果动了视觉设计，核对 `docs/design/guidelines/*`

如果只修改文档：

- 保证文档内容与仓库现状一致
- 不要把愿景文档写成当前已实现事实
