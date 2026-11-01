# 城市事件智能上报及工单调度平台

基于 Spring Cloud Alibaba 的城市事件上报与工单调度平台，覆盖「居民上报 → AI 研判 → 管理员审核 → 派单组长 → 组员处理 → 组长复核 → 管理员验收 → 居民评价」的完整业务闭环。项目包含微服务后端、Vue 3 管理后台、微信原生小程序（居民端 / 组员端 / 组长端）三部分。

## 1. 功能概览

- **居民端小程序**：微信一键登录、拍照与地图选点上报、AI 自动补全描述、事件进度跟踪、撤销与驳回重提、招募报名、居民评分、个人资料（手机号 / 邮箱 / 密码 / 账号）维护。
- **处理人员端小程序**：待办工单、接单、过程记录自动保存、整改后照片上传、AI 生成处理说明、撤销接单、撤回处理结果、组长分配组员、组内复核、团队绩效、通知中心与注意事项发布。
- **管理后台**：数据看板、事件审核（AI 紧急度复评与一键采纳）、代报事件、编辑 / 删除事件、派单推荐、工单管理（验收 / 改派 / AI 核实）、用户管理、工人管理（组长 / 组员 / 片区）、招募管理、地图聚合与定位、低星评价提醒。
- **智能派单**：中 / 高 / 紧急优先级且片区有在岗组长时自动派单；其余进入人工审核，审核弹窗按「片区 → 在办工单 → 距离」推荐责任组长。
- **AI 能力**：GLM-4-Flash 文本研判（类别 / 优先级 / 摘要 / 建议）、紧急度复评、GLM-4.6V 视觉整改核实（拥堵自动回退 GLM-4V）、按事件描述与一张整改后照片生成处理说明。AI 结果仅作参考，最终以人工审核 / 验收为准。
- **工程能力**：Redis 缓存与防穿透、RabbitMQ 异步通知与死信、Sentinel 限流熔断、Seata AT 跨库事务、traceId 全链路日志、Spring Boot Admin 实例监控。

## 2. 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端 | JDK 17、Spring Boot 3.2.12、Spring Cloud 2023.0.3、Spring Cloud Alibaba 2023.0.3.3 |
| 治理 | Nacos 2.4.3（注册 / 配置）、Gateway、OpenFeign、Sentinel、Seata 2.1.0（AT）、Sa-Token |
| 数据 | MySQL 8（5 库 21 表）、MyBatis-Plus、Druid、Redis 7 |
| 消息 | RabbitMQ 3.13（手动 ACK、幂等、死信） |
| 前端 | Vue 3 + Element Plus + 腾讯地图 JSAPI GL；微信原生小程序 + TypeScript |
| AI / 地图 | 智谱 GLM-4-Flash、GLM-4.6V-Flash、GLM-4V-Flash（回退）；腾讯位置服务 |

## 3. 目录结构

```text
.
├── pom.xml                  # Maven 聚合父工程
├── common/
│   ├── common-core/         # 统一响应、异常、校验、traceId、MyBatis-Plus 配置
│   ├── common-security/     # Sa-Token 会话、用户上下文、内部 token
│   └── common-api/          # Feign 契约与 DTO
├── gateway-service/         # 8080 统一入口
├── user-service/            # 8081 city_user
├── event-service/           # 8082 city_event
├── workorder-service/       # 8083 city_workorder
├── ai-service/              # 8084 city_ai
├── map-service/             # 8085 city_map
├── monitor-service/         # 8090 Spring Boot Admin
├── admin-web/               # 管理后台静态页面（Nginx 托管）
├── miniprogram/             # 微信小程序
├── typings/                 # 小程序类型声明
├── deploy/                  # Docker Compose、MySQL 初始化、Nginx、Nacos、Seata 配置
├── scripts/                 # 联调与验收验证脚本
├── docs/                    # 架构、数据库、接口、需求、验收、测试报告、踩坑日志
├── start.ps1                # 一键启动脚本
└── project.config.json      # 微信开发者工具配置
```

## 4. 环境要求

- JDK 17、Maven 3.9+
- Docker Desktop（Compose v2），用于启动 MySQL / Redis / Nacos / RabbitMQ / Seata / 管理后台
- PowerShell 7（推荐，`start.ps1` 使用）
- 微信开发者工具（运行小程序）
- 可选：智谱 API Key、腾讯位置服务 Key、QQ 邮箱 SMTP 授权码、微信小程序 AppSecret

## 5. 拉取后的启动步骤

### 5.1 克隆仓库

```powershell
git clone https://github.com/ISAMI-A/city-event-platform.git
cd city-event-platform
```

### 5.2 准备环境变量

仓库不包含 `.env`（含密钥，已被 `.gitignore` 排除），必须自己创建：

```powershell
Copy-Item deploy\.env.example deploy\.env
```

打开 `deploy\.env`，至少填写：

| 变量 | 说明 |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` | MySQL 根密码（两处保持一致） |
| `MYSQL_HOST_PORT` / `MYSQL_PORT` | 本机 3306 被占用时改成 3307，两处一致 |
| `INTERNAL_TOKEN` | 服务间内部调用令牌，自定义一段随机字符串 |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | RabbitMQ 账号密码 |
| `SBA_USER` / `SBA_PASSWORD` | 监控面板登录账号 |

按需填写（不填则对应功能降级或不可用）：

| 变量 | 用途 |
| --- | --- |
| `LLM_API_KEY`、`LLM_BASE_URL`、`LLM_MODEL` | AI 文本研判 |
| `LLM_VISION_MODEL`、`LLM_VISION_FALLBACK_MODEL` | AI 视觉核实与回退模型 |
| `TENCENT_MAP_KEY`、`TENCENT_MAP_SK`、`TENCENT_MAP_JS_KEY` | 逆地址解析与后台地图 |
| `WECHAT_APP_ID` / `WECHAT_APP_SECRET` | 小程序微信登录 |
| `MAIL_USER` / `MAIL_PASS` / `MAIL_FROM` | 邮箱验证码（QQ 邮箱 SMTP 授权码，不是登录密码） |

### 5.3 启动基础设施

```powershell
docker compose -f deploy/docker-compose.yml up -d
docker compose -f deploy/docker-compose.yml ps
```

MySQL 首次启动会按顺序执行 `deploy/mysql/init/*.sql`，自动建 5 个库、21 张表、字典数据与内置账号。

### 5.4（可选）导入 Nacos 配置

Nacos 控制台 `http://127.0.0.1:8848/nacos` → 配置管理 → 新建，Group 为 `CITY_PLATFORM`：

| Data ID | 内容 |
| --- | --- |
| `city-common.yaml` | `deploy/nacos/city-common.yaml` |
| `user-service.yaml` 等 | `deploy/nacos/user-service.yaml` |

未导入不阻塞启动（配置导入为 optional）。

### 5.5 打包并启动服务

推荐一键启动（自动加载 `.env`、检查容器、按需打包并拉起 6 个业务服务）：

```powershell
.\start.ps1
```

常用参数：

```powershell
.\start.ps1 -NoBuild       # 跳过打包
.\start.ps1 -ForceBuild    # 强制重新打包
.\start.ps1 -SkipInfra     # 基础设施已自行启动
```

也可以手动构建与启动：

```powershell
mvn -B clean package -DskipTests
java -jar user-service/target/user-service-1.0.0-SNAPSHOT.jar
java -jar event-service/target/event-service-1.0.0-SNAPSHOT.jar
java -jar workorder-service/target/workorder-service-1.0.0-SNAPSHOT.jar
java -jar ai-service/target/ai-service-1.0.0-SNAPSHOT.jar
java -jar map-service/target/map-service-1.0.0-SNAPSHOT.jar
java -jar monitor-service/target/monitor-service-1.0.0-SNAPSHOT.jar
java -jar gateway-service/target/gateway-service-1.0.0-SNAPSHOT.jar
```

> 启动顺序：先业务服务、后网关；网关需要服务实例已注册到 Nacos。`start.ps1` 已处理顺序与日志输出（`logs/<服务名>/<服务名>.log`）。

### 5.6 访问入口

| 入口 | 地址 | 账号 |
| --- | --- | --- |
| 管理后台 | http://127.0.0.1:8092/ | `admin / Admin@123` |
| 网关（接口） | http://127.0.0.1:8080 | 需登录 Token |
| Nacos | http://127.0.0.1:8848/nacos | 默认关闭鉴权 |
| RabbitMQ | http://127.0.0.1:15672 | 见 `deploy/.env` |
| Seata 控制台 | http://127.0.0.1:7091 | `seata / seata` |
| Spring Boot Admin | http://127.0.0.1:8090 | 见 `deploy/.env` 的 `SBA_USER` / `SBA_PASSWORD` |

### 5.7 运行小程序

1. 用微信开发者工具打开**项目根目录**（`project.config.json` 已设置 `miniprogramRoot: miniprogram/`）。
2. 修改 `miniprogram/app.ts` 里的 `baseUrl`：本机模拟器用 `http://127.0.0.1:8080`，真机预览改成电脑局域网 IP（如 `http://192.168.x.x:8080`）。
3. 开发阶段在开发者工具中勾选「不校验合法域名、web-view、TLS 版本以及 HTTPS 证书」。

## 6. 演示账号

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `Admin@123` |
| 组长 | `worker_road_leader`、`worker_lighting_leader`、`worker_sanitation_leader`、`worker_facility_leader` | `Worker@123` |
| 组员 | `worker_road_01`、`worker_road_02`、`worker_sanitation_01` 等共 12 个 | `Worker@123` |
| 居民 | `resident_fuzhou`、`resident_taijiang`、`resident_cangshan`、`resident_mawei`、`resident_jinan` | `Resident@123` |

首次初始化后即可用上述账号登录；事件、工单、招募的演示数据随 `deploy/mysql/init` 一起导入一部分，若需完整演示数据可执行 `scripts/reset-demo.ps1`（会清空业务库，图片目录需自行准备）。

## 7. 验证与测试

```powershell
mvn -B test                      # 单元测试
.\scripts\verify-p3.ps1          # 业务闭环端到端
.\scripts\verify-seata.ps1       # Seata 故障注入与回滚
.\scripts\verify-sentinel.ps1    # 网关与 AI 限流
.\scripts\verify-sentinel-circuit.ps1  # AI 熔断
.\scripts\verify-p4.ps1          # AI / 地图 / 同源联调
```

接口与架构细节见 `docs/api.md`、`docs/architecture.md`、`docs/database.md`，问题记录见 `docs/踩坑日志.md`。

## 8. 安全约定

- `.env`、数据库密码、`INTERNAL_TOKEN`、模型 / 地图密钥、微信 AppSecret 一律通过环境变量注入，仓库只提供 `.env.example`。
- 网关不路由 `/internal/**`；内部接口必须携带 `X-Internal-Token`，未配置时默认拒绝。
- 用户上传图片存放于 `data/uploads/`，`data/` 已在 `.gitignore` 中排除。

## 9. 常见问题

1. **启动报数据库连接失败**：确认 Docker 容器已启动、`deploy/.env` 中的 `MYSQL_HOST_PORT` 与 `MYSQL_PORT` 一致，且本机 3306 未被占用（占用时改 3307）。
2. **端口冲突**：8092（管理后台）、8090（监控）、8080-8085（服务）、8091/7091（Seata）需空闲，可在 `.env` 或 Compose 中调整映射。
3. **网关 404 / 服务未注册**：先启动业务服务再启动网关，确认 Nacos 控制台能看到实例。
4. **AI 或地图功能不可用**：多为未填 `LLM_API_KEY` / `TENCENT_MAP_KEY`，系统会降级为人工处理或保留用户填写地址。
5. **小程序图片不显示**：真机对 HTTP 图片有限制，需使用 HTTPS 或在本机模拟器调试；`baseUrl` 必须是手机能访问到的局域网地址。
6. **管理后台打不开**：确认 `docker compose ps` 中 `city-admin-web` 正常运行，并用 `http://127.0.0.1:8092/` 访问。

## 10. 已知限制

1. 腾讯位置服务 WebServiceAPI 免费配额有限，超限时逆地址解析降级为「保留用户地址 + 按区名兜底识别片区」。
2. 微信登录需要自行配置有效的 AppSecret；未配置时使用账号密码登录。
3. GLM-4.6V-Flash 为免费视觉模型，高峰期限流时自动回退 `glm-4v-flash`，AI 结论仅供参考。
4. 演示图片与上传数据不在仓库中（`data/` 已忽略），克隆后需要自备图片或跳过图片相关的演示。
5. Sentinel 规则在代码中初始化，尚未接入 Nacos 持久化与 Dashboard。
