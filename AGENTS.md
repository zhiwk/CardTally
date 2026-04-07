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

## 2. 核心原则

- 先基于源码和 Gradle 配置工作，再参考 README、规划文档和设计文档。
- 当前仓库现实是 `Fragment + XML + SQLite(DatabaseHelper)`，不要默认已经接入 Room 或 Navigation Component。
- 当前仓库已接入应用级中英文国际化；涉及文案、语言切换或字符串资源时，先检查 `LanguageHelper` 和 `res/values*/strings*.xml`。
- 修改优先做最小必要改动，尤其是 bugfix 和局部功能补全。
- 涉及业务规则时，必须先对照 `docs/requirements/decisions/business_rules.md`。
- 涉及视觉改动时，必须先对照 `docs/design/guidelines/*`，并保持"静奢理财日记"方向。
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
└── archive/           # 历史归档
```

本文件保持为入口索引。
