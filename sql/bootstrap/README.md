# 首次部署与预置数据（Issue #4）

## 审计结论

- Issue #4 及评论确认缺少 `permission_init.sql` 和 `seed_dev_admin.sql`。
- `92bc094`（2026-09-26，“数据库转储”）的 `sql/smart-erp/*.sql` 全是 DDL，包含关联表结构不代表存在关联数据。
- 没有自动创建管理员的 Runner 或初始化器。登录通过 `PasswordEncoder.matches` 校验 BCrypt；JWT 过滤器从数据库加载权限，并没有 `*` 通配权限。
- 当前 `RoleConverter` 用角色编码 `admin` 识别超级管理员；角色、用户和部门均需有效。菜单要求 `status=1, visible=1`，且组件路径必须匹配前端。
- 没有 `sys_config` / 参数字典表或必须存在的参数记录。数据库、Redis、JWT、AI 参数来自 `application.yaml` / 环境变量，SQL 无法替代这些配置。
- 通知使用 `eip_notification*`。当前业务发布逻辑可直接传标题和正文，不强制要求固定模板；不伪造通知模板、工厂、物料或业务单据。
- `sql/migrations/` 为空；可执行增量迁移位于 `src/main/resources/db/migration/`。Flyway 不负责空库建表。不要导入转储中的 `flyway_schema_history.sql`，否则可能干扰基线；由 Flyway 自己创建记录。

## 空库开发部署

安装 Python 3、MySQL 8 客户端，并启动 MySQL 8、Redis、JDK 21。命令从仓库根目录执行（脚本也支持任意工作目录）：

```bash
python3 sql/bootstrap/run.py schema -h127.0.0.1 -uroot -p
python3 sql/bootstrap/run.py seed-dev -h127.0.0.1 -uroot -p
python3 sql/bootstrap/run.py verify -h127.0.0.1 -uroot -p
```

`schema` 严格按 `01_schema.sql` 清单导入 39 张应用表，排除 Flyway 内部历史表；DDL 仅用于空库，出错应检查库状态，不能直接继续。
`seed-dev` 依次执行权限、菜单、开发管理员。入口展开 SOURCE 后交给 mysql 批处理，任一 SQL 失败立即退出；每个 seed 文件使用独立事务，重跑可补齐前一文件已提交之后的内容。禁止传 `--force`。

默认账号：`admin` / `OriginDev@123`。管理员角色编码 `admin`，部门编码 `DEFAULT`，用户/角色/部门预留 ID `80001`，菜单预留 `81000..81999`。关联按实际自然键查询 ID，不要求旧库 ID 相同。发生预留 ID 或唯一名称冲突会报错，先检查并调整 seed，不覆盖已有记录。

权限和菜单只新增缺失记录；不重置密码、不恢复被禁用或逻辑删除的记录。若旧库存在同名账号，必须使用其已有密码。脚本会将有效同名 `admin` 账号关联管理员角色，因此只能在管理员确认需要初始化的环境中手动执行。停服、单线程执行，已有数据先备份；这些脚本不是并发升级机制。

生产部署执行 schema 和 `permission_init.sql`、`menu_init.sql`，然后按部署流程使用自己的 BCrypt 密码建立首位管理员及角色关系；不要执行开发账号脚本。应用不会自动运行开发 seed。

数据库名固定为 `smart-erp`，DDL 和现有生产领料迁移均含该限定名；只改 JDBC 数据库名不能完成改名。默认无需修改。

## 配置与启动顺序

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/smart-erp?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
export SPRING_DATASOURCE_USERNAME='<database-user>'
export SPRING_DATASOURCE_PASSWORD='<database-password>'
export SPRING_DATA_REDIS_HOST=127.0.0.1
export SPRING_DATA_REDIS_PORT=6379
export JWT_SECRET='<openssl rand -base64 32 生成并保存的值>'
export AI_API_KEY='<model-api-key>'
./mvnw spring-boot:run
```

AI_API_KEY 是当前配置的必填环境变量；仅验收启动和登录时可用非空占位值，但 AI 调用需要真实凭据。Redis 不写 SQL 预置数据，单号和缓存按需生成。共享环境覆盖仓库中的开发配置。

首次启动 Flyway 对已有表打 version=1 基线，再运行 `V20260921`、`V20261101`；无需手工执行这两份 SQL。历史库存在 Flyway 记录时保留它并让 Flyway 校验，不能删除或伪造历史来跳过校验。

重跑 seed 若复用 Redis，须在应用停止时删除受影响账号的两个缓存键（账号 ID 用实际值替换）：

```bash
redis-cli DEL erp:auth:permissions:v2:80001 erp:auth:menu:v2:80001
```

缓存默认最长 2 小时；重启应用本身不会清空 Redis。不要清空整个共享 Redis。

## 验收

1. `verify` 应返回全部 `ok=1`，失败退出非零；它校验管理员关系、BCrypt 格式、菜单/权限数量和关联完整性。精确权限码覆盖与空库/重跑回归由 `test_seed.py` 检查。
2. 登录：

   ```bash
   curl -sS http://localhost:8080/auth/login -H 'Content-Type: application/json' \
     -d '{"username":"admin","password":"OriginDev@123"}'
   ```

   响应 `code=200`，取 `data.token`，作为 `Authorization: Bearer <token>` 请求
   `GET /system/user/current`、`GET /system/permission`、`GET /system/menu/current`。
   权限应含 `master:material:list` 等精确码，菜单应含系统、基础资料、销售、库存、生产、采购和协同目录。
3. 用相同 Token 请求 `GET /master/material?page=1&pageSize=10`，空业务库应成功返回空列表，不应 403；错误密码应登录失败。
4. 修改管理员密码后重跑 `seed-dev`，应仍使用修改后的密码。业务资料为空是正常状态，后续通过界面录入。

隔离回归（需要 Docker；自动创建并清理临时 MySQL 8.4 容器，不连接开发业务库）：

```bash
python3 sql/bootstrap/test_seed.py
```

## 本次实测（2026-11-02）

在独立 MySQL 8.4 和 Redis 7.4 中完成验证：39 张基础表导入成功，117 个权限码与当前源码完全一致，37 个菜单的组件均存在于配套前端。重复执行无重复，改密/禁用/逻辑删除不会被重置，不同旧 ID 的关联测试通过。

使用当前工作区后端实际启动，Flyway 自动创建 version=1 基线并成功执行 20260921、20261101。默认账号登录、当前用户、117 个权限、8 个顶层菜单（含 AI）和物料空列表均返回成功；错误密码被拒绝。验证只访问隔离库和缓存，没有更改现有业务数据。
