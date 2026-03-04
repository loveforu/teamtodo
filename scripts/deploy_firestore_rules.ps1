param(
    [Parameter(Mandatory = $true)]
    [string]$FirebaseToken,
    [string]$ProjectId = "teamtodo-app"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$env:XDG_CONFIG_HOME = Join-Path $root ".xdg-config"
New-Item -ItemType Directory -Force -Path $env:XDG_CONFIG_HOME | Out-Null

Write-Host "Deploying firestore.rules to project: $ProjectId"
firebase.cmd deploy --only firestore:rules --project $ProjectId --token $FirebaseToken

