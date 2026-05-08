# Smart Home Backend - Server Startup Script
# This script sets up the environment and starts the development server

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   Smart Home Backend Server" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Add Node.js to PATH
$env:Path += ";C:\Program Files\nodejs"

# Verify Node.js is available
try {
    $nodeVersion = node --version
    $npmVersion = npm --version
    Write-Host "Node.js: $nodeVersion" -ForegroundColor Green
    Write-Host "npm: $npmVersion" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "ERROR: Node.js not found. Please install Node.js." -ForegroundColor Red
    exit 1
}

# Check if .env file exists
if (-not (Test-Path ".env")) {
    Write-Host "WARNING: .env file not found!" -ForegroundColor Yellow
    Write-Host "Please create a .env file with database configuration." -ForegroundColor Yellow
    Write-Host ""
}

# Check database connection
Write-Host "Checking database connection..." -ForegroundColor Cyan
$env:Path += ";C:\Program Files\PostgreSQL\16\bin"

# Get password from .env file
if (Test-Path ".env") {
    $dbPassword = (Get-Content .env | Select-String "DB_PASSWORD=" | ForEach-Object { $_.Line -replace "DB_PASSWORD=", "" }).Trim()
    $env:PGPASSWORD = $dbPassword
    
    try {
        $dbTest = & psql -U postgres -d smart_home_db -c "SELECT 1;" 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Database connection OK" -ForegroundColor Green
        } else {
            Write-Host "Database connection failed. Server may not start properly." -ForegroundColor Yellow
        }
    } catch {
        Write-Host "Could not test database connection." -ForegroundColor Yellow
    }
    
    # Clear password from environment
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
}
Write-Host ""

# Start the server
Write-Host "Starting development server..." -ForegroundColor Green
Write-Host "Server will be available at: http://localhost:3003" -ForegroundColor Cyan
Write-Host "API documentation: http://localhost:3003/api-docs" -ForegroundColor Cyan
Write-Host ""
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Gray
Write-Host ""

npm run dev
