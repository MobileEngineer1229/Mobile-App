# Launch the Gradio chat UI. Pass --share for a public link, --checkpoint <path> to load a specific ckpt.

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

Write-Host "[04] Launching Gradio app..." -ForegroundColor Cyan
python -m src.app.gradio_app --config config/model_config.yaml --server-port 8000 @args
if ($LASTEXITCODE -ne 0) { Write-Error "App exited with error (exit $LASTEXITCODE)"; exit $LASTEXITCODE }
