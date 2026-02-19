$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$BackendDir = Join-Path $RootDir 'backend'
$FrontendDir = Join-Path $RootDir 'frontend'

function Resolve-ExecutablePath {
  param(
    [Parameter(Mandatory = $true)]
    [string[]]$Candidates
  )

  foreach ($name in $Candidates) {
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if (-not $cmd) { continue }
    if ($cmd.CommandType -ne 'Application') { continue }
    if (-not $cmd.Path) { continue }
    return $cmd.Path
  }

  return $null
}

$MvnPath = Resolve-ExecutablePath -Candidates @('mvn.cmd', 'mvn.exe', 'mvn')
if (-not $MvnPath) {
  Write-Host 'Missing command: mvn (try installing Maven and ensure mvn.cmd is in PATH)'
  exit 1
}

$NpmPath = Resolve-ExecutablePath -Candidates @('npm.cmd', 'npm.exe', 'npm')
if (-not $NpmPath) {
  Write-Host 'Missing command: npm (try installing Node.js and ensure npm.cmd is in PATH)'
  exit 1
}

if ([string]::IsNullOrWhiteSpace($env:VITE_API_BASE_URL)) {
  $env:VITE_API_BASE_URL = 'http://localhost:8080'
}

if ($args.Count -gt 0 -and $args[0] -eq '--dry-run') {
  Write-Host "ROOT_DIR=$RootDir"
  Write-Host "VITE_API_BASE_URL=$($env:VITE_API_BASE_URL)"
  Write-Host "MVN=$MvnPath"
  Write-Host "NPM=$NpmPath"
  Write-Host "(cd `"$BackendDir`" ; mvn -DskipTests spring-boot:run)"
  Write-Host "(cd `"$FrontendDir`" ; npm run dev)"
  exit 0
}

$nodeModulesDir = Join-Path $FrontendDir 'node_modules'
if (-not (Test-Path $nodeModulesDir)) {
  Push-Location $FrontendDir
  try {
    & $NpmPath i
  } finally {
    Pop-Location
  }
}

$backend = $null
$frontend = $null
$cancelHandler = $null

try {
  $backend = Start-Process -FilePath $MvnPath -ArgumentList @('-DskipTests', 'spring-boot:run') -WorkingDirectory $BackendDir -PassThru -NoNewWindow
  $frontend = Start-Process -FilePath $NpmPath -ArgumentList @('run', 'dev') -WorkingDirectory $FrontendDir -PassThru -NoNewWindow

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
