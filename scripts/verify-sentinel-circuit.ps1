# Sentinel 熔断验证：通过故障注入让 LLM 调用变慢，触发慢调用熔断
$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$base = "http://127.0.0.1:8080"
$script:pass = 0
$script:fail = 0
function Pass($m) { Write-Host "[PASS] $m"; $script:pass++ }
function Fail($m) { Write-Host "[FAIL] $m"; $script:fail++ }

$resident = (Invoke-RestMethod -Uri "$base/api/v1/auth/login" -Method Post `
    -ContentType "application/json; charset=utf-8" `
    -Body ([Text.Encoding]::UTF8.GetBytes('{"username":"resident_demo","password":"Worker@123","client":"miniprogram"}'))).data.token

function SetSlow($enabled) {
    $content = "demo.fault.llm-slow: $($enabled.ToString().ToLower())"
    $raw = curl.exe -s -X POST "http://127.0.0.1:8848/nacos/v1/cs/configs" `
        --data-urlencode "dataId=ai-service.yaml" --data-urlencode "group=CITY_PLATFORM" `
        --data-urlencode "type=yaml" --data-urlencode "content=$content"
    if ($raw -notmatch "true") { throw "Nacos 配置发布失败：$raw" }
    Start-Sleep -Seconds 6
}

function InvokeAnalyze($desc) {
    $body = @{ description = $desc } | ConvertTo-Json -Compress
    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $r = Invoke-RestMethod -Uri "$base/api/v1/ai/analyze" -Method Post `
            -Headers @{ Authorization = "Bearer $resident" } `
            -ContentType "application/json; charset=utf-8" `
            -Body ([Text.Encoding]::UTF8.GetBytes($body)) -TimeoutSec 40
        $watch.Stop()
        [PSCustomObject]@{
            elapsed = $watch.ElapsedMilliseconds
            degraded = $r.data.degraded
            reason = $r.data.degradeReason
            category = $r.data.categoryCode
            code = $r.code
        }
    } catch {
        $watch.Stop()
        [PSCustomObject]@{ elapsed = $watch.ElapsedMilliseconds; degraded = $true; reason = "ERROR"; category = ""; code = -1 }
    }
}

Write-Host "===== 1. 开启 LLM 慢调用故障注入 ====="
SetSlow $true
Pass "已发布 demo.fault.llm-slow=true"

Write-Host "===== 2. 连续慢调用触发熔断 ====="
$results = @()
for ($i = 1; $i -le 4; $i++) {
    $r = InvokeAnalyze "熔断验证第 $i 次：XX路路灯不亮，整段约200米，影响夜间通行，请处理（$([guid]::NewGuid().ToString('N').Substring(0,8))）"
    $results += $r
    Write-Host "第 $i 次：耗时=$($r.elapsed)ms degraded=$($r.degraded) reason=$($r.reason) code=$($r.code)"
}
$slowCalls = ($results | Where-Object { $_.elapsed -ge 3000 }).Count
$circuitOpen = ($results | Where-Object { $_.reason -eq "CIRCUIT_OPEN" }).Count
if ($slowCalls -ge 1) { Pass "已产生 $slowCalls 次慢调用（>=3000ms）" } else { Fail "未产生足够的慢调用：$slowCalls" }
if ($circuitOpen -ge 1) { Pass "熔断已触发，$circuitOpen 次请求快速返回 CIRCUIT_OPEN" } else { Fail "未观察到熔断" }
$fastBlocked = ($results | Where-Object { $_.reason -eq "CIRCUIT_OPEN" -and $_.elapsed -lt 1500 }).Count
if ($fastBlocked -ge 1) { Pass "熔断响应耗时 < 1500ms，未继续调用模型" } else { Fail "熔断响应耗时异常" }

Write-Host "===== 3. 关闭故障并等待熔断恢复 ====="
SetSlow $false
Start-Sleep -Seconds 11
$recovered = InvokeAnalyze "熔断恢复验证：XX公园南门路灯不亮，请处理（$([guid]::NewGuid().ToString('N').Substring(0,8))）"
Write-Host "恢复调用：耗时=$($recovered.elapsed)ms degraded=$($recovered.degraded) reason=$($recovered.reason) category=$($recovered.category)"
if (-not $recovered.degraded -and $recovered.category) { Pass "熔断窗口结束后调用恢复正常" } else { Fail "熔断未恢复：$($recovered.reason)" }

Write-Host "===== 结果 ====="
Write-Host "PASS=$script:pass FAIL=$script:fail"
if ($script:fail -gt 0) { exit 1 }
