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
  - `styles.xml`
  - `styles_cards.xml`
  - `styles_typography.xml`
  - `dimens.xml`
- 当前视觉方向以根目录 `DESIGN.md` 为准；不要恢复已废弃的旧主题或旧设计语言。
- 当前支持浅色、深色和跟随系统，默认浅色；视觉调整以 `DESIGN.md`、2026-10-03 外观决策和实际资源为准，沿用中性卡片体系。

## 业务规则约束

如果修改以下行为，必须先对照 `docs/requirements/decisions/business_rules.md`：

- 删除记录与资产余额回滚
- 分类统计口径
- Agent 操作审计要求
- 离线模式假设
- 记录分页 / 时间范围加载策略

如果代码与文档冲突，先明确当前代码行为，再在修改说明中指出冲突，不要默默“顺手修正”规则。

## 视觉与产品气质约束

CardTally 当前视觉与交互方向以根目录 `DESIGN.md` 为准。

如果是视觉工作，先读 `DESIGN.md` 及当前页面对应的资源，再动布局或代码。

## 命令与验证

优先使用仓库自带 wrapper。

### Windows

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat :app:connectedVerificationDebugAndroidTest
```

### macOS / Linux

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew :app:connectedVerificationDebugAndroidTest
```

### 产物位置

`app/build/outputs/apk/debug/app-debug.apk`

### 测试现状

- `app/build.gradle` 已配置 JUnit4、AndroidX Test、Espresso 依赖。
- 当前仓库已存在实际测试文件，包括：
  - `app/src/test/java/com/example/cardtally/util/AgentSessionTitleHelperTest.kt`
  - `app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperAgentChatSessionTest.kt`
  - `app/src/androidTest/java/com/example/cardtally/util/AiAssistantSettingsHelperTest.kt`
  - `app/src/androidTest/java/com/example/cardtally/MainActivityThemeApplicationTest.kt`
  - `app/src/androidTest/java/com/example/cardtally/util/ThemeHelperTest.kt`
- 用户明确要求验证时，按改动范围选择相关单测、设备测试或 `assembleDebug`；涉及 AI 助手多会话时可选 `AgentSessionTitleHelperTest` / `DatabaseHelperAgentChatSessionTest`，涉及主题时可选 `ThemeHelperTest` / `MainActivityThemeApplicationTest`。不要因代码修改自动启动这些检查。
- 默认不执行 Android Gradle 构建或测试；用户明确要求后，只运行其指定范围。同一工作区内的 Gradle 命令仍必须串行，尤其不要并行运行共享 `app/build/` 产物的任务。详见 `docs/collaboration/skills/android-gradle-serial-verification.md`。

## 文档可信度排序

当多个来源冲突时，默认按这个顺序判断：

1. 当前源码与 Gradle 配置
2. `docs/requirements/decisions/*.md` 中的明确业务规则
3. `docs/requirements/plans/*.md` 中已确认的产品/结构方向
4. `DESIGN.md` 中的视觉约束
5. `README.md`
6. `docs/archive/*.md` 等历史 / 过程性文件

## 提交前最低自检

如果修改了代码或资源，而不是只改文档：

1. 重新阅读所有改动文件，确认风格一致
2. 仅当用户明确要求构建时，执行 `assembleDebug`；否则不构建
3. 检查受影响页面是否符合 `DESIGN.md`
4. 如果动了业务规则，核对 `docs/requirements/decisions/business_rules.md`
5. 如果动了视觉设计，核对 `DESIGN.md`

如果只修改文档：

- 保证文档内容与仓库现状一致
- 不要把愿景文档写成当前已实现事实
