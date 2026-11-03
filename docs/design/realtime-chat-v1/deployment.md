# 员工聊天 V1 · 实现、部署与联调

状态：后端代码已实现，并在独立测试容器中验证。默认关闭；未连接或修改本机现有业务数据，未部署到线上。前端接入使用 [API 文档](api.md) 和 [OpenAPI](openapi.json)。

## 已实现

- 同组织联系人、唯一单聊、幂等建群；文本、图片及办公附件消息。
- 群主/管理员/成员权限、添加/移除、任免管理员、转让、退群及只读解散。
- 发送幂等、会话序号、本人未读/已读、多设备事件、固定会话快照、7天补拉。
- 一次性连接票据、Origin白名单、心跳、跨节点连接租约、慢消费者关闭。
- MySQL事务Outbox、Redis跨节点唤醒、失败重试和持久事件恢复。
- 官方S3 SDK签名直传、50,000,000字节上限、固定对象版本、Tika内容识别、ClamAV扫描、鉴权及Range下载、未引用文件清理。

实现位于 `src/main/java/org/smart/erp/im/`。持久化使用已有Spring JDBC事务与显式SQL行锁；不新增数据库访问框架。IM ID由MySQL生成，并以字符串输出，避免集群使用同一默认雪花节点号产生冲突。

## 1. 数据库升级

先按现有部署流程备份并选择已有轨道，不对同一业务库混用两条历史。

| 现有环境 | 新增迁移 | 开启方式 |
| --- | --- | --- |
| single 原有增量迁移轨道 | `db/migration/V20261103__employee_realtime_chat.sql` | 随原Spring Flyway配置执行；不要手工修改已执行迁移 |
| 租户逐库运维轨道 | `db/tenant-migration/V4__employee_realtime_chat.sql` | 使用现有 `TenantMigrationCommand` 的UPGRADE；目标schema版本由3升到4 |

两份IM DDL相同，测试检查一致性。新增11张IM表及3个权限：`im:access`、`im:group:create`、`im:file:upload`。已有启用的admin角色获得新增权限；其他员工由管理员通过已有角色权限功能分配。角色权限不授予非成员聊天读取权。

逐库轨道同时新增冻结的 `db/schema-contract/tenant-v4.json`；V1～V3及旧契约不修改。已验证空库初始化、旧V3升级、重复升级及账号密码保留。已有multi业务门禁继续关闭，升级schema不会自动开放multi聊天或其他业务。

## 2. 开启文字聊天

复用当前MySQL和Redis连接配置。聊天使用Redis原子GETDEL；本轮测试Redis 7.4。按 [配置样例](application-im.example.yaml) 创建部署配置，设置 `im.enabled=true`，并在 `allowed-origins` 填写真实网页Origin，包含协议和端口，不包含路径/`#`。原生Flutter连接允许不带Origin，仍必须有票据。

```bash
java -jar target/origin-erp-service-0.0.1-SNAPSHOT.jar \
  --spring.config.additional-location=file:/absolute/path/application-im.yaml
```

API路径仍由原网关决定是否加 `/api`；WebSocket相应映射 `/ws/chat`。代理需支持协议升级、WSS，空闲超时大于90秒，并对ticket查询参数脱敏。文本消息不能改用旧 `/ws/notification`。

默认群200人、每用户5条/秒、最多5个连接、同时3个未发送附件、组织附件预约容量20,000,000,000字节。可通过样例配置收紧上限；客户端读取 `/im/config`，不写死配置。固定快照首版最多1000个会话/用户，超限返回503；扩大这一限制需要分页快照和容量设计。

## 3. 启用文件传输

配置 `im.storage.enabled=true`、私有桶、region、可选endpoint及凭据，同时配置ClamAV服务地址。未配置扫描服务时上传预约返回503；扫描暂时不可用时保持VERIFYING，后台退避重试，绝不跳过扫描标成READY。

存储部署要求：

1. 私有桶启用版本控制；授权服务账号签名PUT、HEAD、读取固定版本、列举及删除未引用版本。
2. 签名绑定准确Content-Length和SHA-256 checksum。API返回 `requiredHeaders`；原生客户端按返回值设置，浏览器自动生成Content-Length，文件字节长度必须一致。不要自行发送Host或把应用JWT发给存储服务。
3. 桶CORS允许真实前端Origin的PUT及签名要求的Content-Type/checksum头；预检需通过。endpoint必须是客户端也能访问的外部地址。
4. 清理程序只保留消息绑定版本，不配置会删除这些历史版本的全桶非当前版本过期规则；未引用对象由应用清理。生产环境验证供应商版本/签名/校验行为后再开放附件。

上传预约24小时，签名通常5分钟；响应提供SDK实际签名到期时间。取消后等最后签名过期及物理清理成功才释放容量。READY仍须发送FILE/IMAGE消息才对成员开放下载；每个附件只能绑定一条消息。

类型由Tika检测内容后确定，客户端mime仅是上传声明。V1拒绝宏文件、可执行文件、脚本、嵌套压缩包及无法读取的加密压缩包；压缩包最多1000项、展开100,000,000字节及10秒检查时间。多份文件逐条发送。下载按当前成员及本次入群窗口鉴权；退群前已经下载的文件无法远程收回。

ClamAV运行在仅应用可访问的内部网络，启用INSTREAM，持续更新病毒库。按安装版本核对 [官方配置样例](https://github.com/Cisco-Talos/clamav/blob/main/etc/clamd.conf.sample)，至少确保 `StreamMaxLength` 与 `MaxFileSize` 不小于50,000,000字节、`MaxScanSize`覆盖允许的展开容量，并启用 `AlertExceedsMax`、`AlertEncrypted`，避免扫描器把跳过扫描的文件当成通过。应用设有总扫描超时，异常返回维持VERIFYING。

## 4. 验证证据与边界

本轮后端回归146项通过，0失败、0跳过（未包含现有独立Python/浏览器 `HttpE2EHarnessTest`）。包括20项新增聊天测试，以及原业务、通知、租户隔离和迁移回归。测试均使用独立容器，结束后销毁：MySQL 8.4、Redis 7.4、S3兼容服务。

重点验证：16路并发单聊/同ID重发只落一条，12路消息序号连续；新增成员不可读旧历史，移除后不能下载，重新入群不恢复旧窗口；真实JWT/HTTP/WS及Outbox推送；50,000,000字节成功、超出1字节拒绝；真实Office/PNG识别、固定版本下载、Range、取消与扫描并发、未引用版本/容量清理。

ClamAV在自动化测试中使用INSTREAM协议替身，检查通过、发现病毒、阻塞/取消等调用路径；**未把它当作真实病毒库的检出率验收**。生产扫描服务和所选存储的浏览器CORS仍需部署联调。1000连接/20条每秒等设计容量基线尚未压测，不能作为线上性能承诺。原生系统后台离线推送、前端聊天页面、跨组织、音视频、撤回及编辑不在本轮后端交付范围。

可重跑：

```bash
./mvnw -Dtest=ImIntegrationTests,ImHttpWebSocketTests test
./mvnw -Dtest='!HttpE2EHarnessTest' test
```

第一条运行聊天容器测试；第二条运行后端回归。原租户迁移/跨库回归还需现有 `TENANT_TEST_JDBC_BASE` 和 `TENANT_TEST_REDIS_PORT` 环境变量指向可销毁的测试实例；本轮已显式配置并运行，未用开发业务库替代。

## 5. 运行维护

- 监控IM后台错误日志、`im_outbox`的FAILED/PENDING数量及最早待投递时间、`im_upload`的VERIFYING时间和 `im_storage_budget.reserved_bytes`。Outbox十次失败进入FAILED，仍可通过REST补拉；排除Redis故障后，由维护人员重置失败任务至PENDING并清空租约重新投递。
- 事件只保留7天，游标过期返回410；前端重建固定快照和历史。已发送消息及引用文件不自动删除，上线前按组织确认保留期和容量策略。
- 关闭功能设置 `im.enabled=false`；不会删除历史数据或回退schema。既有连接在最长10秒复核后关闭。
