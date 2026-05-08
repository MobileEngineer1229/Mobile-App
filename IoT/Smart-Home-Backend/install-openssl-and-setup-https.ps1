# Install OpenSSL and Set Up HTTPS
# This script will install OpenSSL and generate certificates

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   HTTPS Setup for Smart Home Backend" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Install OpenSSL
Write-Host "Step 1: Installing OpenSSL..." -ForegroundColor Yellow

# Try winget first
Write-Host "Trying to install OpenSSL via winget..." -ForegroundColor Gray
$wingetResult = winget install ShiningLight.OpenSSL --silent --accept-package-agreements --accept-source-agreements 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "OpenSSL installed successfully!" -ForegroundColor Green
    Start-Sleep -Seconds 3
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
} else {
    Write-Host "Winget installation failed. Please install manually:" -ForegroundColor Yellow
    Write-Host "1. Download from: https://slproweb.com/products/Win32OpenSSL.html" -ForegroundColor White
    Write-Host "2. Install 'Win64 OpenSSL v3.x.x Light'" -ForegroundColor White
    Write-Host "3. Make sure to add OpenSSL to PATH during installation" -ForegroundColor White
    Write-Host ""
    $continue = Read-Host "Press Enter after installing OpenSSL, or type 'skip' to use HTTP instead"
    if ($continue -eq "skip") {
        Write-Host "Skipping HTTPS setup. Use HTTP: http://172.86.88.76:3003/api-docs" -ForegroundColor Yellow
        exit 0
    }
}

# Step 2: Find OpenSSL
Write-Host "`nStep 2: Locating OpenSSL..." -ForegroundColor Yellow
$openssl = Get-Command openssl -ErrorAction SilentlyContinue

if (-not $openssl) {
    $opensslPaths = @(
        "C:\Program Files\OpenSSL-Win64\bin\openssl.exe",
        "C:\Program Files\OpenSSL\bin\openssl.exe"
    )
    
    foreach ($path in $opensslPaths) {
        if (Test-Path $path) {
            $env:Path += ";$(Split-Path $path)"
            $openssl = Get-Command openssl -ErrorAction SilentlyContinue
            break
        }
    }
}

if (-not $openssl) {
    Write-Host "ERROR: OpenSSL not found. Please install it manually." -ForegroundColor Red
    Write-Host "Download from: https://slproweb.com/products/Win32OpenSSL.html" -ForegroundColor Yellow
    exit 1
}

Write-Host "OpenSSL found at: $($openssl.Source)" -ForegroundColor Green

# Step 3: Generate certificates
Write-Host "`nStep 3: Generating SSL certificates..." -ForegroundColor Yellow

$certDir = "certs"
if (-not (Test-Path $certDir)) {
    New-Item -ItemType Directory -Path $certDir | Out-Null
}

$keyFile = "$certDir\server.key"
$certFile = "$certDir\server.crt"

Write-Host "Generating private key..." -ForegroundColor Gray
& openssl genrsa -out $keyFile 2048

Write-Host "Generating certificate..." -ForegroundColor Gray
& openssl req -new -x509 -key $keyFile -out $certFile -days 365 -subj "/CN=172.86.88.76/O=Smart Home Backend/C=US"

if ((Test-Path $certFile) -and (Test-Path $keyFile)) {
    Write-Host "Certificates generated successfully!" -ForegroundColor Green
} else {
    Write-Host "ERROR: Failed to generate certificates" -ForegroundColor Red
    exit 1
}

# Step 4: Update .env file
Write-Host "`nStep 4: Updating .env file..." -ForegroundColor Yellow

$envFile = ".env"
if (-not (Test-Path $envFile)) {
    Write-Host "ERROR: .env file not found!" -ForegroundColor Red
    exit 1
}

$envContent = Get-Content $envFile
$hasHttps = $envContent | Select-String -Pattern "^ENABLE_HTTPS="

if ($hasHttps) {
    # Update existing settings
    $newContent = $envContent | ForEach-Object {
        if ($_ -match "^ENABLE_HTTPS=") { "ENABLE_HTTPS=true" }
        elseif ($_ -match "^HTTPS_PORT=") { "HTTPS_PORT=3003" }
        elseif ($_ -match "^SSL_CERT_PATH=") { "SSL_CERT_PATH=certs/server.crt" }
        elseif ($_ -match "^SSL_KEY_PATH=") { "SSL_KEY_PATH=certs/server.key" }
        else { $_ }
    }
    
    # Add missing settings
    if (-not ($newContent | Select-String -Pattern "^ENABLE_HTTPS=")) {
        $newContent += "ENABLE_HTTPS=true"
    }
    if (-not ($newContent | Select-String -Pattern "^HTTPS_PORT=")) {
        $newContent += "HTTPS_PORT=3003"
    }
    if (-not ($newContent | Select-String -Pattern "^SSL_CERT_PATH=")) {
        $newContent += "SSL_CERT_PATH=certs/server.crt"
    }
    if (-not ($newContent | Select-String -Pattern "^SSL_KEY_PATH=")) {
        $newContent += "SSL_KEY_PATH=certs/server.key"
    }
    
    $newContent | Set-Content $envFile
} else {
    # Add new settings
    Add-Content -Path $envFile -Value ""
    Add-Content -Path $envFile -Value "# HTTPS Configuration"
    Add-Content -Path $envFile -Value "ENABLE_HTTPS=true"
    Add-Content -Path $envFile -Value "HTTPS_PORT=3003"
    Add-Content -Path $envFile -Value "SSL_CERT_PATH=certs/server.crt"
    Add-Content -Path $envFile -Value "SSL_KEY_PATH=certs/server.key"
}

Write-Host ".env file updated!" -ForegroundColor Green

# Step 5: Open firewall port
Write-Host "`nStep 5: Opening firewall port 3003 for HTTPS..." -ForegroundColor Yellow
$firewallRule = Get-NetFirewallRule -DisplayName "Smart Home Backend - HTTPS Port 3003" -ErrorAction SilentlyContinue
if (-not $firewallRule) {
    New-NetFirewallRule -DisplayName "Smart Home Backend - HTTPS Port 3003" -Direction Inbound -LocalPort 3003 -Protocol TCP -Action Allow -Description "Allow Smart Home Backend HTTPS server on port 3003" | Out-Null
    Write-Host "Firewall rule created!" -ForegroundColor Green
} else {
    Write-Host "Firewall rule already exists" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "   HTTPS Setup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Restart your server: .\start-server.ps1" -ForegroundColor White
Write-Host "2. Access: https://172.86.88.76:3003/api-docs" -ForegroundColor White
Write-Host ""
Write-Host "Note: Browsers will show a security warning for self-signed certificates." -ForegroundColor Yellow
Write-Host "Click 'Advanced' → 'Proceed to 172.86.88.76 (unsafe)' to continue." -ForegroundColor Yellow
Write-Host ""
