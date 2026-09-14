# Build frontend (TailAdmin Vue) + Spring Boot JAR for VDS deploy.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

Write-Host "==> Building WinCRM (Vue + Spring Boot JAR)"
& .\mvnw.cmd -DskipTests package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$Jar = Get-ChildItem -Path "target" -Filter "win-crm-*.jar" |
    Where-Object { $_.Name -notlike "*.original" } |
    Select-Object -First 1

Write-Host "==> Done: $($Jar.FullName)"
Write-Host "    Copy this JAR + .env to the VDS, then run scripts/run-prod.sh"
