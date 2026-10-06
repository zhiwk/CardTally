# Current Snapshot

## 当前有效快照

以下按时间记录交接事实；较早条目中的页面、主题和验证状态可能已被上方新条目取代，接手时仍以当前源码为准。

- 2026-10-06：用户已在 GitHub master 提交 release 工作流（`25fb706`），使用 `0.0.2 / versionCode 2` 手动触发运行 `37490346288`。读取失败日志确认 Debug 源码与测试编译完成，129 项 JVM 测试中 2 项失败：`AgentSessionTitleHelperTest` 的固定日期断言隐含 Asia/Shanghai、云端 UTC 跨日；`AssetTypeIconCatalogTest` 仍期待已替换的旧品牌图标。会话标题 helper 增加可传入时区，默认仍按设备本地时区、每次独立格式化；日期测试显式指定时区并覆盖同一时刻 UTC/上海不同日期及跨年午夜边界，资产测试同步当前七款独立品牌图标资源。已同步远端工作流提交并保留此前本地模板修改，静态复核本次源码、测试、资源引用和差异；本轮未重新构建或运行测试、修改 Secrets 或触发 Actions。失败运行尚未进入签名还原、release 打包和 Release 创建；修复后的云端运行与设备页面验证仍待完成。

- 2026-10-06：用户明确 GitHub 首次发布从 `0.0.1 / versionCode 1` 开始，下次为 `0.0.2 / 2`。本地 release 工作流默认值与提示同步修改，内部版本号校验范围由 2–2100000000 改为 1–2100000000；此前最低 2 是工作流人为限制，不是 GitHub 首次发布要求。后续发布仍需手动填写递增版本号，未增加自动递增；本轮仅修改与静态复核，尚未同步用户已在 GitHub 网页提交的工作流，未运行构建或测试、推送或触发 Actions。

- 2026-10-06：为用户在 GitHub 编辑器配置云端发布准备 `.github/workflows/release.yml`，仅手动触发并限制 master，输入 X.Y.Z 版本名称和递增 versionCode。Ubuntu 24.04 / JDK 17 / Gradle 8.13 / SDK 34，因 wrapper JAR 未跟踪而直接由官方 Gradle Action 安装 Gradle；版本和 SDK 路径只修改云端工作副本，不回写源码。串行运行 Debug JVM 单测后，通过四个 Android 签名 Secrets 临时还原签名文件，构建 release，核对签名有效性、包名、版本及非调试属性，附 APK 和 SHA-256 创建 Release 草稿，最终清理签名文件；三项 Action 固定已核对的提交 SHA，禁用构建缓存。已阅读工作流并静态核对 shell / 内嵌 Python 语法与 Gradle 字段，本轮未运行构建或测试、读取或上传签名密钥、推送文件、触发 Actions 或创建实际 Release；云端首次运行及用户页面验证仍待完成。

- 2026-10-05：README 改为 GitHub 首页结构，突出小猫记帐介绍、功能概览、导航和 dev 快速开始，使用指南、备份、签名发布及验证说明折叠展示；补充 macOS / Linux 首次执行 wrapper 的权限设置。按用户上传要求整理本轮 README、应用名称与图标、AI 欢迎语及相关文档，静态复核改动文件、7 个 XML、README 本地链接及图标原图一致性，差异空白检查通过；未纳入本机配置、签名材料、APK、日志或财务数据。本轮未重新构建、运行测试或安装，AI 欢迎语仍仅有静态复核证据。

- 2026-10-05：修正 AI 助手固定欢迎语，移除“已接入 MiniMax”及固定 Provider 描述，中英文改为小猫记帐 AI 助手、财务交流、开启账单操作后可请求当前账本查增改删，以及写入前核对确认。空会话新增欢迎语沿用原持久化入口；已存旧欢迎语在读取当前会话和会话预览时，只对首条、非错误/非工具本地消息、无思考且全文匹配两种旧欢迎语的 assistant 消息替换内存内容，保留消息 ID、时间及数据库历史，其他用户/模型回复不变。无数据库迁移、真实 API 请求或工具权限调整；本轮仅修改及静态复核，未编译、运行测试或安装。

- 2026-10-05：按用户要求串行编译并覆盖安装 dev、release，包含圆脸小猫应用图标及“小猫记帐”名称。首轮 dev 构建成功但包信息核对发现 debug 专用 app_name 覆盖主资源，已补改中英文 debug 名为“小猫记帐 (Dev)”以保留安装区分。最终 `:app:assembleEverydayDebug` 成功（19 秒），`app-debug.apk` 包名为 `com.example.cardtally`、可调试，中英文名称及自适应图标核对通过，ADB `install -r` 返回 `Success`。随后 `:app:assembleEverydayRelease` 成功（68 秒），发布 Lint 通过，`app-release.apk` 包名为 `com.example.cardtally.release`、不可调试，中英文名为“小猫记帐”；发布资源经过路径优化，已按 APK 实际路径核对 adaptive-icon 前/背景结构，签名与保存的发布密钥一致，ADB `install -r` 返回 `Success`。两套数据保留，临时 SDK 配置按本轮执行前字节恢复、wrapper 清理；仓库外日志为 `dev-cat-brand-build-2026-10-05.log` / `release-cat-brand-build-2026-10-05.log`。本轮未运行单元/设备测试或启动应用检查页面，桌面显示实际效果尚待用户查看。

- 2026-10-05：按用户要求将选定的圆脸小猫抱账本图标（`cardtally-app-icon-v6-round-cat.png`）用于应用，并将中英文 `app_name` / `app_name_en` 统一为“小猫记帐”。原 PNG 按原始字节复制到 drawable-nodpi，移除五套旧密度图标；Android 7 使用同名 mipmap-anydpi 位图入口，Android 8+ 使用白底自适应图标与四边 13% 前景内缩，Manifest 普通/圆形图标均沿用该入口。包名、签名、数据库及偏好不变。同步 README、DESIGN 与图标来源说明。本轮仅修改与静态复核，未编译、运行测试或安装；手机尚未应用新名称和图标。

- 2026-10-05：按用户要求编译并覆盖安装 master 当前提交 `190c515` 的最新 release，包含近期累计修改及分类操作区裁切、切换栏样式。串行执行 `:app:assembleEverydayRelease` 成功（78 秒），发布 Lint 通过；`app/build/outputs/apk/release/app-release.apk` 签名与保存的发布密钥一致，包名为 `com.example.cardtally.release`，不可调试。ADB `install -r` 返回 `Success`，保留 release 数据，dev 未操作。本轮临时 SDK 配置按执行前原始字节恢复，wrapper 副本清理，本机日志为 `release-master-build-2026-10-05.log`；未运行单元/设备测试或启动应用检查页面。此前已将六个提交快进合入并推送 master，main 也已创建但保留原 master 默认分支；本条为安装交接事实，尚未提交上传。

- 2026-10-05：按用户要求进行 GitHub 上传前静态检查。移除 174 个仅换行格式变化的差异，其余文件按原行尾格式保留内容；变更前副本保存在仓库外。待提交范围包含近期累计代码、资源、决策和交接改动，190 个新增/修改 XML 可解析，差异空白检查通过；源码与设备分组清单均为 14 组 / 147 项，修正文档残留 142 项及数据库版本注释。未发现真实凭据、私有设备地址或数据文件，签名材料、local.properties、APK/日志及截图不纳入提交；默认背景图片为用户指定打包资源。已静态核对 AI 写入仅走最终原生确认、过期/重复执行防护和备份先验签再解密入口。沿用上一轮 dev 编译/覆盖安装成功证据，本轮未重跑构建或测试，静态检查不替代运行回归。GitHub HTTPS 登录已完成，上传目标为现有 `codex/consolidate-existing-work` 分支，保留已有 5 个本地提交，不自动合并主分支。

- 2026-10-05：按用户要求编译并覆盖安装最新 dev，包含分类编辑/删除操作区遮挡裁切及支出/收入切换栏透明度、文字与短横线样式调整。串行执行 `:app:assembleEverydayDebug` 成功（38 秒），`app/build/outputs/apk/debug/app-debug.apk` 核对为可调试的 `com.example.cardtally`；ADB `install -r` 返回 `Success`，保留 dev 数据。临时 SDK 配置已恢复、wrapper 副本已清理，本机日志为 `dev-category-appearance-build-2026-10-05.log`。构建仅有既有 API/Gradle 弃用警告，无编译错误；本轮未运行单元/设备测试或启动应用验证页面，滑动遮挡、复用及切换栏实际效果仍待页面验证。

- 2026-10-05：按用户统计页参考统一分类管理支出/收入样式。移除 TabLayout 标签的实底 `bg_category_tab_white`，只保留父卡片 `cardSurfaceColor`，透明度沿用当前设置；自定义居中 16sp 标签，选中主文字色加粗、未选中统计页同一 muted 色，28dp × 2dp 主文字色短横线用原生 180ms 动画过渡。保持原生 TabLayout、48dp 点击高度、分类加载与切换逻辑，视图重建同步 currentType；未增加横滑或 ViewPager2。同步 DESIGN、决策和入口，无偏好/数据库迁移。本轮仅修改与静态复核，未编译、运行测试或安装，手机 dev 尚不包含此前操作区遮挡及本次切换栏样式修改。

- 2026-10-05：修复用户截图所示分类管理编辑/删除图标透过半透明卡片可见的问题。`item_category.xml` 操作层初始 INVISIBLE；`CategoryAdapter` 开启共享 `SwipeToEditDeleteHelper.clipCoveredActions`，拖动/吸附及尺寸变化按前卡片右边界裁切操作区，仅显示露出部分，收起时隐藏。绑定/回收释放旧 helper 并取消动画、长按、布局监听，恢复闭合；其他宿主裁切默认关闭，已静态核对归档资产调用。保留分类卡片不透明度及编辑/删除回调与滑动阈值，同步 DESIGN、决策和入口，无偏好/数据库迁移。本轮仅修改与静态复核，未编译、运行测试或安装，手机 dev 尚不包含此修复。

- 2026-10-05：按用户要求编译并覆盖安装最新 dev，包含三张「默认图片 1、2、3」固定陈列、不可删除及直接使用打包资源的规则。串行执行 `:app:assembleEverydayDebug` 成功（26 秒），`app/build/outputs/apk/debug/app-debug.apk` 核对为可调试的 `com.example.cardtally`；ADB `install -r` 返回 `Success`，保留 dev 数据。临时 SDK 配置已恢复、wrapper 副本已清理，本机日志为 `dev-default-wallpapers-build-2026-10-05.log`。本轮未运行单元/设备测试或启动应用验证页面，三张默认图展示、切换和旧选择恢复的实际效果仍待页面验证。

- 2026-10-05：按用户要求将三张提供的图片统一为「默认图片 1、2、3」，固定顺序和既有 ID，全部隐藏/禁用 × 且删除存储入口在写入前拒绝默认 ID；只有导入及 legacy 自选图可删。三张默认图直接从打包资源列出/采样解码（关闭密度缩放），不再依赖私有 JPG/seed 标记；旧副本/标记不主动删除，之前删除过的两张备选图重新可用，既有有效选择 ID 保留。自选图从图片 1 编号，恢复默认/删除当前自选图仍选择默认图片 1。同步中英文、DESIGN、决策和入口；无偏好/数据库迁移。本轮仅修改与静态复核，未编译、运行测试或安装，手机 dev 尚不包含此次默认图规则。

- 2026-10-05：按用户要求编译并覆盖安装最新 dev，包含图库 × 按钮缩至 24dp 圆形底色 / 16dp 图标并靠近右上角的调整。串行执行 `:app:assembleEverydayDebug` 成功（19 秒），`app/build/outputs/apk/debug/app-debug.apk` 核对为可调试的 `com.example.cardtally`；ADB `install -r` 返回 `Success`，保留 dev 数据。临时 SDK 配置已恢复、wrapper 副本已清理，本机日志为 `dev-wallpaper-delete-size-build-2026-10-05.log`。本轮未运行单元/设备测试或启动应用验证页面，按钮实际视觉效果仍待页面验证。

- 2026-10-05：按用户截图缩小图库 × 并靠近缩略图右上角。圆形底色 32dp→24dp，图标容器 20dp→16dp，距顶部/尾侧 4dp，48dp 独立触区不变；背景与波纹遮罩定位同步，逻辑方向支持尾侧对齐。仅修改 `item_wallpaper_image.xml` 与 `bg_wallpaper_delete.xml` 及设计/交接说明，删除规则和图库数据不变。本轮仅静态复核，未编译、测试或安装，手机 dev 尚不包含此次尺寸/位置调整。

- 2026-10-05：按用户要求编译并覆盖安装最新 dev，包含图库 × 删除按钮和两张随应用提供的备选 JPEG 及一次初始化/删除不再出现逻辑。串行执行 `:app:assembleEverydayDebug` 成功（46 秒），`app/build/outputs/apk/debug/app-debug.apk` 核对为可调试的 `com.example.cardtally`；ADB `install -r` 返回 `Success`，保留 dev 数据。临时 SDK 配置已恢复、wrapper 副本已清理，本机日志为 `dev-wallpaper-options-build-2026-10-05.log`。本轮未运行单元/设备测试或启动应用验证页面，两图展示/切换及当前/非当前图片删除的实际效果仍待运行验证。

- 2026-10-05：将用户提供的两张 JPEG 原样保存到 `res/raw/appearance_wallpaper_option_one.jpg` / `option_two.jpg`，作为图库备选。首次读取图库后台按稳定 UUID 复制 JPG 到私有图库并原子写入每图 seed 标记，默认图后陈列两项，保留当前背景、配色与不透明度；复用选择、缩略图和 × 删除，删除仅移除本机副本并保留初始化标记，重进/覆盖升级不重新出现。无新主题偏好/依赖/权限/数据库迁移，图库和标记不纳入财务备份。仅修改及静态复核，未编译、运行测试或安装，手机 dev 尚不包含删除入口及两张备选图。

- 2026-10-05：图库自选缩略图（含旧 legacy）右上角新增 × 删除，默认图隐藏且存储层拒绝删除默认 ID。按钮独立回调、48dp 触区、浅深实底圆形背景和辅助功能说明，复用绑定时重置显示与禁用状态；删除与其他外观操作后台串行，当前图先原子选默认再删文件，失败尝试恢复原选择。成功刷新时保留原背景模式/配色/不透明度及滚动恢复，不误启用图片模式，缓存丢弃已删条目引用。无新权限/依赖/偏好/迁移；本轮仅修改及静态复核，未编译、运行测试或安装，手机 dev 尚不包含删除入口。

- 2026-10-05：按用户要求编译并覆盖安装最新 dev，包含外观页纵向滚动位置恢复、图库异步加载后横向位置恢复及两侧固定 16dp 边距。串行执行 `:app:assembleEverydayDebug` 成功（38 秒），APK `app/build/outputs/apk/debug/app-debug.apk` 核对为可调试的 `com.example.cardtally`；ADB `install -r` 返回 `Success`，保留 dev 数据。临时 SDK 配置已恢复、wrapper 副本已清理，本机日志为 `dev-appearance-scroll-build-2026-10-05.log`。本轮未运行单元/设备测试或启动应用验证页面，滑杆应用后位置及图库边距的实际表现仍待运行验证。

- 2026-10-05：修复外观页调节不透明度后跳到页首及图库贴边。外层 NestedScrollView 添加稳定 `scroll_appearance` ID，随原有 Activity 重建保存/恢复纵向位置；图库用两侧 16dp 外边距限定视口，移除可穿透留白的 clipToPadding=false 和 padding。图库适配器 PREVENT_WHEN_EMPTY 等后台加载条目后恢复横向位置，重建时不再主动跳到选中图，首次进入仍定位当前选择。不改变主题、不透明度数值或图片库保存规则，无新增偏好。本轮仅修改与静态复核，未编译、运行测试或安装，手机 dev 尚不包含该修复。

- 2026-10-05：按用户要求编译并覆盖安装最新 dev，包含本机背景图片库和账单日期组卡片不透明度叠加修复。串行执行 `:app:assembleEverydayDebug` 成功（44 秒），`app/build/outputs/apk/debug/app-debug.apk` 核对为可调试的 `com.example.cardtally`；ADB `install -r` 返回 `Success`，保留 dev 数据。临时 SDK 配置已恢复、wrapper 副本已清理，本机日志为 `dev-wallpaper-library-build-2026-10-05.log`。本轮未运行单元/设备测试或启动应用验证页面，多图导入/切换和透明度实际效果尚待运行验证。

- 2026-10-05：外观页增加本机背景图片库。添加图片不再覆盖旧图，保存为私有 UUID WebP 条目，经过校验的本地选择标记原子更新；原单图文件作为 legacy 保留，无标记时继续使用。默认/旧图/导入图以横向缩略图陈列，当前图显示边框与文字标记，点击切换；恢复默认只切换，不清空图库。保持当前配色与不透明度；缩略图后台按需有界解码、4MiB 缓存、复用取消旧任务及核对绑定 ID，离开页面关闭任务与线程。沿用输入/采样/方向处理限制，无新权限、依赖、主题偏好键或数据库迁移；图库及选择仍不纳入财务备份。本轮仅修改及静态复核，未编译、运行测试或安装，已安装 dev 尚不包含图库及前条不透明度叠加修复。

- 2026-10-05：修复用户截图所示月统计与账单组不透明度差异。源码确认日期组外层 MaterialCardView 与内部 RecordViewHolder 均绘制 `cardSurfaceColor`，半透明重复叠加。共享条目绑定新增默认 false 的 `parentProvidesBackground`，仅 `LedgerDateGroupAdapter` 日内条目传 true，普通条目背景改透明，父卡片唯一绘底并显式透明前景；独立列表仍自行绘底，选中高亮不变，每次绑定重置避免复用串用。无偏好/数据库/业务规则变动，同步 DESIGN、决策和任务入口。本轮仅修改与静态复核，未编译、运行测试或安装。

- 2026-10-05：按用户要求编译并覆盖安装最新 dev，包含纯色/图片两张外观卡片、图片配色跟随系统以及当前不透明度应用顺序和切换保持调整。串行执行 `:app:assembleEverydayDebug` 成功（37 秒），`app/build/outputs/apk/debug/app-debug.apk` 确认为可调试的 `com.example.cardtally`；ADB `install -r` 返回 `Success`，保留 dev 数据。临时 SDK 配置已恢复、wrapper 副本已清理，本机日志为 `dev-appearance-cards-build-2026-10-05.log`。本轮未运行单元/设备测试或启动应用检查页面，外观布局、系统配色响应与切换不透明度尚待运行验证。

- 2026-10-05：按用户截图重整外观页为纯色背景、图片背景两张 12dp 卡片，各包含浅色/深色/跟随系统，六项全页互斥，点击图片任一项直接启用；图片预览为 180dp 圆角居中裁切，图片按钮并排，保留说明与滑杆。图片 `wallpaper_palette` 增加系统值 2，复用备份键，无迁移。用户报告切换不透明度问题：原先单一 `card_opacity` 并无按配色记忆，但主题覆盖在 `super.onCreate` 前可能被夜间模式基础样式重应用覆盖；改为其后/布局前覆盖，并在所有背景或配色切换前保存滑杆当前值，图片三配色共享当前数值。恢复视图时禁止选择回调写入，再按偏好同步；导入中六项禁用，非图片时滑杆禁用。扩展既有设备主题用例源码验证 30%/55% 切换及跟随系统持久恢复和 alpha，补充 JVM 映射源码，未增加设备用例；同步 README、DESIGN、决策和入口。本轮仅静态复核，未编译、运行测试或安装。

- 2026-10-05：按用户要求编译并覆盖安装最新 dev，包含自选背景图片及图片背景浅色/深色配色。串行 `:app:assembleEverydayDebug` 构建成功（44 秒），产物 `app/build/outputs/apk/debug/app-debug.apk` 核对为可调试的 `com.example.cardtally`；ADB `install -r` 返回 `Success`，保留 dev 数据。临时 SDK 配置恢复、wrapper 副本清理，本机日志为 `dev-wallpaper-palette-build-2026-10-05.log`；本轮未运行单元/设备测试或启动应用检查页面，图片导入及浅深切换效果尚待运行验证。

- 2026-10-05：图片背景外观新增独立浅色/深色配色选择，`theme_prefs/wallpaper_palette` 保存 0/1，缺失/非法类型/非法值回退深色，沿用旧图片模式值 3。MainActivity 根据图片配色应用夜间模式；图片样式继承基础主题系统栏，卡片覆盖引用浅色白/深色深灰颜色，遮罩同步浅/深；图片及卡片不透明度不重置。外观页非图片模式或导入期间禁用配色项，我的页摘要显示图片背景配色。完整备份白名单与旧整数导入支持新键，无数据库迁移；自选文件不备份的限制保持。扩展既有主题设备用例源码检查图片浅深切换/重建/卡片颜色，同步 README、DESIGN、决策和入口。本轮仅静态复核，未构建、运行测试或安装。

- 2026-10-05：按用户要求支持选择本机图片作为背景。外观页新增选择/恢复默认按钮和当前图片预览，通过系统 `OpenDocument(image/*)` 取图，无全相册权限和持久外部 URI。`WallpaperHelper` 在后台限流复制（32MB）、采样解码至约 1600px、处理可读取 EXIF 方向并压缩 WebP，同目录原子替换私有文件；失败/取消保留旧背景，临时文件清理。导入期间禁用外观控件，离开页面取消任务；成功后自动启用图片模式并重建，不重置卡片不透明度。Activity 与预览共用绑定/弱缓存，缺图或坏图回退内置图，可恢复默认。无新依赖、数据库迁移或偏好键；自选背景文件尚未纳入备份，文档已说明恢复缺图时回退默认。同步设计/决策/README/任务入口；本轮仅静态复核，未构建、运行测试或安装。

- 2026-10-05：按用户要求编译并覆盖安装包含卡片透明度和不透明度滑杆的最新 dev。串行执行 `:app:assembleEverydayDebug` 成功（47 秒），产物 `app/build/outputs/apk/debug/app-debug.apk` 核对为可调试的 `com.example.cardtally`；ADB `install -r` 返回 `Success`，保留 dev 数据，release 未操作。临时 SDK 配置已恢复、wrapper 副本已清理，本机日志为 `dev-card-opacity-build-2026-10-05.log`。本轮未运行单元/设备测试或启动应用检查页面，滑杆松手应用、备份恢复与真机透明度效果尚未进行运行验证。

- 2026-10-05：用户在卡片透明度调整过程中进一步要求可调节，已在外观页加入「卡片不透明度」Slider 与百分比，0–100%、5% 步长、默认 80%，仅图片模式启用。拖动保存整数偏好 `theme_prefs/card_opacity`，松手重建应用；键盘/TalkBack 调整短暂停顿后应用，销毁页面取消回调。`ThemeHelper` 归一数值及异常类型，Activity 加载布局前应用单一颜色属性覆盖，XML/底图/运行时卡片一致；不改变文字、图标与金额 alpha。完整备份白名单及旧格式整数恢复支持新键，无数据库迁移。与前条卡片透明度改动一起交付；本轮仅静态复核，未构建、运行测试或安装，手机 dev 尚不包含这些调整。

- 2026-10-05：按用户截图为图片背景模式的中性页面卡片增加约 20% 透明度。新增 `cardSurfaceColor`：默认引用原 `surface_light`，图片模式为 `#CC1D2024`；XML 卡片、共享圆角底图及运行时账单/银行/账本/分类/归档组卡片复用属性。文字、金额和图标不改变 alpha；清理统计内层、分类内层与资产普通行的重复底色，避免遮图或叠加变暗。资产选择高亮保持实底，每次绑定重置普通行透明；归档滑动行仍用实底遮住后方操作按钮。模态容器、导航、输入与图片遮罩保持原值，其他外观视觉沿用原配色。同步 README、设计、决策及入口；本轮仅静态复核，未构建、运行测试或安装，手机 dev 尚不包含该透明度调整。

- 2026-10-05：按用户要求编译并覆盖安装最新 dev，包含独立外观页、图片背景外观和账户详情转账入口。串行执行 `:app:assembleEverydayDebug` 成功（45 秒），产物 `app/build/outputs/apk/debug/app-debug.apk` 核对为可调试的 `com.example.cardtally`；ADB `install -r` 返回 `Success`，保留 dev 数据，release 未操作。临时 SDK 配置已恢复、wrapper 副本已清理，本机日志为 `dev-wallpaper-transfer-build-2026-10-05.log`。本轮未运行单元/设备测试或启动应用检查页面，实际图片效果、主题切换及账户转账交互仍待运行验证。

- 2026-10-05：按用户提供的图片新增第四项「图片背景」外观，模式值为 3，默认浅色和原三项保持；独立外观页含单选、原图预览和裁切说明，我的页摘要同步。用户附件实际为 WebP，按原始字节保存到 `drawable-nodpi/appearance_wallpaper.webp`；Activity 仅在该模式加载全屏图片，居中裁切并叠加中性深色遮罩。页面及月份页画布统一使用 `pageBackgroundColor`，图片模式透明，其他模式沿用原底色；深色卡片、弹层、抽屉、输入和导航保持实底。模式切换统一保存后重建，由 Activity 应用配色/样式，解决深色与图片同属 night mode 时不触发更新的问题；原返回栈和状态恢复继续复用。无新偏好键或数据库迁移，原备份整数契约兼容。同步设计/决策/README/入口，扩展既有主题测试源码，设备用例数不变；本轮仅静态复核，未构建、运行测试或安装。

- 2026-10-05：账户详情页「转账」接入既有 `AddRecordFragment`，通过初始 `RecordFormState` 和账本参数直接打开转账类型，以当前账户 ID 预选转出方，转入方为空、日期为当天，状态沿用原草稿恢复机制。入口核对当前账本资产池中的未归档资产，不按所有权限制共享资产记账；无效账户提示返回资产页选择，不按名称猜测。保存继续使用原转账校验和 SQLite 事务（转出扣金额加手续费、转入加金额），返回账户详情重新读取余额、名称/类型与流水。无数据库迁移；同步 README 与任务入口。本轮仅修改和静态复核，未构建、运行测试或安装。

- 2026-10-05：按用户要求将「我的 → 外观」从单选弹窗改为独立二级页。新增 `AppearanceSettingsFragment` 与 XML，复用二级返回栏、现有主题卡片/文字资源及三项单选，立即通过原 `ThemeHelper` 保存并应用。选项区域可滚动，二级页自动隐藏底部导航；`MainActivity` 重建恢复时将外观页归属到「我的」，返回后恢复正确导航和最新模式摘要。原主题偏好值、默认浅色和跟随系统规则保持不变，无新数据库迁移。扩展既有主题设备测试源码覆盖入口、三项状态、主题/页面重建与返回导航，不新增设备用例；本轮仅静态复核，未构建、运行测试或安装。

- 2026-10-04：按用户要求编译并覆盖安装最新 release，包含 AI 多工具调用兼容修复及“不选择”资产卡片调整。串行执行 `:app:assembleEverydayRelease` 成功（56 秒），发布 Lint 通过；产物 `app/build/outputs/apk/release/app-release.apk` 签名与项目保存的密钥一致，包名为 `com.example.cardtally.release` 且不可调试。ADB `install -r` 返回 `Success`，保留 release 数据，dev 未操作。签名配置及密钥继续被 Git 忽略且未上传，临时 SDK 配置已恢复、wrapper 副本已清理；本机日志为 `release-ai-multicall-build-2026-10-04.log`。本轮未运行单元/设备测试或启动应用，真实 AI 工具流程仍待用户实际重试。

- 2026-10-04：按用户要求编译并覆盖安装包含 AI 多工具调用兼容修复的最新 dev。`:app:assembleEverydayDebug` 串行构建成功（11 秒）；核对产物 `app/build/outputs/apk/debug/app-debug.apk` 为可调试的 `com.example.cardtally`，ADB `install -r` 返回 `Success`，保留 dev 数据，release 未操作。临时 SDK 配置已恢复、wrapper 副本已清理，本机日志为 `dev-ai-multicall-build-2026-10-04.log`。本轮未运行单元/设备测试、未启动应用或调用真实服务，多查询后单条写入确认的实际交互仍待验证。

- 2026-10-04：用户新截图确认 AI 账单请求因模型返回多个工具调用被旧单调用限制拒绝。已修正协议及控制器：每响应支持最多 8 个已注册且 ID 唯一的调用，多个查询先完整校验参数，再依次按现有作用域/事务/审计执行；一次追加完整 assistant 调用数组及各 ID 对应结果后继续请求。混合或多个写入不执行、不产生预览，只返回未执行回执并自动要求模型单独重提一条完整写入；原生最终确认、每轮最多成功写入一条、成功后无追加写入保持不变。每轮模型请求及总工具调用均最多 12 次，超限停止；无新数据库迁移。更新决策、README 和协议/客户端测试源码，设备用例数保持 147；本轮仅静态复核，未构建、运行测试、安装或调用真实服务，已安装 dev 尚不包含该兼容修复。

- 2026-10-04：按用户要求编译并覆盖安装最新 dev，包含“不选择”资产卡片位置/样式及 AI 工具错误分类提示。串行执行 `:app:assembleEverydayDebug`，构建成功（30 秒）；核对 APK 为可调试的 `com.example.cardtally`，产物为 `app/build/outputs/apk/debug/app-debug.apk`。ADB `install -r` 返回 `Success`，保留 dev 数据，release 未操作。临时 `local.properties` 已恢复，wrapper 临时副本已清理；本机日志为 `dev-picker-diagnostics-build-2026-10-04.log`。本轮未运行单元/设备测试、未启动应用做页面验证或调用真实 AI 服务；原截图所示 AI 失败的确切分类仍待实际重试确认。

- 2026-10-04：按用户截图调整资产选择器“不选择”样式与位置。移除底部弹层壳内的独立按钮，改在 `AssetFragment` 滚动内容中、页面标题之后及首个资产分类之前显示无分类标题的单行卡片；复用 `item_asset` 与现有分组卡片圆角、底色和内边距，不显示类型或余额，空选择时显示选中底色与勾选标记。新增默认关闭的子页面参数与清空回调，普通收支继续回传原 `NO_ASSET_ID`，转账、重复记账和默认资产宿主保持不提供入口。保留既有条目操作 ID，现有交互测试仍通过该 ID 点击条目；本轮仅静态复核，未构建、运行测试或安装，手机 dev 尚不包含此次样式调整。

- 2026-10-04：排查 AI 账单工具“可能不支持”提示。源码确认原控制器将 HTTP 请求拒绝和所有工具响应解析失败合并提示，多调用响应也进入同一提示；截图不能证明实际属于哪一类，更不能直接认定所选模型不支持工具调用。已拆分安全错误分类：保留 HTTP 拒绝状态码，区分多调用、未注册工具、参数格式、空回复和响应结构异常，不输出或保存 Provider 原始错误内容。维持每响应一个调用及最终写入确认策略；补充协议/客户端测试源码，仅静态复核，未构建、运行测试、安装或请求真实服务。手机已安装的 dev 尚不包含此次错误提示调整。

- 2026-10-04：按用户要求构建并覆盖安装最新 dev，包含 AI 仅最终写入确认及收支资产“不选择”改动。首次构建发现按钮误用 `app:checkable` 导致资源链接失败，按 Material 1.11.0 资源定义改为 `android:checkable`；串行重建 `:app:assembleEverydayDebug` 成功（18 秒）。包名为 `com.example.cardtally`，产物 `app/build/outputs/apk/debug/app-debug.apk`，ADB 覆盖安装返回 `Success`，保留 dev 数据，release 未操作。临时 SDK 配置已恢复，wrapper 临时副本已清理；构建日志在本机工具链目录 `dev-optional-asset-build-2026-10-04*.log`。本轮未运行单元/设备测试、未启动应用或调用真实 AI 接口，确认流程及无资产录入尚未进行运行验证。

- 2026-10-04：按用户要求为新增/编辑收支账单增加“不选择”资产。共用资产弹层增加受 `allowNoAsset` 控制的置顶入口，默认不启用，普通收支开启后回传清空标记，宿主保存空资产 ID/名称并保留类型记忆、重建和再记状态；既有默认资产偏好仍生效，编辑保留原值。转账及重复记账/默认资产设置宿主不启用清空入口。AI 工具同步允许收支创建省略/null 资产，修改省略保留、null 清空，最终预览显示无资产及余额影响；转账仍要求两账户。现有表和余额事务已支持空值，无新迁移。修改相关测试源码，设备总数保持 147；本轮仅实施和静态复核，未构建、运行测试、安装或调用真实 AI 接口。决策见 `docs/requirements/decisions/2026-10-04-optional-record-assets.md`。

- 2026-10-04：按用户进一步要求，AI 账单工具改为仅最终写入确认。开启工具后，查询、读取账户/叶子分类选项及同页上下文复用自动进行，取消逐查询及跨轮授权弹窗；信息缺失在聊天中询问。完整写入建议直接打开预览，一次原生确认执行一条新增/修改/删除；成功后本轮只提供只读工具，控制器同时拒绝追加写入。自动查询保留日志但不记录原生确认时间，原有作用域、过期、快照和防重复执行保护保留；无新数据库迁移。同步开关/确认文案、业务决策和测试源码（设备总数仍 147）；本轮仅实施及静态复核，未构建、运行测试、调用真实模型或安装，上一条 dev 安装尚不包含此次调整。

- 2026-10-04：按用户要求编译并覆盖安装包含 API 模型发现和 AI 账单工具的 dev。`:app:assembleEverydayDebug` 构建成功（1 分 36 秒），包名核对为 `com.example.cardtally`，产物 `app/build/outputs/apk/debug/app-debug.apk`；ADB `install -r` 返回 `Success`，保留 dev 数据，release 未操作。构建临时使用本机 JDK 17 与 Android SDK，仓库 wrapper 的 CRLF 通过临时副本处理，原文件未改写，`local.properties` 已恢复。本轮未运行单元/设备测试、未启动应用或调用真实 AI 接口；v42 升级、确认交互及工具实际调用尚未运行验证。构建日志保存在本机工具链目录 `dev-ai-record-build-2026-10-04.log`。

- 2026-10-04：按用户新要求加入可选 AI 账单增删改查。API 配置增加默认关闭的账单工具开关与本机操作日志查看/清除；工具模式使用 OpenAI Chat Completions function calling 非流式请求，普通聊天保留原流式实现。注册工具为账户/叶子分类选项、账单分页查询/单条读取，以及单条新增、修改、删除（含转账账单）；限定发起时当前账本，模型未知字段、多调用、SQL或普通文本均不执行。每次通过原生卡片与确认页核对预览，确认同时逐请求授权结果/本轮工具上下文外发；跨轮沿用上下文也重新授权，退出页面丢弃内存上下文与待确认操作。写入前核对 5 分钟有效期、快照指纹、当前账本/会话/API 配置，复用原记录事务与余额/手续费规则，操作身份只可消费一次。SQLite v42 增加聊天 `local_only` 字段与 `ai_record_audit` 表，旧日常数据仅增量升级；工具消息可本地持久化/备份，普通聊天双层过滤防止自动重发；日志不随完整备份导出。原生回执先显示，后续模型失败不重复写入。新增协议/参数 JVM 测试及 5 项隔离数据测试源码，设备分组预期 147 项；本轮只做修改和静态复核，未编译、运行测试、调用真实模型接口或安装，数据库迁移和确认交互尚无运行验证。详细范围及验证清单见 `docs/requirements/decisions/2026-10-04-ai-record-tools.md`。

- 2026-10-04：按用户要求，API 配置调整为“服务 URL → API Key → 测试并获取模型 → 下拉选择 → 保存”。新配置 URL/模型为空；保存过的有效配置（包括旧版仅存 Key 时依赖的 MiniMax 默认值）保持兼容。支持 OpenAI 兼容基础地址与标准完整对话地址，模型发现使用同一服务的 GET models 接口和 Bearer Key，不发送聊天或财务数据；获取成功不代表所有模型均有对话权限。URL/Key 改变清空本页模型选择并取消旧请求，页面销毁取消请求；加入鉴权、超时、空列表及不支持接口提示。三项配置一次写入，新增显式配置偏好已纳入完整备份类型白名单，无数据库迁移。新增 URL/模型响应 JVM 测试源码，更新配置缺省及既有设备测试源码，设备用例数量保持不变；本轮仅修改和静态复核，未编译、运行测试、调用真实模型接口或安装。决策见 `docs/requirements/decisions/2026-10-04-api-model-discovery.md`。

- 2026-10-03：按用户要求编译并覆盖安装最新 release，包含深色适配及账单/统计 ViewPager2 改动、统计页闪退修复与 16dp 翻页间距。`:app:assembleEverydayRelease` 构建成功（1 分 24 秒）；APK 签名与项目保存的密钥一致，包名 `com.example.cardtally.release`、不可调试，ADB 安装返回 `Success`，保留 release 数据；未运行测试或启动应用做页面验证，`local.properties` 已恢复，签名材料继续保持忽略且未上传。

- 2026-10-03：按用户要求重新编译并覆盖安装包含另一会话统计页间距改动的 dev；`:app:assembleEverydayDebug` 构建成功（19 秒），ADB 安装返回 `Success`，保留 dev 数据；未运行额外测试或真机页面检查，`local.properties` 已恢复。

- 2026-10-03：统计支出／收入翻页增加 16dp 页间距，使用 ViewPager2 MarginPageTransformer 和共享 spacing_l 资源，让滑动中的图表与排行榜卡片分开；停稳后页面宽度与对齐保持原状。本轮仅静态复核，未构建、测试或安装。

- 2026-10-03：按用户要求编译并覆盖安装统计页闪退修复后的 dev；`:app:assembleEverydayDebug` 构建成功（23 秒），ADB 安装返回 `Success`，保留 dev 数据；未运行测试或启动统计页做运行验证，`local.properties` 已恢复。

- 2026-10-03：修复进入统计页闪退。崩溃日志为 `Pages must fill the whole ViewPager2 (use match_parent)`；两页预先以 null 父容器 inflate，未生成 LayoutParams，附加时被默认参数覆盖。页面适配器创建 ViewHolder 时显式设置 RecyclerView.LayoutParams，宽高均 MATCH_PARENT；已有统计翻页设备测试补充附加页尺寸断言。仅读取已有崩溃日志并静态复核，本轮未构建、运行测试或安装。

- 2026-10-03：按用户要求编译并覆盖安装统计 ViewPager2 与指示线过渡改动后的 dev；`:app:assembleEverydayDebug` 构建成功（39 秒），ADB 安装返回 `Success`，保留 dev 数据；未操作 release，未运行测试或真机页面验证，`local.properties` 已恢复。

- 2026-10-03：统计页改为 ViewPager2 支出/收入双页面，统计图与排行榜一起跟手移动，保留公共周期及汇总；顶部点击执行平滑翻页，选中横线与文字颜色按 onPageScrolled 连续过渡。两页独立图表和排行适配器，共用期间与模式，空态和排行展开后重新计算页面高度；移除旧 StatisticsSwipeLayout 手势。新增指示线随翻页进度的设备测试源码，设备分组预期 142 项。本轮仅静态复核，未编译、测试或安装。

- 2026-10-03：按用户要求编译并覆盖安装统计左右滑动改动后的 dev；`:app:assembleEverydayDebug` 构建成功（28 秒），ADB 安装返回 `Success`，保留 dev 数据；未操作 release，未运行测试或启动应用做页面验证，`local.properties` 已恢复。

- 2026-10-03：统计页新增左滑收入、右滑支出，与顶部点击共用类型切换入口，保留时间范围、图表和排行模式。使用共享统计布局的方向锁定与距离/速度判定，上下滚动、短滑、多指及系统边缘不触发切换；不循环。该页不是 ViewPager2，不提供跟手翻页动画。本轮仅静态复核，未编译、测试或安装。

- 2026-10-03：按用户要求构建并覆盖安装最新 dev，包含 ViewPager2 月份翻页、年份连续不循环和剩余银行夜间资源。`:app:assembleEverydayDebug` 构建成功（1 分 2 秒），APK 安装返回 `Success`，保留 dev 数据；未操作 release，未运行测试或启动应用做页面验证，`local.properties` 已恢复。

- 2026-10-03：账单月份选择器的年份改为 1–9999 连续范围，关闭年份循环，支持持续上下滑动；月份仍为原有 1–12 选择。本轮仅静态复核，未编译、测试或安装。

- 2026-10-03：账单页采用 ViewPager2 1.1.0 原生月份翻页，右滑上月、左滑下月；账本标题/搜索/日历及浮动按钮固定，统计和账单列表一起移动。月份页独立维护汇总、分页游标与滚动位置，仅保留邻近月份数据，返回刷新及重建恢复当前月份，选择器跳转不经过中间页。新增月份索引单测和翻页设备测试源码，设备分组预期更新为 141 项；本轮仅静态复核，未构建、运行测试或安装，手势与视觉验收待真机验证。

- 2026-10-03：修复银行夜间资源遗漏：补齐另外 25 个不同路径格式的白底银行，包括齐商、青岛、青海、上海农商、上饶和深圳农商；天府黑色标识使用圆形浅色衬底，保留品牌路径。已逐资源静态复核，所有带白色画布的银行矢量均有夜间对应资源；本轮未编译、测试或安装。

- 2026-10-03：按用户要求编译并覆盖安装最新 dev，包含账单条目背景统一与资产品牌图标深色适配。`:app:assembleEverydayDebug` 构建成功（23 秒），`app/build/outputs/apk/debug/app-debug.apk` 安装返回 `Success`，保留 dev 数据；未操作 release，未运行测试或真机页面检查，`local.properties` 已恢复。

- 2026-10-03：补齐资产品牌图标深色适配：32 个银行矢量及花呗增加透明底夜间资源，保留原色与矢量比例；PNG 银行图标仅在深色去除与边缘相连的白色留边并按资源缓存，内部白色细节保留。共用绑定用于资产、归档、银行选择和表单，浅色原始资源不变。本轮仅静态复核，未编译、测试或安装。

- 2026-10-03：账单普通条目及多选未选中条目的背景改用 `surface_light`，与上方统计卡片一致，浅色与深色共用；保留多选选中状态的语义底色。本轮仅源码静态复核，未编译、测试或安装。

- 2026-10-03：按用户要求编译并安装深色模式改动后的 dev。`:app:assembleEverydayDebug` 构建成功（46 秒），产物 `app/build/outputs/apk/debug/app-debug.apk`；ADB 覆盖安装返回 `Success`，保留 dev 数据，未操作 release。本轮未运行单元或设备测试、未启动应用做页面验证；`local.properties` 已恢复。

- 2026-10-03：新增「我的 → 外观」浅色 / 深色 / 跟随系统，默认浅色，使用现有主题偏好与 Material DayNight；`values-night` 覆盖共享颜色，普通卡片、图标、账本管理与分类背景移除固定浅色。切换时保留恢复后的主导航页，品牌图标保持原色。决策见 `2026-10-03-dark-appearance.md`，替代旧仅浅色限制；主题相关测试源码已同步，本轮仅静态复核，未构建、运行测试或安装。

- 2026-10-03：按确认方案统一银行图标的可见尺寸。共用绑定入口为银行图标包裹独立 Drawable，按标识非白／非透明边界居中、等比填满现有容器；边界按资源缓存，采样位图即时回收，原始矢量／PNG 不改写。资产、归档、银行选择、资产表单共用该逻辑，其他品牌图标、数据及数据库版本不变；列表复用继续恢复普通图标尺寸和染色。本次仅静态复核，未编译、测试或安装，尚无运行视觉验收结论。

- 2026-10-03：按用户要求，“再记”创建新录入页时同时携带当前叶子分类 ID，新增与编辑后的入口共用此逻辑；标准模式恢复已有选择时展开所属一级分类。日期和资产继续保留，转账仍无分类；恢复沿用现有叶子分类校验。本次未构建、测试或安装。

- 2026-10-03：按用户要求编译、安装银行选择页改动后的 release。首次构建发现本机签名相对路径仍按项目根而非 app 模块解析，已将被忽略的 `keystore.properties` 更新为 `storeFile=../cardtally-release.jks`，密钥仍在项目根目录且未提交或上传。随后 `:app:assembleEverydayRelease` 构建成功，发布 Lint 通过，APK 签名与保存的密钥一致，包名为 `com.example.cardtally.release` 且不可调试。ADB `install -r` 返回 `Success`，覆盖安装保留 release 数据，dev 未操作；本次未运行额外测试或启动应用。产物为 `app/build/outputs/apk/release/app-release.apk`，两次构建日志存于本机工具链目录 `release-bank-build-2026-10-03*.log`；`local.properties` 已恢复。

- 2026-10-03：按用户要求编译、安装银行选择页改动后的 dev Debug。`:app:assembleEverydayDebug` 构建成功（44 秒），ADB `install -r` 返回 `Success`，保留 dev 数据；release 未操作。产物为 `app/build/outputs/apk/debug/app-debug.apk`，日志位于本机工具链目录 `dev-bank-build-2026-10-03.log`。本次未运行测试或启动应用，v41 迁移将在应用下次打开数据库时执行；`local.properties` 已恢复。

- 2026-10-03：储蓄卡、信用卡入口增加本地银行选择页，共 63 家银行及“其他银行”，按常用与字母分组；银行图标使用打包资源并保留原色，新建和编辑资产及普通／归档资产列表同步展示。银行标识沿用 `category_icon_name`，卡片分类保持原值；数据库 v41 一次性将已有两类卡片（含所有账本与归档资产、旧版无标签 type=1）设为其他银行，仅更新图标字段。旧备份的未识别银行展示时回退其他银行。新增业务决策与图标来源／许可记录。本次未构建、测试或安装，升级迁移尚未在设备执行。

- 2026-10-03：按用户要求编译并安装包含七类品牌资产图标的 dev Debug 版本。`:app:assembleEverydayDebug` 构建成功，产物为 `app/build/outputs/apk/debug/app-debug.apk`；使用 ADB `install -r` 覆盖安装返回 `Success`，保留 dev 数据，release 包未操作。本次未执行测试或启动应用；构建日志保存在本机工具链目录，`local.properties` 已由构建入口恢复。

- 2026-10-03：继续按用户截图为花呗、白条、借呗新增独立品牌矢量图标，替换此前借用的支付宝、京东、微信图标；复用统一目录与保留品牌颜色的绑定逻辑，类型选择、新建资产及已有资产列表同步生效。本次未构建、测试或安装。

- 2026-10-03：按用户截图将支付宝、微信钱包、QQ 钱包、京东资产改为彩色圆形品牌图标，共用图标目录供类型选择、新建资产和资产列表使用；绑定时清除统一染色并填满原图标圆形容器，列表复用时恢复普通图标尺寸与染色。已有分类标签匹配的资产直接显示新图标，不修改数据库；旧支付宝/微信类型无标签时也使用品牌图标。本次未构建、测试或安装。

- 2026-10-02：用户确认已导出备份并要求编译、安装 release。因原发布密钥遗失，已生成新的发布密钥及签名配置；按用户随后要求，将同一密钥保存到项目根目录 `cardtally-release.jks`，本机 `keystore.properties` 改为引用该相对路径，两者均由 Git 忽略，未提交或上传。仓库外保留新密钥备份及旧 Windows 配置。`:app:assembleEverydayRelease` 构建成功，发布构建 Lint 通过，APK 签名已核对为新密钥，包名为 `com.example.cardtally.release` 且不可调试。覆盖安装被 Android 以签名不匹配拒绝后，按用户已完成导出的上下文卸载旧 release 并安装新 APK，两步均返回 `Success`；旧 release 本地数据随卸载清除，备份由用户在新版本导入。日常 dev 包未操作；本次未运行额外测试或启动应用。产物为 `app/build/outputs/apk/release/app-release.apk`，日志为 `app/build/reports/verification/release-2026-10-02/`；`local.properties` 已恢复，工具链移至用户目录持久保存。

- 2026-10-01：账单详情相关 Debug 构建成功；相关 JVM 单测 16/16 通过。隔离设备 UI 用例首次 8/9 通过，发现并修复编辑后“再记”读取新 Fragment 未初始化数据库的问题，重新构建后该失败用例复测通过；删除/手续费数据库用例 11/11 通过，共 20 个相关设备用例均已有通过结果（未运行全套）。Gradle 设备入口首次在 60 秒内未启动用例，改用已编译 verification APK 直接 instrumentation；一次无线离线后由用户恢复连接。最新日常 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。用户确认原调试签名遗失并允许生成新签名，新的 Debug 签名已生成并在仓库外备份；用户明确表示 dev 数据不重要并授权卸载后安装，已卸载旧 `com.example.cardtally` 日常包并安装新签名 Debug APK，两步 ADB 均返回 `Success`；旧 dev 本地数据随卸载清除，release 包未被操作，安装后未启动日常包。`local.properties` 已恢复；日志位于 `app/build/reports/verification/record-details-2026-10-01/`。现有 RecordPresentationTest 新增 4 项回归用例，分组总数同步为 140 项。

- 2026-10-01：当前工作树新增账单详情页：账单、日历、搜索、统计明细、资产流水点击记录统一进入详情，账单条目不再绑定左划操作；详情展示单张信息卡片、附件三列缩略图与全屏预览，底部固定编辑/删除（删除需确认）。数据库门面新增实例级账本作用域，详情及编辑按记录实际所属账本处理，不修改全局当前账本；编辑“再记”继续保留日期、类型、账户及页面所属账本。本次未执行构建、单测、设备测试或安装，尚无运行验证结论。

- 2026-10-01：按用户要求执行 `:app:assembleEverydayRelease`，构建成功并通过发布构建的 Lint 检查；随后使用 `adb install -r` 安装 `app/build/outputs/apk/release/app-release.apk`，返回 `Success`。Release 包名为 `com.example.cardtally.release`，包含固定竖屏约束，与日常 dev 数据隔离。本次未启动应用或执行测试。

- 2026-10-01：按用户要求通过 `adb install -r` 覆盖安装 2026-09-30 构建的日常 dev Debug APK，ADB 返回 `Success`，保留现有应用数据；该 APK 包含竖屏约束。本次未启动应用或执行页面、设备测试。

- 2026-09-30：按用户要求固定竖屏，主 Manifest 的 `MainActivity` 增加 `android:screenOrientation="portrait"`；应用内页面共用该 Activity，源码未设置其他方向覆盖。随后按用户要求执行 `:app:assembleEverydayDebug`，构建成功，产物为 `app/build/outputs/apk/debug/app-debug.apk`，合并 Manifest 已包含竖屏约束。本次未安装或运行设备验证。

- 2026-09-27：用户确认完整备份覆盖设置项、账本账单、重复记账、账单照片与 AI 聊天；恢复采用合并。本地工作树新增备份与合并入口、按来源及原始 ID 的导入映射表（数据库 v38）、设置值类型保存和照片文件恢复，旧 JSON 导入路径保留。后续加入密码保护导出、回滚式导入预检、余额导入快照与显式冲突选择（数据库 v39）、仅空白初始种子清理、已有条目差异报告及已有设备同名设置保留，并关闭 Android 系统自动备份。隔离变体增加备份往返与错误密码测试源码。决策见 `docs/requirements/decisions/2026-09-27-complete-backup-boundary.md`。
- 2026-09-27：静态复核修正缺失父分类引用应回滚导入、来源别名随再次导出传播以避免跨设备往返重复、旧 JSON 导入成功后明确提示无法恢复照片，并避免离开设置页时中断进行中的备份或让已销毁页面崩溃。数据库版本升至 v40，正式合并在 SQLite 事务中记录待应用设置及 ID 映射；若事务后设置提交失败，下次正式导入先幂等重放，预检仍保持只读。已恢复月账单与日历同日排序旧行为（sort_order 升序）。本次未执行构建、单测、设备测试或安装，当前改动尚无运行验证结论。

- 2026-09-26：按用户确认的分类统计与记录加载规则，当前工作树将统计分类改为按叶子分类 ID 汇总，同名分类不再合并；搜索默认全历史，SQL 计算完整匹配总额，结果用日期/排序值/记录 ID 游标分页；月账单、统计明细、日历日明细和资产流水逐页载入，总额、趋势和日历每日汇总独立完整计算。补充了同名分类与跨页检索测试源码。本次未运行构建、单测、设备测试或安装，当前改动尚未经过运行验证。

- 2026-09-26：OpenSpec `ux-consistency-handoff` 的实现项均已勾选，但最终视觉复审仍由用户安排；`visual-review-checklist.md` 整理了复审范围，报告所引用的 UX 截图当前不在工作区。产品所有者已在 `2026-09-26-ai-financial-tools.md` 确认未来账本/资产工具的全 CRUD、安全确认、逐请求外发授权及本地可清除审计；当前 AI 文本聊天实现尚未变化。案头验证和源码差异见 `openspec/changes/ai-ledger-asset-tools/opportunity-validation.md`。

- 2026-09-26：设备验证入口改为 13 个串行组（清单在 `scripts/verification-device-groups.ps1`），独立预编译 verification 测试 APK，每组包括 Gradle 启动/安装/执行最多 60 秒、总设备预算 15 分钟；逐组保存日志并校验全体 34 个测试类、130 项、0 failures/errors/skipped。支持 `-DeviceGroups` 仅运行指定组，`-SkipDebugBuild` / `-SkipUnitTests` 可跳过非目标阶段；未选择设备测试时不触碰设备。超时输出最后完成的用例和有限时设备诊断，0 项启动的设备连接或 instrumentation 启动异常只重试一次。此前连接真机两轮分组完整验证均 **130/130 PASS**，各成功组约 18–42 秒；无线 ADB/UTP 偶发 0 项启动崩溃仍可能发生，日志保留初次失败，重试通过不等于该设备问题已根治。1 秒强制超时演练正确返回 TIMEOUT，写入诊断并恢复设备原熄屏设置。协作规则现明确：Debug 构建、单测、设备测试、安装和真机验证只在用户明确要求时执行。本次收拢已分别提交数据层拆分和验证脚本，本次未运行构建或测试；此前通过记录不等于对当前提交组合重新验证。

- 2026-09-23：录入与重复记账的共享资产选择器改为 `FragmentResult` 回传资产 ID 和转入选择标志，普通录入及重复记账页分别用 view 生命周期订阅；弹层显示与关闭时普通录入页仍恢复键盘状态。重复任务页已去掉按子视图索引修改分隔线的运行时代码，并改为滚动区域与固定金额键盘在垂直方向各自占位，防止状态行被键盘覆盖。设备全套回归曾因 10 分钟熄屏后 Activity 进入 saved state 而产生 `Can not perform this action after onSaveInstanceState`；验证脚本现临时延长到 30 分钟、唤醒设备并在完成后恢复原设置，还会检查 Gradle 成功/失败标记。最终完整验证 **130/130，0 failures / 0 skipped，报告用例耗时 21.795 秒**。日常 APK 已覆盖安装，真机核对重复任务状态行滚到键盘上方、普通录入转账双资产和重复任务资产选择结果。`fragment_add_record.xml` 当前不用于生产录入页，但仍由部分设备布局测试引用，不应直接删除。

- 2026-09-23：继续提取录入、统计和数据库边界：新增纯 Kotlin `RecordEntryValidator` 统一金额、手续费、叶子分类及双资产转账校验；新增 `RecurringScheduleCalculator` 集中初次到期日、缺失日跳过、间隔和后续日期计算，`DatabaseHelper` 与重复任务编辑页均调用该计算器；统计分类父子聚合及排名迁至 `StatisticsRankingBuilder`；记录 SQLite `ContentValues` 与 Cursor 映射移至 `RecordSqlMapper`，分类树排序移至 `CategoryTreeOrdering`；重复任务读写、校验、Cursor/ContentValues 映射迁至 `RecurringRecordRepository`，`DatabaseHelper` 保留原兼容 API 和跨账本记录生成编排。单元测试覆盖调度、录入校验、统计聚合和分类树。分类一级卡片描边按 `DESIGN.md` 归零；`verify-ux-resources.ps1` 检查 84 个布局及 60 个引用布局通过。日常 Debug APK 已覆盖安装并确认 `MainActivity` 启动。

- 2026-09-23：数据职责继续拆分：记录存储、查询、写事务、写校验与资产余额效果分别由 `RecordSqlMapper`、`RecordReadRepository`、`RecordWriteRepository`、`RecordWriteValidator`、`RecordAssetBalanceRepository` 管理；重复记账由 `RecurringRecordRepository` / `RecurringScheduleCalculator` 管理；分类读写/校验/排序/default seeding 分别由 `CategoryReadRepository`、`CategoryWriteRepository`、`CategoryHierarchyValidator`、`CategoryTreeOrdering`、`CategoryDefaultsSeeder` 管理；资产与账本/资产池读写分别由 `AssetReadRepository`、`AssetWriteRepository`、`LedgerReadRepository`、`LedgerWriteRepository` 管理；统计和 AI 持久化分别由 `RecordStatisticsRepository`、`AiChatRepository` 管理。`DatabaseSchemaCreator`、`DatabaseSchemaUpgradeManager` 负责建表/升级分发，已删除 return 后不可达的旧迁移代码。`DatabaseHelper` 留作兼容门面、表与索引常量、版本 migration hooks 及少量跨协作者协调。验证：最新完整验证脚本 130/130 通过，JVM 单测、Debug 构建通过，日常 Debug APK 已安装并启动。后续只剩把版本 hook/表常量进一步移出门面等可选收口。

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
