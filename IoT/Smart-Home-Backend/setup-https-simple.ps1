# Simple HTTPS Setup - Alternative Method
# This script provides multiple ways to set up HTTPS

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   HTTPS Setup - Alternative Methods" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Method 1: Try to install OpenSSL via different package managers
Write-Host "Method 1: Trying to install OpenSSL..." -ForegroundColor Yellow

# Try Chocolatey
$choco = Get-Command choco -ErrorAction SilentlyContinue
if ($choco) {
    Write-Host "Found Chocolatey. Installing OpenSSL..." -ForegroundColor Green
    choco install openssl.light -y
    Start-Sleep -Seconds 3
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
}

# Try winget with different package name
if (-not (Get-Command openssl -ErrorAction SilentlyContinue)) {
    Write-Host "Trying winget with different package..." -ForegroundColor Gray
    winget install --id ShiningLight.OpenSSL --silent --accept-package-agreements --accept-source-agreements 2>&1 | Out-Null
    Start-Sleep -Seconds 3
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
}

# Check if OpenSSL is now available
$openssl = Get-Command openssl -ErrorAction SilentlyContinue

if (-not $openssl) {
    # Check common installation paths
    $opensslPaths = @(
        "C:\Program Files\OpenSSL-Win64\bin\openssl.exe",
        "C:\Program Files\OpenSSL\bin\openssl.exe",
        "C:\OpenSSL-Win64\bin\openssl.exe",
        "${env:ProgramFiles(x86)}\OpenSSL-Win64\bin\openssl.exe"
    )
    
    foreach ($path in $opensslPaths) {
        if (Test-Path $path) {
            $env:Path += ";$(Split-Path $path)"
            $openssl = Get-Command openssl -ErrorAction SilentlyContinue
            if ($openssl) {
                Write-Host "Found OpenSSL at: $path" -ForegroundColor Green
                break
            }
        }
    }
}

if ($openssl) {
    Write-Host "OpenSSL is available!" -ForegroundColor Green
    Write-Host "Location: $($openssl.Source)" -ForegroundColor Gray
    
    # Generate certificates
    Write-Host "`nGenerating SSL certificates..." -ForegroundColor Yellow
    
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
        
        # Update .env
        Write-Host "`nUpdating .env file..." -ForegroundColor Yellow
        $envFile = ".env"
        if (Test-Path $envFile) {
            $envContent = Get-Content $envFile
            $hasHttps = $envContent | Select-String -Pattern "^ENABLE_HTTPS="
            
            if (-not $hasHttps) {
                Add-Content -Path $envFile -Value ""
                Add-Content -Path $envFile -Value "# HTTPS Configuration"
                Add-Content -Path $envFile -Value "ENABLE_HTTPS=true"
                Add-Content -Path $envFile -Value "HTTPS_PORT=3003"
                Add-Content -Path $envFile -Value "SSL_CERT_PATH=certs/server.crt"
                Add-Content -Path $envFile -Value "SSL_KEY_PATH=certs/server.key"
                Write-Host ".env file updated!" -ForegroundColor Green
            } else {
                Write-Host "HTTPS already configured in .env" -ForegroundColor Yellow
            }
        }
        
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "   HTTPS Setup Complete!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Restart your server: .\start-server.ps1" -ForegroundColor Cyan
        Write-Host "Then access: https://172.86.88.76:3003/api-docs" -ForegroundColor Cyan
        Write-Host ""
    } else {
        Write-Host "ERROR: Failed to generate certificates" -ForegroundColor Red
    }
} else {
    Write-Host ""
    Write-Host "OpenSSL not found. Here are your options:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Option 1: Manual Installation (Recommended)" -ForegroundColor Cyan
    Write-Host "1. Download OpenSSL from:" -ForegroundColor White
    Write-Host "   https://slproweb.com/products/Win32OpenSSL.html" -ForegroundColor Gray
    Write-Host "2. Download 'Win64 OpenSSL v3.x.x Light' (about 5MB)" -ForegroundColor White
    Write-Host "3. Run the installer" -ForegroundColor White
    Write-Host "4. IMPORTANT: Check 'Copy OpenSSL DLLs to' → 'The OpenSSL binaries (/bin) directory'" -ForegroundColor Yellow
    Write-Host "5. Run this script again: .\setup-https-simple.ps1" -ForegroundColor White
    Write-Host ""
    Write-Host "Option 2: Use HTTP Instead (Quick Fix)" -ForegroundColor Cyan
    Write-Host "Simply use HTTP instead of HTTPS:" -ForegroundColor White
    Write-Host "  http://172.86.88.76:3003/api-docs" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Option 3: Use Chocolatey (If Available)" -ForegroundColor Cyan
    Write-Host "If you have Chocolatey installed:" -ForegroundColor White
    Write-Host "  choco install openssl.light -y" -ForegroundColor Gray
    Write-Host ""
}
