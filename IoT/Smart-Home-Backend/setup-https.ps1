# Quick HTTPS Setup Script
# This script generates certificates and updates .env file

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   HTTPS Setup for Smart Home Backend" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Generate certificate
Write-Host "Step 1: Generating SSL certificate..." -ForegroundColor Yellow
& .\scripts\generate-self-signed-cert.ps1

if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to generate certificate. Exiting." -ForegroundColor Red
    exit 1
}

Write-Host ""

# Step 2: Update .env file
Write-Host "Step 2: Updating .env file..." -ForegroundColor Yellow

$envFile = ".env"
$httpsPort = "3443"

if (Test-Path $envFile) {
    $envContent = Get-Content $envFile
    
    # Check if HTTPS settings already exist
    $hasHttps = $envContent | Select-String -Pattern "ENABLE_HTTPS"
    
    if ($hasHttps) {
        Write-Host "HTTPS settings already exist in .env" -ForegroundColor Yellow
        Write-Host "Updating existing settings..." -ForegroundColor Gray
        
        $newContent = $envContent | ForEach-Object {
            if ($_ -match "^ENABLE_HTTPS=") { "ENABLE_HTTPS=true" }
            elseif ($_ -match "^HTTPS_PORT=") { "HTTPS_PORT=$httpsPort" }
            elseif ($_ -match "^SSL_CERT_PATH=") { "SSL_CERT_PATH=certs/server.crt" }
            elseif ($_ -match "^SSL_KEY_PATH=") { "SSL_KEY_PATH=certs/server.key" }
            else { $_ }
        }
        
        # Add missing settings if they don't exist
        if (-not ($newContent | Select-String -Pattern "^ENABLE_HTTPS=")) {
            $newContent += "ENABLE_HTTPS=true"
        }
        if (-not ($newContent | Select-String -Pattern "^HTTPS_PORT=")) {
            $newContent += "HTTPS_PORT=$httpsPort"
        }
        if (-not ($newContent | Select-String -Pattern "^SSL_CERT_PATH=")) {
            $newContent += "SSL_CERT_PATH=certs/server.crt"
        }
        if (-not ($newContent | Select-String -Pattern "^SSL_KEY_PATH=")) {
            $newContent += "SSL_KEY_PATH=certs/server.key"
        }
        
        $newContent | Set-Content $envFile
    } else {
        Write-Host "Adding HTTPS settings to .env..." -ForegroundColor Gray
        Add-Content -Path $envFile -Value ""
        Add-Content -Path $envFile -Value "# HTTPS Configuration"
        Add-Content -Path $envFile -Value "ENABLE_HTTPS=true"
        Add-Content -Path $envFile -Value "HTTPS_PORT=$httpsPort"
        Add-Content -Path $envFile -Value "SSL_CERT_PATH=certs/server.crt"
        Add-Content -Path $envFile -Value "SSL_KEY_PATH=certs/server.key"
    }
    
    Write-Host "✓ .env file updated" -ForegroundColor Green
} else {
    Write-Host "ERROR: .env file not found!" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Step 3: Open firewall port
Write-Host "Step 3: Opening firewall port $httpsPort..." -ForegroundColor Yellow

$firewallRule = Get-NetFirewallRule -DisplayName "Smart Home Backend - HTTPS Port $httpsPort" -ErrorAction SilentlyContinue
if ($firewallRule) {
    Write-Host "Firewall rule already exists" -ForegroundColor Gray
} else {
    New-NetFirewallRule -DisplayName "Smart Home Backend - HTTPS Port $httpsPort" -Direction Inbound -LocalPort $httpsPort -Protocol TCP -Action Allow -Description "Allow Smart Home Backend HTTPS server on port $httpsPort" | Out-Null
    Write-Host "✓ Firewall rule created" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "   HTTPS Setup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Restart your server: .\start-server.ps1" -ForegroundColor White
Write-Host "2. Access HTTPS at: https://172.86.88.76:$httpsPort/api-docs" -ForegroundColor White
Write-Host ""
Write-Host "Note: Browsers will show a security warning for self-signed certificates." -ForegroundColor Yellow
Write-Host "This is normal for development. Click 'Advanced' → 'Proceed' to continue." -ForegroundColor Yellow
Write-Host ""
