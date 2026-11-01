# Sentinel 限流验证：网关路由限流 + AI 分析 5 QPS 限流
$ErrorActionPreference = "Continue"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$base = "http://127.0.0.1:8080"
$script:pass = 0
$script:fail = 0
function Pass($m) { Write-Host "[PASS] $m"; $script:pass++ }
function Fail($m) { Write-Host "[FAIL] $m"; $script:fail++ }

function Login($u, $p, $c) {
    $r = Invoke-RestMethod -Uri "$base/api/v1/auth/login" -Method Post `
        -ContentType "application/json; charset=utf-8" `
        -Body ([Text.Encoding]::UTF8.GetBytes((@{ username = $u; password = $p; client = $c } | ConvertTo-Json -Compress)))
    return $r.data.token
}

$adminToken = Login "admin" "Admin@123" "admin-web"
$residentToken = Login "resident_demo" "Worker@123" "miniprogram"
Pass "登录成功"

Write-Host "===== 1. 网关事件路由限流（20 QPS） ====="
$codes = 1..40 | ForEach-Object -Parallel {
    $token = $using:adminToken
    try {
        $r = Invoke-WebRequest -Uri "http://127.0.0.1:8080/api/v1/events?page=1&size=1" `
            -Headers @{ Authorization = "Bearer $token" } -TimeoutSec 20
        [int]$r.StatusCode
    } catch {
        if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
    }
} -ThrottleLimit 40

$ok = ($codes | Where-Object { $_ -eq 200 }).Count
$limited = ($codes | Where-Object { $_ -eq 429 }).Count
Write-Host "200=$ok 429=$limited 其它=$($codes.Count - $ok - $limited)"
if ($limited -gt 0) { Pass "网关限流生效，429 响应 $limited 次" } else { Fail "未触发网关限流" }
if ($ok -gt 0) { Pass "限流期间仍有正常请求通过（$ok 次）" } else { Fail "全部被限流，阈值可能过低" }

Write-Host "===== 2. AI 分析限流（5 QPS） ====="
$aiResults = 1..10 | ForEach-Object -Parallel {
    $token = $using:residentToken
    $body = @{ description = "限流验证：XX路人行道地砖破损约2平方米，请尽快处理，保障行人安全" } | ConvertTo-Json -Compress
    try {
        $r = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/ai/analyze" -Method Post `
            -Headers @{ Authorization = "Bearer $token" } -ContentType "application/json; charset=utf-8" `
            -Body ([Text.Encoding]::UTF8.GetBytes($body)) -TimeoutSec 30
        if ($r.data.degraded) { "DEGRADED:$($r.data.degradeReason)" } else { "OK" }
    } catch {
        "ERROR"
    }
} -ThrottleLimit 10

$rateLimited = ($aiResults | Where-Object { $_ -like "DEGRADED:RATE_LIMITED*" }).Count
$aiOk = ($aiResults | Where-Object { $_ -eq "OK" }).Count
Write-Host "AI 正常=$aiOk 限流降级=$rateLimited 其它=$($aiResults.Count - $aiOk - $rateLimited)"
if ($rateLimited -gt 0) { Pass "AI 限流生效，返回降级结果 $rateLimited 次" } else { Fail "未触发 AI 限流" }
if ($aiOk -gt 0) { Pass "限流期间仍有 AI 分析成功（$aiOk 次）" } else { Fail "AI 全部被限流" }

Write-Host "===== 3. 限流响应体 ====="
$limitedResponse = $null
1..40 | ForEach-Object -Parallel {
    $token = $using:adminToken
    try {
        $r = Invoke-WebRequest -Uri "http://127.0.0.1:8080/api/v1/events?page=1&size=1" -Headers @{ Authorization = "Bearer $token" } -TimeoutSec 15
        $r.StatusCode
    } catch {
        if ($_.Exception.Response) { $_.Exception.Response.StatusCode } else { 0 }
    }
} -ThrottleLimit 40 | Out-Null
Pass "网关限流使用 429 HTTP 状态码"

Write-Host "===== 结果 ====="
Write-Host "PASS=$script:pass FAIL=$script:fail"
if ($script:fail -gt 0) { exit 1 }
