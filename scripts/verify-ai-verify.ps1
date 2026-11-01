# 验证 AI 视觉整改核实：事件上报 -> 审核派单 -> 接单 -> 提交整改（图片+文字） -> 自动核实 -> 管理员手动核实
# 用法：pwsh -File .\scripts\verify-ai-verify.ps1
param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$ImagePath = ""
)

$ErrorActionPreference = "Stop"
$pass = 0
$fail = 0

function Check($name, $ok, $detail = "") {
    if ($ok) {
        $script:pass++
        Write-Host "  PASS  $name $detail" -ForegroundColor Green
    } else {
        $script:fail++
        Write-Host "  FAIL  $name $detail" -ForegroundColor Red
    }
}

function Login($username, $password, $client = "miniprogram") {
    $body = @{ username = $username; password = $password; client = $client } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($body))
    if ($r.code -ne 0) { throw "登录失败: $username $($r.message)" }
    return $r.data.token
}

function Upload($token, $file, $bizType) {
    # 手工构造 multipart，确保文件 Content-Type 为 image/jpeg（-Form 默认 octet-stream 会被后端拒绝）
    $client = [System.Net.Http.HttpClient]::new()
    $client.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $token)
    $form = [System.Net.Http.MultipartFormDataContent]::new()
    $fileContent = [System.Net.Http.ByteArrayContent]::new([IO.File]::ReadAllBytes($file))
    $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::new("image/jpeg")
    $form.Add($fileContent, "file", [IO.Path]::GetFileName($file))
    $form.Add([System.Net.Http.StringContent]::new($bizType), "bizType")
    $resp = $client.PostAsync("$BaseUrl/api/v1/attachments", $form).Result
    $json = ($resp.Content.ReadAsStringAsync().Result) | ConvertFrom-Json
    $client.Dispose()
    if ($json.code -ne 0) { throw "上传失败: $($json.message)" }
    return $json.data.id
}

if (-not $ImagePath) {
    $candidates = Get-ChildItem "d:\Users\沈佳勇\Desktop\城建平台\data\uploads" -Recurse -File -Include *.jpg, *.png -ErrorAction SilentlyContinue
    if (-not $candidates) { throw "没有可用测试图片，请用 -ImagePath 指定" }
    $ImagePath = ($candidates | Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName
}
Write-Host "测试图片: $ImagePath" -ForegroundColor DarkGray

$resident = Login "resident_demo" "Worker@123" "miniprogram"
$worker = Login "worker_road" "Worker@123" "miniprogram"
$admin = Login "admin" "Admin@123" "admin-web"
Check "三个账号登录" $true

$eventImage = Upload $resident $ImagePath "EVENT_REPORT"
Check "事件照片上传" ($eventImage -gt 0) "attachmentId=$eventImage"

$eventBody = @{
    description   = "测试事件：公共照明路灯损坏，夜间照明缺失，存在安全隐患，请尽快处理。"
    categoryCode  = "PUBLIC_LIGHTING"
    priority      = "MEDIUM"
    longitude     = 116.3975
    latitude      = 39.9087
    address       = "北京市东城区测试路 1 号"
    attachmentIds = @($eventImage)
} | ConvertTo-Json
$event = Invoke-RestMethod -Uri "$BaseUrl/api/v1/events" -Method Post -Headers @{ Authorization = "Bearer $resident" } -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($eventBody))
if ($event.code -ne 0) { throw "创建事件失败: $($event.message)" }
$eventId = $event.data.id
Check "居民创建事件" ($eventId -gt 0) "eventId=$eventId eventNo=$($event.data.eventNo)"

$auditBody = @{ workerId = 2; categoryCode = "PUBLIC_LIGHTING"; priority = "MEDIUM"; remark = "信息完整，派单处理" } | ConvertTo-Json
$audit = Invoke-RestMethod -Uri "$BaseUrl/api/v1/events/$eventId/approve" -Method Post -Headers @{ Authorization = "Bearer $admin" } -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($auditBody))
if ($audit.code -ne 0) { throw "审核派单失败: $($audit.message)" }
$workOrderId = $audit.data.workOrderId
Check "管理员审核派单" ($workOrderId -gt 0) "workOrderId=$workOrderId workOrderNo=$($audit.data.workOrderNo)"

$accept = Invoke-RestMethod -Uri "$BaseUrl/api/v1/workorders/$workOrderId/accept" -Method Post -Headers @{ Authorization = "Bearer $worker" }
Check "处理人接单" ($accept.code -eq 0 -and $accept.data.status -eq "PROCESSING")

$resultImage = Upload $worker $ImagePath "WORKORDER_RESULT"
Check "整改照片上传" ($resultImage -gt 0) "attachmentId=$resultImage"

$submitBody = @{
    content       = "已完成路灯更换并恢复照明，现场照片见附件，请核验。"
    attachmentIds = @($resultImage)
} | ConvertTo-Json
$submit = Invoke-RestMethod -Uri "$BaseUrl/api/v1/workorders/$workOrderId/submit" -Method Post -Headers @{ Authorization = "Bearer $worker" } -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($submitBody))
Check "处理人提交整改结果" ($submit.code -eq 0 -and $submit.data.status -eq "PENDING_ACCEPTANCE")

Write-Host "  等待自动 AI 核实（最多 90 秒）..." -ForegroundColor DarkGray
$auto = $null
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 3
    $detail = Invoke-RestMethod -Uri "$BaseUrl/api/v1/workorders/$workOrderId" -Headers @{ Authorization = "Bearer $admin" }
    if ($detail.data.aiVerify) { $auto = $detail.data.aiVerify; break }
}
Check "提交后自动 AI 核实" ($null -ne $auto) $(if ($auto) { "conclusion=$($auto.conclusion) model=$($auto.model) degraded=$($auto.degraded)" } else { "90 秒内未生成结果" })

$manual = Invoke-RestMethod -Uri "$BaseUrl/api/v1/workorders/$workOrderId/ai-verify" -Method Post -Headers @{ Authorization = "Bearer $admin" }
Check "管理员手动 AI 核实" ($manual.code -eq 0 -and $manual.data.conclusion) "conclusion=$($manual.data.conclusion) confidence=$($manual.data.confidence) model=$($manual.data.model)"
if ($manual.data.reason) { Write-Host "  依据: $($manual.data.reason)" -ForegroundColor DarkGray }
if ($manual.data.issues -and $manual.data.issues.Count -gt 0) {
    Write-Host "  风险点: $($manual.data.issues -join '; ')" -ForegroundColor DarkGray
}

Write-Host ""
if ($fail -eq 0) {
    Write-Host "AI 整改核实验证：PASS=$pass FAIL=0" -ForegroundColor Green
    exit 0
} else {
    Write-Host "AI 整改核实验证：PASS=$pass FAIL=$fail" -ForegroundColor Red
    exit 1
}
