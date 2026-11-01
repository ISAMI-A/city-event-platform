# P3 端到端验收：居民上报 -> 管理员审核派单 -> 处理人接单提交 -> 管理员验收 -> 事件关闭
# 用法： & .\scripts\verify-p3.ps1
$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$base = "http://127.0.0.1:8080"
$script:pass = 0
$script:fail = 0
function Pass($msg) { Write-Host "[PASS] $msg"; $script:pass++ }
function Fail($msg) { Write-Host "[FAIL] $msg"; $script:fail++ }

function PostJson($url, $token, $body) {
    $json = $body | ConvertTo-Json -Depth 10 -Compress
    return Invoke-RestMethod -Uri $url -Method Post `
        -Headers @{ Authorization = "Bearer $token" } `
        -ContentType "application/json; charset=utf-8" `
        -Body ([System.Text.Encoding]::UTF8.GetBytes($json))
}

function GetJson($url, $token) {
    return Invoke-RestMethod -Uri $url -Headers @{ Authorization = "Bearer $token" }
}

function Login($username, $password, $client) {
    $response = Invoke-RestMethod -Uri "$base/api/v1/auth/login" -Method Post `
        -ContentType "application/json; charset=utf-8" `
        -Body ([System.Text.Encoding]::UTF8.GetBytes((@{
            username = $username; password = $password; client = $client
        } | ConvertTo-Json -Compress)))
    if ($response.code -ne 0) { throw "登录失败：$($response.message)" }
    return $response.data.token
}

function Upload($token, $path, $bizType) {
    $raw = curl.exe -s -X POST "$base/api/v1/attachments" `
        -H "Authorization: Bearer $token" `
        -F "file=@$path;type=image/png" -F "bizType=$bizType"
    $result = $raw | ConvertFrom-Json
    if ($result.code -ne 0) { throw "上传失败：$($result.message)" }
    return $result.data
}

$pngPath = Join-Path $env:TEMP "city-test-1x1.png"
[System.IO.File]::WriteAllBytes($pngPath, [Convert]::FromBase64String(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="))

Write-Host "===== 1. 三角色登录 ====="
$residentToken = Login "resident_demo" "Worker@123" "miniprogram"
$otherResidentToken = Login "resident_other" "Worker@123" "miniprogram"
$adminToken = Login "admin" "Admin@123" "admin-web"
$roadWorkerToken = Login "worker_road" "Worker@123" "miniprogram"
$lightingWorkerToken = Login "worker_lighting" "Worker@123" "miniprogram"
Pass "居民、管理员、两个处理人员登录成功"

Write-Host "===== 2. 上传照片 + AI 分析 ====="
$photo = Upload $residentToken $pngPath "EVENT_REPORT"
Pass "附件上传成功 id=$($photo.id) url=$($photo.accessUrl)"

$ai = PostJson "$base/api/v1/ai/analyze" $residentToken @{
    description = "XX路与YY街交叉口东北角人行道地砖破损约2平方米，行人容易绊倒，已持续三天，请尽快处理"
    address = "北京市东城区XX路与YY街交叉口"
}
if ($ai.code -eq 0 -and $ai.data.categoryCode) {
    Pass "AI 分析成功 category=$($ai.data.categoryCode) priority=$($ai.data.priority) degraded=$($ai.data.degraded)"
} else {
    Fail "AI 分析失败：$($ai | ConvertTo-Json -Depth 5)"
}

Write-Host "===== 3. 居民创建事件 ====="
$event = PostJson "$base/api/v1/events" $residentToken @{
    title = "人行道地砖破损"
    description = "XX路与YY街交叉口东北角人行道地砖破损约2平方米，行人容易绊倒，已持续三天，请尽快处理"
    categoryCode = "ROAD_FACILITY"
    priority = "MEDIUM"
    longitude = 116.397128
    latitude = 39.916527
    address = "北京市东城区XX路与YY街交叉口"
    attachmentIds = @($photo.id)
    aiCallId = $ai.data.callId
}
if ($event.code -eq 0 -and $event.data.status -eq "PENDING_REVIEW") {
    Pass "事件创建成功 eventId=$($event.data.id) eventNo=$($event.data.eventNo)"
} else {
    Fail "事件创建失败：$($event | ConvertTo-Json -Depth 6)"
    exit 1
}
$eventId = $event.data.id

$myList = GetJson "$base/api/v1/events/my?page=1&size=10" $residentToken
if ($myList.code -eq 0 -and ($myList.data.records | Where-Object { $_.id -eq $eventId })) {
    Pass "居民事件列表包含本人事件，total=$($myList.data.total)"
} else {
    Fail "居民事件列表异常"
}

Write-Host "===== 4. 数据权限：其他居民不可查看 ====="
try {
    $forbidden = GetJson "$base/api/v1/events/$eventId" $otherResidentToken
    if ($forbidden.code -eq 40302) { Pass "其他居民查看他人事件被拒绝（40302）" }
    else { Fail "越权校验异常：$($forbidden | ConvertTo-Json -Depth 4)" }
} catch {
    Fail "越权请求异常：$($_.Exception.Message)"
}

Write-Host "===== 5. 管理员审核并派单 ====="
$approve = PostJson "$base/api/v1/events/$eventId/approve" $adminToken @{
    workerId = 2
    categoryCode = "ROAD_FACILITY"
    priority = "HIGH"
    areaCode = "110101"
    remark = "现场核实后派市政道路班组"
}
if ($approve.code -eq 0 -and $approve.data.workOrderId) {
    Pass "审核通过并创建工单 workOrderNo=$($approve.data.workOrderNo)"
} else {
    Fail "审核失败：$($approve | ConvertTo-Json -Depth 6)"
    exit 1
}
$workOrderId = $approve.data.workOrderId

$eventAfterApprove = GetJson "$base/api/v1/events/$eventId" $adminToken
if ($eventAfterApprove.data.status -eq "DISPATCHED") { Pass "事件状态变为 DISPATCHED" }
else { Fail "事件状态异常：$($eventAfterApprove.data.status)" }

Write-Host "===== 6. 工单数据权限 ====="
try {
    $otherWorker = GetJson "$base/api/v1/workorders/$workOrderId" $lightingWorkerToken
    if ($otherWorker.code -eq 13003) { Pass "其他处理人查看工单被拒绝（13003）" }
    else { Fail "工单越权校验异常：$($otherWorker | ConvertTo-Json -Depth 4)" }
} catch {
    Fail "工单越权请求异常：$($_.Exception.Message)"
}

Write-Host "===== 7. 处理人接单 ====="
$accept = PostJson "$base/api/v1/workorders/$workOrderId/accept" $roadWorkerToken @{}
if ($accept.code -eq 0 -and $accept.data.status -eq "PROCESSING") {
    Pass "接单成功，工单状态 PROCESSING"
} else {
    Fail "接单失败：$($accept | ConvertTo-Json -Depth 6)"
}

$repeatAccept = PostJson "$base/api/v1/workorders/$workOrderId/accept" $roadWorkerToken @{}
if ($repeatAccept.code -eq 13002) { Pass "重复接单被拒绝（13002）" }
else { Fail "重复接单未拦截：$($repeatAccept | ConvertTo-Json -Depth 4)" }

Start-Sleep -Seconds 3
$eventAfterAccept = GetJson "$base/api/v1/events/$eventId" $adminToken
if ($eventAfterAccept.data.status -eq "PROCESSING") { Pass "MQ 同步事件状态为 PROCESSING" }
else { Fail "事件状态未同步：$($eventAfterAccept.data.status)" }

Write-Host "===== 8. 处理人提交结果 ====="
$process = PostJson "$base/api/v1/workorders/$workOrderId/process" $roadWorkerToken @{
    content = "已到场设置围挡，准备更换地砖"
}
if ($process.code -eq 0) { Pass "处理记录已保存" } else { Fail "处理记录失败" }

$resultPhoto = Upload $roadWorkerToken $pngPath "WORKORDER_RESULT"
$submit = PostJson "$base/api/v1/workorders/$workOrderId/submit" $roadWorkerToken @{
    content = "已更换破损地砖并检查周边路基，现场已清理"
    attachmentIds = @($resultPhoto.id)
}
if ($submit.code -eq 0 -and $submit.data.status -eq "PENDING_ACCEPTANCE") {
    Pass "提交结果成功，工单状态 PENDING_ACCEPTANCE"
} else {
    Fail "提交结果失败：$($submit | ConvertTo-Json -Depth 6)"
}

Start-Sleep -Seconds 3
$eventAfterSubmit = GetJson "$base/api/v1/events/$eventId" $adminToken
if ($eventAfterSubmit.data.status -eq "PENDING_ACCEPTANCE") { Pass "MQ 同步事件状态为 PENDING_ACCEPTANCE" }
else { Fail "事件状态未同步：$($eventAfterSubmit.data.status)" }

Write-Host "===== 9. 管理员验收通过 ====="
$acceptance = PostJson "$base/api/v1/workorders/$workOrderId/acceptance" $adminToken @{
    result = "PASS"
    remark = "现场核实已修复"
}
if ($acceptance.code -eq 0 -and $acceptance.data.status -eq "COMPLETED") {
    Pass "验收通过，工单状态 COMPLETED"
} else {
    Fail "验收失败：$($acceptance | ConvertTo-Json -Depth 6)"
}

Start-Sleep -Seconds 3
$eventClosed = GetJson "$base/api/v1/events/$eventId" $adminToken
if ($eventClosed.data.status -eq "CLOSED") {
    Pass "事件已关闭（CLOSED），闭环完成"
} else {
    Fail "事件未关闭：$($eventClosed.data.status)"
}

Write-Host "===== 10. 驳回流程 ====="
$photo2 = Upload $residentToken $pngPath "EVENT_REPORT"
$event2 = PostJson "$base/api/v1/events" $residentToken @{
    description = "XX公园南门路灯连续三晚不亮，整段约200米，夜间行人看不清路面"
    categoryCode = "PUBLIC_LIGHTING"
    priority = "MEDIUM"
    longitude = 116.401
    latitude = 39.912
    address = "北京市东城区XX公园南门"
    attachmentIds = @($photo2.id)
}
$reject = PostJson "$base/api/v1/events/$($event2.data.id)/reject" $adminToken @{
    reason = "照片不清晰，请补充路灯编号信息"
}
if ($reject.code -eq 0) { Pass "事件驳回成功" } else { Fail "驳回失败" }
$rejected = GetJson "$base/api/v1/events/$($event2.data.id)" $adminToken
if ($rejected.data.status -eq "REJECTED") { Pass "驳回后状态 REJECTED" } else { Fail "驳回状态异常" }

Write-Host "===== 11. 附件与数据库校验 ====="
$attachments = GetJson "$base/api/v1/attachments/batch?ids=$($photo.id)" $residentToken
if ($attachments.code -eq 0 -and $attachments.data[0].accessUrl) { Pass "附件元数据可查询" } else { Fail "附件元数据异常" }

Write-Host "===== 结果 ====="
Write-Host "PASS=$script:pass FAIL=$script:fail"
Remove-Item $pngPath -ErrorAction SilentlyContinue
if ($script:fail -gt 0) { exit 1 }
