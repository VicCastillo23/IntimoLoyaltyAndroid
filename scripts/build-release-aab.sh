#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -f keystore.properties ]]; then
  echo "Falta keystore.properties — copia keystore.properties.example y configura la firma."
  exit 1
fi

./gradlew bundleRelease --no-daemon
AAB="$ROOT/app/build/outputs/bundle/release/app-release.aab"
echo ""
echo "Sube este archivo a Play Console (Prueba cerrada → Crear versión):"
echo "$AAB"
ls -lh "$AAB"
