FROM eclipse-temurin:17-jdk-jammy AS build

ARG DEBIAN_FRONTEND=noninteractive
ARG ANDROID_CMDLINE_TOOLS=11076708
ENV ANDROID_SDK_ROOT=/opt/android-sdk \
    ANDROID_HOME=/opt/android-sdk \
    GRADLE_HOME=/opt/gradle-9.5.0 \
    PATH=/opt/gradle-9.5.0/bin:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/build-tools/36.0.0:$PATH

RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl unzip python3 git file && \
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

WORKDIR /src
COPY . .

# Restore the controlled prototype signing key only inside this isolated build container.
RUN base64 -d signing/hazrob-prototype.jks.b64 > signing/hazrob-prototype.jks && chmod 600 signing/hazrob-prototype.jks

# Fail closed on the same core integration gates used by the controlled release workflow.
RUN python3 tools/audit_hr061_direct_first_contract.py && \
    python3 tools/audit_0101_integration_contract.py && \
    python3 tools/audit_0101_motion_contract.py && \
    python3 tools/audit_hr047_gas_safety_contract.py

RUN grep -q "versionCode 24" app/build.gradle && grep -q "versionName '0.10.1'" app/build.gradle
RUN gradle --no-daemon :app:testDebugUnitTest
RUN gradle --no-daemon :app:assembleRelease

# Verify the APK is signed by the already-controlled HazRob prototype certificate.
RUN set -eux; \
    APK=app/build/outputs/apk/release/app-release.apk; \
    EXPECTED=d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0; \
    ACTUAL=$(apksigner verify --print-certs "$APK" | awk -F': ' '/SHA-256 digest/ {print $2; exit}' | tr -d ': ' | tr 'A-F' 'a-f'); \
    test "$ACTUAL" = "$EXPECTED"; \
    mkdir -p /out; \
    cp "$APK" /out/HazRob-0.10.1-v24-TEST.apk; \
    sha256sum /out/HazRob-0.10.1-v24-TEST.apk > /out/HazRob-0.10.1-v24-TEST.sha256; \
    apksigner verify --verbose --print-certs "$APK" > /out/HazRob-0.10.1-v24-signing.txt

RUN cat > /out/index.html <<'EOF'
<!doctype html>
<html lang="ar" dir="rtl">
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>HazRob v24 Test Build</title></head>
<body style="font-family:sans-serif;max-width:760px;margin:40px auto;padding:0 18px">
<h1>HazRob 0.10.1 — v24 TEST</h1>
<p>بناء تجريبي: الدخول المباشر يعتمد على ESP32 الرئيسي، وحالة ESP32-CAM مستقلة ولا تمنع استخدام التطبيق.</p>
<p><a href="/HazRob-0.10.1-v24-TEST.apk">تحميل APK</a></p>
<p><a href="/HazRob-0.10.1-v24-TEST.sha256">SHA-256</a> · <a href="/HazRob-0.10.1-v24-signing.txt">Signing verification</a></p>
</body></html>
EOF

FROM nginx:1.27-alpine
COPY --from=build /out/ /usr/share/nginx/html/
EXPOSE 80
