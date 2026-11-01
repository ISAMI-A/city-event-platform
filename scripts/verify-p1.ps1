# P1 冒烟验证（Windows，本机无 Docker/Nacos 时使用 local 模式）
# 用法：powershell -File scripts/verify-p1.ps1
$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$pass = 0
$fail = 0
function Pass($msg) { Write-Host "[PASS] $msg"; $script:pass++ }
function Fail($msg) { Write-Host "[FAIL] $msg"; $script:fail++ }

Write-Host "===== 0. 环境 ====="
try { Pass "java: $((java -version 2>&1 | Select-Object -First 1))" } catch { Fail "缺少 JDK 17" }
try { Pass "maven: $((mvn -v 2>&1 | Select-Object -First 1))" } catch { Fail "缺少 Maven" }
if (Get-Command docker -ErrorAction SilentlyContinue) { Pass "docker 可用" } else { Fail "未安装 Docker：Nacos/RabbitMQ/Compose 运行验证不可用" }

Write-Host "===== 1. 停止旧实例并编译 ====="
Get-NetTCPConnection -State Listen -LocalPort 8080,8081,8083 -ErrorAction SilentlyContinue |
    ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
Start-Sleep -Seconds 2
mvn -B -ntp -DskipTests clean package 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) { Pass "编译成功" } else { Fail "编译失败"; exit 1 }

Write-Host "===== 2. 启动 local 模式服务 ====="
$env:INTERNAL_TOKEN = if ($env:INTERNAL_TOKEN) { $env:INTERNAL_TOKEN } else { "local-dev-internal-token" }
New-Item -ItemType Directory -Force -Path "logs" | Out-Null

Start-Process java -ArgumentList "-jar","user-service\target\user-service-1.0.0-SNAPSHOT.jar","--spring.profiles.active=local" `
    -RedirectStandardOutput "logs\user.out.log" -RedirectStandardError "logs\user.err.log" -WindowStyle Hidden
Start-Process java -ArgumentList "-jar","workorder-service\target\workorder-service-1.0.0-SNAPSHOT.jar","--spring.profiles.active=local" `
    -RedirectStandardOutput "logs\workorder.out.log" -RedirectStandardError "logs\workorder.err.log" -WindowStyle Hidden
Start-Sleep -Seconds 25
Start-Process java -ArgumentList "-jar","gateway-service\target\gateway-service-1.0.0-SNAPSHOT.jar","--spring.profiles.active=local" `
    -RedirectStandardOutput "logs\gateway.out.log" -RedirectStandardError "logs\gateway.err.log" -WindowStyle Hidden
Start-Sleep -Seconds 22

Write-Host "===== 3. Redis ====="
$redisCli = "D:\sbPF\Redis\redis-cli.exe"
if (Test-Path $redisCli) {
    $pong = & $redisCli ping 2>&1
    if ($pong -match "PONG") { Pass "Redis PONG" } else { Fail "Redis 不可用：$pong" }
} else {
    Fail "未找到 redis-cli，跳过 Redis 检查"
}

Write-Host "===== 4. 网关路由 ====="
$ping = curl.exe -s "http://127.0.0.1:8080/api/v1/system/ping"
if ($ping -match '"code":0') { Pass "白名单探针路由成功" } else { Fail "探针失败：$ping" }

$code = curl.exe -s -o NUL -w "%{http_code}" "http://127.0.0.1:8080/api/v1/users"
if ($code -eq "401") { Pass "未登录返回 401" } else { Fail "未登录状态码：$code" }

$invalid = curl.exe -s -o NUL -w "%{http_code}" -H "Authorization: Bearer invalid" "http://127.0.0.1:8080/api/v1/users"
if ($invalid -eq "401") { Pass "无效 token 返回 401" } else { Fail "无效 token 状态码：$invalid" }

Write-Host "===== 5. OpenFeign ====="
$feign = curl.exe -s -H "X-Internal-Token: $env:INTERNAL_TOKEN" "http://127.0.0.1:8083/internal/v1/system/feign-check"
if ($feign -match '"service":"user-service"') { Pass "workorder → user 真实调用成功" } else { Fail "Feign 失败：$feign" }

$internalCode = curl.exe -s -o NUL -w "%{http_code}" "http://127.0.0.1:8083/internal/v1/system/feign-check"
if ($internalCode -eq "403") { Pass "内部接口无凭证被拒绝" } else { Fail "内部接口无凭证状态码：$internalCode" }

Write-Host "===== 6. 未覆盖项（环境缺失） ====="
Write-Host "- Nacos 注册/配置刷新：本机未安装 Nacos"
Write-Host "- MySQL 建库与登录分页：本机 root 密码未知"
Write-Host "- RabbitMQ、Docker Compose 运行：本机未安装 Docker"
Write-Host "- 请在具备 Docker 的 Linux 环境执行 scripts/verify-p1.sh 完成完整验收"

Write-Host "===== 结果 ====="
Write-Host "PASS=$pass FAIL=$fail"
Write-Host '停止服务：Get-NetTCPConnection -State Listen -LocalPort 8080,8081,8083 | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }'
if ($fail -gt 0) { exit 1 }
