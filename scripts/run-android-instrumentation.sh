#!/bin/sh
set -eu

device="emulator-$EMULATOR_PORT"

# sys.boot_completed can be true before Android's package manager is registered.
# Check the package service before Gradle tries to install APKs.
wait_for_package_manager() {
  max_attempts="$1"
  attempt=1
  while [ "$attempt" -le "$max_attempts" ]; do
    boot_status="$(adb -s "$device" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    package_status="$(adb -s "$device" shell service check package 2>&1 || true)"
    if [ "$boot_status" = "1" ] && printf '%s' "$package_status" | grep -Fq "Service package: found"; then
      echo "Android boot and package manager are ready."
      return 0
    fi
    attempt=$((attempt + 1))
    sleep 2
  done
  return 1
}

if ! wait_for_package_manager 60; then
  echo "::warning::Emulator boot completed without a usable package manager; rebooting once."
  adb -s "$device" reboot || true
  adb -s "$device" wait-for-device
  if ! wait_for_package_manager 180; then
    adb -s "$device" shell service check package || true
    echo "::error::Android package manager did not become available after emulator reboot."
    exit 1
  fi
fi

run_instrumentation() {
  log_file="$1"
  if ./gradlew --no-daemon --stacktrace connectedDebugAndroidTest > "$log_file" 2>&1; then
    cat "$log_file"
    return 0
  else
    result=$?
    cat "$log_file"
    return "$result"
  fi
}

if run_instrumentation instrumentation-attempt-1.log; then
  exit 0
fi

# Retry only the specific emulator package-service failure; never mask real tests.
if ! grep -Fq "Can't find service: package" instrumentation-attempt-1.log; then
  echo "::error::Instrumentation failed for a reason other than missing Android package service; not retrying."
  exit 1
fi

echo "::warning::Android package service disappeared during APK installation; rebooting and retrying instrumentation once."
adb -s "$device" reboot || true
adb -s "$device" wait-for-device
if ! wait_for_package_manager 180; then
  adb -s "$device" shell service check package || true
  echo "::error::Android package manager remained unavailable after retry reboot."
  exit 1
fi
run_instrumentation instrumentation-attempt-2.log
