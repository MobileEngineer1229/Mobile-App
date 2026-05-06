$BUN = "C:\Users\arthu\AppData\Local\Microsoft\WinGet\Packages\Oven-sh.Bun_Microsoft.Winget.Source_8wekyb3d8bbwe\bun-windows-x64\bun.exe"
$ROOT = $PSScriptRoot

function Start-Service($name, $dir, $args) {
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$dir'; Write-Host '[' -NoNewline; Write-Host '$name' -ForegroundColor Cyan -NoNewline; Write-Host '] Starting...'; & '$BUN' $args"
}

Write-Host "Starting Bomb.io dev environment..." -ForegroundColor Green

# Server services
Start-Service "login-server"  "$ROOT\server\services\login-server"  "--watch src/index.ts"
Start-Service "match-server"  "$ROOT\server\services\match-server"  "--watch src/index.ts"
Start-Service "battle-server" "$ROOT\server\services\battle-server" "--watch src/index.ts"
Start-Service "rank-server"   "$ROOT\server\services\rank-server"   "--watch src/index.ts"

# Client (Vite)
Start-Service "client"        "$ROOT\client"                         "--bun vite"

Write-Host ""
Write-Host "All services started in separate windows." -ForegroundColor Green
Write-Host "  Client : http://localhost:3000" -ForegroundColor Yellow
Write-Host "  Login  : http://localhost:8000" -ForegroundColor Yellow
Write-Host "  Match  : http://localhost:8001" -ForegroundColor Yellow
Write-Host "  Rank   : http://localhost:8002" -ForegroundColor Yellow
Write-Host "  Battle : WebSocket ws://localhost:5000/battle" -ForegroundColor Yellow
