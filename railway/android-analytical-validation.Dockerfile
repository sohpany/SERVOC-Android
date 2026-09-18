FROM eclipse-temurin:17-jdk-jammy AS build
ARG DEBIAN_FRONTEND=noninteractive
ARG ANDROID_CMDLINE_TOOLS=11076708
ENV ANDROID_SDK_ROOT=/opt/android-sdk ANDROID_HOME=/opt/android-sdk GRADLE_HOME=/opt/gradle-9.5.0 PATH=/opt/gradle-9.5.0/bin:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/build-tools/36.0.0:$PATH
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl unzip git python3 && rm -rf /var/lib/apt/lists/*
RUN mkdir -p /opt/android-sdk/cmdline-tools && curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_CMDLINE_TOOLS}_latest.zip" -o /tmp/cmdline-tools.zip && unzip -q /tmp/cmdline-tools.zip -d /opt/android-sdk/cmdline-tools && mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest && rm /tmp/cmdline-tools.zip && yes | sdkmanager --licenses >/dev/null || true
RUN sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
RUN curl -fsSL "https://services.gradle.org/distributions/gradle-9.5.0-bin.zip" -o /tmp/gradle.zip && unzip -q /tmp/gradle.zip -d /opt && rm /tmp/gradle.zip
WORKDIR /src
COPY . .
RUN gradle --no-daemon :app:testDebugUnitTest --tests 'com.servoc.app.chemcore.ChemCoreFeaturePipelineTest' --tests 'com.servoc.app.analytics.TemporalFeatureExtractorCanonicalAdapterTest' --tests 'com.servoc.app.measurement.SignalFeaturesTest' --tests 'com.servoc.app.measurement.LiveGasStateEngineTest' --tests 'com.servoc.app.measurement.GasAlertCoordinatorTest'
RUN gradle --no-daemon :app:lintDebug
RUN gradle --no-daemon :app:assembleDebug
RUN mkdir -p /out && cp app/build/outputs/apk/debug/app-debug.apk /out/HazRob-Analytical-V4-Stage3-Validation.apk
RUN printf '%s\n' '<!doctype html><html><body><h1>HazRob Analytical V4 Stage 3 Validation</h1><p>Android/ChemCore-only validation. No firmware build or modification.</p></body></html>' > /out/index.html
FROM nginx:1.27-alpine
COPY --from=build /out/ /usr/share/nginx/html/
EXPOSE 80
