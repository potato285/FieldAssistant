#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
cd "$FIELD_ROOT"
mkdir -p evidence
TEST_CLASS="${1:-org.fieldassistant.VisionSmokeTest}"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# Direct instrumentation retains the separately provisioned model files.
result_log="$(mktemp "$FIELD_ROOT/evidence/instrumentation.XXXXXX")"
adb shell am instrument -w -r -e class "$TEST_CLASS" org.fieldassistant.test/androidx.test.runner.AndroidJUnitRunner | tee "$result_log"
# adb can return 0 even when an instrumented test fails.
if ! rg -q '^OK \([0-9]+ tests?\)' "$result_log"; then
    echo "Instrumentation did not report success; see $result_log" >&2
    exit 1
fi
