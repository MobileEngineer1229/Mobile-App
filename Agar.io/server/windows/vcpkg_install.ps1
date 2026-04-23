param([int]$MaxAttempts = 30)

$vcpkg      = Resolve-Path "..\..\vcpkg\vcpkg.exe"
$downloads  = Resolve-Path "..\..\vcpkg\downloads"
$attempt    = 0

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

Write-Host "=== vcpkg install with PowerShell download fallback ===" -ForegroundColor Cyan
Write-Host "vcpkg: $vcpkg"
Write-Host "downloads: $downloads"
Write-Host ""

while ($attempt -lt $MaxAttempts) {
    $attempt++
    Write-Host "--- Attempt $attempt ---" -ForegroundColor Yellow

    # Capture output line-by-line
    $lines = & $vcpkg install --triplet x64-windows 2>&1
    foreach ($line in $lines) { Write-Host $line }
    $exitCode = $LASTEXITCODE

    if ($exitCode -eq 0) {
        Write-Host ""
        Write-Host "=== SUCCESS: All packages installed! ===" -ForegroundColor Green
        Write-Host "Reload AgarServer.sln in Visual Studio and build." -ForegroundColor Green
        exit 0
    }

    # Find lines like: "Downloading https://... -> filename.tar.gz"
    # followed by an SSL error
    $outputText = $lines -join "`n"
    $sslFail = $outputText -match "curl operation failed with error code 35"

    if (-not $sslFail) {
        Write-Host ""
        Write-Host "=== Non-SSL build failure. Check output above. ===" -ForegroundColor Red
        exit 1
    }

    # Extract URL and destination filename from the output
    # Pattern: "Downloading <url> -> <filename>"
    $downloadLines = $lines | Where-Object { $_ -match "^Downloading https://.+ -> .+" }
    # The failed one is just before the SSL error — take the last match
    $failedLine = $downloadLines | Select-Object -Last 1

    if ($failedLine -match "Downloading (https://\S+) -> (\S+)") {
        $url      = $Matches[1]
        $filename = $Matches[2]
        $dest     = Join-Path $downloads $filename

        if (Test-Path $dest) {
            Write-Host "[Skip] Already downloaded: $filename" -ForegroundColor Gray
        } else {
            Write-Host ""
            Write-Host "[PS] Downloading via PowerShell: $filename" -ForegroundColor Cyan
            Write-Host "     URL: $url"
            try {
                Invoke-WebRequest -Uri $url -OutFile $dest -UseBasicParsing -TimeoutSec 120
                Write-Host "[PS] Done: $filename" -ForegroundColor Green
            } catch {
                Write-Host "[PS] Failed: $_" -ForegroundColor Red
                Write-Host "     Retrying vcpkg directly..." -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "[WARN] Could not parse failed download URL. Retrying..." -ForegroundColor Yellow
    }

    Start-Sleep -Seconds 1
}

Write-Host "=== Exceeded $MaxAttempts attempts ===" -ForegroundColor Red
exit 1
