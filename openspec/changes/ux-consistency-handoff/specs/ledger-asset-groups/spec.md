## ADDED Requirements

### Requirement: Shared asset group identity

账本 MUST 使用一组资产；多个账本引用同组时 MUST 共享相同资产ID及余额，而非副本。记录 MUST 保留唯一所属账本。

#### Scenario: Create independent or shared ledger

- **WHEN** 用户新建账本
- **THEN** 可直接选择新建空的独立资产组或去重后的现有整组；默认明确显示主组，创建失败不遗留账本或组

### Requirement: Atomic same group ledger merge

管理页 MUST 支持同组两个账本合并，显式选择保留目标并二次确认；数据库 MUST 拒绝跨组、自合并及主账本作为来源，整个操作 MUST 原子完成。

#### Scenario: Merge shared ledgers

- **WHEN** 用户确认将同组源账本合并到目标
- **THEN** 源记录归入目标、源账本移除，资产ID及余额、记录ID和其他内容不变，全局分类不复制；仅成功后更新当前账本偏好

#### Scenario: Merge the non master root with another ledger

- **WHEN** 非主资产组根账本作为源且仍有第三账本使用该组
- **THEN** 所有幸存引用保持有效且无环，第三账本仍访问相同资产ID与余额；任何失败全部回滚

#### Scenario: Cancel or repeat merge

- **WHEN** 用户取消、提交跨组操作、重复提交已完成合并或发生事务失败
- **THEN** 不产生额外记录或余额变更，不出现部分合并，也不提前切换当前账本

### Requirement: All ledger asset history

资产详情 MUST 按不可变资产ID展示所有账本的相关收支与转入转出，记录去重；MUST 不受当前账本或资产同名影响，历史范围不得静默截断。

#### Scenario: Shared asset with same named asset elsewhere

- **WHEN** 多账本使用同一资产且另一组有同名资产
- **THEN** 详情包含该ID的跨账本流水、每次转账只出现一次，不混入另一ID；日期统计与展示集合一致，转账不计收入支出

### Requirement: Ledger provenance in asset rows

资产详情每条记录 MUST 在右侧金额下显示该记录所属账本名称，使用账单圆角分组和图标体系；此展示模式 MUST 不覆盖其他页面的资产标签。

#### Scenario: Rename merge or edit from asset history

- **WHEN** 账本改名、合并完成或用户从跨账本流水打开编辑
- **THEN** 标签按当前记录归属刷新，编辑不隐式改变所属账本，金额完整可见，资产管理权限保持原规则
