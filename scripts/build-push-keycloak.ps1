# Build et push de l'image Keycloak custom (thème Takalo) sur GHCR.
#
# Usage:
#   .\scripts\build-push-keycloak.ps1             # tag latest + sha
#   .\scripts\build-push-keycloak.ps1 0.1.0       # tag latest + 0.1.0 + sha
#
# Pré-requis:
#   - docker login ghcr.io (PAT avec scope write:packages)

[CmdletBinding()]
param(
    [string]$VersionTag
)

$ErrorActionPreference = 'Stop'

$Image = 'ghcr.io/ranto/takalo-keycloak'

$ScriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$ContextDir = Resolve-Path (Join-Path $ScriptDir '..')
$Dockerfile = Join-Path $ContextDir 'keycloak\Dockerfile'

$Sha = (git -C $ContextDir rev-parse --short HEAD).Trim()

$Tags = @('latest', $Sha)
if ($VersionTag) { $Tags += $VersionTag }

$BuildArgs = @('-f', $Dockerfile)
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
