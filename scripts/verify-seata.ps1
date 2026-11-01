# Seata AT 故障注入验证：审核派单跨库事务在工单侧失败时必须整体回滚
$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$base = "http://127.0.0.1:8080"
$nacos = "http://127.0.0.1:8848"
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

function PostJson($url, $token, $body) {
    $json = $body | ConvertTo-Json -Depth 8 -Compress
    return Invoke-RestMethod -Uri $url -Method Post -Headers @{ Authorization = "Bearer $token" } `
        -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($json))
}

function SetFault($enabled) {
    $content = "demo.fault.create-workorder: $($enabled.ToString().ToLower())"
    $raw = curl.exe -s -X POST "http://127.0.0.1:8848/nacos/v1/cs/configs" --data-urlencode "dataId=workorder-service.yaml" --data-urlencode "group=CITY_PLATFORM" --data-urlencode "type=yaml" --data-urlencode "content=$content"
    if ($raw -notmatch "true") { throw "Nacos 配置发布失败：$raw" }
    Start-Sleep -Seconds 6
}

$png = Join-Path $env:TEMP "seata-test.png"
[System.IO.File]::WriteAllBytes($png, [Convert]::FromBase64String(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="))

Write-Host "===== 1. 准备待审核事件 ====="
$resident = Login "resident_demo" "Worker@123" "miniprogram"
$admin = Login "admin" "Admin@123" "admin-web"
$upload = (curl.exe -s -X POST "$base/api/v1/attachments" -H "Authorization: Bearer $resident" -F "file=@$png;type=image/png" -F "bizType=EVENT_REPORT") | ConvertFrom-Json
$event = PostJson "$base/api/v1/events" $resident @{
    description = "Seata 回滚验证事件：XX路人行道护栏松动，存在安全隐患，请尽快处置"
    categoryCode = "ROAD_FACILITY"; priority = "MEDIUM"
    longitude = 116.398; latitude = 39.915
    address = "北京市东城区XX路人行道"
    attachmentIds = @($upload.data.id)
}
if ($event.code -eq 0 -and $event.data.status -eq "PENDING_REVIEW") {
    Pass "事件已创建 eventId=$($event.data.id)"
} else {
    Fail "事件创建失败"; exit 1
}
$eventId = $event.data.id

Write-Host "===== 2. 开启工单故障开关 ====="
SetFault $true
Pass "已发布 demo.fault.create-workorder=true 并等待刷新"

Write-Host "===== 3. 审核派单（预期失败） ====="
$failed = PostJson "$base/api/v1/events/$eventId/approve" $admin @{
    workerId = 2; categoryCode = "ROAD_FACILITY"; priority = "HIGH"; areaCode = "110101"; remark = "Seata 故障注入"
}
if ($failed.code -ne 0) {
    Pass "审核按预期失败 code=$($failed.code) message=$($failed.message)"
} else {
    Fail "故障注入未生效，审核竟然成功"
}

Write-Host "===== 4. 校验事件状态未被修改 ====="
$detail = Invoke-RestMethod -Uri "$base/api/v1/events/$eventId" -Headers @{ Authorization = "Bearer $admin" }
if ($detail.data.status -eq "PENDING_REVIEW") {
    Pass "事件状态回滚为 PENDING_REVIEW"
} else {
    Fail "事件状态未回滚：$($detail.data.status)"
}

Write-Host "===== 5. 关闭故障开关并重新审核 ====="
SetFault $false
$ok = PostJson "$base/api/v1/events/$eventId/approve" $admin @{
    workerId = 2; categoryCode = "ROAD_FACILITY"; priority = "HIGH"; areaCode = "110101"; remark = "关闭故障后重新审核"
}
if ($ok.code -eq 0 -and $ok.data.workOrderNo) {
    Pass "重新审核成功 workOrderNo=$($ok.data.workOrderNo)"
} else {
    Fail "重新审核失败：$($ok | ConvertTo-Json -Depth 5)"
}

$after = Invoke-RestMethod -Uri "$base/api/v1/events/$eventId" -Headers @{ Authorization = "Bearer $admin" }
if ($after.data.status -eq "DISPATCHED") { Pass "事件状态 DISPATCHED" } else { Fail "事件状态异常：$($after.data.status)" }

Write-Host "===== 结果 ====="
Write-Host "PASS=$script:pass FAIL=$script:fail EVENT_ID=$eventId"
Remove-Item $png -ErrorAction SilentlyContinue
if ($script:fail -gt 0) { exit 1 }
