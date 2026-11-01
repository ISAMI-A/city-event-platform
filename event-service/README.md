# event-service

城市事件主数据、附件元数据、审核与状态机（P1 仅骨架）。

| 项 | 值 |
| --- | --- |
| 端口 | 8082 |
| 数据库 | `city_event` |
| 依赖 | Nacos、MySQL、Redis、RabbitMQ、map-service（Feign，P2） |
| 表 | `t_event`、`t_attachment`、`t_event_status_log`、`t_event_audit_log`、`t_event_ai_snapshot`、`t_event_category`、`undo_log` |

## 当前接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/system/ping` | 存活探针 |

业务接口（事件上报、审核、统计、附件）在 P2/P4 实现，契约见 `docs/api.md`。

## 配置

| 环境变量 | 说明 |
| --- | --- |
| `MYSQL_PASSWORD` | 数据库密码 |
| `INTERNAL_TOKEN` | 内部接口共享密钥 |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | MQ 连接 |
| `MAP_SERVICE_URL` | 仅 local 模式下的静态 Feign 地址 |

## 启动

```bash
java -jar event-service/target/event-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```
