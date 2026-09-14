#!/usr/bin/env bash
# Run production JAR on VDS using .env next to this script's parent or CWD.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ENV_FILE="${ENV_FILE:-$ROOT/.env}"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE — copy .env.example to .env and fill values."
  exit 1
fi

# Export KEY=VALUE from .env (skip comments / blank lines)
set -a
# shellcheck disable=SC1090
source <(grep -v '^\s*#' "$ENV_FILE" | grep -v '^\s*$' | sed 's/\r$//')
set +a

JAR="${JAR_PATH:-}"
if [[ -z "$JAR" ]]; then
  JAR="$(ls -1 "$ROOT"/target/win-crm-*.jar 2>/dev/null | grep -v '\.original$' | head -n1 || true)"
fi
if [[ -z "$JAR" || ! -f "$JAR" ]]; then
  echo "JAR not found. Build first: scripts/build-jar.sh"
  exit 1
fi

UPLOAD_DIR="${FILE_UPLOAD_DIR:-/var/wincrm/uploads}"
mkdir -p "$UPLOAD_DIR"

echo "==> Starting $JAR (profile=${SPRING_PROFILES_ACTIVE:-prod})"
exec java -jar "$JAR" --spring.profiles.active="${SPRING_PROFILES_ACTIVE:-prod}"
