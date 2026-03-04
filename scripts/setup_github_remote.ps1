param(
  [Parameter(Mandatory = $true)]
  [string]$RepoUrl
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot

Write-Host "[1/4] Validate git repo..."
git -c safe.directory=$repoRoot -C $repoRoot rev-parse --is-inside-work-tree | Out-Null

Write-Host "[2/4] Set origin..."
$existing = git -c safe.directory=$repoRoot -C $repoRoot remote
if ($existing -match "^origin$") {
  git -c safe.directory=$repoRoot -C $repoRoot remote set-url origin $RepoUrl
} else {
  git -c safe.directory=$repoRoot -C $repoRoot remote add origin $RepoUrl
}

Write-Host "[3/4] Push main..."
git -c safe.directory=$repoRoot -C $repoRoot push -u origin main

Write-Host "[4/4] Done."
Write-Host "origin => $RepoUrl"
