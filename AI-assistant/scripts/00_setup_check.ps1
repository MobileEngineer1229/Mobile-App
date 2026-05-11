# Verify the Python environment before running the pipeline.
# Run this first to catch missing packages and GPU issues early.

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

Write-Host ""
Write-Host "=== DPRK Assistant - Environment Check ===" -ForegroundColor Cyan
Write-Host ""

# Python version
$pyVer = python --version 2>&1
Write-Host "Python : $pyVer"

# Write the check script to a temp file and run it
$checkPy = [System.IO.Path]::GetTempFileName() -replace '\.tmp$', '.py'
@'
import sys, importlib

print()
print("--- PyTorch / GPU ---")
try:
    import torch
    print(f"  PyTorch   : {torch.__version__}")
    avail = torch.cuda.is_available()
    print(f"  CUDA avail: {avail}")
    if avail:
        print(f"  GPU       : {torch.cuda.get_device_name(0)}")
        vram = torch.cuda.get_device_properties(0).total_memory / 1024**3
        print(f"  VRAM      : {vram:.1f} GB")
        cc = torch.cuda.get_device_capability(0)
        label = ""
        if cc[0] >= 9:
            label = "  (Blackwell - use bfloat16)"
        elif cc[0] >= 8:
            label = "  (Ampere - bfloat16 supported)"
        else:
            label = "  (pre-Ampere - set dtype: float16 in config)"
        print(f"  Compute   : {cc[0]}.{cc[1]}{label}")
    else:
        print("  WARNING: No CUDA GPU found. Training on CPU will be very slow.")
except ImportError:
    print("  ERROR: PyTorch not installed.")
    print("  Install: pip install --pre torch --index-url https://download.pytorch.org/whl/nightly/cu128")
    sys.exit(1)

print()
print("--- Required packages ---")
pkgs = [("sentencepiece", "sentencepiece"), ("gradio", "gradio"),
        ("tensorboard", "tensorboard"), ("tqdm", "tqdm"),
        ("yaml", "pyyaml"), ("numpy", "numpy")]
all_ok = True
for mod, pip_name in pkgs:
    try:
        m = importlib.import_module(mod)
        ver = getattr(m, "__version__", "?")
        print(f"  [OK]      {pip_name} {ver}")
    except ImportError:
        print(f"  [MISSING] {pip_name}")
        all_ok = False
if not all_ok:
    print()
    print("  Install missing: pip install -r requirements.txt")
    sys.exit(1)
'@ | Out-File -FilePath $checkPy -Encoding utf8

python $checkPy
$exitCode = $LASTEXITCODE
Remove-Item $checkPy -ErrorAction SilentlyContinue

if ($exitCode -ne 0) {
    Write-Host ""
    Write-Host "Fix the issues above before continuing." -ForegroundColor Red
    exit 1
}

# Data directory
Write-Host ""
Write-Host "--- Data ---"
if (-not (Test-Path "data\raw")) {
    New-Item -ItemType Directory -Path "data\raw" -Force | Out-Null
    Write-Host "  Created   : data\raw\"
}
$dataFiles = Get-ChildItem "data\raw" -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Extension -in ".txt", ".jsonl" }
if ($dataFiles.Count -gt 0) {
    Write-Host "  [OK] data\raw : $($dataFiles.Count) file(s) found"
} else {
    Write-Host "  [WAIT] data\raw is empty - add .txt or .jsonl files before running scripts 01-03."
    Write-Host "         See data\sample\ for the expected format."
}

# Checkpoints
Write-Host ""
Write-Host "--- Checkpoints ---"
$tokModel = "checkpoints\tokenizer\dprk_sp.model"
if (Test-Path $tokModel) {
    Write-Host "  [OK] Tokenizer : $tokModel"
} else {
    Write-Host "  [WAIT] Tokenizer not trained yet - run script 01 first."
}
$ckpts = Get-ChildItem "checkpoints" -Filter "ckpt_step*.pt" -ErrorAction SilentlyContinue
if ($ckpts -and $ckpts.Count -gt 0) {
    $latest = ($ckpts | Sort-Object Name | Select-Object -Last 1).Name
    Write-Host "  [OK] Latest checkpoint: $latest"
} else {
    Write-Host "  [WAIT] No model checkpoints yet - run scripts 02 and 03 first."
}

# Summary
Write-Host ""
Write-Host "=== Workflow ===" -ForegroundColor Green
Write-Host "  1. Add training data to data\raw\  (.txt or .jsonl)"
Write-Host '       {"text": "..."}'
Write-Host '       {"question": "...", "answer": "..."}'
Write-Host "  2. .\scripts\01_train_tokenizer.ps1"
Write-Host "  3. .\scripts\02_preprocess_data.ps1"
Write-Host "  4. .\scripts\03_train_model.ps1"
Write-Host "  5. .\scripts\04_run_app.ps1"
Write-Host ""
