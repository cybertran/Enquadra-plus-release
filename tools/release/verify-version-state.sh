#!/usr/bin/env bash
set -euo pipefail

STATE_FILE="RELEASE_STATE.yml"
GRADLE_FILE="app/build.gradle.kts"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

[[ -f "$STATE_FILE" ]] || fail "$STATE_FILE not found"
[[ -f "$GRADLE_FILE" ]] || fail "$GRADLE_FILE not found"

state_name="$(sed -nE 's/^versionName:[[:space:]]*"([^"]+)".*/\1/p' "$STATE_FILE")"
state_code="$(sed -nE 's/^versionCode:[[:space:]]*([0-9]+).*/\1/p' "$STATE_FILE")"
source_commit="$(sed -nE 's/^sourceCommit:[[:space:]]*"([0-9a-fA-F]+)".*/\1/p' "$STATE_FILE")"

gradle_name="$(sed -nE 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$GRADLE_FILE" | head -n1)"
gradle_code="$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*([0-9]+).*/\1/p' "$GRADLE_FILE" | head -n1)"
application_id="$(sed -nE 's/^[[:space:]]*applicationId[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$GRADLE_FILE" | head -n1)"

[[ -n "$state_name" ]] || fail "versionName missing from $STATE_FILE"
[[ -n "$state_code" ]] || fail "versionCode missing from $STATE_FILE"
[[ "$source_commit" =~ ^[0-9a-fA-F]{40}$ ]] || fail "sourceCommit is not a 40-character Git SHA"
[[ "$gradle_name" == "$state_name" ]] || fail "versionName mismatch: state=$state_name gradle=$gradle_name"
[[ "$gradle_code" == "$state_code" ]] || fail "versionCode mismatch: state=$state_code gradle=$gradle_code"
[[ -n "$application_id" ]] || fail "applicationId missing from $GRADLE_FILE"

echo "Version state OK: $state_name (versionCode $state_code)"
echo "Canonical source commit: $source_commit"

if [[ $# -ge 1 ]]; then
  apk="$1"
  [[ -f "$apk" ]] || fail "APK not found: $apk"

  if [[ -n "${ANDROID_HOME:-}" ]]; then
    aapt="$(find "$ANDROID_HOME/build-tools" -type f -name aapt -print 2>/dev/null | sort -V | tail -n1)"
  else
    aapt="$(command -v aapt || true)"
  fi
  [[ -n "$aapt" ]] || fail "aapt not found"

  badging="$("$aapt" dump badging "$apk" | head -n1)"
  apk_package="$(printf '%s\n' "$badging" | sed -nE "s/^package: name='([^']+)'.*/\1/p")"
  apk_code="$(printf '%s\n' "$badging" | sed -nE "s/.*versionCode='([^']+)'.*/\1/p")"
  apk_name="$(printf '%s\n' "$badging" | sed -nE "s/.*versionName='([^']+)'.*/\1/p")"

  [[ "$apk_package" == "$application_id" ]] || fail "APK package mismatch: expected=$application_id actual=$apk_package"
  [[ "$apk_code" == "$state_code" ]] || fail "APK versionCode mismatch: expected=$state_code actual=$apk_code"
  [[ "$apk_name" == "$state_name" ]] || fail "APK versionName mismatch: expected=$state_name actual=$apk_name"

  echo "APK version OK: $apk_package $apk_name (versionCode $apk_code)"
fi
