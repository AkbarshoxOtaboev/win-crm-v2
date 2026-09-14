# Local development: Spring Boot (8081) + Vite TailAdmin (5174) together.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

Write-Host "==> Starting backend on :8081"
$backend = Start-Process -PassThru -NoNewWindow -FilePath ".\mvnw.cmd" -ArgumentList @(
    "spring-boot:run"
)

Push-Location "$Root\frontend"
if (-not (Test-Path "node_modules")) {
    Write-Host "==> npm install (frontend)"
    npm install
}
Write-Host "==> Starting frontend (TailAdmin) on :5174"
try {
    npm run dev
} finally {
    Pop-Location
    if ($backend -and -not $backend.HasExited) {
        Write-Host "==> Stopping backend (PID $($backend.Id))"
        Stop-Process -Id $backend.Id -Force -ErrorAction SilentlyContinue
    }
}
