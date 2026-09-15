## Context

这是一份待实施的 UX 合同，不是本轮已修复清单。用户要求审查者不改应用，由其他模型实现并做功能检查，之后再视觉复审。

路径简写：L = `app/src/main/res/layout/`；D = `app/src/main/res/drawable/`；K = `app/src/main/java/com/example/cardtally/`。以下尺寸均 dp，文字均 sp。文件名和控件用途同时定位，实施前用 rg 核对 ID，禁止凭相似文件名改旧版未引用布局。

### 覆盖面与证据等级

源码覆盖的页面簇：账单、统计、我的、搜索、日历；资产首页/类型选择/流水/归档/新建和编辑；记一笔和编辑记录；分类管理/新增分类/图标选择；账本管理/新建和编辑；语言设置；AI 助手/会话/配置；这些入口使用的 BottomSheet 和对话框。

当前主要资源：fragment_ledger、fragment_statistics、fragment_settings_v2、fragment_search、fragment_calendar、fragment_asset_v2、fragment_asset_type_select、fragment_asset_records、fragment_archived_assets、fragment_add_asset、fragment_add_record、fragment_category_manage、fragment_add_category、fragment_ledger_management、fragment_ledger_setup、fragment_language_settings、fragment_agent、fragment_ai_assistant_settings。后缀均 .xml。编辑复用布局不算另一套视觉实现。旧 fragment_asset.xml、fragment_settings.xml 和无调用入口不要求全面翻修或删除。

- **确认/视觉**：当前设备直接看到的现象，并已对照源码。
- **确认/源码**：代码或资源能确定的问题；尚未宣称设备复现。
- **风险**：缺少组合状态实测；必须先复现或测量，再决定变更。

## Goals / Non-Goals

目标：主体卡片一致；操作可辨认且可达；长内容和放大字体不遮挡关键操作；功能语义不因样式调整改变。

非目标：数据库迁移、重写架构、替换图标库、引入 Compose/Room/Navigation、改 AI 协议、添加 AI 记账能力、批量翻修不可达旧资源、重做品牌。

## Decisions

### 统一视觉基线

以下是实施目标，非全部当前实现值：

| 对象 | 目标 |
| --- | --- |
| 浅色页面底 | #EEEEEE，使用现有语义资源 |
| 主体卡片 | 不透明 #FFFFFF，alpha=1，stroke=0，elevation=0，默认圆角12 |
| 记一笔主表单 | 保留参考中的20圆角；不要为统一而改为12 |
| 分段切换 | 白色12圆角，无描边/阴影；选中黑色加粗+短下划线，未选中灰色 |
| 文字 | 现有无衬线；标题22粗体，正文14—16，必要辅助文字不小于12 |
| 常规页边距 | 16；相邻卡片12；卡内通常16，已有账单紧凑行可保留 |
| 图标/点击区 | 一般线性图标20—24；独立操作点击区至少48×48；列表分类图标沿用账单36底/18图标 |
| 日期例外 | 紧凑日期单元格36高；数字区域32×28并居中，用户已指定，不强改48 |
| 次级文本 | onSurfaceVariant_light（当前#565B61），不用 outline 充当正文色，不叠半透明 |

白色指卡片容器填充，不是 TextView 的文字色。保留内部细分隔线、输入框必要边界、收支/转账语义图标底色、错误态和选中勾。默认收入红、支出绿，由收支颜色设置管理；转账黄色。禁止把这些彩色底全部刷白。点击态可有短暂 ripple，但静止时不得灰色覆盖白卡。

### UX01 — 设置导航箭头错误（P2，确认/视觉+源码）

定位：D/ic_arrow_right.xml；L/fragment_settings_v2.xml 内各设置项右侧 ImageView。

证据：该矢量 path 是 x=8.59—13.41、y=7.59—16.59 的矩形；设备显示竖条而非右箭头。

实施：先查全部引用，复用现有 ic_chevron_right 或修正共享矢量为一致的右向 chevron，显示20；整行继续点击。装饰箭头不单独获得无障碍焦点，不重复朗读行标题。

验收：语言、收支颜色、API 配置、分类管理、账本管理、图片数量都显示箭头且原入口正常。

### UX02 — 标题栏与资产动作拥挤（P1，确认/源码；大字体溢出为风险）

定位：L/view_secondary_header.xml（固定48）；fragment_ai_assistant_settings.xml（固定48上偏移）；fragment_add_asset.xml、fragment_add_category.xml、fragment_ledger_setup.xml；fragment_calendar.xml；fragment_asset_records.xml。

证据：存在20/22标题、40返回区与48共享头混用。资产流水单行放返回及编辑/置顶/归档/删除共五个40宽动作，挤压加权标题。

实施：二级页面标题22粗体，返回目标48，栏 minHeight=56、wrap_content；标题与后续内容采用真实相邻约束/垂直布局，删除依赖固定标题高度的内容偏移。资产流水保留返回、标题、编辑、更多，其他动作放更多菜单；沿用既有事件、状态与危险操作确认，不重写财务逻辑。长资产名允许两行且标题栏增长。

验收：320/360宽、1/2倍字体下标题与内容不重叠；长名称不把编辑/更多挤出屏幕；置顶/取消、归档/恢复、删除可达且确认流程保持。用隔离数据验证这些变更操作。

### UX03 — 操作点击区偏小（P2，确认/源码，今天操作有设备测量）

定位：L/fragment_add_record.xml 的 btn_close、btn_take_photo；item_record.xml、item_asset.xml、item_category.xml 的滑动操作；fragment_category_manage.xml 的 tab_layout；fragment_search.xml 搜索/取消/筛选；dialog_search_date_range.xml；bottom_sheet_record_date.xml 的 text_select_today；新增分类确认操作。

证据：存在32/40高操作、40高 Tab、wrap_content 文本按钮；设备“选择今天”点击节点约19高。

实施：操作节点 minWidth/minHeight=48，或明确不重叠的父点击区/TouchDelegate；图标本身无需变大。文本按钮垂直居中，长文本可换行并增高。不要给装饰图标、日期网格应用48规则。

验收：测量实际点击节点而不只看背景；边缘点击一次触发一次，邻近点击区不重叠；滑动与滚动、拍照、返回、日期快捷选择不回归。

### UX04 — 重要辅助文字过浅（P1，确认/源码）

定位：L/item_record.xml 的 text_time，K/adapter/DateGroupAdapter.kt 的副标题绑定；fragment_ai_assistant_settings.xml 的说明/状态；item_asset.xml 类型；item_ledger_header.xml 指标标签。

证据：记录副标题使用 colorOutline（#D5D8DC）；配置说明叠加0.8/0.75透明度；另有10/11字号。

实施：具有信息价值的副标题改语义次级文字色#565B61，alpha=1，至少12；分隔线仍用 outline。金额颜色服从设置，不随辅助文字更改。

验收：支出/收入类型、账户、API 状态可读，白底普通文本对比度至少4.5:1；文本放大不被固定行高裁掉。

### UX05 — 长金额挤压内容（P1，风险）

定位：L/item_record.xml、item_asset.xml、item_statistics.xml、item_ledger_header.xml、fragment_search.xml。当前右侧金额多为 wrap_content，左侧 weight；搜索总计与日期并排，部分指标 marquee。

先复现：320/360/600宽、1/1.3/2字体，24字中文分类/账户及长英文名，金额999,999,999.99、负数、零，统计百分比同时出现；使用内存模型，不改真实金额。

若失败，实施：空间足够时保留一行；不足时金额移到下一行全宽右对齐，名称最多两行，行高自适应。金额完整展示，不省略、不跑马灯、不缩到14以下、不改变金额精度。汇总栏可改垂直“标签+完整数值”，仍不足再增加行数，不强塞等宽三列。百分比不得覆盖名称。

验收：金额和操作完整可见，名称有可访问的完整文本；点击、分类分组及金额计算不变。若当前布局在上述组合全部通过，保留并提交测量证据。

### UX06 — 主操作形状不一致（P2，确认/源码）

定位：L/fragment_add_asset.xml 的 btn_save（56高、圆角4）、fragment_add_record.xml 底栏、fragment_category_manage.xml 添加按钮、bottom_sheet_record_date.xml 确认、fragment_ai_assistant_settings.xml 保存；D/shape_button_primary.xml。

实施：一般表单/弹层文字主按钮采用黑底白字、14中等字重、圆角12、minHeight48、wrap_content、elevation0、可见按压态。优先现有 MaterialButton 样式复用，不新造重复资源。例外：记一笔双按钮保留图标+说明两层内容及参考形状；圆形 FAB 保持圆形。自定义可点击容器需按钮语义及键盘可操作。

验收：保存、再记一笔、日期确认与 API 保存无重复提交；禁用状态可辨认，文本2倍字体完整且按钮增长。

### UX07 — 图标选择经过过滤后丢失新选中态（P1，确认/源码）

定位：K/adapter/IconPickerAdapter.kt，L/item_icon.xml，图标选择调用方。

证据：点击只更新 selectedPosition；过滤又用构造参数 selectedIcon 找位置，导致新选中B在过滤后回到初始A。

实施：用可更新的图标名称作为唯一选择状态，点击更新名称，过滤和绑定从名称派生位置；点击前检查 bindingAdapterPosition != NO_POSITION。选项暂被过滤掉不能抹除选择。选中以现有浅色底+16勾标记表达，不仅靠颜色；提供本地化描述和选中语义。禁止修改持久化图标键。

验收：初始A→选择B→过滤不含B→清空搜索，B仍选中；回调一次且为B；无效位置无崩溃；保存后重新进入仍为B。用适配器隔离测试和独立测试存储验证。

### UX08 — 分类图标浏览难用（P2，确认/视觉+源码）

定位：K/AddCategoryFragment.kt 的 renderIconLibrary、createIconTile；L/fragment_add_category.xml；图标更多入口。

证据：左栏渲染完整分组、右栏每组仅take(16)，父容器按最长左栏增高，右下大量留白；图标标签显示截断的 arrow aut... 等原始英文键。左侧分类当前可以切换，不要重新实现已修复功能。

实施：浏览区最大高度360且不超过可用内容高度；左右分别可滚动，不按全部分类总高度展开。左栏88宽、行minHeight48；右栏每格至少48，间隔8，按可用宽度选择列数，正常360宽可四列。搜索/更多放浏览区上方可达操作区，不放长导航底部。精选图标使用有意义的中英文本地化名；完整库仍可按原始键搜索，不要求翻译全部库，不改变保存键。

验收：每个分组可达且切换正确，更多始终可进入；选中图标能保存；320宽和2倍字不截断关键操作，嵌套滚动无抢手势死区。

### UX09 — 弹层高度与键盘组合（P1，风险）

定位：L/dialog_icon_picker.xml（固定560高）、bottom_sheet_record_category.xml（列表320高）、bottom_sheet_record_date.xml、bottom_sheet_ledger_period.xml；K/AddRecordFragment.kt、金额键盘控制器及弹层创建逻辑。

先复现：备注系统键盘打开→日期/资产/分类；金额自定义键盘打开→同三弹层；六周月份、横屏、2倍字体、短视口。

若失败，实施：切换前清理相应输入焦点并正确收起系统/自定义键盘；按实际 WindowInsets 限制弹层可用高，不能重复计算底部 inset。正文在可用空间滚动，标题和确认保持可达；图标列表不固定560屏幕高，使用可用高上限。不得用任意 postDelayed 延时掩盖状态竞争，不牺牲日期36单元格约定。

验收：弹层不悬在残留键盘上，关闭不会自动重弹键盘；草稿/金额/备注不丢；连续开关、旋转后确认和取消可达。当前代码已有金额收起逻辑，先核实，不重复叠补丁。

### UX10 — AI 发送状态与动作语义不符（P1，确认/源码；完整生命周期待测）

定位：K/AgentFragment.kt 的 updateSendingState、sendCurrentMessage；L/fragment_agent.xml 的 button_send/image_send/progress_sending（以当前ID为准）。

证据：流式状态隐藏发送图像，但按钮仍可触发停止；描述仍为发送。未配置时仅设置 enabled，外观可能仍像可用发送操作。

实施状态表：

| 状态 | 外观与行为 |
| --- | --- |
| 未配置 | 输入/发送明确禁用，保留可达配置按钮，不发请求 |
| 空闲可发送 | 发送箭头，描述“发送”，一次触发 |
| 请求建立中 | 可见忙碌指示，禁止重复发送；不凭空新增尚不支持的取消语义 |
| 流式响应 | 发送位置显示停止方块，描述“停止回复”，沿用现有取消处理 |
| 错误/取消 | 恢复可用输入，保留已有草稿/部分文本规则，不新增自动重试 |

菜单和新会话禁用时也有明确状态。通过现有 fake/可控回调验证，不发真实计费请求，不改网络协议。已有 AI 自适应纵向布局和可滚动配置卡不得回退为固定偏移。

验收：所有状态图标与真实动作一致，无空白可点击圆圈；停止仅取消当前请求，不污染新会话；旋转/切会话按现有生命周期规则工作。

### UX11 — 本地化与空资产引导（P2，确认/源码；隐藏入口已观察）

定位：L/fragment_asset_records.xml、fragment_add_asset.xml、fragment_archived_assets.xml 的硬编码文案；资产选择空态及导航显示偏好；values/strings.xml 与 values-en/strings.xml。

实施：将当前可达页面硬编码标题、操作、空态及无障碍描述迁至现有字符串资源，补齐英文。不要全面翻修未引用旧资源。无资产且资产导航隐藏时，提示应指向“我的→账户资产”开启入口后添加账户，而不是只说去不存在的资产页；有入口时沿用直接引导。不要自动改变用户偏好，也不要重新加入“无”选项。

验收：中英文标题/按钮/错误/空态无混杂；隐藏入口用户知道下一步；语言切换不丢表单；图标按钮可朗读，装饰图标不重复朗读。

### UX12 — 测试和文档证据需要收口（P2，确认/文档）

定位：docs/design/current-ux-inventory.md、DESIGN.md、docs/collaboration/current-snapshot.md、docs/collaboration/ux-consistency-audit.md、scripts/verify-ux-resources.ps1。

实施：按当前源码修正仍描述旧 Home/旧视觉的相关入口说明；保留历史材料的历史身份，不把计划写成实现。静态脚本只是直接属性扫描，不能宣称验证了运行时主题、全部页面或可访问性。按每项实际实现增加隔离测试；报告明确列出没跑的组合及原因。根 AGENTS.md 不塞本轮流水账。

验收：实施报告能从UX编号追溯到文件、测试、结果和待视觉复审项；所有任务勾选都有证据。

### UX13 — 新建/编辑账本表单统一（2026-09-11新增，待实施）

业务更新：第4、5、7点的新建开关/来源账本交互由增补文件 `ledger-asset-groups.md`（UX14）替代；使用直接可选的独立/现有资产组。其余白卡尺寸、编辑不改资产和按钮规则保留。实施必须同时阅读该增补与 `specs/ledger-asset-groups/spec.md`。

定位：L/fragment_ledger_setup.xml；K/LedgerSetupFragment.kt（renderAssets、图标选择、显隐）；共享IconPickerDialog。用户截图是当前问题页，不是要求照搬现有缺陷。按以下确定参数实施，不需自行识图设计。

1. 页面底#EEEEEE，左右16dp。标题沿用22sp粗体、返回48dp、minHeight56且wrap_content。正文可滚动，底部按钮单独布局，不能被正文或IME覆盖。
2. 基本信息合成一张#FFFFFF、12dp圆角、无描边/阴影卡片。卡内16dp；“账本名称”标签14sp次级色；标签下8dp为输入区域，minHeight48、wrap_content、正文16sp，完整保留placeholder和错误提示。移除当前bg_input_surface的静止外框，保留明确焦点/错误提示，不修改共享输入背景影响其他页面。
3. 名称区与图标行之间12dp间距和一条内部细分隔线。ledger_setup_icon_picker为minHeight56、wrap_content整行点击；图标使用36dp浅中性圆角底，中心18dp实际保存图标。标签16sp，最多两行、权重占剩余空间；尾部20dp矢量chevron，距标签至少12dp。废弃字符“›”和32dp裸图标，不更改图标键或将所有选择替换为默认图标。
4. 基本信息卡下12dp是资产关系白色12dp卡。将ledger_setup_asset_relation_section与展开的asset_mode_group/source list归于同一外层卡，禁止每个radio再嵌套卡。开关行minHeight64、wrap_content，水平16dp/垂直12dp；标题16sp，说明12sp次级色，间隔4dp，说明可自然换行。开关外点击区至少48×48，与文字至少12dp间隔；整行和Switch事件统一处理，点击一次仅切换一次。保留现有开关视觉体系与已确认业务文案。
5. 开关关闭时，在同卡内部细分隔线后显示独立/共享其他账本选项，每行minHeight48且wrap_content、水平16dp。来源标题14sp次级色、上间距12dp；来源账本行minHeight56、长名最多两行，并明确唯一选择标记。来源列表按现有可选集合渲染，不为展示增加账本或改变共享规则。无来源时展示现有空态，不能出现空白来源区域。
6. 创建/保存按钮左右16dp、底16dp加实际系统inset一次；minHeight52、wrap_content、黑底白字14sp、圆角12、elevation0。保留当前灰色页面底，不额外引入浮动阴影或全新装饰底栏。按状态显示创建账本/保存修改，焦点键盘出现时仍可到达按钮。
7. 新建保留默认共用主资产与关闭后的原模式语义。编辑只显示基本信息卡与保存，整个资产关系卡（包括其margin、展开选项与来源）GONE；保存只能沿用updateLedgerDetails，不修改资产池。旋转/图标弹层返回保留名称、图标和新建资产草稿；检查开关UI与selectedSourceLedgerId一致，发现业务冲突先按决策复核。
8. 真机/隔离验收：新建开/关两态、共享来源有/无、编辑；320/360宽、字体1/2、中英、长账本名、名称键盘→图标弹层→选中返回、空名称错误。无裁字、无标签覆盖开关、按钮完整可达；新建保存与编辑不改资产池使用隔离数据验证。不得通过清除用户应用制作空态。

第三轮遗留F1—F6按recheck-2026-09-11.md继续处理；新增页面样式不意味着旧缺陷已验收。

### UX15 — 现有资产组选择区层级与点击语义异常（P1，截图确认）

定位：L/fragment_ledger_setup.xml 的 `ledger_setup_existing_row`、`ledger_setup_asset_list`、`ledger_setup_choose_ledger`、`ledger_setup_group_assets`；K/LedgerSetupFragment.kt 的 `renderGroupState`、`selectExistingGroup`、`openGroupPicker`；values/strings.xml 与 values-en/strings.xml。

证据：第六轮截图中，“使用现有资产组”选项的副文案已显示“主资产组 · 1项资产”，其下又单独显示“选择资产组”和“日常、好纠结”。后两行没有共同的整行点击背景、尾部 chevron 或明确选中标记，成员账本名称也容易被理解成另一个字段；选择动作、当前资产组和成员摘要被拆散，信息重复且点击位置不清晰。

实施：资产关系卡仍保留“新建独立资产组 / 使用现有资产组”两个模式行，不增加嵌套卡。模式行只表达模式及通用说明；选择现有模式后，在同卡内部细分隔线下显示一个 `minHeight 64dp`、`wrap_content`、全宽可点击的资产组选择行。该行以14sp次级标签“选择资产组”开头，16sp主文本显示当前资产组名称，12sp次级摘要合并成员账本与资产数量，尾部使用20dp矢量 chevron；长名称最多两行，文字占剩余宽度。不得把成员名称作为脱离控件的裸文本，也不得在模式副文案和选择行重复同一组摘要。

交互：点击“使用现有资产组”模式行只选择该模式并显示选择行；点击选择行才打开现有 BottomSheet。默认主资产组仍可直接创建，不强迫用户打开弹层。切换到独立组时，选择行及其分隔线、margin和点击目标整体 GONE；切回后恢复之前选中的稳定组 ID。整行点击、键盘焦点和 TalkBack 都只有一次打开行为，chevron 是装饰元素。

验收：默认主组、切换独立后返回、选择非主组、空组、长组名/成员名、中英文、320/360dp、字体1/2、旋转和图标弹层返回；必须能从静态截图看出模式、当前选择及入口，任意文字不重叠/裁切，创建结果与UX14资产组规则完全一致。

### UX16 — 共用金额键盘字符不可见（P0，截图确认）

定位：L/layout_amount_keypad.xml；values/styles_keypad.xml 的 `Widget.CardTally.KeypadButton` 与 Primary；drawable/bg_summary_item.xml、shape_button_primary.xml；K/util/AmountKeypadController.kt；所有 include 该键盘的新增/编辑记录与资产页面。

证据：第六轮浅色主题截图中，数字、点号、加减号和删除键均渲染为黑色圆角块且字符不可见，只有主操作“确定”为白字。XML 中数字文本仍存在，因此仅检查 `android:text` 或 View 非空会产生假 PASS；当前 framework Button 父样式、背景 drawable 与运行时 tint 的组合是优先核查点。

实施：为普通按键使用明确的主题表面背景和前景色，清除或覆盖 framework Button 继承的 background tint，不能让 tint 把白色 drawable 重新着成主按钮黑色。普通键在浅色主题使用白色或明确的浅中性色表面与深色字符；主“确定/=”键保持黑底白字并与普通键有清晰层级。深色主题使用对应主题 token，不硬编码导致反色失效。数字、`.`、`−`、`+`、删除和本地化“确定”均必须显示；删除优先使用现有矢量图标或可稳定渲染且可朗读的字符，不能依赖缺字字形。按键保持至少48dp点击区、统一圆角和间距，不用 elevation 制造浮层。

验收不能只检查属性：在浅色、深色、跟随系统三种主题中获取实际渲染结果，逐个核对所有键的可见标签/图标、普通键前景与实际背景至少4.5:1、主键至少4.5:1、禁用/按压后仍可辨认。覆盖新增记录、编辑记录、新增资产和编辑资产（如复用同一布局），以及“确定”与表达式“=”两态、320/360dp、字体1/2和底部系统 inset。点击行为、表达式求值、默认 `0.00`、收起键盘及保存/再记一笔规则不得改变。

## Risks / Trade-offs

- 大字体下允许页面更高和更多滚动，不以固定高度维持截图外观。
- 不同功能容器不应全部相同圆角：12普通卡、20录入主卡、圆FAB是明确例外。
- 隐藏导航不代表功能废弃；3/4/5导航组合应在隔离配置中检查，不修改用户偏好来造覆盖率。
- 旧快照可能落后源码；以源码复核，不用本审查覆盖用户之后的新改动。

## Validation Strategy

实施后先检查构建说明并串行运行静态资源脚本、相关单测、testDebugUnitTest、assembleDebug；涉及设备测试时先审查测试安全性，再单独执行 assembleDebugAndroidTest/指定测试类，禁止并行 Gradle。使用专用模拟器或不启动 Activity、不读写偏好和数据库的隔离布局测试补充长内容状态。

至少覆盖：当前页面簇空/非空状态；中文/英文；320/360/600宽；字体1/1.3/2；横竖屏；两种键盘→弹层；金额键盘浅色/深色/跟随系统的实际像素可读性；3/4/5底部导航；TalkBack 焦点和按钮名称。不能覆盖的明确 BLOCKED，不标 PASS。

业务回归：记录只选叶子分类；搜索/账单/统计/日历展示同一分类图标；收入支出配色设置和转账黄色；资产选择不含“无”、新建首次不预选；编辑账本无资产关系区且保存不改原关系；金额默认/焦点/空值行为不被样式修改；日期选择不隐藏整个月历；编辑/保存/再记一笔/归档恢复/删除的原规则；AI 配置及文本聊天生命周期。涉及数据变更仅用隔离测试数据。

最终视觉检查由用户安排原审查者进行；实施模型完成的是代码与功能交付，不是最终视觉签收。
