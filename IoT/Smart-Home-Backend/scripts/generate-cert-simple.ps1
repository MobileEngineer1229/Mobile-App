# Simple Certificate Generation using PowerShell
# This uses Windows built-in certificate tools

Write-Host "Generating SSL certificate..." -ForegroundColor Cyan

$certPath = "certs"
if (-not (Test-Path $certPath)) {
    New-Item -ItemType Directory -Path $certPath | Out-Null
}

$certFile = "$certPath/server.crt"
$keyFile = "$certPath/server.key"
$pfxFile = "$certPath/server.pfx"

# Try to find OpenSSL
$openssl = $null
$opensslPaths = @(
    "C:\Program Files\OpenSSL-Win64\bin\openssl.exe",
    "C:\Program Files\OpenSSL\bin\openssl.exe",
    "C:\OpenSSL-Win64\bin\openssl.exe"
)

foreach ($path in $opensslPaths) {
    if (Test-Path $path) {
        $openssl = $path
        $env:Path += ";$(Split-Path $path)"
        break
    }
}

# Also check if openssl is in PATH
if (-not $openssl) {
    $opensslCmd = Get-Command openssl -ErrorAction SilentlyContinue
    if ($opensslCmd) {
        $openssl = $opensslCmd.Source
    }
}

if ($openssl) {
    Write-Host "Using OpenSSL at: $openssl" -ForegroundColor Green
    Write-Host "Generating private key..." -ForegroundColor Yellow
    & $openssl genrsa -out $keyFile 2048
    
    Write-Host "Generating certificate..." -ForegroundColor Yellow
    & $openssl req -new -x509 -key $keyFile -out $certFile -days 365 -subj "/CN=172.86.88.76/O=Smart Home Backend/C=US"
    
    if (Test-Path $certFile -and Test-Path $keyFile) {
        Write-Host "Certificate generated successfully!" -ForegroundColor Green
        Write-Host "  Certificate: $certFile" -ForegroundColor Gray
        Write-Host "  Private Key: $keyFile" -ForegroundColor Gray
        exit 0
    }
}

# Alternative: Use PowerShell to create a self-signed certificate
Write-Host "OpenSSL not found. Using PowerShell certificate generation..." -ForegroundColor Yellow

try {
    $cert = New-SelfSignedCertificate `
        -DnsName "172.86.88.76", "localhost" `
        -CertStoreLocation "cert:\LocalMachine\My" `
        -KeyExportPolicy Exportable `
        -KeySpec Signature `
        -KeyLength 2048 `
        -KeyAlgorithm RSA `
        -HashAlgorithm SHA256 `
        -NotAfter (Get-Date).AddYears(1)
    
    # Export certificate to PEM format
    $certPath2 = $cert.PSPath
    $password = ConvertTo-SecureString -String "temp123" -Force -AsPlainText
    
    # Export to PFX first
    Export-PfxCertificate -Cert $cert -FilePath $pfxFile -Password $password | Out-Null
    
    # Convert PFX to PEM using certutil (if available) or provide instructions
    Write-Host "Certificate created in Windows certificate store." -ForegroundColor Green
    Write-Host "Certificate thumbprint: $($cert.Thumbprint)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "To use this certificate, you need to:" -ForegroundColor Yellow
    Write-Host "1. Export it to PEM format, OR" -ForegroundColor White
    Write-Host "2. Use the PFX file with a different approach" -ForegroundColor White
    Write-Host ""
    Write-Host "Simplest solution: Install OpenSSL from:" -ForegroundColor Cyan
    Write-Host "https://slproweb.com/products/Win32OpenSSL.html" -ForegroundColor White
    Write-Host ""
    Write-Host "Or use HTTP instead of HTTPS for now:" -ForegroundColor Yellow
    Write-Host "http://172.86.88.76:3003/api-docs" -ForegroundColor White
    
    exit 1
} catch {
    Write-Host "Failed to generate certificate: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install OpenSSL or use HTTP instead." -ForegroundColor Yellow
    exit 1
}
