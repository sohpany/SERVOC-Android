FROM node:20-bookworm-slim AS relaytest
WORKDIR /relay
COPY remote/video-relay/package.json ./
RUN npm install --no-audit --no-fund
COPY remote/video-relay/ ./
# This test exercises the local HTTP/bootstrap/WSS relay contract and then the live
# Render bootstrap + HiveMQ diagnostic exposed by production /health.
RUN npm test && touch /relay-test-pass

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

# Restore the controlled prototype signing key only inside this isolated build container.
RUN base64 -d signing/hazrob-prototype.jks.b64 > signing/hazrob-prototype.jks && chmod 600 signing/hazrob-prototype.jks

# Fail closed on the same core integration gates used by the controlled release workflow.
RUN python3 tools/audit_hr061_direct_first_contract.py && \
    python3 tools/audit_0101_integration_contract.py && \
    python3 tools/audit_0101_motion_contract.py && \
    python3 tools/audit_hr047_gas_safety_contract.py

# HR066 comprehensive regression sweep: execute every retained audit, including older
# subsystem contracts, so stale assumptions or cross-component drift fail the build.
RUN set -eux; \
    for f in tools/audit_*.py; do \
      echo "===== $f ====="; \
      python3 "$f"; \
    done

RUN python3 tools/audit_0101_firmware_baseline.py && \
    mkdir -p /tmp/fw-main /tmp/fw-cam && \
    arduino-cli compile --fqbn esp32:esp32:esp32:PartitionScheme=huge_app --warnings all --output-dir /tmp/fw-main firmware/HazRob && \
    arduino-cli compile --fqbn esp32:esp32:esp32cam --warnings all --output-dir /tmp/fw-cam firmware/HazRob-CAM

# Package the exact validated firmware sources together with the binaries produced by the pinned
# esp32:esp32@3.3.11 toolchain so field flashing uses the same bytes/contracts as this APK build.
RUN mkdir -p /out && python3 - <<'PY'
from pathlib import Path
import zipfile

def pack(name, sketch_name, source_dir, compiled_dir):
    # Arduino expects the sketch itself to be one folder whose primary .ino name
    # matches that folder. Keep all related source tabs and validated binaries
    # together under that single root so extraction is immediately usable.
    out=Path('/out')/name
    root=Path(sketch_name)
    with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
        for p in sorted(Path(source_dir).rglob('*')):
            if p.is_file():
                z.write(p, root/p.relative_to(source_dir))
        for p in sorted(Path(compiled_dir).rglob('*')):
            if p.is_file():
                z.write(p, root/'build'/p.relative_to(compiled_dir))

pack('HazRob-ESP32-Main-0.10.2-HR067.zip','HazRob','firmware/HazRob','/tmp/fw-main')
pack('HazRob-ESP32-CAM-0.10.2-HR067.zip','HazRob-CAM','firmware/HazRob-CAM','/tmp/fw-cam')
PY
RUN sha256sum /out/HazRob-ESP32-Main-0.10.2-HR067.zip > /out/HazRob-ESP32-Main-0.10.2-HR067.sha256 && \
    sha256sum /out/HazRob-ESP32-CAM-0.10.2-HR067.zip > /out/HazRob-ESP32-CAM-0.10.2-HR067.sha256

RUN grep -q "versionCode 26" app/build.gradle && grep -q "versionName '0.10.2'" app/build.gradle
RUN gradle --no-daemon :app:testDebugUnitTest
RUN gradle --no-daemon :app:lintDebug
RUN gradle --no-daemon :app:assembleRelease

# Verify the APK is signed by the already-controlled HazRob prototype certificate.
RUN set -eux; \
    APK=app/build/outputs/apk/release/app-release.apk; \
    EXPECTED=d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0; \
    ACTUAL=$(apksigner verify --print-certs "$APK" | awk -F': ' '/SHA-256 digest/ {print $2; exit}' | tr -d ': ' | tr 'A-F' 'a-f'); \
    test "$ACTUAL" = "$EXPECTED"; \
    mkdir -p /out; \
    cp "$APK" /out/HazRob-0.10.2-v26-HR067.apk; \
    sha256sum /out/HazRob-0.10.2-v26-HR067.apk > /out/HazRob-0.10.2-v26-HR067.sha256; \
    apksigner verify --verbose --print-certs "$APK" > /out/HazRob-0.10.1-v25-signing.txt

RUN cat > /out/index.html <<'EOF'
<!doctype html>
<html lang="ar" dir="rtl">
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>HazRob 0.10.2 HR067</title></head>
<body style="font-family:sans-serif;max-width:760px;margin:40px auto;padding:0 18px">
<h1>HazRob 0.10.2 — HR067</h1>
<p>حزمة HR067 الموحدة: تطبيق HazRob 0.10.2 مع منظومة Analytical v4، وفريموير Main/CAM مطابق للمصدر المجمد HR066 دون تعديل.</p>
<p><a href="/HazRob-0.10.2-v26-HR067.apk">تحميل APK</a></p>
<p><a href="/HazRob-0.10.2-v26-HR067.sha256">APK SHA-256</a> · <a href="/HazRob-0.10.1-v25-signing.txt">Signing verification</a></p>
<p><a href="/HazRob-ESP32-Main-0.10.2-HR067.zip">Firmware Main ESP32</a> · <a href="/HazRob-ESP32-Main-0.10.2-HR067.sha256">SHA-256</a></p>
<p><a href="/HazRob-ESP32-CAM-0.10.2-HR067.zip">Firmware ESP32-CAM</a> · <a href="/HazRob-ESP32-CAM-0.10.2-HR067.sha256">SHA-256</a></p>
<p>Validation: complete audit sweep + relay local/live Render/HiveMQ contract + Android unit tests + Android lint + Main/CAM compile + signed release build.</p>
</body></html>
EOF

FROM nginx:1.27-alpine
COPY --from=build /out/ /usr/share/nginx/html/
EXPOSE 80
