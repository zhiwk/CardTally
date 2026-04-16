# Android Debug APK 安装 Skill

用于把 CardTally 的 `debug apk` 通过 `adb` 安装到已连接的 Android 设备上。

## 适用场景

- 已完成 `assembleDebug`，需要把 APK 安装到手机
- 需要覆盖安装最新调试包
- 需要在真机上继续做截图、回归或交互验证

## 前置检查

安装前先确认：

1. 已执行过 `skills/android-build-debug.md` 对应的编译步骤
2. 本地 APK 存在：`app/build/outputs/apk/debug/CardTally-debug.apk`
3. `adb devices` 能看到目标设备处于 `device` 状态
4. 如有多台设备，已确认目标设备 `serial`

## 推荐命令

### 1. 检查设备

```powershell
adb devices
```

### 2. 安装 debug apk

```powershell
adb install -r app/build/outputs/apk/debug/CardTally-debug.apk
```

说明：

- `-r` 表示保留应用数据并覆盖安装
- 默认适合日常开发验证

## 多设备场景

如果同时连接多台设备，必须显式指定：

```powershell
adb -s <serial> install -r app/build/outputs/apk/debug/CardTally-debug.apk
```

不要在多设备场景下省略 `-s <serial>`。

## 成功判定

安装输出包含 `Success`。

如果后续还要验证界面，可继续使用：

- `skills/adb-current-screen-screenshot.md`

## 常见问题判读

### `adb: no devices/emulators found`

- 当前没有连上设备
- 检查 USB 调试、数据线、驱动或无线调试

### `more than one device/emulator`

- 当前连接了多台设备
- 改用 `adb -s <serial> install -r ...`

### `INSTALL_FAILED_VERSION_DOWNGRADE`

- 设备上的现有安装包版本号更高
- 需要先卸载旧包，或改用更高版本号重新打包

### `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

- 设备上已有签名不一致的同包名应用
- 需要先卸载设备上的旧应用再安装

### `adb: failed to stat ...`

- 本地 APK 路径不存在
- 先确认 `assembleDebug` 已成功且 APK 已产出

## 推荐执行顺序

1. 执行 `adb devices`
2. 如有多设备，确认目标 `serial`
3. 确认 `app/build/outputs/apk/debug/CardTally-debug.apk` 已生成
4. 执行 `adb install -r ...`
5. 检查输出是否为 `Success`

## 一句话记忆

> 先确认 `CardTally-debug.apk` 已生成，再用 `adb install -r` 覆盖安装；多设备时必须带 `-s <serial>`。
