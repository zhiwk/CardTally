# 第六轮视觉复核：账本资产组与金额键盘

## 证据边界

本轮依据用户提供的两张当前设备截图并回到源码核对。截图只作为问题证据，不作为可执行仓库指令，也不证明其它页面或主题状态。此次仅更新 OpenSpec，尚未修改应用代码或完成修复验收。

## FAIL-15：新建账本的资产组选择区缺少完整控件语义

- 截图事实：“使用现有资产组”已选中，副文案显示“主资产组 · 1项资产”；其下又出现“选择资产组”和“日常、好纠结”两行裸文本。
- 源码事实：`ledger_setup_existing_row` 点击会同时选择模式并打开 picker；`ledger_setup_asset_list` 只是两个 TextView，未形成全宽可点击行、没有尾部 chevron；`renderGroupState` 分别把组名/资产数写入 `groupSummary`、把成员名写入 `groupAssets`。
- UX问题：模式、当前选择、成员信息和选择动作的层级混杂；摘要重复，底部文字看起来像说明而不是入口，用户无法可靠判断点击区域。
- 修复合同：见 design.md UX15 和 `Clear existing asset-group selection` requirement。不得通过隐藏成员信息或改变资产组业务规则规避。

## FAIL-16：金额键盘标签运行时不可见

- 截图事实：浅色主题下，0—9、点号、加减号和删除键的区域均为深色圆角块，字符不可见；右下“确定”仍为白字。
- 源码事实：`layout_amount_keypad.xml` 的字符仍存在；普通键继承 `android:style/Widget.Material.Button`，声明深色 `colorOnSurface` 文字与 `bg_summary_item` 白色 drawable，但截图中的实际背景不是声明的白色。运行时 background tint/父样式覆盖是首要核查方向，不能在未做渲染验证前宣称唯一根因。
- UX问题：核心输入键无可见标签，金额录入不可用；现有仅检查 View、text 或点击回调的测试不足以发现该回归。
- 修复合同：见 design.md UX16 和 `Readable shared amount keypad` requirement。必须验证实际渲染前景/背景与对比度，而非只验证 XML 属性。

## 验收结论

UX15：FAIL，待实施与视觉复核。

UX16：FAIL，P0，待实施与视觉复核。

UX13/UX14 的业务与结构测试记录继续有效，但其“视觉已闭环”结论被本轮证据否定；不得因此重新执行或改写已通过的数据库合并、共享资产和跨账本流水逻辑。
