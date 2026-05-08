# Helper script to set PostgreSQL password
# This script helps you set the PostgreSQL password to match your .env file

Write-Host "PostgreSQL Password Setup Helper" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# Find PostgreSQL
$pgPaths = @(
    "C:\Program Files\PostgreSQL\16\bin",
    "C:\Program Files\PostgreSQL\15\bin",
    "C:\Program Files\PostgreSQL\14\bin"
)

$psqlPath = $null
foreach ($path in $pgPaths) {
    if (Test-Path "$path\psql.exe") {
        $psqlPath = $path
        $env:Path += ";$path"
        break
    }
}

if (-not $psqlPath) {
    $psqlPath = Get-Command psql -ErrorAction SilentlyContinue
    if ($psqlPath) {
        $psqlPath = Split-Path $psqlPath.Source
    } else {
        Write-Host "ERROR: psql not found. Please ensure PostgreSQL is installed." -ForegroundColor Red
        exit 1
    }
}

$newPassword = "213515"

Write-Host "This script will set the PostgreSQL password for user 'postgres' to: $newPassword" -ForegroundColor Yellow
Write-Host ""
Write-Host "You will be prompted to enter your CURRENT PostgreSQL password." -ForegroundColor Cyan
Write-Host "If you don't remember it, you can:" -ForegroundColor White
Write-Host "  1. Check if you saved it during PostgreSQL installation" -ForegroundColor Gray
Write-Host "  2. Use pgAdmin 4 to reset it" -ForegroundColor Gray
Write-Host "  3. Or update the .env file to use your current password" -ForegroundColor Gray
Write-Host ""

$confirm = Read-Host "Do you know your current PostgreSQL password? (Y/N)"
if ($confirm -ne "Y" -and $confirm -ne "y") {
    Write-Host "`nPlease use one of these alternatives:" -ForegroundColor Yellow
    Write-Host "`n1. Use pgAdmin 4:" -ForegroundColor Green
    Write-Host "   - Open pgAdmin 4" -ForegroundColor White
    Write-Host "   - Connect to server (use your password)" -ForegroundColor White
    Write-Host "   - Right-click 'Login/Group Roles' -> postgres -> Properties" -ForegroundColor White
    Write-Host "   - Set password to: $newPassword" -ForegroundColor White
    Write-Host "`n2. Or update .env file:" -ForegroundColor Green
    Write-Host "   - Edit .env file" -ForegroundColor White
    Write-Host "   - Change DB_PASSWORD to match your current PostgreSQL password" -ForegroundColor White
    exit 0
}

Write-Host "`nAttempting to set password..." -ForegroundColor Yellow
Write-Host "You will be prompted for your current password." -ForegroundColor Cyan
Write-Host ""

# Try to set password (will prompt for current password)
$result = & psql -U postgres -c "ALTER USER postgres WITH PASSWORD '$newPassword';" 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Password set successfully!" -ForegroundColor Green
    Write-Host "`nYou can now run: .\complete-setup.ps1" -ForegroundColor Cyan
} else {
    Write-Host "❌ Failed to set password:" -ForegroundColor Red
    Write-Host $result -ForegroundColor Red
    Write-Host "`nMake sure you entered the correct current password." -ForegroundColor Yellow
    Write-Host "If you continue to have issues, use pgAdmin 4 or update the .env file." -ForegroundColor Yellow
    exit 1
}
