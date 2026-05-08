# Script to run device type migration
# Updates device_type_enum from ('sensor', 'actuator', 'controller') to ('lamp', 'camera', 'electronics')

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "   Device Type Migration" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Green

$env:PGPASSWORD = "213515"
$dbName = "smart_home_db"
$dbUser = "postgres"
$migrationFile = "database\migration_update_device_types.sql"

Write-Host "Running migration: $migrationFile" -ForegroundColor Cyan
Write-Host "Database: $dbName" -ForegroundColor Cyan
Write-Host "User: $dbUser`n" -ForegroundColor Cyan

# Run the migration
& psql -U $dbUser -d $dbName -f $migrationFile

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host "   Migration Completed Successfully!" -ForegroundColor Green
    Write-Host "========================================`n" -ForegroundColor Green
    Write-Host "Device types updated:" -ForegroundColor Yellow
    Write-Host "  - lamp" -ForegroundColor White
    Write-Host "  - camera" -ForegroundColor White
    Write-Host "  - electronics" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host "`nMigration failed. Please check the error messages above." -ForegroundColor Red
    Write-Host ""
}
