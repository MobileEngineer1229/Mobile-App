# =============================================================================
# Offline Environment Setup Script
# 오프라인 환경 구성 스크립트
# =============================================================================
# 인터넷 연결 없이 이 프로젝트의 가상환경을 재구성합니다.
# wheels/ 폴더의 패키지 파일을 사용합니다.
#
# 전제조건:
#   - Python 3.10 이상이 설치되어 있어야 합니다
#   - PyTorch (torch)는 wheels/에 포함되지 않습니다 (용량 2.5GB)
#     → PyTorch는 온라인에서 별도 설치하거나, wheels/에 수동으로 추가하십시오
#
# 사용법:
#   .\setup_offline.ps1
# =============================================================================

$ErrorActionPreference = "Continue"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " 조선말 인공지능 조수 — 오프라인 환경 구성" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Python 확인
$pythonVersion = python --version 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Python이 설치되지 않았습니다. Python 3.10 이상을 설치한 후 다시 실행하십시오."
    exit 1
}
Write-Host "[1/5] Python 확인: $pythonVersion" -ForegroundColor Green

# wheels 폴더 확인
if (-not (Test-Path "wheels")) {
    Write-Error "wheels/ 폴더가 없습니다. 온라인 상태에서 먼저 이 프로젝트를 실행하십시오."
    exit 1
}
$wheelCount = (Get-ChildItem wheels\*.whl).Count
Write-Host "[2/5] wheels/ 폴더 확인: $wheelCount 개 패키지 파일 발견" -ForegroundColor Green

# 기존 venv 삭제 후 재생성
if (Test-Path ".venv") {
    Write-Host "[3/5] 기존 .venv 삭제 중..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force ".venv"
}
Write-Host "[3/5] 가상환경 생성 중..." -ForegroundColor Green
python -m venv .venv --system-site-packages
if ($LASTEXITCODE -ne 0) {
    Write-Error "가상환경 생성 실패"
    exit 1
}
Write-Host "      .venv 생성 완료" -ForegroundColor Green

# wheels에서 패키지 설치
Write-Host "[4/5] wheels/에서 패키지 설치 중..." -ForegroundColor Green
.\.venv\Scripts\pip.exe install --no-index --find-links wheels\ `
    numpy pyyaml sentencepiece tensorboard tqdm gradio `
    pdfplumber python-docx easyocr `
    --quiet
if ($LASTEXITCODE -ne 0) {
    Write-Host "      일부 패키지 설치 실패 (torch 등은 별도 설치 필요)" -ForegroundColor Yellow
} else {
    Write-Host "      패키지 설치 완료" -ForegroundColor Green
}

# PyTorch 확인
Write-Host "[5/5] PyTorch 확인 중..." -ForegroundColor Green
$torchCheck = .\.venv\Scripts\python.exe -c "import torch; print(torch.__version__)" 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "      torch: $torchCheck" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "  [경고] PyTorch가 없습니다. 아래 명령으로 설치하십시오:" -ForegroundColor Yellow
    Write-Host "  RTX 5070 (CUDA 12.8):" -ForegroundColor White
    Write-Host "    pip install torch>=2.7 --index-url https://download.pytorch.org/whl/cu128" -ForegroundColor White
    Write-Host "  기타 GPU (CUDA 12.1):" -ForegroundColor White
    Write-Host "    pip install torch>=2.7 --index-url https://download.pytorch.org/whl/cu121" -ForegroundColor White
    Write-Host "  CPU 전용:" -ForegroundColor White
    Write-Host "    pip install torch>=2.7" -ForegroundColor White
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " 설치 완료!" -ForegroundColor Cyan
Write-Host " 가상환경 활성화: .\.venv\Scripts\Activate.ps1" -ForegroundColor Cyan
Write-Host " 앱 실행:         .\scripts\04_run_app.ps1" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
