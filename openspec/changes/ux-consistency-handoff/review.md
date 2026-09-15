# 实施复审：暂不通过

本次仅验收，没有修改应用代码，也未勾选或归档变更。实施报告本身明确为部分实施，tasks.md 仍有大量未完成项，不能将 OpenSpec 文档齐全理解为实施完成。

## 必须处理的问题

### R1 / P1 — 图标搜索键盘遮住取消按钮（UX09，真机复现）

- 定位：`app/src/main/java/com/example/cardtally/adapter/IconPickerAdapter.kt:89` 的 applyResponsiveRecyclerHeight；`app/src/main/res/layout/dialog_icon_picker.xml` 的 recycler_icons；各图标弹窗调用方。
- 复现：我的→分类管理→添加一级分类→更多图标→点击搜索框。无需输入、保存或修改任何分类。
- 实际：键盘覆盖弹窗底部，“取消”不可见。弹出键盘前后网格仍为1296px（432dp）；弹出后取消节点纵坐标1820—1964px，已在键盘覆盖区。截图检查也确认按钮被遮挡。临时证据：`app/build/review-icon-ime.png`。
- 根因：仅在显示前按 displayMetrics.heightPixels 的72%限制网格，既不扣除弹窗标题/搜索/按钮，也不响应 IME 可用高变化。
- 修复要求：监听实际窗口可用高与 IME/insets，扣除固定内容后给网格剩余空间，网格滚动、搜索与取消保持可见；键盘开关后重算。不要使用延时或仅换一个屏高百分比。
- 验收：同一路径键盘开/关，竖/横屏及大字体，取消始终可见可点，所有图标仍可滚动选择。

### R2 / P1 — 停止后重发缺少请求身份隔离（UX10，源码确认竞态；未发真实请求）

- 定位：`AgentFragment.kt:235`、`:280`、`:625`；`network/MiniMaxClient.kt` 的全局 isCancelled 和 activeCall。
- 事件顺序：请求A流式中→停止（设置 suppressNextTerminalCallback=true 并立即恢复发送）→发B（该标记被清成false）→A的晚到回调进入当前页面。回调只读共享 isSending/currentSessionId 和最后一条消息，没有核对请求ID。
- 影响：A的晚到终态可结束B的发送状态或覆盖B的最后消息；晚到chunk在B发送中也可被接受。不能用“抑制下一次终态”代替按请求身份过滤。
- 相关根因：客户端 onResponse 开始即清 activeCall，流式期间 cancel 不一定能关闭该Call；全局取消标记又会被新请求清除。该客户端问题有既有因素，但本次停止控件验收必须覆盖此调用链。
- 修复要求：为每次请求绑定不可变请求身份及会话身份，停止/离开即失效；每个chunk和终态都核验身份。客户端取消标记/Call生命周期也必须属于对应请求，不能被B复位而重新接受A。
- 验收：fake确定性调度A停止→B启动→A迟到chunk/失败/成功→B完成，断言B文本、状态、会话和落库结果均不受A影响。禁止真实计费请求。

### R3 / P2 — 停止回复没有同步结束消息适配器（UX10，源码确认）

- 定位：`AgentFragment.kt:639` 的 persistCurrentAssistantDraftBeforeForcedStop；`adapter/AgentChatAdapter.kt:68` 的 finalizeStreamingMessage。
- 实际：停止路径更新 chatMessages、数据库和会话列表，却没有调用消息适配器的 finalizeStreamingMessage/等价同步；适配器维护自己的消息副本及 streamingMessageIndex。终态随后又被主动抑制。
- 影响：当前气泡继续显示未加取消提示的旧副本，停止后的视觉和持久化结果不一致；适配器流式索引也未在停止时清理。
- 修复要求：停止时将同一最终消息同步到模型、适配器和持久化层，结束流式索引；空占位另按现有规则处理，不重复保存。
- 验收：fake收到部分文本→停止，当前气泡立即显示停止后的内容/状态；离开再进入显示一致；只落库一次。

### R4 / P2 — 精选图标浏览仍未满足文字化规范（UX08，真机+源码）

- 定位：`AddCategoryFragment.kt:159`、`:200`、`:239`；`fragment_add_category.xml:59`。
- 真机：默认“常用”仍展示 arrow autofit content/down/height 等英文原始键；未选中和选中分组都使用同一浅灰背景，只有字重/文字色区别。临时证据：`app/build/review-category.png`。
- 源码：精选映射未覆盖实际take(16)集合；文字11sp，低于交接12sp下限；浏览区写死360dp，不是360dp最大值加可用高约束。
- 修复要求：为每组实际展示的精选图标补齐中英文标签；至少12sp；未选中组透明，选中组独立底色并暴露selected语义；按可用空间限制浏览区，分别滚动。补精选标签覆盖测试，不改变持久化图标键。
- 另：搜索提示举例“交通、餐饮”，但 IconPickerSelectionState 只匹配英文图标键，输入这些中文不会命中。应提供标签/分组别名搜索，或将提示改成实际支持的查询，不能继续承诺不存在的中文搜索能力。

### R5 / P2 — 必需项仍未实施或验证（UX05/06/11/12）

- 实施报告已承认：长金额组合未复现；周期弹层与账本保存按钮尚未统一；可达页面本地化及隐藏资产入口的空态指引未完成；AI fake生命周期未测试。
- 这些不是经用户豁免的验收范围。没有测试夹具并非无法推进：可以使用不连接用户数据库的布局测量与内存模型，fake则采用最小可注入边界，不必重构应用架构。
- 继续按原tasks.md完成并逐项记录；不能只把它们转成后续建议便宣布整体完成。

## 已确认改善

- 设置页chevron形状恢复正常，白色圆角卡片符合本轮基线；已看实施截图并从设备进入分类管理验证路由。
- 新增分类默认支出绿底、更多图标入口可进入；没有进行保存。
- 图标选择模型已使用稳定名称，过滤不会重置新选择；相关纯Kotlin测试存在且通过。适配器点击与保存完整链路仍需补测试。
- 二级头与资产更多菜单存在对应实现；未在用户资产上执行置顶/归档/删除验收。

## 本轮验证证据

- `openspec validate ux-consistency-handoff --strict --no-interactive`：PASS，仅表示规范有效。
- `powershell -NoProfile -File scripts/verify-ux-resources.ps1`：PASS，71布局/49直接引用，不能覆盖键盘与运行时状态。
- `gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain`：BUILD SUCCESSFUL，任务为UP-TO-DATE。
- 为避免仅依赖缓存，另行串行执行 `gradlew.bat :app:testDebugUnitTest --rerun-tasks --console=plain`：BUILD SUCCESSFUL，22任务实际执行。
- 从本轮TEST-*.xml汇总为 **54项、0失败**，不是实施报告的55项；报告应更正数量。
- 真机仅导航/打开新增分类和图标搜索、弹出键盘，没有保存、清数据、改偏好或发送AI请求。检查后返回账单。
- 尚未覆盖全部页面、字体矩阵、横屏、TalkBack、非空财务业务回归；不宣称全量验收通过。

## 交给实施模型

先修R1—R3并补确定性测试，再完成R4、R5和原未勾选任务。更新implementation-report.md的真实证据与数量。不要修改用户数据，不自行归档；交回后再做视觉复审。
