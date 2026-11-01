# user-service

用户、角色与登录认证。

| 项 | 值 |
| --- | --- |
| 端口 | 8081 |
| 数据库 | `city_user` |
| 依赖 | Nacos、MySQL、Redis |
| 表 | `t_user`、`t_role`、`t_user_role`、`t_worker_profile` |

## 主要接口

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/login` | 匿名 | 账号密码登录（BCrypt） |
| POST | `/api/v1/auth/logout` | 登录 | 注销 |
| GET | `/api/v1/auth/me` | 登录 | 当前用户 |
| GET | `/api/v1/users` | ADMIN | 用户分页（验证 MP 分页） |
| GET | `/api/v1/system/ping` | 匿名 | 存活探针 |
| GET | `/internal/v1/system/ping` | 内部凭证 | 供 Feign 调用 |

## 内置账号（仅开发/演示）

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `Admin@123` | ADMIN |
| `worker_road` | `Worker@123` | WORKER（道路） |
| `worker_lighting` | `Worker@123` | WORKER（照明） |
| `worker_sanitation` | `Worker@123` | WORKER（环卫） |

## 配置

| 环境变量 | 说明 |
| --- | --- |
| `MYSQL_PASSWORD` | 数据库密码（必填） |
| `INTERNAL_TOKEN` | 内部接口共享密钥（必填，未设置则拒绝内部调用） |
| `NACOS_SERVER_ADDR` | Nacos 地址，默认 `127.0.0.1:8848` |

## 启动

```bash
java -jar user-service/target/user-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```
