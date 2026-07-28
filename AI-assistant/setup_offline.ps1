# =============================================================================
# Offline Environment Setup Script
# Offline environment configuration script
# =============================================================================
# Reconstruct the virtual environment of this project without an internet connection..
# wheels/ Use the package file in the folder.
#
# Prerequisites:
#   - Python 3.10 or more must be installed
#   - PyTorch (torch)is wheels/not included in (Capacity 2.5GB)
#     → PyTorchcan be installed separately online or, wheels/Add it manually to
#
# How to use:
#   .\setup_offline.ps1
# =============================================================================

$ErrorActionPreference = "Continue"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Joseon Dynasty artificial intelligence assistant — Offline environment configuration" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Python OK
$pythonVersion = python --version 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "PythonThis is not installed. Python 3.10 After installing or more, please run it again."
    exit 1
}
Write-Host "[1/5] Python OK: $pythonVersion" -ForegroundColor Green

# wheels Check folder
if (-not (Test-Path "wheels")) {
    Write-Error "wheels/ There is no folder. Run this project online first."
    exit 1
}
$wheelCount = (Get-ChildItem wheels\*.whl).Count
Write-Host "[2/5] wheels/ Check folder: $wheelCount dog package file found" -ForegroundColor Green

# existing venv Recreate after deletion
if (Test-Path ".venv") {
    Write-Host "[3/5] existing .venv Deleting..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force ".venv"
}
Write-Host "[3/5] Creating virtual environment..." -ForegroundColor Green
python -m venv .venv --system-site-packages
if ($LASTEXITCODE -ne 0) {
    Write-Error "Virtual environment creation failed"
    exit 1
}
Write-Host "      .venv Creation completed" -ForegroundColor Green

# wheelsInstall package from
Write-Host "[4/5] wheels/Installing package from..." -ForegroundColor Green
.\.venv\Scripts\pip.exe install --no-index --find-links wheels\ `
    numpy pyyaml sentencepiece tensorboard tqdm gradio `
    pdfplumber python-docx easyocr `
    --quiet
if ($LASTEXITCODE -ne 0) {
    Write-Host "      Some packages failed to install (torch etc. require separate installation.)" -ForegroundColor Yellow
} else {
    Write-Host "      Package installation complete" -ForegroundColor Green
}

# PyTorch OK
Write-Host "[5/5] PyTorch Checking..." -ForegroundColor Green
$torchCheck = .\.venv\Scripts\python.exe -c "import torch; print(torch.__version__)" 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "      torch: $torchCheck" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "  [warning] PyTorchThere is no. Install it with the command below:" -ForegroundColor Yellow
    Write-Host "  RTX 5070 (CUDA 12.8):" -ForegroundColor White
    Write-Host "    pip install torch>=2.7 --index-url https://download.pytorch.org/whl/cu128" -ForegroundColor White
    Write-Host "  guitar GPU (CUDA 12.1):" -ForegroundColor White
    Write-Host "    pip install torch>=2.7 --index-url https://download.pytorch.org/whl/cu121" -ForegroundColor White
    Write-Host "  CPU exclusive:" -ForegroundColor White
    Write-Host "    pip install torch>=2.7" -ForegroundColor White
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Installation complete!" -ForegroundColor Cyan
Write-Host " Activation of virtual environment: .\.venv\Scripts\Activate.ps1" -ForegroundColor Cyan
Write-Host " run app:         .\scripts\04_run_app.ps1" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
