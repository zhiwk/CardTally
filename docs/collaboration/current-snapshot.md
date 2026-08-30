# Current Snapshot

## 当前有效快照

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
