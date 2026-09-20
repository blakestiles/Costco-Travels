# Costco Travel Smart Rebook - start-demo.ps1
# PowerShell equivalent of start-demo.sh for native Windows use (not Git Bash).
# Starts SQL Server via docker compose, creates the smartrebook database if needed, then
# starts the three Spring Boot services in the background, health-polling each in turn.
# Safe to re-run: every step is idempotent / resumable.

$ErrorActionPreference = "Continue"

$RootDir = Split-Path -Parent $PSScriptRoot
Set-Location $RootDir

$envFile = Join-Path $RootDir ".env"
$envVars = @{}
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
        $parts = $_ -split '=', 2
        $envVars[$parts[0].Trim()] = $parts[1].Trim()
    }
}

function Get-EnvOrDefault($key, $default) {
    if ($envVars.ContainsKey($key) -and $envVars[$key]) { return $envVars[$key] }
    return $default
}

$MssqlSaPassword = Get-EnvOrDefault "MSSQL_SA_PASSWORD" "DevOnly_P@ssw0rd123"
$MssqlDb = Get-EnvOrDefault "MSSQL_DB" "smartrebook"
$BookingPort = Get-EnvOrDefault "BOOKING_SERVICE_PORT" "8080"
$HotelPort = Get-EnvOrDefault "HOTEL_SUPPLIER_PORT" "8081"
$CarPort = Get-EnvOrDefault "CAR_SUPPLIER_PORT" "8082"

$PidDir = Join-Path $RootDir "scripts\.pids"
$LogDir = Join-Path $RootDir "logs"
New-Item -ItemType Directory -Force -Path $PidDir | Out-Null
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

Write-Host "== Costco Travel Smart Rebook - start-demo =="

Write-Host "[1/5] Starting SQL Server (docker compose)..."
docker compose up -d

Write-Host "[2/5] Waiting for SQL Server to become healthy..."
$healthy = $false
for ($i = 0; $i -lt 30; $i++) {
    $status = docker inspect --format='{{.State.Health.Status}}' smartrebook-sqlserver 2>$null
    if ($status -eq "healthy") { $healthy = $true; Write-Host "    SQL Server is healthy."; break }
    Start-Sleep -Seconds 2
}
if (-not $healthy) { Write-Warning "SQL Server did not report healthy in time; continuing anyway." }

Write-Host "[3/5] Ensuring database '$MssqlDb' exists (idempotent)..."
docker exec smartrebook-sqlserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "$MssqlSaPassword" -Q "IF DB_ID('$MssqlDb') IS NULL CREATE DATABASE $MssqlDb;"

function Wait-ForHealth($name, $port) {
    for ($i = 0; $i -lt 30; $i++) {
        try {
            $resp = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:$port/actuator/health" -TimeoutSec 2 -ErrorAction Stop
            if ($resp.StatusCode -eq 200) { Write-Host "    $name is up (http://localhost:$port)."; return $true }
        } catch { }
        Start-Sleep -Seconds 2
    }
    Write-Warning "$name did not report healthy on port $port in time."
    return $false
}

function Test-PortResponding($port) {
    try {
        $resp = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:$port/actuator/health" -TimeoutSec 2 -ErrorAction Stop
        return $resp.StatusCode -eq 200
    } catch { return $false }
}

function Start-ServiceJar($name, $port, $jarPattern) {
    $pidFile = Join-Path $PidDir "$name.pid"

    if (Test-Path $pidFile) {
        $existingPid = Get-Content $pidFile
        if (Get-Process -Id $existingPid -ErrorAction SilentlyContinue) {
            Write-Host "    $name already running (pid $existingPid); skipping."
            return
        }
    }

    if (Test-PortResponding $port) {
        Write-Host "    $name already responding on port $port (started outside this script); skipping launch."
        return
    }

    $jarFile = Get-ChildItem -Path $jarPattern -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $jarFile) {
        Write-Warning "Skipping $name: no build artifact found matching $jarPattern (run .\mvnw.cmd clean package first)."
        return
    }

    Write-Host "    Launching $name from $($jarFile.FullName) ..."
    $logFile = Join-Path $LogDir "$name.out.log"
    $proc = Start-Process -FilePath "java" -ArgumentList "-jar", "`"$($jarFile.FullName)`"" `
        -RedirectStandardOutput $logFile -RedirectStandardError "$logFile.err" `
        -WindowStyle Hidden -PassThru
    $proc.Id | Out-File -FilePath $pidFile -Encoding ascii
}

Write-Host "[4/5] Starting Spring Boot services (hotel, car, booking)..."
Start-ServiceJar "hotel-supplier-service" $HotelPort (Join-Path $RootDir "hotel-supplier-service\target\hotel-supplier-service-*.jar")
Wait-ForHealth "hotel-supplier-service" $HotelPort | Out-Null

Start-ServiceJar "car-supplier-service" $CarPort (Join-Path $RootDir "car-supplier-service\target\car-supplier-service-*.jar")
Wait-ForHealth "car-supplier-service" $CarPort | Out-Null

Start-ServiceJar "booking-service" $BookingPort (Join-Path $RootDir "booking-service\target\booking-service-*.war")
Wait-ForHealth "booking-service" $BookingPort | Out-Null

Write-Host "[5/5] Done."
Write-Host ""
Write-Host "=============================================================="
Write-Host " Costco Travel Smart Rebook - demo environment"
Write-Host "--------------------------------------------------------------"
Write-Host " Member Portal   http://localhost:$BookingPort"
Write-Host " Operations      http://localhost:$BookingPort/ops"
Write-Host " Demo booking    CT-DEMO-78291"
Write-Host "=============================================================="
