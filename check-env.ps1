function Get-MergedEnvValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $processValue = [Environment]::GetEnvironmentVariable($Name, "Process")
    if (-not [string]::IsNullOrEmpty($processValue)) {
        return $processValue
    }

    $userValue = [Environment]::GetEnvironmentVariable($Name, "User")
    if (-not [string]::IsNullOrEmpty($userValue)) {
        return $userValue
    }

    return [Environment]::GetEnvironmentVariable($Name, "Machine")
}

Write-Host "Checking environment variables..." -ForegroundColor Yellow

$requiredVars = @("QWEN_API_KEY")
$mapVars = @("AMAP_JS_KEY", "AMAP_WEB_KEY")
$missingVars = @()

foreach ($var in $requiredVars) {
    $value = Get-MergedEnvValue -Name $var
    if ([string]::IsNullOrEmpty($value)) {
        $missingVars += $var
        Write-Host "ERROR ${var}: missing" -ForegroundColor Red
    } else {
        Write-Host "OK ${var}: set" -ForegroundColor Green
    }
}

$configuredMapVars = @()
foreach ($var in $mapVars) {
    $value = Get-MergedEnvValue -Name $var
    if ([string]::IsNullOrEmpty($value)) {
        Write-Host "WARN ${var}: missing" -ForegroundColor DarkYellow
    } else {
        $configuredMapVars += $var
        Write-Host "OK ${var}: set" -ForegroundColor Green
    }
}

$amapSecurityKey = Get-MergedEnvValue -Name "AMAP_JS_SECURITY_KEY"
if ([string]::IsNullOrEmpty($amapSecurityKey)) {
    Write-Host "WARN AMAP_JS_SECURITY_KEY: missing (frontend map pages may need it)" -ForegroundColor DarkYellow
} else {
    Write-Host "OK AMAP_JS_SECURITY_KEY: set" -ForegroundColor Green
}

if ($configuredMapVars.Count -eq 0) {
    $missingVars += "AMAP_JS_KEY or AMAP_WEB_KEY (at least one)"
}

if ($missingVars.Count -gt 0) {
    Write-Host ""
    Write-Host "Missing required variables:" -ForegroundColor Yellow
    $missingVars | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    Write-Host ""
    Write-Host "PowerShell examples:" -ForegroundColor Cyan
    Write-Host "  `$env:QWEN_API_KEY='your_api_key'" -ForegroundColor Gray
    Write-Host "  `$env:AMAP_JS_KEY='your_js_key'" -ForegroundColor Gray
    Write-Host "  `$env:AMAP_WEB_KEY='your_web_key'" -ForegroundColor Gray
    Write-Host "  `$env:AMAP_JS_SECURITY_KEY='your_js_security_code'" -ForegroundColor Gray
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "Required startup variables are configured." -ForegroundColor Green
Write-Host "Application startup can proceed." -ForegroundColor Green
