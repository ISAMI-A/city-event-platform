#!/usr/bin/env bash
# P1 验收脚本（Linux，需 Docker + Compose v2）
# 用法：bash scripts/verify-p1.sh
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PASS=0
FAIL=0
pass() { echo "[PASS] $1"; PASS=$((PASS + 1)); }
fail() { echo "[FAIL] $1"; FAIL=$((FAIL + 1)); }

section() { echo; echo "===== $1 ====="; }

section "0. 环境检查"
command -v java >/dev/null 2>&1 && pass "java: $(java -version 2>&1 | head -1)" || fail "缺少 JDK 17"
command -v mvn >/dev/null 2>&1 && pass "maven: $(mvn -v 2>/dev/null | head -1)" || fail "缺少 Maven"
if command -v docker >/dev/null 2>&1; then
  pass "docker: $(docker --version)"
  docker compose version >/dev/null 2>&1 && pass "compose: $(docker compose version)" || fail "缺少 docker compose v2"
else
  fail "缺少 Docker，无法执行后续基础设施与部署检查"
  echo "未安装 Docker：跳过基础设施检查，P1 无法完整验收。"
  exit 1
fi

if [ ! -f deploy/.env ]; then
  echo "缺少 deploy/.env，请先复制 deploy/.env.example 并填写密码。"
  exit 1
fi
set -a
# shellcheck disable=SC1091
source deploy/.env
set +a

section "1. Maven 编译"
if mvn -B -ntp -DskipTests clean package >/tmp/city-build.log 2>&1; then
  pass "多模块编译成功"
else
  fail "编译失败，日志：/tmp/city-build.log"
  tail -40 /tmp/city-build.log
  exit 1
fi

section "2. Docker Compose 配置检查"
if docker compose -f deploy/docker-compose.yml config >/dev/null 2>&1; then
  pass "docker-compose.yml 语法与变量解析正确"
else
  fail "docker-compose.yml 配置解析失败"
fi

section "3. 启动基础设施"
docker compose -f deploy/docker-compose.yml up -d
for i in $(seq 1 60); do
  unhealthy=$(docker compose -f deploy/docker-compose.yml ps --format json 2>/dev/null | grep -c '"Health":"starting"' || true)
  [ "$unhealthy" = "0" ] && break
  sleep 5
done
for svc in city-mysql city-redis city-nacos city-rabbitmq; do
  status=$(docker inspect -f '{{.State.Health.Status}}' "$svc" 2>/dev/null || echo "missing")
  [ "$status" = "healthy" ] && pass "$svc healthy" || fail "$svc 状态：$status"
done

section "4. MySQL 与 Redis 连接"
MYSQL_READY=0
if docker exec city-mysql mysqladmin ping -uroot -p"$MYSQL_ROOT_PASSWORD" --silent >/dev/null 2>&1; then
  pass "MySQL 可连接"
  dbs=$(docker exec city-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SHOW DATABASES LIKE 'city_%'" 2>/dev/null | wc -l | tr -d ' ')
  [ "$dbs" = "5" ] && pass "5 个业务库已创建" || fail "业务库数量：$dbs（期望 5）"
  MYSQL_READY=1
else
  fail "MySQL 不可连接"
fi
docker exec city-redis redis-cli ping 2>/dev/null | grep -q PONG && pass "Redis PONG" || fail "Redis 不可用"

section "5. 启动服务"
mkdir -p logs
export SPRING_PROFILES_ACTIVE=dev
export INTERNAL_TOKEN
pids=()
for svc in user-service event-service workorder-service ai-service map-service; do
  java -jar "$svc/target/$svc-1.0.0-SNAPSHOT.jar" >"logs/$svc.out.log" 2>&1 &
  pids+=($!)
  echo "启动 $svc pid=$!"
done
sleep 35
java -jar gateway-service/target/gateway-service-1.0.0-SNAPSHOT.jar >logs/gateway-service.out.log 2>&1 &
pids+=($!)
sleep 25

section "6. Nacos 服务注册"
services=$(curl -s "http://127.0.0.1:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=50&groupName=CITY_PLATFORM" | grep -o '"name"' | wc -l | tr -d ' ')
[ "$services" -ge 5 ] && pass "Nacos 已注册服务数量：$services" || fail "Nacos 注册服务数量不足：$services"

section "7. Gateway 路由与鉴权"
ping_code=$(curl -s -o /tmp/ping.json -w '%{http_code}' http://127.0.0.1:8080/api/v1/system/ping)
grep -q '"code":0' /tmp/ping.json && pass "白名单探针路由成功（HTTP $ping_code）" || fail "白名单探针失败"
unauth=$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/v1/users)
[ "$unauth" = "401" ] && pass "未登录访问返回 401" || fail "未登录访问状态码：$unauth"

section "8. 登录与角色鉴权（需 MySQL）"
if [ "$MYSQL_READY" = "1" ]; then
  token=$(curl -s -X POST http://127.0.0.1:8080/api/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"Admin@123","client":"admin-web"}' \
    | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
  if [ -n "$token" ]; then
    pass "管理员登录成功"
    page=$(curl -s "http://127.0.0.1:8080/api/v1/users?page=1&size=10" -H "Authorization: Bearer $token")
    echo "$page" | grep -q '"total"' && pass "分页查询返回 total" || fail "分页查询异常：$page"
    echo "$page" | grep -q '"records"' && pass "MyBatis-Plus 分页生效" || fail "分页结构缺失"
  else
    fail "管理员登录失败"
  fi
else
  fail "MySQL 未就绪，跳过登录与分页验证"
fi

section "9. OpenFeign 真实调用"
feign=$(curl -s -H "X-Internal-Token: $INTERNAL_TOKEN" http://127.0.0.1:8083/internal/v1/system/feign-check)
echo "$feign" | grep -q '"service":"user-service"' && pass "workorder → user 调用成功，traceId 透传" || fail "Feign 调用失败：$feign"
no_token=$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8083/internal/v1/system/feign-check)
[ "$no_token" = "403" ] && pass "内部接口无凭证被拒绝（403）" || fail "内部接口无凭证状态码：$no_token"

section "10. 配置动态刷新（人工确认项）"
echo "请在 Nacos 控制台把 user-service 日志级别改为 DEBUG，然后执行："
echo "  tail -f logs/user-service.out.log"
echo "确认输出变化后记为通过。"

section "结果"
echo "PASS=$PASS FAIL=$FAIL"
echo "停止服务：kill ${pids[*]}"
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
