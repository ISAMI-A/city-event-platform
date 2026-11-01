# ============================================
# 城市事件智能上报及工单调度平台 - 一键启动器
# ============================================
# 使用方法（建议 PowerShell 7）：
#   1. .\start.ps1                  智能启动：基础设施 + 按需打包 + 6 个微服务
#   2. .\start.ps1 -NoBuild         跳过打包，直接用现有 jar 启动
#   3. .\start.ps1 -ForceBuild      强制重新打包
#   4. .\start.ps1 -SkipInfra       跳过 Docker 基础设施（已自行启动 MySQL/Redis/Nacos/RabbitMQ/Seata）
#   5. .\start.ps1 -StopInfra       停止服务时一并停止 Docker 容器（默认保留，下次启动更快）
#   6. 按 Ctrl+C 停止本次启动的所有 Java 服务
#
# 访问入口：
#   管理后台 http://127.0.0.1:8092/ （admin / Admin@123）
#   网关     http://127.0.0.1:8080
#   Nacos    http://127.0.0.1:8848/nacos
#   RabbitMQ http://127.0.0.1:15672 （账号见 deploy/.env）
#   Seata    http://127.0.0.1:7091
# ============================================

param(
    [switch]$NoBuild,      # 跳过打包
    [switch]$ForceBuild,   # 强制重新打包
    [switch]$SkipInfra,    # 跳过 Docker 基础设施
    [switch]$StopInfra     # 退出时停止 Docker 容器
)

$ErrorActionPreference = "Continue"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "    城市事件平台 - 一键启动器" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

$ROOT = $PSScriptRoot
Set-Location $ROOT

# ─── 0. 加载 deploy/.env（密钥、数据库密码等统一外置，不硬编码） ───
# 已存在的环境变量优先，方便从外部临时覆盖
$envFile = Join-Path $ROOT "deploy\.env"
if (Test-Path $envFile) {
    Write-Host "[env] 加载 $envFile ..." -ForegroundColor DarkGray
    foreach ($line in Get-Content $envFile) {
        $line = $line.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { continue }
        $idx = $line.IndexOf("=")
        if ($idx -le 0) { continue }
        $key = $line.Substring(0, $idx).Trim()
        $val = $line.Substring($idx + 1).Trim()
        if ($val.Length -ge 2 -and (($val.StartsWith('"') -and $val.EndsWith('"')) -or ($val.StartsWith("'") -and $val.EndsWith("'")))) {
            $val = $val.Substring(1, $val.Length - 2)
        }
        if ($key -ne "" -and $null -eq [Environment]::GetEnvironmentVariable($key, "Process")) {
            [Environment]::SetEnvironmentVariable($key, $val, "Process")
        }
    }
} else {
    Write-Host "[env] 未找到 $envFile，服务将使用代码内默认值（缺少 LLM/地图 Key 时相关功能降级）" -ForegroundColor Yellow
}

$LOG_DIR = Join-Path $ROOT "logs"
if (-not (Test-Path $LOG_DIR)) { New-Item -ItemType Directory -Path $LOG_DIR | Out-Null }

# ─── 工具函数 ───
function Test-PortOpen($Port) {
    # 同时尝试 IPv4/IPv6 回环，避免服务只监听 ::1 时误判
    $candidates = @(
        @{ Family = [System.Net.Sockets.AddressFamily]::InterNetwork;   Host = [System.Net.IPAddress]::Loopback },
        @{ Family = [System.Net.Sockets.AddressFamily]::InterNetworkV6; Host = [System.Net.IPAddress]::IPv6Loopback }
    )
    foreach ($cfg in $candidates) {
        $tcp = New-Object System.Net.Sockets.TcpClient -ArgumentList $cfg.Family
        try {
            $async = $tcp.BeginConnect($cfg.Host, $Port, $null, $null)
            $ok = $async.AsyncWaitHandle.WaitOne(800)
            if ($ok) { $tcp.EndConnect($async); return $true }
        } catch {
            # 当前地址族不可用，尝试下一个
        } finally {
            $tcp.Close()
        }
    }
    return $false
}

function Stop-PortOwner($Port) {
    # 只结束 java 进程，避免误杀 Docker 的端口代理或其他程序
    $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (-not $conns) { return }
    foreach ($ownerId in ($conns | Select-Object -ExpandProperty OwningProcess -Unique)) {
        $proc = Get-Process -Id $ownerId -ErrorAction SilentlyContinue
        if ($proc -and $proc.ProcessName -eq "java") {
            Write-Host "  端口 $Port 被 java (PID $ownerId) 占用，结束旧进程..." -ForegroundColor Yellow
            & taskkill /PID $ownerId /T /F 2>$null | Out-Null
        }
    }
    Start-Sleep -Milliseconds 600
}

function Wait-Port($Port, $TimeoutSec, $Label) {
    for ($i = 0; $i -lt $TimeoutSec; $i++) {
        if (Test-PortOpen $Port) { return $true }
        Start-Sleep -Seconds 1
    }
    Write-Host "  $Label 端口 $Port 等待超时（$TimeoutSec 秒）" -ForegroundColor Yellow
    return $false
}

function Get-ServiceJar($Module) {
    Get-ChildItem "$ROOT\$Module\target\$Module-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike "*.original" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Test-SrcNewerThan($JarTime) {
    # 任一模块源码/pom 比 jar 新即需要重新打包（common 变更会影响所有服务）
    $moduleDirs = @("common", "monitor-service", "user-service", "event-service", "workorder-service", "ai-service", "map-service", "gateway-service")
    $items = @(Get-Item "$ROOT\pom.xml" -ErrorAction SilentlyContinue)
    foreach ($m in $moduleDirs) {
        $items += Get-Item "$ROOT\$m\pom.xml" -ErrorAction SilentlyContinue
        $items += Get-Item "$ROOT\$m\src" -ErrorAction SilentlyContinue
    }
    foreach ($item in $items) {
        if (-not $item) { continue }
        if ($item.PSIsContainer) {
            $newest = Get-ChildItem $item.FullName -Recurse -File -ErrorAction SilentlyContinue |
                Sort-Object LastWriteTime -Descending | Select-Object -First 1
            if ($newest -and $newest.LastWriteTime -gt $JarTime) { return $true }
        } elseif ($item.LastWriteTime -gt $JarTime) {
            return $true
        }
    }
    return $false
}

# 服务定义：启动顺序即数组顺序，网关最后启动（依赖下游服务注册）
$SERVICES = @(
    @{ Name = "monitor-service";   Module = "monitor-service";   Port = 8090; Label = "监控面板" },
    @{ Name = "user-service";      Module = "user-service";      Port = 8081; Label = "用户服务" },
    @{ Name = "event-service";     Module = "event-service";     Port = 8082; Label = "事件服务" },
    @{ Name = "workorder-service"; Module = "workorder-service"; Port = 8083; Label = "工单服务" },
    @{ Name = "ai-service";        Module = "ai-service";        Port = 8084; Label = "AI 服务" },
    @{ Name = "map-service";       Module = "map-service";       Port = 8085; Label = "地图服务" },
    @{ Name = "gateway-service";   Module = "gateway-service";   Port = 8080; Label = "网关" }
)

$procList = @()
$docker = $null
$infraStarted = $false

try {
    # ─── 1. 环境检查 ───
    Write-Host "[1/5] 环境检查 ..." -ForegroundColor Yellow

    $javaCmd = (Get-Command java -ErrorAction SilentlyContinue).Source
    if (-not $javaCmd) {
        Write-Host "  找不到 java，请确认 JDK 17 已安装并在 PATH 中" -ForegroundColor Red
        exit 1
    }

    $mvnCmd = "D:\apache-maven-3.9.9\bin\mvn.cmd"
    if (-not (Test-Path $mvnCmd)) {
        $mvnCmd = (Get-Command mvn.cmd -ErrorAction SilentlyContinue).Source
    }

    # Docker CLI：优先 PATH，其次 Docker Desktop 默认安装位置
    $docker = (Get-Command docker -ErrorAction SilentlyContinue).Source
    if (-not $docker) {
        $dockerCandidate = Join-Path $env:LOCALAPPDATA "Programs\DockerDesktop\resources\bin\docker.exe"
        if (Test-Path $dockerCandidate) { $docker = $dockerCandidate }
    }
    if (-not $docker) {
        $dockerCandidate = "C:\Program Files\Docker\Docker\resources\bin\docker.exe"
        if (Test-Path $dockerCandidate) { $docker = $dockerCandidate }
    }

    # ─── 2. 启动 Docker 基础设施 ───
    if ($SkipInfra) {
        Write-Host "[2/5] 跳过基础设施（-SkipInfra）" -ForegroundColor DarkGray
    } elseif (-not $docker) {
        Write-Host "[2/5] 未找到 docker，跳过基础设施；如需数据库等请自行启动或安装 Docker Desktop" -ForegroundColor Yellow
    } else {
        Write-Host "[2/5] 检查 Docker 基础设施 ..." -ForegroundColor Yellow
        & $docker info 2>$null | Out-Null
        if ($LASTEXITCODE -ne 0) {
            $ddExe = Join-Path $env:LOCALAPPDATA "Programs\DockerDesktop\Docker Desktop.exe"
            if (Test-Path $ddExe) {
                Write-Host "  Docker 未运行，正在启动 Docker Desktop（最多等待 120 秒）..." -ForegroundColor Yellow
                Start-Process -FilePath $ddExe | Out-Null
                for ($i = 0; $i -lt 120; $i++) {
                    Start-Sleep -Seconds 1
                    & $docker info 2>$null | Out-Null
                    if ($LASTEXITCODE -eq 0) { break }
                }
            }
            & $docker info 2>$null | Out-Null
            if ($LASTEXITCODE -ne 0) {
                Write-Host "  Docker Desktop 未就绪：可用 -SkipInfra 跳过，或手动启动后重试" -ForegroundColor Red
                exit 1
            }
        }

        $composeArgs = @("compose", "-f", "$ROOT\deploy\docker-compose.yml", "--env-file", "$envFile", "up", "-d")
        # 6379 已被占用（本机 Redis 或已运行的 city-redis）时不再拉起容器，避免端口冲突
        if (-not (Test-PortOpen 6379)) {
            $composeArgs += "redis"
        } else {
            Write-Host "  [infra] 6379 已在监听，复用现有 Redis，不启动 city-redis 容器" -ForegroundColor DarkGray
        }
        $composeArgs += @("mysql", "nacos", "rabbitmq", "seata", "admin-web")

        & $docker @composeArgs
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  基础设施启动失败（docker compose exit $LASTEXITCODE）" -ForegroundColor Red
            exit 1
        }
        $infraStarted = $true

        $mysqlPort = if ($env:MYSQL_HOST_PORT) { [int]$env:MYSQL_HOST_PORT } else { 3307 }
        $redisPort = if ($env:REDIS_PORT) { [int]$env:REDIS_PORT } else { 6379 }
        $rabbitPort = if ($env:RABBITMQ_PORT) { [int]$env:RABBITMQ_PORT } else { 5672 }
        $seataPort = if ($env:SEATA_PORT) { [int]$env:SEATA_PORT } else { 8091 }
        Write-Host "  等待基础设施就绪 ..." -ForegroundColor DarkGray
        if (-not (Wait-Port $mysqlPort 90 "MySQL")) { exit 1 }
        if (-not (Wait-Port $redisPort 30 "Redis")) { exit 1 }
        if (-not (Wait-Port $rabbitPort 60 "RabbitMQ")) { exit 1 }
        if (-not (Wait-Port $seataPort 90 "Seata")) { exit 1 }
        if (-not (Wait-Port 8848 90 "Nacos")) { exit 1 }
        try {
            Invoke-WebRequest -Uri "http://127.0.0.1:8848/nacos/v1/console/health/readiness" -TimeoutSec 5 -UseBasicParsing | Out-Null
        } catch {
            Write-Host "  Nacos 健康检查失败，请稍后重试或查看容器日志" -ForegroundColor Yellow
        }
        Write-Host "  基础设施就绪" -ForegroundColor Green
    }

    # ─── 3. 智能打包 ───
    if ($NoBuild) {
        Write-Host "[3/5] 跳过打包（-NoBuild）" -ForegroundColor DarkGray
    } else {
        $jarTimes = @()
        $missingJar = $false
        foreach ($svc in $SERVICES) {
            $jar = Get-ServiceJar $svc.Module
            if (-not $jar) { $missingJar = $true; break }
            $jarTimes += $jar.LastWriteTime
        }
        $needBuild = $ForceBuild -or $missingJar
        if (-not $needBuild -and $jarTimes.Count -gt 0) {
            $oldestJarTime = ($jarTimes | Sort-Object | Select-Object -First 1)
            $needBuild = Test-SrcNewerThan $oldestJarTime
        }

        if ($needBuild) {
            if (-not $mvnCmd) {
                Write-Host "  找不到 Maven，无法打包；如已有 jar 可用 -NoBuild 启动" -ForegroundColor Red
                exit 1
            }
            if ($ForceBuild) {
                Write-Host "[3/5] 强制重新打包（-ForceBuild）..." -ForegroundColor Yellow
            } elseif ($missingJar) {
                Write-Host "[3/5] 存在模块缺少 jar，开始打包 ..." -ForegroundColor Yellow
            } else {
                Write-Host "[3/5] 检测到源码变更，重新打包 ..." -ForegroundColor Yellow
            }
            # 先停旧服务释放 jar 文件锁，否则 repackage 报 Unable to rename
            foreach ($svc in $SERVICES) { Stop-PortOwner $svc.Port }
            Push-Location $ROOT
            try {
                & $mvnCmd -B -DskipTests package
                $buildCode = $LASTEXITCODE
            } finally {
                Pop-Location
            }
            if ($buildCode -ne 0) {
                Write-Host "  打包失败（exit $buildCode），请手动执行: mvn -B -DskipTests package 排查" -ForegroundColor Red
                exit 1
            }
            Write-Host "  打包完成" -ForegroundColor Green
        } else {
            Write-Host "[3/5] 源码无变更，跳过打包（jar 已是最新）" -ForegroundColor DarkGray
        }
    }

    foreach ($svc in $SERVICES) {
        if (-not (Get-ServiceJar $svc.Module)) {
            Write-Host "  找不到 $($svc.Name) 的 jar，请先执行: mvn -B -DskipTests package" -ForegroundColor Red
            exit 1
        }
    }

    # ─── 4. 启动微服务 ───
    Write-Host "[4/5] 启动 6 个微服务 ..." -ForegroundColor Yellow
    foreach ($svc in $SERVICES) {
        $jar = Get-ServiceJar $svc.Module
        Stop-PortOwner $svc.Port
        $outLog = Join-Path $LOG_DIR "$($svc.Name).log"
        $errLog = Join-Path $LOG_DIR "$($svc.Name).err.log"
        $proc = Start-Process -FilePath $javaCmd `
            -ArgumentList @("-jar", "`"$($jar.FullName)`"") `
            -WorkingDirectory $ROOT -NoNewWindow -PassThru `
            -RedirectStandardOutput $outLog -RedirectStandardError $errLog
        $procList += $proc

        $ok = $false
        for ($i = 0; $i -lt 75; $i++) {
            Start-Sleep -Seconds 1
            if ($proc.HasExited) { break }
            if (Test-PortOpen $svc.Port) { $ok = $true; break }
        }
        if ($ok) {
            Write-Host "  $($svc.Label) 已启动: http://127.0.0.1:$($svc.Port)  日志 logs\$($svc.Name)\$($svc.Name).log" -ForegroundColor Green
        } else {
            Write-Host "  $($svc.Label) 启动失败或超时，请查看 logs\$($svc.Name)\$($svc.Name).log 与 logs\$($svc.Name).err.log" -ForegroundColor Red
            Write-Host "  已启动的服务将保留，按 Ctrl+C 可统一停止" -ForegroundColor Yellow
            break
        }
    }

    # 网关健康检查（路由到 user-service 的 ping）
    # 网关端口监听后仍需等待下游服务注册与路由刷新，这里最多重试 30 秒
    if (Test-PortOpen 8080) {
        $gatewayOk = $false
        for ($i = 0; $i -lt 15; $i++) {
            try {
                $ping = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/system/ping" -TimeoutSec 5
                if ($ping.code -eq 0) {
                    Write-Host "  网关转发正常（ping -> $($ping.data.service)）" -ForegroundColor Green
                    $gatewayOk = $true
                    break
                }
            } catch {
                # 下游尚未注册完成，继续重试
            }
            Start-Sleep -Seconds 2
        }
        if (-not $gatewayOk) {
            Write-Host "  网关已监听但 /api/v1/system/ping 暂不可用，请查看 logs\gateway-service.log 与下游服务日志" -ForegroundColor Yellow
        }
    }

    # ─── 5. 输出访问信息并保持运行 ───
    Write-Host "[5/5] 启动完成" -ForegroundColor Green
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "  管理后台  http://127.0.0.1:8092/  (admin / Admin@123)" -ForegroundColor White
    Write-Host "  监控面板  http://127.0.0.1:8090  (SBA_USER / SBA_PASSWORD)" -ForegroundColor White
    Write-Host "  网关      http://127.0.0.1:8080" -ForegroundColor White
    Write-Host "  Nacos     http://127.0.0.1:8848/nacos" -ForegroundColor White
    Write-Host "  RabbitMQ  http://127.0.0.1:15672" -ForegroundColor White
    Write-Host "  Seata     http://127.0.0.1:7091" -ForegroundColor White
    Write-Host "  日志目录  $LOG_DIR" -ForegroundColor White
    Write-Host "  按 Ctrl+C 停止所有服务" -ForegroundColor White
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host ""

    while ($true) {
        Start-Sleep -Seconds 2
        $exited = $procList | Where-Object { $_.HasExited } | Select-Object -First 1
        if ($exited) {
            Write-Host "检测到服务进程退出（PID $($exited.Id)），正在停止其余服务..." -ForegroundColor Red
            break
        }
    }
} finally {
    Write-Host ""
    Write-Host "正在停止服务 ..." -ForegroundColor Yellow
    foreach ($proc in $procList) {
        if ($proc -and -not $proc.HasExited) {
            & taskkill /PID $proc.Id /T /F 2>$null | Out-Null
        }
    }
    foreach ($svc in $SERVICES) { Stop-PortOwner $svc.Port }
    $procList = @()

    if ($StopInfra -and $docker -and $infraStarted) {
        Write-Host "正在停止 Docker 基础设施（数据卷保留）..." -ForegroundColor Yellow
        & $docker compose -f "$ROOT\deploy\docker-compose.yml" --env-file "$envFile" stop
    }
    Write-Host "已停止" -ForegroundColor Green
}
