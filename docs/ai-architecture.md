# AI 目录结构

按 Java 职责平铺，接口和实现只在 `service/impl` 分开。包含对话、历史、查询工具和需要用户确认的采购审批提案；RAG 仍为未接入主链路的草稿。

```text
ai/
├── controller/       对话、消息、聊天与动作确认接口
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

`controller → service → persistence / tool` 是聊天主链路。普通聊天使用 `AiQueryTools` 明确登记的查询工具；流式聊天还登记 `AiActionTool` 的采购审批准备工具，先返回 ACTION 待确认事件，再由用户通过动作确认接口触发领域服务。模型准备提案时不会直接执行审批。`rag` 目前只有模型、Mapper 和配置，没有上传、切分、检索服务。

对外契约维持原样：HTTP 路径不变，SSE 使用 `type`、`content`，请求字段为 `conversationId`、`message`；各工具名称和参数名保持原有定义。对话归属在 Service 层验证，查询工具通过 `ToolExecutionSupport` 恢复登录用户上下文，沿用业务 Service 的 RBAC 校验。

Spring AI JDBC `ChatMemory` 保存模型上下文；`ai_conversation` 和 `ai_message` 保存前端可见历史。后两张表的建表脚本为 `sql/smart-erp/ai_conversation.sql`、`sql/smart-erp/ai_message.sql`，首次部署入口见 `sql/bootstrap/README.md`。


## 流式与会话边界

- 前端逐行解析 SSE，支持 LF、CRLF、CR、多行 data 与跨字节 UTF-8 分片；ERROR 只回调一次，ERROR 或 `[DONE]` 提前结束时取消响应读取。
- 每个流绑定自己的 AbortController。切换会话、新建会话、停止生成或离开页面时中断旧流；旧回调与旧 finally 不得修改新请求或弹出旧审批确认。
- 归档会话仍可读取历史，后端在写入用户消息前拒绝继续发送，普通和流式入口共用此校验。
- 待确认动作通过 Redis 实际序列化器保存和读取；record 必须保留类型信息，避免反序列化成 Map 后确认失败。动作仍校验用户、有效期和业务版本，领域服务执行实际审批。

回归入口：后端 `AiMessageServiceImplTest`、`AiPendingActionSerializationTest`、既有会话/工具/动作测试；前端 `ai-api.spec.ts`、`ai-conversation.spec.ts`、`ai-markdown.spec.ts`。回归使用模型桩和流分片模拟，不代替真实模型 API 联调。
