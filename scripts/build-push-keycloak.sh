#!/usr/bin/env bash
# Build et push de l'image Keycloak custom (thème Takalo) sur GHCR.
#
# Usage:
#   ./scripts/build-push-keycloak.sh             # tag latest + sha
#   ./scripts/build-push-keycloak.sh 0.1.0       # tag latest + 0.1.0 + sha
#
# Pré-requis:
#   - docker login ghcr.io (PAT avec scope write:packages)

set -euo pipefail

IMAGE="ghcr.io/ranto/takalo-keycloak"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTEXT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
DOCKERFILE="${CONTEXT_DIR}/keycloak/Dockerfile"

SHA="$(git -C "${CONTEXT_DIR}" rev-parse --short HEAD)"
VERSION_TAG="${1:-}"

TAGS=("latest" "${SHA}")
[[ -n "${VERSION_TAG}" ]] && TAGS+=("${VERSION_TAG}")

BUILD_ARGS=()
for t in "${TAGS[@]}"; do
  BUILD_ARGS+=("-t" "${IMAGE}:${t}")
done

echo ">> Build ${IMAGE} (tags: ${TAGS[*]})"
docker build -f "${DOCKERFILE}" "${BUILD_ARGS[@]}" "${CONTEXT_DIR}"

for t in "${TAGS[@]}"; do
  echo ">> Push ${IMAGE}:${t}"
  docker push "${IMAGE}:${t}"
done

echo ">> Done. Image: ${IMAGE}"
echo "   Tags: ${TAGS[*]}"
