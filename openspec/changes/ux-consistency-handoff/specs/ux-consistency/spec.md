## ADDED Requirements

### Requirement: Consistent surfaces

当前可达页面的普通主体卡片 MUST 使用不透明白色、12dp圆角、零描边和零阴影，遵守 design.md 的明确例外及语义色约定。

#### Scenario: Light theme at rest

- **WHEN** 用户打开浅色主题页面且没有按压控件
- **THEN** 普通卡片为 #FFFFFF、alpha=1，页面底为 #EEEEEE；录入主卡保持20dp圆角，收支和转账语义底色保留

### Requirement: Recognizable and reachable actions

导航与独立操作 MUST 有正确图形、可访问名称和至少48dp点击区；用户指定的36dp紧凑日期单元格不受此点击区规则强制扩高。

#### Scenario: Settings navigation

- **WHEN** 用户浏览设置项并点击其整行
- **THEN** 右侧显示 chevron 而非竖条，进入原目标，装饰图标不重复朗读

#### Scenario: Secondary header actions

- **WHEN** 用户在资产流水页打开更多菜单
- **THEN** 原置顶、归档或恢复、删除等适用动作可达，危险操作保留确认，标题不覆盖操作

### Requirement: Readable adaptive content

信息文本 MUST 保持可读；关键金额 MUST 完整展示；实际出现溢出的布局 MUST 自适应高度或换行，不通过省略金额或修改精度解决。

#### Scenario: Large font and long values

- **WHEN** 使用320dp宽、2倍字体及999,999,999.99金额和长名称进行隔离测试
- **THEN** 金额和操作不被遮挡，辅助文字至少12sp，正文不用 outline 或额外半透明降低可读性

### Requirement: Stable icon selection

图标选择器 MUST 以稳定图标键保存当前选择，过滤仅影响可见列表，不能重置用户新选择。

#### Scenario: Filter after selecting another icon

- **WHEN** 初始选择A，用户改选B并返回表单，再打开选择器，搜索隐藏B后清空搜索
- **THEN** B仍选中，回调和保存对象为B，选中有非颜色标记和可访问语义

### Requirement: Usable category icon browsing

分类图标浏览 MUST 限制到可用空间、允许分组和图标分别滚动，所有图标选择入口 MUST 统一使用同一套分组的图标浏览，不再提供按字母序的全量图标弹窗，不改变持久化图标键。

#### Scenario: Browsing many groups

- **WHEN** 用户在小屏设备切换和滚动全部图标分组
- **THEN** 不因左侧完整分组撑高右侧空白区，仍能在分组间切换并保存选择

#### Scenario: Opening the picker from any entry point

- **WHEN** 用户从新增分类、编辑分类、记一笔子分类或账本图标打开图标选择
- **THEN** 显示同一套分组图标浏览，图标仅以图形呈现，每个图标仍有可访问名称与选中语义，已保存的旧图标可见且可重新选择

### Requirement: Safe keyboard to sheet transition

输入和弹层切换 MUST 保持草稿且不发生键盘遮挡确认操作，布局按实际可用空间处理。

#### Scenario: Open date from an active editor

- **WHEN** 系统备注键盘或自定义金额键盘打开时用户进入日期选择
- **THEN** 不残留键盘顶起弹层，确认和取消可达，日期36dp单元格保持居中，关闭后草稿不丢失

### Requirement: Accurate AI action states

AI 发送控件 MUST 与真实可用行为一致；不更改既有聊天协议或增加自动执行能力。

#### Scenario: Streaming response

- **WHEN** 当前会话正在流式回复且支持取消
- **THEN** 操作显示停止图形和“停止回复”描述，点击取消当前请求而不触发重复发送

#### Scenario: Missing configuration

- **WHEN** API 配置不完整
- **THEN** 输入及发送明确禁用，配置入口可达，不发请求

### Requirement: Localized guidance

可达页面的操作和状态文案 MUST 使用现有中英文资源；空态指引 MUST 与当前可用导航一致。

#### Scenario: No assets and hidden asset navigation

- **WHEN** 用户打开空资产选择列表但底部资产入口隐藏
- **THEN** 指引说明可从“我的→账户资产”开启入口后添加账户，不自动改偏好，不增加“无”选项

### Requirement: Evidence based implementation handoff

实施者 MUST 复核源码、对风险先复现、保留用户数据，并逐项记录实际测试结果；不得将未测试项或最终视觉验收标为通过。

#### Scenario: Implementation completion

- **WHEN** 实施者交付代码及功能检查结果
- **THEN** 提供UX编号对应文件和测试证据、未完成项、实际APK状态，保留变更待视觉复审，不自行归档

### Requirement: Ledger form visual consistency

新建和编辑账本 MUST 按 design.md 的 UX13 使用灰色页面、纯白12dp无边框无阴影分组卡。名称和图标 MUST 合为基本信息卡；图标行使用36dp底、18dp实际图标和矢量chevron。布局 MUST 使用最小高度与自适应高度，保留焦点、错误提示和键盘可达性。

#### Scenario: Create ledger with expanded asset options

- **WHEN** 新建账本中选择独立或现有资产组，或使用320dp宽、2倍字体及长名称
- **THEN** 资产说明、模式和组选择在同一白色分组内，不裁字或覆盖选择控件，创建按钮可达，选择符合ledger-asset-groups规范

#### Scenario: Edit ledger without changing assets

- **WHEN** 编辑已有账本并保存名称或图标
- **THEN** 整个资产关系卡及展开区域和空白间距不显示，原资产池关系不变

### Requirement: Commit icon selection once

共享图标弹层 MUST 在选中图标时回调一次并关闭，调用方保留该图标键与其余表单草稿。

#### Scenario: Select from search results

- **WHEN** 用户在搜索结果点击一个图标
- **THEN** 弹层关闭，表单显示所选图标，再次打开仍为该选择，不需额外关闭操作

### Requirement: Consistent request lifecycle on immediate failure

AI请求生命周期 MUST 同时支持同步失败和异步完成；完成状态不得被发送调用返回后的初始化覆盖。

#### Scenario: Synchronous sender failure

- **WHEN** 注入sender在发送返回前同步报告失败
- **THEN** 请求不再处于loading，控件与消息状态一致，允许后续发送；测试不得使用真实计费请求

### Requirement: Safe and meaningful device verification

验收 MUST 检查实际可见文字和统一坐标空间内的布局，而非仅检查text值或非零尺寸。设备流程 MUST 不卸载或清除用户日常应用；写入测试仅在隔离环境进行。

#### Scenario: Long amount layout evidence

- **WHEN** 测试320dp宽、2倍字体及长金额
- **THEN** 检查文本行完整、无省略或裁切、相关控件不重叠，使用真实布局约束并记录未覆盖组合

### Requirement: Clear existing asset-group selection

新建账本选择“使用现有资产组”时，页面 MUST 在资产关系卡内提供一个独立、全宽、至少64dp高的资产组选择行，清楚区分模式说明、当前资产组名称和成员/资产数量摘要。该行 MUST 具有尾部 chevron、明确点击及无障碍语义；模式副文案和选择行 MUST NOT 重复同一组摘要，成员名称 MUST NOT 作为脱离点击控件的裸文本出现。

#### Scenario: Select an existing asset group

- **WHEN** 用户选择“使用现有资产组”且当前为主资产组
- **THEN** 模式行只表达使用已有组的含义；下方选择行显示“选择资产组”、主资产组名称、成员与资产数摘要，点击整行打开组选择 BottomSheet，默认组无需打开弹层也可直接创建

#### Scenario: Toggle asset-group mode

- **WHEN** 用户从现有组切到独立组再切回现有组
- **THEN** 选择行及其分隔线在独立模式完整隐藏，切回后按稳定组 ID 恢复之前选择；每次整行点击只产生一次模式切换或弹层打开

### Requirement: Readable shared amount keypad

共用金额键盘 MUST 在浅色、深色和跟随系统主题中显示全部数字、点号、加减号、删除及“确定/=”内容。普通键与主键 MUST 使用不同且明确的主题前景/背景，实际渲染对比度均至少4.5:1；framework 继承 tint、按压或禁用状态 MUST NOT 造成同色文字与背景。每个按键 MUST 保持至少48dp点击区和可访问名称。

#### Scenario: Open amount keypad in light theme

- **WHEN** 用户在浅色主题的新增或编辑记录/资产页聚焦金额字段
- **THEN** 所有普通键显示浅色表面与深色字符，主“确定/=”键显示黑底白字，不出现只有黑色空白圆角块的键盘

#### Scenario: Use expression and confirm states

- **WHEN** 用户依次输入数字、点号、加减表达式、删除并完成求值或确认
- **THEN** 每个操作在所有状态下可见且行为不变，“确定”和“=”正确切换；默认0.00、收起、自定义键盘与页面保存逻辑不回归
