# gateway-service

统一 API 网关（WebFlux）。

| 项 | 值 |
| --- | --- |
| 端口 | 8080 |
| 数据库 | 无 |
| 依赖 | Nacos、Redis（Sa-Token 会话） |
| 技术栈 | Spring Cloud Gateway、Sa-Token Reactor、Spring Cloud LoadBalancer |

## 职责

- 统一入口与路由（不做 `StripPrefix`，服务内路径即 `/api/v1/...`）
- Sa-Token 统一登录校验，白名单：`/api/v1/auth/login`、`/api/v1/auth/wx-login`、`/api/v1/system/ping`、`/actuator/**`
- traceId 生成与透传（`X-Trace-Id`）
- CORS

## 路由

| 路径 | 目标 |
| --- | --- |
| `/api/v1/system/ping` | user-service（探针） |
| `/api/v1/auth/**`、`/api/v1/users/**`、`/api/v1/workers/**` | user-service |
| `/api/v1/events/**`、`/api/v1/attachments/**` | event-service |
| `/api/v1/workorders/**` | workorder-service |
| `/api/v1/ai/**` | ai-service |
| `/api/v1/map/**` | map-service |

> `/internal/**` 不在网关路由中，只能服务间调用。

## 启动

```bash
export NACOS_SERVER_ADDR=127.0.0.1:8848
export REDIS_HOST=127.0.0.1
java -jar gateway-service/target/gateway-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

`local` 模式：关闭 Nacos，路由使用 `http://127.0.0.1:808x` 静态地址，仅用于冒烟验证。

## 注意

- 网关不依赖 `common-core`（避免 Servlet 依赖进入 WebFlux 技术栈）。
- 登录用户不在网关写入请求头，由下游服务基于共享 Redis 会话解析 token，避免 WebFlux 异步链路丢失 Sa-Token ThreadLocal 上下文。
