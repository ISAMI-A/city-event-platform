﻿# 重置演示数据：清空 4 个业务库 + 上传目录，并重建完整演示数据
# 用法：.\scripts\reset-demo.ps1            （复制图片 + 重置数据库 + 清缓存）
#      .\scripts\reset-demo.ps1 -SkipImages （只重置数据库）
param(
    [switch]$SkipImages
)

$ErrorActionPreference = 'Stop'

$ROOT = Split-Path -Parent $PSScriptRoot
$SRC = 'D:\Users\沈佳勇\Desktop\AIGC Detection\data\test_images\testss'
$UPLOAD = Join-Path $ROOT 'data\uploads'
$DEMO_DIR = Join-Path $UPLOAD 'demo'

# 容器名 -> docker 可执行文件
$dockerCandidates = @(
    (Join-Path $env:LOCALAPPDATA 'Programs\DockerDesktop\resources\bin\docker.exe'),
    'docker'
)
$docker = $null
foreach ($candidate in $dockerCandidates) {
    if ($candidate -eq 'docker') {
        if (Get-Command docker -ErrorAction SilentlyContinue) { $docker = 'docker'; break }
    } elseif (Test-Path $candidate) {
        $docker = $candidate
        break
    }
}
if (-not $docker) {
    throw '找不到 docker 命令，请确认 Docker Desktop 已启动'
}

# 图片映射：源文件 -> 演示存储名
$IMAGE_MAP = @(
    @('12.png', 'demo-ev01.png'),
    @('20.png', 'demo-ev02.png'),
    @('18.png', 'demo-ev03.png'),
    @('17.png', 'demo-ev04.png'),
    @('11.png', 'demo-ev05.png'),
    @('26.png', 'demo-ev06.png'),
    @('0.png', 'demo-ev07.png'),
    @('2.png', 'demo-ev08.png'),
    @('6.png', 'demo-ev09.png'),
    @('10.png', 'demo-ev10.png'),
    @('8.png', 'demo-ev11.png'),
    @('13.png', 'demo-ev12.png'),
    @('16.png', 'demo-ev13.png'),
    @('26.png', 'demo-wo02-process.png'),
    @('10.png', 'demo-wo06-process.png'),
    @('0p.jpg', 'demo-wo07-result.jpg'),
    @('2p.jpg', 'demo-wo08-result.jpg'),
    @('2.png', 'demo-wo08-process.png'),
    @('6p.jpg', 'demo-wo09-result.jpg'),
    @('10p.jpg', 'demo-wo10-result.jpg'),
    @('0p.jpg', 'demo-wo13-result.jpg'),
    @('13.png', 'demo-wo12-process.png')
)

Write-Host '[1/4] 清理上传目录 ...' -ForegroundColor Yellow
if (-not $SkipImages) {
    if (Test-Path $UPLOAD) {
        Get-ChildItem $UPLOAD -Force | Remove-Item -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $DEMO_DIR | Out-Null

    Write-Host '[2/4] 复制演示图片 ...' -ForegroundColor Yellow
    foreach ($pair in $IMAGE_MAP) {
        $source = Join-Path $SRC $pair[0]
        if (-not (Test-Path $source)) {
            throw "缺少参考图片：$source"
        }
        Copy-Item $source (Join-Path $DEMO_DIR $pair[1]) -Force
    }
    Write-Host ("  已复制 {0} 张图片到 {1}" -f $IMAGE_MAP.Count, $DEMO_DIR) -ForegroundColor Green
} else {
    Write-Host '  已跳过图片复制' -ForegroundColor DarkGray
}

Write-Host '[3/4] 重置数据库 ...' -ForegroundColor Yellow
$sql = 'source /docker-entrypoint-initdb.d/demo/reset-demo-data.sql'
& $docker exec city-mysql mysql -uroot -p1234 --default-character-set=utf8mb4 -e $sql
if ($LASTEXITCODE -ne 0) {
    throw "数据库重置失败（退出码 $LASTEXITCODE）"
}

Write-Host '[4/4] 清理 Redis 缓存与会话 ...' -ForegroundColor Yellow
$names = & $docker ps --format '{{.Names}}'
if ($names -contains 'city-redis') {
    & $docker exec city-redis redis-cli FLUSHALL | Out-Null
} else {
    & redis-cli -h 127.0.0.1 -p 6379 FLUSHALL 2>$null | Out-Null
}

$counts = & $docker exec city-mysql mysql -uroot -p1234 -N --default-character-set=utf8mb4 -e @'
SELECT CONCAT('用户=', (SELECT COUNT(*) FROM city_user.t_user),
       ' 事件=', (SELECT COUNT(*) FROM city_event.t_event),
       ' 工单=', (SELECT COUNT(*) FROM city_workorder.t_work_order),
       ' 附件=', (SELECT COUNT(*) FROM city_event.t_attachment));
'@
Write-Host ("  完成：{0}" -f ($counts | Where-Object { $_ -match '用户=' })) -ForegroundColor Green
Write-Host '  演示账号：admin / Admin@123' -ForegroundColor Cyan
Write-Host '  组长：worker_road_leader / worker_lighting_leader / worker_sanitation_leader / worker_facility_leader，密码 Worker@123' -ForegroundColor Cyan
Write-Host '  组员：worker_road_01 等，密码 Worker@123；居民：resident_fuzhou 等，密码 Resident@123' -ForegroundColor Cyan
