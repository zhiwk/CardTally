# Task Entrypoints

## 所有任务都先读

1. `README.md`
2. `AGENTS.md`
3. `docs/collaboration/README.md`

## 涉及业务规则 / 数据逻辑时再读

1. `docs/requirements/decisions/business_rules.md`
2. `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`
3. 受影响页面对应的 Fragment / Adapter / Model

## 涉及页面重构 / 信息架构 / 首版产品方向时再读

1. `docs/requirements/plans/2026-03-26-cardtally-implementation-plan.md`
2. `docs/requirements/plans/2026-03-26-cardtally-product-restart-design.md`
3. `docs/requirements/plans/*.md`

## 涉及视觉设计 / UI 风格时再读

1. `DESIGN.md`
2. 当前页面对应的布局、Fragment、Adapter 和资源文件

## 涉及历史计划 / 过程文档时再读

- `docs/requirements/plans/*.md`
这些文件可用于理解历史执行意图或阶段性方案，但不能直接当成当前代码现状的权威来源，必须回到代码与高可信文档验证。

## 常见任务入口

### 改默认分类目录

先读 `database/DefaultCategoryCatalog.kt`、`ReferencedCategoryCatalog.kt`、`MergedCategoryMigration.kt`、`CategoryDefaultsSeeder.kt`、`CategoryDefaultsAppender.kt`、`DatabaseHelper` 的升级/打开/全空补建入口，以及 `BackupArchiveManager.removePristineSeed`。当前按2026-10-23融合分类决策使用15支出父类/91二级、9收入父类/29二级，共144节点；三餐统一餐饮。onOpen→completeOnce按目录版本2在事务内先整理明确旧路径再补齐并标记完成，覆盖已为v43和已完成版本1的旧库。旧19节点与参考177节点的路径见迁移器；未知自定义/深层子树/无法细分名称保留，不猜测、不清库。同名根叶子不转父类；合并叶子时同步records/recurring_records/record_deletion_undo分类ID及backup_import_map，不改金额及历史名称快照。持久化别名保证提交后标准/快速排序偏好可幂等恢复。分类、目录状态、别名均为数据库本地；后两者不参与财务备份，成功后不随每次打开恢复删除/重命名项。备份严格空白种子识别兼容19/177/144目录。新账本仍共享全局分类，不复制分类。


### 改底部菜单样式选择

先读 BottomNavigationSettingsFragment、BottomNavigationPreferences、MainActivity.refreshBottomNavigationStyle 和 ElasticBottomNavigationView.setDocked。默认悬浮；吸附时 page bottomToTop=nav_shell，非吸附/二级页 bottomToBottom=parent。菜单尺寸由同一 shell 测量，只有悬浮模式添加滚动尾部/FAB额外避让；样式偏好参与完整/云备份。


### 改账单页浮动操作显示

账单吸附底栏尾部留白：MainActivity.applyFloatingNavigationInsets仅对LedgerFragment的整月recycler_records调用LedgerScrollTail，吸附且菜单可见时使用ledger_docked_scroll_tail=32dp，悬浮仍按原XML112dp基线及max(基线,悬浮高度+gap)计算。保留floatingViewBaselines原值，月份ViewHolder绑定后post重新应用，避免新月/复用列表漏设；不要将此修复扩到统计/资产/设置、日内列表、FAB位置或系统导航边距。

先读 `LedgerFragment.kt`、`util/LedgerFloatingActionsController.kt` 与 DESIGN 的 Floating action button。只把当前月份外层列表的滚动事件传给控制器；其他月份、布局恢复不应触发方向隐藏。向下/向上累计 24dp 隐藏/恢复，惯性完全结束后 1.5 秒恢复；回顶启用门槛为两屏、退出门槛为 1.5 屏，继续遵守 `ScrollTopFabHelper` 设置。月份切换、回顶过程、手动打断及暂停/销毁统一处理，不能另行直接改 FAB visibility 与控制器竞争。分页加载、月份/列表滚动位置恢复、底栏避让和日卡片留白规则保持原样。

### 改底部导航样式

先读 `DESIGN.md` 的 Navigation shell、`activity_main.xml`、`MainActivity.kt`、`bottom_nav_menu.xml`、`bg_navigation_dock.xml`、`values/navigation_colors.xml`、导航颜色 selector、`view/ElasticBottomNavigationView.kt` 及导航文字样式。当前按用户 FlClash 截图使用悬浮胶囊：21dp 两侧、系统导航区之上 8dp 底部留白、60dp 最小高度、22dp 图标、12sp 标签、图标/标签容器 2dp 间距并整体垂直居中、选中底色包住图标和文字，无右侧开始/暂停按钮。不论三/四/五项，胶囊保持五项宽度，入口等距分布。页面容器覆盖整个可用窗口，滚动尾部、新增按钮及 AI 输入区单独避让悬浮底栏，见 `MainActivity.applyFloatingNavigationInsets`。只为整月 `recycler_records` / 整页 NestedScrollView 添加滚动尾部空间，不能给日内 `recycler_day_records` 或资产内层列表追加底部避让，否则每天卡片会产生大块空白。原生条目背景/波纹透明，避免双圈。弹性胶囊自绘，原生 active indicator 关闭；核对触摸取消、快速切换、布局恢复、系统动画关闭及离页清理，保留二级页隐藏契约。图片背景模式下胶囊为不透明中性灰、1dp 中性描边和 6dp 阴影，四周透明，不受卡片不透明度影响；选中层使用单独的较强中性色阶。

### 改账本 / 记录 / 设置等页面

先读对应 Fragment，再读关联布局、Adapter、资源文件。

「我的」列表样式集中在SettingsRowLayout.bind/styleRow：10个选项结果ID显式识别并放右侧箭头前，14sp次级色、剩余文本空间42%限宽/末尾省略，真实说明留标题下；不要再把结果与说明一起移入左列。保留所有TextView ID及其完整text、开关和原点击/保存契约，小窗/字体变化由布局回调重算限宽。

「我的」收支颜色/周起始日/图片数量菜单由SettingsFragment.showSettingsChoiceMenu委托SettingsChoiceMenu（PopupWindow+ListView）及纯SettingsChoiceMenuGeometry处理，不再使用显示后追加padding的ListPopupWindow。完整测量换行行高、勾选/selector占位和上下8dp内边距，菜单宽度可超过锚点但限制在可见应用窗口8dp安全边距内；下方能完整容纳就向下，否则上方空间更多时向上。两侧都放不全时取较大一侧限高内部滚动，空间相同优先下方；双侧连一整项都容纳不了时回退窗口内展示。能放全时从首项开始，溢出才定位勾选项。窗口变化重算、锚点离屏/移除及宿主销毁关闭；不修改已注册的选择/保存回调。

分类新增/编辑当前入口为CategoryManageFragment→CategoryEditorSheetFragment，使用CategorySheetUi共用圆角底部表单及合并/删除弹层；不是旧AddCategoryFragment整页或dialog_add_category系统弹窗。保留CATEGORY_SAVED_RESULT，新增category_editor_action只转发已保存ID的合并/删除请求；草稿有关闭确认且不随合并提交，图标选择器返回Dialog供生命周期清理。目标列表搜索完整路径，合并确认明细可展开；不要更改事务、迁移或删除保护规则。

### 改“记一笔” / “编辑记录”录入页

先读：

1. `app/src/main/java/com/example/cardtally/AddRecordFragment.kt`
2. `app/src/main/java/com/example/cardtally/EditRecordFragment.kt`
3. `app/src/main/res/layout/fragment_add_record_quick.xml`（当前两种模式共用的录入壳）
4. `app/src/main/java/com/example/cardtally/adapter/RecordAssetSheetAdapter.kt`
5. `app/src/main/java/com/example/cardtally/adapter/RecordCategoryTreeAdapter.kt`
6. `app/src/main/res/layout/bottom_sheet_record_*.xml`
7. `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`

注意：两种模式共用 `fragment_add_record_quick.xml`，区别在页内分类列表；金额键盘常驻，日期和资产仍使用底部选择器。

页内新增分类见`RecordCategoryAddSheetFragment`、`CategorySelectorAdapter.onAddCategory`、`RecordCategoryGroupAdapter.onAddCategory`及`DatabaseHelper.getRecordCategoryParents/addLeafCategoryForRecord`→`CategoryWriteRepository`。尾部添加项仅由AddRecordFragment显式开启，不参与选择、拖拽或排序ID；转账隐藏，重复记账等旧宿主不自动增加。一个弹窗内切换表单/一级分类选择/暂拟一级分类；最终父子同事务保存，同名复用且校验叶子，已被账单/有效撤销/重复任务直接引用的一级分类不做父级。FragmentResult只更新同类型新分类选择，保留录入草稿；打开/关闭及恢复重建须维护openSheet/金额键盘/系统IME契约，不能在新增弹窗恢复时再打开旧分类树选择器。

一级选择使用上图标/下名称三列网格，列表下方常驻新一级图标+名称输入，不再有展开新建按钮；已有选择与新草稿互斥，parentIcon/pendingIcon分别保存，最终通过newParentIcon可选参数同事务落库。两处图标入口统一经openIconPicker等待IME隐藏/动画结束/窗口稳定，再在本宿主局部configureIconPicker限75%并给图标内容弹性高度；不要改共享IconPickerDialog其他宿主，也不要用固定延时替代窗口检查，离页/停止时须取消等待。

收支可不选择资产，见 `docs/requirements/decisions/2026-10-04-optional-record-assets.md`。`RecordAssetPickerBottomSheetFragment` 的 `allowNoAsset` 默认关闭，普通收支开启后以 `NO_ASSET_ID = 0` 回传清空，宿主保存 null；转账、重复记账及默认资产设置不启用此入口。修改该回调必须核对所有宿主，不能把清空标记当真实资产 ID。

### 改重复记账 / 转账资产选择

账户详情入口见 `AssetRecordsFragment.openRecord` / `openTransfer` 与 `AddRecordFragment.newAssetRecordInstance` / `newTransferInstance`：两者以当前账本和当前可用资产 ID 初始化草稿。「记一笔」只预选本资产为支出账户，收入和转账仍沿用原逻辑；直接「转账」预选本资产为转出方，转入方为空。仅初始化草稿，后续用户选择及重建恢复优先，不修改全局默认资产；保存沿用原记账事务，返回详情重新读取余额及流水。共享资产可转账，不以管理所有权限制记账。

先读：

1. `app/src/main/java/com/example/cardtally/RecurringRecordEditFragment.kt`
2. `app/src/main/java/com/example/cardtally/RecurringRecordsFragment.kt`
3. `app/src/main/java/com/example/cardtally/RecordAssetPickerBottomSheetFragment.kt`
4. `app/src/main/java/com/example/cardtally/AssetFragment.kt`
5. `app/src/main/java/com/example/cardtally/adapter/AssetAdapter.kt`
6. `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`
7. `docs/requirements/decisions/business_rules.md`

注意：普通「记一笔」和重复记账共用资产选择器，通过 `FragmentResult` 回传资产 ID 与是否为转入资产；分别验证两个宿主的结果处理及弹层关闭行为。任务账本通过 `pickerLedgerId` 限制资产列表，不应改变当前账本。默认资产设置使用独立的 `DefaultRecordAssetPickerBottomSheetFragment` 结果契约。

默认支出/收入账户与普通/编辑/重复记账通过 `AssetPickerSheetFragment` 复用 `bottom_sheet_default_record_asset_picker.xml`、`DefaultRecordAssetPickerAdapter` 和纯筛选器 `DefaultAssetPickerOptions`，不再内嵌 AssetFragment。两个具体Fragment保留独立结果契约：默认设置仍是 `selected_id`/`for_income`，记账仍是 `selected_asset_id`/`select_destination`。保留普通收支可选无资产（ID0）及转账排除另一侧账户规则，重复记账继续传任务ledgerId并复用原getAllAssets(ledgerId)范围，不改默认账户清空语义。共享层只返回选择、不保存默认值，保留记账页键盘shown/dismissed回调；超过5个账户显示搜索，最多占可用窗口75%。

默认选择器按资产页的资金/信用/充值/理财/应收/应付类型分组；置顶仅在所属组内靠前，空组和搜索后无结果组不展示，不另设重复的置顶列表。修改分类标签映射时同时核对 `AssetFragment` 与 `DefaultAssetPickerOptions.groupOf`，定位选中项须计入标题行。

### 改数据库或数据展示

手动分类合并见2026-10-23-manual-category-merge决策、CategoryMergePlanner、CategoryMergeRepository、DatabaseHelper.previewCategoryMerge/mergeCategories及CategoryManageFragment。先规划兼容目标，确认完整路径及跨账本影响数量，再在事务内重新校验并合并；同时迁移账单/撤销的分类ID与名称/路径快照、重复任务、备份导入映射和排序别名，不调用会影响余额的保存/删除业务。叶子/整组分别处理，结构冲突或历史非叶子绑定拒绝；不要与自动目录迁移保留历史名称的策略混同。

先读：

1. `DatabaseHelper.kt`
2. 相关 `model/*.kt`
3. 发起查询或渲染数据的 Fragment / Adapter

数据协作者边界：

- 记录行映射：`database/RecordSqlMapper.kt`
- 记录查询与分页：`database/RecordReadRepository.kt`
- 记录写入/删除撤销事务编排：`database/RecordWriteRepository.kt`
- 记录写入规则校验：`database/RecordWriteValidator.kt`
- 收支/转账手续费对资产余额的效果：`database/RecordAssetBalanceRepository.kt`
- 重复记账模板读写：`database/RecurringRecordRepository.kt`
- 分类只读查询及路径：`database/CategoryReadRepository.kt`
- 分类写入与排序：`database/CategoryWriteRepository.kt`
- 新数据库默认分类初始化：`database/CategoryDefaultsSeeder.kt`
- 分类父子关系校验：`database/CategoryHierarchyValidator.kt`
- 分类树展示顺序：`database/CategoryTreeOrdering.kt`
- 资产行读取与映射：`database/AssetReadRepository.kt`
- 资产元数据及归档/排序写入：`database/AssetWriteRepository.kt`
- AI 会话与消息 SQLite 读写：`database/AiChatRepository.kt`
- 统计总额、手续费和周期聚合 SQL：`database/RecordStatisticsRepository.kt`
- 账本/资产池及资产归属查询：`database/LedgerReadRepository.kt`
- 账本/共享资产池写入与合并：`database/LedgerWriteRepository.kt`
- 建表与数据库版本升级编排：`database/DatabaseSchemaCreator.kt`、`database/DatabaseSchemaUpgradeManager.kt`

这些类由 `DatabaseHelper` 保持兼容门面调用；表/索引常量、版本升级配置和剩余账本 UI 编排仍在 `DatabaseHelper`，后续调整需保持原事务与资产池作用域语义。

### 改 AI 助手 / MiniMax 对话 / 会话持久化

先读：

1. `app/src/main/java/com/example/cardtally/AgentFragment.kt`
2. `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`
3. `app/src/main/java/com/example/cardtally/adapter/AgentChatAdapter.kt`
4. `app/src/main/java/com/example/cardtally/adapter/AgentSessionAdapter.kt`
5. `app/src/main/java/com/example/cardtally/model/AiChatMessage.kt`
6. `app/src/main/java/com/example/cardtally/model/AiChatSession.kt`
7. `app/src/main/java/com/example/cardtally/util/AiAssistantSettingsHelper.kt`
8. `app/src/main/java/com/example/cardtally/network/MiniMaxClient.kt`
9. `app/src/main/res/layout/fragment_agent.xml`
   - API 配置任务另读 `AiAssistantSettingsFragment`、`fragment_ai_assistant_settings.xml`、`network/ApiEndpoints.kt`、`ApiModelClient.kt`、`ApiModelCatalog.kt` 和 `docs/requirements/decisions/2026-10-04-api-model-discovery.md`；新增偏好字段同时核对 `BackupArchiveManager.expectedPreferenceType`。
10. `docs/collaboration/skills/android-gradle-serial-verification.md`（如果需要跑构建 / 单测 / 真机验证）

账单工具任务另读 `docs/requirements/decisions/2026-10-04-ai-record-tools.md`、`ai/RecordToolProtocol.kt`、`RecordToolClient.kt`、`AiRecordAssistant.kt`、`AiRecordEngine.kt`、`AiRecordAuditStore.kt`、`view_ai_record_action.xml`、`RecordWriteRepository.kt` 与 `RecordWriteValidator.kt`；同时核对 `AiChatMessage.isLocalOnly`、聊天持久化与普通聊天请求过滤，不能自动重发工具查询结果。

### 做真机截图 / 页面取证 / UI 回归留档

先读：

1. `skills/adb-current-screen-screenshot.md`
2. 如同时涉及构建或测试，再读 `docs/collaboration/skills/android-gradle-serial-verification.md`

注意：当前仓库推荐先用 `adb devices` 确认设备状态；如果有多台设备，后续截图命令必须带 `-s <serial>`。如果设备提示存在多个 display，或默认截图出现黑图，先执行 `adb shell dumpsys SurfaceFlinger --display-id`，再改用 `screencap -d <display-id>`；需要留档时默认保存到根目录 `screenshot/`。

### 做 Debug 编译或 APK 安装

先读：

1. `skills/android-build-debug.md`
2. `skills/android-install-debug-apk.md`
3. 如同时涉及测试或整轮验证，再读 `docs/collaboration/skills/android-gradle-serial-verification.md`

注意：当前仓库内 Gradle 构建与测试默认串行；安装前先确认 `app/build/outputs/apk/debug/app-debug.apk` 已生成。

注意：`verification` flavor 会把变体名拆开，日常命令写作 `:app:assembleDebug` / `:app:testDebugUnitTest` 仍可用（同 `:app:assembleEverydayDebug` / `:app:testEverydayUnitTest`）；会重置数据库的 device 测试只能跑 `:app:connectedVerificationDebugAndroidTest`，它以 `.verification` applicationId 安装并与日常数据隔离。项目只保留 `dev`、`verification` flavor，release 通过 build type 使用 `.release` applicationId。

### 改主题 / 样式 / 视觉一致性

先读：

1. `util/ThemeHelper.kt`
2. `MainActivity.kt` 与 `res/values/styles.xml`
3. `res/values/*.xml` 及受影响页面的资源文件
4. `util/ThemeColorHelper.kt`（如果涉及 Kotlin 运行时取色）
5. 受影响页面布局 / Adapter / Fragment
6. `DESIGN.md`

注意：当前支持浅色、深色、跟随系统和图片背景，默认浅色；主题入口为 `ThemeHelper`、`SettingsFragment` 与 `values-night`，视觉方向以 `DESIGN.md` 为准，不要继续沿用已删除的历史设计指南或 Stitch 导出。

图片模式见 `2026-10-05-wallpaper-appearance.md`：模式 3 使用独立样式与单独保存的浅色/深色/跟随系统配色（默认深色，`theme_prefs/wallpaper_palette`），页面根背景为 `?attr/pageBackgroundColor`，Activity 持有固定图片和中性遮罩；`view/StableWallpaperImageView` 按排除系统栏、不含键盘缩小的窗口高度居中裁切，Android 11+ 使用 currentWindowMetrics，旧版结合根 IME/navigation insets 与可见期间参考高度；页面继续 adjustResize。键盘开关不能改变背景缩放和位置，窗口大小/配置变化需重算，图库预览不使用该视图；页面卡片使用 `?attr/cardSurfaceColor` / `ThemeColorHelper.resolveCardSurface`，图片模式为 80% 不透明，其余模式实底。卡片内部普通行不要再铺同一底色：`LedgerDateGroupAdapter` 在 `DateGroupAdapter.RecordViewHolder.bind` 传 `parentProvidesBackground=true`，日期组父卡片绘制唯一底色，内部账单行透明；独立条目默认自行绘底，每次绑定重置，选中高亮及归档滑动行保留实底；弹层和导航实底。切换模式或图片配色保存后重建 Activity，不能仅靠 night mode 更新，因为普通与图片模式可能共享同一夜间模式。图片样式继承配色对应的系统栏；遮罩与卡片颜色通过 `wallpaper_colors.xml` 的浅色/夜间资源切换。新增页面时沿用画布属性，勿用不透明背景盖住图片。

卡片不透明度滑杆见 `AppearanceSettingsFragment`、`ThemeHelper.getCardOpacity`、`values/card_opacity_styles.xml`：图片模式默认 80%，可按 5% 步长调至 0–100%，保存后松手应用；外观页 `scroll_appearance` 必须保留稳定 ID，使 NestedScrollView 随页面重建恢复当前位置；Activity 在 `super.onCreate` 处理夜间模式之后、布局加载之前叠加颜色属性覆盖，防止基础主题重应用覆盖当前不透明度。切换任何背景/图片配色前保存滑杆当前值，图片三种配色共用这一值，不分别记忆。偏好键为 `theme_prefs/card_opacity` 和图片配色 `theme_prefs/wallpaper_palette`，修改持久化时核对 `BackupArchiveManager` 白名单和 `DataTransferManager` 旧整数恢复，不能通过整卡 View alpha 降低文字可读性。

图片库见 `WallpaperHelper`、`WallpaperGalleryAdapter` 与外观页的 `OpenDocument` 回调：32MB 输入上限、采样/方向归一，新增 UUID WebP 文件到私有 `appearance/wallpapers/`；四张用户指定 PNG（`res/drawable-nodpi/appearance_wallpaper.png` 与 `res/raw/appearance_wallpaper_option_*.png`）为默认图片 1、2、3、4，按固定 ID 与顺序直接从打包资源列出/采样解码，不再复制 JPG 或读写 seed 标记；此前已删除备选图也重新可用，原选择 ID 保留，本地 `selected_wallpaper` 文件原子保存经过校验的图片 ID；旧 `appearance/wallpaper.webp` 保留为图库条目，缺少选择标记时继续使用旧图。添加不覆盖旧图，恢复默认仅切换选择，导入失败/取消清理本次临时文件，不先删原图。四张默认图均不提供删除入口且存储层拒绝删除，只有用户导入/旧自选图的缩略图右上角 × 使用独立回调和 48dp 触区；删除当前图前原子保存默认选择，删文件失败尝试恢复原选择。删除后台串行执行后重建但不启用图片模式，保留原配色/不透明度及滚动恢复，不能复用默认会启用图片的导入成功逻辑。横向 RecyclerView 展示默认图片 1、2、3、4 / 旧图 / 新增图，视口两侧固定 16dp margin，不能以允许绘制穿透的 padding 模拟边距；适配器采用 PREVENT_WHEN_EMPTY 延后恢复位置，重建时不再主动定位选中图。缩略图后台按需解码并缓存，复用取消旧任务且回调核对条目 ID，离开页面关闭线程与缓存；预览/全屏共用绑定及弱缓存。内置图作为恢复/缺图回退，无外部 URI 偏好或新增权限。图库与选择在 v3 完整备份中导出，旧 v2 仍可导入；恢复使用内容去重与选择映射，须核对 BackupArchiveManager 和 WallpaperHelper，不导出不可移植的本机路径。

外观选择在 `AppearanceSettingsFragment` / `fragment_appearance_settings.xml` 独立二级页，分为纯色、图片两张卡片，各直接提供浅色/深色/跟随系统；两组全页互斥，点选另一卡片即切换背景，状态恢复期间只同步不触发保存，`SettingsFragment` 仅保留入口与模式摘要。调整该页需同时核对 `MainActivity` 的重建后导航归属、返回栈及底部导航隐藏行为。

分类管理半透明卡片后的编辑/删除见 `CategoryAdapter`、`item_category.xml` 与 `SwipeToEditDeleteHelper` 的 `clipCoveredActions=true`：操作层初始 INVISIBLE，拖动及吸附动画按前卡片右边界裁切，只绘制已露出部分，收起时隐藏；不能通过改成实底取消用户选定的不透明度。绑定/回收先 `dispose()` 取消旧动画、长按及布局监听，再恢复闭合状态。共享 helper 其他宿主默认不开启裁切，修改时也核对归档资产回调。

分类支出/收入样式见 `fragment_category_manage.xml`、`item_category_type_tab.xml`、`bg_category_type_indicator.xml` 与 `CategoryManageFragment.styleTypeTab`：保持原生 TabLayout、48dp 点击区；标签透明，只有父卡片铺 `cardSurfaceColor`，16sp 字体及选中加粗/未选中 muted 色与统计页一致，28dp × 2dp 横线原生 180ms 过渡。不要重新给标签设置实底 `bg_category_tab_white`。

### 改文案 / 国际化 / 语言切换

先读：

1. `app/src/main/java/com/example/cardtally/util/LanguageHelper.kt`
2. `app/src/main/res/values/strings.xml`
3. `app/src/main/res/values-en/strings.xml`
4. 受影响页面对应的 `Fragment` 和 `layout`
5. 如涉及设置入口，再读 `SettingsFragment.kt` 和 `fragment_settings.xml`

补充现实：

- 语言切换入口在“我的”页 `SettingsFragment`
- 当前已做过一轮减闪处理，但真机切换中英文仍会轻微闪屏
- 后续目标是更平滑的淡入淡出过渡，而不是维持当前闪动效果
- 如需继续排查，连同 `MainActivity`、`activity_main.xml` 与主题 / 窗口动画资源一起看

### 改产品结构 / 新页面框架

先读：

1. `docs/requirements/plans/2026-03-26-cardtally-implementation-plan.md`
2. 相关页面职责和 IA 文档
3. 当前旧页面实现

不要直接按旧页面一比一延续；这个仓库仍处于“旧结构”向“新骨架”过渡阶段。

## 关于页 / GitHub 更新

先读 `docs/requirements/decisions/2026-10-07-about-github-updates.md`，再核对 `AboutFragment.kt`、`update/GitHubRelease.kt`、`update/AppUpdateViewModel.kt`、`fragment_about.xml`、`fragment_settings_v2.xml`、Manifest 与 FileProvider 路径。版本以 `app/build.gradle` 为准；云端发布输入须递增 versionCode，包名、签名及渠道资产命名须与已安装包一致。

## 云备份 / 自动备份

先读 `docs/requirements/decisions/2026-10-07-cloud-backup.md` 与完整备份决策，再核对 `cloud/CloudBackupFragment`、`CloudSettings`、`CloudBackupManager`、`CloudStore`、`CloudCrypto`、`CloudBackupWorker` 及原备份/图库代码。当前仅支持HTTPS WebDAV，未验证的S3入口/字段/签名/协议实现已移除；旧非WebDAV自动目标必须先停用并返回空配置，不能自动切到休眠WebDAV或重用其他提供方凭据。确认保存WebDAV时替换旧云连接配置，断开仍清除所有本机云凭据，不删除远端。Wi-Fi 必须使用实际 transport 判断，不能仅依赖 UNMETERED；清理仅当前设备自动历史，默认 15。恢复预检缓存不得在正式确认时再次联网下载。相关 JVM 用例位于 `app/src/test/java/com/example/cardtally/cloud/`，未执行；真实协议验证需要指定服务和用户另外要求。
