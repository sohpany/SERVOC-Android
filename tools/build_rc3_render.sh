#!/usr/bin/env bash
# Independent RC3 Android build on Render, bypasses GitHub Actions runner starvation.
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive
umask 022
mkdir -p .tooling public
# Never serve a stale APK from an earlier deploy on a failed or partial build.
rm -f public/HazRob-0.10.2-HR069-R1-RC3*.apk public/HazRob-0.10.2-HR069-R1-RC3*.sha256 public/RC3-BUILD-MANIFEST.txt
printf '%s\n' "RC3_BUILD_STARTED source=${RENDER_GIT_COMMIT:-unknown}"
if ! command -v java >/dev/null || ! java -version 2>&1 | grep -q '17\\.'; then
  if [ ! -x .tooling/jdk/bin/java ]; then
    curl --fail --location --retry 3 --retry-delay 3 \
      'https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse' -o .tooling/jdk.tar.gz
    mkdir -p .tooling/jdk
    tar -xzf .tooling/jdk.tar.gz -C .tooling/jdk --strip-components=1
    rm -f .tooling/jdk.tar.gz
  fi
  export JAVA_HOME="$PWD/.tooling/jdk"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
java -version
if [ ! -x .tooling/android-sdk/cmdline-tools/latest/bin/sdkmanager ]; then
  mkdir -p .tooling/android-sdk/cmdline-tools
  curl --fail --location --retry 3 --retry-delay 3 \
    'https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip' \
    -o .tooling/android.zip
  unzip -q .tooling/android.zip -d .tooling/android-sdk/cmdline-tools
  mv .tooling/android-sdk/cmdline-tools/cmdline-tools .tooling/android-sdk/cmdline-tools/latest
  rm -f .tooling/android.zip
fi
export ANDROID_HOME="$PWD/.tooling/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
yes | sdkmanager --licenses >/dev/null || true
sdkmanager 'platforms;android-36' 'build-tools;36.0.0' 'platform-tools'
if [ ! -x .tooling/gradle/gradle-9.5.0/bin/gradle ]; then
  curl --fail --location --retry 3 --retry-delay 3 \
    'https://services.gradle.org/distributions/gradle-9.5.0-bin.zip' -o .tooling/gradle.zip
  mkdir -p .tooling/gradle
  unzip -q .tooling/gradle.zip -d .tooling/gradle
  rm -f .tooling/gradle.zip
fi
GRADLE="$PWD/.tooling/gradle/gradle-9.5.0/bin/gradle"
echo '===== RC3 source audits ====='
python3 tools/audit_rc3_release_candidate.py
python3 tools/audit_0102_rc2_runtime_contract.py
python3 tools/audit_0102_release_contract.py
echo '===== Signing identity & ChemCore Stable ====='
test -f signing/hazrob-prototype.jks.b64
base64 --decode signing/hazrob-prototype.jks.b64 > signing/hazrob-prototype.jks
chmod 600 signing/hazrob-prototype.jks
trap 'rm -f signing/hazrob-prototype.jks' EXIT
python3 tools/build_hr069_embedded_stable.py --out-dir app/build/generated/hazrobAssets/generated/chemcore/embedded_stable
python3 tools/verify_rc3_embedded_stable.py --asset-dir app/build/generated/hazrobAssets/generated/chemcore/embedded_stable
echo '===== Android unit tests ====='
"$GRADLE" --no-daemon --stacktrace :app:testDebugUnitTest
echo '===== APK signing/build ====='
"$GRADLE" --no-daemon --stacktrace :app:assembleRelease
APK=app/build/outputs/apk/release/app-release.apk
test -s "$APK"
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose --print-certs "$APK"
python3 tools/verify_rc3_embedded_stable.py --asset-dir app/build/generated/hazrobAssets/generated/chemcore/embedded_stable --apk "$APK"
CERT=$("$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --print-certs "$APK" | awk -F': ' '/SHA-256 digest/ {print $2;exit}' | tr -d ': ' | tr 'A-F' 'a-f')
# Pin the certificate observed in the currently controlled RC2 build: preserve in-place user data.
test "$CERT" = 'd6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0'
BADGING=$("$ANDROID_HOME/build-tools/36.0.0/aapt" dump badging "$APK" | head -n 1)
echo "$BADGING" | grep -q "package: name='com.servoc.app'"
echo "$BADGING" | grep -q "versionCode='31'"
echo "$BADGING" | grep -q "versionName='0.10.2'"
APK_NAME="HazRob-0.10.2-HR069-R1-RC3-WORKING-${RENDER_GIT_COMMIT:-unknown}.apk"
cp "$APK" "public/$APK_NAME"
sha256sum "public/$APK_NAME" > "public/$APK_NAME.sha256"
{
 echo "status=BUILT_TESTED_WORKING_NOT_FIELD_APPROVED"
 echo "source_repo=sohpany/SERVOC-Android"
 echo "source_branch=release/hazrob-0.10.2-rc3"
 echo "source_sha=${RENDER_GIT_COMMIT:-unknown}"
 echo "apk_filename=$APK_NAME"
 echo "apk_sha256=$(sha256sum "public/$APK_NAME" | awk '{print $1}')"
 echo "signer_sha256=$CERT"
 echo "apk_badging=$BADGING"
 echo "embedded_stable=1.0.0-hr069r1"
 echo "stable_screening_priors=49"
 echo "m4_calibrations=0"
 echo "android_testDebugUnitTest=PASS"
 echo "apk_signature=PASS"
 echo "stable_bundle_exact_match=PASS"
 echo "on_device_acceptance=PENDING"
} > public/RC3-BUILD-MANIFEST.txt
cat > public/index.html <<'EOF'
<!doctype html><html lang="en"><meta charset="utf-8"><meta name="robots" content="noindex,nofollow">
<title>HazRob wheeled RC3 working build</title>
<h1>HazRob wheeled 0.10.2 HR069-R1 RC3</h1><p>WORKING / NOT FIELD-APPROVED. This is NOT HazRob Tank.</p>
<p>The APK and manifest are available below after build verification.</p>
<ul><li><a href="RC3-BUILD-MANIFEST.txt">Build manifest</a></li>
<li><a href="RC3-APK-SHA256.txt">APK SHA256</a></li></ul></html>
EOF
cp "public/$APK_NAME.sha256" public/RC3-APK-SHA256.txt
echo "RC3_SIGNED_APK_VERIFIED $APK_NAME"
