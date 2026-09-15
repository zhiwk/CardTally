# 第三轮复审：部分通过，整体仍不通过

本轮复核第二轮返工报告、源码、JVM测试、设备测试APK、指定安全设备测试与真机交互。未修改应用代码或任务状态，未保存分类、账本或财务数据，也未发送AI请求。

## 结论摘要

- R1 键盘遮挡：默认竖屏真机通过。
- R2/R3 请求隔离与停止气泡：源码方向改善，但真实调用链测试仍缺；另发现同步回调竞态。
- 上轮设备测试APK编译错误：已修复。
- 图标选择后关闭：真机确认仍未修复。
- 精选区高度、长金额测试、本地化及原任务矩阵：仍未完成到可验收程度。

因此不能归档 OpenSpec，也不能把 tasks.md 标成全部完成。

## 已通过或已有可靠证据

### T1 — R1 默认竖屏键盘遮挡通过

真机路径：我的→分类管理→添加一级分类→更多图标→点击搜索并输入 `home`。

- IME：`mInputShown=true`
- BottomSheet：`[0,753][1080,1491]`
- 关闭按钮：`[912,807][1056,951]`
- 搜索框：`[48,951][1032,1095]`
- 图标网格：`[0,1131][1080,1491]`

关闭按钮、搜索框和网格均在键盘上方，上一轮“取消按钮被键盘遮住”的核心缺陷在当前默认竖屏已修复。横屏、2倍字体和极短视口仍属未覆盖组合。

### T2 — 设备测试APK恢复可编译

强制执行 `:app:assembleDebugAndroidTest --rerun-tasks`：BUILD SUCCESSFUL，41任务全部实际执行。旧 `ScreenStateBundleTest` 已适配 `InFlightAiLifecycle` 的句柄接口。

### T3 — 当前自动验证

- OpenSpec strict validate：PASS。
- 静态资源脚本：PASS，71布局/49引用，仅是静态检查。
- `:app:testDebugUnitTest --rerun-tasks`：BUILD SUCCESSFUL；68项，0失败，0错误。
- 指定安全测试 `RecordRowLayoutMeasurementTest`：2项执行，Gradle成功；不读写数据库、偏好或网络。
- `app-debug.apk` 已用 `adb install -r` 覆盖安装并启动。此前 connected 测试结束后设备包列表中已无 CardTally，因此旧报告的“已安装”状态当时已过期；本轮现已恢复安装。

## 必须继续返工

### F1 / P1 — 图标选中后弹层仍不关闭（真机确认）

定位：`app/src/main/java/com/example/cardtally/adapter/IconPickerDialog.kt:39`。

真机点击首个图标后：`root_icon_picker` 仍为 `[0,1616][1080,2354]`，点击项变为“a b，已选中”，弹层未关闭。用户只能再点内容描述为“取消”的关闭按钮。这与原有“选择即返回”交互不一致，也未满足第二轮 S3。

修复：选择成功时仅调用一次业务回调，然后收起键盘并 dismiss；单纯关闭不触发选择。四个调用入口补共享组件/仪器测试，验证一次点击、一次回调、弹层关闭、预览更新。不要改成未经确认的“多选预览后确认”。

### F2 / P1 — AI 同步失败回调会留下错误的在飞生命周期

定位：`AgentFragment.kt:281` 调用 `sendChat`，到 `:355` 才执行 `requestLifecycle.markStarted(handle)`；`MiniMaxClient.kt:73` 与 `:90` 可在配置无效或URL非法时同步回调失败。

若URL字符串满足“配置完整”判断但不是合法URL，sendChat在返回句柄前同步触发Failure。UI线程上的 `runOnUiThread` 会立即执行：终态先 `markFinished()`，随后 sendChat 返回又 `markStarted(handle)`。结果是UI可显示空闲，但 requestLifecycle 内部仍为 loading，且页面销毁时因 `isSending=false` 不会取消/复位它。

修复：建立生命周期/句柄与回调顺序必须对同步和异步sender都正确。推荐让sender始终异步回调，或在调用前登记可替换的请求生命周期，再原子绑定句柄；不要靠延时。用 fake 同步Failure、异步Failure、流式、停止后重发分别验证 `isSending`、identity、handle和适配器最终一致。

### F3 / P2 — 报告声称有 Fragment fake 注入口，但没有调用链测试

`senderFactory` 已存在，但现有测试未使用它。`AiRequestIdentityTest` 只操作纯状态类/StringBuilder，没有驱动 AgentFragment、AgentChatAdapter 或持久化调用，因此不能证明报告所述完整结果。

另 `AiRequestIdentity.begin(sessionId)` 与 `isCurrent(requestId, sessionId)` 仍完全忽略 sessionId；类注释声称ID与会话都匹配，代码只匹配请求ID。应存储活动会话ID并测试不匹配会话，或删除误导参数/注释并由调用层明确保证会话隔离。

### F4 / P2 — 精选浏览高度仍用固定估算

定位：`AddCategoryFragment.kt:93`。当前仍用 `screenHeight - 104dp`，注释也写明是 estimate；只post一次，不基于pane真实窗口坐标/Insets，也不响应字体、横屏或IME变化。第二轮 S4 未完成。

修复：使用实际可用容器和pane在窗口中的位置计算，360dp只是上限；仅在目标高度变化时更新，并处理监听移除。覆盖320dp、2倍字体、横屏。

### F5 / P2 — 长金额测试无法证明规范通过

定位：`RecordRowLayoutMeasurementTest.kt:31-32,58-60`；`item_record.xml:85-86`。

测试把整行强制为EXACTLY 60dp，却用 `row.height > 0` 宣称“行高增长”；这个断言必然成立，不能证明2倍字体自适应。`nameView.text == longName` 只证明模型字符串未变，不证明屏幕可见文本未被省略。布局本身仍明确 `ellipsize=end`、`maxLines=1`，与规范“长标题可两行/不足时重排”不一致。测试也没有检测名称与金额重叠、TextView layout ellipsis count、金额文字测量宽度或可访问完整名称。

修复：用AT_MOST/真实Recycler父约束测量wrap_content高度；检查 `layout.getEllipsisCount(line)`、各视图矩形不重叠、amount paint/text width与可见宽、2倍字体行高，以及320/360/600。若实际省略或冲突，按design.md重排，不以测试名称代替证据。

### F6 / P2 — OpenSpec任务和本地化仍未收口

`tasks.md` 除1.1和2.1外仍全部未勾选，与“已经完成OpenSpec”的说法不符。实施报告也承认UX09矩阵、UX11剩余本地化、UX07/UX10完整链路未完成。

当前可达资源仍有硬编码示例：`fragment_asset_v2.xml`、`fragment_add_asset.xml`、`fragment_asset_type_select.xml`、`fragment_ledger_management.xml`、`dialog_asset.xml`、`dialog_month_picker.xml`、`dialog_search_date_range.xml`。空资产动态引导已经实现，不要回退；继续补齐其余中英文资源与验证。

## 代码质量补充

`IconPickerDialog` 已增加“高度不变则不setLayoutParams”的保护，上一轮无条件反馈循环已修正。但匿名 `OnGlobalLayoutListener` 没有保存或在dismiss/detach时移除；短期对象可被回收，不直接判定当前崩溃，但共享弹层应明确监听生命周期，避免重复打开产生无谓回调。

## 下一轮验收门槛

1. 修复F1和F2，并增加真正驱动共享弹层/Agent调用链的测试。
2. 修正F4/F5，提供真实布局测量，而不是固定估算或弱断言。
3. 完成F6及原tasks中仍要求的安全验证；据实勾选任务。
4. 串行通过静态检查、JVM单测、assembleDebug、assembleDebugAndroidTest和指定安全设备测试。
5. 更新报告后停止，不归档，交回最终视觉复审。
