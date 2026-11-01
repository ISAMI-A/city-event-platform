# P5 测试报告：Redis / RabbitMQ / Sentinel / Seata AT

> 测试时间：2026-10-21
> 结论：四项技术均完成实际运行验证，结果如下。腾讯地图 Key 已开通 WebServiceAPI，但当日配额耗尽，地图解析按降级处理（已如实记录）。

---

## 1. Redis（全部通过）

### 1.1 缓存设计

| 用途 | Key | TTL | 失效策略 |
| --- | --- | --- | --- |
| 事件详情（热点查询） | `city:event:detail:{eventId}` | 10 分钟 + 0–60 秒随机抖动 | 审核/驳回/工单状态同步后主动删除 |
| 空结果防穿透 | `city:event:null:{eventId}` | 60 秒 | 事件创建后自然过期 |
| 事件统计 | `city:event:stat:overview` | 60 秒 | 事件创建、审核、驳回、状态同步后删除 |
| AI 分析结果 | `city:ai:analysis:{sha256}` | 20 分钟 | 到期自然失效 |
| Sa-Token 会话 | `Authorization:login:*` | 2 小时 | 登出/踢人删除 |

### 1.2 实际验证

| 检查 | 结果 |
| --- | --- |
| 事件详情首次查询写入缓存 | `city:event:detail:12` 存在，TTL=610s |
| 事件详情重复查询 | 命中缓存，返回状态 `DISPATCHED`、附件 1 张、时间线 2 条 |
| 统计接口 | `total=11 pending=6 closed=2`，Redis 值一致，TTL=60s |
| 审核后缓存失效 | 审核事件 12 后统计缓存被删除，重新查询重建（TTL=60） |
| 越权校验不被缓存绕过 | 缓存中含 `reporterId`，命中后仍执行归属校验，返回 40302 |
| 不存在事件 | 返回 12001，并写入 60 秒空值缓存 |

> 一致性策略：Cache-Aside（读时回填、写时删除），主动删除 + TTL 兜底；事件状态由 MQ 消费更新后同样删除缓存。

## 2. RabbitMQ（全部通过）

### 2.1 工单分配通知

| 项 | 实现 |
| --- | --- |
| 生产者 | `workorder-service` 创建工单后，在本地事务 `afterCommit` 发送 `workorder.assigned` |
| 交换机/队列 | `city.workorder.topic` → `city.notify.queue`（绑定 `workorder.assigned`） |
| 消费者 | `event-service` 校验工单当前状态为 `PENDING_ACCEPT` 且处理人一致后，写入 `t_notification` |
| 唯一标识 | 消息 `messageId`（UUID）+ 业务唯一索引 `uk_notify_user_biz(user_id,biz_type,biz_id)` |
| 幂等 | Redis `city:mq:idempotent:notify:{messageId}` + 数据库唯一索引双保险 |
| 失败处理 | 消费异常 → `nack(requeue=false)` → 死信交换机 `city.dlx.exchange` → `city.dlx.queue`，ERROR 日志含 messageId/traceId |
| 事务一致性 | 先提交数据库事务，再发送消息；发送失败只记录日志，不回滚业务；消费者不以消息为准，必须复核工单库状态 |

### 2.2 实际验证

| 检查 | 结果 |
| --- | --- |
| 派单后通知消费 | 处理人收到 1 条通知：`工单 WO202610210005（事件 EV202610210012）已分配给您，请及时接单处理。` |
| 队列积压 | `city.notify.queue` 0，`city.event.sync.queue` 0 |
| 失败消息进入死信 | 发布无效消息（eventId=99999）后 `city.dlx.queue` = 1，日志记录“工单分配通知处理失败，进入死信队列” |
| 重复投递 | Redis 幂等键命中时直接 ACK，不重复写通知 |

## 3. Sentinel（全部通过）

| 规则 | 配置 | 实测 |
| --- | --- | --- |
| 网关事件路由限流 | 20 QPS | 40 并发：200=20，429=20，响应 `code=42901` |
| AI 服务限流 | 5 QPS | 10 并发：成功 5，降级 5（`RATE_LIMITED`） |
| AI 慢调用熔断 | RT>3000ms、比例 100%、最小 1 次、窗口 10s、熔断 10s | 第 1 次 8282ms；第 2–4 次 14–19ms 返回 `CIRCUIT_OPEN`；关闭故障 10s 后恢复（5115ms，正常分析） |

> 熔断使用 `DegradeRule`（慢调用比例）；故障注入开关 `demo.fault.llm-slow` 通过 Nacos 热更新，仅演示用途。

## 4. Seata AT（全部通过）

| 检查 | 结果 |
| --- | --- |
| 全局事务入口 | `EventService.approve` 标注 `@GlobalTransactional(name="approve-and-dispatch")` |
| 参与库/表 | `city_event`：`t_event`、`t_event_audit_log`、`t_event_status_log`；`city_workorder`：`t_work_order`、`t_work_order_log` |
| 分支注册 | 日志可见 `branch register success ... lockKeys:t_event:...` |
| 故障注入 | `demo.fault.create-workorder=true` → 审核返回 50301，事件保持 `PENDING_REVIEW`，工单未创建 |
| 回滚完整性 | 两库 `undo_log` 均为 0，无残留全局事务 |
| 正常提交 | 关闭故障后重新审核成功，生成 `WO202610210007`，事件 `DISPATCHED` |

**不纳入分布式事务的操作**：大模型调用、腾讯地图调用、文件上传、RabbitMQ 发送与消费。
原因：外部 API 与异步消息不可回滚；MQ 采用“事务提交后发送 + 消费幂等 + 业务状态复核”的最终一致策略。

## 5. 外部依赖现状

| 项 | 状态 |
| --- | --- |
| 智谱 GLM-4-Flash | 正常，返回类别/摘要/优先级/判断理由/建议/置信度 |
| 腾讯地图 WebServiceAPI | Key 已开通（单次直连返回 `status=0`，北京市/东城区/110101），但 map-service 调用时返回 `status=121 此key每日调用量已达到上限`，系统按降级处理；配额恢复后自动生效 |
| 微信开发者工具 | 已完成模拟器编译与 6 页面验证（0 错误 0 警告），详见 5.1；未做真机预览与上传 |

### 5.1 小程序编译与页面验证

| 项 | 结果 |
| --- | --- |
| 打开方式 | `cli.bat open --project d:\Users\沈佳勇\Desktop\城建平台`，AppID `wx6e2a9f37005ab6aa`，TS/Less 编译插件 |
| 编译结果 | 0 错误 0 警告，Console 仅有 3 条无害提示（懒加载、关闭域名校验、自动热重载） |
| 居民流程 | 登录 `resident_demo` → 上报页（选点/拍照/类别/AI 分析按钮齐全）→ 我的事件（6 条）→ 事件详情（时间线、关联工单） |
| 处理人流程 | 登录 `worker_road` → 我的工单（5 条）→ 工单详情（状态、事件信息、接单按钮） |
| 发现并修复的问题 | 详情页照片 `src=/uploads/...` 被渲染层解析为模拟器 pageframe 地址导致加载失败；修复为前端拼接 `baseUrl`，并在网关新增 `/uploads/**` → event-service 路由；复验图片请求 HTTP 200（image/png），缩略图正常显示，Console 无红色错误 |
| 未执行 | 真机预览/上传（需该 AppID 的开发者上传权限），未产生任何脏数据 |

## 6. 复现命令

```bash
# 基础设施与前端
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d

# 构建与单元测试
mvn -B clean package

# 启动 6 个服务（环境变量见 deploy/.env）

# 验证
.\scripts\verify-p3.ps1              # 业务闭环 20 项
.\scripts\verify-seata.ps1           # Seata 6 项
.\scripts\verify-sentinel.ps1        # 限流 6 项
.\scripts\verify-sentinel-circuit.ps1 # 熔断 5 项
.\scripts\verify-p4.ps1              # AI/地图/同源 12 项
```

管理后台：`http://127.0.0.1:8092/`（admin / Admin@123）
