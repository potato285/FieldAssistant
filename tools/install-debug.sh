#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
cd "$FIELD_ROOT"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n org.fieldassistant/.MainActivity
