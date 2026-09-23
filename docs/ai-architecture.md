# AI 目录结构

按 Java 职责平铺，接口和实现只在 `service/impl` 分开。保留现有对话、历史、只读工具，以及尚未完成的审批提案与 RAG 代码。

```text
ai/
├── controller/       现有三个 HTTP 接口
├── request/          请求参数
├── result/           HTTP、SSE、工具与审批提案返回值
├── service/          服务接口
│   └── impl/         服务实现与标题生成
├── persistence/      对话/消息实体、状态和 Mapper
├── config/           系统提示词、工具白名单、RAG 配置
├── tool/
│   ├── query/        五类只读 ERP 查询工具
│   └── action/       采购审批提案与待办草稿
└── rag/              文档与片段模型、Mapper 草稿
```

`controller → service → persistence / tool` 是聊天主链路。模型只能调用 `AiQueryTools` 明确登记的只读工具；`tool/action` 的准备工具尚未登记，因此不会让聊天执行审批。`rag` 目前只有模型、Mapper 和配置，没有上传、切分、检索服务。

对外契约维持原样：HTTP 路径不变，SSE 使用 `type`、`content`，请求字段为 `conversationId`、`message`；各工具名称和参数名保持原有定义。对话归属在 Service 层验证，查询工具通过 `ToolExecutionSupport` 恢复登录用户上下文，沿用业务 Service 的 RBAC 校验。

Spring AI JDBC `ChatMemory` 保存模型上下文；`ai_conversation` 和 `ai_message` 保存前端可见历史。后两张表的手动建表脚本在 `sql/migrations/V20260922__ai_conversation_history.sql`。
