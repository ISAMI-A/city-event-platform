# map-service

腾讯位置服务适配、逆地址解析与区域字典（P1 仅骨架）。

| 项 | 值 |
| --- | --- |
| 端口 | 8085 |
| 数据库 | `city_map` |
| 依赖 | Nacos、MySQL、Redis、腾讯位置服务（P2） |
| 表 | `t_area`、`t_map_call_log` |

## 当前接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/system/ping` | 存活探针 |

逆地址解析、地点搜索、区域字典在 P2 实现，契约见 `docs/api.md` 第 6 章。

## 配置（P2 使用）

| 环境变量 | 说明 |
| --- | --- |
| `TENCENT_MAP_KEY` | WebService Key，禁止入库 |
| `TENCENT_MAP_BASE_URL` | 默认 `https://apis.map.qq.com` |

## 启动

```bash
java -jar map-service/target/map-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```
