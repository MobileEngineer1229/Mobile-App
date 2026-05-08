# Smart Home Backend - Setup Script
# Run: .\setup.ps1

Write-Host "Setting up Smart Home Backend..." -ForegroundColor Cyan
Write-Host ""

# Initialize nvm and switch to Node.js 18.18.0
$nvmHome = [Environment]::GetEnvironmentVariable("NVM_HOME", "User")
if (-not $nvmHome) {
    $nvmHome = [Environment]::GetEnvironmentVariable("NVM_HOME", "Machine")
}

$nvmSymlink = [Environment]::GetEnvironmentVariable("NVM_SYMLINK", "User")
if (-not $nvmSymlink) {
    $nvmSymlink = [Environment]::GetEnvironmentVariable("NVM_SYMLINK", "Machine")
}

if ($nvmHome -and (Test-Path "$nvmHome\nvm.exe")) {
    $env:Path = "$nvmHome;$env:Path"
    Write-Host "Switching to Node.js 18.18.0..." -ForegroundColor Yellow
    
    # Suppress nvm-windows settings.txt error
    $ErrorActionPreference = 'SilentlyContinue'
    & "$nvmHome\nvm.exe" use 18.18.0 2>&1 | Where-Object { $_ -notmatch 'settings.txt' } | Out-Null
    $ErrorActionPreference = 'Continue'
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Node.js 18.18.0 not installed. Installing..." -ForegroundColor Yellow
        $ErrorActionPreference = 'SilentlyContinue'
        & "$nvmHome\nvm.exe" install 18.18.0 2>&1 | Where-Object { $_ -notmatch 'settings.txt' } | Out-Null
        & "$nvmHome\nvm.exe" use 18.18.0 2>&1 | Where-Object { $_ -notmatch 'settings.txt' } | Out-Null
        $ErrorActionPreference = 'Continue'
    }
    
    # Add symlink to PATH (where node.exe and npm are located)
    if ($nvmSymlink -and (Test-Path $nvmSymlink)) {
        $env:Path = "$nvmSymlink;$env:Path"
    }
    
    Write-Host "Node.js: $(node -v)" -ForegroundColor Green
    Write-Host "npm: $(npm -v)" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "Warning: nvm not found. Using system Node.js" -ForegroundColor Yellow
    Write-Host "Node.js: $(node -v)" -ForegroundColor Cyan
    Write-Host ""
}

# Install dependencies
Write-Host "Installing dependencies..." -ForegroundColor Yellow
npm install

if ($LASTEXITCODE -eq 0) {
    Write-Host "Dependencies installed successfully!" -ForegroundColor Green
} else {
    Write-Host "Failed to install dependencies" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Setup complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Create .env file (copy from .env.example if available)" -ForegroundColor White
Write-Host "2. Configure database connection in .env" -ForegroundColor White
Write-Host "3. Run: npm run dev" -ForegroundColor White

