# 开发计划

> 文档版本：v0.1（规划稿）
> 周期约束：2–4 周，团队 2–3 人 + AI 编程 Agent
> 说明：计划以「阶段 + 周粒度」组织，不承诺具体天数；每阶段的完成标准是**可验证的证据**，而非"代码写完"。

---

## 1. 阶段划分总览

| 阶段 | 名称 | 核心目标 | 依赖 | 里程碑 |
| --- | --- | --- | --- | --- |
| P0 | 规划与准备 | 文档评审、决策确认、外部凭证与服务器就绪 | — | 本规划文档通过评审 |
| P1 | 工程骨架与基础设施 | 多模块可编译、Nacos/Gateway/MySQL/Redis/MQ/SBA 跑通 | P0 | 一次 `docker compose up` 起基础设施，网关注册可见 |
| P2 | 认证与事件上报主流程 | 登录、上传、AI 分析、地图解析、事件提交与查询 | P1 | 居民端完成一次真实上报 |
| P3 | 工单闭环与分布式能力 | 审核派单（Seata）、接单处理验收、MQ 状态同步 | P2 | 全流程闭环 + 故障注入回滚成功 |
| P4 | 治理能力与管理后台 | Sentinel 限流、Redis 缓存、统计地图、Web 后台、小程序完善 | P2（P3 可并行） | 管理后台可完成审核到验收全操作 |
| P5 | 容器化部署与演示 | 全量镜像、Compose 部署、端到端演示、答辩材料 | P1–P4 | Linux 服务器上从零启动并完成演示脚本 |

### 1.1 关键路径

`P0 → P1（Seata/SBA 基础验证）→ P2（事件+AI+地图）→ P3（Seata/MQ）→ P5`

P4 中的前端页面与小程序页面**可与 P3 并行**，是缩短总工期的唯一缓冲点。Seata 与 Docker 部署是风险最高的两处，必须前置验证（分别在 P1、P5 开始前做最小验证）。

### 1.2 周粒度排期（4 周方案，3 周方案见 1.3）

| 周次 | 后端 | 前端 | 验证节点 |
| --- | --- | --- | --- |
| 第 1 周 | P0 收尾、P1 骨架、Nacos/Gateway/SBA、登录与鉴权 | 管理后台脚手架、登录页；小程序登录页 | 服务注册可见、网关 401/200 正确、SBA 全 UP |
| 第 2 周 | P2 事件、附件、AI、地图、事件查询；Seata 最小 Demo | 小程序上报页（拍照/选点/AI 确认）、我的事件 | 一次真实上报入库；AI 结构校验日志可查 |
| 第 3 周 | P3 工单闭环、Seata 审核派单、MQ、幂等与死信 | 小程序工单页、管理后台审核/验收/派单 | 故障注入回滚成功；消息重试进死信可演示 |
| 第 4 周 | P4 Sentinel/Redis/统计、P5 镜像与部署 | 管理后台统计与地图、整体联调 | Linux 从零部署 + 10 步演示脚本通过 |

### 1.3 3 周压缩方案（若周期不足）

- 砍掉：管理后台「地图聚合」改为简单标记点；统计只保留 2 个接口（overview + by-category）；用户管理仅保留启用/禁用。
- 砍掉：改派功能、事件撤销、处理记录多次追加（保留一次提交）。
- 保留不可砍的硬性项：Nacos、Gateway、Feign、Sentinel、Seata（含回滚验证）、Redis、MQ（含重试）、三角色鉴权、SBA、Docker、大模型真实调用、地图真实调用。

---

## 2. 各阶段任务与验收（Definition of Done）

### P0 规划与准备

| 任务 | 产出 | 验收 |
| --- | --- | --- |
| 文档评审 | 本套 6 份文档 | 负责人确认 Q1–Q8（见 requirements.md 第 7 章） |
| 外部凭证 | 小程序 AppSecret、腾讯位置服务 Key（小程序/WebService/JSAPI）、大模型 API Key | 各完成 1 次 curl/接口连通测试 |
| 服务器 | Linux 4C8G、Docker 24+、Compose v2、开放端口 | `docker run hello-world` 成功 |
| Git | `git init`、`.gitignore`、分支策略、远程仓库 | 首次提交仅含现有小程序文件 + docs |
| 数据库 | 5 个库的初始化 SQL 草稿 | 在本地/服务器 MySQL 执行无报错 |

> **P0 不在本轮自动执行**：需用户确认后再 `git init` 与创建工程代码。

### P1 工程骨架与基础设施（"第一阶段实施"）

| 任务 | 明细 | 验证 |
| --- | --- | --- |
| T1.1 多模块骨架 | 根 POM + common（core/api/security）+ gateway + 6 个服务模块，全部可 `mvn -q clean package` | 构建成功，无版本冲突警告 |
| T1.2 公共能力 | 统一响应 `R<T>`、全局异常、错误码枚举、Jackson 配置、Logback 模板、traceId 过滤器 | 任一服务异常返回统一结构 |
| T1.3 Nacos | Compose 启动 Nacos，服务注册 + 配置导入（`city-common.yaml`） | 控制台可见实例；改日志级别动态生效 |
| T1.4 Gateway | 路由配置、鉴权过滤器、白名单、traceId 注入 | `/api/v1/**` 正确转发；无 token 返回 401 |
| T1.5 数据层 | Druid + MyBatis-Plus + 分页 + 自动填充；各库建表 | 分页 total 正确；`create_time` 自动填充 |
| T1.6 Redis | 连接、序列化配置、Sa-Token 会话存储 | 登录后 Redis 出现会话键；登出后消失 |
| T1.7 RabbitMQ | 交换机/队列/死信声明、生产者确认、消费者手动 ACK 骨架 | 控制台可见交换机与队列 |
| T1.8 SBA | monitor-service + actuator 暴露 + Nacos 发现 | 控制台 6 个服务 UP |
| T1.9 基础部署文件 | `deploy/docker-compose.yml`（仅 infra 组）+ `.env.example` + MySQL init | Linux 上 infra 全部 healthy |

**P1 完成标准（硬性）**：

1. 6 个后端服务可同时启动并注册到 Nacos；
2. 通过网关访问任一服务的 `/api/v1/system/ping` 返回统一响应；
3. 未携带 token 访问受保护接口返回 401，管理员 token 可访问；
4. SBA 页面所有服务状态 UP；
5. `docker compose -f deploy/docker-compose.yml up -d` 在 Linux 上成功，无容器反复重启；
6. 仓库可通过 `git clone` + `mvn package` 在另一台机器复现构建。

### P2 认证与事件主流程

| 任务 | 明细 |
| --- | --- |
| T2.1 登录体系 | 微信 `code2session`（含 dev mock）、账号密码登录、Sa-Token 签发、角色上下文 |
| T2.2 文件上传 | 魔数校验、大小/类型限制、UUID 命名、`TEMP/BOUND` 状态、静态访问 |
| T2.3 AI 分析 | DeepSeek 客户端、提示词 v1、JSON 严格解析、7 项校验、降级、调用日志 |
| T2.4 地图解析 | 腾讯 WebService 封装、逆地址解析、缓存、降级 |
| T2.5 事件提交 | 参数校验、Feign 调 map-service 补齐区域、附件绑定、AI 快照、编号生成 |
| T2.6 事件查询 | 我的事件分页、详情（时间线/附件/AI 快照）、撤销、驳回重提 |
| T2.7 小程序页面 | 登录、首页、上报（拍照+选点+AI 确认）、我的事件、详情 |

**完成标准**：小程序完成 1 次真实上报 → 数据库有事件 + 附件元数据 + AI 快照 → 详情页显示完整信息；AI 超时/返回非法 JSON 时接口仍返回 200 且 `degraded=true`。

### P3 工单闭环与分布式能力

| 任务 | 明细 |
| --- | --- |
| T3.1 Seata 接入 | Server 部署、TC 注册 Nacos、两库 `undo_log`、数据源代理（V1 验证） |
| T3.2 审核派单 | `@GlobalTransactional` + Feign 创建工单，故障注入开关 |
| T3.3 工单生命周期 | 接单（乐观锁）、处理记录、提交结果、验收、改派、越权校验 |
| T3.4 MQ 同步 | 工单状态变更消息、事件状态消费者、幂等、重试、死信 |
| T3.5 处理人端小程序 | 待办列表、工单详情、接单、处理、提交（拍照） |

**完成标准**：

1. 开启 `demo.fault.create-workorder=true` 时审核失败，事件状态与工单数据均无变化，`undo_log` 无残留；
2. 关闭开关后审批成功，事件 `DISPATCHED` + 工单 `PENDING_ACCEPT`；
3. 接单后事件在 5 秒内变为 `PROCESSING`（MQ 消费）；
4. 并发接单仅 1 次成功，其余返回 `13002`；
5. 消费异常消息重试 3 次后进入死信队列，日志含 `messageId` 与 `traceId`。

### P4 治理能力与管理后台

| 任务 | 明细 |
| --- | --- |
| T4.1 Sentinel | 网关路由限流、AI 熔断降级、规则持久化 Nacos、压测脚本 |
| T4.2 Redis 缓存 | 事件详情缓存、统计缓存、缓存删除策略、命中日志 |
| T4.3 统计接口 | overview / by-category / by-area / trend / map-points |
| T4.4 管理后台 | 登录、待审核、审核派单、工单列表与验收、用户管理、统计看板、地图分布 |
| T4.5 小程序完善 | 状态时间线、驳回重提、处理人历史、异常提示 |

**完成标准**：管理后台完成"审核 → 派单 → 验收"全操作；统计数字与数据库一致；限流压测可复现 42901；事件详情二次查询命中缓存。

### P5 容器化部署与演示

| 任务 | 明细 |
| --- | --- |
| T5.1 镜像 | 每个服务多阶段 Dockerfile，JVM 内存限制，健康检查 |
| T5.2 Compose | infra + app 全量编排、依赖 `depends_on: condition: service_healthy`、卷与网络 |
| T5.3 前端部署 | admin-web 构建产物由 Nginx 托管，`/api` 反代网关 |
| T5.4 一键启动 | `scripts/deploy.sh`（拉取、构建、启动、健康检查） |
| T5.5 端到端演示 | 10 步演示脚本 + 故障注入演示 + 压测演示 |
| T5.6 答辩材料 | 架构图、时序图、测试证据截图、技术栈验证清单 |

**完成标准**：空服务器上按 README 步骤从零部署成功；演示脚本 10 步全部通过；全部容器健康；SBA 全 UP。

---

## 3. 团队分工

| 角色 | 职责 | 主要阶段 |
| --- | --- | --- |
| 成员 A（架构/基础设施） | 父 POM、common、Gateway、Nacos、Seata、Sentinel、SBA、Docker/Compose、部署脚本 | P0/P1/P3(Seata)/P4(Sentinel)/P5 |
| 成员 B（业务后端） | user/event/workorder 业务、状态机、MQ、幂等与重试、统计 SQL | P1(数据层)/P2/P3 |
| 成员 C（前端/文档） | admin-web、小程序页面、接口联调、演示脚本、答辩材料 | P2(小程序)/P4/P5 |
| AI Agent | 脚手架、重复 CRUD、DTO/Mapper、单元测试、文档初稿、脚本 | 全阶段（产出需人工评审） |

协作约定：

- 接口先定契约（`docs/api.md` + OpenAPI），前后端并行；变更必须同步文档。
- 每人每日只合并可编译代码；`develop` 分支保持随时可构建。
- 跨模块改动（common、数据库）由成员 A 评审后合并。

---

## 4. Git 规范

| 项 | 规范 |
| --- | --- |
| 分支 | `main`（可部署）、`develop`（集成）、`feature/{模块}-{描述}`、`fix/{描述}`、`docs/{描述}` |
| 提交信息 | `feat(event): 新增事件撤销接口`、`fix(gateway): 修复白名单匹配错误`、`docs: 补充 Seata 故障注入说明` |
| 合并 | 功能分支 → `develop` 用 PR/MR；发布打 tag `v0.1.0` |
| `.gitignore` | `target/`、`node_modules/`、`dist/`、`.env`、`logs/`、`uploads/`、`*.iml`、`.idea/`、`miniprogram_npm/` |
| 禁止入库 | API Key、AppSecret、数据库密码、证书、构建产物、大文件（> 5MB） |

> 当前目录尚未初始化 Git，P0 确认后执行；初始化时不得删除既有小程序文件。

---

## 5. 测试方案

### 5.1 分层测试

| 层级 | 范围 | 工具 | 通过标准 |
| --- | --- | --- | --- |
| 单元测试 | 状态机迁移、AI 输出校验、权限判定、编号生成、幂等键 | JUnit 5 + Mockito | 核心类覆盖率 ≥ 60%，状态机与校验 100% 分支覆盖 |
| 接口测试 | 各服务 Controller（MockMvc） | Spring Boot Test | 正常/参数错误/越权/状态冲突四类用例通过 |
| 集成测试 | 真实 MySQL/Redis/MQ/Nacos | 远程环境 + 脚本 | 关键链路可重复执行 |
| 故障注入 | Seata 回滚、MQ 消费异常、AI 超时、地图失败 | 开关 + mock | 每项有预期结果与证据 |
| 端到端 | 三种角色全流程 | 演示脚本（Postman/curl/PowerShell） | 10 步全通过 |
| 压测 | 网关限流、AI 熔断 | JMeter 或脚本并发 | 限流阈值可复现 |

### 5.2 关键测试用例清单

| 编号 | 用例 | 预期 |
| --- | --- | --- |
| TC-01 | 居民未登录调事件接口 | 401 / 40101 |
| TC-02 | 居民调管理员审核接口 | 40301 |
| TC-03 | 处理人接非本人工单 | 13003 |
| TC-04 | 两用户并发接同一工单 | 1 成功 1 返回 13002 |
| TC-05 | 关闭事件再次提交 | 12002 |
| TC-06 | 上传 6MB 图片 | 16002 |
| TC-07 | 上传伪装成 jpg 的 txt | 16001 |
| TC-08 | AI 返回非 JSON | 降级 `OTHER`，事件仍可提交 |
| TC-09 | AI 返回非法 category | 归 `OTHER`，日志 `invalid_category` |
| TC-10 | 审核派单时注入故障 | 事件/工单均回滚，无残留 |
| TC-11 | MQ 消费抛异常 | 重试 3 次 → 死信，事件状态未被改脏 |
| TC-12 | 同一消息重复投递 | 仅处理一次 |
| TC-13 | 50 并发请求事件接口 | 超出阈值返回 42901 |
| TC-14 | 事件详情连续查询两次 | 第二次命中缓存，无 SQL |
| TC-15 | 地图服务不可用 | 事件仍提交成功，`addressPending=true` |

### 5.3 测试数据

- 1 名管理员、3 名处理人员（道路/照明/环卫）、5 名居民。
- 20 条事件覆盖 5 种状态、4 个类别、3 个区域，用于统计与地图演示。
- 初始化脚本 `deploy/mysql/init/07-demo-data.sql`（仅演示环境启用）。

---

## 6. 计划层面的风险与缓解

| 风险 | 触发信号 | 缓解措施 |
| --- | --- | --- |
| Seata 接入耗时超预期 | P1 最小 Demo 启动报错/`undo_log` 不写 | 第 1 周就做最小 Demo；失败则退化为"本地事务 + 补偿任务"，但**必须提前告知**（不满足硬性要求，需用户决策） |
| 无 Docker 环境 | 本地无法验证 Compose | 第 1 周内拿到 Linux 服务器；所有部署验证在服务器进行 |
| 小程序审核/域名限制 | 真机无法请求后端 | 开发者工具关闭域名校验；正式演示用 HTTPS 域名或体验版 |
| AI Key 不可用/限流 | 分析接口 429/超时 | 降级逻辑先行；准备 2 个供应商 Key |
| 前端进度落后 | 管理后台页面未完成 | 优先保证"审核/派单/验收"三页；统计页可后置 |
| 成员并行冲突 | common/数据库频繁冲突 | 接口与表结构第 1 周冻结，变更走评审 |

---

## 7. 第一阶段（P1）任务清单与验收标准（摘要）

> 本节即"规划完成后第一阶段实施"的落地清单，需用户确认后执行。

**任务**

1. 初始化 Git 仓库与 `.gitignore`（保留现有小程序文件）。
2. 创建 Maven 多模块骨架（父 POM + common 三模块 + gateway + 6 服务 + monitor）。
3. 落地 `common-core` 的 `R<T>`、错误码、全局异常、Jackson、Logback、traceId。
4. 落地 `common-security` 的 Sa-Token 配置、网关鉴权过滤器、用户上下文。
5. 编写 `deploy/docker-compose.yml`（infra：MySQL/Redis/RabbitMQ/Nacos/Sentinel/Seata）与 `.env.example`。
6. 编写 5 个库的初始化 SQL 与 `undo_log`。
7. 各服务接入 Nacos 注册与配置、Druid/MyBatis-Plus、actuator。
8. 搭建 `monitor-service` 并验证服务发现。
9. 编写 `scripts/` 下的启动与健康检查脚本。

**验收标准**

| 编号 | 验收项 | 判定方式 |
| --- | --- | --- |
| A1 | 全模块可构建 | `mvn -q clean package -DskipTests` 成功 |
| A2 | 服务注册 | Nacos 控制台可见 6 个服务实例 |
| A3 | 统一入口 | 经网关访问 `/api/v1/system/ping` 返回 `code=0` |
| A4 | 鉴权生效 | 无 token 401；ADMIN token 200；RESIDENT 访问管理员接口 40301 |
| A5 | 配置动态刷新 | Nacos 修改日志级别，服务日志即时变化 |
| A6 | 数据层 | 分页查询 total 正确，公共字段自动填充 |
| A7 | 监控 | SBA 列出全部服务且 UP |
| A8 | 基础设施部署 | Linux 上 infra 容器全部 healthy，重启后数据不丢 |
| A9 | 日志 | 日志含 `traceId`，网关与服务 `traceId` 一致 |
| A10 | 无回归 | 现有小程序工程文件未被修改/删除，开发者工具仍可打开 |

> P1 验收全部通过后，才进入 P2 业务编码。
