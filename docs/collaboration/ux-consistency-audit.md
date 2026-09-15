# UX 一致性检查（2026-09-09）

## 基准与范围

以用户最新账单页和记一笔截图为基准：灰色页面、纯白圆角主体卡片、无装饰性描边和阴影、系统无衬线字体。`DESIGN.md` 的 Current visual baseline 优先于下文历史描边卡片描述。

本轮检查页面布局、共享行、弹窗及相关适配器。静态检查解析 71 个 XML 布局，追踪 49 个代码引用或 include 布局；这些数量不是“49 个真机页面通过”。

## 页面检查与调整

| 页面组 | 结果 |
| --- | --- |
| 账单、搜索、统计、我的、语言 | 主体已符合白色卡片基准；修正统计图例旧字体，保留收支语义色 |
| 记一笔、编辑记录（共用布局） | 移除图片预览边框、底部操作栏阴影；次要操作使用不透明浅灰表面 |
| 日期、资产、分类、统计周期弹窗 | 去除旧字体；统一 16dp 横向间距、48dp 关闭/翻月按钮；日期摘要卡白底无边框 |
| 资产、资产类型、资产流水、归档 | 资产流水摘要去边框；新增/编辑资产卡片统一 12dp；归档页明确背景与标题样式 |
| 分类管理、新增分类、图标选择 | 保留已统一管理列表；分类编辑面板用共享圆角背景，图标选择去阴影并采用主题选中色 |
| 账本管理、新增/编辑账本 | 检查列表与动态卡片；名称外的独立白色面板改为圆角；不改资产关系规则 |
| AI 助手、API 配置、会话列表 | 主体纯白圆角、紧凑标题；API 输入框保留边界；会话选中保留色块和标记但去描边 |

共享普通卡片资源为 `bg_card_surface.xml`；旧 `bg_summary_item.xml` 已改为不透明白色并保留原内边距。输入框与分隔线仍允许细边界，语义图标底色不变。主体卡片与选中状态不能混为一谈。

## 验证证据与边界

- 本轮已运行 `assembleDebug`、`testDebugUnitTest` 和 `scripts/verify-ux-resources.ps1`。最新执行结果以交接快照为准。
- 中断前已在设备进入账单、统计、资产、归档、我的、API 配置、分类管理、新增分类、账本管理、记一笔及日期弹窗。API 配置有截图目视检查，其余多为控件层级和入口检查，不能宣称全部视觉状态验证完毕。
- 恢复后设备原应用已不可用，重新安装成功；继续检查记一笔的资产空列表与分类弹窗。无资产时补充中英文说明，不增加“无”选项或默认选择。
- 未创建或删除财务记录，未修改资产余额或 API 配置，未发起 AI 请求。
- 待专项验证：非空搜索结果、资产流水和图片预览的真机展示，编辑表单保存、AI 有消息/请求中/错误状态、大字体、横屏及 TalkBack。不能以静态检查代替这些验证。
- 未引用的旧版 `fragment_asset.xml`、`fragment_settings.xml` 等资源未全面重做，也未删除；当前入口使用 v2 布局。旧样式库中未使用的样式不代表实际页面。

## OpenSpec 审查交接（2026-09-09）

- 用户最新要求为“仅检查并提出修改，交给其他模型实施和功能验证”；此次未修改应用代码，未重新构建 APK。
- OpenSpec CLI 1.12.0 已安装，使用工具无关初始化（tools none）。后续实施入口为 [HANDOFF.md](../../openspec/changes/ux-consistency-handoff/HANDOFF.md)（仓库路径：`openspec/changes/ux-consistency-handoff/HANDOFF.md`）。
- 完整修改清单在同目录 `design.md`，分 UX01—UX12，明确区分设备观察、源码确认和待复现风险。此清单不是已实施清单。
- `openspec validate ux-consistency-handoff --strict --no-interactive` 已通过；4/4 planning artifacts complete 仅表示规划文档齐全，所有实施任务仍未勾选。最终视觉验收留待用户另行安排。

## 实施结果收口（2026-09-09）

- 同一 OpenSpec 变更随后由实施模型按 design.md 落地：UX01/UX02/UX03/UX04/UX06/UX07/UX08 主体及 UX09/UX10/UX11 的可达代码部分已完成；逐项结果与未覆盖组合见 [implementation-report.md](../../openspec/changes/ux-consistency-handoff/implementation-report.md)。
- 本文件此前的页面检查描述仍代表“审查基线”，不等于实施后的真机回归；实施后的真机与视觉验收状态以实施报告为准。

## 复审返工收口（review.md 的 R1—R5，2026-09-09）

- 原审查者新增 `review.md`（“暂不通过”），随后实施模型完成返工：R1 图标选择器改 `BottomSheetDialog` + 监听 IME/insets 重排网格，并已在真机验证键盘弹起后关闭按钮/网格位于键盘上方；R2 每请求独立取消句柄 + `AiRequestIdentity` 过滤停止后的迟到回调（新增 `AiChatSender` 接口与 `AgentFragment.senderFactory` 测试注入口）；R3 停止时同步消息适配器与空占位清理；R4 精选图标只显示有中英标签项、12sp、选中分组独立底色、按可用高度夹紧、别名搜索；R5 周期弹层/账本保存按钮统一 12 圆角、长金额布局测量测试、空资产“入口隐藏→指向 我的→账户资产”动态指引。
- 返工后 `testDebugUnitTest` 为 68 项 0 失败；`RecordRowLayoutMeasurementTest`（androidTest，仅 inflate+measure）2 项设备测试通过；`assembleDebug` 与静态脚本通过；APK 已重装。R1 设备测量：键盘弹起后关闭按钮 y=807—951、网格底 y=1491，均在键盘上方；截图 `screenshot/review_r1_icon_picker.png`。逐项结果与仍待办见实施报告。


## 后续检查方法

### 续检：AI 长内容与大字体

- `fragment_agent.xml` 主区改为垂直自适应排布，移除固定页头偏移和固定正文底部占位；多行输入的实际高度参与布局。API 未配置提示卡可滚动，避免放大字体时按钮不可达。
- 新增 `AgentLayoutIsolationTest`，只 inflate 布局、绑定内存消息，不启动 Activity，不读写偏好/数据库，不发网络请求；只运行此类，没有运行其他会改动数据的设备测试。
- 三项设备测试通过：已配置页在 1/1.3/2 倍字体下页头、正文、多行输入框不重叠；1/2 倍字体下长用户/错误回复文本不越界且流式 payload 保留文本；2 倍字体、较短视口下配置卡可滚动。
- 这不是完整聊天生命周期、系统键盘或 TalkBack 验证，也未覆盖非空搜索、资产流水和图片预览。
- 修复已有 `ScreenStateBundleTest` 的过期位置参数构造，仅使测试与当前草稿模型兼容；该测试本轮未执行。
- 最终 `assembleDebug`、`assembleDebugAndroidTest`、`testDebugUnitTest` 及静态样式检查通过；最新主 APK 已覆盖安装。测试 APK 也安装在设备上，未卸载或清除应用数据。

运行 `powershell -NoProfile -File scripts/verify-ux-resources.ps1` 可防止直接引用布局重新出现 serif/italic、白色 Material 卡片描边，以及共享卡片半透明填充。该脚本不完整解析主题继承、运行时取色和可访问性，仍须按改动页面真机验证。
