FROM node:20-bookworm-slim AS relaytest
WORKDIR /relay
COPY remote/video-relay/package.json ./
RUN npm install --no-audit --no-fund
COPY remote/video-relay/ ./
RUN HAZROB_SKIP_LIVE_RELAY_PROBE=1 npm test && touch /relay-test-pass

FROM eclipse-temurin:17-jdk-jammy AS build

ARG DEBIAN_FRONTEND=noninteractive
ARG ANDROID_CMDLINE_TOOLS=11076708
ENV ANDROID_SDK_ROOT=/opt/android-sdk \
    ANDROID_HOME=/opt/android-sdk \
    GRADLE_HOME=/opt/gradle-9.5.0 \
    PATH=/opt/gradle-9.5.0/bin:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/build-tools/36.0.0:$PATH

RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl unzip python3 git file openssl && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p /opt/android-sdk/cmdline-tools && \
    curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_CMDLINE_TOOLS}_latest.zip" -o /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d /opt/android-sdk/cmdline-tools && \
    mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip && \
    yes | sdkmanager --licenses >/dev/null || true

RUN sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"

RUN curl -fsSL "https://services.gradle.org/distributions/gradle-9.5.0-bin.zip" -o /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    rm /tmp/gradle.zip

RUN curl -fsSL "https://raw.githubusercontent.com/arduino/arduino-cli/master/install.sh" | BINDIR=/usr/local/bin sh
RUN arduino-cli config init && \
    arduino-cli config add board_manager.additional_urls https://espressif.github.io/arduino-esp32/package_esp32_index.json && \
    arduino-cli core update-index && \
    arduino-cli core install esp32:esp32@3.3.11

WORKDIR /src
COPY . .
COPY --from=relaytest /relay-test-pass /tmp/hazrob-relay-test-pass

# Restore the controlled prototype signing key only inside this isolated validation container.
RUN base64 -d signing/hazrob-prototype.jks.b64 > signing/hazrob-prototype.jks && chmod 600 signing/hazrob-prototype.jks

# Source contracts: no historical HR gate may override the active 0.10.2 / HR069 policy.
RUN python3 tools/audit_0101_integration_contract.py && \
    python3 tools/audit_0101_motion_contract.py && \
    python3 tools/audit_0102_release_contract.py && \
    python3 tools/audit_0103_measurement_foundation.py

# Compile both firmware targets internally. Outputs remain ephemeral in this build stage.
RUN python3 tools/audit_0101_firmware_baseline.py && \
    mkdir -p /tmp/fw-main /tmp/fw-cam && \
    arduino-cli compile --fqbn esp32:esp32:esp32:PartitionScheme=huge_app --warnings all --output-dir /tmp/fw-main firmware/HazRob && \
    arduino-cli compile --fqbn esp32:esp32:esp32cam --warnings all --output-dir /tmp/fw-cam firmware/HazRob-CAM

RUN grep -q "versionCode 26" app/build.gradle && grep -q "versionName '0.10.2'" app/build.gradle
RUN gradle --no-daemon :app:testDebugUnitTest
RUN gradle --no-daemon :app:lintDebug
RUN gradle --no-daemon :app:assembleRelease

# Validate signing/integrity without exporting a downloadable APK.
RUN set -eux; \
    APK=app/build/outputs/apk/release/app-release.apk; \
    EXPECTED=d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0; \
    test -f "$APK"; \
    ACTUAL=$(apksigner verify --print-certs "$APK" | awk -F': ' '/SHA-256 digest/ {print $2; exit}' | tr -d ': ' | tr 'A-F' 'a-f'); \
    test "$ACTUAL" = "$EXPECTED"; \
    apksigner verify --verbose "$APK"; \
    sha256sum "$APK" > /tmp/internal-apk.sha256; \
    mkdir -p /validation; \
    cp /tmp/internal-apk.sha256 /validation/APK-INTERNAL-SHA256.txt; \
    printf '%s\n' \
      'HazRob 0.10.2 / HR069 internal validation build: PASS' \
      'APK was built and verified inside the build stage only.' \
      'No APK or firmware ZIP is exported by this Railway service before P13.' \
      > /validation/VALIDATION.txt

RUN cat > /validation/index.html <<'EOF'
<!doctype html>
<html lang="ar" dir="rtl">
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>HazRob 0.10.2 HR069 Validation</title></head>
<body style="font-family:sans-serif;max-width:760px;margin:40px auto;padding:0 18px">
<h1>HazRob 0.10.2 — HR069</h1>
<p>خدمة تحقق داخلي فقط. يتم بناء التطبيق وتجميع Main/CAM وتشغيل الاختبارات والتحقق من التوقيع داخل مرحلة البناء.</p>
<p><strong>لا يتم نشر أو توفير ملف APK أو حزمة Firmware للتنزيل قبل مرحلة P13.</strong></p>
</body></html>
EOF

FROM nginx:1.27-alpine
COPY --from=build /validation/ /usr/share/nginx/html/
EXPOSE 80
