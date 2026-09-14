#!/usr/bin/env bash
set -euo pipefail
set -x

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

rm -rf public/hr048 build/hr048 .tooling/hr048
mkdir -p public/hr048 build/hr048/master build/hr048/camera .tooling/hr048/bin signing

# Fail-closed software contracts first. HR048 integration preserves the baseline 0.10.1
# cross-component gate and explicitly validates the intentional drive-map remediation delta.
HR048_SKIP_INTEGRATION=1 python3 tools/audit_hr048_drive_contract.py | tee public/hr048/HR048-DRIVE-AUDIT.txt
python3 tools/audit_hr048_integration_contract.py | tee public/hr048/HR048-INTEGRATION-AUDIT.txt
python3 tools/audit_0101_motion_contract.py | tee public/hr048/MOTION-AUDIT.txt
python3 tools/audit_hr047_gas_safety_contract.py | tee public/hr048/GAS-SAFETY-AUDIT.txt

# Android controlled test/build environment.
base64 -d signing/hazrob-prototype.jks.b64 > signing/hazrob-prototype.jks
chmod 600 signing/hazrob-prototype.jks

curl -fsSL 'https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse' -o .tooling/hr048/jdk.tar.gz
mkdir -p .tooling/hr048/jdk
tar -xzf .tooling/hr048/jdk.tar.gz -C .tooling/hr048/jdk --strip-components=1
export JAVA_HOME="$ROOT/.tooling/hr048/jdk"
export PATH="$JAVA_HOME/bin:$PATH"
java -version 2>&1 | tee public/hr048/JAVA.txt

mkdir -p .tooling/hr048/android-sdk/cmdline-tools
curl -fsSL 'https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip' -o .tooling/hr048/android.zip
unzip -q .tooling/hr048/android.zip -d .tooling/hr048/android-sdk/cmdline-tools
mv .tooling/hr048/android-sdk/cmdline-tools/cmdline-tools .tooling/hr048/android-sdk/cmdline-tools/latest
export ANDROID_HOME="$ROOT/.tooling/hr048/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
yes | sdkmanager --licenses >/dev/null || true
sdkmanager 'platforms;android-36' 'build-tools;36.0.0' 'platform-tools'

curl -fsSL 'https://services.gradle.org/distributions/gradle-9.5.0-bin.zip' -o .tooling/hr048/gradle.zip
unzip -q .tooling/hr048/gradle.zip -d .tooling/hr048/gradle
GRADLE="$ROOT/.tooling/hr048/gradle/gradle-9.5.0/bin/gradle"
"$GRADLE" --no-daemon :app:testDebugUnitTest 2>&1 | tee public/hr048/ANDROID-TESTS.txt
"$GRADLE" --no-daemon :app:assembleRelease 2>&1 | tee public/hr048/ANDROID-BUILD.txt

APK=app/build/outputs/apk/release/app-release.apk
test -f "$APK"
cp "$APK" public/hr048/HazRob-0.10.1-HR048-TEST.apk
sha256sum "$APK" > public/hr048/HazRob-0.10.1-HR048-TEST.sha256
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose --print-certs "$APK" > public/hr048/HazRob-0.10.1-HR048-signing.txt
ACTUAL_SIGNER=$("$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --print-certs "$APK" | awk -F': ' '/SHA-256 digest/ {print $2; exit}' | tr -d ': ' | tr 'A-F' 'a-f')
test "$ACTUAL_SIGNER" = 'd6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0'

# ESP32 controlled build environment.
curl -fsSL https://raw.githubusercontent.com/arduino/arduino-cli/master/install.sh | BINDIR="$ROOT/.tooling/hr048/bin" sh
export PATH="$ROOT/.tooling/hr048/bin:$PATH"
rm -f "$HOME/.arduino15/arduino-cli.yaml"
arduino-cli config init
arduino-cli config add board_manager.additional_urls https://espressif.github.io/arduino-esp32/package_esp32_index.json
arduino-cli core update-index
arduino-cli core install esp32:esp32@3.3.11
arduino-cli version | tee public/hr048/ARDUINO-CLI.txt
arduino-cli core list | grep 'esp32:esp32' | tee public/hr048/ESP32-CORE.txt

arduino-cli compile --fqbn esp32:esp32:esp32:PartitionScheme=huge_app --warnings all --output-dir build/hr048/master firmware/HazRob 2>&1 | tee public/hr048/ESP32-COMPILE.txt
arduino-cli compile --fqbn esp32:esp32:esp32cam --warnings all --output-dir build/hr048/camera firmware/HazRob-CAM 2>&1 | tee public/hr048/ESP32-CAM-COMPILE.txt

cp build/hr048/master/*.bin public/hr048/
cp build/hr048/camera/*.bin public/hr048/

# Source packages exclude local secret headers but retain examples/configuration contracts.
rm -f firmware/HazRob/RemoteCloudSecrets.h firmware/HazRob-CAM/RemoteVideoSecrets.h
(
  cd firmware
  zip -qr ../public/hr048/HazRob-ESP32-v0.10.1-HR048-source.zip HazRob
  zip -qr ../public/hr048/HazRob-ESP32-CAM-v0.10.1-HR048-source.zip HazRob-CAM
)

SOURCE_SHA="${RENDER_GIT_COMMIT:-$(git rev-parse HEAD 2>/dev/null || echo unknown)}"
printf 'HazRob 0.10.1 HR048 FULL VALIDATION\nsource=%s\nhr048_drive_audit=PASS\nhr048_integration_audit=PASS\nmotion_audit=PASS\ngas_safety_regression=PASS\nandroid_tests=PASS\nandroid_release_build=PASS\nandroid_signer=PASS\nesp32_master_compile=PASS\nesp32_cam_compile=PASS\nphysical_field_acceptance=PENDING\nflammability_offline_bundle=PENDING\n' "$SOURCE_SHA" > public/hr048/build-info.txt
find public/hr048 -type f ! -name SHA256SUMS.txt -print0 | sort -z | xargs -0 sha256sum > public/hr048/SHA256SUMS.txt

cat > public/hr048/index.html <<'EOF'
<!doctype html><meta charset="utf-8"><meta name="robots" content="noindex,nofollow"><title>HazRob 0.10.1 HR048 validation</title>
<h1>HazRob 0.10.1 HR048 validation</h1>
<p>Software/build validation artifacts. Physical field acceptance remains pending.</p>
<ul>
<li><a href="HazRob-0.10.1-HR048-TEST.apk">Android APK</a></li>
<li><a href="HazRob-ESP32-v0.10.1-HR048-source.zip">Main ESP32 source ZIP</a></li>
<li><a href="HazRob-ESP32-CAM-v0.10.1-HR048-source.zip">ESP32-CAM source ZIP</a></li>
<li><a href="HR048-DRIVE-AUDIT.txt">HR048 drive audit</a></li>
<li><a href="HR048-INTEGRATION-AUDIT.txt">HR048 integration audit</a></li>
<li><a href="ANDROID-TESTS.txt">Android tests</a></li>
<li><a href="ESP32-COMPILE.txt">Main ESP32 compile log</a></li>
<li><a href="ESP32-CAM-COMPILE.txt">ESP32-CAM compile log</a></li>
<li><a href="SHA256SUMS.txt">SHA-256 manifest</a></li>
<li><a href="build-info.txt">Build info</a></li>
</ul>
EOF
