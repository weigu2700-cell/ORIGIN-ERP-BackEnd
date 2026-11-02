# T3：用 Flyway 管理平台与租户数据库版本

本轮实现逐库初始化、显式旧库接管、升级校验与失败记录。多租户正式业务门禁仍保留；缓存、消息、AI 和异步隔离属于 T4。本轮不迁移现有业务库。

## 迁移轨道

| 对象 | 迁移目录 | 历史表 | 当前版本 |
| --- | --- | --- | --- |
| 平台库 | `src/main/resources/db/platform-migration` | `flyway_platform_schema_history` | 2 |
| 每个租户库 | `src/main/resources/db/tenant-migration` | `flyway_tenant_schema_history` | 2 |
| 未接管的旧单企业库 | 原 `db/migration` | 原 `flyway_schema_history` | 保留原有历史 |

平台 V1 是租户登记；V2 增加 `plt_tenant_operation`。租户 V1 冻结 `5118ebc` 的 39 张应用表，并纳入“采购入库实际日期可空”的兼容修正；生产领料仓库可空和 AI 记忆表也已包含。V2 创建物理库归属标记、117 个公共权限和 37 个菜单，补齐已有有效 admin 角色的关联。没有工厂、物料等虚构业务数据，没有开发账号或默认密码。

所有新脚本都不选择/创建数据库、不限定业务库名、不复制旧 Flyway 历史。迁移入口显式传入独立 DataSource，不使用 HTTP 租户路由。`baselineOnMigrate=false`、`cleanDisabled=true`，不提供自动 repair。

旧库的基线含义见 [Flyway baseline 说明](https://documentation.red-gate.com/flyway/flyway-concepts/baselines)；自动 baseline 会绕过非空库的保护检查，因此这里采用先校验、再显式接管的方式，参见 [baselineOnMigrate 官方说明](https://documentation.red-gate.com/fd/flyway-baseline-on-migrate-setting-277578974.html)。

已应用脚本不可修改；下一次变更新增 V3、V4。同时更新应用支持版本、对应的结构契约和回归。当前精确支持 schema_version=2，不承诺跨 schema 版本的滚动升级。

## 准备与权限

需要 JDK 21、Python 3、MySQL 8；本轮在 MySQL 8.4 验证。由部署方先创建独立平台库和租户库，字符集 `utf8mb4`、排序规则 `utf8mb4_0900_ai_ci`。本工具不持有建库、创建用户或授权能力。

将 `sql/tenant/tenancy.example.properties` 复制到仓库外，例如 `/secure/tenancy.properties`，替换地址并通过环境注入凭据。文件应仅允许部署人员读取。运行时可通过 `SPRING_CONFIG_ADDITIONAL_LOCATION` 加载同一格式的配置；连接引用与配置键一致，配置中各连接必须指向不同库。

维护进程使用独立的迁移账号：平台库需登记表 DML、迁移 DDL，目标库需 DDL/DML 和结构检查权限；不得写其他企业数据库。运行服务使用各租户自己的受限账号，不授予 DDL/建库权限。可使用相同配置结构，在维护进程与运行服务分别注入对应身份。

执行前停用/停止目标租户流量并备份；当前工具不自动排空在途事务，不作为在线 DDL 发布器。所有命令逐个指定租户，没有“自动升级全部库”入口。

## 平台初始化或 T1 登记库接管

空平台库：

```bash
python3 sql/tenant/run.py platform-init /secure/tenancy.properties
```

若平台库已经执行过 T1 的 `sql/platform/01_tenant_registry.sql`：

```bash
python3 sql/tenant/run.py platform-adopt /secure/tenancy.properties
```

接管先对照冻结的 `platform-v1.json` 检查结构，匹配后才 baseline=1，再执行 V2。已有未知表、列、索引或约束时拒绝接管。普通 `platform-init` 不会给非空未知库自动打基线。

## 新租户初始化

先在已升级的平台库登记，保持尚未开通状态：

```sql
START TRANSACTION;
INSERT INTO plt_tenant(id,code,name,status)
VALUES('enterprise-a','company-a','企业 A','PROVISIONING');
INSERT INTO plt_tenant_database(tenant_id,connection_ref,schema_status)
VALUES('enterprise-a','enterprise_a','UPGRADING');
COMMIT;
```

由部署方选择首位管理员用户名和密码。使用交互式 BCrypt 工具或密码管理系统生成 cost=10..16 的哈希，通过 `TENANT_ADMIN_USERNAME` 和 `TENANT_ADMIN_BCRYPT` 注入维护进程；不要把明文密码、哈希放进命令行参数或提交到仓库。此入口没有默认账号。

```bash
python3 sql/tenant/run.py new /secure/tenancy.properties enterprise-a
python3 sql/tenant/run.py verify /secure/tenancy.properties enterprise-a
```

`new` 拒绝已有未知表的库。执行顺序为：核对平台版本/登记/物理连接 → 取得租户迁移锁 → schema_status=UPGRADING → Flyway V1/V2 → 绑定物理库归属 → 在独立事务中建立首位管理员、部门、admin 角色及全部公共关联 → 校验版本、结构、管理员 → READY / ACTIVE。

首位管理员预留 ID 为 80001，仅用于新空库；用户名由部署方指定。失败重试不重置已成功创建的账号密码，也不把既有普通账号自动提升为管理员。已开通后再次执行 `new` 会拒绝，后续使用 `upgrade`。

## 已有业务库接管

先备份并停止旧应用，确保原 Flyway 不再同时管理该库。将原数据库注册到平台，但先保持 PROVISIONING/UPGRADING；连接配置直接指向原库，不复制或重写业务 ID。然后执行：

```bash
python3 sql/tenant/run.py adopt /secure/tenancy.properties enterprise-a
```

入口严格对照 `tenant-v1.json` 的 39 张表结构，包括列、索引、默认值、注释、字符集与约束；排除原 Flyway 历史表和自增当前值，不读取/清空业务行。额外视图、触发器、存储过程、事件或未知差异都会拒绝。不同 MySQL 版本的结构输出若不一致，也先人工核对，不能直接放宽校验。

特别核对：`prd_production_picking.warehouse_id`、`pur_in_stock.in_date` 都应可空。旧库尚未完成该修正时，先审阅并在维护窗口执行对应增量；采购入库列的目标定义为：

```sql
ALTER TABLE pur_in_stock MODIFY COLUMN in_date DATETIME NULL COMMENT '实际入库日期（上架后填写）';
```

全部结构匹配且原历史没有失败记录后，显式在新历史表 baseline=1，再执行 V2。原 `flyway_schema_history` 及其 checksum 保留不动，已有业务行、用户密码、禁用和逻辑删除标记不被重置。至少要有一个已有的有效管理员及部门/角色关系；工具不会给旧库补通用开发账号。公共 seed 的自然键/预留菜单 ID 冲突会失败，需人工判断后再试。

接管后只运行租户轨道，禁止再次启动旧单企业迁移器。运行共用服务时仍使用 `tenant-verification` profile，以关闭旧全局迁移器和 AI 自动建表；完成 T4 之前业务入口返回 503。

## 升级、失败与恢复

```bash
python3 sql/tenant/run.py upgrade /secure/tenancy.properties enterprise-a
python3 sql/tenant/run.py verify /secure/tenancy.properties enterprise-a
```

重复升级无待执行版本时不会重跑 seed，不更改账号密码或禁用状态。停用租户迁移成功后仍保持 SUSPENDED，不自动恢复营业。

平台通过 MySQL 会话锁阻止同租户并发维护；Flyway 还维护每库自身的历史和锁。操作记录包含动作、阶段、前后版本、时间与固定错误码，不保存密码/哈希。迁移失败只把当前租户 schema_status 设为 FAILED，新开通租户不会被标为 ACTIVE；其他租户保持可用。版本记录只在全部校验完成后更新。

进程被杀或平台连接中断时，可能留下 UPGRADING/RUNNING，仍不允许服务。下一次取得租户锁后把遗留 RUNNING 标为 INTERRUPTED，再按实际 Flyway 历史重试。MySQL DDL 可能已部分提交：若 Flyway 记录失败或 checksum 不符，先检查失败阶段、备份与结构差异，由部署人员决定恢复或专门修复，再重试；工具不删除失败记录、自动 repair、clean 或恢复快照。

```sql
SELECT t.id,t.status,d.schema_status,d.schema_version
FROM plt_tenant t JOIN plt_tenant_database d ON d.tenant_id=t.id;
SELECT id,tenant_id,action,phase,status,error_code,version_before,version_after
FROM plt_tenant_operation ORDER BY started_at DESC;
-- 在目标租户库查询：
SELECT installed_rank,version,type,checksum,success FROM flyway_tenant_schema_history;
SELECT tenant_id FROM sys_tenant_identity WHERE singleton=1;
```

实际运行仍有两道校验：平台必须为 ACTIVE/READY 且版本匹配；首次建立连接池时物理库归属必须与企业 ID 相同。连接配置写反、旧版本或半初始化库会被拒绝，不回退其他企业。

## 验证与剩余边界

```bash
python3 sql/platform/test_tenants.py
```

测试启动临时 MySQL 并清理，仅操作随机前缀的隔离库。覆盖双库同用户 ID、新库与旧库、独立历史、公共 seed、密码/数据保留、未知 schema 与 checksum 失败、并发锁、中断重试、错误目标/物理归属、停用保留、外部配置与运行时隔离。没有指定测试数据库而跳过集成类，不能算验收通过。依赖已缓存时，部署命令支持 `--offline`。

本轮实测：39 项测试通过、无跳过，其中 16 项为真实 MySQL/Flyway 生命周期测试。干净副本仅叠加本轮改动后，测试与可执行 JAR 打包均通过；默认启动类仍是原应用。另实际执行平台初始化及租户 new/verify/upgrade 命令，带空格的外部配置路径也通过。没有访问或迁移现有业务库。

尚未实现：自动分配数据库/账号、平台运维认证/UI、在线排流、自动备份恢复及连接池热轮换。下一步是 T4 全链路隔离，多租户业务门禁继续保留。
