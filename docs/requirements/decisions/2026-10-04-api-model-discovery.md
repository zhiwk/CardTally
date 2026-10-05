# API 配置与模型发现

状态：产品所有者已确认；代码已修改，尚未构建或运行验证。

决策日期：2026-10-04

依据：用户要求 API 配置页取消预设 URL 和模型，先填写 URL，再填写 API Key，通过按钮获取模型，然后使用下拉列表选择。

- 新配置的 URL 与模型留空，不默认选择 Provider 或模型。本决策替代 AI 工具计划中“新用户默认 DeepSeek”的配置预设方向。
- 页面顺序为服务 URL、API Key、测试并获取模型、模型下拉列表、保存。新配置必须从服务返回的列表选择模型，未选中前不能保存。
- 保留已有有效 URL、Key 和模型。旧版只存 Key 时依赖的 MiniMax 默认值继续生效；只读兼容不写回或覆盖旧配置。已有保存的模型可展示并保留，用户可主动重新获取列表。
- 模型发现面向 OpenAI 兼容接口：使用 Bearer Key 获取 JSON `data[].id`。基础 URL 自动解析同服务的 models / chat/completions 路径；根域名使用 /v1，带路径的基础 URL 保留该路径。标准完整 chat/completions 与旧 MiniMax text/chatcompletion_v2 地址也可获取同一 API 前缀的 models。服务器需提供对应接口，不增加其他协议或硬编码模型列表。
- 此按钮只验证模型列表接口，不逐个发送对话测试，不读取或外发账本、资产及聊天内容。获取成功不保证列表中每个模型适合文本聊天或拥有调用权限。
- 用户修改 URL 或 Key 后，当前模型列表与选择失效，旧请求取消；返回页面不会自动发起联网。失败显示明确原因并可重试，未保存的输入不修改已有配置。
- API Key 只写入本机配置，凭证不写日志或保存到视图状态；查询凭证只发给用户指定服务，不跟随重定向。已有完整备份边界继续适用，新增配置标志参与偏好备份。
- 不新增 Provider 选择、AI 财务工具、数据库迁移或自动对话测试。默认仅修改及静态复核；构建、测试、安装须另行明确要求。

接口参考：[OpenAI 模型列表](https://developers.openai.com/api/reference/resources/models/methods/list)、[MiniMax OpenAI 兼容模型列表](https://platform.minimax.cn/docs/api-reference/models/openai/list-models)。
