## Why

> 状态：产品范围已由 `docs/requirements/decisions/2026-09-26-ai-financial-tools.md` 确认，功能尚未实施。`opportunity-validation.md` 的竞品案头研究不等于外部用户需求验证。

CardTally 当前 AI 助手只能通过 MiniMax 专用客户端进行持久化文本聊天，不能读取或操作本地财务实体。产品目标已调整为支持多种大模型，默认 Provider 为 DeepSeek；用户希望 AI 在继续聊天的同时，能够按明确意图查询、新建、修改和删除任意账本及资产。该能力会改变现行“AI 不执行财务操作”的产品边界，且会把部分本地财务数据发送给用户所选模型 Provider，因此必须以显式授权、聊天页确认卡片、业务规则复用和本地审计为前提。

## What Changes

- 将现有 MiniMax 专用网络层重构为 Provider 可扩展的聊天接口；默认使用 DeepSeek，同时允许用户配置其他受支持 Provider 或 OpenAI-compatible 模型端点。
- 在现有流式聊天中接入 Provider 无关的结构化工具调用；模型只能选择受信任的账本/资产工具，不能生成或执行 SQL。
- 为全部账本及全部资产提供按稳定 ID 定位的查询、新建、修改和删除能力，不受当前页面选中账本限制。
- 所有读取均按用户请求最小化返回；所有写入先生成确定性预览，再由客户端在 AI 聊天消息流中渲染带“确认执行”和“取消”按钮的原生操作卡片。
- 余额直接修改、账本级联删除、资产永久删除等高风险操作在同一聊天卡片内进入再次确认状态，并显示不可逆影响。
- 复用 `DatabaseHelper` 现有业务规则、资产组归属、主账本保护、叶子分类约束和事务边界；共享资产的修改必须展示对所有引用账本的影响。
- 增加本地持久化的 AI 财务操作审计，并保存工具协议继续对话所需的最小消息字段。
- 当模型、接口或响应不支持结构化工具调用时，自动退回纯聊天；绝不从普通文本猜测工具及参数后写库。
- 本变更只定义 OpenSpec，不修改应用代码。

## Capabilities

### New Capabilities

- `ai-financial-tool-runtime`: AI 工具协议、授权、确认、并发保护、审计、隐私与失败降级。
- `ai-ledger-asset-management`: 账本和资产实体的可查询范围、CRUD 操作及业务约束。

### Modified Capabilities

无已有 OpenSpec capability。本变更在产品层面取代既有“AI 仅文本聊天、不得执行财务操作”的限制；正式实施前必须同步更新对应决策文档和长期协作说明。

## Impact

预计涉及 `AgentFragment`、`MiniMaxClient` 的 Provider 抽象与兼容迁移、AI 配置、聊天适配器、聊天消息与会话存储、`DatabaseHelper`、账本/资产领域模型、聊天页操作卡片以及隔离测试。数据库需要增加 Provider 配置、工具协议消息和本地审计的持久化结构，迁移 MUST 保留现有聊天、MiniMax 配置和财务数据。不新增云端数据库或服务，API Key 仍由用户在本机配置。

## Confirmed Product Decisions

1. AI 网络层支持多种大模型 Provider，DeepSeek 是新用户和无有效历史配置时的默认 Provider；已有有效 Provider 配置不得被静默覆盖。
2. 所有写操作均在 AI 聊天页生成客户端原生确认卡片，由用户点击按钮授权；聊天文本中的“好的”“确认”等自然语言不构成执行授权。
3. 支持账本和资产的完整 CRUD；不开放账单、分类、转账、预算等独立工具。
4. 允许直接余额校准但须二次确认；允许账本删除级联记录但须精确影响预览和二次确认。
5. 财务工具默认关闭，每次外发财务字段前逐请求授权；本地审计需可清除。

约束性细节与 2026-08-12 决策的精确适用范围见 `docs/requirements/decisions/2026-09-26-ai-financial-tools.md`。以上批准仅是产品范围，不代表代码已实现。

## Remaining Implementation Work

跨账本读写领域服务、稳定 ID 与历史名称引用兼容、级联预览/执行原子性、Provider 数据隔离、审计查看/清除界面和迁移策略仍待设计和实现。必须复用有约束力业务决策，不得将产品批准误作实现证据。
