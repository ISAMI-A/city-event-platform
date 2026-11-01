# P4 测试报告（含 Seata / Sentinel 补强）

> 测试时间：2026-10-21
> 结论：Seata 回滚 6/6、Sentinel 限流 6/6、AI 与地图 12/12、管理后台浏览器联调全部通过；单元测试 11/11。

---

## 1. 新增能力

| 能力 | 实现 |
| --- | --- |
| Seata AT | `approve-and-dispatch` 全局事务覆盖事件库与工单库；故障注入开关 `demo.fault.create-workorder`（Nacos 热更新） |
| Sentinel | 网关按路由限流（事件 20 QPS、AI 20 QPS、工单 30 QPS），AI 服务内部 5 QPS；限流返回 429/42901 或 AI 降级结果 |
| AI 完善 | 统一 LLM 封装（智谱 GLM-4-Flash）、判断理由 `reasoning`、7 项校验、Redis 结果缓存、提示词注入防护、错误分类（限流/配额/超时）、请求摘要脱敏 |
| 地图完善 | 腾讯逆地址解析封装、Redis 缓存、无效坐标校验、失败降级；`/api/v1/map/config` 仅返回前端受限 Key；管理后台点位接口 |
| 管理后台 | 单页 Vue 3 + Element Plus + Leaflet（本地 vendor，无 CDN 依赖）：登录、事件筛选/详情/审核、地图标记、工单验收 |
| 小程序 | 登录、事件上报（拍照/选点/AI 确认/手动兜底）、事件列表、事件详情、工单列表、工单处理页，含加载/空/异常/定位拒绝状态 |

## 2. Seata 验证（6/6）

| 步骤 | 结果 |
| --- | --- |
| 全局事务拦截器生效（EventService.approve） | PASS |
| 事件库分支注册（t_event、t_event_audit_log、t_event_status_log） | PASS |
| 开启故障后审核失败 | PASS（code=50301，消息含“故障注入”） |
| 事件状态回滚为 PENDING_REVIEW | PASS |
| 关闭故障后重新审核成功，工单创建 | PASS |
| 两库 undo_log 均为 0、Seata 提交/回滚日志可追溯 | PASS |

关键日志：

```text
Begin new global transaction [172.18.0.6:8091:5188982993209724930]
branch register success ... lockKeys:t_event:10;t_event_audit_log:6;t_event_status_log:22
Commit branch transaction successfully ... Committing global transaction is successfully done
```

> Seata Server 使用 `apache/seata-server:2.1.0`，file 存储 + Nacos 注册；客户端因容器注册 IP 为 Docker 内网地址，使用 `seata.service.grouplist.default=127.0.0.1:8091` 直连宿主映射端口。

## 3. Sentinel 验证（6/6）

| 项目 | 结果 |
| --- | --- |
| 网关事件路由 20 QPS | 40 并发：200=20，429=20 |
| 网关限流响应 | HTTP 429，JSON code=42901 |
| AI 服务内部 5 QPS | 10 并发：成功 5，限流降级 5 |
| AI 限流响应 | code=0，`degraded=true`，`degradeReason=RATE_LIMITED`，用户可手动选择类别继续上报 |

## 4. AI 与地图验收（12/12）

| 检查 | 结果 |
| --- | --- |
| 真实 GLM 调用，返回类别/优先级/置信度 | PASS |
| 返回判断理由 `reasoning` | PASS |
| 返回建议措施 | PASS |
| 置信度在 [0,1] | PASS |
| 相同描述第二次命中 Redis 缓存（callId 相同） | PASS |
| 提示词注入被识别并降级（PROMPT_INJECTION） | PASS |
| 地图配置不返回服务端 WebService Key | PASS（`jsApiEnabled=false`） |
| 无效坐标降级且保留地址 | PASS |
| AI 降级时手动选类别仍可上报 | PASS（EV202610210011） |
| 管理后台同源事件列表 | PASS（total=11） |
| 管理后台地图点位 | PASS（points=11） |
| 管理后台浏览器联调 | PASS |

## 5. 管理后台浏览器联调

| 步骤 | 结果 |
| --- | --- |
| 登录 admin/Admin@123，顶部显示“系统管理员（ADMIN）” | PASS |
| 事件表格 10 条，首行 EV202610210010（已派单） | PASS |
| 详情弹窗：描述、处理进度、现场照片 | PASS |
| 地图分布：约 10 个彩色标记，点击显示事件编号 | PASS |
| 工单验收：表格有工单数据 | PASS |
| 控制台无 JS 异常与接口 4xx/5xx | PASS |

## 6. 小程序实现与联调说明

已实现 6 个页面：登录、事件上报、我的事件、事件详情、我的工单、工单处理。

- 上报页包含：地图选点（`wx.chooseLocation`）、拍照（`wx.chooseMedia`）、AI 分析与结果确认、手动类别兜底、加载/错误/定位拒绝提示。
- 接口调用与后端契约一致；后端接口已通过 HTTP 端到端脚本验证。
- **本机未安装微信开发者工具**，无法在本次环境执行小程序编译与真机预览；需在开发者工具中导入项目根目录（`miniprogramRoot=miniprogram/`）后验证。小程序不包含任何服务端 Key。

## 7. 已知限制

1. **腾讯地图 Key 当前未开通 WebServiceAPI（status=199）**：逆地址解析按降级处理（保留用户填写地址）；管理后台地图使用 OpenStreetMap 底图 + GCJ-02→WGS84 转换展示真实标记。开通 WebServiceAPI 与 JSAPI 后可将底图切换为腾讯地图。
   - **后续更新（P5 收尾）**：WebServiceAPI 已开通（当前受每日配额 121 限制）；管理后台底图已切换为腾讯地图 JSAPI GL，`TENCENT_MAP_JS_KEY` 经 `/api/v1/map/config` 下发，浏览器实测渲染正常。
2. 微信 `code2session` 登录未实现（无 AppSecret），小程序使用账号密码登录。
3. Seata 生产部署建议改为 db 存储 + 容器注册地址可通过 `seata.server.ip` 或网关代理解决；当前演示环境使用 grouplist 直连。
4. Sentinel 规则在代码中初始化，尚未接入 Nacos 持久化与 Dashboard；限流阈值可在 `SentinelGatewayConfiguration` 与 `SentinelRuleConfig` 调整。
5. 管理后台地图底图在 P4 阶段非腾讯（Key 权限不足），标记、筛选、详情与点位接口均为真实数据；P5 收尾已切换为腾讯地图 JSAPI GL。

## 8. 复现

```bash
# 基础设施（含 Seata 与管理后台 Nginx）
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d

# 构建与单元测试
mvn -B clean package

# 启动服务（注入 .env 中的变量）
# 验证
.\scripts\verify-p3.ps1      # 业务闭环 20 项
.\scripts\verify-seata.ps1   # 分布式事务 6 项
.\scripts\verify-sentinel.ps1 # 限流 6 项
.\scripts\verify-p4.ps1      # AI/地图/同源 12 项
```

管理后台：`http://127.0.0.1:8092/`（admin / Admin@123）
