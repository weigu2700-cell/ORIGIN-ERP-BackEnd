# 员工实时聊天 V1 · API 与实时协议

**状态：后端首版已实现，默认关闭；开启与依赖配置见 [部署说明](deployment.md)。** 字段及校验的机器版本见 [OpenAPI](openapi.json)，业务边界见 [方案](design.md)。

## 1. 通用约定

- 路径为服务端应用内路径。部署若有 `/api` 前缀，由网关/客户端 baseURL 添加，不在每条路径重复。
- 所有应用内REST（包括文件下载）均使用 `Authorization: Bearer <JWT>`；对象存储直传PUT使用签名URL及返回的请求头，不携带应用JWT。JSON使用UTF-8；tenantId、senderId、uploaderId由服务端身份取得，客户端不得指定。
- ID/seq/eventSeq均为十进制字符串，客户端不得转换为浮点数。幂等ID为客户端生成UUID，重试不换ID。
- 时间采用带时区RFC3339 UTC，例如 `2026-11-03T06:00:00Z`，前端按本地时区显示。
- 请求DTO拒绝未知字段。普通成功HTTP200并沿用 `{code:200,msg:"请求成功",data:...}`；新建也使用200以保持项目习惯。
- 文件 PUT 直传由对象存储返回原生状态；文件下载为二进制200/206，不包JSON。
- `/im/config` 提供最终生效限额；文档默认值不是让客户端写死。

```json
{"code":200,"msg":"请求成功","data":{"conversationId":"110001"}}
```

IM领域错误保留相同包裹，HTTP与code一致：

```json
{"code":409,"msg":"相同发送标识对应不同内容","data":{"error":"IDEMPOTENCY_CONFLICT","requestId":"req-uuid","details":{}}}
```

共享JWT过滤器/网关可能返回 `data:null`，前端仍须处理通用HTTP401/403/503，不假定每个错误都有 `data.error`。基础对象详见OpenAPI；以下ID为示例。

## 2. 推荐接入流程

1. 登录后检查组织业务开放与 `im:access`；调用config、contacts。
2. 调用会话列表取得首个固定快照和 `syncEventSeq`，用 `snapshotToken + cursor` 取完后续分页。分页条目仍校验最新访问权。
3. POST `ws-tickets`，连接返回的相对WS路径（https→wss）；拿到 `SESSION.READY` 后立即用快照水位补拉事件。
4. WebSocket和REST补拉统一按eventId去重、按eventSeq排序推进本机已处理水位；断线前收到的序号不因重连归零。
5. 发消息走REST，HTTP确认后合并临时气泡；之后再收到WS事件不重复添加。发送响应里的senderEventSeq不能直接覆盖尚有缺口的同步游标。
6. 弱网结果未知：用原clientMessageId查询结果；未查到才以同一ID/同一正文重试。不能生成新UUID盲目重发。
7. WS断开重新取票，连接后补拉。前台每30秒检查/补拉水位，保证最后一条WS丢失也能恢复。收到410则重建固定快照；切组织/退出清空旧流、缓存和待确认操作。

## 3. 配置、联系人与会话

| 方法与路径 | 请求要点 | data |
| --- | --- | --- |
| GET `/im/config` | 无 | maxFileBytes、maxGroupMembers、textMaxChars、allowedExtensions等 |
| GET `/im/contacts` | keyword可选，cursor可选，limit默认20/最大100 | `records:[{userId,displayName,departmentName,avatarUrl}],nextCursor,hasMore` |
| POST `/im/conversations/direct` | `{peerUserId}`，不能是自己 | Conversation；并发重复创建返回同一会话 |
| GET `/im/conversations` | 首次不带cursor；后续带snapshotToken+cursor；limit最大100 | ConversationSnapshot，见下文 |
| GET `/im/conversations/{id}` | 当前成员；解散时保留的成员可只读 | Conversation |
| PUT `/im/conversations/{id}/preferences` | `{muted:true}` | Preference；仅改本人设置 |

联系人接口不要求系统用户管理权限，也不返回手机号、权限列表或凭据。只有账号启用、具备IM使用资格的同组织员工可选。

Conversation：

```json
{
  "id":"110001","type":"GROUP","name":"装配一组","status":"ACTIVE",
  "ownerId":"80001","version":3,"memberCount":3,"myRole":"OWNER",
  "visibleFromSeq":"1","lastSeq":"42","lastReadSeq":"39","unreadCount":2,
  "muted":false,"lastMessage":{"id":"900042","conversationId":"110001","seq":"42","senderId":"80002","clientMessageId":"e91e6db3-fbf5-47d9-8931-fdbd46d2a3d2","type":"TEXT","text":"工单资料已补齐","file":null,"system":null,"createdAt":"2026-11-03T06:00:00Z"},
  "updatedAt":"2026-11-03T06:00:00Z"
}
```

单聊的 `name` 为对方显示名，ownerId/myRole为null；version为1起。ACTIVE/DISSOLVED均可在列表中出现；已退群/被移除者不再返回该会话。头像缺失可为null；不为每条消息承诺在线状态。

快照返回：

```json
{"records":[],"snapshotToken":"opaque-token","nextCursor":null,"hasMore":false,"syncEventSeq":"1842","totalUnreadCount":7,"expiresAt":"2026-11-03T06:10:00Z"}
```

`syncEventSeq`在整个快照分页中固定。处理完快照后从此水位补拉；取新快照时会话/偏好/已读也同步。首版每用户快照最多1000个会话，超过则503，需先做容量扩展。静音只影响提醒意愿，不取消事件或未读累计。cursor不允许跨用户/组织或换snapshotToken使用。

## 4. 消息、历史与已读

| 方法与路径 | 请求 | data |
| --- | --- | --- |
| POST `/im/conversations/{id}/messages` | SendMessage，见下文 | `{message,deduplicated,senderEventSeq}` |
| GET `/im/conversations/{id}/messages` | beforeSeq或afterSeq，二者互斥；都不传为最近一页；limit最大100 | `{records,hasMore,nextSeq,direction,latestSeq}` |
| GET `/im/messages/by-client-id` | clientMessageId必填，当前发送者范围 | Message；未提交404 |
| PUT `/im/conversations/{id}/read` | `{throughSeq:"42"}` | `{conversationId,lastReadSeq,unreadCount}` |

三种发送请求：

```json
{"clientMessageId":"e91e6db3-fbf5-47d9-8931-fdbd46d2a3d2","type":"TEXT","text":"请核对附件中的排产资料"}
```

```json
{"clientMessageId":"f43faef5-d1e4-4472-b741-b3bdf36a9230","type":"FILE","fileId":"700001"}
```

```json
{"clientMessageId":"c50e44f6-fcb4-46b3-b8cc-c9533838b42e","type":"IMAGE","fileId":"700002"}
```

TEXT要求非空白文字，最多4000 Unicode字符且UTF-8最多16KiB；不允许同时带fileId。FILE/IMAGE发送还需要im:file:upload资格；只带fileId，不接受文件URL或对象key；IMAGE要求已校验的图片MIME，消息发送成功后附件只能绑定这条消息。SYSTEM只能由服务端生成，不允许客户端发送。

消息响应含发送者ID、clientMessageId、type、text/file/system、createdAt；空分支为null。SYSTEM结构为 `{action,actorId,targetUserIds,description}`；action仅为服务端枚举：GROUP_CREATED、MEMBERS_ADDED、MEMBER_REMOVED、ROLE_CHANGED、OWNER_TRANSFERRED、MEMBER_LEFT、GROUP_DISSOLVED、GROUP_RENAMED。文件对象为 `{id,name,size,mime,sha256}`，不返回桶名或对象key。

历史records始终按seq升序：beforeSeq返回小于该值的最近limit条，nextSeq是本页最小seq；afterSeq返回大于该值的最早limit条，nextSeq是本页最大seq。无数据nextSeq为null。`direction`为BACKWARD/FORWARD；最近一页采用BACKWARD。所有数据受visibleFromSeq限制，latestSeq是当前可见的最新提交序号。

已读水位只增不减，不能标到未来序号；解散群允许推进已读。本人消息与SYSTEM不计未读；seq差不等于未读条数。已读更新向本人其他设备发 `READ.UPDATED`，V1不提供群内逐人已读名单。

## 5. 建群与群管理

| 方法与路径 | 请求 | 结果与权限 |
| --- | --- | --- |
| POST `/im/groups` | `{clientRequestId,name,memberIds}`，name 1～64字符，memberIds 2～199名其他员工 | Conversation；创建者OWNER；需要im:group:create |
| PUT `/im/groups/{id}` | `{name,expectedVersion}` | Conversation；OWNER/ADMIN |
| GET `/im/groups/{id}/members` | cursor可选、limit最大100；翻页须携带首响应groupVersion | `{records,nextCursor,hasMore,groupVersion}` |
| POST `/im/groups/{id}/members` | `{memberIds,expectedVersion}`，1～50人 | Conversation；OWNER/ADMIN；全成或全败 |
| DELETE `/im/groups/{id}/members/{userId}` | query expectedVersion必填 | Conversation；OWNER可移除非本人；ADMIN只能移除MEMBER |
| PUT `/im/groups/{id}/members/{userId}/role` | `{role:"ADMIN",expectedVersion}`，role仅ADMIN/MEMBER | Conversation；OWNER |
| POST `/im/groups/{id}/transfer` | `{newOwnerId,expectedVersion}` | Conversation；OWNER，目标须为当前成员 |
| POST `/im/groups/{id}/leave` | `{expectedVersion}` | `{conversationId,version}`；非OWNER |
| POST `/im/groups/{id}/dissolve` | `{expectedVersion}` | Conversation，status=DISSOLVED；OWNER |

建群clientRequestId在当前组织当前用户下幂等，相同键不同名称或成员集合409；其他群变更依靠expectedVersion防重复和并发覆盖。成员分页在版本变化时409，须从第一页重取，不能混合不同成员版本。

成员记录：`{userId,displayName,departmentName,avatarUrl,role,joinedAt}`。添加已在群内的成员返回409/MEMBER_ALREADY_PRESENT，失败不部分添加。拉人前再次检查目标当前启用/IM权限/同组织；不能仅相信联系人列表中曾经出现过。

## 6. 附件接口

| 方法与路径 | 请求 | data |
| --- | --- | --- |
| POST `/im/uploads` | `{clientUploadId,conversationId,name,size,mime,sha256}` | UploadReservation |
| POST `/im/uploads/{id}/renew` | 无body；仅上传者，未完成且预约未过期 | 新UploadReservation，不新建附件 |
| POST `/im/uploads/{id}/complete` | 无body；上传者且仍有会话权限 | UploadStatus，一般先VERIFYING |
| GET `/im/uploads/{id}` | 上传者本人且仍有会话权限 | UploadStatus |
| DELETE `/im/uploads/{id}` | 取消本人未发送的附件；已发送409 | UploadStatus，status=CANCELLED；重复取消幂等 |
| GET `/im/files/{id}/content` | 需要JWT；可选单段Range | 文件流200/206，拒绝多段Range；非法范围416 |

预约示例：

```json
{"clientUploadId":"33b95964-124e-4f54-9638-b89916818d06","conversationId":"110001","name":"装配排产.xlsx","size":24681,"mime":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}
```

响应（此URL仅为示例）：

```json
{"uploadId":"600001","method":"PUT","uploadUrl":"https://storage.example.invalid/private-object?signature=example","requiredHeaders":{"Content-Type":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},"expiresAt":"2026-11-03T06:05:00Z","reservationExpiresAt":"2026-11-04T06:00:00Z","maxFileBytes":50000000}
```

上传PUT必须采用返回的头和签名URL；原生端设置准确Content-Length，浏览器由其自动按文件字节生成，不手动设置Host。签名也绑定SHA-256校验值；URL不得写入普通日志或随聊天消息发送。签名过期可renew；POST同clientUploadId且仍为INITIATED返回原预约的新签名，已提交/取消则409/UPLOAD_ALREADY_COMPLETED并查询原uploadId；调用complete后不能再renew，轮询状态即可。若complete发现对象未上传完成返回409/UPLOAD_NOT_COMPLETE，可在上传实际完成后再次complete。

```json
{"uploadId":"600001","status":"READY","file":{"id":"700001","name":"装配排产.xlsx","size":24681,"mime":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"},"rejectionReason":null}
```

状态为 INITIATED/VERIFYING/READY/REJECTED/EXPIRED/CANCELLED；非READY时file为null。后端在校验前固定对象版本；旧PUT签名不能覆盖已验证版本的下载内容。扫描不可用保持VERIFYING，客户端建议2秒、5秒、10秒退避轮询，不自动重复上传/发送。

READY不等于已发送。客户端拿到fileId后单独发FILE/IMAGE消息；成功绑定后当前可见成员才可下载。离群、移除、跨会话、只持有别人的fileId均不能绕过下载权限。上传/校验失败不产生聊天气泡或永久附件占用。

## 7. 实时连接与补偿

### REST

| 方法与路径 | 请求 | data |
| --- | --- | --- |
| POST `/im/ws-tickets` | `{clientId}`，稳定的安装/浏览器实例UUID | `{ticket,expiresAt,websocketPath,heartbeatSeconds,idleTimeoutSeconds}` |
| GET `/im/sync/events` | afterEventSeq必填、limit默认100/最大100 | `{records,nextAfterEventSeq,hasMore,highWatermark}` |

票据60秒有效、只用一次。WebSocket使用服务端返回的相对路径：`/ws/chat?ticket=...`；只能向当前API服务器同源地址建连，不接受客户端任意拼接外部目标。不在URL携带JWT，也不经WS指定tenant/user或订阅任意群ID。

### 建连和心跳

```json
{"type":"SESSION.READY","protocolVersion":1,"connectionId":"conn-uuid","userId":"80001","latestEventSeq":"1842","heartbeatSeconds":30,"idleTimeoutSeconds":90}
```

client→server：`{"type":"PING","nonce":"ping-uuid"}`；server→client：`{"type":"PONG","nonce":"ping-uuid"}`。READY/PONG不是持久事件，不推进游标。其他客户端消息帧返回 `PROTOCOL.ERROR`，连续违规关闭1008。

```json
{
  "protocolVersion":1,"eventId":"300042","eventSeq":"1843","type":"MESSAGE.CREATED",
  "conversationId":"110001","occurredAt":"2026-11-03T06:00:00Z",
  "payload":{"message":{"id":"900042","conversationId":"110001","seq":"42","senderId":"80002","clientMessageId":"e91e6db3-fbf5-47d9-8931-fdbd46d2a3d2","type":"TEXT","text":"工单资料已补齐","file":null,"system":null,"createdAt":"2026-11-03T06:00:00Z"}}
}
```

### 持久事件

| type | payload | 客户端动作 |
| --- | --- | --- |
| MESSAGE.CREATED | `{message}` | 按messageId/clientMessageId合并气泡；按会话seq排列 |
| CONVERSATION.UPDATED | `{conversation}` | 更新标题、状态、摘要与本人未读/偏好 |
| MEMBER.CHANGED | `{action,userId,role,groupVersion}` | 更新/重取群成员；action为ADDED/REMOVED/ROLE_CHANGED/OWNER_TRANSFERRED/LEFT |
| CONVERSATION.REVOKED | `{reason}`，不含历史正文 | 清理会话缓存、下载入口和待发送内容 |
| READ.UPDATED | `{conversationId,lastReadSeq,unreadCount}` | 同步本人其他设备已读 |
| EVENT.SKIPPED | `{reason:"ACCESS_CHANGED"}` | 不呈现正文，只推进游标；重新核对访问权，清理当前已无权访问的缓存，不据此删除重新入群后的有效数据 |

补拉接口与WS持久信封相同。投递/补拉时访问权已撤销的历史事件只返回SKIPPED，不泄露原payload；关闭群的合法只读成员仍可取历史。`eventSeq`按用户流严格递增，收件者不同序号可不同。

有缺口/乱序时先缓存有界事件，再调用sync补齐；不要直接将游标设成收到的最大值。ACK仅本地记录已处理游标，V1无需向服务器报告网络投递ACK。WS frame上限64KiB，文件不从WS传输。

### 关闭码

| 码 | 意义 | 恢复 |
| --- | --- | --- |
| 1000 | 正常关闭/客户端主动退出 | 用户需要时重新连接 |
| 1008 | 协议或权限策略违规 | 停止重试，刷新身份/权限 |
| 1013 | 慢消费者/资源限制 | 退避、重新取票、REST补拉 |
| 4401 | 登录过期 | 重新登录，不沿用旧组织游标 |
| 4403 | 账号/组织停用或IM资格撤销 | 停止发送与重连，解释不可用状态 |
| 4409 | 同clientId被新连接替换 | 当前实例停止抢占式无限重连 |

握手时401/403/429通常是HTTP拒绝，不会产生WS关闭帧；客户端同样需处理连接失败。

## 8. 错误与限制

| HTTP | data.error示例 | 含义/前端恢复 |
| --- | --- | --- |
| 400 | INVALID_ARGUMENT / INVALID_CURSOR | 字段或游标错误，修正请求 |
| 401 | UNAUTHENTICATED / TICKET_INVALID | 重新认证/取票 |
| 403 | IM_NOT_ENABLED / GROUP_ROLE_REQUIRED | 模块资格或群角色不足 |
| 404 | RESOURCE_NOT_FOUND | 不存在、跨组织、非成员、不可见历史、未发送附件；不区分以免枚举 |
| 409 | IDEMPOTENCY_CONFLICT / VERSION_CONFLICT / GROUP_NOT_WRITABLE / OWNER_TRANSFER_REQUIRED / FILE_NOT_READY / FILE_ALREADY_USED / UPLOAD_NOT_COMPLETE / UPLOAD_ALREADY_COMPLETED / UPLOAD_SIZE_MISMATCH | 核对结果/刷新状态；不换幂等键自动重试 |
| 410 | SYNC_CURSOR_EXPIRED / SNAPSHOT_EXPIRED / UPLOAD_EXPIRED | 重建快照或重新预约 |
| 413 | FILE_TOO_LARGE | 文件超过50,000,000字节 |
| 415 | FILE_TYPE_NOT_ALLOWED | 文件类型不支持或不匹配 |
| 416 | INVALID_RANGE | 调整下载范围 |
| 429 | RATE_LIMITED / QUOTA_EXCEEDED | 遵循Retry-After；存储配额由管理员处理 |
| 503 | CHAT_UNAVAILABLE / STORAGE_UNAVAILABLE | 保留输入，恢复后查询结果；现有multi门禁也可503 |

对于写入的网关超时、5xx、断线，先按clientMessageId/clientRequestId核对结果，或刷新群version与成员状态。建群复用同clientRequestId重试得到原会话；群管理刷新确认后再操作。附件上传/complete按同uploadId恢复，不把“返回慢”当作需要重新发一份文件。

## 9. 前端联调验收顺序

两个同组织账号建立单聊 → 三人群 → 相同幂等ID重发 → 多设备与断网补拉 → 未读/已读 → 邀请/移除/转让/退群/解散 → 文件上传/检查/发送/下载 → 越权及跨组织负例。AI助手、系统通知和人际聊天是不同会话类型，不能把同一条聊天内容同步写入三份收件箱。
