#!/usr/bin/env bash
# Build et push de l'image backend sur GitHub Container Registry (GHCR).
#
# Usage:
#   ./scripts/build-push.sh             # tag latest + sha
#   ./scripts/build-push.sh 0.1.0       # tag latest + 0.1.0 + sha
#
# Pré-requis:
#   - docker login ghcr.io (PAT avec scope write:packages)

set -euo pipefail

IMAGE="ghcr.io/ranto/takalo-backend"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTEXT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

SHA="$(git -C "${CONTEXT_DIR}" rev-parse --short HEAD)"
VERSION_TAG="${1:-}"

TAGS=("latest" "${SHA}")
[[ -n "${VERSION_TAG}" ]] && TAGS+=("${VERSION_TAG}")

BUILD_ARGS=()
for t in "${TAGS[@]}"; do
  BUILD_ARGS+=("-t" "${IMAGE}:${t}")
done

echo ">> Build ${IMAGE} (tags: ${TAGS[*]})"
docker build "${BUILD_ARGS[@]}" "${CONTEXT_DIR}"

for t in "${TAGS[@]}"; do
  echo ">> Push ${IMAGE}:${t}"
  docker push "${IMAGE}:${t}"
done

echo ">> Done. Image: ${IMAGE}"
echo "   Tags: ${TAGS[*]}"