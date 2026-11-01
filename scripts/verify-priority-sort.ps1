# 验证事件/工单列表按优先级排序（URGENT > HIGH > MEDIUM > LOW）
param([string]$BaseUrl = "http://127.0.0.1:8080")
$ErrorActionPreference = "Stop"

function Login($u, $p, $c = "miniprogram") {
    $body = @{ username = $u; password = $p; client = $c } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($body))
    if ($r.code -ne 0) { throw "登录失败: $u" }
    return $r.data.token
}

function Upload($token, $file, $bizType) {
    $client = [System.Net.Http.HttpClient]::new()
    $client.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $token)
    $form = [System.Net.Http.MultipartFormDataContent]::new()
    $fc = [System.Net.Http.ByteArrayContent]::new([IO.File]::ReadAllBytes($file))
    $fc.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::new("image/jpeg")
    $form.Add($fc, "file", [IO.Path]::GetFileName($file))
    $form.Add([System.Net.Http.StringContent]::new($bizType), "bizType")
    $resp = $client.PostAsync("$BaseUrl/api/v1/attachments", $form).Result
    $json = $resp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    $client.Dispose()
    if ($json.code -ne 0) { throw "上传失败: $($json.message)" }
    return $json.data.id
}

$img = (Get-ChildItem "d:\Users\沈佳勇\Desktop\城建平台\data\uploads" -Recurse -File -Include *.jpg, *.png | Select-Object -First 1).FullName
if (-not $img) { throw "没有测试图片" }
$resident = Login "resident_demo" "Worker@123"
$admin = Login "admin" "Admin@123" "admin-web"

$priorities = @("LOW", "MEDIUM", "HIGH", "URGENT")
$eventIds = @()
$eventNos = @()
foreach ($p in $priorities) {
    $aid = Upload $resident $img "EVENT_REPORT"
    $body = @{
        description   = "排序验证事件：$p 优先级，公共照明路灯损坏需要处理。"
        categoryCode  = "PUBLIC_LIGHTING"
        priority      = $p
        longitude     = 116.3975
        latitude      = 39.9087
        address       = "北京市东城区排序测试路"
        attachmentIds = @($aid)
    } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/v1/events" -Method Post -Headers @{ Authorization = "Bearer $resident" } -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($body))
    if ($r.code -ne 0) { throw "创建事件失败: $($r.message)" }
    $eventIds += $r.data.id
    $eventNos += $r.data.eventNo
}
Write-Host "已创建事件: $($eventIds -join ',')" -ForegroundColor DarkGray

$desc = Invoke-RestMethod -Uri "$BaseUrl/api/v1/events?page=1&size=50&sortBy=priority&order=desc" -Headers @{ Authorization = "Bearer $admin" }
$descOrder = @($desc.data.records | Where-Object { $eventNos -contains $_.eventNo } | ForEach-Object { $_.priority })
$asc = Invoke-RestMethod -Uri "$BaseUrl/api/v1/events?page=1&size=50&sortBy=priority&order=asc" -Headers @{ Authorization = "Bearer $admin" }
$ascOrder = @($asc.data.records | Where-Object { $eventNos -contains $_.eventNo } | ForEach-Object { $_.priority })
Write-Host "事件降序: $($descOrder -join ' > ')" -ForegroundColor DarkGray
Write-Host "事件升序: $($ascOrder -join ' > ')" -ForegroundColor DarkGray

$ok1 = ($descOrder -join ",") -eq "URGENT,HIGH,MEDIUM,LOW"
$ok2 = ($ascOrder -join ",") -eq "LOW,MEDIUM,HIGH,URGENT"
if ($ok1 -and $ok2) { Write-Host "  PASS 事件按优先级排序" -ForegroundColor Green } else { Write-Host "  FAIL 事件排序" -ForegroundColor Red }

# 为 3 条事件派单，验证工单排序
$workerIds = @(2, 3, 4)
$workOrderIds = @()
$workOrderNos = @()
for ($i = 0; $i -lt 3; $i++) {
    $body = @{ workerId = $workerIds[$i]; categoryCode = "PUBLIC_LIGHTING"; priority = $priorities[$i + 1]; remark = "排序验证派单" } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/v1/events/$($eventIds[$i + 1])/approve" -Method Post -Headers @{ Authorization = "Bearer $admin" } -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($body))
    if ($r.code -ne 0) { throw "派单失败: $($r.message)" }
    $workOrderIds += $r.data.workOrderId
    $workOrderNos += $r.data.workOrderNo
}
Write-Host "已创建工单: $($workOrderIds -join ',')" -ForegroundColor DarkGray

$wdesc = Invoke-RestMethod -Uri "$BaseUrl/api/v1/workorders?page=1&size=50&sortBy=priority&order=desc" -Headers @{ Authorization = "Bearer $admin" }
$wOrder = @($wdesc.data.records | Where-Object { $workOrderNos -contains $_.workOrderNo } | ForEach-Object { $_.priority })
Write-Host "工单降序: $($wOrder -join ' > ')" -ForegroundColor DarkGray
$ok3 = ($wOrder -join ",") -eq "URGENT,HIGH,MEDIUM"
if ($ok3) { Write-Host "  PASS 工单按优先级排序" -ForegroundColor Green } else { Write-Host "  FAIL 工单排序" -ForegroundColor Red }

Write-Host "EVENT_IDS=$($eventIds -join ',')"
Write-Host "WORKORDER_IDS=$($workOrderIds -join ',')"
if ($ok1 -and $ok2 -and $ok3) { exit 0 } else { exit 1 }
