#!/usr/bin/env bash
#
# Downloads the JetBrains kotlin-lsp standalone build for local development /
# verification. kode never bundles the LSP (design ch. 04); this just fetches a
# user-local copy and prints the launcher path.
#
# Usage:
#   scripts/fetch-kotlin-lsp.sh [dest-dir] [version]
#
# Then:
#   export KODE_LSP_PATH="$(scripts/fetch-kotlin-lsp.sh)"
#   ./gradlew :adapter:test --tests '*LspIntegrationTest*'
#
# NOTE on network egress: the standalone builds are hosted on
# `download-cdn.jetbrains.com`. In sandboxed/CI environments that host must be on
# the network allowlist, otherwise the download returns HTTP 403
# ("Host not in allowlist"). GitHub-only allowlists are not sufficient.
#
set -euo pipefail

DEST_DIR="${1:-${HOME}/.cache/kode/kotlin-lsp}"
VERSION="${2:-262.8190.0}"
BASE="https://download-cdn.jetbrains.com/language-server/kotlin-server/${VERSION}"

# Pick the artifact for this OS/arch.
os="$(uname -s)"
arch="$(uname -m)"
case "${os}/${arch}" in
    Linux/x86_64)        ARTIFACT="kotlin-server-${VERSION}.tar.gz" ;;
    Linux/aarch64)       ARTIFACT="kotlin-server-${VERSION}-aarch64.tar.gz" ;;
    Darwin/x86_64)       ARTIFACT="kotlin-server-${VERSION}.tar.gz" ;;
    Darwin/arm64)        ARTIFACT="kotlin-server-${VERSION}-aarch64.tar.gz" ;;
    *) echo "Unsupported platform ${os}/${arch}; download manually from the release page." >&2; exit 1 ;;
esac
URL="${BASE}/${ARTIFACT}"

find_launcher() {
    find "${DEST_DIR}" -maxdepth 3 -name 'kotlin-lsp.sh' 2>/dev/null | head -n1
}

existing="$(find_launcher || true)"
if [[ -n "${existing}" && -x "${existing}" ]]; then
    echo "${existing}"
    exit 0
fi

mkdir -p "${DEST_DIR}"
TMP="$(mktemp)"
trap 'rm -f "${TMP}"' EXIT

echo "Downloading ${URL}" >&2
curl -fsSL "${URL}" -o "${TMP}"
tar -xzf "${TMP}" -C "${DEST_DIR}"

LAUNCHER="$(find_launcher || true)"
[[ -n "${LAUNCHER}" ]] || { echo "kotlin-lsp.sh not found in archive" >&2; exit 1; }
chmod +x "${LAUNCHER}"
echo "${LAUNCHER}"
