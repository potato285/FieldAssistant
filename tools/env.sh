#!/usr/bin/env bash
# Source from any directory. Keep development downloads/caches inside the project.
FIELD_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export JAVA_HOME="${FIELD_JAVA_HOME:-/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home}"
export ANDROID_HOME="$FIELD_ROOT/tools/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_USER_HOME="$FIELD_ROOT/.android"
export GRADLE_USER_HOME="$FIELD_ROOT/.gradle"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$FIELD_ROOT/tools/gradle-8.9/bin:$PATH"
