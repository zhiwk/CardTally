# Android Debug 编译 Skill

用于在 CardTally 中执行本地 Android Debug 构建，并确认 `debug apk` 是否成功产出。

只有用户明确要求构建时才执行本 Skill 中的 Gradle 命令；代码修改完成后不自动构建。构建要求本身也不代表已获准安装 APK 或执行真机验证。

## 适用场景

- 需要验证当前改动是否还能成功编译
- 需要生成 `debug apk` 供真机安装
- 需要在进一步安装或截图前先确认构建通过

## 核心规则

1. 优先使用仓库自带 wrapper
2. 用户明确要求 Debug 构建时执行 `assembleDebug`；`verification` flavor（仅供隔离设备测试）会把变体名拆成 `devDebug`，等价显式任务是 `:app:assembleEverydayDebug`
3. 同一工作区里的 Gradle 构建与测试命令默认串行，不要并发执行
4. 会重置数据库的设备测试只在 `:app:connectedVerificationDebugAndroidTest` 下运行，不要在 `dev` 变体上跑

## 推荐命令

### Windows

```powershell
.\gradlew.bat assembleDebug
```

### macOS / Linux

```bash
./gradlew assembleDebug
```

## 成功判定

至少同时确认这两点：

1. Gradle 输出包含 `BUILD SUCCESSFUL`
2. APK 位于 `app/build/outputs/apk/debug/app-debug.apk`

## 失败判读

优先区分两类问题：

### 真实代码或资源错误

- Kotlin 编译失败
- 资源缺失
- Manifest 或 XML 错误

### 并发导致的中间产物冲突

- `Tool execution aborted`
- `FileNotFoundException` 指向 `app/build/intermediates/...`
- Dex 或 desugar 中间目录异常

如果用户已要求构建且怀疑是并发问题，先停止其他 Gradle 任务，再单独重跑一次 `assembleDebug`；不要在未获要求时自行重跑。

## 推荐执行顺序

1. 确认用户已明确要求 Debug 构建
2. 确认当前就在仓库根目录
3. 执行 `assembleDebug`
4. 检查 `BUILD SUCCESSFUL`
5. 检查 `app/build/outputs/apk/debug/app-debug.apk` 是否存在
6. 只有用户另行要求安装时，再使用 `skills/android-install-debug-apk.md`

## 一句话记忆

> 在 CardTally 里，仅在用户明确要求时用 wrapper 跑 `assembleDebug`；安装或真机验证也需明确要求。
