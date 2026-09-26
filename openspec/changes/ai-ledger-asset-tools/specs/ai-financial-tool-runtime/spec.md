## ADDED Requirements

### Requirement: Provider-neutral chat with DeepSeek default

AI 聊天层 MUST 通过统一 Provider 接口支持多种大模型，且新安装、无有效历史配置或用户主动恢复默认时 MUST 选择 DeepSeek 作为默认 Provider。Provider 专属字段 MUST 由 adapter 转换，不得渗入账本/资产领域服务；已有有效 Provider 配置 MUST 保留，升级不得静默切换其端点或模型。

#### Scenario: New user opens AI configuration

- **WHEN** 用户没有有效的历史 AI Provider 配置
- **THEN** 配置页预选 DeepSeek 和 Provider catalog 中当前支持结构化工具的默认模型，用户仍可改选其他受支持 Provider 或兼容端点

#### Scenario: Existing MiniMax user upgrades

- **WHEN** 本机已有完整且有效的 MiniMax 配置
- **THEN** 应用将其保留为 MiniMax Provider 配置并继续使用，不覆盖 API Key、模型或 URL；只有用户主动选择时才切换到 DeepSeek

### Requirement: Capability-gated model selection

每个 Provider/模型组合 MUST 声明并验证聊天、流式、结构化工具和协议续接能力。只支持聊天或未通过工具契约验证的模型 MAY 用于普通聊天，但 MUST 禁用财务工具并说明原因。OpenAI-compatible URL 本身 MUST NOT 被视为工具能力证明。

#### Scenario: User selects a chat-only model

- **WHEN** 所选模型能返回文本但不支持可靠的结构化工具调用
- **THEN** 普通聊天继续可用，财务工具状态显示不可用，任何自然语言形式的工具请求都不得写库

#### Scenario: User switches provider with pending write

- **WHEN** AI 聊天中存在待确认写操作而用户切换 Provider、模型或协议
- **THEN** 待确认操作立即过期，旧卡片按钮禁用；历史可见消息保留，旧 Provider 私有续接字段不得发送给新 Provider

### Requirement: Opt-in financial tool access

AI 财务工具 MUST 默认关闭，并与普通聊天能力分开授权。启用界面 MUST 告知用户必要财务字段可能被发送给当前 Provider。每一轮会外发财务字段的请求 MUST 在读取、序列化或发送之前显示本次字段类别及目标 Provider，并取得用户逐请求授权；只开启总开关不构成该轮授权。关闭后 MUST 立即阻止新的财务工具调用，但不得破坏普通聊天和本地财务数据。

#### Scenario: User chats without enabling finance access

- **WHEN** 用户未开启财务工具而提出读取或修改账本、资产的请求
- **THEN** 助手可以解释如何启用，但 MUST 不读取本地财务实体、不把财务数据发送给模型且不创建待执行操作

#### Scenario: User has enabled finance tools but declines this request

- **WHEN** 用户已开启财务工具，但拒绝某一轮将发送财务字段的逐请求授权
- **THEN** 本轮不得读取/序列化额外财务字段、调用工具或创建待执行操作；普通文本聊天仍可继续且不得附带财务数据

### Requirement: Structured allowlisted tool protocol

客户端 MUST 只接受注册表中名称、版本和 JSON schema 均匹配的工具调用；模型 MUST 不获得 SQL、文件、网络或任意代码执行入口。解析普通聊天文本 MUST NOT 产生数据库写入。

#### Scenario: Model returns an unknown or malformed call

- **WHEN** 模型返回未知工具、额外禁止字段、错误类型、非法枚举、越界数值或无法解析的 JSON
- **THEN** 工具网关拒绝调用并返回结构化错误，不读取超出请求范围的数据且数据库保持不变

#### Scenario: Provider has no tool-call capability

- **WHEN** 所选模型或接口不支持结构化工具调用，或响应只包含看似工具指令的自然语言
- **THEN** 应用退回纯聊天并明确说明未执行操作，MUST NOT 猜测工具名或参数

### Requirement: In-chat native confirmation for every write

每个写工具 MUST 先生成只读预览和一次性待执行操作，再由客户端在 AI 聊天消息流中渲染原生操作卡片。卡片 MUST 至少提供“确认执行”和“取消”按钮；只有用户点击当前有效卡片的按钮才可执行。自然语言回复、模型输出的 Markdown/HTML/文本按钮、重复发送、通知点击或键盘提交 MUST NOT 视为执行授权。

#### Scenario: Confirm an ordinary update

- **WHEN** 用户查看聊天卡片中包含目标稳定 ID、当前值、变更后值和影响范围的普通写入预览并点击“确认执行”
- **THEN** 卡片按钮立即禁用并显示执行中；应用重新校验目标与规则后最多执行一次，随后将卡片更新为可核对的成功或失败终态

#### Scenario: Confirm a destructive or balance operation

- **WHEN** 操作为资产余额校准、账本级联删除或资产永久删除
- **THEN** 聊天卡片先展示不可逆性和精确影响，再切换为“再次确认”按钮；任一步取消都不得写库

#### Scenario: Model emits a fake confirmation button

- **WHEN** 模型回复包含“确认执行”字样、Markdown 链接、HTML 按钮或伪造的操作 ID
- **THEN** 聊天适配器将其作为普通文本渲染，不绑定点击处理；只有客户端工具网关创建的有效 `PendingOperation` 可以生成可点击操作卡片

### Requirement: Atomic, fresh and idempotent execution

写操作 MUST 在 SQLite 事务中比较预览时的版本或快照指纹，执行业务变更并写入审计。操作 ID MUST 一次性使用；网络重试、流式重连、旋转重建或重复点击 MUST NOT 导致重复写入。

#### Scenario: Data changes after preview

- **WHEN** 待确认目标或相关引用在预览后被其他页面修改、删除或改变权限
- **THEN** 执行被拒绝，旧待执行操作失效并提示重新生成预览，不得覆盖较新的数据

#### Scenario: Same operation is submitted twice

- **WHEN** 已完成或已拒绝的操作 ID 再次提交
- **THEN** 返回原有终态或幂等冲突，数据库和审计不得新增第二次业务效果

### Requirement: Minimal financial context disclosure

应用 MUST 仅在满足当前用户请求所需时读取并发送财务字段，不得在每轮聊天预加载全部账本和资产。列表工具 MUST 分页并有硬性条数上限；数据库名称等非可信文本 MUST 始终作为数据处理，不能修改系统指令、权限或确认要求。

#### Scenario: User asks about one named asset

- **WHEN** 用户询问某一资产且存在唯一匹配
- **THEN** 工具只返回识别与回答所需字段，不附带无关账本、其他资产、API Key、请求头或本地设备信息

### Requirement: Durable local audit without secrets

每个写工具的建议、确认、拒绝、取消、过期、成功和失败终态 MUST 在本地持久化，并关联操作 ID、会话、工具调用、规范化参数、影响摘要、快照指纹及时间。API Key、Authorization 头和模型隐藏推理 MUST NOT 写入审计或可见聊天。用户 MUST 能在本地查看操作结果并明确清除 AI 财务审计；清除审计 MUST NOT 改变任何业务数据。

#### Scenario: App restarts after a confirmed write

- **WHEN** 写入成功后应用进程重启并重新打开相关会话
- **THEN** 用户仍能看到该操作的结果卡片，系统能证明相同操作 ID 已执行且不会再次执行

#### Scenario: User clears AI financial audit

- **WHEN** 用户在应用内明确清除 AI 财务审计
- **THEN** 仅删除审计数据；账本、资产、记录、会话和聊天消息不受影响，应用不得把审计上传到 Provider

### Requirement: Bounded tool loop and cancellation

单个用户回合 MUST 采用有限工具循环并最多保留一个待确认写操作。用户停止生成、切换会话、关闭财务工具或待确认操作过期时，未执行操作 MUST 失效。

#### Scenario: Tool loop exceeds its limit

- **WHEN** 模型连续调用超过客户端配置上限仍未给出最终答复
- **THEN** 客户端停止循环、保留已经完成的只读结果、取消未确认写入并告知用户没有继续执行

### Requirement: Protocol-compatible conversation persistence

应用 MUST 以统一格式保存继续结构化工具对话所必需的可见消息、工具调用、调用 ID 和最小工具结果，并维持调用 ID 配对。Provider 为协议续接要求的私有字段 MAY 隔离保存在该 Provider 的扩展载荷中，但 MUST 不渲染、不写入财务审计且不得传给其他 Provider；旧聊天数据迁移 MUST 无损。

#### Scenario: Continue chatting after a tool result

- **WHEN** 用户在一次查询或写入完成后继续追问
- **THEN** 发给模型的历史保持 assistant 工具调用与对应 tool 结果的合法顺序，不丢失原有用户和助手可见消息

#### Scenario: DeepSeek tool conversation requires continuation state

- **WHEN** DeepSeek 所选模式要求在后续工具请求中回传 Provider 专属续接字段
- **THEN** DeepSeek adapter 仅向同一 Provider/模型回传所需字段，不在 UI 或审计中展示；无法完整安全回传时停止工具链并退回纯聊天
