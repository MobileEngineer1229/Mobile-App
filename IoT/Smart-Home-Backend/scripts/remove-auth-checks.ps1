# PowerShell script to remove redundant authentication checks
# This script removes "if (!req.user)" blocks from all controllers
# The authenticate middleware already handles authentication

Write-Host "Removing redundant authentication checks from controllers..." -ForegroundColor Green

$controllers = Get-ChildItem -Path "src\controllers\*.ts" -Recurse

foreach ($file in $controllers) {
    $content = Get-Content $file.FullName -Raw
    $originalContent = $content
    
    # Pattern 1: Standard if (!req.user) block
    $content = $content -replace '(?s)if \(!req\.user\) \{\s+sendError\(res, ''UNAUTHORIZED'', ''User not authenticated'', 401\);\s+return;\s+\}\s+', ''
    
    # Pattern 2: req.user?.id check (chatbot controller style)
    $content = $content -replace '(?s)const userId = req\.user\?\.id;\s+if \(!userId\) \{\s+sendError\(res, ''UNAUTHORIZED'', ''User not authenticated'', 401\);\s+return;\s+\}\s+', ''
    
    if ($content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -NoNewline
        Write-Host "Updated: $($file.Name)" -ForegroundColor Yellow
    }
}

Write-Host "Done! Now update req.user.id to req.user!.id where needed." -ForegroundColor Green
