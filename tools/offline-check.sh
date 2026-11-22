#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
cd "$FIELD_ROOT"
mkdir -p evidence
if [[ "${1:-}" != "--allow-network-interruption" ]]; then
    echo "This test disconnects the phone and any laptop using its hotspot/USB tethering."
    echo "Do not run on the current shared connection. Existing offline evidence is in evidence/."
    echo "When an independent connection is available, explicitly pass --allow-network-interruption."
    exit 2
fi
prior_airplane="$(adb shell settings get global airplane_mode_on | tr -d '\r')"
prior_wifi="$(adb shell settings get global wifi_on | tr -d '\r')"
prior_mobile="$(adb shell settings get global mobile_data | tr -d '\r')"
restore() {
    if [[ "$prior_airplane" == "1" ]]; then
        adb shell cmd connectivity airplane-mode enable
    else
        adb shell cmd connectivity airplane-mode disable
    fi
    if [[ "$prior_wifi" == "1" || "$prior_wifi" == "2" ]]; then adb shell svc wifi enable; else adb shell svc wifi disable; fi
    echo "Restored network settings (airplane, Wi-Fi, mobile-data preference):"
    adb shell settings get global airplane_mode_on
    adb shell cmd wifi status
    adb shell settings get global mobile_data
}
trap restore EXIT
echo "Original airplane=$prior_airplane wifi=$prior_wifi mobile_data=$prior_mobile"
adb shell cmd connectivity airplane-mode enable
adb shell svc wifi disable
sleep 2
test "$(adb shell settings get global airplane_mode_on | tr -d '\r')" == "1"
# The mobile_data preference may stay 1 while airplane mode disables the radio.
# Do not change that preference or mistake it for an active cellular connection.
echo "Verified airplane_mode_on=1"
wifi_status="$(adb shell cmd wifi status)"
echo "$wifi_status"
[[ "$wifi_status" == *"Wifi is disabled"* ]]
adb shell dumpsys connectivity | rg 'Active default network:' | tee evidence/offline-connectivity.txt
rg -q 'Active default network: none' evidence/offline-connectivity.txt
adb shell am instrument -w -r -e class org.fieldassistant.VisionSmokeTest,org.fieldassistant.LlamaSmokeTest,org.fieldassistant.SpeechSynthesisTest \
    org.fieldassistant.test/androidx.test.runner.AndroidJUnitRunner | tee evidence/offline-instrumentation.txt
rg -q '^OK \(3 tests\)' evidence/offline-instrumentation.txt
adb pull /sdcard/Android/data/org.fieldassistant/files/evidence/vision-smoke.json evidence/vision-phone-offline.json
adb pull /sdcard/Android/data/org.fieldassistant/files/evidence/llama-smoke.json evidence/llama-phone-offline.json
adb pull /sdcard/Android/data/org.fieldassistant/files/evidence/offline-speech.json evidence/offline-speech.json
adb pull /sdcard/Android/data/org.fieldassistant/files/evidence/offline-speech-en.wav evidence/offline-speech-en.wav
adb pull /sdcard/Android/data/org.fieldassistant/files/evidence/offline-speech-ta.wav evidence/offline-speech-ta.wav
python3 - <<'PY'
import json
for name in ['vision-phone-offline.json', 'llama-phone-offline.json']:
    report=json.load(open('evidence/'+name))
    assert report['airplane_mode'] == 1, name
    if name.startswith('llama'): assert report['status'] == 'SUCCESS', report
print('Both real inference reports confirm airplane mode was enabled.')
PY
