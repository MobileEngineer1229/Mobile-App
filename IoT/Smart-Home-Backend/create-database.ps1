# PowerShell script to create PostgreSQL database
# Usage: .\create-database.ps1

$dbName = "smart_home_db"
$dbUser = "postgres"
$dbPassword = "213515"

Write-Host "Creating PostgreSQL database: $dbName" -ForegroundColor Cyan

# Try to find psql in common PostgreSQL installation paths
$psqlPaths = @(
    "C:\Program Files\PostgreSQL\16\bin\psql.exe",
    "C:\Program Files\PostgreSQL\15\bin\psql.exe",
    "C:\Program Files\PostgreSQL\14\bin\psql.exe",
    "C:\Program Files\PostgreSQL\13\bin\psql.exe",
    "C:\Program Files\PostgreSQL\12\bin\psql.exe",
    "$env:ProgramFiles\PostgreSQL\*\bin\psql.exe"
)

$psql = $null
foreach ($path in $psqlPaths) {
    if (Test-Path $path) {
        $psql = $path
        break
    }
}

# If not found, try to find it in PATH
if (-not $psql) {
    $psql = Get-Command psql -ErrorAction SilentlyContinue
    if ($psql) {
        $psql = $psql.Source
    }
}

if (-not $psql) {
    Write-Host "ERROR: psql not found. Please:" -ForegroundColor Red
    Write-Host "1. Add PostgreSQL bin directory to your PATH, OR" -ForegroundColor Yellow
    Write-Host "2. Use pgAdmin to create the database manually, OR" -ForegroundColor Yellow
    Write-Host "3. Run this SQL command in pgAdmin or psql:" -ForegroundColor Yellow
    Write-Host "   CREATE DATABASE smart_home_db;" -ForegroundColor White
    exit 1
}

Write-Host "Found psql at: $psql" -ForegroundColor Green

# Set PGPASSWORD environment variable
$env:PGPASSWORD = $dbPassword

# Create database
$createDbCommand = "CREATE DATABASE $dbName;"
$result = & $psql -U $dbUser -c $createDbCommand 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "Database '$dbName' created successfully!" -ForegroundColor Green
} else {
    if ($result -match "already exists") {
        Write-Host "Database '$dbName' already exists. That's fine!" -ForegroundColor Yellow
    } else {
        Write-Host "Error creating database:" -ForegroundColor Red
        Write-Host $result -ForegroundColor Red
        exit 1
    }
}

# Clear password from environment
Remove-Item Env:\PGPASSWORD

Write-Host "`nNext step: Run the schema file:" -ForegroundColor Cyan
Write-Host "  psql -U postgres -d smart_home_db -f database/schema.sql" -ForegroundColor White

