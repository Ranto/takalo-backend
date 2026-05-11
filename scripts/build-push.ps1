# Build et push de l'image backend sur GitHub Container Registry (GHCR).
#
# Usage:
#   .\scripts\build-push.ps1             # tag latest + sha
#   .\scripts\build-push.ps1 0.1.0       # tag latest + 0.1.0 + sha
#
# Pré-requis:
#   - docker login ghcr.io (PAT avec scope write:packages)

[CmdletBinding()]
param(
    [string]$VersionTag
)

$ErrorActionPreference = 'Stop'

$Image = 'ghcr.io/ranto/takalo-backend'

$ScriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$ContextDir = Resolve-Path (Join-Path $ScriptDir '..')

$Sha = (git -C $ContextDir rev-parse --short HEAD).Trim()

$Tags = @('latest', $Sha)
if ($VersionTag) { $Tags += $VersionTag }

$BuildArgs = @()
foreach ($t in $Tags) { $BuildArgs += @('-t', "${Image}:${t}") }

Write-Host ">> Build $Image (tags: $($Tags -join ', '))"
docker build @BuildArgs $ContextDir
if ($LASTEXITCODE -ne 0) { throw "docker build failed" }

foreach ($t in $Tags) {
    Write-Host ">> Push ${Image}:${t}"
    docker push "${Image}:${t}"
    if ($LASTEXITCODE -ne 0) { throw "docker push failed for ${t}" }
}

Write-Host ">> Done. Image: $Image"
Write-Host "   Tags: $($Tags -join ', ')"
