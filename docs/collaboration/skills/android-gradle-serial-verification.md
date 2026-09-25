# Android Gradle 串行验证 Skill

用于处理 CardTally 里的 Android 构建、单元测试、真机测试验证，避免因为并行执行 Gradle 命令而误判结果。

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

在同一工作区里，按下面顺序串行执行：

1. `assembleDebug`
2. `testDebugUnitTest`
3. `connectedDebugAndroidTest`

如果只需要某一项，就只跑那一项；不要为了“节省时间”把它们并发丢给多个终端调用。

### 有限时验证入口

为避免单个 UI 测试、UTP 或 ADB 子进程无限等待，优先使用：

```powershell
.\scripts\run-android-verification.ps1
```

该入口串行执行 Debug 编译、JVM 单测和隔离设备测试；默认超时分别为 10、5、8 分钟。任何阶段都会明确返回 `PASS`、`FAIL` 或 `TIMEOUT`，并把 stdout/stderr 保存到 `app/build/reports/verification/<run-id>/`。成功判定同时要求 Gradle 进程返回 0 且日志包含 `BUILD SUCCESSFUL`，失败标记优先于进程码。设备阶段开始时，脚本会唤醒设备并临时把屏幕超时设为 30 分钟，结束或失败时恢复原值，避免 ActivityScenario 因手机熄屏进入保存状态。超时会清理 Gradle 子进程和 verification 包，但不会触碰日常或 release 包。

需要只验证编译和 JVM 单测时使用：

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

> 在 CardTally 里，Android Gradle 构建与测试验证默认串行，不要并发跑任何 `gradlew` / Gradle 任务；看到 `Tool execution aborted` 时，先排查并发冲突，再决定是不是代码真的失败。
