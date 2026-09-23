# Current Snapshot

## 当前有效快照

- 2026-09-23：录入与重复记账的共享资产选择器改为 `FragmentResult` 回传资产 ID 和转入选择标志，普通录入及重复记账页分别用 view 生命周期订阅；弹层显示与关闭时普通录入页仍恢复键盘状态。重复任务页已去掉按子视图索引修改分隔线的运行时代码，并改为滚动区域与固定金额键盘在垂直方向各自占位，防止状态行被键盘覆盖。此前一次完整套件超时；后续完整复跑 130/130 通过，测试 XML 运行时间 446.5 秒，完整验证脚本的设备阶段约 7 分 58 秒。日常 APK 已覆盖安装，真机核对重复任务状态行滚到键盘上方、普通录入转账双资产和重复任务资产选择结果。`fragment_add_record.xml` 当前不用于生产录入页，但仍由部分设备布局测试引用，不应直接删除。

- 2026-09-23：继续提取录入、统计和数据库边界：新增纯 Kotlin `RecordEntryValidator` 统一金额、手续费、叶子分类及双资产转账校验；新增 `RecurringScheduleCalculator` 集中初次到期日、缺失日跳过、间隔和后续日期计算，`DatabaseHelper` 与重复任务编辑页均调用该计算器；统计分类父子聚合及排名迁至 `StatisticsRankingBuilder`；记录 SQLite `ContentValues` 与 Cursor 映射移至 `RecordSqlMapper`，分类树排序移至 `CategoryTreeOrdering`；重复任务读写、校验、Cursor/ContentValues 映射迁至 `RecurringRecordRepository`，`DatabaseHelper` 保留原兼容 API 和跨账本记录生成编排。单元测试覆盖调度、录入校验、统计聚合和分类树；全套隔离设备测试 **130/130 通过**，仪器报告运行 446.5 秒。分类一级卡片描边按 `DESIGN.md` 归零；`verify-ux-resources.ps1` 检查 84 个布局及 60 个引用布局通过。日常 Debug APK 已覆盖安装并确认 `MainActivity` 启动。

- 2026-09-23：分类数据职责继续拆分：层级规则由 `CategoryHierarchyValidator` 管理，只读分类查询、叶子查询和路径构建由 `CategoryReadRepository` 管理；新增 `CategoryWriteRepository` 处理分类新增/修改/删除、排序和排序迁移。`DatabaseHelper` 保留原有 API、错误类型与默认分类/版本迁移入口。验证：此前完整验证脚本三阶段 PASS，设备 130 项；本轮 JVM 单测和 `assemble-debug` PASS，分类树/递归查询/迁移/删除撤销/schema 的 18 项隔离设备测试 PASS。

- 2026-09-20：重复记账功能已落地：支持每日/每周/每月/每年/间隔周期、结束日期、启停、跨账本展示，以及支出/收入/转账任务；转账任务分别保存转出与转入资产 ID，资产选择范围按任务账本过滤，选择任务账本不会切换应用当前账本。每月 31 日和每年 2 月 29 日在目标日期不存在时跳过执行。相关实现位于 `RecurringRecordEditFragment`、`RecurringRecordsFragment`、`RecurringRecordScheduler`、`RecurringRecordWorker` 与 `DatabaseHelper`。

- 2026-09-20：曾修复普通「记一笔」转账资产点击无效的回归：当时共享选择器改用宿主接口后，普通录入页未收到选中回调；弹层关闭还需恢复金额键盘。后续回调契约已由 2026-09-23 项改为 `FragmentResult`，不再按宿主类型分流选择事件。

- 2026-09-20：分类面板一级分类卡片保持白色：`item_record_category_group.xml` 显式设置卡片、标题区域和子分类区域为白色，并在 `RecordCategoryGroupAdapter` 绑定时清除 MaterialCardView 前景层与运行时背景覆盖。该视觉修复已完成 Debug/Release 构建并安装验证。

- 2026-09-13：资产管理页统一使用一个多选入口，顶部独立“合并”入口已移除；普通状态右上角使用 `tabler_list_check` 多选图标，进入选择状态后切换关闭图标，底部保留删除与合并操作。合并选择同资产组内两个账本后再明确选择保留目标；资产组拆分入口和手势不再提供。

- 2026-09-13：资产管理页保留页内“合并账本”入口，但合并改为在本页进入选择模式：先选择同一资产组的两个账本，再明确选择保留目标，主账本参与时固定保留主账本，跨组选择会在选择阶段拦截；确认前展示源/目标和记录数量，成功后才清理选择状态。移除了账本行上的“拆分为独立资产组”入口和手势，不再提供现有资产组拆分功能。

- 2026-09-13：在“我的 → 管理”加入 JSON 导入/导出。`DataTransferManager` 导出账本、资产组关系、分类、资产、记录、AI 会话/消息及现有 SharedPreferences（包括 API Key），金额保留数据库整数分；导入采用合并模式，按实体内容映射 ID、记录和消息去重，未知实体新建，字段异常使用默认值或跳过并显示统计。文件操作使用系统文档选择器，导入导出均在后台线程执行；当前附件保留原有 URI 引用，未复制二进制文件。

- 2026-09-13：新增 `scripts/run-android-verification.ps1` 作为有界验证入口。它串行运行 `:app:assembleEverydayDebug`、`:app:testEverydayUnitTest`、`:app:connectedVerificationDebugAndroidTest`，默认阶段超时 10/5/8 分钟；每阶段写独立 stdout/stderr 日志，遇到失败或超时立即停止，超时返回 124 并清理 Gradle 子进程及 verification 包，不操作日常/release 包。验证结果：完整入口 **PASS**（编译、JVM 单测、110 项隔离设备测试均通过）；`-SkipDeviceTests` 入口 **PASS**。

- 2026-09-13：金额内部精度优化已落地一部分：项目只保留 `dev`、`verification` 两个 flavor，release 使用 `.release` applicationId；beta flavor 和 beta 首启清理逻辑已移除。数据库版本升至 v32：记录/资产/撤销余额金额列使用 SQLite `INTEGER` 分，应用模型与 UI 仍使用元单位 `Double`，边界通过 `Money` 转换（如 UI `1.22` ↔ DB `122`），键盘表达式改为整数分计算；记录写入/更新与资产余额效果纳入事务并加数据层校验；当前核心查询索引已加入，`getTodayRecordsPage()` 已补当前账本条件。注意：当前仍保留旧共享资产关系表和旧迁移代码作为兼容源码，尚未完成 asset pool / attachment 表级范式删除；日常旧库迁移仍是后续独立范围。

- 2026-09-13：按用户要求移除 beta 应用后，`assembleEverydayRelease` 成功并安装到 PNM-AN10，包名为 `com.example.cardtally.release`；dev 编译和 JVM 单测通过。移除 beta 后重新执行隔离设备测试时，构建和测试 APK 安装完成并启动 111 项测试，但 8 分钟内没有结束，验证入口按设计返回 `TIMEOUT` 并清理测试进程；未将该轮设备测试记为 PASS。手机上的旧 `com.example.cardtally.beta` 包已卸载；当前设备保留 dev、release 及测试专用 verification 包。

- 2026-09-13：修复 AI 回复出现整片 `null`，并支持展示模型思考过程。根因：`MiniMaxPayloadParser` 用 `optString("content")`，而 `org.json` 在键存在但值为 JSON `null` 时返回字符串 `"null"`；`deepseek-flash` 流式时把思考放在 `delta.reasoning_content` 且 `delta.content` 为 `null`，于是每个思考分片都追加一个 `"null"`，最终随消息落库（历史不清理，按用户选择）。修复：新增 `JSONObject.stringOrNull`（`!has || isNull` 视为无），流式同时解析 `content` 与 `reasoning_content`（兼容 `reasoning`），非流式读 `message.reasoning_content`；`MiniMaxChatResult` 的 `Success/StreamingChunk/StreamingDone` 增加可空 `reasoning`，`MiniMaxClient` 分别累积正文与思考。展示：`AiChatMessage` 增加 `reasoning`，`ai_chat_messages` 增加 `reasoning_content`（`DATABASE_VERSION 30→31`，`onUpgrade` 用 `ensureColumn`），`item_agent_message.xml` 在回答气泡上方加可折叠「思考过程」块（默认收起 2 行预览，流式时自动展开、答案开始后收起，点击标题切换），`AgentChatAdapter`/`AgentFragment` 同步渲染与落库；思考内容不回传给模型。验证：`:app:testEverydayUnitTest` 通过（新增 6 条解析回归）；`:app:assembleEverydayDebug` 成功；`:app:connectedVerificationDebugAndroidTest` **110/110、约 32 秒**（新增 `AgentReasoningMessageTest` 4 项、`DatabaseHelperAgentChatSessionTest` 思考往返 1 项）。PNM-AN10 真机真实请求：`layout_agent_reasoning` 出现、思考正文与回答正文均无 `nullnull` 连续串，回复正常（思考文本里出现的 “null” 是模型自身措辞，非解析结果）。

- 2026-09-13：修复录入金额的 ¥ 间距与长金额自适应，并修掉一个导致整轮设备测试卡死的布局死循环。新增 `util/EntryAmountLayoutController`：去掉快速布局 `edit_amount` 的 `minEms`、把 ¥ 的 `marginStart` 收到 6dp，短金额时字段保持 `wrap_content` 让 ¥ 紧贴数字；文本超出备注列剩余宽度时把字段钳到可用宽度并按宽度比例缩小字号（下限：快速 14sp、标准 12sp，`TextPaint` 推算、不依赖框架 autosize）。**根因修复**：控制器最初在 `OnGlobalLayoutListener` 里无条件 `row.requestLayout()`，形成“全局布局→applyForWidth→requestLayout→全局布局”的死循环，使 `AmountKeypadRenderTest.keypadOpensInARealActivity_andKeepsItsLabels` 的 `waitForIdleSync()` 卡到 **337 秒**，整轮 6 分 13 秒并因设备 5 分钟熄屏触发 12 个 `IllegalStateException: Can not perform this action after onSaveInstanceState` 假失败。现改为仅当字段宽度或字号实际变化时才 `requestLayout()`，并加重入保护。验证：`:app:testEverydayUnitTest` 通过；`:app:assembleEverydayDebug` / `:app:assembleEverydayRelease` 成功；`:app:connectedVerificationDebugAndroidTest` **105/105、约 30 秒**（`AmountKeypadRenderTest` 由 337s 回到秒级，新增 `EntryAmountLayoutTest` 4 项含真机布局死循环回归）。Release 已重装（`com.example.cardtally.release`）；PNM-AN10 真机复核：短金额 `¥ [402,1298][439,1379]` 与金额左缘间距 6px（约 2dp）；输入 19 位 `1000000000000000000` 时金额字段仍被钳在 `[445,1324][984,1385]`（高度由 121px 收到 63px，即字号已缩小）且文本完整、`¥` 仍紧贴。

- 2026-09-12：新增转账手续费与转账账户大卡。业务规则见 `docs/requirements/decisions/business_rules.md` 第 9 节：手续费仅转账；转出资产扣「金额+手续费」、转入资产加「金额」；删除对称回滚；手续费计入支出总额与支出趋势、不计入分类；列表显示「含手续费」。实现：`Record.fee`；`DATABASE_VERSION=30`，`records` 与 `record_deletion_undo` 各加 `fee REAL NOT NULL DEFAULT 0`（`onUpgrade(oldVersion<30)` 用 `ensureColumn`）；`createRecordValues`（仅 `type==2` 写入）、`createRecordFromCursor`、`createRecordDeletionUndoValues`、`applyRecordAssetEffect`、`getTotalByType`/`getTotalByTypeAndDateRange`/`getMonthlyStatistics` 全部按规则处理；`RecordFormState.feeBuffer` 支持草稿恢复。`AmountKeypadController` 新增 `bindTarget`/`selectTarget`/`resetTarget`，金额与手续费共用一个键盘并按当前目标输入。快速与标准布局都把转出/转入改为独立 `transfer_accounts_block`（`row_asset`/`row_destination_asset` 整行大卡 + 交换），支出/收入改用 `row_asset_single`，转账出现 `row_fee`/`edit_fee`；标准模式移除旧的 `removeView/addView` 行重排。`item_record.xml` 新增 `text_fee`，`DateGroupAdapter`/`RecordAdapter` 在转账 `fee>0` 时显示「含手续费」。验证：`:app:testEverydayUnitTest` 通过；`:app:assembleEverydayDebug` 成功；`:app:connectedVerificationDebugAndroidTest` **101/101**（新增 `DatabaseHelperTransferFeeTest` 4 项、`QuickRecordLayoutTest` 键盘目标切换与双布局转账卡用例）。真机（PNM-AN10）复核：快速转账 `transfer_accounts_block [48,437][1032,1201]`、转出/转入卡显示「请选择转出/转入资产」、`row_fee`+`edit_fee` 存在、键盘贴底 `[0,1622][1080,2354]`；标准转账同样显示两张整行大卡与手续费行。验证后偏好保持「快速模式」。


- 2026-09-12：按用户参考图把快速模式改为「顶部类型行 + 分类白卡 + 固定组合面板 + 常驻键盘」的结构，并修复转账模式下键盘悬空。`fragment_add_record_quick.xml` 根节点改为 `ConstraintLayout`：键盘 `bottom→parent.bottom`、面板 `bottom→键盘.top`（间距 12dp）、分类白卡 `top→类型行.bottom` 且 `bottom→面板.top`、高度 `0dp`，因此分类卡在转账时 `GONE` 也不会把键盘推离底部（旧 `LinearLayout` + `weight=1` 占位在 `GONE` 时失效，导致键盘下方出现空白）。唯一组合白卡 `quick_record_panel` 紧贴键盘上方，包含备注(`edit_description`)+`¥`前缀(`text_amount_prefix`)+金额(`edit_amount`，hint `0.00`)、紧凑缩略图条 `card_photo_preview`、以及时间(`row_date`/`text_date`，今天显示「今天」)、资产(`row_asset`/`text_asset_value`，快速模式显示「无账户」)、附件(`btn_take_photo` 整块可点 + `text_photo_count` 显示「附件(n/max)」)；转账时 `row_destination_asset` 与 `btn_swap_transfer_assets` 在第二行可切换。`layout_amount_keypad_quick.xml` 按参考图重排：数字区 `123/456/789/.0再记`，右栏 `⌫/−/+/完成`。`AddRecordFragment` 删除 `quick_record_scroll` 避让逻辑，新增 `rootView` 引用修正 `onCreateView` 阶段 `getView()` 为 null 导致附件计数/分类空态未绑定的问题；缩略图在快速模式收紧到 64dp。`item_category_selector.xml` 名称 14sp、图标 40dp。验证：`:app:testEverydayUnitTest`、`:app:assembleEverydayDebug` 成功；`:app:connectedVerificationDebugAndroidTest` **95/95**（新增键盘贴底回归用例 `quickLayout_keepsKeypadPinnedToBottomWhenCategoryCardIsHidden`）；PNM-AN10 真机层级确认支出模式 `quick_record_panel [48,1237][1032,1586]`、键盘 `[0,1622][1080,2354]`，转账模式分类卡 `GONE` 后 `quick_record_panel [48,1093][1032,1586]`、键盘仍为 `[0,1622][1080,2354]`（底边贴内容区底），`text_date=今天`、`text_asset_value=无账户`、`text_photo_count=附件(0/3)`、`btn_save_and_add` 位于数字区底行、`btn_save` 位于右栏底部。
- 2026-09-12：记一笔/编辑记录新增可切换布局模式。`RecordEntryModePreferences`（`record_entry_mode_prefs` / `record_entry_mode`，默认 `standard`，非法值修复为 `STANDARD`）持久化 `STANDARD` / `QUICK`。「我的」一级页新增「记一笔模式」行（`card_record_entry_mode` / `text_record_entry_mode`），点击进入二级页 `RecordEntryModeSettingsFragment` + `fragment_record_entry_mode_settings`（沿用语言设置的单选交互，返回时 `SettingsFragment.onResume()` 刷新当前值）。`AddRecordFragment` 按偏好 inflate `fragment_add_record` 或 `fragment_add_record_quick`；`EditRecordFragment` 继续继承，因此新增与编辑都跟随模式。
- 快速模式：页内 `recycler_quick_categories`（`GridLayoutManager` 3–5 列）只平铺叶子分类，`CategorySelectorAdapter` 改为稳定 ID + 勾选标记 + 完整路径 contentDescription；金额键盘常驻（`layout_amount_keypad_quick`），数字区 `123/456/789/.0再记`、右栏 `⌫/−/+/完成`。`AmountKeypadController` 新增 `alwaysVisible`、`hideForModal`/`restoreAfterModal`、`hideForSoftKeyboard`/`restoreAfterSoftKeyboard`，键位查找改为 null-safe 以兼容无 `keypad_confirm`/`keypad_hide` 的快速键盘，并保持 `AddAssetFragment` 默认行为不变。备注获焦时临时隐藏金额键盘，日期/资产弹层关闭后恢复；快速模式转账行只切可见性，不复用标准模式的 `removeView/addView` 重排。
- 快速模式不改变业务语义：仍只选叶子分类并保存 `category_id` 与快照；转账仍需转出/转入且不能相同（`saveRecord` 新增统一校验，并校验分类仍为叶子）；不改数据库结构与 `Record` 字段。
- 验证：`:app:testEverydayUnitTest` 通过；`:app:assembleEverydayDebug` 成功；`:app:connectedVerificationDebugAndroidTest` **94/94**（新增 `RecordEntryModePreferencesTest` 4 项、`QuickRecordLayoutTest` 3 项）。真机（PNM-AN10）`uiautomator dump` 复核：设置行默认「标准模式」→ 二级页选中并持久化 → 返回刷新为「快速模式」；快速布局实测 `recycler_quick_categories [72,461][1008,989]`、键盘 `[0,1622][1080,2354]` 常驻、`完成 [804,2156][1050,2312]`、`再记 [804,1988][1050,2144]`（各 82×52dp），`layout_buttons` 与 `keypad_confirm` 按设计不存在。验证后已把偏好切回标准模式。
- 该功能的未覆盖项（不得当 PASS）：TalkBack、字体 2 倍下快速布局排布、深色主题（仓库 `values-night` 为空）。
- 2026-09-12：分类图标浏览改为共享的“左分组 + 右图标网格”组件（`IconBrowserBinder` + `CategoryIconGridAdapter`），图标只显示图形、仍带可访问名称（`icon_featured_labels` 优先，否则英文名）。每组精选约 60–100 个（`IconCategoryCatalog.groups`，共 915），新增/编辑分类、记一笔子分类、账本图标四个入口统一使用 `IconPickerDialog` 的分组浏览；按字母序的全量弹窗（`IconPickerAdapter`/`IconPickerSelectionState`/搜索）已删除。旧图标若不在精选集，选择器顶部显示“当前图标”并可重选；保存键仍是 `tabler_xxx`。补齐 `app/src/main/assets/third_party/tabler_icons/LICENSE`（MIT）。验证：`:app:testEverydayUnitTest` 73 项 0 失败（新增 `IconCategoryCatalogTest`），`:app:assembleEverydayDebug` 成功。
- 2026-09-12：Gradle 变体拆分为 `dev`（日常，applicationId `com.example.cardtally`）与 `verification`（重置数据库的设备测试，applicationIdSuffix `.verification`）；release 使用仓库外 keystore 并加 `.release` 后缀，与 debug 数据隔离。日常命令 `:app:assembleEverydayDebug` / `:app:assembleEverydayRelease` / `:app:testEverydayUnitTest`，隔离设备测试 `:app:connectedVerificationDebugAndroidTest`。签名细节见 `AGENTS.md` 第 6 节。
- 2026-09-11（第七轮）已按第六轮视觉复核修复 UX15（新建账本现有资产组选择区）与 UX16（共用金额键盘浅色主题下字符不可见，P0）。证据见 openspec ux-consistency-handoff 的 implementation-report.md「第七轮实施」；**仍待用户视觉复审，变更未归档**。
- UX16 根因：主题把 framework `Button` 换成 Material3 `MaterialButton`，父样式 `android:background=@empty`，而 `android:background` 一旦设置会让 MaterialButton 跳过 `backgroundTint`，按键最终无背景。已把键盘按键改成 `<TextView>` 并显式声明 `bg_keypad_key` / `bg_keypad_key_primary`（普通键浅底深字、确定键黑底白字，48dp、12dp 圆角）。XML 文本属性不再是判据：`AmountKeypadRenderTest` 用 inflate→measure→`draw(Canvas)` 逐键断言显式不透明表面、对比度 ≥4.5:1、≥48dp、键心出现 ≥2 种像素（真实字形已绘制），并在真实 `MainActivity` + `AddRecordFragment` 的生产 `bind()` 路径上复核。
- UX15：资产关系卡内新增全宽 `ledger_setup_group_row`（14sp「选择资产组」标签 + 16sp 组名 + 12sp「成员 + 资产数」摘要 + 20dp 矢量 chevron，minHeight 64dp，整行可点击可聚焦，带整行 contentDescription），与模式行分离；模式行只保留通用说明，不再重复组摘要；模式行只切模式，只有该行打开 BottomSheet；独立模式整段（含分隔线）隐藏。
- 第七轮验证：静态脚本 73/51 PASS；JVM 78 项 0 失败；`assembleDebug` 成功；隔离设备全套 **87/87**（新增 `AmountKeypadRenderTest` 4 项、`LedgerSetupFormTest` 增至 8 项）；APK 已覆盖安装，`CardTally.db` 仍在。
- 第七轮真机复核（原 BLOCKED 已解除）：用 `uiautomator dump` 取精确 bounds 后实测——点击 `edit_amount [318,545][984,665]` 弹出键盘 `[0,1622][1080,2354]`，15 个按键各 246×156px（82×52dp），`⌫`/`−`/`+`/`确定` 文本均在下发可见，`确定` 为黑底白字；点 `1` `+` `2` → 输入框 `1+2`、按钮变 `=`，点 `=` → `3.00`、按钮回 `确定`。截图 `screenshot/ux16_keypad_light.png`、`ux16_keypad_equals.png`、`ux15_ledger_form.png`。注意：本机第一屏截图是 1080×2420，按 dp 估算坐标会错位（曾被误判为「adb 点击不可靠」），真机操作前先用 `uiautomator dump` 取 bounds。
- 第七轮未覆盖（不得当 PASS）：深色/跟随系统主题（仓库 `values-night` 为空、`ThemeHelper` 恒返回 light，无法给出真实证据，已按主题 token 编写）；字体 2 倍下键盘/表单排布截图；TalkBack；UX15 非主/空组真机截图（由隔离测试覆盖）。

- 2026-09-11（第四轮）已按 OpenSpec ux-consistency-handoff 第6/7/8节实施：第三轮遗留 F1—F6、UX13 账本页统一、UX14 资产组/同组合并/跨账本流水。逐项证据见该目录 `implementation-report.md` 与 `tasks.md`；**最终视觉复审仍待用户安排，变更未归档**。
- 设备测试安全改造：`app/build.gradle` 新增 `verification` flavor（`applicationIdSuffix ".verification"`）；会 `deleteDatabase("CardTally.db")` 的设备测试通过 `testing/IsolatedTestGuard.kt` 只在隔离变体运行。日常命令仍为 `:app:assembleDebug` / `:app:testDebugUnitTest`，`devDebug` 产物镜像回 `app/build/outputs/apk/debug/app-debug.apk`；设备测试用 `:app:connectedVerificationDebugAndroidTest`。若变体名歧义，显式等价任务为 `:app:assembleEverydayDebug` / `:app:testEverydayUnitTest`。
- 已落地数据能力：`getAllRecordsByAssetId`（按 assetId 跨账本、每行一次）、`getAssetGroups`（按稳定池根去重）、`createLedgerInAssetGroup`（单事务）、`validateLedgerMerge` / `mergeLedgerInto(source,target)`（单事务、同组校验、主账本保护、第三方共享引用重定向）；旧的 `mergeLedgerIntoCurrent` 已删除。
- 已落地页面：新建/编辑账本统一白卡并直选独立/现有资产组；账本/资产管理页新增可发现「合并」入口 + 保留目标选择 + 二次确认；资产详情按 assetId 显示跨账本流水，金额下方显示所属账本名（`Record.ledgerId/ledgerName`，仅该页使用）。
- 记录项 `item_record.xml` 改为 `RecordRowLayoutController` 驱动：金额过长或字体放大时移到独立整行，金额用框架 auto-size 且不低于 14sp，名称最多两行；不再省略金额。
- 第五轮闭环验证：静态脚本 73 布局/51 引用 PASS；JVM 78 项 0 失败；`assembleDebug` 成功；隔离设备套件 **80 项全通过**。原 5 个失败均为过期测试契约：资产名称绑定、v9 财务保留和把合法 `transfer` 当非法 token，现已按当前源码/决策修正。
- UX09/UX10 补齐：AI fake 生命周期现含配置缺失且确认 sender 零调用；新增金额键盘→日期/资产/分类真实弹层测试、42 单元日历、640×320dp/2倍字体短视口测量。日期弹层实测底部操作裁切后改为“日历滚动 + 底部操作固定”。
- 未覆盖（不得当 PASS）：最终视觉复审、图标选择后自动关闭的人工真机点验、TalkBack、UX13 全组合人工观感、AI 真实请求链路。该折叠设备的强制横屏坐标跨 display 不一致，不能作为视觉坐标 PASS；自动化改用不旋转设备的横屏布局测量，避免 instrumentation 清理挂起。

- 2026-09-11新增已确认需求：独立/现有资产组选择、同组合并、资产详情跨账本流水及账本名称标签。决策见 `docs/requirements/decisions/2026-09-11-ledger-asset-groups.md`；实施规划见OpenSpec ux-consistency-handoff的UX14与tasks第8节。上述数据层与页面已于第四轮实施，见本文件第一节与实施报告。

### 2026-09-11 第三轮复核与新增账本页规划

- 最新结论见 `openspec/changes/ux-consistency-handoff/recheck-2026-09-11.md`：默认竖屏图标搜索键盘复测通过，选择后不关闭仍复现；第三轮未整体通过。历史测试结果不代表本次重新执行。
- 新建/编辑账本 UX13 已加入该变更的 design、spec 与 tasks 第7节，仍待实施；第6节是第三轮遗留项。本轮仅更新规划，未修改应用代码。
- 用户日常应用不得运行自动卸载目标包的测试流程。测试先核对安装清理行为，优先隔离环境；详细安全说明见最新复核报告。

## 2026-09-09 UX 实施（按 OpenSpec ux-consistency-handoff 执行）

- 实施模型按 `openspec/changes/ux-consistency-handoff/` 的 design.md 与 spec 落地代码，已完成项见同目录 `implementation-report.md`；最终视觉验收仍待用户安排，未自行归档 OpenSpec 变更。
- 已落地：设置行右向 chevron（UX01）；共享二级头 minHeight56/标题22 与 AI 配置页真实相邻布局、资产流水“返回+标题+编辑+更多”及置顶/归档/删除移入菜单（UX02）；UX03 触碰目标加高（记录/资产/分类滑动动作、分类 Tab、日期弹层“选择今天”、记录关闭与拍照、搜索取消/筛选）；信息文本语义色与字号（UX04）；常规主按钮统一黑底 12 圆角并转 MaterialButton（UX06）；图标选择以图标名为稳定选中态 + 勾选标记 + 选中朗读，新增纯 Kotlin 选择模型与单测（UX07）；图标浏览受限高双列滚动 + 全入口搜索 + 分组标题本地化与精选标签（UX08，部分精选标签未覆盖全部组内图标）；记一笔打开弹层前收起系统键盘并做图标选择器可用高约束（UX09，只实现代码侧，组合未全测）。
- 静态脚本 `scripts/verify-ux-resources.ps1` 通过（71 布局 / 49 引用）；`testDebugUnitTest` 55 项 0 失败；`assembleDebug` 成功；最新 `app-debug.apk` 已安装到当前设备。截图证据存于根目录 `screenshot/`（`ux01_settings_after.png`、`ux09_date_sheet.png`）。
- 风险类（UX05 长金额、UX09 多种键盘/横屏/大字体组合、UX11 若干可达页面硬编码文案）只完成代码侧可达部分，未声明全部组合真机通过；具体逐项 PASS/FAIL/BLOCKED 见 `implementation-report.md`。

### 2026-09-09 复审返工（review.md 的 R1—R5）

- 审查给出 `review.md`：暂不通过，列 R1（图标搜索键盘遮住取消）、R2（停止后重发缺少请求身份隔离）、R3（停止回复未同步消息适配器）、R4（精选图标浏览未文字化）、R5（未实施项与测试数量）。
- 返工已落地：图标选择器改为 `BottomSheetDialog` 并监听窗口 insets/IME 重排网格（R1，已在真机验证键盘弹起后关闭按钮/网格位于键盘上方）；`MiniMaxClient` 每请求独立取消句柄 + `AiRequestIdentity` 请求身份过滤停止后的迟到回调（R2，含 `AiChatSender` 接口与 `AgentFragment.senderFactory` 测试注入口）；停止时同步 `AgentChatAdapter.finalizeStreamingMessage` / 空占位 `discardStreamingPlaceholder`（R3）；精选图标只显示有中英标签的项、12sp、选中分组独立底色、按可用高度夹紧、别名搜索（R4）；周期弹层与账本保存按钮统一 12 圆角、长金额布局测量测试、空资产“入口隐藏→指向我的→账户资产”动态指引（R5）。
- 返工后：`testDebugUnitTest` 为 **68 项、0 失败**；`RecordRowLayoutMeasurementTest`（androidTest，仅 inflate+measure，不写用户数据）2 项设备测试通过；`assembleDebug` 成功；静态脚本通过；APK 已重装。图标选择器键盘弹起验证：关闭按钮 y=807—951、网格底 y=1491，均在键盘上方。
- 仍待办：UX09 多键盘/横屏/大字体矩阵、UX11 剩余硬编码与空资产隐藏入口动态指引（已完成该指引）、UX07/UX10 适配器点击+保存链路与 AI fake 生命周期 Fragment 级测试；逐项见实施报告。


## 2026-09-09 UX 一致性检查

- 续检：AI 页改为自适应垂直布局，配置提示卡可滚动。新增 `AgentLayoutIsolationTest` 三项设备布局测试通过，覆盖大字体、多行输入、长消息、流式文本更新和配置卡滚动；不访问数据库/API。完整键盘、聊天生命周期及非空财务页面仍未验证，见检查记录。

- 已将用户最新灰底、纯白圆角、无主体卡片描边/阴影基准补入 `DESIGN.md`，新增共享 `bg_card_surface` 与次要操作背景。
- 本轮统一 API 配置、AI 会话、日期/资产/分类/周期弹窗、资产流水摘要、资产表单、账本/分类面板及录入底栏等残留样式；保留输入边界和语义色。
- 新增 `scripts/verify-ux-resources.ps1`；当前静态检查通过（71 个布局解析，49 个代码/include 引用布局）。
- 本次最终 `assembleDebug` 成功；`testDebugUnitTest` 为 49 项、0 失败、0 错误。最终 APK 已安装，资产空状态说明已在设备层级中确认可见。
- 页面覆盖、已做设备检查与未验证状态详见 `ux-consistency-audit.md`。不应将本轮描述为全部页面所有交互已通过真机回归。

以下内容用于帮助后续 AI 快速识别仓库的最近实现状态，避免把已落地的内容继续误判为"规划中"。

## 2026-04-06 已落地状态

- 旧的 `The Curated Chronicle / 静奢理财日记` 视觉方向已废弃；当前视觉契约为根目录 `DESIGN.md`。
  - `app/src/main/res/values/colors_light.xml`
  - `app/src/main/res/values/styles.xml`
  - `app/src/main/res/drawable/bg_circle_primary_container.xml`
  - `app/src/main/res/drawable/bg_summary_item.xml`
- 底部导航已加入 `AI Agent` 入口，并新增 `AgentFragment.kt` 与 `fragment_agent.xml`
- 以下页面已完成一轮明显的 UI / 信息层重设计：
  - `AddRecordFragment` / `fragment_add_record.xml`
  - `AssetFragment` / `fragment_asset.xml` / `item_asset.xml`
  - `StatisticsFragment` / `fragment_statistics.xml` / `item_statistics.xml`
  - `SettingsFragment` / `fragment_settings.xml`
- 旧版 Stitch 设计导出与“静奢理财日记”视觉指南已移除；当前视觉入口为根目录 `DESIGN.md`。

## 2026-04-06 本地构建修复

- 修正 `app/src/main/res/values/colors_light.xml` 中的非法颜色值 `#Transparent`
- 补齐新布局实际引用到的兼容颜色别名
- 修正 `app/src/main/res/layout/fragment_asset.xml` 中非法的 `android:gravity="baseline"`
- 在以上修复后，`./gradlew assembleDebug` 和 `.\gradlew.bat assembleDebug` 已验证可通过

## 2026-04-07 最新代码现实

- `MainActivity` 继续统一承载浮动底部导航，不在页面内部重复实现导航
- 底部导航文案已调整为中文：`账单 / 统计 / 资产 / AI 助手 / 我的`
- `home / assets / agent / me / records` 的当前实现仍需以源码复核，不再以历史 Stitch 导出作为视觉依据
- `Agent` 页底部输入区已上移，避免与活动级底部导航重叠
- `EditRecordFragment` 已不再维护独立 UX 布局，而是直接复用 `fragment_add_record.xml`
- `fragment_edit_record.xml` 已移除，新增记录与编辑记录现在共享同一套录入 UX 基准
- `EditAssetFragment` 已切换为复用 `fragment_add_asset.xml`
- `fragment_edit_asset.xml` 已移除，新增资产与编辑资产现在共享同一套录入 UX 基准
- 应用已接入应用级中英文国际化，当前支持 `中文 / English`
- 语言切换入口位于"我的"页，并在切换后立即全局生效
- 国际化基础设施集中在 `LanguageHelper.kt`、`app/src/main/res/values/strings.xml`、`app/src/main/res/values-en/strings.xml` 和 `app/src/main/res/xml/locale_config.xml`
- 语言切换当前已做过一轮减闪处理：去掉额外 `recreate()`、补了无动画窗口切换、过渡遮罩，以及避免重复 `setApplicationLocales(...)`
- 但真机从“我的”页切换中英文时仍会轻微闪屏；当前只能算“减轻”，不能算“解决”
- 该问题后续期望方案是更平滑的淡入淡出过渡；如果继续处理，优先沿 `SettingsFragment -> LanguageHelper -> MainActivity/activity_main.xml` 这条链路排查
- "我的"页已新增 `AI 助理` 分组，包含 AI 入口显示开关与 `MiniMax 配置` 二级设置页
- AI 显示开关当前会控制底部 `AI 助手` tab 的可见性，并在关闭时阻止继续停留在 Agent 页面
- `AgentFragment` 已从静态示例页切换为 MiniMax BYOK 聊天页；当前请求默认携带 `stream=true`
- AI 客户端会按实际响应内容识别流式 / 非流式返回：如果收到 SSE / chunk 形态内容，则增量刷新当前 Assistant 气泡；如果返回完整 JSON，则回退为一次性解析完整回复
- `AgentChatAdapter` 已支持对当前流式消息做 payload 级内容刷新，避免每个 chunk 都走整列表重绘
- AI 页面当前支持区分 `TIMEOUT / CANCELLED / INTERRUPTED / NETWORK` 等失败态，不再统一映射为普通网络失败
- 对于取消、超时或中断这类场景，如果 Assistant 内容已经部分到达，当前会优先保留已收到的回复片段，而不是直接丢弃
- AI 设置状态当前使用 `AiAssistantSettingsHelper.kt` 持久化到本地 `SharedPreferences`，并保存 `API Key / 模型 / 完整请求 URL`
- AI 设置中的 URL 语义已改为"完整请求 URL"，客户端不再自动拼接固定 MiniMax endpoint

## 2026-04-09 最新代码现实

- `AgentFragment` 已从"单会话内存态"升级为"SQLite 持久化多会话聊天页"
- AI 助手会话和消息当前由 `DatabaseHelper` 持久化，新增了会话与消息表；切换页面或重启应用后，会恢复上次活动会话与历史消息
- `AiAssistantSettingsHelper.kt` 当前除保存 `API Key / 模型 / 完整请求 URL` 外，也会保存当前活动会话 ID
- `AgentFragment` 左上角菜单已改为本地左滑会话栏入口，可切换历史会话
- `AgentFragment` 右上角头像位已替换为 `+` 号，用于新建会话；默认会话命名格式为"新会话-年月日"
- 会话项当前支持长按重命名，列表渲染由 `AgentSessionAdapter` 驱动
- AI 回复在流式完成后会落库；取消、超时或中断时，如果已有部分回复内容，也会按错误态消息保留下来
- 当前仓库已新增真实测试文件，不再是"只有测试依赖、没有实际测试"状态；现有测试覆盖会话默认命名、AI 会话/消息 SQLite 持久化，以及活动会话 ID 偏好存储

## 2026-04-10 最新代码现实

- 底部导航当前由 `MainActivity` 统一控制一级页 / 二级页显隐；`nav_shell` 会在 `Ledger / Statistics / Asset / Agent / Settings` 等一级页显示，在新增 / 编辑 / 配置等二级页隐藏
- `AgentFragment` 当前在会话抽屉展开时会临时隐藏 `nav_shell`，关闭抽屉后恢复，避免抽屉与底部导航叠层冲突
- Agent 页底部输入区当前已改为与 `nav_shell` 使用同样的左右边距；输入壳静态高度为 `56dp`，发送按钮为 `48dp`，用于保持与浮动底部导航更一致的容器比例
- 底部导航 active indicator 已切换为透明，不再依赖浅白色块高亮当前 tab
- 主题设置当前只保留 `浅色 / 深色 / 跟随系统` 三档；`fragment_custom_theme.xml` 与蓝 / 绿 / 橙彩色主题资源已移除
- `ThemeHelper.kt` 当前会对旧的彩色主题存档值做兜底回退，避免历史 `theme_mode` 越界继续污染运行时
- 当前仓库已新增 `MainActivityThemeApplicationTest` 与 `ThemeHelperTest`，用于验证主题回退与非 AI 页面深色模式应用
- 深色模式修复方向已经从"只在 Agent 页使用主题属性"扩展到更广泛的布局 / Adapter / 资源层：当前已通过主题属性、`values-night/colors_system.xml` 和 `values-night/colors_legacy_light_overrides.xml` 开始收口旧的 `*_light` 直接引用
- "记一笔"金额输入当前默认显示 `0.00`；当该默认值尚未被改动时，点击或聚焦金额框会自动选中默认值，便于直接覆盖输入，同时保存后的金额清空逻辑也会回到 `0.00`

## 2026-04-14 最新代码现实

- 分类系统已升级为**树形层级结构**，支持任意深度（默认最大深度 2，可在设置中调整至 50）
- `categories` 表已新增 `parent_id` 字段，`records` 表已新增 `category_id`、`category_name_snapshot`、`category_path_snapshot` 字段
- 记账记录现在通过 `category_id` 绑定到**叶子分类**（无子分类的分类），并保留快照用于历史追溯
- `CategoryManageFragment` 支持层级缩进展示、父分类选择、删除保护（有子分类或关联记录时禁止删除）
- 新增 `CategoryHierarchySettingsHelper.kt` 用于管理分类层级深度设置（`getMaxCategoryDepth()` / `setMaxCategoryDepth()` / `sanitizeCategoryDepth()`）
- `SettingsFragment` 新增「分类层级上限」设置入口，支持 1-50 的深度配置
- `AddRecordFragment` / `EditRecordFragment` 已改为仅选择叶子分类，并保存 `category_id` 和快照
- 兼容历史数据：唯一命中的分类名称会自动回填 `category_id`；模糊匹配时保留快照，`category_id` 设为 null
- 新增测试覆盖：
  - `DatabaseHelperCategoryTreeTest` - 分类树的 CRUD、层级计算、环检测、删除保护
  - `DatabaseHelperRecursiveCategoryMigrationTest` - 历史数据迁移与回填逻辑
  - `DatabaseHelperRecursiveCategoryQueryTest` - 递归查询方法
  - `CategoryHierarchySettingsHelperTest` - 深度设置的持久化与边界值
- 新增实施计划文档：`docs/requirements/plans/2026-04-13-recursive-category-id-demo-plan.md`
- `activity_main.xml` 中的底部导航壳已调整为更明确的卡片式容器：使用 `MaterialCardView` 承载导航、带 1dp 描边和轻阴影，并将选中项恢复为柔和的胶囊型 active indicator，避免导航看起来像贴底的纯平条带

## 2026-04-16 最新代码现实

- “账本”一级页当前仍使用 `StatisticsFragment`，未拆出独立 `RecordsFragment`
- `StatisticsFragment` 页内已新增 `统计 / 明细` 双视图切换
- 统计视图当前展示分类维度的收入 / 支出聚合结果
- 明细视图当前通过 `DatabaseHelper.getAllRecords()` 读取全部记录，并按日期分组后交给 `DateGroupAdapter` 渲染
- 账本明细视图已复用现有记录编辑与删除交互；删除后会在当前页内刷新列表
- `fragment_statistics.xml` 已从单一统计列表壳更新为带轻量切换控件的账本页壳
- 账本页右上角当前已改为锚点式下拉菜单，用于切换 `统计（支出）/ 统计（收入）/ 明细`，不再使用底部抽屉切换该模式
- 账本折线图当前在 `周 / 月` 周期下，横轴仅显示“日”数字，不再重复显示“月”信息；`年` 周期仍按月份显示
- 当周期为 `ALL / CUSTOM` 时，图表模式会回到饼图，右上图表切换控件会隐藏但保留占位，避免“趋势概览”副标题发生纵向跳动
- 当周期为 `CUSTOM` 时，顶部 `周 / 月 / 年 / 全部` 按钮当前允许无选中态，不再强制高亮某个预设周期
- 从账本明细进入“记一笔”后返回，当前会重新绑定 `recyclerRecords.adapter`，避免因 Fragment View 重建导致列表空白
- 账本明细记录项保留按日期分组与编辑/删除交互，视觉语言、间距和信息排布以当前账本实现为准

## 2026-04-16 录入页最新现实

- `AddRecordFragment` / `EditRecordFragment` 当前继续共享 `fragment_add_record.xml`
- “记一笔 / 编辑记录”主页面当前结构仍以源码为准，后续视觉重做遵循根目录 `DESIGN.md`
- 日期选择已从系统直接弹窗切换为底部抽屉中的 `DatePicker`
- 资产选择已从页内 `Spinner` 切换为底部抽屉列表，当前由 `RecordAssetSheetAdapter` 渲染
- 分类选择已从页内网格切换为树形底部抽屉，当前由 `RecordCategoryTreeAdapter` 渲染，并支持任意深度展开
- 当前录入页仍只允许选择**叶子分类**，并继续保存 `category_id`、`category_name_snapshot`、`category_path_snapshot`
- `AddRecordFragment` / `EditRecordFragment` 当前已统一通过 `nav_shell` 控制二级页进入时隐藏底部导航，而不是单独操作 `bottom_navigation`
- 普通入口进入“记一笔”时，返回会回到发起页面；如果开启“快捷记账”后冷启动直接进入“记一笔”，当前返回会落到账本页
- 当前录入页的下一步精修重点已收敛为：分类抽屉视觉层级、资产抽屉信息呈现、金额区与底部保存区留白

## 2026-04-16 协作技能最新现实

- 根目录已新增 `skills/`，用于存放可直接执行的本地协作 skill，而不是继续把所有 skill 都放在 `docs/collaboration/skills/`
- 当前已提供 3 个根目录 skill：`skills/android-build-debug.md`、`skills/android-install-debug-apk.md`、`skills/adb-current-screen-screenshot.md`
- ADB 截图 skill 当前默认把留档截图保存到根目录 `screenshot/`
- 部分设备存在多 display；如果默认 `screencap` 出现黑图，当前应先执行 `adb shell dumpsys SurfaceFlinger --display-id`，再改用 `screencap -d <display-id>`

## 环境注意事项

- `local.properties` 属于本机环境文件；当前构建依赖其中的 `sdk.dir` 或等效 Android SDK 环境变量，不要提交该文件
- Android Gradle 验证在同一工作区内默认串行执行；不要并行跑任何 `gradlew` / Gradle 任务，尤其不要并行跑 `assembleDebug`、`testDebugUnitTest`、`connectedDebugAndroidTest` 这类共享 `app/build/` 产物的任务，避免因中间产物互踩、命令结果被错误消费、或 `Tool execution aborted` 而误判

## 2026-08-29 交接快照

以下是当前工作区最近一轮 UI、分类图标和录入交互修改的实现事实。后续 agent 仍需回到源码确认细节，不要只依据本节文字。

### 分类图标与列表

- 分类图标系统已迁移到 Tabler Icons：`TablerIconCatalog` 负责资源映射；旧的 `MaterialSymbolCatalog` 及其资源已移除。
- 分类管理、分类选择、账单记录、统计排行、搜索结果和资产选择等涉及分类的列表，当前优先使用分类保存的图标名，并保留名称 / ID 兼容回退。
- 一级分类管理页面已使用实际绑定图标，不再统一显示占位四格图标。
- 搜索记录条目已补齐分类图标解析：优先分类 ID 图标，通用占位图标时按分类名称回退。

### 录入页与编辑页

- `EditRecordFragment` 继续复用 `fragment_add_record.xml`；“记一笔”和“编辑记录”使用同一套录入壳和底部抽屉交互。
- 支出 / 收入 / 转账切换卡片使用独立的 `bg_record_type_tabs.xml`，填充色为不透明 `#FFFFFF`，不再使用 `bg_summary_item.xml` 的半透明填充；选中态仍通过文字加粗和底部指示线表达。
- 录入页主信息卡片使用 `@color/surface_light`（浅色为 `#FFFFFF`），描边宽度为 `0dp`；内部行分隔线仍保留。
- 金额输入当前源码行为：未编辑时默认显示 `0.00`；进入金额输入时清空该默认值；退出输入且仍为空时恢复 `0.00`。如果后续要完全改成“添加备注”式占位提示，需要继续修改 `AmountKeypadController` 与金额字段资源，当前尚未完成该改法。
- 点击日期、资产或分类前，`AddRecordFragment` 会先通过 `AmountKeypadController` 收起金额键盘，避免键盘与 BottomSheet 叠加。
- 编辑账本时已隐藏共用资产、独立资产、资产来源账本及资产来源列表；新建账本仍保留资产关系设置。

### 日期与资产 BottomSheet

- 资产选择 BottomSheet 已移除“无”选项；新建记录首次打开时不默认选中任何资产。
- 资产条目使用白色 `12dp` 圆角卡片、无描边、账单条目风格的图标 / 间距 / 字体；选中状态使用勾选标记而非边框强调。
- 日期选择 BottomSheet 的 compact 日期单元格为 `36dp` 高，日期数字区域为 `32dp × 28dp` 并水平、垂直居中；金额在该日期选择器中不显示。
- 日历页选中日期下方的记录区域使用白色 `12dp` 圆角卡片，空记录时隐藏整张记录卡片而不是留下空白容器。
- 该日历卡片曾发生过 ID 误绑定导致整个月历被隐藏的问题，当前 `card_calendar_records` 已绑定到下方记录卡片，修改时注意不要再次复用错误节点。

### 颜色与卡片约定

- 浅色主题主体卡片前景色统一以 `#FFFFFF` 为基准（`@color/surface_light` / `@color/editorial_surface_lowest`）。页面背景通常为 `#EEEEEE`，不要将页面背景误当成卡片前景色。
- 记账记录、统计、资产、设置、分类管理等主体卡片当前使用白色；收支 / 转账图标底色仍按 `IncomeExpenseColorScheme` 和转账黄色语义显示。
- 涉及视觉调整时继续遵守 `DESIGN.md` 的卡片分组、圆角、无阴影 / 无描边要求；不要把按钮或状态容器的彩色底误改成主体卡片颜色。

### 构建与交接状态

- Debug APK 输出名已从 `CardTally-debug.apk` 改为 `app-debug.apk`。
- 最近一次验证命令：`.\gradlew.bat assembleDebug`，结果为 `BUILD SUCCESSFUL`。
- 最近一次产物路径：`app/build/outputs/apk/debug/app-debug.apk`。
- 当前工作区存在大量既有 UI、Tabler 资源、数据库和文档改动，交接 agent 不应使用 reset / clean / checkout 等破坏性操作清理工作区。
- 本轮主要改动文件包括：`AddRecordFragment.kt`、`CalendarFragment.kt`、`LedgerSetupFragment.kt`、`DateGroupAdapter.kt`、`LedgerDateGroupAdapter.kt`、`LedgerCalendarAdapter.kt`、`RecordAssetSheetAdapter.kt`、`AmountKeypadController.kt`，以及对应的录入、日历和资产 BottomSheet 布局资源。
- 尚未完成自动化真机回归；需要在可用 Android 设备上重点验证：金额键盘切换 BottomSheet、日期单元格布局、资产无默认选择、日历空记录状态、编辑账本隐藏资产关系区域，以及分类图标在搜索 / 账单中的显示。
