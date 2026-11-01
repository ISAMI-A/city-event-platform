# ai-service

大模型适配、结构化输出校验与调用日志（P1 仅骨架）。

| 项 | 值 |
| --- | --- |
| 端口 | 8084 |
| 数据库 | `city_ai` |
| 依赖 | Nacos、MySQL、Redis、大模型 API（P2） |
| 表 | `t_ai_call_log`、`t_ai_prompt_template` |

## 当前接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/system/ping` | 存活探针 |

`POST /api/v1/ai/analyze` 与分析结果 7 项校验在 P2 实现，契约见 `docs/api.md` 第 5 章。

## 配置（P2 使用）

| 环境变量 | 说明 |
| --- | --- |
| `LLM_BASE_URL` | 默认 `https://api.deepseek.com` |
| `LLM_API_KEY` | 模型 Key，禁止入库 |
| `LLM_MODEL` | 默认 `deepseek-chat` |

## 启动

```bash
java -jar ai-service/target/ai-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```
