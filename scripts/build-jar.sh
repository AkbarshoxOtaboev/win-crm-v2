#!/usr/bin/env bash
# Build frontend (TailAdmin Vue) + Spring Boot JAR for VDS deploy.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "==> Building WinCRM (Vue + Spring Boot JAR)"
./mvnw -DskipTests package

JAR="$(ls -1 target/win-crm-*.jar | grep -v '\.original$' | head -n1)"
echo "==> Done: $JAR"
echo "    Copy this JAR + .env to the VDS, then: scripts/run-prod.sh"
