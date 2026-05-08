# Generate Self-Signed SSL Certificate for Development
# This creates a certificate that browsers will warn about (expected for self-signed certs)

Write-Host "Generating self-signed SSL certificate..." -ForegroundColor Cyan
Write-Host ""

$certPath = "certs"
if (-not (Test-Path $certPath)) {
    New-Item -ItemType Directory -Path $certPath | Out-Null
    Write-Host "Created certs directory" -ForegroundColor Green
}

$certFile = "$certPath/server.crt"
$keyFile = "$certPath/server.key"

# Check if OpenSSL is available
$openssl = Get-Command openssl -ErrorAction SilentlyContinue
if (-not $openssl) {
    Write-Host "OpenSSL not found. Installing OpenSSL..." -ForegroundColor Yellow
    
    # Try to install via winget
    winget install OpenSSL.OpenSSL.Light --silent --accept-package-agreements --accept-source-agreements 2>&1 | Out-Null
    
    # Wait a moment and refresh PATH
    Start-Sleep -Seconds 2
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    
    $openssl = Get-Command openssl -ErrorAction SilentlyContinue
    if (-not $openssl) {
        Write-Host "ERROR: Could not install OpenSSL automatically." -ForegroundColor Red
        Write-Host "Please install OpenSSL manually from: https://slproweb.com/products/Win32OpenSSL.html" -ForegroundColor Yellow
        Write-Host "Or use a reverse proxy (nginx) instead." -ForegroundColor Yellow
        exit 1
    }
}

Write-Host "Generating private key..." -ForegroundColor Yellow
& openssl genrsa -out $keyFile 2048

Write-Host "Generating certificate..." -ForegroundColor Yellow
& openssl req -new -x509 -key $keyFile -out $certFile -days 365 -subj "/CN=172.86.88.76/O=Smart Home Backend/C=US"

if (Test-Path $certFile -and Test-Path $keyFile) {
    Write-Host ""
    Write-Host "Certificate generated successfully!" -ForegroundColor Green
    Write-Host "  Certificate: $certFile" -ForegroundColor Gray
    Write-Host "  Private Key: $keyFile" -ForegroundColor Gray
    Write-Host ""
    Write-Host "NOTE: This is a self-signed certificate." -ForegroundColor Yellow
    Write-Host "Browsers will show a security warning. Click Advanced and Proceed to continue." -ForegroundColor Yellow
} else {
    Write-Host "ERROR: Failed to generate certificate" -ForegroundColor Red
    exit 1
}
