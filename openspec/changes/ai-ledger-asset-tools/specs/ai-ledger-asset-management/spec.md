## ADDED Requirements

### Requirement: Query every ledger by stable identity

AI 在财务工具已授权时 MUST 能分页列出全部账本，并按不可变 `ledger_id` 获取单个账本。结果 MUST 区分主账本、资产组所有者/成员、当前选中状态及允许的操作；同名账本不得合并或静默任选。

#### Scenario: Multiple ledgers have the same name

- **WHEN** 用户以名称指代账本且查询返回多个匹配
- **THEN** 助手展示足以区分的候选项并询问用户，获得明确 `ledger_id` 前不得创建写入预览

### Requirement: Create ledger with explicit asset-group intent

AI MUST 能创建账本，但参数必须明确账本名称、图标以及创建独立资产组或复用哪个现有资产组。名称与图标 MUST 经过与原生页面相同的校验；资产组意图缺失或有歧义时 MUST 先询问用户。

#### Scenario: Create a ledger in an existing group

- **WHEN** 用户明确选择现有资产组并确认预览
- **THEN** 账本和组引用在一个事务中创建，资产使用原有稳定 ID 和余额，不复制资产；失败时不遗留账本或引用

### Requirement: Update ledger without changing unrelated relationships

AI MUST 能按 `ledger_id` 修改账本当前允许编辑的字段。未在规范化参数中出现的字段、资产组关系、账单归属和当前账本选择 MUST 保持不变。

#### Scenario: Rename a shared ledger

- **WHEN** 用户确认仅修改某账本名称
- **THEN** 只更新该账本名称及必要更新时间，资产组、资产 ID、余额、记录和其他账本保持不变

### Requirement: Delete ledger with protection and exact impact

AI MUST 能请求删除允许删除的账本，但 MUST 沿用主账本、资产组根、当前账本回退及关联数据的现有规则。预览 MUST 从数据库实时计算受影响记录、资产引用和账本，不得由模型估算；删除使用聊天操作卡片的高风险再次确认和单个事务。

#### Scenario: Attempt to delete a protected ledger

- **WHEN** 目标为主账本、受保护资产组根或当前规则禁止删除的账本
- **THEN** 服务返回明确拒绝原因且不生成可执行删除操作，模型不得通过改名、切换当前账本或复制数据绕过保护

#### Scenario: Delete an eligible ledger

- **WHEN** 用户查看聊天操作卡片中的精确级联影响并完成确认与再次确认
- **THEN** 删除按现有数据库规则原子完成，当前账本偏好仅在成功后安全回退，失败时全部回滚

### Requirement: Query every asset by stable identity

AI 在财务工具已授权时 MUST 能按全部、账本或资产组范围分页列出资产，并按不可变 `asset_id` 获取单个资产，包括归档状态、所有者资产组、共享引用摘要和当前可执行操作。同名资产不得混淆。

#### Scenario: Same asset name exists in different groups

- **WHEN** 两个资产组存在同名资产
- **THEN** 查询返回不同 `asset_id`、所属组和引用账本摘要；写入预览必须锁定用户选择的具体 ID

### Requirement: Create asset in an explicit owner group

AI MUST 能在明确的资产组所有者下创建资产，并复用原生资产校验、类型、分类标签、图标、计入总额和金额精度规则。不得因当前页面账本不同而在错误资产组创建。

#### Scenario: Create a shared asset

- **WHEN** 用户选择被多个账本引用的资产组并确认创建预览
- **THEN** 仅创建一个稳定资产 ID，所有引用账本按现有共享规则可见同一资产，不创建同名副本

### Requirement: Update asset and expose shared effects

AI MUST 能按 `asset_id` 修改当前允许编辑的资产字段。共享资产的预览 MUST 显示所有受影响引用账本；服务通过资产组所有者根执行并沿用权限规则，不改变资产 ID、组关系或账单归属。

#### Scenario: Update a shared asset name

- **WHEN** 用户确认修改共享资产名称
- **THEN** 同一资产 ID 在所有引用账本中显示新名称，余额、记录、资产组关系和非目标字段保持不变

### Requirement: Treat direct balance update as high-risk calibration

AI 对资产金额的直接修改 MUST 被标记为“余额校准”，预览显示精确的旧值、新值、差额、币种/精度和共享影响，并要求二次确认。操作 MUST 不自动创建、修改或删除账单记录。

#### Scenario: Calibrate an asset balance

- **WHEN** 用户明确给出目标余额并在聊天操作卡片中完成确认与再次确认
- **THEN** 事务只更新该资产余额及审计信息；若目标值、资产状态或快照已变化则拒绝执行

### Requirement: Archive, restore and permanently delete assets

AI MUST 区分可逆归档、恢复和永久删除。归档/恢复遵守现有资产组权限；永久删除仅允许已归档且无任何记录引用的资产，并使用聊天操作卡片的高风险再次确认。模型不得把归档描述为永久删除，也不得承诺永久删除可恢复。

#### Scenario: Delete an asset used by records

- **WHEN** 用户要求永久删除仍被任意账本记录引用的资产
- **THEN** 服务拒绝永久删除并可建议归档，不改变记录、余额或资产引用

#### Scenario: Permanently delete an unused archived asset

- **WHEN** 资产已归档、无记录引用且用户在聊天操作卡片中完成精确影响预览、确认与再次确认
- **THEN** 资产在单个事务中永久删除并记录审计；重复提交不产生额外效果

### Requirement: Preserve excluded financial domains

本 capability 的账本和资产 CRUD MUST NOT 隐式创建、修改或删除账单记录、分类、转账、预算、资产组关系或合并关系，只有当前账本删除规则明确要求的级联行为除外，且该行为必须出现在删除预览中。

#### Scenario: User asks to edit a transaction through asset tools

- **WHEN** 用户要求借助本变更修改账单、转账或分类
- **THEN** 助手说明该能力不在当前范围，不得将请求伪装成资产余额修改或账本更新
