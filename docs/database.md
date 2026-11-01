# 数据库设计说明书

> 文档版本：v0.1（规划稿）
> 数据库：MySQL 8.0，字符集 `utf8mb4`，排序规则 `utf8mb4_general_ci`，引擎 `InnoDB`，时区 `Asia/Shanghai`

---

## 1. 分库策略

**按微服务边界垂直分库，每个服务独占一个库，禁止跨库 JOIN 与跨库写入。**

| 库名 | 归属服务 | 说明 |
| --- | --- | --- |
| `city_user` | `user-service` | 用户、角色、处理人员档案 |
| `city_event` | `event-service` | 事件、附件元数据、审核与状态流水、AI 建议快照、类别字典 |
| `city_workorder` | `workorder-service` | 工单、处理记录、验收、改派、附件引用 |
| `city_ai` | `ai-service` | AI 调用日志、提示词模板 |
| `city_map` | `map-service` | 行政区区划字典、地图调用日志 |

### 1.1 为什么不使用单库多表

- 数据归属明确，避免"多个服务随意改同一张表"（需求硬性要求）。
- Seata AT 分支事务天然落在各自的库，`undo_log` 按库隔离，便于故障注入验证。

### 1.2 为什么不使用分库分表中间件

单表数据量在课程项目规模（万级）内，不需要 ShardingSphere；引入只会增加部署与排错成本。

---

## 2. 通用约定

| 项 | 约定 |
| --- | --- |
| 主键 | `id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` |
| 业务编号 | 事件 `EV + yyyyMMdd + 4 位序号`，工单 `WO + yyyyMMdd + 4 位序号`，由服务内序列 + 唯一索引保证唯一 |
| 逻辑删除 | `deleted TINYINT NOT NULL DEFAULT 0`（MyBatis-Plus `@TableLogic`），0 正常 / 1 删除 |
| 乐观锁 | `version INT NOT NULL DEFAULT 0`（MyBatis-Plus `@Version`），工单接单、事件审核使用 |
| 审计字段 | `create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`、`update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`、`create_by BIGINT`、`update_by BIGINT` |
| 金额/坐标 | 坐标 `DECIMAL(10,6)`，禁止 `FLOAT` |
| 枚举 | 使用 `VARCHAR(32)` 存英文枚举值，不使用 MySQL `ENUM`（便于演进） |
| 索引命名 | `uk_{表}_{列}`、`idx_{表}_{列}` |
| 外键 | **不建物理外键**，由应用层保证引用完整性（微服务分库场景下物理外键不可行） |
| 大文本 | 描述类字段用 `VARCHAR`（≤ 2000）或 `TEXT`；不使用 `BLOB` 存文件 |

---

## 3. city_user 库

### 3.1 ER 关系

```text
t_user 1 ─── N t_user_role N ─── 1 t_role
t_user 1 ─── 1 t_worker_profile   （仅 WORKER 角色存在）
```

### 3.2 t_user（用户）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 用户 ID |
| open_id | VARCHAR(64) | UK `uk_user_open_id`，可空 | 微信 openid，居民必填 |
| union_id | VARCHAR(64) | NULL | 微信 unionid（预留） |
| username | VARCHAR(64) | UK `uk_user_username`，可空 | 登录账号；微信注册自动生成 `wx_xxxxxx` |
| password_hash | VARCHAR(100) | NULL | BCrypt，未设置时为空 |
| nickname | VARCHAR(64) | NOT NULL | 昵称/姓名展示 |
| avatar_url | VARCHAR(512) | NULL | 头像 |
| phone | VARCHAR(20) | NULL | 手机号（本人页脱敏展示） |
| email | VARCHAR(128) | UK `uk_user_email`，可空 | 绑定邮箱 |
| email_verified | TINYINT | NOT NULL DEFAULT 0 | 邮箱是否已验证 |
| status | VARCHAR(16) | NOT NULL DEFAULT 'ENABLED' | `ENABLED` / `DISABLED` |
| last_login_time | DATETIME | NULL | 最后登录时间 |
| create_time / update_time / create_by / update_by / deleted / version | — | 公共字段 | — |

索引：`uk_user_open_id(open_id)`、`uk_user_username(username)`、`uk_user_email(email)`、`idx_user_status(status)`。

### 3.3 t_role（角色）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | — |
| role_code | VARCHAR(32) | UK `uk_role_code` | `RESIDENT` / `WORKER` / `ADMIN` |
| role_name | VARCHAR(64) | NOT NULL | 居民 / 处理人员 / 管理员 |
| description | VARCHAR(255) | NULL | — |
| create_time / update_time / deleted | — | 公共字段 | — |

初始化数据：三个固定角色。

### 3.4 t_user_role（用户角色关联）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK |
| user_id | BIGINT UNSIGNED | 用户 ID |
| role_code | VARCHAR(32) | 角色编码 |
| create_time | DATETIME | — |

索引：`uk_user_role(user_id, role_code)`、`idx_user_role_code(role_code)`。

### 3.5 t_worker_profile（处理人员档案）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK |
| user_id | BIGINT UNSIGNED | UK `uk_worker_user_id` |
| dept_code | VARCHAR(32) | 责任部门编码：`ROAD` / `LIGHTING` / `SANITATION` / `FACILITY` |
| dept_name | VARCHAR(64) | 部门名称（冗余展示） |
| area_code | VARCHAR(32) | 主要负责区域编码 |
| skill_tags | VARCHAR(255) | 技能标签，逗号分隔（演示用） |
| status | VARCHAR(16) | `ON_DUTY` / `OFF_DUTY` |
| create_time / update_time / deleted / version | — | 公共字段 |

索引：`idx_worker_dept(dept_code)`、`idx_worker_area(area_code)`。

> 说明：不建 `t_role_menu` / 菜单权限表——本项目角色固定、权限按角色硬编码，动态菜单权限会带来无收益的复杂度。

### 3.6 初始化数据要求

- 1 个管理员：`admin / Admin@123`（BCrypt 存储，**首次登录强制改密可作为可选增强，非本期必须**）。
- 3 个处理人员：分别归属 `ROAD`、`LIGHTING`、`SANITATION` 部门，密码 `Worker@123`。
- 索引与唯一约束由 `deploy/mysql/init/01-city_user.sql` 建立。

---

## 4. city_event 库

### 4.1 t_event（事件主表）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | 事件 ID |
| event_no | VARCHAR(32) | UK `uk_event_event_no` | 业务编号 |
| reporter_id | BIGINT UNSIGNED | NOT NULL | 上报居民 ID（不建外键，逻辑引用） |
| category_code | VARCHAR(32) | NOT NULL, `idx_event_category` | 最终类别（管理员确认后为准） |
| ai_category_code | VARCHAR(32) | NULL | AI 建议类别（留痕，不参与业务判断） |
| title | VARCHAR(100) | NOT NULL | 简短标题（可由 AI 摘要截取，用户可改） |
| description | VARCHAR(1000) | NOT NULL | 事件描述 |
| summary | VARCHAR(255) | NULL | AI 摘要 |
| priority | VARCHAR(16) | NOT NULL DEFAULT 'MEDIUM' | 最终优先级 |
| ai_priority | VARCHAR(16) | NULL | AI 建议优先级 |
| ai_confidence | DECIMAL(4,3) | NULL | AI 置信度 0–1 |
| ai_suggestion | VARCHAR(600) | NULL | AI 建议处理措施 |
| status | VARCHAR(32) | NOT NULL, `idx_event_status` | 见状态机 |
| province | VARCHAR(64) | NULL | 省 |
| city | VARCHAR(64) | NULL | 市 |
| district | VARCHAR(64) | NULL | 区县 |
| street | VARCHAR(128) | NULL | 街道 |
| area_code | VARCHAR(32) | NULL, `idx_event_area` | 区域编码（用于筛选与统计） |
| address | VARCHAR(255) | NOT NULL | 详细地址 |
| longitude | DECIMAL(10,6) | NOT NULL | GCJ-02 经度 |
| latitude | DECIMAL(10,6) | NOT NULL | GCJ-02 纬度 |
| photo_count | INT | NOT NULL DEFAULT 0 | 照片数量 |
| auditor_id | BIGINT UNSIGNED | NULL | 审核人 |
| audit_time | DATETIME | NULL | 审核时间 |
| reject_reason | VARCHAR(255) | NULL | 驳回原因 |
| dispatch_time | DATETIME | NULL | 派单时间 |
| close_time | DATETIME | NULL | 关闭时间 |
| create_time / update_time / create_by / update_by / deleted / version | — | 公共字段 | — |

索引：
- `uk_event_event_no(event_no)`
- `idx_event_reporter(reporter_id, create_time)`
- `idx_event_status_update(status, update_time)`
- `idx_event_category(category_code)`
- `idx_event_area(area_code)`
- `idx_event_geo(longitude, latitude)`（地图范围粗略筛选；不引入空间索引，课程项目按区域编码 + 边界框过滤足够）

### 4.2 状态机与合法迁移

| 当前状态 | 动作 | 目标状态 | 校验 |
| --- | --- | --- | --- |
| `PENDING_REVIEW` | `APPROVE` | `DISPATCHED` | 仅 ADMIN；必须指定处理人；Seata 全局事务 |
| `PENDING_REVIEW` | `REJECT` | `REJECTED` | 仅 ADMIN；`reject_reason` 必填 |
| `PENDING_REVIEW` | `CANCEL` | `CANCELLED` | 仅上报人本人 |
| `REJECTED` | `RESUBMIT` | `PENDING_REVIEW` | 仅上报人本人；记录版本 +1 |
| `DISPATCHED` | `WORKORDER_ACCEPTED` | `PROCESSING` | 仅 MQ 消费触发（工单已接单） |
| `PROCESSING` | `WORKORDER_SUBMITTED` | `PENDING_ACCEPTANCE` | 仅 MQ 消费触发 |
| `PENDING_ACCEPTANCE` | `WORKORDER_RETURNED` | `PROCESSING` | 仅 MQ 消费触发 |
| `PENDING_ACCEPTANCE` | `WORKORDER_COMPLETED` | `CLOSED` | 仅 MQ 消费触发；写 `close_time` |

所有迁移必须使用 `UPDATE ... WHERE id=? AND status=?`（乐观条件更新），受影响行数为 0 即视为状态冲突返回 `40902`。

### 4.3 t_attachment（附件元数据，统一归 event-service）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | 附件 ID |
| biz_type | VARCHAR(32) | NOT NULL | `EVENT_REPORT` / `WORKORDER_RESULT` / `WORKORDER_PROCESS` |
| biz_id | BIGINT UNSIGNED | NULL, `idx_attachment_biz` | 关联业务 ID（提交后绑定） |
| owner_id | BIGINT UNSIGNED | NOT NULL | 上传人 |
| original_name | VARCHAR(255) | NOT NULL | 原始文件名 |
| stored_name | VARCHAR(128) | NOT NULL, UK `uk_attachment_stored` | UUID 重命名 |
| file_path | VARCHAR(512) | NOT NULL | 相对路径 `2026/10/21/uuid.jpg` |
| access_url | VARCHAR(512) | NOT NULL | 访问 URL（Nginx 代理） |
| content_type | VARCHAR(64) | NOT NULL | `image/jpeg` / `image/png` |
| file_size | INT UNSIGNED | NOT NULL | 字节 |
| sha256 | CHAR(64) | NOT NULL | 文件摘要（去重/校验） |
| status | VARCHAR(16) | NOT NULL DEFAULT 'TEMP' | `TEMP` 上传未绑定 / `BOUND` 已绑定 |
| create_time / update_time / deleted | — | 公共字段 | — |

索引：`uk_attachment_stored(stored_name)`、`idx_attachment_biz(biz_type, biz_id)`、`idx_attachment_owner(owner_id)`、`idx_attachment_status(status, create_time)`（清理孤儿文件）。

> 文件实体不落库；`file_path` = `${UPLOAD_ROOT}/{yyyy}/{MM}/{dd}/{uuid}.{ext}`，Docker 中挂载到 `uploads` 卷。

### 4.4 t_event_status_log（事件状态流水）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK |
| event_id | BIGINT UNSIGNED | `idx_event_status_log_event` |
| event_no | VARCHAR(32) | 冗余，便于日志检索 |
| from_status | VARCHAR(32) | 原状态，首次为空 |
| to_status | VARCHAR(32) | 目标状态 |
| action | VARCHAR(32) | `CREATE` / `APPROVE` / `REJECT` / `RESUBMIT` / `CANCEL` / `MQ_SYNC` |
| operator_id | BIGINT UNSIGNED | 操作人（MQ 同步时为系统账号 0） |
| operator_role | VARCHAR(32) | 角色 |
| remark | VARCHAR(255) | 备注 |
| create_time | DATETIME | — |

### 4.5 t_event_audit_log（审核记录）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK |
| event_id | BIGINT UNSIGNED | `idx_event_audit_event` |
| auditor_id | BIGINT UNSIGNED | 审核人 |
| decision | VARCHAR(16) | `APPROVE` / `REJECT` |
| before_category / after_category | VARCHAR(32) | 类别变更留痕 |
| before_priority / after_priority | VARCHAR(16) | 优先级变更留痕 |
| reason | VARCHAR(255) | 驳回/修改原因 |
| create_time | DATETIME | — |

### 4.6 t_event_ai_snapshot（AI 建议快照）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK |
| event_id | BIGINT UNSIGNED | `idx_event_ai_event` |
| ai_call_log_id | BIGINT UNSIGNED | 关联 `city_ai.t_ai_call_log.id`（逻辑引用） |
| category_code / priority | VARCHAR | AI 建议值 |
| summary / suggestion | VARCHAR | AI 文本 |
| confidence | DECIMAL(4,3) | 置信度 |
| model_name | VARCHAR(64) | 模型标识 |
| accepted | TINYINT | 用户是否采纳（对比最终提交值） |
| create_time | DATETIME | — |

> 存快照而非只存 AI 日志的原因：事件是业务实体，必须能独立追溯"当时 AI 给了什么建议、用户改了什么"。

### 4.7 t_event_category（类别字典）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK |
| code | VARCHAR(32) | UK `uk_category_code` |
| name | VARCHAR(64) | 名称 |
| description | VARCHAR(255) | 说明 |
| sort | INT | 排序 |
| enabled | TINYINT | 是否启用 |
| create_time / update_time | — | 公共字段 |

初始化 5 条：`ROAD_FACILITY`、`PUBLIC_LIGHTING`、`ENVIRONMENT_SANITATION`、`PUBLIC_FACILITY`、`OTHER`。

---

## 5. city_workorder 库

### 5.1 t_work_order（工单主表）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | 工单 ID |
| work_order_no | VARCHAR(32) | UK `uk_wo_no` | 业务编号 |
| event_id | BIGINT UNSIGNED | UK `uk_wo_event_id` | 一个事件仅一个有效工单 |
| event_no | VARCHAR(32) | NOT NULL | 冗余，便于展示 |
| category_code | VARCHAR(32) | NOT NULL | 冗余事件类别 |
| priority | VARCHAR(16) | NOT NULL | 冗余优先级 |
| assignee_id | BIGINT UNSIGNED | NOT NULL, `idx_wo_assignee` | 处理人 |
| assignee_name | VARCHAR(64) | NOT NULL | 处理人姓名快照 |
| dept_code | VARCHAR(32) | NULL | 责任部门 |
| status | VARCHAR(32) | NOT NULL, `idx_wo_status` | 见状态机 |
| creator_id | BIGINT UNSIGNED | NOT NULL | 派单人（ADMIN） |
| assign_time | DATETIME | NOT NULL | 派单时间 |
| accept_time | DATETIME | NULL | 接单时间 |
| submit_time | DATETIME | NULL | 提交结果时间 |
| finish_time | DATETIME | NULL | 完成时间 |
| acceptance_remark | VARCHAR(255) | NULL | 最近一次验收意见 |
| return_count | INT | NOT NULL DEFAULT 0 | 验收退回次数 |
| reassign_count | INT | NOT NULL DEFAULT 0 | 改派次数 |
| create_time / update_time / create_by / update_by / deleted / version | — | 公共字段 | `version` 用于接单并发控制 |

索引：`uk_wo_no(work_order_no)`、`uk_wo_event_id(event_id)`、`idx_wo_assignee_status(assignee_id, status)`、`idx_wo_status_update(status, update_time)`。

### 5.2 工单状态机

| 当前状态 | 动作 | 目标状态 | 校验 / 副作用 |
| --- | --- | --- | --- |
| `PENDING_ACCEPT` | `ACCEPT` | `PROCESSING` | 仅本人；`UPDATE ... WHERE id=? AND status='PENDING_ACCEPT'`；并发失败返回 `13002`；发 MQ |
| `PROCESSING` | `ADD_PROCESS_LOG` | `PROCESSING` | 仅本人；追加处理记录与照片，不改状态 |
| `PROCESSING` | `SUBMIT` | `PENDING_ACCEPTANCE` | 仅本人；处理说明必填 + 至少 1 张照片；发 MQ |
| `PENDING_ACCEPTANCE` | `ACCEPT_PASS` | `COMPLETED` | 仅 ADMIN；写验收记录；发 MQ |
| `PENDING_ACCEPTANCE` | `ACCEPT_REJECT` | `RETURNED` → 业务回到 `PROCESSING` | 仅 ADMIN；意见必填；`return_count+1`；发 MQ |
| `PENDING_ACCEPT` / `PROCESSING` | `REASSIGN` | 状态不变（`PENDING_ACCEPT`） | 仅 ADMIN；写改派记录；`reassign_count+1`；发 MQ |

### 5.3 t_work_order_log（处理记录 / 操作流水）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK |
| work_order_id | BIGINT UNSIGNED | `idx_wo_log_wo` |
| work_order_no | VARCHAR(32) | 冗余 |
| action | VARCHAR(32) | `CREATE` / `ACCEPT` / `PROCESS` / `SUBMIT` / `ACCEPT_PASS` / `ACCEPT_REJECT` / `REASSIGN` |
| from_status / to_status | VARCHAR(32) | 状态变化 |
| operator_id | BIGINT UNSIGNED | 操作人 |
| operator_role | VARCHAR(32) | 角色 |
| content | VARCHAR(1000) | 处理说明 / 验收意见 |
| attachment_ids | VARCHAR(512) | 本记录关联附件 ID，逗号分隔（**仅引用，不存文件元数据**） |
| create_time | DATETIME | — |

### 5.4 t_work_order_acceptance（验收记录）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK |
| work_order_id | BIGINT UNSIGNED | `idx_wo_acc_wo` |
| work_order_no | VARCHAR(32) | 冗余 |
| acceptor_id | BIGINT UNSIGNED | 验收人 |
| result | VARCHAR(16) | `PASS` / `REJECT` |
| remark | VARCHAR(255) | 验收意见 |
| create_time | DATETIME | — |

### 5.5 t_work_order_reassign（改派记录）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK |
| work_order_id | BIGINT UNSIGNED | 索引 |
| from_assignee_id | BIGINT UNSIGNED | 原处理人 |
| to_assignee_id | BIGINT UNSIGNED | 新处理人 |
| reason | VARCHAR(255) | 改派原因（必填） |
| operator_id | BIGINT UNSIGNED | 操作管理员 |
| create_time | DATETIME | — |

### 5.6 附件引用关系

工单不保存文件元数据，只保存 `attachment_id`（`attachment_ids` 字段 + `t_work_order_log`）。附件详情由前端调用 `GET /api/v1/attachments/{id}` 或批量接口获取，归属 `event-service`。

> 好处：文件元数据单一归属，避免附件表在两个库重复；代价是展示时需要一次批量查询（可接受）。

---

## 6. city_ai 库

### 6.1 t_ai_call_log（调用日志）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK |
| trace_id | VARCHAR(64) | `idx_ai_trace` |
| biz_type | VARCHAR(32) | `EVENT_ANALYZE` |
| user_id | BIGINT UNSIGNED | 调用人 |
| model_name | VARCHAR(64) | 如 `deepseek-chat` |
| prompt_version | VARCHAR(16) | 提示词版本 |
| request_summary | VARCHAR(500) | 输入摘要（截断，脱敏） |
| response_summary | VARCHAR(1000) | 输出摘要（截断） |
| parsed_ok | TINYINT | 是否成功解析 JSON |
| validation_result | VARCHAR(255) | 后端校验结论（如 `invalid_category`） |
| error_code | VARCHAR(32) | 失败原因 |
| latency_ms | INT | 耗时 |
| prompt_tokens / completion_tokens | INT | 用量（供应商返回时记录） |
| create_time | DATETIME | — |

### 6.2 t_ai_prompt_template（提示词模板）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK |
| template_code | VARCHAR(64) | UK `uk_prompt_code` |
| version | VARCHAR(16) | 版本号 |
| system_prompt | TEXT | 系统提示词 |
| user_prompt | TEXT | 用户提示词模板（含占位符） |
| enabled | TINYINT | 是否启用 |
| create_time / update_time | — | 公共字段 |

---

## 7. city_map 库

### 7.1 t_area（行政区 / 区域字典）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK |
| area_code | VARCHAR(32) | UK `uk_area_code`（腾讯 adcode 或自定义区域码） |
| area_name | VARCHAR(64) | 名称 |
| parent_code | VARCHAR(32) | 上级编码 |
| level | VARCHAR(16) | `PROVINCE` / `CITY` / `DISTRICT` / `STREET` |
| center_lng / center_lat | DECIMAL(10,6) | 中心点 |
| enabled | TINYINT | 是否启用 |
| create_time / update_time | — | 公共字段 |

索引：`uk_area_code(area_code)`、`idx_area_parent(parent_code)`。

### 7.2 t_map_call_log（地图调用日志）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK |
| trace_id | VARCHAR(64) | — |
| call_type | VARCHAR(32) | `REVERSE_GEOCODER` / `PLACE_SEARCH` |
| request_param | VARCHAR(500) | 请求参数摘要 |
| success | TINYINT | 是否成功 |
| error_code | VARCHAR(32) | 失败原因 |
| latency_ms | INT | 耗时 |
| create_time | DATETIME | — |

---

## 8. Seata 相关表

`city_event` 与 `city_workorder` 两个库**必须**各建一张 `undo_log`（官方 AT 模式脚本，来源：Seata 官方 `script/client/at/db/mysql.sql`）：

```sql
CREATE TABLE IF NOT EXISTS `undo_log`
(
    `branch_id`     BIGINT       NOT NULL COMMENT 'branch transaction id',
    `xid`           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    `rollback_info` LONGBLOB     NOT NULL COMMENT 'rollback info',
    `log_status`    INT(11)      NOT NULL COMMENT '0:normal status,1:defense status',
    `log_created`   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    `log_modified`  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4 COMMENT ='AT transaction mode undo table';
```

> 该脚本需在实现阶段与 Seata Server 版本对应目录下的官方脚本逐字核对一次，避免版本差异。

---

## 9. DDL 示例（关键表）

### 9.1 t_event

```sql
CREATE TABLE `t_event`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '事件ID',
    `event_no`       VARCHAR(32)     NOT NULL COMMENT '事件编号',
    `reporter_id`    BIGINT UNSIGNED NOT NULL COMMENT '上报人ID',
    `category_code`  VARCHAR(32)     NOT NULL COMMENT '最终类别',
    `ai_category_code` VARCHAR(32)   NULL COMMENT 'AI建议类别',
    `title`          VARCHAR(100)    NOT NULL COMMENT '标题',
    `description`    VARCHAR(1000)   NOT NULL COMMENT '描述',
    `summary`        VARCHAR(255)    NULL COMMENT 'AI摘要',
    `priority`       VARCHAR(16)     NOT NULL DEFAULT 'MEDIUM' COMMENT '最终优先级',
    `ai_priority`    VARCHAR(16)     NULL COMMENT 'AI建议优先级',
    `ai_confidence`  DECIMAL(4, 3)   NULL COMMENT 'AI置信度',
    `ai_suggestion`  VARCHAR(600)    NULL COMMENT 'AI建议措施',
    `status`         VARCHAR(32)     NOT NULL COMMENT '状态',
    `province`       VARCHAR(64)     NULL,
    `city`           VARCHAR(64)     NULL,
    `district`       VARCHAR(64)     NULL,
    `street`         VARCHAR(128)    NULL,
    `area_code`      VARCHAR(32)     NULL COMMENT '区域编码',
    `address`        VARCHAR(255)    NOT NULL COMMENT '详细地址',
    `longitude`      DECIMAL(10, 6)  NOT NULL COMMENT '经度GCJ-02',
    `latitude`       DECIMAL(10, 6)  NOT NULL COMMENT '纬度GCJ-02',
    `photo_count`    INT             NOT NULL DEFAULT 0,
    `auditor_id`     BIGINT UNSIGNED NULL,
    `audit_time`     DATETIME        NULL,
    `reject_reason`  VARCHAR(255)    NULL,
    `dispatch_time`  DATETIME        NULL,
    `close_time`     DATETIME        NULL,
    `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`      BIGINT UNSIGNED NULL,
    `update_by`      BIGINT UNSIGNED NULL,
    `deleted`        TINYINT         NOT NULL DEFAULT 0,
    `version`        INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_event_no` (`event_no`),
    KEY `idx_event_reporter` (`reporter_id`, `create_time`),
    KEY `idx_event_status_update` (`status`, `update_time`),
    KEY `idx_event_category` (`category_code`),
    KEY `idx_event_area` (`area_code`),
    KEY `idx_event_geo` (`longitude`, `latitude`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='城市事件';
```

### 9.2 t_work_order

```sql
CREATE TABLE `t_work_order`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '工单ID',
    `work_order_no`  VARCHAR(32)     NOT NULL COMMENT '工单编号',
    `event_id`       BIGINT UNSIGNED NOT NULL COMMENT '事件ID',
    `event_no`       VARCHAR(32)     NOT NULL COMMENT '事件编号',
    `category_code`  VARCHAR(32)     NOT NULL,
    `priority`       VARCHAR(16)     NOT NULL,
    `assignee_id`    BIGINT UNSIGNED NOT NULL COMMENT '处理人ID',
    `assignee_name`  VARCHAR(64)     NOT NULL COMMENT '处理人姓名快照',
    `dept_code`      VARCHAR(32)     NULL,
    `status`         VARCHAR(32)     NOT NULL COMMENT '工单状态',
    `creator_id`     BIGINT UNSIGNED NOT NULL,
    `assign_time`    DATETIME        NOT NULL,
    `accept_time`    DATETIME        NULL,
    `submit_time`    DATETIME        NULL,
    `finish_time`    DATETIME        NULL,
    `acceptance_remark` VARCHAR(255) NULL,
    `return_count`   INT             NOT NULL DEFAULT 0,
    `reassign_count` INT             NOT NULL DEFAULT 0,
    `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`      BIGINT UNSIGNED NULL,
    `update_by`      BIGINT UNSIGNED NULL,
    `deleted`        TINYINT         NOT NULL DEFAULT 0,
    `version`        INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wo_no` (`work_order_no`),
    UNIQUE KEY `uk_wo_event_id` (`event_id`),
    KEY `idx_wo_assignee_status` (`assignee_id`, `status`),
    KEY `idx_wo_status_update` (`status`, `update_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='处理工单';
```

> 两张表均省略了部分索引与注释，实际 DDL 以第 4、5 章字段表为准，需在建表脚本中逐字段核对。

### 9.3 初始化顺序

`deploy/mysql/init/` 下按编号执行：

1. `00-databases.sql`：创建 5 个库 + 统一字符集。
2. `01-city_user.sql`：建表 + 角色/管理员/处理人员初始化。
3. `02-city_event.sql`：建表 + 类别字典。
4. `03-city_workorder.sql`：建表 + `undo_log`。
5. `04-city_ai.sql`：建表 + 默认提示词模板。
6. `05-city_map.sql`：建表 + 区域字典（演示城市）。
7. `06-undo-log-event.sql`：`city_event.undo_log`。
8. `07-demo-data.sql`：演示数据（仅演示/答辩环境启用，生产禁止执行）。

---

## 10. 索引与性能说明

| 场景 | 索引 | 说明 |
| --- | --- | --- |
| 居民"我的事件" | `idx_event_reporter(reporter_id, create_time)` | 覆盖列表排序 |
| 管理员待审核列表 | `idx_event_status_update(status, update_time)` | 状态 + 时间排序 |
| 处理人待办工单 | `idx_wo_assignee_status(assignee_id, status)` | 高频查询 |
| 地图区域筛选 | `idx_event_area(area_code)` + `idx_event_geo` | 先按区县收敛，再按包围盒过滤 |
| 统计（按类别/区域） | `idx_event_category`、`idx_event_area` | 数据量大时改为定时汇总表，本期不做 |
| 幂等/唯一 | 事件编号、工单编号、`event_id` 唯一工单 | 防重复派单 |

- 列表查询必须分页，禁止无 `LIMIT` 的全表扫描。
- 统计接口使用聚合 SQL + Redis 短时缓存（60s），避免每次实时全表聚合。

---

## 11. 数据一致性规则

| 规则 | 实现 |
| --- | --- |
| 一个事件只有一个有效工单 | `uk_wo_event_id` 唯一索引兜底 |
| 工单必须对应真实事件 | 创建前由 `event-service` 校验事件状态为 `PENDING_REVIEW` 且审核通过 |
| 事件状态与工单状态一致 | MQ 消费更新 + 定时对账任务（每日扫描不一致并告警，本期可作为加分项） |
| 附件必须归属业务 | 上传为 `TEMP`，提交时绑定 `BUS_ID` 并置 `BOUND`；孤儿文件定时清理 |
| 删除策略 | 业务数据一律逻辑删除；审计/流水表不删除 |
