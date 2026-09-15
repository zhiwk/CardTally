# 第二轮复审：有改善，仍暂不通过

范围：核对返工后的 R1—R5、源码、测试与实施截图。此次未修改应用代码，没有改任务勾选或归档。以下明确区分本轮运行证据和源码判断，不能将未完成真机复测的项目写成通过。

## 需返工

### S1 / P1 — 图标弹层每次布局又无条件申请布局

定位：`app/src/main/java/com/example/cardtally/adapter/IconPickerDialog.kt:98`，以及其下方 `OnGlobalLayoutListener`。

`onGlobalLayout` 每次调用 updateGrid，然后无论高度是否变化都执行 `recycler.layoutParams = params`。View.setLayoutParams 会申请新布局，因此稳定页面仍可反复 measure/layout，浪费主线程并使自动化空闲判定失败。不能直接把实施报告中的无法 idle 归因于键盘光标闪烁；当前代码本身就有反馈循环。此项为源码确认，尚未在本轮设备测量布局频率。

修复要求：只在目标高度确实改变时更新布局参数；明确布局/Insets监听的注册与移除时机。尽量使用一个有界容器的约束/weight处理剩余空间，不混用正高度和未清除的 layout_weight。补测试证明静止后布局次数稳定，而不只是能截图。

另需复测计算：当前 `totalHeight - imeBottom - systemBottom` 可能重复扣除覆盖的底部 Insets；`recycler.top` 是相对父节点的坐标，而 totalHeight 来自 decor，未扣完整弹层顶部位置；强制至少120dp网格也可能超过极短视口剩余高。这些计算风险不宣称本轮已真机复现，需以实际窗口坐标和键盘开关测量确认。

### S2 / P1 — 设备测试 APK 编译失败

定位：`app/src/androidTest/java/com/example/cardtally/state/ScreenStateBundleTest.kt:130`、`:131`；新接口位于 `state/StateSupport.kt`。

本轮实际执行 `gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --console=plain`：主 APK assembleDebug 完成（UP-TO-DATE），设备测试在 compileDebugAndroidTestKotlin 失败。

错误：

```text
ScreenStateBundleTest.kt:130:48 Too many arguments for public constructor(): InFlightAiLifecycle
ScreenStateBundleTest.kt:131:33 No value passed for parameter 'handle'
```

根因：生命周期改为无参构造和 markStarted(handle)，旧测试仍传取消lambda并调用无参markStarted。主APK与JVM单测通过不覆盖 androidTest 的编译。

修复要求：用新取消句柄/可控测试替身更新测试，保留“取消发生且加载状态复位”的断言，不能删除或跳过测试来让构建通过。先串行完成 assembleDebugAndroidTest，再运行经过安全审查的指定隔离类；不得在用户数据库跑财务变更测试。

### S3 / P2 — 选择图标后不再自动关闭弹层

定位：`app/src/main/java/com/example/cardtally/adapter/IconPickerDialog.kt:39`；四处 Fragment 的 IconPickerDialog.show 调用。

新共享弹层回调只向调用者传递图标，没有 `dialog.dismiss()`；各调用者也拿不到 dialog 并且未关闭它。相比上一版选中立即返回，这会导致选择已经应用但仍停在弹层中，用户只能再按标为“取消”的关闭按钮退出，操作含义不清。此项为源码确认，未在用户账本/分类上保存测试数据。

修复要求：图标点击成功回调一次后收键盘、关闭弹层并返回调用页；仅关闭而未选中时不得触发选择回调。若要改成多次预览+确认必须另行授权，不要暗改原流程。

测试至少覆盖新增分类、分类编辑、账本图标与录入中的图标入口，验证一次点击、一次回调、弹层dismiss，以及返回后预览一致。

### S4 / P2 — 精选浏览“按可用高度限制”仍是屏高估算

定位：`app/src/main/java/com/example/cardtally/AddCategoryFragment.kt:86` 的 clampBrowseAreaToViewport。

实际是一次post，使用 displayMetrics.heightPixels 减固定104dp作为“appbar + name + color rows estimate”，再取360dp上限。未测实际pane顶部、可用内容区、IME或放大字体后的控件高度，也不随之后窗口变化重新计算。与报告中的“按可用视口高度夹住”仍有差距。

修复要求：基于真实容器测量/Insets确定剩余空间，在窗口和字体变化时更新，360dp仅为最大值。补320dp宽、2倍字体、横屏及键盘状态的隔离布局证据。不要再改一个固定估算常量充当适配。

### S5 / P2 — 原交接范围仍有未完成项

实施报告仍列出 UX05 长金额未安全复现、UX11 硬编码及隐藏资产入口引导未完成、UX09组合与AI完整fake链路未完成。没有用户豁免，不能作为“已返工完成”的结案状态。

要求：继续按原tasks完成；长金额使用内存模型/隔离布局测量，不需真实财务数据。AI注入最小fake边界，验证实际消息模型、适配器及落库调用次数，不只测试一个编号类。

## 已改善，不应重复报成原缺陷

- **R2请求隔离**：Fragment每次begin生成新编号，chunk与终态入口核验编号；停止使旧编号失效。客户端改为每请求独立cancelled和Call句柄，新的请求不再清除旧请求取消标记。上一轮所述全局布尔取消缺陷已得到实质性修正。
- **R3停止气泡同步**：停止有内容时调用finalizeStreamingMessage，无内容时移除空占位；源码已补齐适配器同步。完整设备/fake流程尚未验证，不宣称持久化回归通过。
- **R4图标文字**：精选区域改为只显示有标签集合，12sp，未选中分组透明、选中独立底色并selected；共享搜索加入分组与标签别名。源码方向符合返工目标。高度限制单独见S4。
- **UX06按钮**：周期和账本保存的12dp圆角修改已存在。
- **数量更正**：本轮重新执行的67项JVM测试与报告一致。

## AI测试边界补充

`AiRequestIdentity.begin(sessionId)` 和 `isCurrent(requestId, sessionId)` 当前未存储/使用sessionId，类注释声称“两个身份都匹配”与实际不一致。现有Fragment在切会话时有失效处理，因此本次不直接把它判为已发生串会话缺陷；但应实现所声明的会话核验或删去误导接口，并补同requestId/不同sessionId的测试。

`AiRequestIdentityTest` 中的编排测试使用本地StringBuilder与状态字符串，并未驱动Fragment、MiniMaxClient、适配器或数据库；不能作为完整“停止→重发→晚到回调→落库一致”的功能验收。仍需最小fake测试连接实际使用链路。

## 本轮验证记录

| 检查 | 结果 |
| --- | --- |
| OpenSpec strict validate | PASS |
| 静态资源脚本 | PASS，71布局/49引用 |
| assembleDebug | 完成，UP-TO-DATE |
| assembleDebugAndroidTest | FAIL，ScreenStateBundleTest两处API不兼容 |
| testDebugUnitTest --rerun-tasks | PASS，22任务实际执行；67测试，0失败，0错误 |
| 新实施截图 review_r1_icon_picker.png | 已查看：关闭按钮移到顶部；截图本身没有可见系统键盘，不能证明键盘开启时的遮挡问题已解决 |
| 本轮实时手机视觉复测 | 未完成：检查时前台不是CardTally，未切换或操作其他应用；已请用户方便时切回图标选择页 |

没有发送AI请求、修改偏好、保存分类/账本或更改财务数据。未宣称本轮全页面视觉/交互回归通过。

## 下一轮交付要求

先修S1—S3并让设备测试APK可构建；再完成S4/S5及原未勾选任务。对R2/R3补实际调用链fake证据。更新implementation-report.md区分源码已改、JVM通过、设备通过和仍未验证；不要自行归档，交回视觉复审。
