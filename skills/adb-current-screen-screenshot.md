# Android 当前页面截图 Skill

用于在 CardTally 的真机联调、视觉回归、缺陷复现过程中，通过 `adb` 获取手机当前页面截图，并把截图拉回本地工作区。

## 适用场景

- 需要查看手机当前页面的真实 UI
- 需要为 bug 报告补一张当前页面截图
- 需要在改动前后对比真机界面
- 需要让后续协作者基于同一张设备截图继续分析

## 前置检查

执行截图前，先确认：

1. 本机已安装 Android SDK Platform Tools，`adb` 可直接调用
2. 手机已通过 USB 或无线调试连接
3. 设备已授权当前电脑调试
4. `adb devices` 能看到目标设备处于 `device` 状态，而不是 `offline` / `unauthorized`

## 多 Display 设备注意事项

部分 Android 设备会暴露多个 display。此时如果直接执行默认的 `adb shell screencap -p ...`，有可能截到错误的 display，结果表现为：

- 截图是纯黑或接近纯黑
- 截图内容和手机当前看到的页面不一致
- `adb` 输出 `Multiple displays were found...` 警告

遇到这种情况时，不要继续使用默认截图方式，应先查询设备上的 display id，再显式指定 `-d <display-id>`。

## 推荐命令

### 1. 检查已连接设备

```powershell
adb devices
```

如果存在多台设备，后续命令都应带上 `-s <serial>`，避免截错设备。

### 2. 在设备上生成当前页面截图

```powershell
adb shell screencap -p /sdcard/cardtally-current-screen.png
```

说明：

- `screencap -p` 会直接输出 PNG
- 文件先落到设备侧 `/sdcard/`，兼容性通常最好
- 默认文件名建议稳定使用 `cardtally-current-screen.png`，便于后续覆盖更新

### 3. 拉回到本地工作区

```powershell
adb pull /sdcard/cardtally-current-screen.png .
```

如果需要落到固定目录，可显式指定目标路径，例如：

```powershell
adb pull /sdcard/cardtally-current-screen.png docs/design/assets/device-captures/
```

### 4. 可选：清理设备侧临时文件

```powershell
adb shell rm /sdcard/cardtally-current-screen.png
```

## 显式指定 Display 截图

当设备存在多个 display，或默认截图出现黑图时，推荐改用下面的流程。

### 1. 查询 display id

```powershell
adb shell dumpsys SurfaceFlinger --display-id
```

输出通常类似：

```text
Display 4630946846403687059 (HWC display 0)
Display 4630946324137792660 (HWC display 3)
```

这里真正传给 `screencap -d` 的值，是前面的长数字 display id，而不是 `0` 或 `1` 这样的逻辑编号。

### 2. 指定 display id 截图

```powershell
adb shell screencap -p -d <display-id> /sdcard/cardtally-current-screen.png
adb pull /sdcard/cardtally-current-screen.png .
```

### 3. 带时间戳保存到固定目录

```powershell
$displayId = "<display-id>"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$localDir = "screenshot"
$localFile = "$localDir/cardtally-current-screen-$timestamp.png"
New-Item -ItemType Directory -Force -Path $localDir | Out-Null
adb shell screencap -p -d $displayId /sdcard/cardtally-current-screen.png
adb pull /sdcard/cardtally-current-screen.png "$localFile"
```

### 4. 多设备加多 display 场景

```powershell
$serial = "<serial>"
$displayId = "<display-id>"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$localDir = "screenshot"
$localFile = "$localDir/cardtally-current-screen-$timestamp.png"
New-Item -ItemType Directory -Force -Path $localDir | Out-Null
adb -s $serial shell dumpsys SurfaceFlinger --display-id
adb -s $serial shell screencap -p -d $displayId /sdcard/cardtally-current-screen.png
adb -s $serial pull /sdcard/cardtally-current-screen.png "$localFile"
```

## 固定目录加时间戳保存

如果需要为回归记录、缺陷取证或设计对比保留历史截图，推荐把截图拉到固定目录，并用时间戳命名，避免被下一次截图覆盖。

### 推荐目录

- `screenshot/`

### PowerShell 示例

```powershell
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$localDir = "screenshot"
$localFile = "$localDir/cardtally-current-screen-$timestamp.png"
New-Item -ItemType Directory -Force -Path $localDir | Out-Null
adb shell screencap -p /sdcard/cardtally-current-screen.png
adb pull /sdcard/cardtally-current-screen.png "$localFile"
```

说明：

- 先用 `New-Item -ItemType Directory -Force` 确保本地目录存在
- 文件名形如 `cardtally-current-screen-20260416-153045.png`
- 这种方式适合连续多次截图，不会覆盖旧文件

### 多设备 PowerShell 示例

```powershell
$serial = "<serial>"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$localDir = "screenshot"
$localFile = "$localDir/cardtally-current-screen-$timestamp.png"
New-Item -ItemType Directory -Force -Path $localDir | Out-Null
adb -s $serial shell screencap -p /sdcard/cardtally-current-screen.png
adb -s $serial pull /sdcard/cardtally-current-screen.png "$localFile"
```

## 多设备场景

当 `adb devices` 返回多台设备时，使用：

```powershell
adb -s <serial> shell screencap -p /sdcard/cardtally-current-screen.png
adb -s <serial> pull /sdcard/cardtally-current-screen.png .
```

不要在未指定序列号的情况下直接截图，否则可能命中错误设备。

## 推荐执行顺序

1. `adb devices`
2. 如有多设备，确认目标 `serial`
3. 打开手机上的目标页面
4. 如果设备提示存在多个 display，先执行 `adb shell dumpsys SurfaceFlinger --display-id`
5. 默认截图正常时，执行 `adb shell screencap -p ...`
6. 如出现黑图或多 display 警告，改用 `adb shell screencap -p -d <display-id> ...`
7. 执行 `adb pull ...`
8. 必要时再清理设备侧临时文件

如果需要保留历史截图，把第 7 步改为“拉到固定目录并带时间戳文件名”。

## 常见问题判读

### `adb: device unauthorized`

- 手机还没授权这台电脑
- 重新插拔设备或重新弹出授权框后再确认一次

### `adb: no devices/emulators found`

- 当前没有连上设备
- 先检查 USB 调试、数据线、驱动或无线调试连接

### `more than one device/emulator`

- 当前连接了多台设备
- 改用 `adb -s <serial> ...`

### `remote object does not exist`

- 设备侧截图文件没成功生成
- 先单独执行一次 `adb shell screencap -p ...`，再重新 `pull`

### 截图是黑色的

- 这通常不是页面本身黑，而是截到了错误的 display
- 先看命令输出里是否出现 `Multiple displays were found...`
- 然后执行 `adb shell dumpsys SurfaceFlinger --display-id`
- 改用 `adb shell screencap -p -d <display-id> ...` 逐个验证正确的 display

### 本地目录不存在

- `adb pull` 目标目录如果不存在，保存可能失败
- 先创建目录，或使用 `New-Item -ItemType Directory -Force -Path ...`

## 一句话记忆

> 先用 `adb devices` 确认设备；如果是多 display 设备，先查 `display id` 再用 `screencap -d <display-id>` 截图；需要留档时优先保存到根目录 `screenshot/` 并带时间戳命名。
