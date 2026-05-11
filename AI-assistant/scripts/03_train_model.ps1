# Train the GPT model. Pass extra flags through (e.g. --max-steps 50, --resume <path>).

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

Write-Host "[03] Training model..." -ForegroundColor Cyan
python -m src.train.train --config config/model_config.yaml @args
if ($LASTEXITCODE -ne 0) { Write-Error "Training failed (exit $LASTEXITCODE)"; exit $LASTEXITCODE }
Write-Host "[03] Done." -ForegroundColor Green
