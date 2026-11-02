# T4：缓存、通知、WebSocket 与 AI 的企业边界

本轮完成当前后端实现路径的隔离改造与针对性验证。多租户正式业务入口继续返回 503；尚未完成双端切换、完整业务流程及灰度运维验收，不代表已可上线。

## Redis 与锁

`RedisTemplate`、`StringRedisTemplate` 共用 `TenantRedisKeySerializer`，multi 的物理键为 `erp:t:{tenantId}:{原始key}`。键来自已验证的 TenantScope，缺少上下文直接失败；客户端不能通过传入其他企业前缀绕过，因为输入仍会被放入当前企业空间。

统一覆盖权限、菜单、看板、BOM、用户索引集合、业务序列、AI 动作和 Lua KEYS；hash field 与 value 不做前缀转换。Redisson 使用显式的 `TenantKeyspace.key()`，覆盖 BOM、采购入库、销售发货及看板锁。续期/释放使用已经确定的物理锁名，不依赖 Redisson 工作线程继承 ThreadLocal。

single 保留原来的 Redis 键及单号序列，避免升级后序列重置；single 也拒绝不匹配的显式租户上下文。旧单企业接入 multi 前仍需在维护窗口核对/迁移单号计数与业务库已有最大单号，不能仅凭新命名空间从 1 开始；该项列入上线门槛，当前业务门禁不会移除。

菜单缓存顶层统一写 ArrayList，避免 List.of()/Stream.toList() 缺少根类型信息导致读取失败；遇到旧格式损坏缓存只删除当前企业的对应键并回源，不清共享 Redis。

## 显式异步快照

`TenantExecutor.captureTenant()` 产生只含企业的 TenantWork，通知与提交后缓存回调使用它；`capture()` 产生企业、用户和权限的 TenantExecution，AI 使用它。

快照复制用户标识与权限，不保存可变请求 SecurityContext/LoginUser。每次执行先检查企业 ACTIVE/READY/版本，再打开 TenantScope；涉及用户时同时恢复新的 SecurityContext，finally 恢复原线程状态。同企业允许嵌套，跨企业作用域立即拒绝。工作必须在业务事务开始前恢复企业，不通过 InheritableThreadLocal 隐式传播。

## 通知与 WebSocket

- 发布时冻结 tenantId，并复制业务引用和收件人集合，原 DTO 后续修改不改变事件内容。
- AFTER_COMMIT 监听器先恢复事件企业，再调用 REQUIRES_NEW 持久化服务；持久化服务核对事件与当前企业一致。各库独立去重，相同 requestId 在两家企业分别有效。
- 通知提交后的推送也携带明确企业。数据库已提交后，停用或推送失败只记录日志，不把原业务响应反向变成失败；收件箱仍是可靠查询来源。
- WS 握手验证 JWT 企业、有效期、账号状态和冲突企业头；连接按 `(tenantId,userId)` 分组，不接收客户端自选 userId。
- 并发发送由 ConcurrentWebSocketSessionDecorator 串行化，慢连接有缓冲和发送时间上限。发送前与默认每 10 秒清理过期 Token/停用企业连接；检查周期可用 `tenancy.websocket-check-interval-ms` 配置。平台不可用时关闭相关连接，不继续使用旧授权。

当前仍是单应用实例的 WS 会话管理，没有实现跨节点消息总线；不能直接扩为多应用副本并声称通知投递完整。

## AI 记忆、工具与流式回调

服务端生成 multi 记忆标识 `t:{tenantId}:u:{userId}:c:{conversationId}`；模型和客户端不能自行挑选。记忆读取/写入先恢复企业并检查会话用户、逻辑删除与归档状态，再访问该企业的 JDBC 表。

新增 Flyway `V3__tenant_chat_memory_id.sql`，把记忆标识列从 36 扩为 192 字符，并按本库会话所有者转换既有数值 ID，保留已有模型记忆。迁移器先完成 V2、写入物理库归属，再执行 V3；没有匹配会话的旧记忆保留但不对模型开放。V1/V2 没有修改；平台版本保持 2，租户当前版本变为 **3**，运行时拒绝旧租户版本。升级仍使用 [T3 的逐库入口](tenancy-t3-flyway.md)。single 继续使用原数值会话 ID，不要求旧单企业库跟随新租户轨道。

Spring AI 2.0 的 JDBC builder 即使显式指定方言仍会探测数据源，因此多租户记忆延迟至首个有效企业作用域内构建；启动不获取无企业的业务连接，也不自动建表。

流式链路把不可变 TenantExecution 放入 Reactor Context；工具、消息保存、标题和动作回调都在自身边界恢复身份，避免只在 HTTP Filter 设置 ThreadLocal。模型记忆另按显式 memory ID 恢复企业，因为 advisor 可能在模型线程执行。模型持续输出期间每 5 秒重新检查企业状态，停用后取消流；订阅前停用、模型错误或未完成时取消，都不继续启动后续成功回调，已经提交的写入不会撤销。

待确认动作的 Redis 键、值与执行锁都有企业边界，确认时核对企业、用户、有效期、业务单号及版本。生成动作时还检查会话归属和归档状态。锁使用随机持有者，释放采用原子比较删除，过期旧任务不会误删新持有者的锁。旧 single 待确认动作若缺少 tenantId 会被拒绝，重新发起即可，不改变账号密码。

这些边界参考 [Reactor Context 传播说明](https://projectreactor.io/docs/core/release/reference/advanced-contextPropagation.html) 和 [Spring AI 记忆标识说明](https://docs.spring.io/spring-ai/reference/api/chat-memory.html)，具体 API 以仓库实际使用的 Spring AI 2.0.0 源码为准。

## 验证

```bash
python3 sql/platform/test_tenants.py
```

脚本创建并清理临时 MySQL 8.4、Redis 7.4；包含 Flyway、请求隔离、真实 Redis 与相关既有单元测试。若本机禁止 Mockito 自附加，可传入匹配版本的 `-DargLine=-javaagent:/path/to/mockito-core.jar`。缺少集成环境导致跳过不能算验收通过。

已覆盖：同用户 ID 的权限/菜单索引失效隔离、单号与 Redisson 锁隔离、动作值归属和锁持有者校验、跨线程快照恢复/异常/线程复用、WS 企业与有效期、真实 MyBatis/新事务通知、重复通知只推一次、真实 Spring AI advisor/JDBC 记忆与流式消息/标题保存，以及模型错误、取消和停用中的流。

模型使用本地测试替身，不调用外部模型服务；通知与 AI 存储使用真实隔离数据库。HTTP 验证专用 Controller 仅在测试源码中。

本轮实测：92 项相关回归全部通过、无跳过。干净副本的复现脚本成功创建/清理独立 MySQL、Redis，包含旧库记忆转换；最终 JAR 打包与冷启动通过。另从干净源码执行平台及双企业初始化，验证同用户 ID=80001 的实际登录与独立权限缓存，正式业务入口仍返回 503。没有连接或迁移现有业务库。

## 下一步

推进 T5：Web 与移动端企业登录、当前企业展示、退出切换时取消旧请求/WS/AI，并拦截旧企业晚到的响应。之后完成真实业务同 ID 联调、旧库单号计数接管、连接预算和恢复演练，再决定开放多租户业务。RAG 当前只有未接入的实体/Mapper，没有新增对外检索或导出接口。
