# T1/T2 多租户最小验证

本阶段实现租户登记基础、身份与数据库路由，不是可上线的完整 SaaS。完整方向见 [多租户方案](multi-tenancy.md)。

## 已实现

- 独立平台数据源与两张登记表，记录企业编码、启停状态、连接配置引用、路由版本及 schema 就绪状态；使用参数化查询，不经过业务 Mapper。
- `TenantScope` 必须在事务开始前建立，同租户可嵌套，不同租户不得嵌套；关闭后恢复上下文，线程池不隐式继承。
- 使用 Spring AbstractDataSource 显式路由，不动态改写全局路由映射；租户库连接池按需建立、有数量与单池上限，无默认目标；平台和各配置连接不能使用相同主机/端口/库名组合。该校验不能识别不同 DNS 别名实际指向同一数据库，部署时仍需核对。
- 数据库地址和凭据从服务端外部配置读取，平台表只保存 connection_ref；禁止客户端指定数据源或请求级数据库凭据。
- 登录支持 tenantCode，JWT 包含 tenantId、版本和 tenant-api 用途；请求按已验签身份确定租户，冲突的 X-Tenant-ID 返回 403。
- 区分 401 无效登录、403 停用/冲突、503 平台或租户库暂时不可用，不回退其他企业。
- 单企业默认兼容原有账号密码登录；旧无企业 Token 只在 single 且 allow-legacy-tokens=true 时接受。平台用途 Token 不作为旧业务 Token 接受。
- 多租户模式只开放登录、企业解析及测试专用路径；菜单、采购生产库存、AI、WS 等正式业务接口均返回 503。权限缓存不会进入验证链路。

## 明确尚未实现

自动建库、完整开通重试状态机、平台运维登录/UI、逐库迁移、连接池闲置回收/在线轮换、权限和业务缓存隔离、通知与异步隔离、双端企业登录界面仍属于后续阶段。
当前登记 ACTIVE/READY 是对已经准备好的库的人工确认，不代表程序已经校验 schema 与所有迁移历史。routing_version 改变后需受控重启；本阶段不关闭活跃池进行热替换。

## 配置与手工登记

默认 `application.yaml` 使用 single 模式，无需平台库。新签发的 Token 会绑定配置中的企业 ID；验证旧 Token 的兼容开关可通过 `TENANCY_ALLOW_LEGACY_TOKENS=false` 关闭。

多租户验证需显式启用 `tenant-verification` profile。它关闭 Flyway、SQL init、AI JDBC 记忆自动配置和建表；AI 记忆使用拒绝访问的实现，避免后台任务误用全局数据。

在独立平台库执行 `sql/platform/01_tenant_registry.sql`，业务库需事先准备；**不能把当前固定 smart-erp 库名的全部 SQL 循环导入其他库**。T3 会提供无限定库名的初始化轨道。

外部配置示意（不要提交真实凭据）：

```yaml
tenancy:
  platform:
    url: jdbc:mysql://db-host:3306/erp_platform
    username: ${PLATFORM_DB_USER}
    password: ${PLATFORM_DB_PASSWORD}
  connections:
    enterprise_a:
      url: jdbc:mysql://db-host:3306/erp_a
      username: ${TENANT_A_DB_USER}
      password: ${TENANT_A_DB_PASSWORD}
    enterprise_b:
      url: jdbc:mysql://db-host:3306/erp_b
      username: ${TENANT_B_DB_USER}
      password: ${TENANT_B_DB_PASSWORD}
```

每个业务账号只授权自己的数据库。配置键不能从请求拼接而来。人工核对两个已初始化的库后，在平台库登记：

```sql
START TRANSACTION;
INSERT INTO plt_tenant(id,code,name,status) VALUES
 ('enterprise-a','company-a','企业 A','PROVISIONING'),
 ('enterprise-b','company-b','企业 B','PROVISIONING');
INSERT INTO plt_tenant_database(tenant_id,connection_ref,schema_status,schema_version) VALUES
 ('enterprise-a','enterprise_a','READY','人工核对的版本'),
 ('enterprise-b','enterprise_b','READY','人工核对的版本');
UPDATE plt_tenant SET status='ACTIVE' WHERE id IN ('enterprise-a','enterprise-b');
COMMIT;
```

通过 `SPRING_CONFIG_ADDITIONAL_LOCATION` 加载外部文件，再启用 profile：

```bash
SPRING_PROFILES_ACTIVE=tenant-verification ./mvnw spring-boot:run
```

`POST /auth/tenant/resolve` 接受 `{"tenantCode":"company-a"}`，仅返回企业编码和名称。
`POST /auth/login` 接受 `tenantCode + username + password`。当前没有可供业务人员使用的多租户正式业务页；验证接口 Controller 仅存在于测试源码，不会被打进生产包。

## 可重复的隔离回归

```bash
python3 sql/platform/test_tenants.py
```

需要 Docker、JDK 21 和项目依赖。脚本启动临时 MySQL 8.4，设置 `TENANT_TEST_JDBC_BASE` 后运行测试，结束即删除容器。测试每次使用唯一库名前缀，创建一个平台库和两个租户库，并只清理自己创建的库。

测试从真实 LoginController/LoginService、MyBatis UserMapper、JWT Filter 与 JDBC 事务走通 HTTP 模拟请求。测试 UserService 用薄适配调用实际 Mapper；不读取 Redis 权限缓存，不调用模型服务。它验证的是 T2 链路，不代替完整 Spring Security/业务端到端测试。

覆盖：两企业相同用户 ID=80001 与相同用户名登录；各库读取与写入隔离；伪造企业头、旧 Token、缺少 Token 被拒绝；停用/连接版本变化不影响另一企业；缺失上下文拒绝连库；事务内禁止切租户；公共解析不泄露连接；正式业务/AI/WS 门禁。

本机 JDK 若禁止 Mockito 自附加，可向脚本传入本机匹配版本的 `-DargLine=-javaagent:/path/to/mockito-core.jar`；不要修改测试使其回退业务数据库。没有设置测试数据库环境变量时，MySQL 集成类会明确跳过，不能把跳过当成隔离验收通过。

## 本轮验证与下一步

22 项 JWT/上下文/单企业兼容/安全响应测试和真实 MySQL 双库 MockMvc 测试通过，无跳过。回归同时包含原有全局异常处理器，验证企业异常不被转成 HTTP 200。另使用独立平台库和 Redis 启动实际应用，核验双数据源装配及验证模式门禁，不访问现有业务数据。

从当前 HEAD 创建干净副本，仅加入本轮多租户改动后再次执行上述 22 项测试，全部通过；不依赖工作区内其他未提交修改。

下一步 T3：冻结目标 schema，完成租户 V1 基线与逐库迁移，再做 T4 Redis、事件、AI、WS 与异步上下文隔离。全链路隔离完成之前，不允许移除多租户业务门禁。
