# 服务端开发与部署参考

产品定位、能力范围与首次启动入口见 [README](../README.md)。本文面向需要联调、扩展功能和维护部署的开发者，命令均从仓库根目录执行。

## 快速开始

### 环境要求

- JDK 21
- MySQL 8.x
- Redis 6+
- Git

项目已包含 Maven Wrapper，无需额外安装 Maven。

### 获取代码

```bash
git clone https://github.com/weigu2700-cell/ORIGIN-ERP-BackEnd.git
cd ORIGIN-ERP-BackEnd
```

### 初始化数据库

先选择部署轨道：现有单企业环境使用本节 bootstrap；多租户新库、旧库接管使用下方“组织与多租户”中的逐库 Flyway 入口。两条路径不要对同一业务库混用。

2026-09-26 的“数据库转储”提交只包含 DDL，没有用户、权限或菜单预置数据。
首次开发部署请使用下列入口（需要 Python 3 和 MySQL 8 客户端）：

```bash
# 从后端仓库根目录执行；-p 交互输入数据库密码
python3 sql/bootstrap/run.py schema -h127.0.0.1 -uroot -p
python3 sql/bootstrap/run.py seed-dev -h127.0.0.1 -uroot -p
python3 sql/bootstrap/run.py verify -h127.0.0.1 -uroot -p
```

执行顺序为：全部基础表 → 权限 → 菜单 → 部门/管理员角色/账号及关联 →
启动应用，由 Flyway 执行 `src/main/resources/db/migration/` 的增量迁移。
开发默认账号 **`admin` / `OriginDev@123`**，密码以 BCrypt 保存，首次登录后修改。
seed 可重复执行且保留已有密码；schema 入口仅用于空库，不可在已有业务库重跑。
完整的审计结论、生产部署区别、缓存处理和登录验收见 [数据库初始化说明](../sql/bootstrap/README.md)。
通知表使用 `eip_notification*`，已包含在基础表清单中，无需额外通知迁移脚本。

数据库、Redis 和 JWT 默认配置位于 `src/main/resources/application.yaml`。本地可直接按文件中的开发配置启动；共享、测试和生产环境应使用环境变量或外部配置覆盖，禁止沿用仓库中的开发凭据。

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/smart-erp?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
export SPRING_DATASOURCE_USERNAME='root'
export SPRING_DATASOURCE_PASSWORD='<local-database-password>'
# 生成一次后存入密钥管理系统；不要每次启动都重新生成
openssl rand -base64 32
export JWT_SECRET='<base64-secret-from-previous-command>'
```

Spring Boot 会通过环境变量的宽松绑定将上述 `SPRING_DATASOURCE_*` 配置映射到 `spring.datasource.*`。

常用配置项：

| 配置                                                       | 默认值                                     | 说明                            |
| ---------------------------------------------------------- | ------------------------------------------ | ------------------------------- |
| `spring.datasource.url`                                    | `jdbc:mysql://localhost:3306/smart-erp...` | MySQL 连接地址                  |
| `spring.datasource.username`                               | `root`                                     | MySQL 用户名                    |
| `spring.datasource.password`                               | 本地开发值                                 | 生产环境必须覆盖                |
| `spring.data.redis.host`                                   | `127.0.0.1`                                | Redis 地址                      |
| `spring.data.redis.port`                                   | `6379`                                     | Redis 端口                      |
| `security.jwt.secret` / `JWT_SECRET`                       | 开发密钥                                   | Base64 编码，解码后至少 32 字节 |
| `security.jwt.expiration-millis` / `JWT_EXPIRATION_MILLIS` | `18000000`                                 | Token 有效期，默认 5 小时       |

`JWT_SECRET` 必须是 Base64 编码且解码后至少 32 字节。可选的 `JWT_EXPIRATION_MILLIS` 默认为 `18000000`（5 小时）。

### 启动服务

macOS / Linux：

```bash
./mvnw spring-boot:run
```

Windows：

```powershell
./mvnw.cmd spring-boot:run
```

> 前置依赖：本地需运行 Redis（默认 `127.0.0.1:6379`）；AI 助手需配置大模型密钥，例如：
> `AI_API_KEY=sk-xxx ./mvnw spring-boot:run`（当前接入 DeepSeek，模型名见 `application.yaml` 的 `spring.ai.openai`）。

默认服务地址：<http://localhost:8080>

Swagger UI：<http://localhost:8080/swagger-ui.html>

OpenAPI JSON：<http://localhost:8080/v3/api-docs>

## 组织与多租户

部署方案为 **一租户一库、共用服务**。默认 `tenancy.mode=single` 兼容单企业运行；多租户验证使用 `tenant-verification` profile 和仓库外部连接配置。平台只保存连接引用，真实连接与凭据由部署环境提供，客户端不能指定数据库。

### 当前可用范围

- 已实现组织解析、组织 JWT、数据库路由、物理库归属校验、独立 Flyway 历史，以及 Redis、通知、WebSocket、AI 与异步上下文边界。
- Web 与移动端已接入组织编码核验、当前组织展示、退出后重新登录切换；最近组织仅为客户端本地记录，不是公开企业目录。
- **multi 模式目前 `businessAvailable=false`**：允许登录和读取当前会话，主页提示“当前组织暂未开放业务”；采购、生产、库存、菜单、AI、WebSocket 等正式业务入口仍返回 503。不能仅修改前端页面就开放业务。
- 正式放开前仍需完成同 ID 的跨组织业务联调、旧单号计数接管、连接预算、备份恢复和灰度验收。平台运维 UI、自动建库和连接池热轮换尚未实现。

### Flyway 轨道与执行顺序

| 对象               | 迁移目录                | 历史表                           | 当前版本         |
| ------------------ | ----------------------- | -------------------------------- | ---------------- |
| 平台库             | `db/platform-migration` | `flyway_platform_schema_history` | 2                |
| 每个租户库         | `db/tenant-migration`   | `flyway_tenant_schema_history`   | 3                |
| 未接管的旧单企业库 | `db/migration`          | `flyway_schema_history`          | 保留既有增量历史 |

目录均位于 `src/main/resources/`。新租户的 V1 建表、V2 公共权限/菜单及归属标记、V3 AI 记忆标识升级按顺序执行；公共 seed 不包含通用开发账号或虚构业务数据。已应用脚本保持不可变，后续结构变更新增版本。

1. 部署人员预先创建独立平台库和租户库，准备迁移账号与运行账号；将 [配置模板](../sql/tenant/tenancy.example.properties) 复制到仓库外。
2. 执行 `platform-init` 初始化空平台库；已有 T1 登记库使用 `platform-adopt`。然后按 [T3 操作说明](plans/tenancy-t3-flyway.md) 登记目标租户，保持 PROVISIONING/UPGRADING。
3. 新租户通过环境变量注入 `TENANT_ADMIN_USERNAME` 和 `TENANT_ADMIN_BCRYPT`（BCrypt cost 10–16），再执行 `new`、`verify`；**没有默认账号或密码**。已有业务库先备份停服，按结构契约检查后执行 `adopt`，保留已有账号及密码。
4. 后续逐租户执行 `upgrade`、`verify`；全部校验成功才更新 READY/ACTIVE。失败检查操作记录与 Flyway 历史，不通过删除历史、自动 repair 或 clean 绕过校验。

```bash
# 从后端仓库根目录执行；配置文件与凭据由部署方准备
python3 sql/tenant/run.py platform-init /secure/tenancy.properties
# 完成平台登记并注入管理员环境变量后：
python3 sql/tenant/run.py new /secure/tenancy.properties enterprise-a
python3 sql/tenant/run.py verify /secure/tenancy.properties enterprise-a
# 后续版本升级：
python3 sql/tenant/run.py upgrade /secure/tenancy.properties enterprise-a
```

共用服务启动时使用运行账号对应的外部配置；该 profile 关闭旧全局 Flyway 与 AI 自动建表，数据库维护由上述独立入口完成：

```bash
SPRING_PROFILES_ACTIVE=tenant-verification \
SPRING_CONFIG_ADDITIONAL_LOCATION=file:/secure/tenancy-runtime.properties \
./mvnw spring-boot:run
```

方案及维护细节见 [多租户方案](plans/multi-tenancy.md)、[Flyway 操作说明](plans/tenancy-t3-flyway.md)、[缓存与异步隔离](plans/tenancy-t4-isolation.md)。

## 认证与响应

组织相关接口与最新 Web、移动端配套使用：

| 方法   | 地址                   | 认证与用途                                            |
| ------ | ---------------------- | ----------------------------------------------------- |
| `GET`  | `/auth/tenant/context` | 匿名；返回 single/multi，single 附带默认组织          |
| `POST` | `/auth/tenant/resolve` | 匿名；入参 `tenantCode`，仅返回已核验的组织编码与名称 |
| `POST` | `/auth/login`          | 匿名；multi 提交 `tenantCode`、`username`、`password` |
| `GET`  | `/auth/tenant/session` | JWT；返回当前组织、用户基础身份和 `businessAvailable` |

没有公开组织列表。single 兼容省略 `tenantCode`；multi 必须提供有效组织编码。新 JWT 绑定组织，后续请求以验签结果确定组织，不能通过修改请求头切换。无效登录返回 401，组织停用或冲突返回 403，平台/目标库不可用或业务门禁返回 503。

登录成功后，客户端在受保护请求中携带 JWT：

```http
Authorization: Bearer <token>
```

接口使用统一响应结构：

```json
{
  "code": 200,
  "msg": "请求成功",
  "data": {}
}
```

权限模型：

```text
User ── UserRole ── Role ── RoleMenu ── Menu
                       └──── RolePermission ── Permission
```

## 缓存设计

系统采用 **Cache-Aside（旁路缓存）** 模式，以 Redis 作为集中缓存，目标是减少读多写少场景（尤其是多级 BOM 递归展开）对数据库的重复查询。以下示例使用 single 模式的原始键；multi 模式会统一加上 `erp:t:{tenantId}:`，Redisson 锁也绑定组织，缺少有效组织上下文时拒绝访问。详见 [T4 隔离说明](plans/tenancy-t4-isolation.md)。

### BOM 缓存

BOM 展开是典型的高读压力场景：一次展开可能递归数十个节点，每个节点都要查询 BOM 头与 BOM 明细。为此引入物料级 active BOM 缓存。

- **缓存键**：`erp:bom:hot:{materialId}`
- **缓存值**：`BOMCacheDto`，包含 BOM 头字段与 `BOMItemCacheDto` 明细列表，代表某物料当前 `ACTIVE` 版本（version 最大）的 BOM。
- **TTL**：正常 BOM 120-150 分钟（随机抖动，避免缓存雪崩），空哨兵 5-10 分钟。

读取流程（由 `BOMRedis#getOrLoad` 统一负责，Service 只提供 DB loader）：

```text
1. 查 Redis（key = `erp:bom:hot:{materialId}`）
   ├── 命中 BOMCacheDto → 直接返回
   ├── 命中 EMPTY → 返回 null（空值缓存 TTL 5~10 分钟）
   └── 未命中/历史脏类型
        2. 用 Redisson 锁 `erp:lock:bom:build:{materialId}` 双检
        3. 查 DB：取该物料 status=ACTIVE 且 version 最大的 BOM 及明细
        4. 正常值缓存 120~150 分钟；无 BOM 写入 EMPTY 空值缓存
        5. 返回 BOMCacheDto 或 null
```

`BOMController` 的 BOM 展开（`getBOMExplosion`）及递归 `buildExplosionChildren` 均通过该入口取数，树中公共子件（如 A→B→D 与 A→C→D 的 D）第二次起直接命中缓存。

### 失效策略（写时失效）

旁路缓存必须保证写入后缓存不脏读，因此在 BOM 状态变更时主动删除缓存键：

| 操作     | 触发方法     | 缓存动作                                  |
| -------- | ------------ | ----------------------------------------- |
| 激活 BOM | `activeBOM`  | 事务提交后 `evictAfterCommit(materialId)` |
| 停用 BOM | `disableBOM` | 事务提交后 `evictAfterCommit(materialId)` |
| 新增明细 | `addBOMItem` | 事务提交后 `evictAfterCommit(materialId)` |
| 新增 BOM | `addBOM`     | 不影响（新增为 DRAFT，不参与缓存）        |

### 已知权衡

- **缓存穿透**：不存在 active BOM 的采购件 / 原材料写入 `EMPTY` 空值标记，短 TTL 降低递归展开中的重复回源。
- **提交一致性**：BOM 写路径仅在数据库事务 `afterCommit` 后失效缓存；事务回滚不会误删旧缓存，删除失败只记录日志，不反向影响已提交事务。
- **TTL 兜底**：缓存删除失败时，旧值可能保留至实际 TTL 到期；正常值 TTL 为 120–150 分钟，需要结合业务对陈旧数据的容忍度维护失效链路。

发货单状态锁同样使用 Redisson watchdog（`erp:lock:sales-delivery:{id}`），在事务完成后释放。库存入库使用数据库唯一键
`uk_material_warehouse_id` 配合 MySQL `INSERT ... ON DUPLICATE KEY UPDATE` 原子累加；这条 SQL 路径同时覆盖首次并发入库和既有库存增量，流水在同事务回查后的库存上计算。

开放 Issue 的处理结论与新增能力计划见 [Issue 核查与实施计划](plans/open-issues.md)。

## 消息通知与 WebSocket

### 全链路流程

```text
业务状态流转成功
      │
      ▼
eip.service.NotificationPublisher.publish(NotificationPublishDTO) 发布业务事件
      │
      ▼
原业务事务提交成功（回滚则不发送）
      │
      ▼
eip.listener.NotificationPublishEventListener
      │
      ├── 指定用户：仅通知该用户
      └── 权限编码：解析启用角色、启用用户，并由管理员兜底
      │
      ▼
写入 eip_notification（独立事务，isRead=false）
      │
      ▼
通知事务提交后，由现有 WebSocketSessionManager 定向推送
      │
      ├── 在线：发送 TextMessage
      └── 离线：保留数据库记录，登录后通过 REST 拉取
```

业务模块只依赖 `eip.service.NotificationPublisher` 和 DTO，不直接依赖通知表、权限表或 WebSocket。
发布事件在原业务事务中产生，监听器在 `AFTER_COMMIT` 阶段解析收件人，再以独立事务写入发布批次和收件箱；
收件箱事务提交后才尝试 WebSocket 推送。推送失败只记录警告，不回滚业务数据或已经持久化的通知。

每次发布以 `requestId` 作为批次幂等键，业务通知默认生成
`BUSINESS:{businessType}:{businessId}`；同一批次通过 `(publish_id, user_id)` 保证每位收件人只有一条收件箱记录。
不同业务阶段使用不同 `businessType`，因此“报工待审批”“报工审批通过”和“报工驳回”可以分别通知。

### 已接入的业务节点

| 业务节点                     | 收件人规则                       | 通知后的动作       |
| ---------------------------- | -------------------------------- | ------------------ |
| 生产需求已自动排产           | 拥有生产订单下达权限的用户       | 审核并下达生产订单 |
| 采购需求创建                 | 拥有采购需求审批权限的用户       | 审批采购需求       |
| 采购需求审批通过             | 拥有采购订单新增或编辑权限的用户 | 创建或更新采购订单 |
| 采购入库单创建               | 拥有采购入库审批权限的用户       | 审批入库单         |
| 采购入库审批通过             | 拥有采购入库上架权限的用户       | 完成上架           |
| 生产领料单备料完成或审批通过 | 拥有领料确认权限的用户           | 确认领料           |
| 生产报工提交                 | 拥有报工审批权限的用户           | 审批报工单         |
| 生产报工审批通过或驳回       | 报工人本人                       | 查看审批结果       |
| 报工完成并生成成品入库单     | 拥有成品入库操作权限的用户       | 审批成品入库单     |
| 销售发货单确认               | 拥有销售出库完成权限的用户       | 完成销售出库       |

### 建立连接与认证

WebSocket 端点为：

```text
ws://localhost:8080/ws/notification?token=<JWT>
```

HTTPS 环境使用：

```text
wss://api.example.com/ws/notification?token=<JWT>
```

握手流程：

1. `/ws/**` 在 Spring Security HTTP 过滤链中放行，使请求可以完成协议升级。
2. `WebSocketAuthInterceptor` 从 `token` 查询参数读取 JWT，并调用现有 `JwtUtil` 校验。
3. 校验成功后将 `tenantId`、`userId` 和有效期写入 WebSocket session attributes；校验失败直接拒绝握手。
4. `NotificationWebSocketHandler` 建立连接时注册用户会话，关闭时精确移除同一会话。
5. `WebSocketSessionManager` 使用线程安全 Map 保存当前在线连接，并按 `(tenantId, userId)` 定向发送文本消息。

当前实现支持同一组织账号多浏览器、多设备同时在线：新连接追加而非替换旧连接；推送遍历该组织用户的全部活动会话。并发发送串行化，慢连接有缓冲和发送时限；失效连接独立移除。发送前与周期检查会核验有效期和组织状态。会话管理仍局限于单应用实例，尚无跨节点消息总线；multi 的正式 WS 入口仍受业务门禁限制。

JWT 位于 WebSocket URL 查询参数中。生产环境必须使用 WSS，并避免在反向代理访问日志、APM 或错误页中记录完整查询字符串。

### REST API

所有 REST 接口都基于 `CurrentUser` 限定当前用户数据。详情查询和已读操作不会访问其他用户的通知。

| 方法  | 地址                             | 说明                                                      |
| ----- | -------------------------------- | --------------------------------------------------------- |
| `GET` | `/sys/notification`              | 分页查询通知，参数为 `pageNum`、`pageSize`、可选 `isRead` |
| `GET` | `/sys/notification/{id}`         | 查询当前用户的一条通知详情                                |
| `GET` | `/sys/notification/unread/count` | 查询当前用户未读数量                                      |
| `PUT` | `/sys/notification/{id}/readed`  | 将当前用户指定通知标记为已读                              |
| `PUT` | `/sys/notification/all/readed`   | 将当前用户全部未读通知标记为已读                          |

通知类型：

| 编码 | 枚举       | 含义       |
| ---- | ---------- | ---------- |
| `0`  | `SYSTEM`   | 系统消息   |
| `1`  | `BUSINESS` | 业务消息   |
| `2`  | `WARNING`  | 预警消息   |
| `3`  | `TASK`     | 任务消息   |
| `4`  | `CUSTOM`   | 自定义消息 |

WebSocket 推送内容与通知详情字段保持一致，示例：

```json
{
  "id": "9007199254740993",
  "userId": "10001",
  "type": 1,
  "title": "销售订单已确认",
  "content": "销售订单 SO-001 已完成确认，请及时处理后续业务。",
  "businessType": "sales_order",
  "businessId": "20001",
  "businessNo": "SO-001",
  "isRead": false,
  "readTime": null,
  "createTime": "2026-09-17 08:00:00",
  "updateTime": "2026-09-17 08:00:00"
}
```

`Long` 类型 ID 返回给 JavaScript 客户端时应保持字符串形式，避免超过 `Number.MAX_SAFE_INTEGER` 后产生精度损失。

### 在业务中发送通知

业务 Service 注入 `NotificationPublisher`，用 `NotificationBusinessRefDTO` 表达业务引用，
用 `RecipientSelectorDTO` 表达收件人选择：

```java
NotificationPublishDTO publish = NotificationPublishDTO.business(
        NotificationType.TASK,
        "待审批采购需求",
        "采购需求 PR-001 已创建，请及时审批。",
        NotificationBusinessRefDTO.of(
                "PURCHASE_DEMAND_PENDING_APPROVAL", demandId, "PR-001"),
        RecipientSelectorDTO.permissions(
                Set.of("purchase:demand:approve"), true));
notificationPublisher.publish(publish);
```

发布时也可以直接选择用户（例如报工审批结果）：设置 `recipients.userIds`，并保持
`includeAdministrators=false`，因此不会扩散给管理员。权限选择会解析启用角色和启用用户，
`includeAdministrators=true` 时额外覆盖启用管理员。系统通知可通过 `/eip/notification-templates`
提前配置模板和收件人；发布接口 `/eip/notifications/publish` 支持 PRESET_ONLY、MERGE、OVERRIDE
策略，分别表示仅模板收件人、模板与手选收件人合并、手选收件人覆盖模板。
不使用模板直接发布时，默认使用本次手选收件人；调用方可传入稳定 `requestId` 防止重复提交。

业务模块不应直接操作 `NotificationPersistenceService` 或 `WebSocketSessionManager`，统一通过单参数
`NotificationPublisher#publish` 保持“业务提交、通知落库、实时推送”的顺序和故障隔离。

## AI 助手

AI 助手基于 Spring AI 2.0（OpenAI 兼容协议，当前接入 DeepSeek 大模型）构建，以对话方式帮助业务人员查询库存、解读单据与经营指标。它不是一个独立账号，而是**以当前登录用户的身份**调用后端业务方法，因此受与前端完全一致的 RBAC 约束。

### 能力概览

- **多轮对话**：Spring AI 的 JDBC `ChatMemory` 按 `conversationId` 维护模型上下文；`ai_conversation` / `ai_message` 则保存前端可见的对话和消息历史（建表脚本见 `sql/smart-erp/ai_conversation.sql` 和 `sql/smart-erp/ai_message.sql`）。
- **工具调用（Tool Calling）**：模型可在生成过程中调用业务工具查询真实数据。已接入库存（`get_material_stock` 按物料编码聚合各仓库在库/预留/可用量）、销售（`query_sales_orders` / `query_sales_deliveries`）、生产（`query_production_orders` / `query_production_demands`）、采购（`query_purchase_demands` / `query_purchase_orders`）与通知（`query_my_notifications`）等只读查询工具，并以当前登录用户身份受 RBAC 约束。
- **流式与非流式**：提供一次性返回与 SSE 逐字流式返回两种模式，前端可边生成边渲染。
- **对话与消息管理**：创建、查看、归档对话与拉取历史消息，均按当前用户归属隔离。

### 接口

| 方法   | 地址                                        | 说明                                      |
| ------ | ------------------------------------------- | ----------------------------------------- |
| `POST` | `/ai/assistant/chat`                        | 非流式对话                                |
| `POST` | `/ai/assistant/chat/stream`                 | 流式对话（SSE，响应 `text/event-stream`） |
| `POST` | `/ai/conversation`                          | 创建对话，返回 `conversationId`           |
| `GET`  | `/ai/conversation`                          | 当前用户的对话列表                        |
| `GET`  | `/ai/conversation/{conversationId}`         | 获取指定对话（归属校验）                  |
| `PUT`  | `/ai/conversation/{conversationId}/archive` | 归档对话                                  |
| `GET`  | `/ai/message/{conversationId}`              | 拉取某对话的历史消息                      |

对话入参 `AiAssistantRequest`：

```json
{
  "conversationId": "10001",
  "message": "查一下物料 A-1001 在各仓库的库存情况"
}
```

流式接口返回 `Flux<AiAssistantStreamResult>`（内容、标题或错误事件）；非流式返回 `Result<AiAssistantResult>`。需确认的业务动作与普通查询分开处理；RAG 目前仍是未接入主链路的草稿，不应按已上线知识库部署。

模块包结构与依赖规则见 [AI 模块结构](ai-architecture.md)。

### 鉴权模型

- 所有 AI 接口同样需要 `Authorization: Bearer <token>`。
- 助手以当前登录用户身份执行。工具调用走的是 `MaterialStockService.pageMaterialStock()`，该方法带有 `@PreAuthorize("hasAnyAuthority('inventory:material-stock:list')")`，因此用户必须拥有相应权限工具才会成功，否则返回无权限错误。
- 对话与消息在当前组织数据库内按 `userId` 校验归属；multi 的模型记忆标识由服务端生成为 `t:{tenantId}:u:{userId}:c:{conversationId}`，异步/流式回调显式恢复组织和用户身份。正式 AI 入口仍受 multi 门禁限制。

### 配置

AI 配置位于 `application.yaml` 的 `spring.ai` 段，敏感项建议用环境变量覆盖：

| 配置                                                      | 环境变量                              | 说明                                                                 |
| --------------------------------------------------------- | ------------------------------------- | -------------------------------------------------------------------- |
| `spring.ai.openai.api-key`                                | `AI_API_KEY`                          | 大模型 API Key（当前为 DeepSeek）                                    |
| `spring.ai.model.embedding`                               | `AI_EMBEDDING_MODEL`                  | 默认 `none`，RAG 尚未接入聊天                                        |
| `spring.ai.openai.embedding.api-key`                      | `AI_EMBEDDING_API_KEY`                | 预留的嵌入模型密钥                                                   |
| `spring.ai.openai.base-url`                               | —                                     | 服务地址，当前 `https://api.deepseek.com`                            |
| `spring.ai.openai.chat.options.model`                     | `SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL` | 模型名，须与部署账号实际可用模型一致                                 |
| `spring.ai.chat.memory.repository.jdbc.initialize-schema` | —                                     | 旧 single 配置可自动建表；多租户验证为 `never`，改由逐库 Flyway 管理 |

> 模型名以部署环境配置和服务端实际支持值为准。调用报模型不支持时，核对服务商账号可用模型后修改配置。

## 项目结构

```text
src/main/java/org/smart/erp/
├── common/       # 响应、异常、安全、WebSocket、配置、序号、Excel 和通用工具
├── ai/           # AI 助手：controller、request、result、service、persistence、config、tool、rag
├── system/       # 用户、角色、部门、菜单、权限和认证
├── tenant/       # 组织身份、登记、数据源路由、Flyway 维护及异步隔离
├── eip/          # 通知实体、DTO、收件人解析、持久化与 WebSocket 适配
│   ├── entity/ dto/ mapper/ service/ (service/impl/)
│   ├── controller/ converter/ event/ listener/ adapter/ port/
│   └── enums/ vo/
├── master/       # 客户、供应商、工厂、车间、产线、仓库和物料
├── inventory/    # 物料库存与库存流水
├── sales/        # 销售订单与销售出库
├── production/   # BOM、生产需求、生产订单和生产领料
└── purchase/     # 采购需求、采购订单和采购入库

sql/smart-erp/
├── sys_*.sql              # 系统与权限表
├── md_*.sql               # 基础资料表
├── inv_*.sql              # 库存表
├── sal_*.sql              # 销售表
├── prd_*.sql              # BOM 与生产表
├── pur_*.sql              # 采购表
├── permission_init.sql    # 与后端鉴权一致的权限初始化
├── menu_init.sql          # 配套 Web 客户端菜单
└── seed_dev_admin.sql     # 本地开发管理员数据
```

每个业务模块通常包含：

```text
controller → dto → service → mapper → entity / vo
```

## 构建与验证

```bash
# 编译
./mvnw clean compile

# 运行测试
./mvnw test

# 打包
./mvnw clean package
```

测试集使用 Testcontainers + MySQL 8.4 验证首次并发入库。Docker 可用时会自动运行该集成测试；
未安装或未启动 Docker 时自动跳过，不阻断其余单元测试。CI 应提供 Docker 环境，避免将该并发测试跳过。

构建产物位于 `target/`。

数据库初始化与多租户隔离有独立回归入口，需要 Docker；脚本使用临时数据库/Redis，不连接开发业务库：

```bash
python3 sql/bootstrap/test_seed.py
python3 sql/platform/test_tenants.py
```

未提供集成环境导致的跳过不能视为隔离验收通过。前端配套回归分别在 Web 仓库运行 `npm run test:unit -- --run`、移动端仓库运行 `npm run test:run`。

## 开发规范

- Controller 负责参数校验、权限声明和统一响应，核心业务逻辑放在 Service。
- 跨表写操作使用事务，订单与库存变化必须保持原子性。
- 状态切换必须校验当前状态，禁止客户端绕过业务流程。
- 实体主键使用 `Long`；传给 JavaScript 客户端时按字符串处理。
- 枚举通过 MyBatis-Plus 映射数据库值，避免散落的魔法数字。
- 新增字段时同步更新实体、DTO、VO、Mapper 查询和 SQL 脚本。
- 新增或修改接口时同步维护 OpenAPI 注解和客户端类型。

## 生产部署建议

- 使用环境变量或配置中心管理数据库、Redis 和 JWT 配置。
- 不要在仓库中提交生产密码、私钥或访问令牌。
- 在反向代理层启用 HTTPS、请求大小限制和访问日志。
- 为 `/ws/notification` 配置 WebSocket 协议升级、合理的空闲超时和 WSS；不要记录带 JWT 的完整查询字符串。
- 对数据库执行定期备份，并对库存、订单等核心表保留审计能力。
- 部署前执行完整测试和打包，确认目标环境使用 JDK 21。
