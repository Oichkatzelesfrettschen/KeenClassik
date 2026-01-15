#!/usr/bin/env bash
set -euo pipefail

export PATH=$PATH:/opt/android-sdk/platform-tools

FLAVOR="${KEEN_FLAVOR:-${FLAVOR:-classik}}"
FLAVOR_CAP="${FLAVOR^}"
TASK="assemble${FLAVOR_CAP}Debug"
APK="app/build/outputs/apk/${FLAVOR}/debug/app-${FLAVOR}-debug.apk"
PACKAGE="com.oichkatzelesfrettschen.keenclassik.${FLAVOR}"

echo "Building APK (${FLAVOR})..."
./gradlew "$TASK"

if [ ! -f "$APK" ]; then
  echo "APK not found: $APK" >&2
  exit 1
fi

echo "Installing APK..."
adb install -r "$APK"

echo "Launching Keen (${FLAVOR}) with custom seed..."
adb shell am start -n "${PACKAGE}/.KeenActivity" --ei gameSize 5 --ei gameDiff 1 --ei gameMultOnly 0 --el gameSeed 12345
