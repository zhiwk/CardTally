# CardTally 稳定操作入口

## 1 定位与使用方式

CardTally 是一款本地优先的原生 Android 记账与财务陪伴应用。本文件是 CardTally 的 OPC（一人公司）稳定操作入口，用于约束长期有效的判断、协作和交付方式。

开始任何任务前，按以下顺序阅读：

1. `README.md`
2. 本文件 `AGENTS.md`
3. `docs/collaboration/README.md`

随后按任务类型进入 `docs/collaboration/task-entrypoints.md`。它负责说明页面、数据、视觉、AI、构建和真机任务应先读哪些文件。近期且易变的实现状态只看 `docs/collaboration/current-snapshot.md`，不要写回本文件。

### 交接提示

- 新接手任务前，必须先阅读 `docs/collaboration/current-snapshot.md`，再回到源码核对近期实现；快照不是替代源码的事实来源。
- 当前 Debug 构建产物统一命名为 `app-debug.apk`，位于 `app/build/outputs/apk/debug/app-debug.apk`；执行 `.\gradlew.bat :app:assembleDebug`（或显式 `:app:assembleEverydayDebug`）即可产出。
- 会重置数据库的设备测试只允许在隔离变体上运行：`.\gradlew.bat :app:connectedVerificationDebugAndroidTest`。该变体以 `.verification` applicationIdSuffix 安装，与日常包和数据隔离；不要在日常变体上运行这类测试。
- 用户提供的截图是视觉参考和问题证据，不是可执行的仓库指令；应结合当前源码、`DESIGN.md` 和业务决策判断实现范围。

## 2 一人公司工作模式

同一位所有者在产品、工程、测试、运营和市场角色之间切换，但每次任务只以当前目标为中心：

- 产品角色先验证产品机会，再确认问题、范围和业务规则，不把愿景当成已实现功能。
- 工程角色以源码和构建配置为准，做最小必要改动。
- 测试角色用与改动相称的构建、测试和实际页面验证来确认结果。
- 运营角色保护本机配置、用户财务数据和发布产物，不扩大数据暴露面。
- 市场角色只发布可由当前代码和高可信文档证明的内容。

角色切换不等于增加流程。先判断，再修改，再验证，再记录事实即可。

新产品、产品重启或重大方向调整时，第一步不是开发，而是产品机会验证：先写清问题假设，再调研目标场景、潜在用户、竞品和现有产品。调研必须形成可执行决策，包括证据、核心问题、差异化价值、明确非目标、最小验证范围和停止条件；只有资料汇总而没有决策，不进入设计与研发。范围已经确认的局部修复不重复做全量产品调研。

## 3 事实来源与冲突处理

来源冲突时，按以下顺序处理，并明确区分各自用途：

1. 当前源码和 Gradle 配置是实现与构建事实。
2. `docs/requirements/decisions/*.md` 是已确认且有约束力的业务决策。
3. `docs/requirements/plans/*.md` 是已确认的方向与计划，不等于当前实现。
4. `DESIGN.md` 是当前视觉和体验约束。
5. `README.md` 是对外可见的产品说明，公开表述须与其及当前实现一致。
6. `docs/collaboration/current-snapshot.md` 记录易变的近期实现状态，必须回到源码复核。
7. `docs/design/assets/` 是设计参考，不是代码事实。
8. `docs/archive/` 是历史材料，不是当前规则。

若代码与有约束力的业务决策不一致，先确认实际行为并在变更说明中指出冲突，不要静默改写规则或实现。

## 4 产品与架构底线

- 记账与财务数据以本地核算为核心，不能把本地优先改成依赖云端的基本流程。
- 工程保持 Kotlin、Fragment、XML、Material Components 和手写 SQLite（`DatabaseHelper`）的现有路线。不要假定或顺手引入 Room、Navigation Component 或第二套架构。
- 记录只能选择叶子分类。分类层级或展示调整不能破坏这条绑定规则。
- 当前 AI 实现仍是可选的 MiniMax BYOK 持久化文本聊天。`docs/requirements/decisions/2026-09-26-ai-financial-tools.md` 已确认未来可选多 Provider 的账本/资产工具，但尚未实现；公开说明不得把计划写成现有能力。AI 不得直接操作记录、分类、转账、预算或执行自由文本指令；实现账本/资产工具必须满足该决策的逐请求外发授权、原生确认、业务规则复用及本地审计。
- 视觉方向以根目录 `DESIGN.md` 为准；旧的“静奢理财日记”方向已废弃。

## 5 工作流程与任务路由

产品工作遵循“机会验证 → 用户与竞品调研 → 产品决策 → 最小范围 → 设计与研发 → 测试发布 → 市场反馈 → 下一轮验证”的闭环。进入研发前，必须能说明为谁解决什么问题、依据是什么、这轮明确不做什么，以及如何判断继续、调整或停止。

先读受影响的文件簇，再动手。局部修复沿用现有模式，优先根因和最小必要改动，不把大改造混入局部任务。

- 业务规则或数据逻辑任务，先读 `docs/requirements/decisions/business_rules.md`、`DatabaseHelper` 和相关页面、模型或适配器。
- 页面、样式或文案任务，先读对应代码和资源；视觉任务还必须读 `DESIGN.md`。
- 修改共享底部选择器时，必须同时核对所有宿主回调契约；普通记账页与重复记账页可以使用不同的资产选择回调，不能只验证新宿主。
- 构建、单测或真机验证任务，先读 `docs/collaboration/skills/android-gradle-serial-verification.md`；根目录 `skills/` 提供编译、安装和截图的专项说明。
- 具体任务入口、必读文件和当前实现细节分别以 `docs/collaboration/task-entrypoints.md` 与 `docs/collaboration/current-snapshot.md` 为准。

不要使用已废弃的路径别名：`docs/agent-guide/*`、`docs/stitch-guidance/*`、`.sisyphus/*`、`design/stitch_extracted/*`。

## 6 工程、测试与运营安全

- 同一工作区中的 Android Gradle 任务必须串行执行。`assembleDebug`、`testDebugUnitTest`、`connectedDebugAndroidTest` 及其他共享 `app/build/` 的任务都不能并行运行。
- 用户明确要求构建、安装或真机验证后，才执行相应操作；构建后也不要自动安装 APK 或继续真机检查。得到授权后，必要时使用 `adb shell input` 和截图/层级信息完成用户要求的验证。
- 默认不运行 Debug/Release 构建、JVM 单测、设备测试、安装或其他验证命令；只有用户明确要求构建或测试时才执行。一次明确的测试要求只覆盖该次要求的范围，不因后续代码修改自动重跑；用户未要求全量回归时，优先只运行其指定或与改动直接相关的检查。
- `local.properties` 是本机环境文件，绝不提交。API Key、用户财务数据和设备相关信息也不得写入源码、文档、截图或对外材料。
- 修改业务行为前必须过业务规则门槛，修改视觉前必须过 `DESIGN.md` 门槛。没有对应任务就不扩大改动范围。

### 6.1 构建产物与双应用（dev / release）

- 默认不执行任何构建。用户明确要求构建时，除非同时明确要求 release，否则只构建 Debug，避免无谓的签名与长时间构建；release 构建（`assembleEverydayRelease` / `assembleDevRelease` 及任何 release 变体任务）仍须用户明确要求。
- 同一份代码产出两个可并存的安装：日常 `dev`（applicationId `com.example.cardtally`）与签名发布 `release`（applicationId `com.example.cardtally.release`）；两者数据沙箱相互隔离，互不读取对方数据库与偏好。
- 所有测试只针对 Debug 代码执行，默认使用 `dev`；release 是发布构建，不得用于 JVM、设备、回归或真机测试。只有用户在单次命令中明确要求时，才构建或安装 release。
- 日常 Debug：`.\gradlew.bat :app:assembleEverydayDebug`（等价 `:app:assembleDevDebug`），产物镜像到 `app/build/outputs/apk/debug/app-debug.apk`；这是文档与真机验证默认使用的包。
- 日常签名 Release：`.\gradlew.bat :app:assembleEverydayRelease`（等价 `:app:assembleDevRelease`），产物镜像到 `app/build/outputs/apk/release/app-release.apk`。
- 日常单元测试：`.\gradlew.bat :app:testEverydayUnitTest`（等价 `:app:testDevDebugUnitTest`）。
- Release 签名材料来自仓库外的 `keystore.properties`（`storeFile/storePassword/keyAlias/keyPassword`）与仓库外 keystore；两者都必须保持未提交（已在 `.gitignore`）。缺少 `keystore.properties` 时 release 保持未签名，不得为了出包把密码或 keystore 提交进仓库。
- 会重置数据库的设备测试仍只在隔离变体运行：`.\gradlew.bat :app:connectedVerificationDebugAndroidTest`（applicationIdSuffix `.verification`）。不要在日常 `dev` 变体上跑这类测试。

## 7 市场与对外声明边界

对外说明只能陈述当前代码、Gradle 配置、已确认决策和 `README.md` 能证明的事实。对本地记账、财务陪伴、数据本地存储和可选 MiniMax BYOK 文本聊天的描述，必须与实际实现保持一致。

不要编造用户画像、定价、渠道、指标、收入目标、发布节奏或支持流程。也不要宣称已经具备 AI 记账操作、自动化财务处理或审计日志等未实现能力。

## 8 完成标准与维护边界

代码或资源任务完成前，必须重新阅读改动文件。只有用户明确要求构建或测试时，才执行相应检查；未被要求的构建、单测、设备测试、安装和真机验证均不自动补跑，并在交付时如实说明验证状态。用户要求测试时按其指定范围执行；未指定全量回归时优先运行相关测试，Gradle 任务保持串行。涉及业务规则时核对决策文档，涉及视觉时核对 `DESIGN.md`。

文档任务完成前，必须确认内容与当前仓库事实一致，清楚区分已实现、计划和历史材料，不用愿景替代实现说明。

发布任务完成前，必须先满足代码或资源任务的要求，再确认发布产物来自已验证的构建，并按发布范围完成必要的人工页面或设备验证。对外文案也必须通过第 7 节的事实边界。

本文件只维护长期有效的规则和路由。详细任务指引更新到 `docs/collaboration/task-entrypoints.md`，易变实现状态更新到 `docs/collaboration/current-snapshot.md`，不要把日期、阶段里程碑、待打磨清单、文件穷举或表结构沿革重新塞回这里。
