<div align="center">
  <h1>ORIGIN ERP Service</h1>

  <p>面向制造企业的 ERP 后端服务，以可配置权限、可追溯库存和跨业务单据协同为核心。</p>

  <p>
    <img src="https://img.shields.io/badge/Java-21-ed8b00?logo=openjdk&logoColor=white" alt="Java 21" />
    <img src="https://img.shields.io/badge/Spring_Boot-3.4-6db33f?logo=springboot&logoColor=white" alt="Spring Boot 3.4" />
    <img src="https://img.shields.io/badge/MyBatis--Plus-3.5-2f54eb" alt="MyBatis-Plus 3.5" />
    <img src="https://img.shields.io/badge/MySQL-8-4479a1?logo=mysql&logoColor=white" alt="MySQL 8" />
    <a href="./LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="MIT License" /></a>
  </p>
</div>

## 项目简介

ORIGIN ERP Service 是原点 ERP 的统一业务后端。项目采用 Spring Boot 模块化单体架构，按照系统、基础资料、销售、库存、生产和采购划分业务边界，并通过 JWT、RBAC、事务、库存流水和状态机式业务校验保障系统安全与数据一致性。

配套项目：

- [ORIGIN ERP Web](https://github.com/weigu2700-cell/ORIGIN-ERP--FrontEnd)
- [ORIGIN ERP 移动端](https://github.com/weigu2700-cell/ORIGIN-ERP-Moblie)

## 项目亮点

- **模块化单体架构**：保留单体部署和事务的一致性优势，同时按业务域隔离 Controller、Service、Mapper 和模型。
- **RBAC 双层权限**：菜单控制页面入口，Permission 控制接口操作，支持角色、部门、菜单和操作权限组合配置。
- **制造业务闭环**：销售需求可驱动生产，生产订单基于 BOM 计算净需求，缺料衔接采购，出入库同步库存。
- **库存全程可追溯**：统一维护在库量、预留量和可用量，关键库存变化写入流水。
- **明确的单据状态流转**：订单确认、审核、下达、开工、完工、取消和上架均在服务端验证前置状态。
- **多级 BOM 能力**：支持 BOM 版本管理、启用/停用、递归展开和按生产数量计算物料需求。
- **全链路缓存**：基于 Redis 的旁路缓存（Cache-Aside），BOM 展开查询优先命中缓存，显著降低多级 BOM 递归查询的数据库压力，并在 BOM 激活/停用时主动失效。
- **移动作业复用同一后端**：Web 与移动端共享账户、权限、业务数据和统一响应协议。
- **开箱即用的接口文档**：通过 Springdoc OpenAPI 提供 Swagger UI，便于联调和接口验收。

## 业务能力

| 业务域   | 主要能力                                                 |
| -------- | -------------------------------------------------------- |
| 系统管理 | 登录认证、用户、角色、部门、菜单、权限及关联分配         |
| 基础资料 | 客户、供应商、物料、仓库、工厂、车间、生产线、物料供应商 |
| 销售管理 | 销售订单、库存预留、销售出库及状态流转                   |
| 库存管理 | 物料库存、预留/释放、可用量、库存流水、Excel 导入导出    |
| BOM 管理 | BOM 创建、版本、启用/停用、多级展开、物料需求计算        |
| 生产管理 | 生产需求、生产订单、下达/开工/完工/取消、生产领料        |
| 采购管理 | 采购需求、采购订单、采购入库审核与上架                   |

## 业务链路

```text
销售订单
   │
   ├── 库存充足 ──→ 库存预留 ──→ 销售出库 ──→ 库存流水
   │
   └── 库存不足 ──→ 生产需求 ──→ 生产订单 ──→ BOM 净需求
                                          │
                                          ├── 库存领料
                                          └── 采购需求 ──→ 采购订单 ──→ 入库审核/上架
```

库存口径：

```text
可用量 = 在库量 - 预留量
```

## 技术栈

| 分类       | 技术                           |
| ---------- | ------------------------------ |
| 运行环境   | Java 21                        |
| 核心框架   | Spring Boot 3.4.5              |
| Web 与校验 | Spring Web、Jakarta Validation |
| 认证授权   | Spring Security、JJWT 0.12     |
| 数据访问   | MyBatis-Plus 3.5               |
| 数据库     | MySQL 8.x                      |
| 缓存与单号 | Spring Data Redis、Redisson   |
| 接口文档   | Springdoc OpenAPI 2.7          |
| Excel      | FastExcel                      |
| 构建工具   | Maven Wrapper                  |

## 缓存设计

系统采用 **Cache-Aside（旁路缓存）** 模式，以 Redis 作为集中缓存，目标是减少读多写少场景（尤其是多级 BOM 递归展开）对数据库的重复查询。

### BOM 缓存

BOM 展开是典型的高读压力场景：一次展开可能递归数十个节点，每个节点都要查询 BOM 头与 BOM 明细。为此引入物料级 active BOM 缓存。

- **缓存键**：`erp:bom:hot:{materialId}`
- **缓存值**：`BOMCacheDto`，包含 BOM 头字段与 `BOMItemCacheDto` 明细列表，代表某物料当前 `ACTIVE` 版本（version 最大）的 BOM。
- **TTL**：正常 BOM 120~150 分钟（随机抖动，避免缓存雪崩），空哨兵 5~10 分钟。

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

| 操作       | 触发方法      | 缓存动作                           |
| ---------- | ------------- | ---------------------------------- |
| 激活 BOM   | `activeBOM`   | 事务提交后 `evictAfterCommit(materialId)` |
| 停用 BOM   | `disableBOM`  | 事务提交后 `evictAfterCommit(materialId)` |
| 新增明细   | `addBOMItem`  | 事务提交后 `evictAfterCommit(materialId)` |
| 新增 BOM   | `addBOM`      | 不影响（新增为 DRAFT，不参与缓存） |

### 已知权衡

- **缓存穿透**：不存在 active BOM 的采购件 / 原材料写入 `EMPTY` 空值标记，短 TTL 降低递归展开中的重复回源。
- **提交一致性**：BOM 写路径仅在数据库事务 `afterCommit` 后失效缓存；事务回滚不会误删旧缓存，删除失败只记录日志，不反向影响已提交事务。
- **TTL 兜底**：极端情况下（如缓存删除失败）最多 2 小时内读到旧值，依赖 TTL 自动过期保证最终一致。

发货单状态锁同样使用 Redisson watchdog（`erp:lock:sales-delivery:{id}`），在事务完成后释放。库存入库使用数据库唯一键
`uk_material_warehouse_id` 配合 MySQL `INSERT ... ON DUPLICATE KEY UPDATE` 原子累加；这条 SQL 路径同时覆盖首次并发入库和既有库存增量，流水在同事务回查后的库存上计算。

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

1. 创建数据库 `smart-erp`。
2. 执行 `sql/smart-erp/` 下的业务表脚本。
3. 执行 `permission_init.sql` 初始化权限数据。
4. 如需本地管理员账号，执行 `seed_dev_admin.sql`。

数据库与 Redis 默认配置位于 `src/main/resources/application.yaml`。数据库密码和 JWT 密钥不提供代码内默认值，
启动前必须通过环境变量或外部配置提供：

```bash
export DB_PASSWORD='<local-database-password>'
# 生成一次后存入密钥管理系统；不要每次启动都重新生成
openssl rand -base64 32
export JWT_SECRET='<base64-secret-from-previous-command>'
```

`JWT_SECRET` 必须是 Base64 编码且解码后至少 32 字节。可选的 `JWT_EXPIRATION_MILLIS`默认为 `18000000`（5 小时）。

### 启动服务

macOS / Linux：

```bash
./mvnw spring-boot:run
```

Windows：

```powershell
./mvnw.cmd spring-boot:run
```

默认服务地址：<http://localhost:8080>

Swagger UI：<http://localhost:8080/swagger-ui.html>

OpenAPI JSON：<http://localhost:8080/v3/api-docs>

## 认证与响应

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

## 项目结构

```text
src/main/java/org/smart/erp/
├── common/       # 响应、异常、安全、配置、序号、Excel 和通用工具
├── system/       # 用户、角色、部门、菜单、权限和认证
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
├── permission_init.sql    # 权限初始化
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
- 对数据库执行定期备份，并对库存、订单等核心表保留审计能力。
- 部署前执行完整测试和打包，确认目标环境使用 JDK 21。

## 未来计划

以下能力处于规划阶段，尚未落地：

- **WebSocket 任务推送**：服务端通过 WebSocket 主动向客户端推送任务与消息，例如生产任务下发、审批待办提醒、库存预警与单据状态变更通知，替代前端轮询，降低延迟与无效请求。
- **AI 智能问答助手**：基于企业业务数据（库存、订单、生产进度、采购等）的自然语言问答与辅助决策，支持以对话方式查询经营指标、定位异常单据，并逐步接入流程建议与自动化工单。

## 参与贡献

1. Fork 仓库并创建功能分支：`git checkout -b feature/your-feature`
2. 完成功能并执行 `./mvnw test`
3. 使用清晰的提交信息，例如：`feat: add production picking approval`
4. 推送分支并创建 Pull Request，说明业务规则、数据影响和验证方式

问题与建议请提交到 [GitHub Issues](https://github.com/weigu2700-cell/ORIGIN-ERP-BackEnd/issues)。

## 许可证

本项目基于 [MIT License](./LICENSE) 开源。
