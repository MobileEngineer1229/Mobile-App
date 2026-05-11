# Train SentencePiece BPE tokenizer on data/raw/.
# Run from anywhere; we cd to the project root first.

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

Write-Host "[01] Training tokenizer..." -ForegroundColor Cyan
python -m src.tokenizer.train_tokenizer --config config/model_config.yaml @args
if ($LASTEXITCODE -ne 0) { Write-Error "Tokenizer training failed (exit $LASTEXITCODE)"; exit $LASTEXITCODE }
Write-Host "[01] Done." -ForegroundColor Green
