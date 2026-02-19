$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$BackendDir = Join-Path $RootDir 'backend'
$FrontendDir = Join-Path $RootDir 'frontend'

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
  Write-Host 'Missing command: mvn'
  exit 1
}
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
  Write-Host 'Missing command: npm'
  exit 1
}

if ([string]::IsNullOrWhiteSpace($env:VITE_API_BASE_URL)) {
  $env:VITE_API_BASE_URL = 'http://localhost:8080'
}

if ($args.Count -gt 0 -and $args[0] -eq '--dry-run') {
  Write-Host "ROOT_DIR=$RootDir"
  Write-Host "VITE_API_BASE_URL=$($env:VITE_API_BASE_URL)"
  Write-Host "(cd `"$BackendDir`" ; mvn -DskipTests spring-boot:run)"
  Write-Host "(cd `"$FrontendDir`" ; npm run dev)"
  exit 0
}

$nodeModulesDir = Join-Path $FrontendDir 'node_modules'
if (-not (Test-Path $nodeModulesDir)) {
  Push-Location $FrontendDir
  try {
    npm i
  } finally {
    Pop-Location
  }
}

$backend = $null
$frontend = $null
$cancelHandler = $null

try {
  $backend = Start-Process -FilePath 'mvn' -ArgumentList @('-DskipTests', 'spring-boot:run') -WorkingDirectory $BackendDir -PassThru -NoNewWindow
  $frontend = Start-Process -FilePath 'npm' -ArgumentList @('run', 'dev') -WorkingDirectory $FrontendDir -PassThru -NoNewWindow

  $cancelHandler = [ConsoleCancelEventHandler]{
    param($sender, $e)
    $e.Cancel = $true
    if ($script:__stopping) { return }
    $script:__stopping = $true
    if ($backend -and -not $backend.HasExited) { Stop-Process -Id $backend.Id -Force -ErrorAction SilentlyContinue }
    if ($frontend -and -not $frontend.HasExited) { Stop-Process -Id $frontend.Id -Force -ErrorAction SilentlyContinue }
  }
  [Console]::add_CancelKeyPress($cancelHandler)

  Write-Host 'Backend:  http://localhost:8080'
  Write-Host 'Frontend: http://localhost:5173'
  Write-Host 'Press Ctrl+C to exit'

  while ($true) {
    $backend.Refresh()
    $frontend.Refresh()

    if ($backend.HasExited -or $frontend.HasExited) {
      exit 1
    }

    Start-Sleep -Seconds 1
  }
} finally {
  if ($cancelHandler) {
    [Console]::remove_CancelKeyPress($cancelHandler)
  }
  if ($backend -and -not $backend.HasExited) { Stop-Process -Id $backend.Id -Force -ErrorAction SilentlyContinue }
  if ($frontend -and -not $frontend.HasExited) { Stop-Process -Id $frontend.Id -Force -ErrorAction SilentlyContinue }
}
