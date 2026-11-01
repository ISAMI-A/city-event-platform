# P3 业务闭环测试报告

> 阶段：事件上报、审核、工单派发、处理与验收
> 测试时间：2026-10-21
> 结论：**端到端 20 项检查全部通过，单元测试 11 项全部通过**

---

## 1. 测试环境

| 项 | 值 |
| --- | --- |
| 操作系统 | Windows 11，PowerShell 7.6.7 |
| JDK / Maven | 17.0.10 / 3.9.9 |
| 基础设施 | Docker Desktop 29.5.3 + Compose v5.1.4 |
| 容器 | MySQL 8.0（宿主 3307）、Redis 7.2、Nacos 2.4.3、RabbitMQ 3.13 |
| 服务 | gateway 8080、user 8081、event 8082、workorder 8083、ai 8084、map 8085 |
| 配置模式 | `dev`（真实 Nacos 注册、真实 MySQL/Redis/RabbitMQ） |

## 2. 测试账号与数据

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `resident_demo` | `Worker@123` | RESIDENT |
| `resident_other` | `Worker@123` | RESIDENT（用于越权验证） |
| `admin` | `Admin@123` | ADMIN |
| `worker_road`（id=2） | `Worker@123` | WORKER（道路） |
| `worker_lighting`（id=3） | `Worker@123` | WORKER（照明） |

初始化脚本：`deploy/mysql/init/01-city_user.sql`、`07-demo-data.sql`、`08-p3-ai-analysis.sql`。

## 3. 实际执行的命令

```bash
# 构建（含单元测试）
mvn -B -ntp clean package

# 数据库迁移（容器首次初始化已自动执行，存量库需手动执行）
mysql -h127.0.0.1 -P3307 -uroot -p****** < deploy/mysql/init/08-p3-ai-analysis.sql
mysql -h127.0.0.1 -P3307 -uroot -p****** < deploy/mysql/init/07-demo-data.sql

# 启动服务（环境变量注入，不落库）
java -jar user-service/target/user-service-1.0.0-SNAPSHOT.jar
java -jar event-service/target/event-service-1.0.0-SNAPSHOT.jar
java -jar workorder-service/target/workorder-service-1.0.0-SNAPSHOT.jar
java -jar ai-service/target/ai-service-1.0.0-SNAPSHOT.jar
java -jar map-service/target/map-service-1.0.0-SNAPSHOT.jar
java -jar gateway-service/target/gateway-service-1.0.0-SNAPSHOT.jar

# 端到端验收
.\scripts\verify-p3.ps1
```

## 4. 端到端验收结果（20/20 通过）

| 步骤 | 检查项 | 结果 |
| --- | --- | --- |
| 1 | 居民、管理员、两个处理人员登录 | PASS |
| 2 | 附件上传（元数据 + 访问地址） | PASS |
| 3 | AI 分析（GLM-4-Flash 真实调用，非降级） | PASS |
| 4 | 居民创建事件，状态 `PENDING_REVIEW` | PASS |
| 5 | 居民"我的事件"包含本人事件 | PASS |
| 6 | 其他居民查看他人事件被拒绝（40302） | PASS |
| 7 | 管理员审核通过并创建工单 | PASS |
| 8 | 事件状态变为 `DISPATCHED` | PASS |
| 9 | 其他处理人查看工单被拒绝（13003） | PASS |
| 10 | 处理人接单，工单 `PROCESSING` | PASS |
| 11 | 重复接单被拒绝（13002） | PASS |
| 12 | MQ 同步事件为 `PROCESSING` | PASS |
| 13 | 处理记录保存 | PASS |
| 14 | 提交结果，工单 `PENDING_ACCEPTANCE` | PASS |
| 15 | MQ 同步事件为 `PENDING_ACCEPTANCE` | PASS |
| 16 | 管理员验收通过，工单 `COMPLETED` | PASS |
| 17 | 事件关闭（`CLOSED`） | PASS |
| 18 | 驳回流程，事件 `REJECTED` | PASS |
| 19 | 驳回后状态正确 | PASS |
| 20 | 附件元数据可查询 | PASS |

## 5. 数据库证据

### 5.1 事件

| 事件编号 | 状态 | 类别 | 优先级 | 审核人 | 照片数 | 已关闭 |
| --- | --- | --- | --- | --- | --- | --- |
| EV202610210004 | CLOSED | ROAD_FACILITY | HIGH | 1 | 1 | 是 |
| EV202610210005 | REJECTED | PUBLIC_LIGHTING | MEDIUM | 1 | 1 | 否 |

> 事件 4 的优先级由居民提交的 `MEDIUM` 被管理员审核改为 `HIGH`，证明 AI/用户提交值不直接决定最终审核结果。

### 5.2 状态流水（event 4）

| 原状态 | 新状态 | 动作 |
| --- | --- | --- |
| — | PENDING_REVIEW | CREATE |
| PENDING_REVIEW | DISPATCHED | APPROVE |
| DISPATCHED | PROCESSING | MQ_SYNC（接单消息） |
| PROCESSING | PENDING_ACCEPTANCE | MQ_SYNC（提交消息） |
| PENDING_ACCEPTANCE | CLOSED | MQ_SYNC（验收消息） |

### 5.3 工单日志（WO202610210002）

| 动作 | 原状态 | 新状态 | 附件 |
| --- | --- | --- | --- |
| CREATE | — | PENDING_ACCEPT | — |
| ACCEPT | PENDING_ACCEPT | PROCESSING | — |
| PROCESS | PROCESSING | PROCESSING | — |
| SUBMIT | PROCESSING | PENDING_ACCEPTANCE | 附件 id=6 |
| ACCEPT_PASS | PENDING_ACCEPTANCE | COMPLETED | — |

### 5.4 附件

| id | 业务类型 | 业务 ID | 状态 |
| --- | --- | --- | --- |
| 5 | EVENT_REPORT | 4 | BOUND |
| 6 | WORKORDER_RESULT | 2 | BOUND |

图片访问地址：`http://127.0.0.1:8082/uploads/2026/10/21/{uuid}.png`，实测 HTTP 200。

### 5.5 消息队列

`city.event.sync.queue` 消息数 0、`city.dlx.queue` 消息数 0，说明消息全部被正常消费，无死信积压。

## 6. 单元测试与接口校验

| 测试类 | 用例数 | 结果 |
| --- | --- | --- |
| `AiOutputValidatorTest` | 5 | 通过 |
| `EventStateMachineTest` | 2 | 通过 |
| `WorkOrderStateMachineTest` | 2 | 通过 |
| `WorkOrderServiceTest`（并发接单/越权） | 2 | 通过 |

接口测试由 `scripts/verify-p3.ps1` 通过真实 HTTP 调用覆盖（登录、创建、审核、接单、提交、验收、越权、参数校验）。

## 7. 已验证的关键技术点

| 技术点 | 验证方式 |
| --- | --- |
| MyBatis-Plus | 实体读写、分页、逻辑删除、审计字段自动填充 |
| OpenFeign | event → map（逆地址解析）、event → ai（分析结果）、event → workorder（创建工单）、workorder → user（处理人校验）、workorder → event（事件快照/附件绑定） |
| 状态机 | 白名单迁移 + 条件更新，非法迁移返回 40901/12002 |
| 并发控制 | `UPDATE ... WHERE status=? AND assignee_id=?`，重复接单返回 13002 |
| 数据权限 | 居民只能看本人事件（40302）；处理人只能看本人工单（13003） |
| 文件存储 | 二进制落磁盘，数据库只存元数据与访问路径，魔数校验 |
| 事务 | 事件审核 + 建工单同事务；Feign 失败回滚事件状态；工单按 event_id 幂等 |
| 消息 | 事务提交后发送，消费端 Redis 幂等 + 手动 ACK + 死信队列 |
| 日志 | 关键操作 INFO 日志带 `eventNo/workOrderNo` 与 `traceId` |

## 8. 已知限制

1. **腾讯地图 Key 当前返回 status=199（未开启 WebServiceAPI）**，因此逆地址解析走降级：保留用户填写地址、`areaCode` 为空，不阻断事件创建。需在腾讯位置服务控制台为该 Key 开通 WebServiceAPI。
2. 微信 `code2session` 登录未实现（无 AppSecret），当前使用账号密码登录；小程序端在 P4 接入。
3. 未接入 Seata：审核派单采用「本地事务 + 工单按 event_id 幂等 + Feign 失败回滚」保证一致性；Seata AT 在后续阶段补充。
4. 未实现居民撤销/驳回后重提、工单改派的完整 UI；改派接口已实现。
5. 统计看板、地图分布、管理后台页面不在本阶段范围。
6. 工单结果附件由 processing 阶段的 event-service 统一绑定；附件归属校验依赖上传人 ID，跨服务强校验可后续用签名票据加强。

## 9. 复现步骤

1. 启动 Docker Desktop 与基础设施：`docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d`
2. 按 `deploy/.env` 注入环境变量（数据库密码、`INTERNAL_TOKEN`、`LLM_API_KEY`、`TENCENT_MAP_KEY`）。
3. `mvn -B clean package` 构建。
4. 依次启动 5 个业务服务与网关。
5. 执行 `.\scripts\verify-p3.ps1`，预期 `PASS=20 FAIL=0`。
