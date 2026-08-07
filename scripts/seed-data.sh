#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080/api}"

curl -sS -X POST "${API_BASE_URL}/admin/seed" \
  -H "Content-Type: application/json"

echo
