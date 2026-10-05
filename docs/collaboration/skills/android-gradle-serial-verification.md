# Android Gradle 串行验证 Skill

用于处理 CardTally 里的 Android 构建、单元测试、真机测试验证，避免因为并行执行 Gradle 命令而误判结果。

## 执行授权

- 默认不运行 Debug 构建、JVM 单测或设备测试；只有用户明确要求构建或测试时才执行。
- 一次明确要求只执行对应范围；不要因后续代码修改自动重跑，也不要把指定测试扩展成全量回归。
- 用户要求构建本身不代表已获准安装 APK 或执行真机验证；这些步骤也需用户明确要求。

## 适用场景

- 需要执行 `assembleDebug`
- 需要执行 `testDebugUnitTest`
- 需要执行 `connectedDebugAndroidTest`
- 需要在同一轮任务里同时验证构建、单测、真机测试

## 本次问题提炼出的核心规则

### 1. 同一工作区里的 Android Gradle 验证命令默认串行执行

不要在同一个仓库工作区里并行运行任何 `gradlew` / Gradle 命令，尤其是这些命令：

- `assembleDebug`
- `testDebugUnitTest`
- `connectedDebugAndroidTest`
- 其他会读写同一个 `app/build/` 中间产物的 Gradle 任务

原因不是“逻辑上相关”，而是它们会共享 `build/`、`intermediates/`、`desugar_graph/`、Dex 产物与设备侧安装状态。并行执行时，可能出现：

- `Tool execution aborted`
- `FileNotFoundException` 指向 `app/build/intermediates/...`
- 一个命令成功，另一个命令因共享产物被覆盖而异常结束
- 人误把“命令被中止”读成“测试失败”

### 2. `Tool execution aborted` 不是自动等于测试失败

如果同一轮里并行触发了多个 Gradle 命令，某个 tool call 出现 `Tool execution aborted`，先不要直接下“构建失败 / 测试失败”的结论。

先分别确认每条命令自己的真实结果：

- 有没有 `BUILD SUCCESSFUL`
- 有没有明确的 failing test / compilation error / resource error
- 有没有只是因为另一个并发 Gradle 进程占用了共享输出目录或设备会话

## 推荐执行顺序

用户明确要求执行多项验证时，在同一工作区里按下面顺序串行执行：

1. `assembleDebug`
2. `testDebugUnitTest`
3. `connectedDebugAndroidTest`

如果只需要某一项，就只跑那一项；不要为了“节省时间”把它们并发丢给多个终端调用。

### 有限时验证入口

用户明确要求运行完整验证时，为避免单个 UI 测试、UTP 或 ADB 子进程无限等待，优先使用：

```powershell
.\scripts\run-android-verification.ps1
```

不带参数时该命令会运行完整的 Debug 构建、JVM 单测和 14 组隔离设备测试，仅在用户明确要求全套验证时调用。需要定向验证时，可跳过无关阶段：

```powershell
# 仅 JVM 单测
.\scripts\run-android-verification.ps1 -SkipDebugBuild -SkipDeviceTests

# 单个或多个设备测试组；组名见 scripts/verification-device-groups.ps1
.\scripts\run-android-verification.ps1 -SkipDebugBuild -SkipUnitTests -DeviceGroups category-db,record-db

# 仅日常 Debug 构建
.\scripts\run-android-verification.ps1 -SkipUnitTests -SkipDeviceTests
```

按指定组运行时仍会检查完整分组清单，但只预编译 verification 测试 APK 并运行所选组；不会执行日常 Debug APK 构建或 JVM 单测。

该入口串行执行日常 Debug 编译、JVM 单测、隔离设备测试 APK 预编译，再按 `scripts/verification-device-groups.ps1` 串行运行 14 个设备组。构建不计入组时限；普通设备组默认最多 60 秒，特别耗时的组可在清单中设置独立上限（当前加密备份组为 360 秒），设备阶段总时限默认 15 分钟。脚本先检查分组恰好覆盖全部 `*Test.kt` 类，再核对每组新生成的 XML 测试数、失败/跳过数和测试类，最终合计必须为 147 项。每组独立保存 stdout/stderr 至 `app/build/reports/verification/<run-id>/`；超时额外记录最后完成用例与设备进程/唤醒状态，并停止该组。只有明确“0 项启动且设备连接或 instrumentation 启动失败”时才自动重试一次（两次仍失败即 FAIL），运行中的测试超时不重试。成功判定同时要求 Gradle 进程返回 0 且日志包含 `BUILD SUCCESSFUL`。设备阶段会唤醒手机并临时将熄屏时间设为 30 分钟，结束后恢复原值；只清理 `.verification` 包进程，不触碰日常或 release 数据。

用户明确要求运行日常 Debug 构建和 JVM 单测、跳过设备测试时使用：

```powershell
.\scripts\run-android-verification.ps1 -SkipDeviceTests
```

## 推荐判读方式

### 构建失败时

优先区分两类问题：

1. **真实代码 / 资源错误**
   - Kotlin 编译错误
   - 资源找不到
   - manifest / XML 问题
2. **并发执行导致的环境 / 中间产物冲突**
   - `desugar_graph`
   - Dex 中间目录文件丢失
   - `Tool execution aborted`

如果怀疑是第二类，先停止并行任务，再单独顺序重跑一次同样的命令。单跑成功，就不要把之前的并发异常记录成代码缺陷。

### 测试结果判读时

- 单独运行成功过的测试，不要因为后续并发调用被中止就改判为失败
- 必须以该条命令自己的 Gradle 输出为准
- 如果测试命令没有明确失败栈，也没有 failing test 名称，不要编造失败结论

## 在 CardTally 中的具体约束

- 当前仓库是 Android + Gradle + 真机可连环境
- 真机测试会和安装、Dex、构建产物共享状态
- 因此一次任务里涉及多条 Android 验证命令时，默认采用“上一条完成，再跑下一条”的策略
- 即使多个命令彼此独立，只要它们通过 `gradlew` 进入同一工作区，也不要并行触发

### 变体与隔离约束

- 仓库存在 `dev` 与 `verification` 两个 flavor：`dev` 是日常包（applicationId `com.example.cardtally`），`verification` 带 `.verification` 后缀，仅用于设备测试。
- 会执行 `context.deleteDatabase("CardTally.db")` 的设备测试只能跑 `:app:connectedVerificationDebugAndroidTest`；在 `dev` 变体上运行时，`IsolatedTestGuard.requireIsolatedBuild()` 会跳过这些测试，避免清掉用户真实账本数据。
- 日常构建与单测仍使用 `:app:assembleDebug` / `:app:testDebugUnitTest`（等价显式名 `:app:assembleEverydayDebug` / `:app:testEverydayUnitTest`）。

## 一句话记忆

> 在 CardTally 里，未经用户明确要求不要运行 Android Gradle 构建或测试；获准运行后，Gradle 任务必须串行。看到 `Tool execution aborted` 时，先排查并发冲突，再决定是不是代码真的失败。
