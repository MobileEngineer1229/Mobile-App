# Tokenize and shard data/raw/ into data/processed/{train,val}.bin.

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

Write-Host "[02] Preprocessing data into token shards..." -ForegroundColor Cyan
python -m src.data.preprocess --config config/model_config.yaml @args
if ($LASTEXITCODE -ne 0) { Write-Error "Preprocessing failed (exit $LASTEXITCODE)"; exit $LASTEXITCODE }
Write-Host "[02] Done." -ForegroundColor Green
