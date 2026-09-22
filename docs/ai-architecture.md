# AI 模块结构

`org.smart.erp.ai` 按能力划分，再在能力内部按职责分层。现有 HTTP 路径、请求和响应格式不随 Java 包名变化。

```text
ai/
├── assistant/      对话编排：controller → service → ChatClient；config 放系统提示词
├── conversation/   对话与消息：controller → service → mapper/entity；dto 是接口返回值
├── tool/           只读业务查询工具；result 是工具专用返回结构
├── action/         审批提案草稿：model / service / tool（尚未对模型开放）
└── rag/            知识库预留：config / entity / mapper（尚无检索链路）
```

## 依赖方向

- `controller` 只处理 HTTP 协议，调用同一能力的 `service`；业务校验留在服务层。
- `assistant.service` 编排模型、对话归属校验、消息记录和标题生成。它通过 `AiQueryTools` 获取可暴露给模型的工具列表，不直接引用各业务模块的 Mapper。
- `conversation.service` 使用本能力的 Mapper 管理对话与消息，并通过 `CurrentUser` 校验归属；其他能力只能经 Service 访问对话数据。
- `conversation` 的两张表存前端历史；Spring AI JDBC `ChatMemory` 存模型上下文。前者需执行 `sql/migrations/V20260922__ai_conversation_history.sql`，两套存储不要混用。
- `tool` 调用各业务模块的 Service，使用请求携带的安全上下文执行，保留原有 RBAC 校验。`tool.result` 只服务模型工具返回，不作为 HTTP DTO。
- `action` 当前仅有审批提案模型与准备逻辑。没有确认、执行和持久化闭环，所以 `PurchaseWriteTool` 不注册为 Spring Bean，也不加入 `AiQueryTools`。上线写操作前需要补用户确认、一次性令牌、并发版本校验、权限复核和审计。
- `rag` 当前只有表映射和配置项，尚未接入向量化、检索和文档管理接口。不要把它描述为已上线的知识库能力。

## 新能力放置规则

1. 新增只读查询：在 `tool` 中实现，返回类型放 `tool.result`，然后显式加入 `AiQueryTools`，并补工具授权测试。
2. 新增 HTTP 对话接口：入参/出参放对应能力的 `dto`，Controller 只调用 Service。
3. 新增写操作：先补完整确认与执行流程，再把专用工具加入模型白名单；不要直接把业务 Service 的写方法暴露给模型。
4. 新增知识库：把切分、嵌入、检索分别放在 `rag` 的服务层，复用 `rag.mapper`，并在启用前补对应建表脚本。

## 配置

主模型密钥由 `AI_API_KEY` 提供。嵌入模型默认关闭；接入 RAG 服务时将 `AI_EMBEDDING_MODEL` 设为 `openai`，并通过 `AI_EMBEDDING_API_KEY` 提供嵌入模型密钥。
