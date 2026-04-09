# AGENTS.md

CardTally 仓库的 AI 协作入口文件。开始任何修改前，先读本文件，再按任务类型继续读 `docs/collaboration/` 下的拆分文档。

## 1. 使用方式

所有任务都先读：

1. `README.md`
2. 本文件 `AGENTS.md`
3. `docs/collaboration/README.md`

然后按任务类型继续读对应文档：

- 项目事实 / 仓库地图：`docs/collaboration/project-overview.md`
- 任务入口 / 先读什么：`docs/collaboration/task-entrypoints.md`
- 工程约束 / 视觉约束 / 提交前检查：`docs/collaboration/engineering-constraints.md`
- 最近代码现实 / 当日快照：`docs/collaboration/current-snapshot.md`
- Android 构建 / 单测 / 真机验证：`docs/collaboration/skills/android-gradle-serial-verification.md`

## 2. 核心原则

- 先基于源码和 Gradle 配置工作，再参考 README、规划文档和设计文档。
- 当前仓库现实是 `Fragment + XML + SQLite(DatabaseHelper)`，不要默认已经接入 Room 或 Navigation Component。
- 当前录入页复用现实：`AddRecordFragment` / `EditRecordFragment` 共享 `fragment_add_record.xml`；`AddAssetFragment` / `EditAssetFragment` 共享 `fragment_add_asset.xml`。
- 当前 AI 助手现实：`AgentFragment` 已接入 MiniMax BYOK 流式文本聊天，请求默认携带 `stream=true`，并会按实际响应内容识别流式 / 非流式返回；当前已支持增量渲染回复，以及区分 `TIMEOUT / CANCELLED / INTERRUPTED / NETWORK` 等失败态并在已有内容时保留部分回复。当前 AI 助手会话与消息已持久化到 SQLite，可跨页面切换与重启保留；`AgentSessionAdapter` / `AiChatSession` / `DatabaseHelper` 共同支撑多会话切换、新建与重命名。`AiAssistantSettingsFragment` / `AiAssistantSettingsHelper` 负责本机保存 API Key、模型、完整请求 URL 与当前活动会话 ID。
- 当前主题现实：主题设置当前只保留 `浅色 / 深色 / 跟随系统` 三档；历史蓝 / 绿 / 橙彩色主题已从设置入口与资源层移除，`ThemeHelper` 会把旧的彩色主题存档值回退到浅色主题。当前仓库正在以主题属性和 `values-night` 覆盖替换旧的 `*_light` 直接引用；涉及主题/深色模式问题时，优先检查 `ThemeHelper`、`ThemeSettingsFragment`、`styles.xml`、`values-night/*.xml` 与受影响布局/适配器。
- 当前导航现实：`MainActivity` 统一持有浮动底部导航壳；一级页显示，进入二级页面（add/edit/settings detail 等）后隐藏整个 `nav_shell`，不要再在单个 Fragment 中分散维护导航显隐规则。
- 当前仓库已接入应用级中英文国际化；涉及文案、语言切换或字符串资源时，先检查 `LanguageHelper` 和 `res/values*/strings*.xml`。
- 修改优先做最小必要改动，尤其是 bugfix 和局部功能补全。
- 同一工作区内执行 Android Gradle 验证时，默认串行运行 `assembleDebug`、`testDebugUnitTest`、`connectedDebugAndroidTest` 等命令；不要并行跑共享 `app/build/` 产物的任务，避免出现 `Tool execution aborted` 或中间产物互相踩踏导致误判。
- 涉及业务规则时，必须先对照 `docs/requirements/decisions/business_rules.md`。
- 涉及视觉改动时，必须先对照 `docs/design/guidelines/*`，并保持"静奢理财日记"方向。
- 文档目录已统一到当前结构；不要再引用旧路径别名，例如 `docs/agent-guide/*`、`docs/stitch-guidance/*`、`.sisyphus/*`、`design/stitch_extracted/*`。
- `local.properties` 是本机环境文件，不要提交。

## 3. 文档可信度排序

当多个来源冲突时，默认按这个顺序判断：

1. 当前源码与 Gradle 配置
2. `docs/requirements/decisions/*.md`
3. `docs/requirements/plans/*.md`
4. `docs/design/guidelines/*.md`
5. `README.md`
6. `docs/archive/*`、`docs/design/assets/*`

## 4. 最低自检

如果修改了代码或资源，而不是只改文档：

1. 重新阅读所有改动文件，确认风格一致
2. 执行 `assembleDebug`
3. 检查受影响页面是否仍符合"静奢理财日记"方向
4. 如果动了业务规则，核对 `docs/requirements/decisions/business_rules.md`
5. 如果动了视觉设计，核对 `docs/design/guidelines/*`

如果只修改文档：

- 保证文档内容与仓库现状一致
- 不要把愿景文档写成当前已实现事实

## 5. 文档目录

```
docs/
├── requirements/      # 需求文档
│   ├── plans/        # 实施计划
│   └── decisions/    # 业务决策
├── design/            # 设计文档
│   ├── guidelines/   # 设计指南
│   └── assets/       # 设计产出物
├── collaboration/     # AI协作指南
│   └── skills/       # 专项协作 skill
└── archive/           # 历史归档
```

本文件保持为入口索引。
