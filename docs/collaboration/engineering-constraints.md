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
  - `colors_gradients.xml`
  - `styles.xml`
  - `styles_cards.xml`
  - `styles_typography.xml`
  - `dimens.xml`

## 业务规则约束

如果修改以下行为，必须先对照 `.sisyphus/decisions/business_rules.md`：

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

如果是视觉工作，先读 `docs/stitch-guidance/README.md` 及其关联文件，再动资源与布局。

## 命令与验证

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
- 当前仓库中未发现 `app/src/test/` 或 `app/src/androidTest/` 下的实际测试文件。
- 这意味着很多改动最终只能依靠构建通过和受影响路径的人工验证。

## 文档可信度排序

当多个来源冲突时，默认按这个顺序判断：

1. 当前源码与 Gradle 配置
2. `.sisyphus/decisions/*.md` 中的明确业务规则
3. `docs/plans/*.md` 中已确认的产品/结构方向
4. `docs/stitch-guidance/*.md` 中的视觉约束
5. `README.md`
6. `.sisyphus/plans/*.md`、`design/stitch_extracted/*` 等历史 / 过程性文件

## 提交前最低自检

如果修改了代码或资源，而不是只改文档：

1. 重新阅读所有改动文件，确认风格一致
2. 执行 `assembleDebug`
3. 检查受影响页面是否还符合“静奢理财日记”方向
4. 如果动了业务规则，核对 `.sisyphus/decisions/business_rules.md`
5. 如果动了视觉设计，核对 `docs/stitch-guidance/*`

如果只修改文档：

- 保证文档内容与仓库现状一致
- 不要把愿景文档写成当前已实现事实
