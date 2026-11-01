# API 接口规范

> 文档版本：v0.1（规划稿）
> 网关统一入口：`http://{host}:8080`；所有外部接口以 `/api/v1` 开头；内部接口以 `/internal/v1` 开头且**不经网关暴露**。
> 数据库设计见 [database.md](./database.md)，状态枚举见 [requirements.md](./requirements.md)。

---

## 1. 通用约定

### 1.1 请求头

| Header | 必填 | 说明 |
| --- | --- | --- |
| `Authorization` | 除白名单外必填 | `Bearer {token}` |
| `Content-Type` | 是 | JSON：`application/json`；上传：`multipart/form-data` |
| `X-Trace-Id` | 否 | 客户端可传，网关无则生成；响应回传 |
| `X-Idempotency-Key` | 否 | 提交类接口幂等键（UUID），Redis 24h 去重 |
| `X-Client` | 否 | `miniprogram` / `admin-web`，用于日志区分 |

### 1.2 统一响应

```json
{
  "code": 0,
  "message": "OK",
  "data": {},
  "traceId": "8f2c1d9a4b7e4c3f",
  "timestamp": "2026-10-21 18:40:00"
}
```

- `code = 0` 表示成功，其余见错误码表。
- HTTP 状态码：业务异常统一返回 HTTP 200 + 业务 `code`（便于前端统一处理）；认证失败返回 401，限流返回 429，网关/服务不可用返回 503。**网关层异常例外**：鉴权失败 401、限流 429。
- `data` 为空时返回 `null`；列表无数据返回 `[]`。
- 敏感字段脱敏：手机号 `138****1234`，openid 不返回前端。

### 1.3 分页

请求：`page`（默认 1）、`size`（默认 10，最大 100）。

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "records": [],
    "total": 0,
    "page": 1,
    "size": 10,
    "pages": 0
  }
}
```

### 1.4 时间与坐标

- 时间：`yyyy-MM-dd HH:mm:ss`，时区 `Asia/Shanghai`。
- 坐标：GCJ-02，`longitude`、`latitude` 为 `DECIMAL(10,6)`，请求体使用 number（最多 6 位小数）。

### 1.5 错误码表

| code | HTTP | 含义 | 触发场景 |
| --- | --- | --- | --- |
| 0 | 200 | 成功 | — |
| 40001 | 200 | 参数校验失败 | 字段缺失/格式错误/长度超限，`data` 含字段错误列表 |
| 40002 | 200 | 请求体格式错误 | JSON 解析失败 |
| 40101 | 401 | 未登录或 Token 失效 | 网关校验失败 |
| 40102 | 200 | 账号或密码错误 | 登录失败 |
| 40103 | 200 | 账号已禁用 | 状态 `DISABLED` |
| 40104 | 200 | 微信登录失败 | code2session 失败/凭据未配置 |
| 40105 | 200 | 微信绑定凭证失效 | bindTicket 过期或已使用 |
| 40301 | 200 | 无权限 | 角色不符 |
| 40302 | 200 | 资源归属不符 | 操作他人事件/工单 |
| 40303 | 200 | 内部调用凭证无效 | `/internal/**` 校验失败 |
| 40401 | 200 | 资源不存在 | — |
| 40901 | 200 | 状态不允许该操作 | 非法状态迁移 |
| 40902 | 200 | 重复操作/并发冲突 | 工单已被接单、重复审核 |
| 42901 | 429 | 请求过于频繁 | Sentinel 限流 |
| 50001 | 200 | 系统异常 | 未捕获异常 |
| 50301 | 503 | 服务暂不可用 | 服务下线/熔断 |
| 11001 | 200 | 用户已存在 | 注册/建号重复 |
| 11002 | 200 | Token 与用户不匹配 | 会话异常 |
| 11003 | 200 | 微信或账号已绑定 | 微信绑定冲突 |
| 12001 | 200 | 事件不存在 | — |
| 12002 | 200 | 事件状态不允许该操作 | 如已关闭事件再提交 |
| 12003 | 200 | 附件数量/类型不合法 | 照片 0 张或 > 9 张 |
| 13001 | 200 | 工单不存在 | — |
| 13002 | 200 | 工单已被接单 | 并发接单 |
| 13003 | 200 | 工单不属于当前处理人 | 越权操作 |
| 14001 | 200 | AI 服务不可用 | 超时/供应商错误（已降级时用 `degraded=true` 而非报错） |
| 14002 | 200 | AI 输出校验失败 | JSON 无法解析且兜底失败 |
| 15001 | 200 | 地图服务异常 | 腾讯接口错误 |
| 15002 | 200 | 地址解析失败 | 无结果/坐标越界 |
| 16001 | 200 | 文件类型不支持 | 非 jpg/png |
| 16002 | 200 | 文件过大 | > 5MB |

### 1.6 幂等与并发

- 事件提交、接单、验收：服务端以数据库状态条件更新保证幂等（`WHERE status = ?`），冲突返回 `40902`。
- `X-Idempotency-Key` 在 Redis 中 `SETNX`，TTL 24h；同一键的重复请求返回首次响应缓存（仅用于提交类接口）。

---

## 2. 认证与用户（user-service，8081）

### 2.1 微信登录与绑定（小程序）

#### 2.1.1 微信登录

`POST /api/v1/auth/wx-login`

```json
// 请求
{ "code": "wx.login 返回的 code" }
// 响应（已绑定：直接签发 token）
{
  "code": 0,
  "message": "OK",
  "data": {
    "token": "xxxx",
    "userId": 1001,
    "nickname": "张三",
    "avatarUrl": "/uploads/2026/10/22/xxx.png",
    "activeRole": "RESIDENT",
    "roles": ["RESIDENT"],
    "expireSeconds": 7200,
    "needBind": false
  }
}
// 响应（未绑定：返回一次性绑定凭证）
{
  "code": 0,
  "message": "OK",
  "data": { "needBind": true, "bindTicket": "8f3a..." }
}
```

- 服务端调用微信 `code2session` 换 `openid`；已绑定用户直接登录。
- 未绑定用户不自动建号，返回 `bindTicket`（Redis 存 5 分钟、一次性），由前端选择绑定工人账号或注册居民账号。
- `dev` 环境可通过 `WECHAT_MOCK_ENABLED=true` 开启 mock，此时 `code` 以 `mock:` 前缀即可模拟换 openid（生产必须关闭）。

#### 2.1.2 工人绑定已有账号

`POST /api/v1/auth/wx-bind`

```json
{ "bindTicket": "8f3a...", "username": "worker_road", "password": "Worker@123" }
```

- 校验账号密码通过后，把当前微信 openid 写入该账号并签发 token；绑定不取消原密码登录方式。
- 该账号已绑定其他微信、或该微信已绑定其他账号时返回 `11003`。
- 绑定凭证过期返回 `40105`，需重新走微信登录。

#### 2.1.3 居民首次注册

`POST /api/v1/auth/wx-register`

```json
{ "bindTicket": "8f3a...", "nickname": "张三" }
```

- 创建居民账号并绑定 `RESIDENT` 角色；`nickname` 可空，缺省生成 `居民_xxxx`（4 位随机字符）。
- 系统自动生成唯一登录账号 `wx_` + 6 位随机字符（如 `wx_k7m2p9`），个人页只读展示；登录账号可在编辑页修改（唯一校验），密码、手机号、邮箱由用户补充。
- 头像由前端注册成功后调用 `POST /api/v1/attachments`（`bizType=USER_AVATAR`）上传，再调用 `PUT /api/v1/auth/me/profile` 回写。

### 2.2 账号密码登录

`POST /api/v1/auth/login`

```json
{ "username": "admin", "password": "Admin@123", "client": "admin-web" }
```

响应同 2.1.1，`activeRole` 为 `ADMIN` 或 `WORKER`。密码错误返回 `40102`，禁用返回 `40103`。

### 2.2.1 居民自助注册

`POST /api/v1/auth/register`（匿名）

```json
{ "username": "resident_new", "password": "Resident@123", "nickname": "新居民", "phone": "13800100099" }
```

- 用户名：字母开头，4-32 位字母/数字/下划线；密码 8-32 位；昵称 2-20 字；手机号 11 位且唯一。
- 创建账号并绑定 `RESIDENT` 角色，注册成功直接返回 token（免二次登录）。
- 用户名或手机号重复返回 `11001`。

### 2.2.2 已登录账号绑定 / 解绑微信

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/auth/wx/bind` | `{ "code": "wx.login 返回的 code" }`，绑定当前微信到当前账号 |
| POST | `/api/v1/auth/wx/unbind` | 解绑当前账号的微信；账号必须有密码，否则返回 `40901` |

- 一个微信只能绑定一个账号，冲突返回 `11003`。
- 解绑只清空 `open_id/union_id`，账号密码登录不受影响；`GET /auth/me` 返回 `wxBound` 供前端展示绑定状态。

### 2.3 登出 / 当前用户

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/auth/logout` | 注销当前 token |
| GET | `/api/v1/auth/me` | 返回 `userId/username/nickname/avatarUrl/phone(脱敏)/email/emailVerified/hasPassword/wxBound/activeRole/roles[]` |
| PUT | `/api/v1/auth/me/profile` | `{ "nickname": "张三", "avatarUrl": "/uploads/..." }`，仅允许修改本人昵称与头像 |

### 2.3.1 账号资料自助维护（登录后）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| PUT | `/api/v1/auth/me/username` | `{ "username": "fuzhou_lin", "password": "当前登录密码" }`，字母开头 4-32 位字母/数字/下划线；全局唯一，占用返回 `11001`；已设置密码需带当前密码 |
| PUT | `/api/v1/auth/me/phone` | `{ "phone": "13800138000", "password": "当前登录密码" }`，11 位格式 + 唯一校验，不发短信；**更换已绑定手机号必须带当前登录密码**（未设置密码需先设置），密码错误返回 `40102` |
| PUT | `/api/v1/auth/me/password` | `{ "oldPassword": "旧密码（未设置过可不传）", "newPassword": "8-32 位" }`；原密码错误返回 `40102` |
| POST | `/api/v1/auth/email/code` | `{ "email": "a@b.com" }`，发送 6 位验证码，5 分钟有效；60 秒冷却、单邮箱日 10 条、单 IP 日 20 条，超限返回 `42901` |
| POST | `/api/v1/auth/email/bind` | `{ "email": "a@b.com", "code": "123456", "password": "当前登录密码" }`，校验通过后写入邮箱并标记已验证；**更换已绑定邮箱必须带当前登录密码**；邮箱重复返回 `11001` |
| POST | `/api/v1/auth/email/unbind` | 解绑邮箱（清空 `email` 与验证标记） |

- 首次绑定手机号 / 邮箱不需要密码；已绑定后更换时先校验当前登录密码，再校验新值（邮箱还需新邮箱验证码）。
- 账号未设置密码时不允许更换已绑定的手机号 / 邮箱，需先在编辑页设置密码。

- 邮箱验证码由 user-service 通过 QQ 邮箱 SMTP 真实发送；`deploy/.env` 配置 `MAIL_USER`（完整邮箱地址）与 `MAIL_PASS`（SMTP 授权码）。
- 未配置 `MAIL_USER` 时按 `MAIL_LOG_CODE=true` 把验证码打印到 user-service 日志，便于本地联调；生产必须改为 `false`。
- 招募报名前置校验手机号，未绑定返回 `40901`；管理员报名名单展示完整手机号与邮箱。

### 2.4 用户管理（ADMIN）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/users` | 分页查询，参数 `role`、`status`、`keyword`（昵称/用户名/手机号）；返回 `roles[]`、`lastLoginTime` |
| GET | `/api/v1/users/{id}` | 用户详情（含 `roles[]`，工人附带 `workerProfile`） |
| PATCH | `/api/v1/users/{id}/status` | `{ "status": "ENABLED" }`；禁止修改自己的账号状态 |
| PATCH | `/api/v1/users/{id}/roles` | `{ "roles": ["WORKER"] }`，至少保留一个角色；禁止改自己；新增 WORKER 前需有工人档案 |
| POST | `/api/v1/users/{id}/reset-password` | `{ "newPassword": "xxx" }`，8–32 位；禁止重置自己 |
| GET | `/api/v1/workers` | 处理人列表，返回 `id/username/nickname/phone(脱敏)/deptCode/deptName/areaCode/status/onDutyCount` |
| POST | `/api/v1/workers` | 新建处理人员账号（`username/password/nickname/phone/deptCode/areaCode`），同时建 USER + WORKER 角色 + 档案 |
| PUT | `/api/v1/workers/{id}` | 更新处理人（`nickname/deptCode/areaCode/status`），`{id}` 为 userId |
| GET | `/api/v1/workers/me/profile` | 处理人员查看本人档案（部门/区域/在岗状态/账号状态） |

> 所有 ADMIN 接口在网关按路径 + 角色拦截，服务内再以 `@SaCheckRole("ADMIN")` 二次校验。
> 角色变更不会刷新已登录会话中的 `roles/activeRole`，被调整用户需重新登录后生效。

---

## 3. 事件（event-service，8082）

### 3.1 居民上报

`POST /api/v1/events`

```json
{
  "title": "人行道地砖破损",
  "description": "XX路与YY街交叉口东北角人行道地砖破损约2平方米，行人容易绊倒，已持续三天",
  "categoryCode": "ROAD_FACILITY",
  "priority": "MEDIUM",
  "longitude": 116.397128,
  "latitude": 39.916527,
  "address": "北京市东城区XX路与YY街交叉口",
  "areaCode": "110101",
  "attachmentIds": [10001, 10002],
  "aiCallId": 90001,
  "idempotencyKey": "c0a8...（可选，也可放请求头）"
}
```

规则：

- `description` 10–1000 字；`attachmentIds` 1–9 个且必须为当前用户上传的 `TEMP` 附件；`categoryCode` 必须为字典值。
- 服务端二次调用 `map-service` 逆地址解析补齐 `province/city/district/street/areaCode`（前端传入的地址仅作参考，**以服务端解析结果为准**；解析失败时保留前端值并标记 `addressPending=true`）。
- 保存 `t_event_ai_snapshot`（若带 `aiCallId`），记录用户是否采纳建议。
- 状态初始为 `PENDING_REVIEW`；返回 `eventId/eventNo/status`。

响应：

```json
{ "code": 0, "message": "OK", "data": { "eventId": 5001, "eventNo": "EV202610210001", "status": "PENDING_REVIEW" } }
```

### 3.2 居民查询与操作

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/events/my` | 本人事件分页，参数 `status`、`page`、`size` |
| GET | `/api/v1/events/{id}` | 事件详情（本人或 ADMIN；WORKER 仅限其工单关联事件） |
| POST | `/api/v1/events/{id}/cancel` | 仅 `PENDING_REVIEW` 且本人 |
| PUT | `/api/v1/events/{id}/resubmit` | 仅 `REJECTED` 且本人；可修改描述/类别/位置/附件 |

详情响应包含：基本信息、AI 建议快照、附件列表、状态时间线（`t_event_status_log`）、工单摘要（通过 Feign 查询 `workorder-service` 只读接口，失败时降级为不展示工单区块，不阻断详情）。

### 3.3 管理员审核与派单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/events` | 分页查询：`status/categoryCode/areaCode/keyword/startTime/endTime` |
| GET | `/api/v1/events/{id}/admin-detail` | 审核详情（含 AI 建议对比、上报人信息、历史同类事件数） |
| POST | `/api/v1/events/{id}/approve` | 审核通过并派单（**Seata 全局事务入口**） |
| POST | `/api/v1/events/{id}/reject` | 驳回，`reason` 必填 5–200 字 |

审核通过请求：

```json
{
  "workerId": 2001,
  "categoryCode": "ROAD_FACILITY",
  "priority": "HIGH",
  "areaCode": "110101",
  "remark": "现场核实后派市政道路班组"
}
```

响应：

```json
{ "code": 0, "message": "OK", "data": { "eventId": 5001, "status": "DISPATCHED", "workOrderId": 7001, "workOrderNo": "WO202610210001" } }
```

失败（故障注入或分支失败）时返回 `50001`，且事件状态保持 `PENDING_REVIEW`，不产生工单。

### 3.4 统计与地图分布

| 方法 | 路径 | 参数 | 返回 |
| --- | --- | --- | --- |
| GET | `/api/v1/events/statistics/overview` | — | `total/pendingReview/processing/closed/avgHandleHours` |
| GET | `/api/v1/events/statistics/by-category` | `startTime/endTime` | `[{code,name,count}]` |
| GET | `/api/v1/events/statistics/by-area` | 同上 | `[{areaCode,areaName,count}]` |
| GET | `/api/v1/events/statistics/trend` | `days=7` | `[{date,count}]` |
| GET | `/api/v1/events/map-points` | `status/categoryCode/areaCode/startTime/endTime/bbox` | `[{id,eventNo,lng,lat,categoryCode,status,priority}]` |

- 统计接口仅 ADMIN；结果缓存 60s（Redis）。
- `bbox` 格式 `minLng,minLat,maxLng,maxLat`，最多返回 500 个点，超出时按区域聚合（返回 `cluster: true` 与聚合点）。

### 3.5 附件

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/attachments` | `multipart`：`file` + `bizType`（`EVENT_REPORT`/`WORKORDER_RESULT`/`WORKORDER_PROCESS`），返回 `{id,accessUrl,fileName,size}` |
| GET | `/api/v1/attachments/batch` | `ids=1,2,3` 批量查询元数据（供工单展示） |
| DELETE | `/api/v1/attachments/{id}` | 仅本人且状态 `TEMP` |

- 上传限制：jpg/jpeg/png，≤ 5MB，魔数校验，UUID 重命名，返回 `accessUrl` 由 Nginx 静态代理。
- 图片访问：生产环境直接 Nginx 托管；开发环境由 `event-service` 的静态资源映射（仅 `dev`）。

---

## 4. 工单（workorder-service，8083）

### 4.1 处理人员

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/workorders/my` | 本人工单分页：`status`（`PENDING_ACCEPT/PROCESSING/PENDING_ACCEPTANCE/COMPLETED`） |
| GET | `/api/v1/workorders/{id}` | 工单详情（事件快照、处理记录、附件、验收意见）；仅本人工单或 ADMIN |
| POST | `/api/v1/workorders/{id}/accept` | 接单；并发冲突返回 `13002` |
| POST | `/api/v1/workorders/{id}/process` | 追加处理记录：`{content, attachmentIds[]}` |
| POST | `/api/v1/workorders/{id}/submit` | 提交结果：`{content 必填 10–1000 字, attachmentIds 至少 1 个}` |
| POST | `/api/v1/workorders/{id}/ai-analyze` | 提交前 AI 预检：`{content, attachmentIds[]}`；仅本人工单，结果不落库、不改状态 |

- 详情响应 `data.event` 含 `title/description/address/longitude/latitude`，`data.eventPhotos` 为居民上报原图（`id/accessUrl/contentType/fileSize`），供处理人对照与地图导航。

接单响应：

```json
{ "code": 0, "message": "OK", "data": { "workOrderId": 7001, "status": "PROCESSING", "acceptTime": "2026-10-21 19:02:11" } }
```

### 4.2 管理员

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/workorders` | 全量分页：`status/assigneeId/eventNo/categoryCode` |
| POST | `/api/v1/workorders/{id}/acceptance` | `{ "result": "PASS" }` 或 `{ "result": "REJECT", "remark": "..." }` |
| POST | `/api/v1/workorders/{id}/reassign` | `{ "assigneeId": 2002, "reason": "原处理人休假" }` |

- 验收通过后事件由 MQ 异步关闭（秒级延迟，前端提示"验收成功，事件关闭中"）。
- 改派仅允许 `PENDING_ACCEPT` / `PROCESSING`。

---

## 5. AI（ai-service，8084）

### 5.1 事件分析

`POST /api/v1/ai/analyze`

```json
// 请求（description 与 imageUrls 至少一项非空）
{
  "description": "河道污染",
  "address": "福建省福州市仓山区建新镇下安新苑附近",
  "categoryHint": null,
  "imageUrls": ["/uploads/2026/10/22/xxx.jpg"]
}
// 响应（正常）
{
  "code": 0,
  "message": "OK",
  "data": {
    "callId": 90001,
    "categoryCode": "ENVIRONMENT_SANITATION",
    "description": "下安新苑附近河道水面漂浮大量生活垃圾与枯枝，水体发黑并伴有异味，污染范围约数十米，影响周边居民生活",
    "summary": "河道水面漂浮垃圾，水体发黑有异味",
    "priority": "HIGH",
    "suggestion": "安排环卫班组打捞漂浮物，并排查沿线排污口",
    "confidence": 0.91,
    "degraded": false,
    "model": "glm-4.6v-flash"
  }
}
// 响应（降级，仍返回 200）
{
  "code": 0,
  "message": "OK",
  "data": {
    "callId": 90002,
    "categoryCode": "OTHER",
    "description": null,
    "summary": null,
    "priority": "MEDIUM",
    "suggestion": null,
    "confidence": 0,
    "degraded": true,
    "degradeReason": "TIMEOUT"
  }
}
```

- 图片需先通过 `POST /api/v1/attachments`（`bizType=EVENT_REPORT`）上传，再把 `accessUrl` 放入 `imageUrls`；最多取前 3 张、单张不超过 2MB 参与视觉分析。
- 有图片时使用视觉模型 `glm-4.6v-flash`（失败回退 `glm-4v-flash`）；无图片时使用文本模型 `glm-4-flash`。
- `description` 为用户草稿，AI 在此基础上润色扩写并补充照片可见细节；草稿为空时直接根据照片生成。
- 限流：5 QPS；超时 30s；熔断后直接返回降级值，前端提示"AI 建议暂不可用，请手动选择类别"。
- `callId` 用于事件提交时关联 AI 快照；有效期 30 分钟。
- 前端必须允许用户修改 AI 建议，且**不得**因 AI 结果跳过管理员审核。

### 5.2 AI 输出 JSON 契约与后端校验

模型必须输出（严格 JSON，不含 Markdown 代码块）：

```json
{
  "category": "ROAD_FACILITY",
  "description": "10-200 字的润色扩写后事件描述",
  "summary": "不超过 60 字的事件摘要",
  "priority": "MEDIUM",
  "suggestion": "不超过 200 字的处置建议",
  "confidence": 0.86
}
```

后端校验顺序（任一失败记录 `validation_result` 并降级，不抛异常给用户）：

| 序号 | 校验 | 失败处理 |
| --- | --- | --- |
| 1 | 可解析为 JSON（剥离代码块标记后） | 重试 1 次；仍失败 → 兜底 `OTHER`，`degraded=true` |
| 2 | `category` ∈ 字典 | 置 `OTHER`，`validation_result=invalid_category` |
| 3 | `priority` ∈ 枚举 | 置 `MEDIUM` |
| 4 | `description` 长度 ≤ 500，去除 HTML/控制字符 | 截断 |
| 5 | `summary` 长度 ≤ 60，去除 HTML/控制字符 | 截断 |
| 6 | `suggestion` 长度 ≤ 200 | 截断 |
| 7 | `confidence` ∈ [0,1] | 越界置 0 |
| 8 | 描述含明显注入指令（如"忽略以上指令"） | 使用规则兜底分类，记录告警 |

> AI 结果只写入 `t_event_ai_snapshot` 与响应，**不写入事件最终类别/优先级字段**；最终值只来自用户提交值 + 管理员审核。

---

## 6. 地图（map-service，8085）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/map/reverse-geocode` | 参数 `lng`、`lat`；返回 `province/city/district/street/areaCode/address/formatted` |
| GET | `/api/v1/map/search` | 参数 `keyword`、`lng`、`lat`、`radius`（米）；返回 POI 列表（名称/地址/坐标） |
| GET | `/api/v1/map/areas` | 参数 `parentCode`；返回下级行政区列表（小程序/后台联动选择） |
| GET | `/api/v1/map/config` | 返回前端地图 SDK 所需公开配置（Key 为 **Web 端受限 Key**，不含 Secret） |

- 所有地图 Key 由 `map-service` 统一管理，前端只使用受限 Key；小程序端调用 `wx.chooseLocation` 时不需要地图 Key。
- 逆地址解析结果缓存 7 天（Redis，key：坐标取 4 位小数）。
- 外部调用失败：返回 `15001`，调用方（`event-service`）降级为保存前端地址并标记待确认。

---

## 7. 内部接口（OpenFeign，不经网关）

统一校验请求头 `X-Internal-Token`（与 Nacos 配置/环境变量一致），缺失或不匹配返回 `40303`。网关不配置 `/internal/**` 路由。

| 提供方 | 方法 | 路径 | 调用方 | 说明 |
| --- | --- | --- | --- | --- |
| workorder-service | POST | `/internal/v1/workorders` | event-service | 创建工单（**Seata AT 分支**） |
| workorder-service | GET | `/internal/v1/workorders/by-event/{eventId}` | event-service | 事件详情关联工单摘要 |
| user-service | GET | `/internal/v1/users/{id}/worker-profile` | workorder-service | 校验处理人角色/部门/在岗 |
| user-service | GET | `/internal/v1/users/{id}/brief` | event-service | 上报人昵称（详情展示） |
| map-service | GET | `/internal/v1/map/reverse-geocode` | event-service | 逆地址解析（补齐区域） |
| event-service | GET | `/internal/v1/events/{id}/snapshot` | workorder-service | 工单详情展示事件快照 |
| event-service | GET | `/internal/v1/events/{eventId}/attachments` | workorder-service | 事件上报原图（工单详情展示 + AI 整改前后对照） |

创建工单（内部）请求：

```json
{
  "eventId": 5001,
  "eventNo": "EV202610210001",
  "categoryCode": "ROAD_FACILITY",
  "priority": "HIGH",
  "assigneeId": 2001,
  "assigneeName": "李师傅",
  "deptCode": "ROAD",
  "creatorId": 1
}
```

响应：`{ "data": { "workOrderId": 7001, "workOrderNo": "WO202610210001", "status": "PENDING_ACCEPT" } }`

> 该分支事务参与方必须使用各自独立数据源，且 `undo_log` 表已建立；故障注入开关 `demo.fault.create-workorder` 由 Nacos 控制。

---

## 8. 消息契约（RabbitMQ）

### 8.1 事件状态变更

- Exchange：`city.event.topic`，Routing Key：`event.status.changed`
- 消息体：

```json
{
  "messageId": "uuid",
  "traceId": "8f2c1d9a4b7e4c3f",
  "eventId": 5001,
  "eventNo": "EV202610210001",
  "fromStatus": "PENDING_REVIEW",
  "toStatus": "DISPATCHED",
  "operatorId": 1,
  "occurredAt": "2026-10-21 18:50:00",
  "version": 1
}
```

- 消费方：`event-service`（通知/审计落库），必须幂等。

### 8.2 工单状态变更

- Exchange：`city.workorder.topic`，Routing Key：`workorder.status.changed`
- 消息体：

```json
{
  "messageId": "uuid",
  "traceId": "...",
  "workOrderId": 7001,
  "workOrderNo": "WO202610210001",
  "eventId": 5001,
  "fromStatus": "PENDING_ACCEPT",
  "toStatus": "PROCESSING",
  "eventTargetStatus": "PROCESSING",
  "operatorId": 2001,
  "occurredAt": "2026-10-21 19:02:11",
  "version": 1
}
```

- 消费方：`event-service` 按 `eventTargetStatus` 更新事件状态（条件更新 + 幂等键）。
- `workorder.assigned` 路由键用于通知处理人（站内通知表/日志）。

---

## 9. 接口实现清单（按阶段）

| 阶段 | 接口 |
| --- | --- |
| P1 基础设施 | `/actuator/health`、`/api/v1/auth/login`、`/api/v1/auth/wx-login`（mock） |
| P2 事件主流程 | 附件上传、AI 分析、事件提交/我的事件/详情、管理员审核/驳回、内部地图接口 |
| P3 工单闭环 | 工单列表/详情/接单/处理/提交、验收、改派、MQ 状态同步 |
| P4 统计与管理 | 统计 4 个接口、地图点位、用户管理、处理人管理 |
| P5 部署演示 | 全部接口回归 + 故障注入接口（仅 dev） |

## 10. P3 实现状态（2026-10-21）

| 模块 | 接口 | 状态 |
| --- | --- | --- |
| 认证 | `POST /api/v1/auth/login`、`POST /api/v1/auth/logout`、`GET /api/v1/auth/me` | 已实现 |
| 用户 | `GET /api/v1/users`（分页） | 已实现 |
| 事件 | `POST /api/v1/events`、`GET /api/v1/events/my`、`GET /api/v1/events`、`GET /api/v1/events/{id}` | 已实现 |
| 事件审核 | `POST /api/v1/events/{id}/approve`、`POST /api/v1/events/{id}/reject` | 已实现 |
| 附件 | `POST /api/v1/attachments`、`GET /api/v1/attachments/batch` | 已实现 |
| 工单 | `GET /api/v1/workorders/my`、`GET /api/v1/workorders`、`GET /api/v1/workorders/{id}` | 已实现 |
| 工单流转 | `POST /{id}/accept`、`POST /{id}/process`、`POST /{id}/submit`、`POST /{id}/acceptance`、`POST /{id}/reassign` | 已实现 |
| AI | `POST /api/v1/ai/analyze` | 已实现（智谱 GLM-4-Flash） |
| 地图 | `GET /api/v1/map/reverse-geocode` | 已实现（腾讯 Key 未开通 WebServiceAPI 时降级） |
| 内部 | 事件快照、附件绑定、工单创建、处理人校验、地图解析、AI 分析查询 | 已实现 |
| 未实现 | 微信登录、事件撤销/重提、统计 4 个接口、用户管理写接口、处理人管理接口 | 待后续阶段 |

### 10.1 P4 补充实现（2026-10-21）

| 模块 | 接口/能力 | 状态 |
| --- | --- | --- |
| AI | `POST /api/v1/ai/analyze` 返回 `reasoning`（判断理由）；Redis 缓存；限流/配额/超时分类；请求摘要脱敏 | 已实现 |
| 地图 | `GET /api/v1/map/config`（仅前端受限 Key）、无效坐标降级、`GET /api/v1/events/map-points` | 已实现 |
| 用户 | `GET /api/v1/workers`（管理员派单候选） | 已实现 |
| 治理 | 网关 Sentinel 路由限流、AI 服务 5 QPS、Seata AT 审核派单 | 已实现 |
| 前端 | 小程序 6 个页面、管理后台（Vue3 + Element Plus + Leaflet，Nginx 同源托管） | 已实现 |

> 已实现接口的请求/响应结构与本文档契约一致；端到端验证结果见 `docs/p3-test-report.md`。
> OpenAPI 描述文件尚未生成，后续可用 springdoc 自动导出。

## 11. P6 组织架构、两级验收、评分、招募与紧急度复评（2026-10-23）

### 11.1 组织架构

- 不新增角色：组长仍为 `WORKER`，通过 `t_worker_profile.worker_type` 区分 `LEADER / MEMBER`，组员通过 `leader_id` 归属组长。
- 管理员只把工单派给组长；组长再把工单分配给本组组员，也可自行接单处理。
- 片区字段：`work_zone`（片区名称）、`zone_lng/zone_lat`（片区中心坐标）。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/workers?type=LEADER` | 管理员查询处理人，可按类型过滤；返回新增 `workerType/leaderId/leaderName/workZone/zoneLng/zoneLat` |
| GET | `/api/v1/workers/my-team` | 组长查看本组组员（仅 LEADER） |
| POST | `/api/v1/workers` | 新建处理人，新增 `workerType/leaderId/workZone/zoneLng/zoneLat` |
| PUT | `/api/v1/workers/{id}` | 更新处理人，`leaderId=0` 表示清空所属组长 |

### 11.2 工单两级验收

状态机新增 `PENDING_LEADER_REVIEW`（待组长复核）：

- 组员提交：`PROCESSING → PENDING_LEADER_REVIEW`；组长复核通过 `→ PENDING_ACCEPTANCE`，退回 `→ PROCESSING`（退回次数 +1）。
- 组长本人提交：`PROCESSING → PENDING_ACCEPTANCE`，直接进入管理员验收。
- 事件侧只在进入 `PENDING_ACCEPTANCE` 时才同步状态，组长复核期间事件保持 `PROCESSING`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/workorders/team` | 组长查看本组工单（`leader_id=本人`） |
| GET | `/api/v1/workorders/loads?ids=1,2` | 按处理人统计在办工单数，供分配时做工作量平衡 |
| GET | `/api/v1/workorders/team/stats` | 组内月度绩效：完成数、平均评分、平均处理时长、退回次数、绩效分 |
| POST | `/api/v1/workorders/{id}/assign` | 组长分配组员：`{ assigneeId }`，仅能分配本组组员，分配后回到待接单 |
| POST | `/api/v1/workorders/{id}/leader-review` | 组长复核：`{ result: "PASS"/"REJECT", remark }`（退回时 remark 必填） |
| POST | `/api/v1/workorders/{id}/rating` | 居民评价：`{ rating: 1-5, tags: [], comment }`，仅事件上报人、仅已完成工单、每单一次 |

绩效分公式：`完成数 × 4 + 平均评分 × 10 − 退回次数 × 2`（最低 0）。

### 11.3 招募公告与报名

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/recruitments` | 管理员查看全部岗位（含报名人数） |
| POST | `/api/v1/recruitments` | 发布岗位：`title/deptCode/workZone/headcount/requirement/description/deadline/coverUrl` |
| PUT | `/api/v1/recruitments/{id}` | 编辑岗位 |
| PUT | `/api/v1/recruitments/{id}/status` | 上架/下架：`{ status: "OPEN"/"CLOSED" }` |
| GET | `/api/v1/recruitments/{id}/applications` | 查看报名名单 |
| PUT | `/api/v1/recruitments/applications/{applyId}/status` | 处理报名：`{ status: "PENDING"/"ACCEPTED"/"REJECTED" }` |
| GET | `/api/v1/recruitments/open` | 居民端招募列表（仅招募中且未过期，含 `appliedByMe`） |
| GET | `/api/v1/recruitments/{id}` | 岗位详情 |
| POST | `/api/v1/recruitments/{id}/apply` | 居民报名：`{ note }`，重复报名返回冲突 |

### 11.4 事件 AI 紧急度复评

- 居民上报、驳回后重新提交时自动异步复评；失败只记日志，不影响上报。
- 管理员可在审核弹窗手动“重新 AI 评估”，并可一键采纳 AI 等级。
- 输出字段写入 `t_event.ai_review_*`：`priority/reason/impact/responseMinutes/confidence/time`。
- 仅作审核参考，最终优先级仍以管理员审核结果为准。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/events/{id}/ai-review` | 管理员手动重新评估紧急程度，返回最新事件详情 |
| POST | `/internal/v1/ai/review-event` | ai-service 内部复评接口（Feign，不经网关） |

### 11.5 福州市六城区范围

- 允许区划代码：`350102 鼓楼 / 350103 台江 / 350104 仓山 / 350105 马尾 / 350111 晋安 / 350112 长乐`。
- 经纬度外包框（GCJ-02）：经度 `118.35–119.95`、纬度 `25.75–26.35`；上报与重新提交都同时校验坐标与区划。
- 越界返回参数错误：`仅支持福州市六城区（鼓楼区、台江区、仓山区、马尾区、晋安区、长乐区）范围内上报事件`。
