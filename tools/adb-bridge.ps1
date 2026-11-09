param(
    [string]$ListenAddress = "192.168.3.3",
    [int]$Port = 5037,
    [string]$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
)

# USB 手机 -> Windows adb（监听局域网）-> WSL 的 ADB_SERVER_SOCKET

$ErrorActionPreference = "Stop"
if (-not (Test-Path $Adb)) {
    $Adb = "adb"
}

function Show-Devices {
    param([string]$Label)
    $out = & $Adb devices 2>&1
    Write-Host "[$Label]" -ForegroundColor Cyan
    foreach ($line in $out) { Write-Host "  $line" }
    $count = 0
    foreach ($line in $out) {
        if ($line -match "^\S+\s+device$") { $count++ }
    }
    return ($count -gt 0)
}

# 0. 先确认手机真的连上了
if (-not (Show-Devices "USB")) {
    Write-Host "没有检测到设备：先开 USB 调试、插线，并在手机上点允许。" -ForegroundColor Yellow
    exit 1
}

# 1. 清掉旧的 adb 服务（默认只监听 127.0.0.1，还占着 5037）
& $Adb kill-server 2>&1 | Out-Null
Start-Sleep -Seconds 2

# 2. 仍然占着 5037 的 adb 进程直接停掉
$owners = @(Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique)
foreach ($ownerPid in $owners) {
    if ($ownerPid -and $ownerPid -ne 0) {
        $proc = Get-Process -Id $ownerPid -ErrorAction SilentlyContinue
        if ($proc -and $proc.ProcessName -like "adb*") {
            Write-Host "停止占用 $Port 的 $($proc.ProcessName)（PID $ownerPid）" -ForegroundColor Yellow
            Stop-Process -Id $ownerPid -Force
            Start-Sleep -Seconds 1
        }
    }
}

# 3. 起一个监听局域网的 adb 服务，它会自己拉起 USB 连接
function Start-LanServer {
    Write-Host "在 ${ListenAddress}:$Port 启动 adb 服务..." -ForegroundColor Cyan
    $script:server = Start-Process -FilePath $Adb -PassThru -WindowStyle Hidden -ArgumentList @(
        "-a", "-P", "$Port", "nodaemon", "server"
    )
    Start-Sleep -Seconds 4
    if ($script:server.HasExited) {
        Write-Host "server 退出了：端口可能仍被占用。" -ForegroundColor Red
        return $false
    }
    for ($i = 0; $i -lt 8; $i++) {
        if (Show-Devices "LAN ${ListenAddress}:$Port") { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

if (-not (Start-LanServer)) {
    Write-Host "局域网侧连不上，检查防火墙规则 adb server (LAN)。" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "好了，WSL 里执行：" -ForegroundColor Green
Write-Host "  export ADB_SERVER_SOCKET=tcp:${ListenAddress}:$Port"
