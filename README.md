# 小猫记帐（CardTally）

<p align="center">
  <img src="app/src/main/res/drawable-nodpi/ic_launcher_artwork.png" width="112" height="112" alt="小猫记帐：黑白小猫抱账本" />
</p>

**本地优先的 Android 记账应用，记录收支、管理账户，随时回顾财务状况。**

日常记账与统计在本机完成。可选的 AI 助手连接你配置的服务，支持财务交流、查账和账单操作建议；新增、修改、删除均由你核对并确认后执行。

支持 Android 7.0 及以上 · 中文 / English · 浅色 / 深色 / 跟随系统

[主要功能](#主要功能) · [快速开始](#快速开始) · [使用指南](#使用指南) · [数据与隐私](#数据与隐私) · [开发与验证](#开发与验证)

## 主要功能

| 功能 | 说明 |
| --- | --- |
| 收支记账 | 标准与快速录入，支持分类、日期、备注和照片；收入、支出可不关联资产。 |
| 账户与转账 | 管理余额与流水，账户详情可发起转账，支持手续费。 |
| 账单与统计 | 按月翻阅、搜索和日历查账；按周、月、年或自定义周期查看趋势与分类排行。 |
| 分类与重复记账 | 管理树形分类，设置默认收支资产；支持每日、每周、每月、每年及间隔周期任务。 |
| 个性化外观 | 纯色或图片背景，三张内置图片与本机图片库，卡片不透明度可调。 |
| 本机备份 | 密码保护备份与合并恢复，兼容旧 ZIP / JSON 导入。 |
| 可选 AI 助手 | 自填服务地址与 API Key、获取模型列表；流式对话、本机会话历史及经确认的单条账单操作。 |

## 快速开始

从源码构建 dev 版需要 **JDK 17 和 Android SDK 34**。可用 Android Studio 打开项目，或使用命令行：

```bash
git clone https://github.com/zhiwk/CardTally.git
cd CardTally
```

在本机 `local.properties` 中设置 `sdk.dir`，指向当前机器的 Android SDK。Windows 与 Linux 各自使用本机路径，此文件不提交到 Git。

**Windows（PowerShell）**

```powershell
.\gradlew.bat :app:assembleEverydayDebug
```

**macOS / Linux**

```bash
chmod +x gradlew
./gradlew :app:assembleEverydayDebug
```

生成的 APK：`app/build/outputs/apk/debug/app-debug.apk`。设备开启 USB 调试并连接后安装：

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`-r` 保留对应版本的现有数据。多台设备连接时，用 `adb -s <serial> install -r ...` 指定目标。

安装后，点击「+」记一笔；在「资产」管理账户，在「我的」设置分类、重复记账、外观与备份。AI 助手需先在「我的 → AI 助理」开启入口并配置服务。

## 使用指南

<details>
<summary>记一笔与转账</summary>

1. 在一级页面点击「+」，选择支出、收入或转账并填写金额。
2. 收支记录选择叶子分类，通过底部选择器设置日期与资产；资产可选「不选择」，记录仍计入统计，但不影响账户余额。
3. 转账必须选择不同的转出、转入账户。填写手续费时，转出账户扣除“金额 + 手续费”，转入账户增加“金额”，手续费计入支出。
4. 可填写备注、添加照片，点击「保存」完成。

在「我的 → 记一笔模式」选择标准或快速模式。若开启快捷记账，启动应用时直接进入录入页。

也可从资产账户详情点击「转账」，当前账户自动成为转出方。保存后返回详情会更新余额与流水，账单归属当前账本。

</details>

<details>
<summary>查账、统计与修改</summary>

- **账单**：默认展示当前账本、本月汇总与明细。右滑查看上个月，左滑查看下个月；顶部账本栏固定，统计卡片和列表一起翻页。
- **统计**：选择周、月、年或自定义周期；左右滑动或点击顶部切换支出／收入，选中横线跟随页面过渡。
- **详情**：点击账单查看内容和附件，通过详情页编辑或删除。账单、搜索、日历、统计明细及资产流水共用详情入口。

编辑收支时选择「不选择」可清除资产关联，保存后撤回原账户余额影响。删除账单需要确认。

</details>

<details>
<summary>分类与重复记账</summary>

在「我的 → 分类管理」切换支出／收入，添加、编辑或删除分类。记录只能绑定没有子分类的叶子分类；有子分类或关联记录的分类受到删除保护。

新数据库初始化的分类如下，之后可按需调整：

| 类型 | 一级分类 | 叶子分类 |
| --- | --- | --- |
| 支出 | 购物 | 服饰、家电、数码 |
| 支出 | 餐饮 | 早午晚餐 |
| 支出 | 居住 | 房租、酒店 |
| 支出 | 交通 | 短途、飞机高铁 |
| 收入 | 工作 | 工资、报销 |
| 收入 | 理财 | 股票、基金、黄金 |

在「我的 → 重复记账」新建任务，选择账本、类型、周期、开始时间及可选结束时间。转账任务需要不同的转出、转入账户。任务账本不改变应用当前账本；每月 31 日或每年 2 月 29 日在目标日期不存在时跳过该次执行。

</details>

<details>
<summary>外观</summary>

「我的 → 外观」是独立二级页面，分为纯色背景、图片背景两张卡片。两类均可选择浅色、深色或跟随系统；默认纯色浅色。

- 三张内置「默认图片 1、2、3」固定陈列且不可删除。
- 点击「添加图片」通过系统选择器导入；图片保存在本机图库，缩略图可左右滑动，点击切换背景。
- 自选图片右上角「×」可删除，删除当前图片后切换到默认图片 1。「恢复默认图片」只切换当前选择，不清空图库。
- 图片按屏幕比例居中裁切；单次导入文件不超过 32MB，大图采样后保存到应用私有目录。
- 图片背景的卡片不透明度为 0%–100%，默认 80%，按 5% 步长调整。松手后应用并保存，切换浅深配色沿用当前数值，保留外观页滚动位置。

文字和图标保持完整不透明度；弹窗与底部导航使用所选配色的实底。一级页显示底部导航，录入、外观、API 配置等二级页隐藏底部导航，返回后恢复。

</details>

<details>
<summary>AI 助手</summary>

1. 在「我的 → AI 助理」开启「显示 AI 入口」。
2. 打开「API 配置」，依次填写服务 URL 和 API Key。URL 可为基础地址或完整对话地址。
3. 点击「测试并获取模型」，从返回的下拉列表选择模型并保存。新配置不预设 URL 或模型。
4. 进入「AI 助手」开始对话。左上角菜单切换历史会话，长按会话可重命名，右上角「+」新建会话。
5. 需要查账或记账时，在「API 配置」开启「允许 AI 提出账单操作」，默认关闭。

模型列表查询只请求所配置服务的 OpenAI 兼容模型列表接口，不发送聊天或账本数据，也不验证每个模型的对话权限。普通聊天支持流式回复，服务返回普通 JSON 时自动回退为一次性展示。会话、消息及模型返回的思考过程保存在本机；思考默认折叠，不回传给模型。

开启账单操作后，服务和模型须支持 OpenAI 兼容工具调用：

- AI 可自动查询当前账本、读取账户和叶子分类选项；查询结果及本页对话上下文会发送给所配置服务，不发送账户余额或附件。
- 中途查账和补充信息无需额外确认，缺少信息时通过聊天询问。
- 最终新增、修改或删除打开原生预览，核对账本、内容及余额影响后点击一次「确认执行」。聊天文字“确认”不会执行写入。
- 每轮最多成功写入一条账单，含转账；多项建议会先整理成独立的单条建议，不批量写入。
- 页面退出后待确认操作作废，切换会话、账本或 API 配置后不复用原上下文。已成功的操作不会因后续模型回复失败自动重做。
- API 配置页可查看或清除本机操作日志；AI 不提供账本、资产、分类或预算管理。

</details>

## 数据与隐私

财务数据和聊天记录保存在本机 SQLite，API Key 保存在本机配置中。日常记账与统计以本机数据为基础；AI 功能需要网络，聊天内容和开启账单工具后的查询结果会发送给你配置的服务。模型和服务需支持对应的 OpenAI 兼容接口。

AI 账单操作默认关闭。开启后，中途查询和补充信息通过聊天完成，最终写入需要在原生预览中点击「确认执行」；每轮最多成功写入一条账单。AI 当前不提供账本、资产、分类或预算管理。

<details>
<summary>备份内容与恢复范围</summary>

在「我的」导出密码保护备份或选择备份文件导入。完整备份包含账本、资产、分类、账单、重复记账、设置、AI 聊天和账单照片，恢复时与本机数据合并。

- 备份包含已保存的 AI API Key，请保管好文件与密码。
- Android 系统自动备份已关闭。
- 旧 ZIP / JSON 可继续导入，旧 JSON 不含照片文件。
- 背景图片库及当前图片选择尚不包含在备份中；恢复到没有自选图片的设备时使用内置背景。
- AI 操作日志仅留本机，不随完整备份导出；账单工具聊天消息参与聊天备份，但不会混入普通聊天请求。

</details>

## 开发与验证

工程采用 **Kotlin + Fragment + XML + Material Components + 手写 SQLite**。账单月份与统计收支切换使用 ViewPager2，联网使用 OkHttp，重复任务使用 WorkManager。

当前工具链：Kotlin 2.0.0、Android Gradle Plugin 8.3.0、Gradle Wrapper 8.13；最低 API 24，编译与目标 API 34。同一工作区中的 Gradle 构建和测试须串行执行。

<details>
<summary>dev / release 版本与签名发布</summary>

| 版本 | 应用名称 | applicationId |
| --- | --- | --- |
| dev（Debug） | 小猫记帐 (Dev) | `com.example.cardtally` |
| release | 小猫记帐 | `com.example.cardtally.release` |

两版可以同时安装，数据库与偏好分别保存在各自沙箱中。仓库与内部工程标识继续使用 CardTally。

release 使用本机未跟踪的 `keystore.properties` 与发布签名密钥，配置项为 `storeFile`、`storePassword`、`keyAlias`、`keyPassword`。缺少配置时产物未签名，不能直接安装。签名材料不提交到仓库。

Windows（PowerShell）：

```powershell
.\gradlew.bat :app:assembleEverydayRelease
```

macOS / Linux：

```bash
./gradlew :app:assembleEverydayRelease
```

签名配置齐全时，构建后可安装：

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

</details>

<details>
<summary>单元测试与隔离设备验证</summary>

验证命令按需手动运行，构建和安装不会自动执行完整回归。单元测试与设备测试使用 Debug；会重置数据库的设备测试仅使用独立的 `.verification` 安装，保护日常 dev 与 release 数据。

Windows 的串行验证入口：

```powershell
# 完整 Debug 构建、JVM 单测与隔离设备测试
.\scripts\run-android-verification.ps1

# 仅 JVM 单测
.\scripts\run-android-verification.ps1 -SkipDebugBuild -SkipDeviceTests

# 仅分类数据库设备组
.\scripts\run-android-verification.ps1 -SkipDebugBuild -SkipUnitTests -DeviceGroups category-db
```

macOS / Linux 可按需串行运行：

```bash
# 日常 Debug JVM 单测
./gradlew :app:testEverydayUnitTest

# 隔离设备测试
./gradlew :app:connectedVerificationDebugAndroidTest
```

验证脚本及分组见 [scripts](scripts/)，近期实际构建、安装及未验证事项见 [当前快照](docs/collaboration/current-snapshot.md)。

</details>

<details>
<summary>目录结构</summary>

```text
CardTally/
├── app/src/main/        # 页面、数据逻辑、AI、布局与资源
├── app/src/test/        # JVM 单元测试
├── app/src/androidTest/ # 隔离设备测试与布局检查
├── scripts/            # 串行验证脚本
├── docs/               # 业务决策、计划、设计及交接
├── skills/             # 本地构建、安装与截图说明
├── AGENTS.md           # 稳定协作规则
├── DESIGN.md           # 当前视觉约束
└── README.md
```

</details>

进一步阅读：

- [协作入口](AGENTS.md)与[任务路由](docs/collaboration/task-entrypoints.md)：任务约束和源码入口。
- [当前快照](docs/collaboration/current-snapshot.md)：近期实现、构建与验证事实。
- [设计系统](DESIGN.md)与[应用图标](docs/design/assets/app-icon/README.md)：视觉约束及图标来源。
- [AI 账单工具决策](docs/requirements/decisions/2026-10-04-ai-record-tools.md)：工具范围与原生确认规则。

## 参与贡献

欢迎通过 [Issues](https://github.com/zhiwk/CardTally/issues) 提交问题或建议，通过 Pull Request 提交修改。报告问题时请说明复现步骤、应用版本和 Android 版本；分享截图或日志前，请移除 API Key 和个人财务信息。

修改业务规则、数据或界面前，请先阅读 [协作入口](AGENTS.md)和相关决策。计划与历史材料位于 `docs/`，当前能力以源码为准。

项目代码采用 [MIT 许可证](LICENSE)。
