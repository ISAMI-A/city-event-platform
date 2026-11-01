# workorder-service

工单创建、接单、处理、验收与改派（P1 仅骨架与 Feign 验证）。

| 项 | 值 |
| --- | --- |
| 端口 | 8083 |
| 数据库 | `city_workorder` |
| 依赖 | Nacos、MySQL、Redis、RabbitMQ、user-service（Feign） |
| 表 | `t_work_order`、`t_work_order_log`、`t_work_order_acceptance`、`t_work_order_reassign`、`undo_log` |

## 当前接口

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/system/ping` | 匿名 | 存活探针 |
| GET | `/api/v1/system/feign-check` | ADMIN | Feign 链路验证（经网关） |
| GET | `/internal/v1/system/feign-check` | 内部凭证 | Feign 链路验证（脚本/联调） |

`feign-check` 真实调用 user-service 的 `/internal/v1/system/ping`，返回两段服务信息与同一 `traceId`。

## 配置

| 环境变量 | 说明 |
| --- | --- |
| `MYSQL_PASSWORD` | 数据库密码 |
| `INTERNAL_TOKEN` | 内部接口共享密钥（必须与 user-service 一致） |
| `USER_SERVICE_URL` | 仅 local 模式下指定静态地址 `http://127.0.0.1:8081` |
| `RABBITMQ_*` | MQ 连接 |

## 启动

```bash
java -jar workorder-service/target/workorder-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```
