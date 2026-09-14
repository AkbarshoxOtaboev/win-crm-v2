#!/usr/bin/env bash
# Local development: Spring Boot (8081) + Vite TailAdmin (5174).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

cleanup() {
  if [[ -n "${BACKEND_PID:-}" ]] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    kill "$BACKEND_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

echo "==> Starting backend on :8081"
./mvnw spring-boot:run &
BACKEND_PID=$!

cd "$ROOT/frontend"
if [[ ! -d node_modules ]]; then
  echo "==> npm install (frontend)"
  npm install
fi

echo "==> Starting frontend (TailAdmin) on :5174"
npm run dev
