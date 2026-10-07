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

### 改账单页浮动操作显示

先读 `LedgerFragment.kt`、`util/LedgerFloatingActionsController.kt` 与 DESIGN 的 Floating action button。只把当前月份外层列表的滚动事件传给控制器；其他月份、布局恢复不应触发方向隐藏。向下/向上累计 24dp 隐藏/恢复，惯性完全结束后 1.5 秒恢复；回顶启用门槛为两屏、退出门槛为 1.5 屏，继续遵守 `ScrollTopFabHelper` 设置。月份切换、回顶过程、手动打断及暂停/销毁统一处理，不能另行直接改 FAB visibility 与控制器竞争。分页加载、月份/列表滚动位置恢复、底栏避让和日卡片留白规则保持原样。

### 改底部导航样式

先读 `DESIGN.md` 的 Navigation shell、`activity_main.xml`、`MainActivity.kt`、`bottom_nav_menu.xml`、`bg_navigation_dock.xml`、`values/navigation_colors.xml`、导航颜色 selector、`view/ElasticBottomNavigationView.kt` 及导航文字样式。当前按用户 FlClash 截图使用悬浮胶囊：21dp 两侧、系统导航区之上 8dp 底部留白、60dp 最小高度、22dp 图标、12sp 标签、图标/标签容器 2dp 间距并整体垂直居中、选中底色包住图标和文字，无右侧开始/暂停按钮。不论三/四/五项，胶囊保持五项宽度，入口等距分布。页面容器覆盖整个可用窗口，滚动尾部、新增按钮及 AI 输入区单独避让悬浮底栏，见 `MainActivity.applyFloatingNavigationInsets`。只为整月 `recycler_records` / 整页 NestedScrollView 添加滚动尾部空间，不能给日内 `recycler_day_records` 或资产内层列表追加底部避让，否则每天卡片会产生大块空白。原生条目背景/波纹透明，避免双圈。弹性胶囊自绘，原生 active indicator 关闭；核对触摸取消、快速切换、布局恢复、系统动画关闭及离页清理，保留二级页隐藏契约。图片背景模式下胶囊为不透明中性灰、1dp 中性描边和 6dp 阴影，四周透明，不受卡片不透明度影响；选中层使用单独的较强中性色阶。

### 改账本 / 记录 / 设置等页面

先读对应 Fragment，再读关联布局、Adapter、资源文件。

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

收支可不选择资产，见 `docs/requirements/decisions/2026-10-04-optional-record-assets.md`。`RecordAssetPickerBottomSheetFragment` 的 `allowNoAsset` 默认关闭，普通收支开启后以 `NO_ASSET_ID = 0` 回传清空，宿主保存 null；转账、重复记账及默认资产设置不启用此入口。修改该回调必须核对所有宿主，不能把清空标记当真实资产 ID。

### 改重复记账 / 转账资产选择

账户详情的「转账」入口见 `AssetRecordsFragment.openTransfer` 与 `AddRecordFragment.newTransferInstance`：以当前账本和当前可用资产 ID 初始化转账草稿，预选转出方，转入方为空；保存沿用原记账事务，返回详情重新读取余额及流水。共享资产可转账，不以管理所有权限制记账。

先读：

1. `app/src/main/java/com/example/cardtally/RecurringRecordEditFragment.kt`
2. `app/src/main/java/com/example/cardtally/RecurringRecordsFragment.kt`
3. `app/src/main/java/com/example/cardtally/RecordAssetPickerBottomSheetFragment.kt`
4. `app/src/main/java/com/example/cardtally/AssetFragment.kt`
5. `app/src/main/java/com/example/cardtally/adapter/AssetAdapter.kt`
6. `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`
7. `docs/requirements/decisions/business_rules.md`

注意：普通「记一笔」和重复记账共用资产选择器，通过 `FragmentResult` 回传资产 ID 与是否为转入资产；分别验证两个宿主的结果处理及弹层关闭行为。任务账本通过 `pickerLedgerId` 限制资产列表，不应改变当前账本。默认资产设置使用独立的 `DefaultRecordAssetPickerBottomSheetFragment` 结果契约。

### 改数据库或数据展示

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

图片库见 `WallpaperHelper`、`WallpaperGalleryAdapter` 与外观页的 `OpenDocument` 回调：32MB 输入上限、采样/方向归一，新增 UUID WebP 文件到私有 `appearance/wallpapers/`；三张用户指定 PNG（`res/drawable-nodpi/appearance_wallpaper.png` 与 `res/raw/appearance_wallpaper_option_*.png`）为默认图片 1、2、3，按固定 ID 与顺序直接从打包资源列出/采样解码，不再复制 JPG 或读写 seed 标记；此前已删除备选图也重新可用，原选择 ID 保留，本地 `selected_wallpaper` 文件原子保存经过校验的图片 ID；旧 `appearance/wallpaper.webp` 保留为图库条目，缺少选择标记时继续使用旧图。添加不覆盖旧图，恢复默认仅切换选择，导入失败/取消清理本次临时文件，不先删原图。三张默认图均不提供删除入口且存储层拒绝删除，只有用户导入/旧自选图的缩略图右上角 × 使用独立回调和 48dp 触区；删除当前图前原子保存默认选择，删文件失败尝试恢复原选择。删除后台串行执行后重建但不启用图片模式，保留原配色/不透明度及滚动恢复，不能复用默认会启用图片的导入成功逻辑。横向 RecyclerView 展示默认图片 1、2、3 / 旧图 / 新增图，视口两侧固定 16dp margin，不能以允许绘制穿透的 padding 模拟边距；适配器采用 PREVENT_WHEN_EMPTY 延后恢复位置，重建时不再主动定位选中图。缩略图后台按需解码并缓存，复用取消旧任务且回调核对条目 ID，离开页面关闭线程与缓存；预览/全屏共用绑定及弱缓存。内置图作为恢复/缺图回退，无外部 URI 偏好或新增权限。图库与选择在 v3 完整备份中导出，旧 v2 仍可导入；恢复使用内容去重与选择映射，须核对 BackupArchiveManager 和 WallpaperHelper，不导出不可移植的本机路径。

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

先读 `docs/requirements/decisions/2026-10-07-cloud-backup.md` 与完整备份决策，再核对 `cloud/CloudBackupFragment`、`CloudSettings`、`CloudBackupManager`、`CloudStore`、`S3Signer`、`CloudCrypto`、`CloudBackupWorker` 及原备份/图库代码。Wi-Fi 必须使用实际 transport 判断，不能仅依赖 UNMETERED；清理仅当前设备自动历史，默认 15。恢复预检缓存不得在正式确认时再次联网下载。相关 JVM 用例位于 `app/src/test/java/com/example/cardtally/cloud/`，未执行；真实协议验证需要指定服务和用户另外要求。
