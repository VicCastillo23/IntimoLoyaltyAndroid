#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"

if [[ ! -f "$APK" ]]; then
  echo "Compilando debug…"
  (cd "$ROOT" && ./gradlew assembleDebug --no-daemon)
fi

echo "Esperando dispositivo USB (depuración activada)…"
adb wait-for-device
DEV="$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
if [ -z "$DEV" ]; then
  echo "No hay dispositivo autorizado. Revisa el popup «Permitir depuración USB» en el teléfono."
  adb devices -l
  exit 1
fi

echo "Instalando en $DEV…"
adb -s "$DEV" install -r "$APK"
echo "Listo: Intimo Loyalty (debug) instalada."
