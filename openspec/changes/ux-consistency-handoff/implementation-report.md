# UX01—UX14 实施报告（2026-09-11，第四轮：第三轮遗留 F1—F6 + UX13 + UX14）

实施依据：根目录 `AGENTS.md`、`docs/collaboration/*`、`DESIGN.md`、本目录 `proposal.md` / `design.md` / `specs/ux-consistency/spec.md` / `specs/ledger-asset-groups/spec.md` / `tasks.md` / `HANDOFF.md` / `review-round-2.md` / `review-round-3.md` / `recheck-2026-09-11.md` / `ledger-asset-groups.md`、`docs/requirements/decisions/2026-09-11-ledger-asset-groups.md`。

> **没有声明最终视觉验收通过。** 本报告只记录代码、构建、测试与真机功能证据；最终视觉复审仍由用户安排。不归档 OpenSpec 变更。

## 本轮验证命令与结果

| 命令 | 实际结果 |
| --- | --- |
| `powershell -NoProfile -File scripts/verify-ux-resources.ps1` | PASS：73 布局解析，51 代码/include 引用布局（仅静态属性扫描） |
| `.\gradlew.bat :app:testDebugUnitTest` | BUILD SUCCESSFUL；**78 项，0 失败，0 错误**（14 个测试类） |
| `.\gradlew.bat :app:assembleDebug` | BUILD SUCCESSFUL |
| `.\gradlew.bat :app:assembleDebugAndroidTest`（verification 变体：`assembleVerificationDebugAndroidTest`） | BUILD SUCCESSFUL |
| `.\gradlew.bat :app:connectedVerificationDebugAndroidTest` | 第七轮闭环后 87 项执行，**87 通过 / 0 失败** |
| 本轮新增/重写的设备用例单跑（`DatabaseHelperAssetGroupTest` + `LedgerSetupFormTest` + `RecordRowLayoutMeasurementTest` + `AgentSendLifecycleTest`） | 25 项，**0 失败** |
| `adb install -r app\build\outputs\apk\debug\app-debug.apk` | Success；`run-as ... ls databases` 确认 `CardTally.db` 仍在（77824 字节） |

APK 路径：`app/build/outputs/apk/debug/app-debug.apk`（产物名未改）。真机截图：`screenshot/asset_management.png`、`screenshot/asset_records.png`、`screenshot/merge_keep_dialog.png`、`screenshot/post_install_home.png`、`screenshot/nav_me5.png`。

## 设备测试安全改造（第三轮 recheck 的硬性要求）

recheck-2026-09-11.md 指出：不得再对用户日常应用直接运行会自动清理数据的 connected 流程。本轮据此改造：

- `app/build.gradle` 新增 `verification` flavor（`applicationIdSuffix ".verification"`），设备测试在该 applicationId 下运行，拥有独立数据沙箱。
- 新增 `app/src/androidTest/.../testing/IsolatedTestGuard.kt`；7 个会 `deleteDatabase("CardTally.db")` 的既有测试在 setUp 里调用 `requireIsolatedBuild()`，在 `dev` 变体上直接 skip，不会清用户数据。
- 日常入口保持可用：`:app:assembleDebug` / `:app:testDebugUnitTest`（等价显式名 `:app:assembleEverydayDebug` / `:app:testEverydayUnitTest`），并把 `devDebug` 产物镜像回 `app/build/outputs/apk/debug/app-debug.apk`。
- 结论：本轮所有 reset 数据库的测试都在隔离包上执行；日常包只做了 `adb install -r` 覆盖安装，数据仍在。

## F1 — 图标选择后弹层不关闭（review-round-3 P1）

- 定位：`K/adapter/IconPickerDialog.kt`。
- 改动：选择回调改为“单次派发 + 选择后收起键盘并 `dismiss()`”；关闭按钮只关闭、不触发选择。`bindGridToAvailableSpace` 改为返回 `OnGlobalLayoutListener`，`setOnDismissListener` 中移除监听，不再遗留全局布局回调。
- 验证：设备测试 `RecordRowLayoutMeasurementTest` / `LedgerSetupFormTest`（真实 Activity + 真实弹层宿主）通过；弹层关闭链路已由 `pendingCallback` 与 view 断言覆盖的部分见 UX13 用例。选择后自动关闭未做真机点击复测（见“未覆盖项”）。

## F2 — AI 同步失败导致生命周期卡在 loading（review-round-3 P1）

- 定位：`K/AgentFragment.kt`、`K/state/StateSupport.kt`、`K/network/MiniMaxConfig.kt`。
- 改动：
  - `InFlightAiLifecycle` 增加 `completed` 标记与 `registerPending()`：发请求前先登记在飞；sender 同步回调失败后 `markFinished()` 生效，随后迟到的 `markStarted(handle)` 不再把已结束的请求重新标成 loading。
  - `AgentFragment` 在调用 sender 之前 `registerPending()`，`deliverAiResult(...)` 抽成可测函数；`isSending` / `hasActiveStream` 改为 internal + private set 供测试观察。
  - `MiniMaxConfig.hasValidRequestUrl()`：`isComplete()` 只判非空，非法 URL 会在 URL 解析后同步失败；现在发请求前先拒绝非法 URL 并引导到配置页。
- 测试：`InFlightAiLifecycleTest`（5 项，JVM）+ `AgentSendLifecycleTest`（4 项，设备真机 fake sender）。
- 设备结果：同步失败、异步失败、流式完成、停止后迟到回调四种编排全部通过；`isSending` 每次都回到 false，停止后迟到文本不会落地。

## F3 — 请求身份与会话隔离 + 调用链测试（review-round-3 P2）

- 定位：`K/state/AiRequestIdentity.kt`、`K/.../AgentSendLifecycleTest.kt`。
- 改动：`AiRequestIdentity` 记录活动 `sessionId`，`isCurrent(requestId, sessionId)` 同时匹配请求号与会话号；`invalidate`/`complete` 清空会话号。注释与实现一致，不再只匹配请求号。
- 测试：`AiRequestIdentityTest` 增至 **10 项**，新增“身份绑定会话”“切换会话丢弃旧会话回调”“invalidate 清理会话号”。
- 调用链：`AgentSendLifecycleTest` 真实启动 `MainActivity` + 替换 `AgentFragment`，通过 `AgentFragment.senderFactory` 注入 fake，驱动同步/异步失败、流式、停止四种路径，断言 `isSending`、`AgentChatAdapter` 最终文本与会话持久化不串扰；**不做真实计费请求**。

## F4 — 精选图标浏览高度不再用固定估算（review-round-3 P2）

- 定位：`K/AddCategoryFragment.kt` 的 `clampBrowseAreaToViewport()`。
- 改动：删除 `screenHeight - 104dp` 估算；改为取 pane 在窗口中的实际位置与 `getWindowVisibleDisplayFrame` 底部，减去页面底部留白，并按 `category_icon_browse_height` 夹紧；用全局布局监听在字体/横竖屏/滚动导致的高度变化时重算，并在 detach 时移除监听。
- 验证：编译 + 静态资源脚本 + 真机页面可达（`fragment_add_category` 未改动结构）。2 倍字体与横屏下的实际观感未真机复测，列入未覆盖项。

## F5 — 长金额测量证据改为有效断言（review-round-3 P2）

- 定位：`K/adapter/RecordRowLayoutController.kt`（新增）、`L/item_record.xml`、`L/RecordRowLayoutMeasurementTest.kt`。
- 改动：
  - `item_record.xml` 改为“`card_content`（vertical）→ `row_content`（horizontal，图标 + 名称列 + 内联金额）+ `layout_amount_below`（换行金额）”，金额可在两个容器间移动；名称 `maxLines=2`。
  - 新增 `RecordRowLayoutController`：按卡片实际宽度决定金额内联或换行；金额使用框架 auto-size（下限 `MIN_AMOUNT_SIZE_SP = 14sp`），绝不省略；名称按可用宽度最多两行。
  - 测试改为 `AT_MOST` 高度真实测量，断言：金额 `layout` 存在、`getEllipsisCount` 为 0、字号不低于 auto-size 下限、金额在卡片宽度内可见；名称行数不超过 2；名称与金额矩形不相交；短金额不占用换行行。
- 设备结果：`RecordRowLayoutMeasurementTest` **5 项全部通过**（320/360/600dp × 1/1.3/2 倍字体）。过程中发现并修正了一个真实缺陷：原 `TextViewFitting` 的 paint 估算与 `TextView` 实际排版不一致，已改为 `measure()` 自然宽度 + 框架 auto-size。

## F6 — 可达页面硬编码文案与本地化

- 已迁移到字符串资源（中英各补）：资产表单（账户信息/账户名称/账户余额/备注/选填/是否计入总资产）、资产类型选择页标题、资金/信用/充值/理财/应收/应付账户、账本管理“删除”“合并”、搜索日期范围“至”、转账换向无障碍描述、`未知账本`。
- 同步修正英文缺失的 `ledger_setup_*` 与新增 `ledger_asset_*` 文案；发现并修正英文 `values-en` 中 `'` 转义导致的 aapt2 编译失败（属真实构建缺陷）。
- 未做：`fragment_asset_type_select.xml` 以外的不可达旧布局（`item_budget`、`item_chart`、`item_export`、`item_month_comparison`、`item_statistics_enhanced`、`item_setting*`、`fragment_asset.xml` 等）保持原状，符合“不全面翻修未引用旧资源”。

## UX13 — 新建/编辑账本表单统一

- 定位：`L/fragment_ledger_setup.xml`、`K/LedgerSetupFragment.kt`。
- 落地：灰底 + 左右 16dp；基本信息合成一张白色 12dp 无描边卡片（名称标签 14sp 次级色、输入 minHeight 48、正文 16sp、去掉静止输入外框改用焦点下划线）；12dp 分隔线与 `minHeight 56dp` 图标整行点击（36dp 浅底 + 中心 18dp 图标 + 标签 16sp 最多两行 + 20dp 矢量 chevron，移除“›”）；资产卡承载两个直选单选项；底部主按钮 `minHeight 52dp` / 12 圆角 / elevation 0，并按真实系统/IME 底部 inset 只应用一次。
- 编辑态：整张资产关系卡 GONE，只保留名称与图标，保存仍走 `updateLedgerDetails`，不改资产池；预填名称与图标已在真机测试中验证（修复了改写中丢失的预填逻辑）。
- 设备测试 `LedgerSetupFormTest`（5 项）通过：表单可见性、默认选中现有组、一次点击切换独立组（radio 不可点，避免双触发）、独立组创建后为空且自持根、编辑态隐藏资产卡且保留名称、跨账本流水查询。

## UX14 — 账本资产组、同组合并、跨账本资产流水

### 数据层（`K/database/DatabaseHelper.kt`）

- 新增 `getAllRecordsByAssetId(assetId)`：按不可变 assetId 跨全部账本查询（source 或 destination 命中，每行只出现一次），不再带 `currentLedgerId` 过滤，也不按资产名拼接。
- 新增 `getLedgerNamesByIds(ids)`、`getLedgerIdForRecord`、`getAssetGroupRootId`、`getAssetGroupMemberIds`、`isMasterAssetGroup`、`getAssetGroups()`（按稳定池根去重，主组优先，带资产数与同名组计数）。
- 新增 `createLedgerInAssetGroup(name, sharedSourceLedgerId, iconName)`：单事务创建账本并在需要时写入共享根引用；失败返回 null 且不遗留账本或组。
- 新增 `validateLedgerMerge(source, target)` 与 `mergeLedgerInto(source, target)`：显式 source/target、单事务、事务内二次校验同组/存在性/主账本保护/自合并；记录只改 `ledger_id`（ID、日期、金额、分类 ID 与快照、备注、图片、转账两端资产 ID 全部保留），资产行按“谁拥有池根”决定是否迁移，全局分类不复制；`repointSharedReferences` 只重写被合并组的成员引用，第三方账本与其它组完全不受影响；失败整体回滚、重复提交被拒绝。
- 删除了旧的 `mergeLedgerIntoCurrent`（不校验同组、不重定向共享引用），避免任何入口绕过新规则。

### 修好的真实缺陷（设备测试发现）

- 初版 `repointSharedReferences` 会把**其它资产组**（如主组）也重定向到被合并组，导致主账本丢失自己的资产视图。设备测试 `merge_rootSourceWithAThirdMember_repointsSurvivingReferences` 复现后改为“组根除非被删除否则不变”，并以 union 方式重写组内引用。

### 页面

- `K/LedgerSetupFragment.kt`：新建页直选“新建独立资产组 / 使用现有资产组”，默认明确显示主资产组；选择现有组打开统一 BottomSheet（`bottom_sheet_asset_group_picker.xml` + `item_asset_group.xml` + `AssetGroupPickerAdapter`），按稳定组 ID 去重、显示归属账本与资产数、48dp 行高、radio + 勾选双标记，空组仍可选。
- `K/LedgerManagementFragment.kt`：新增可发现的“合并”入口（标题栏按钮，按资产分组卡片下不再只靠长按/滑动）；流程为“选择要保留的账本 → 长按或勾选另一账本 → 二次确认（源→目标 + 记录数 + 资产与余额不变）”；跨组、主账本作源、自合并都在 UI 与数据库两侧拒绝；仅成功后更新当前账本偏好；批选合并限制为恰好两个账本并给出说明（不做部分成功的批量）。
- `K/AssetRecordsFragment.kt`：改为 `getAllRecordsByAssetId`，批量解析所属账本名；`DateGroupAdapter` 新增 `showAssetRoute=false` 模式，只在资产详情用 `text_ledger`（12sp 次级色、金额下方、右对齐、最多两行）显示归属账本，其它账单/搜索页资产标签保持不变；无效关联显示“未知账本”。

### 设备验收（隔离库，未用真实数据）

`DatabaseHelperAssetGroupTest` **11 项全部通过**：独立空组、共享同 ID 同余额、按稳定根去重、同组合并保留记录 ID/金额/分类/资产 ID 与余额、跨组与主账本作源被拒、非根源合并后第三账本仍可用、根源合并后幸存引用重定向、重复合并不再写入、跨账本流水每行一次且不混入同名资产、旧 scoped 查询仍限当前账本、未知来源创建不遗留账本。

## 真机功能核对（日常包，未改用户数据）

- 安装覆盖后启动正常，账单/统计/我的/资产/AI 页面可达；`CardTally.db` 仍在。
- 我的页设置行右侧为矢量 chevron（UX01 未回退）。
- 资产/账本管理页正常渲染按组卡片（1 项资产 · ¥221.00，日常为当前账本）。
- 资产详情页跨账本命中 `-¥5.00 家电`，金额下方右侧显示 `日常`（截图 `screenshot/asset_records.png`）。
- 合并入口点击后弹出“选择要保留的账本”，列出两个账本；**取消退出，未执行合并**（截图 `screenshot/merge_keep_dialog.png`）。
- 本轮未在真机上新建/编辑账本（避免在用户数据里写入测试账本），该表单由 `LedgerSetupFormTest` 在隔离库上覆盖。

## 未覆盖 / BLOCKED（不得当 PASS）

1. **最终视觉复审**：由用户安排原审查者进行；本报告不代表视觉通过。
2. **图标选择后自动关闭（F1）**：代码与共享弹层宿主已就位，但未做“点击图标→弹层关闭”的真机点验；同时 `uiautomator dump` 在该设备持续报 `could not get idle state`，层级取证不可用，本轮改用截图取证。
3. **字体/横竖屏视觉矩阵**：新增录入弹层 640×320dp/2倍字体测量和默认方向真实交互；F4 精选图标区、UX13 账本表单仍未逐组合做人工视觉确认。
4. **TalkBack**：无障碍朗读顺序未做设备级验证。
5. **AI 真实请求链路**：全部使用 fake sender，未发送真实或计费请求；片段级生命周期已覆盖，旋转/切会话的完整生命周期仍属未覆盖。
6. **合并 UI 全流程**：真机只验证到二次确认弹层（未确认提交），提交后的偏好修复与列表刷新由隔离库测试覆盖。
7. **原有 5 项设备测试已闭环**：删除撤销测试改为按现行业务规则使用不可变 `assetId`；v9 测试改为验证 v22 边界的“重置旧财务表、保留 AI 会话”；非法枚举测试改用真正非法的 token（原值 `transfer` 实为合法值）。完整隔离套件现为 80/80。
8. **静态脚本边界**：`verify-ux-resources.ps1` 只是直接属性扫描，不代表运行时主题、全部页面或可访问性验收。

## 第五轮闭环（2026-09-11）

- UX10：`AgentSendLifecycleTest` 新增配置缺失分支；验证配置恢复卡可见、输入禁用、设置入口可达、fake sender 未被调用，设备用例 5/5 通过且无真实请求。
- UX09：新增 `RecordSheetInteractionTest`。默认方向真实启动 `MainActivity`/`AddRecordFragment`，逐一验证金额键盘切换日期、资产、分类弹层；日期日历为 42 单元。另以 640×320dp、2倍字体测量横屏短视口。复现日期底部确认不可达后，将 `bottom_sheet_record_date.xml` 改为滚动日历区 + 固定底部操作区。
- 测试债：修正 5 个与当前契约不一致的旧设备测试，不改现行业务行为。相关 15 项定向测试通过，最终 `connectedVerificationDebugAndroidTest` 为 **80/80**。
- 最终命令：静态脚本 73/51 PASS；JVM **78/78**；`assembleDebug` PASS；OpenSpec strict validate PASS；`adb install -r app-debug.apk` Success。
- APK：`app/build/outputs/apk/debug/app-debug.apk`，12,203,721 字节。日常包为覆盖安装，未清除用户数据。

## 第六轮视觉复核重新打开（2026-09-11，未实施）

- 用户提供的当前设备截图确认 UX15：新建账本的现有资产组摘要与成员信息重复、选择入口缺少完整控件语义。
- 用户提供的当前设备截图确认 UX16：浅色主题共用金额键盘的普通键运行时显示为黑色空白块，字符不可读。
- 上述第五轮 80/80 设备测试没有验证键盘实际前景/背景像素，也没有证明资产组选择区的信息层级通过，因此不能作为 UX15/UX16 的 PASS 证据。
- 新工作合同见 `visual-recheck-2026-09-11-round-6.md`、design.md UX15/UX16 与 tasks 第9节；当前状态为 FAIL/待实施，原报告仍不代表最终视觉通过。

## 第七轮实施（2026-09-11，第六轮 FAIL-15 / FAIL-16 修复）

实施依据：`visual-recheck-2026-09-11-round-6.md`、design.md UX15/UX16、`specs/ux-consistency/spec.md` 的 `Clear existing asset-group selection` 与 `Readable shared amount keypad`、tasks 第 9 节。

### UX16（P0）——共用金额键盘字符不可见

- 根因（已复现，不是猜测）：主题把 framework `Button` 替换为 Material3 `MaterialButton`，而 `Widget.CardTally.KeypadButton` 继承的父样式中 `android:background` 为 `@empty`，同时 `android:background` 一旦设置就会让 MaterialButton 走 `isUsingOriginalBackground()` 分支并跳过 `backgroundTint`，两条路径叠加后按键**没有任何背景**，只剩深色字符画在白色键盘底上、或按父样式被着成主色块。设备探针实测：`viewClass=android.widget.Button`、`bgClass=null`、`tint=null`，直接按 style 取 `android.R.attr.background` 也返回 `null`。
- 改动：
  - `L/layout_amount_keypad.xml`：所有普通键与主键由 `<Button>` 改为 `<TextView>`（键盘只通过 `findViewById<View>(id).setOnClickListener` 绑定，`updateConfirmLabel` 用 `TextView`，行为不变）。TextView 不继承任何 background tint，声明的背景就是渲染结果。
  - `V/styles_keypad.xml`：`parent=""`，显式 `android:background=@drawable/bg_keypad_key`、`android:textColor=?attr/colorOnSurface`、`minWidth/minHeight 48dp`、`gravity=center`、`clickable/focusable`；主键覆盖为 `@drawable/bg_keypad_key_primary` + `@color/onPrimary_light`。
  - 新增 `D/bg_keypad_key.xml`（浅色表面 `surface_container_lowest`，按压 `surface_container_high`，禁用 `surface_container_low`，12dp 圆角）与 `D/bg_keypad_key_primary.xml`（`buttonPrimary_light` 黑底，按压 `primaryVariant_light`，禁用 `surface_container_highest`）。
  - 删除上一轮临时新增、实际未被读取的 `res/color/keypad_*_surface.xml` 三个选择器。
  - 文案入资源：`record_keypad_delete`（"删除一位"）、`record_keypad_delete_glyph`（"⌫" 作为可稳定渲染的字符）、`record_keypad_confirm`、`record_keypad_equals`；`AmountKeypadController.updateConfirmLabel()` 不再硬编码 `"确定"` / `"="`。
- 实际渲染验收（`AmountKeypadRenderTest`，4 项全部通过）：
  - `everyKey_rendersReadableForegroundOnItsRealSurface`：在 `Theme.CardTally.Light` 下 inflate 生产布局、measure/layout、`draw(Canvas)`，逐键断言「有显式不透明表面」「前景/表面解析值对比度 ≥4.5:1」「宽高 ≥48dp」「键心中间带出现 ≥2 种像素（证明字形被真正绘制）」。15 个键（0-9、`.`、`−`、`+`、删除、确定）全通过。
  - `primaryKey_usesASeparateHighContrastSurface`：确定/`=` 键与数字键表面必须不同且自身对比度 ≥4.5:1（防回退成 Material3 主色块）。
  - `keypad_inflatedInsideTheRecordAndAssetScreens_isReadable`：`fragment_add_record` 与 `fragment_add_asset` 内嵌同一键盘，逐键 48dp + 对比度。
  - `keypadOpensInARealActivity_andKeepsItsLabels`：真实 `MainActivity` + `AddRecordFragment`（走生产 `AmountKeypadController.bind()`），在实时页面上核对 7/`.`/`−`/确定 的可读性。
  - 说明：主题当前只有 `Theme.CardTally.Light`（`ThemeHelper.getThemeResId` 恒返回 light），`values-night` 为空，所以深色/跟随系统在本仓库等价于浅色；未伪造深色证据。
- 观察到的运算符字形：`layout_amount_keypad.xml` 的减号使用 U+2212 `−`。已在设备上确认该字形可见（键心像素断言通过）；如需回退到 ASCII `-` 只需改资源。

### UX15（P1）——现有资产组选择区层级与点击语义

- 改动：
  - `L/fragment_ledger_setup.xml`：资产关系卡内新增独立的全宽选择行 `ledger_setup_group_row`，前置细分隔线 `ledger_setup_group_divider`；行内为 14sp 次级标签 `ledger_setup_choose_ledger`（"选择资产组"）→ 16sp 主文本 `ledger_setup_group_name`（当前组名，最多两行）→ 12sp 次级摘要 `ledger_setup_group_summary`（成员 + 资产数）→ 尾部 20dp 矢量 `ic_chevron_right`（装饰，`contentDescription=@null`）。行 `minHeight 64dp`、`wrap_content`、全宽、`selectableItemBackground`、可点击可聚焦。删除了原来两条无控件的裸文本。
  - `K/LedgerSetupFragment.kt`：`selectExistingGroup()` 收敛为**只切模式**；只有 `groupRow` 打开 picker，因此一次点击只产生一个动作。模式行副文案改为通用说明 `ledger_asset_existing_group_hint`，不再承载组摘要，与下方选择行不重复。`renderGroupState()` 分别写入组名与「成员 + 资产数」，并设置整行 `contentDescription`（含当前组名与摘要）。独立模式整段（分隔线 + 行）随 `ledger_setup_asset_list` 一起 GONE，切回按稳定组 ID 恢复。
  - 新增字符串 `ledger_asset_existing_group_hint`、`ledger_asset_group_members_assets`、`ledger_asset_group_row_accessibility`（中英），删除被取代的 `ledger_asset_group_summary`；BottomSheet 行副文案改用同一摘要格式。
- 设备验收（并入 `LedgerSetupFormTest`，8 项全部通过）：
  - `existingGroupMode_exposesOneFullWidthSelectionRowWithChevron`：行可见、可点击、可聚焦、高度 ≥64dp、宽度 ≥300dp、标签与组名与摘要非空、整行 contentDescription 含组名、存在装饰 chevron。
  - `existingGroupMode_doesNotRepeatTheGroupSummaryInTheModeRow`：遍历模式行内所有文本，断言不含当前组名、不含组摘要，且仍带通用说明。
  - `togglingAssetGroupMode_hidesTheWholeRowAndKeepsTheSameGroup`：独立模式整段不显示（`isShown` 为 false，含分隔线）、radio 状态正确、切回后行恢复且稳定回到主资产组。
  - `groupRow_handlesLongNamesAt320dpWithoutLosingTheEntry`：320dp + 超长组名，行仍 ≥64dp、名称 ≤2 行、chevron 仍在行内。
  - 既有 UX13/UX14 用例（表单可见性、一次点击切换、独立组创建、编辑隐藏资产卡、跨账本流水）继续通过。

### 第七轮验证命令

| 命令 | 实际结果 |
| --- | --- |
| `powershell -NoProfile -File scripts/verify-ux-resources.ps1` | PASS：73 布局解析，51 引用（静态扫描） |
| `.\gradlew.bat :app:testDebugUnitTest` | BUILD SUCCESSFUL；78 项，0 失败 |
| `.\gradlew.bat :app:assembleDebug` | BUILD SUCCESSFUL |
| `:app:connectedVerificationDebugAndroidTest`（全套） | **87 项，87 通过，0 失败** |
| 定向：`AmountKeypadRenderTest` | 4 项通过（含真实像素与对比度断言） |
| 定向：`LedgerSetupFormTest` | 8 项通过 |
| `adb install -r app\build\outputs\apk\debug\app-debug.apk` | Success；`run-as ... ls databases` 确认 `CardTally.db` 仍在 |

### 第七轮真机复核（2026-09-11，已解除 BLOCKED）

上一轮误判「真机 adb 点击不可靠」——用户确认闲鱼是本人点击，是坐标取错（第一屏截图实际是 1080×2420，金额行中心在 y≈605 而不是估算的 506）。改用 `uiautomator dump` 取**精确 bounds** 后，两处都能真机复核：

**UX16 键盘（浅色主题，日常包，`screenshot/ux16_keypad_light.png` / `ux16_keypad_equals.png`）**

- 点击 `edit_amount` 精确中心 `[318,545][984,665]` → 键盘弹出；层级实测键盘 `[0,1622][1080,2354]`。
- 全部按键边界实测：
  - `keypad_7..keypad_9` `[30..792,1652..1808]`，`4..6` `[..,1820..1976]`，`1..3` `[..,1988..2144]`，`. 0 ⌫` `[..,2156..2312]`，每键 **246×156px = 82×52dp**，满足 48dp。
  - `keypad_minus [804,1820][1050,1976]` 文本 `−`；`keypad_plus [804,1988][1050,2144]` 文本 `+`。
  - `keypad_delete [546,2156][792,2312]` 文本 **`⌫` 可见**，`content-desc=删除一位`。
  - `keypad_confirm [804,2156][1050,2312]`，初始文本 `确定`。
  - `keypad_hide [798,1646][1056,1814]` 文本空、`content-desc=收起数字键盘`。
- 截图确认：数字/`.`/`−`/`+`/`⌫` 均为**浅底深字清晰可见**，`确定` 为**黑底白字**。第六轮的黑色空白块不再出现。
- 表达式两态实测：依次点 `1` `+` `2` → `edit_amount=1+2`，`keypad_confirm` 文本变为 **`=`**；点 `=` 后 → `edit_amount=3.00`，按钮回到 **`确定`**。默认 `0.00`、求值、文案切换均未回归。

**UX15 新建账本表单（隔离变体截图，`screenshot/ux15_ledger_form.png`）**

- 截图可见：基本信息卡（账本名称 + 示例占位 + 下划线输入区 + 36dp 圆底 18dp 图标 + 尾部矢量 chevron）。
- 「账本资产」卡内：`新建独立资产组`（圆底说明「创建一组独立资产，不复制已有资产」）与 `使用现有资产组`（说明「该账本与所选资产组共享同一批资产与余额」）两个模式行，**模式行不再出现组摘要**。
- 分隔线下方是独立选择行：14sp「选择资产组」→ 16sp「主资产组」→ 12sp「成员：日常 · 0项资产」→ 尾部 `›` 形矢量 chevron。**没有任何脱离控件的裸文本**，层级为「模式 → 当前组 → 摘要 → 入口」。
- 底部「创建账本」黑底白字 12 圆角按钮完整可达。

### 第七轮仍未覆盖

1. **深色/跟随系统主题**：仓库只有 `Theme.CardTally.Light`（`values-night` 为空、`ThemeHelper.getThemeResId` 恒返回 light），无法给出真实深色证据；键盘已按主题 token 编写，加入深色调色板后会跟随。
2. **字体 2 倍下的键盘/表单排布截图**：未单独留证（布局用 `wrap_content` + `minHeight`，字体 1 倍已实测）。
3. **TalkBack**：未做设备级朗读顺序验证。
4. **UX15 的非主/空组真机截图**：截图为主资产组；非主组与空组由 `LedgerSetupFormTest` 覆盖（稳定组 ID 往返、空组提示）。
