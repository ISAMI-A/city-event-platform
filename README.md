# 城市事件智能上报及工单调度平台

基于 Spring Cloud Alibaba 的城市事件上报与工单调度平台。已完成 **P1 工程骨架**、**P3 业务闭环**、**P4 AI/地图集成与前端**、**P5 技术组件治理**，并补齐管理后台数据概览、用户管理、工人管理与小程序角色化导航。管理后台、小程序功能已可完整演示。

## 1. 当前已实现

| 能力 | 说明 |
| --- | --- |
| Maven 多模块 | 父工程统一 Java 17 / Boot 3.2.12 / Cloud 2023.0.3 / SCA 2023.0.3.3 |
| 公共模块 | `common-core`（响应/异常/校验/日志/MP）、`common-security`（Sa-Token/内部鉴权）、`common-api`（Feign 契约） |
| 服务 | `gateway-service`、`user-service`、`event-service`、`workorder-service`、`ai-service`、`map-service` |
| Nacos | 全部服务接入注册发现与配置导入（`CITY_PLATFORM` 分组） |
| 网关 | 5 类路由、Sa-Token 统一鉴权、traceId 生成与透传、CORS |
| 数据层 | 每服务独立库、Druid + MyBatis-Plus（分页/乐观锁/逻辑删除/自动填充） |
| 统一规范 | `R` 响应体、错误码、全局异常、参数校验、Logback 日志格式 |
| 数据库 | 5 个库 + 21 张表 + 分类字典 + 内置账号 + `undo_log` |
| 开发环境 | Docker Compose 编排 MySQL 8.0 / Redis 7.2 / Nacos 2.4.3 / RabbitMQ 3.13 |
| 事件业务 | 上报、附件上传与绑定、列表/详情、类别/状态/时间筛选分页、按优先级排序、审核通过/驳回、数据权限 |
| 工单业务 | 审核派单、接单、处理记录、提交结果、验收/退回、改派、状态流水、按优先级排序 |
| AI 分析 | 智谱 GLM-4-Flash 文本分析 + glm-4.6v-flash 读图生成/润色事件描述 + 后端校验 + 超时/异常降级 |
| AI 整改核实 | 处理人提交整改文字+现场照片后自动视觉核实（已整改/部分整改/未整改/无法判断），管理员验收弹窗可手动重新核实，模型拥堵时自动回退 glm-4v-flash；照片送入模型前自动压缩到最长边 1280px |
| 地图解析 | 腾讯位置服务逆地址解析 + Redis 缓存 + 失败降级（不阻断上报） |
| 地图定位 | 管理后台“定位我”：浏览器定位 WGS84 → GCJ-02 → 腾讯底图显示当前位置、精度与气泡（需授权定位） |
| 消息 | 工单状态变更事务提交后发送，消费端 Redis 幂等 + 手动 ACK + 死信队列 |
| 监控面板 | Spring Boot Admin 3.2.3（8090）：7 个实例健康、JVM/连接池指标、动态日志级别、线程/堆 dump，表单登录保护 |
| 测试 | 11 个单元测试 + 20 项真实 HTTP 端到端检查（见 `docs/p3-test-report.md`） |
| Seata | AT 全局事务覆盖审核派单，Nacos 热更新故障开关，undo_log 回滚验证通过 |
| Sentinel | 网关路由限流（事件 20 QPS / AI 20 QPS / 工单 30 QPS）、AI 服务 5 QPS 降级 |
| 管理后台 | Vue 3 + Element Plus + 腾讯地图 JSAPI GL，Nginx 同源托管：数据概览、事件审核（AI 紧急度复评/采纳/重评）、工单管理（验收/改派/AI 核实/结果照片/组长复核）、用户管理、工人管理（组长/组员/片区）、招募管理、地图聚合与定位 |
| 小程序 | 自定义角色 tabBar、我的（只读展示账号/手机号/邮箱/密码状态，点“编辑”进入独立资料页：头像昵称、手机号、邮箱验证码、密码、微信绑定）、居民上报/撤销/驳回重提、居民端招募轮播与报名、居民评分、工人接单/处理记录/提交结果/照片网格预览/AI 预检、组长团队与绩效/分配组员/组内复核 |
| P6 组织架构 | 管理员 → 4 名组长 → 每名组长 3 名组员；管理员只派给组长，组长分配组员并复核，组长本人提交直达管理员验收 |
| P6 范围限制 | 事件仅允许福州市六城区（鼓楼/台江/仓山/马尾/晋安/长乐），前后端双重校验 |
| P6 AI 复评 | 上报后自动异步复评紧急程度并给出依据/影响/建议响应时限，管理端可采纳或手动重评，失败不阻塞上报 |
| P4 测试 | AI/地图/同源联调 12 项、Seata 6 项、Sentinel 6 项（见 `docs/p4-test-report.md`） |
| Redis 缓存 | 事件详情（10min+抖动）、统计（60s）、空值防穿透、写后主动失效 |
| MQ 通知 | 派单后 `workorder.assigned` 异步通知，消费端校验工单状态 + 幂等 + 死信 |
| Sentinel 熔断 | AI 慢调用熔断（RT>3s），实测 `CIRCUIT_OPEN` 快速降级并可恢复 |
| P5 测试 | Redis/MQ/Sentinel/Seata 全部实际验证（见 `docs/p5-test-report.md`） |

## 2. 目录结构

```text
.
├── pom.xml                  # 聚合父 POM
├── common/
│   ├── common-core/         # 统一响应、异常、校验、traceId、MP 配置、日志模板
│   ├── common-security/     # Sa-Token 会话、用户上下文、内部接口鉴权
│   └── common-api/          # Feign 接口与 DTO
├── gateway-service/         # 8080
├── user-service/            # 8081  city_user
├── event-service/           # 8082  city_event
├── workorder-service/       # 8083  city_workorder
├── ai-service/              # 8084  city_ai
├── map-service/             # 8085  city_map
├── deploy/                  # Compose、MySQL 初始化、Nacos 配置样例、.env.example
├── scripts/                 # P1/P3 验证脚本
├── docs/                    # 需求、架构、数据库、API、计划、验收、测试报告、踩坑日志
├── miniprogram/             # 微信小程序（现有，P2 完善）
├── typings/                 # 小程序类型声明（现有）
├── package.json             # 小程序工程配置（现有）
└── project.config.json      # 微信开发者工具配置（现有）
```

> `miniprogram/`、`typings/` 等为原有小程序工程文件，本阶段未修改。

## 3. 环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17 | 已验证 17.0.10 |
| Maven | 3.9+ | 已验证 3.9.9 |
| MySQL | 8.0（容器）/ 8.4（本机开发可兼容） | 需要初始化 5 个库 |
| Redis | 6+（容器 7.2） | Sa-Token 会话与缓存 |
| Nacos | 2.4.x | 注册与配置，需开放 8848/9848/9849 |
| RabbitMQ | 3.13.x | 事件状态同步、工单分配通知（手动 ACK + 死信 + 幂等） |
| Docker | 24+ / Compose v2 | 仅用于基础设施与部署验证 |

## 4. 快速开始（推荐：Docker 基础设施）

### 4.0 一键启动器（推荐）

```powershell
# PowerShell 7 中执行；自动加载 deploy/.env、启动容器、按需打包并拉起 6 个服务
.\start.ps1

.\start.ps1 -NoBuild      # 跳过打包
.\start.ps1 -ForceBuild   # 强制重新打包
.\start.ps1 -SkipInfra    # 跳过 Docker 基础设施
.\start.ps1 -StopInfra    # 退出时同时停止容器
```

按 `Ctrl+C` 停止本次启动的所有 Java 服务；服务日志位于 `logs/<服务名>/<服务名>.log`。

```bash
# 1. 准备环境变量（禁止提交 .env）
cp deploy/.env.example deploy/.env
# 编辑 deploy/.env：填写 MYSQL_ROOT_PASSWORD、INTERNAL_TOKEN、RABBITMQ 密码等
# 邮箱验证码需额外填写 MAIL_USER（完整 QQ 邮箱）与 MAIL_PASS（QQ 邮箱 SMTP 授权码），并把 MAIL_LOG_CODE 改为 false

# 2. 启动基础设施 + 管理后台
docker compose -f deploy/docker-compose.yml up -d
docker compose -f deploy/docker-compose.yml ps

# 3. 管理后台与监控面板
# http://127.0.0.1:8092/  admin / Admin@123
# http://127.0.0.1:8090   SBA 监控面板（账号见 deploy/.env 的 SBA_USER / SBA_PASSWORD）
```

MySQL 首次启动会按顺序执行 `deploy/mysql/init/*.sql`（建库、建表、内置账号、字典、undo_log）。

### 4.0 演示数据重置（可重复执行）

```powershell
# 清空 4 个业务库与上传目录，重建账号、事件全状态、工单全状态与配套附件
.\scripts\reset-demo.ps1
# 只重置数据库、不动图片
.\scripts\reset-demo.ps1 -SkipImages
```

- SQL：`deploy/mysql/init/demo/reset-demo-data.sql`（容器内路径 `/docker-entrypoint-initdb.d/demo/reset-demo-data.sql`）。
- 图片：从 `D:\Users\沈佳勇\Desktop\AIGC Detection\data\test_images\testss` 复制 22 张到 `data/uploads/demo/`。
- 演示账号：

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `Admin@123` |
| 组长 | `worker_road_leader` / `worker_lighting_leader` / `worker_sanitation_leader` / `worker_facility_leader` | `Worker@123` |
| 组员 | `worker_road_01` / `worker_road_02` / `worker_sanitation_01` 等共 12 个 | `Worker@123` |
| 居民 | `resident_fuzhou` / `resident_taijiang` / `resident_cangshan` / `resident_mawei` / `resident_jinan` | `Resident@123` |
| 停用演示 | `resident_test`（DISABLED） | `Resident@123` |

- 事件覆盖 7 种状态（3 待审核、1 已驳回、1 已撤销、2 已派单、2 处理中、2 待验收、2 已关闭）且全部位于福州市六城区，含 AI 紧急度复评结果；工单覆盖待接单/处理中/待组长复核/待验收/已完成，包含组长分配、组长复核、居民评分、AI 核实结果与站内通知；另含 4 个招募岗位与 3 条报名记录。

业务数据统一存放在项目根 `data/`：上传附件在 `data/uploads/年/月/日/`（`deploy/.env` 的 `UPLOAD_ROOT` 可改），数据库只保存相对路径与元数据；`data/` 已在 `.gitignore` 中排除。

### 4.1 导入 Nacos 配置（可选但推荐）

Nacos 控制台 `http://localhost:8848/nacos` → 配置管理 → 新建：

| Data ID | Group | 格式 | 内容 |
| --- | --- | --- | --- |
| `city-common.yaml` | `CITY_PLATFORM` | YAML | `deploy/nacos/city-common.yaml` |
| `user-service.yaml` 等 | `CITY_PLATFORM` | YAML | 参考 `deploy/nacos/user-service.yaml` |

未导入时不阻断启动（配置导入为 `optional`）。

### 4.2 启动服务

先启动业务服务，再启动网关（网关需要服务实例才能路由）：

```bash
# 环境变量（每个终端）
export INTERNAL_TOKEN=change-me-internal-token
export MYSQL_PASSWORD=你的数据库密码
export SPRING_PROFILES_ACTIVE=dev
# 外部服务（P3 使用，均不写入代码）
export LLM_BASE_URL=https://open.bigmodel.cn/api/paas/v4
export LLM_API_KEY=你的智谱Key
export LLM_MODEL=glm-4-flash
export TENCENT_MAP_KEY=你的腾讯位置服务WebServiceKey
export WECHAT_APP_ID=wx6e2a9f37005ab6aa
export WECHAT_APP_SECRET=你的小程序AppSecret
export UPLOAD_ROOT=uploads

java -jar user-service/target/user-service-1.0.0-SNAPSHOT.jar
java -jar event-service/target/event-service-1.0.0-SNAPSHOT.jar
java -jar workorder-service/target/workorder-service-1.0.0-SNAPSHOT.jar
java -jar ai-service/target/ai-service-1.0.0-SNAPSHOT.jar
java -jar map-service/target/map-service-1.0.0-SNAPSHOT.jar
java -jar gateway-service/target/gateway-service-1.0.0-SNAPSHOT.jar
```

构建（含单元测试）：

```bash
mvn -B clean package
```

## 5. 本机无 Nacos 的最小模式（仅用于冒烟验证）

`local` profile 关闭 Nacos 注册与配置、把网关路由改为静态地址、Feign 改静态地址：

```bash
java -jar user-service/target/user-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=local
java -jar workorder-service/target/workorder-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=local
java -jar gateway-service/target/gateway-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=local
```

> `local` 模式**不能**用于验证服务注册发现，仅验证应用上下文、网关路由与 Feign HTTP 链路。

## 6. 验证命令

```bash
# 网关白名单探针（应返回 code=0）
curl -s http://localhost:8080/api/v1/system/ping

# 未登录访问受保护接口（应返回 HTTP 401 + code=40101）
curl -s -i http://localhost:8080/api/v1/users

# 管理员登录（需要 MySQL 已初始化）
curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123","client":"admin-web"}'

# 带 token 查询用户分页（验证 MyBatis-Plus 分页与角色鉴权）
curl -s "http://localhost:8080/api/v1/users?page=1&size=10" \
  -H "Authorization: Bearer <上一步返回的 token>"

# OpenFeign 真实调用（workorder → user 内部接口）
curl -s -H "X-Internal-Token: $INTERNAL_TOKEN" \
  http://localhost:8083/internal/v1/system/feign-check
```

一键验证脚本：

- `scripts/verify-p1.ps1`：P1 骨架与基础设施冒烟（Windows）
- `scripts/verify-p1.sh`：P1 完整验收（Linux + Docker）
- `scripts/verify-p3.ps1`：P3 业务闭环端到端验收（20 项检查）
- `scripts/verify-seata.ps1`：Seata 故障注入与回滚验证
- `scripts/verify-sentinel.ps1`：网关与 AI 限流验证
- `scripts/verify-sentinel-circuit.ps1`：AI 慢调用熔断验证
- `scripts/verify-p4.ps1`：AI 判断理由/缓存/降级、地图配置、同源联调

## 7. 安全约定

- `.env`、数据库密码、`INTERNAL_TOKEN`、模型 API Key 一律通过环境变量注入，**禁止提交仓库**。
- 仓库只提供 `deploy/.env.example`。
- 网关不路由 `/internal/**`；内部接口必须携带 `X-Internal-Token`，未配置时默认拒绝。
- 对外只暴露网关端口（8080）与必要基础设施端口。

## 8. 配置说明

| 文件 | 用途 |
| --- | --- |
| `application.yml` | 公共配置（端口、Nacos、数据源、MyBatis-Plus、Sa-Token、actuator） |
| `application-dev.yml` | 本机开发（Nacos 本地地址） |
| `application-docker.yml` | 容器内运行（服务名 `mysql/redis/nacos/rabbitmq`） |
| `application-local.yml` | 无 Nacos 冒烟模式（静态地址、关闭注册与配置中心） |

环境变量完整清单见 `deploy/.env.example`。

## 9. 已知限制（截至 P5）

1. **腾讯地图 WebServiceAPI 配额受限（status=121）**：map-service 按降级处理并保留用户地址，配额恢复后自动生效。管理后台底图已切换为腾讯地图 JSAPI GL（Key 由 `/api/v1/map/config` 下发，浏览器实测可正常渲染与展示事件点位）；小程序端仍使用微信内置地图选点。
2. 微信 `code2session` 登录未实现（无 AppSecret），小程序使用账号密码登录；已在微信开发者工具模拟器完成编译与 6 页面验证（0 错误 0 警告），未做真机预览与上传。
3. Seata 客户端使用 grouplist 直连宿主映射端口；生产环境建议改为 db 存储并配置可路由的注册地址。
4. Sentinel 规则在代码中初始化，尚未接入 Nacos 持久化与 Dashboard。
5. 统计看板仅提供 overview 接口，居民撤销/驳回重提未实现。
6. **GLM-4.6V-Flash 为免费视觉模型，高峰期限流（429 / 1305）**：已实现自动回退 `glm-4v-flash`，结果中的 `model` 字段记录实际使用的模型；付费视觉模型（`glm-4.6v` / `glm-4.6v-flashx`）当前账户余额不足。
