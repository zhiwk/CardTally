# Task Entrypoints

## 所有任务都先读

1. `README.md`
2. `AGENTS.md`
3. `docs/agent-guide/README.md`

## 涉及业务规则 / 数据逻辑时再读

1. `.sisyphus/decisions/business_rules.md`
2. `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`
3. 受影响页面对应的 Fragment / Adapter / Model

## 涉及页面重构 / 信息架构 / 首版产品方向时再读

1. `docs/plans/2026-03-26-cardtally-implementation-plan.md`
2. `docs/plans/2026-03-26-cardtally-product-restart-design.md`
3. `docs/plans/*.md`

## 涉及视觉设计 / UI 风格 / Stitch 设计稿时再读

1. `docs/stitch-guidance/README.md`
2. `docs/stitch-guidance/brand-design-guide.md`
3. `docs/stitch-guidance/visual-design-guide.md`
4. `docs/stitch-guidance/page-design-guide.md`
5. `docs/stitch-guidance/home-page-spec.md`
6. 必要时查看 `design/stitch_extracted/`

## 涉及历史计划 / 过程文档时再读

- `.sisyphus/plans/*.md`
- `design/stitch_extracted/`

这些文件可用于理解历史执行意图、阶段性方案或外部设计产物，但不能直接当成当前代码现状的权威来源，必须回到代码与高可信文档验证。

## 常见任务入口

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

### 改文案 / 国际化 / 语言切换

先读：

1. `app/src/main/java/com/example/cardtally/util/LanguageHelper.kt`
2. `app/src/main/res/values/strings.xml`
3. `app/src/main/res/values-en/strings.xml`
4. 受影响页面对应的 `Fragment` 和 `layout`
5. 如涉及设置入口，再读 `SettingsFragment.kt` 和 `fragment_settings.xml`

### 改产品结构 / 新页面框架

先读：

1. `docs/plans/2026-03-26-cardtally-implementation-plan.md`
2. 相关页面职责和 IA 文档
3. 当前旧页面实现

不要直接按旧页面一比一延续；这个仓库仍处于“旧结构”向“新骨架”过渡阶段。
