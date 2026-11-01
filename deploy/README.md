# deploy 目录

## 用途

开发环境基础设施编排（P1）。业务服务镜像在 P5 阶段补充。

## 文件

| 文件 | 说明 |
| --- | --- |
| `docker-compose.yml` | MySQL 8.0、Redis 7.2、Nacos 2.4.3、RabbitMQ 3.13 |
| `.env.example` | 环境变量模板，复制为 `.env` 使用 |
| `mysql/init/*.sql` | 建库、建表、内置账号、字典、`undo_log`，按文件名顺序执行 |
| `nacos/*.yaml` | Nacos 配置样例（Data ID 对应文件名） |

## 使用

```bash
cp .env.example .env
# 填写 MYSQL_ROOT_PASSWORD、INTERNAL_TOKEN、RABBITMQ_PASSWORD

docker compose up -d
docker compose ps
docker compose logs -f mysql
```

## 注意事项

1. MySQL 初始化脚本**只在数据卷首次创建时执行**；如需重新初始化：
   `docker compose down -v`（会删除数据，仅开发环境使用）。
2. Nacos 2.x 必须同时映射 `8848`、`9848`、`9849`，否则服务注册失败。
3. 若 `nacos/nacos-server:v2.4.3` 拉取失败，改用 `v2.4.2`（客户端版本为 2.4.2）。
4. 容器内服务通过服务名互访：`mysql`、`redis`、`nacos`、`rabbitmq`。
5. `.env` 已被 `.gitignore` 排除，禁止提交。
