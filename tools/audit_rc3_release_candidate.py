#!/usr/bin/env python3
"""RC3 source protection/traceability guard. Not a replacement for Gradle or device tests."""
from pathlib import Path
p=lambda x:Path(x).read_text(encoding="utf-8")
app=p("app/build.gradle")
release=p("app/src/main/java/com/servoc/app/ReleaseAudit.java")
ui=p("app/src/main/java/com/servoc/app/ui/FieldSessionsTab.java")
reports=p("app/src/main/java/com/servoc/app/ui/ReportsTab.java")
repository=p("app/src/main/java/com/servoc/app/field/FieldSessionRepository.java")
schema=p("app/src/main/java/com/servoc/app/data/ExperimentsDb.java")
kit=p("app/src/main/java/com/servoc/app/ui/UiKit.java")
measure=p("app/src/main/java/com/servoc/app/ui/MeasurementsTab.java")
settings=p("app/src/main/java/com/servoc/app/ui/SettingsTab.java")
echo=p("app/src/main/java/com/servoc/app/measurement/EchoReading.java")
mic=p("app/src/main/java/com/servoc/app/ui/PhysicalMeasurementsUiEnhancer.java")
r0=p("app/src/main/java/com/servoc/app/calibration/AutoR0Capture.java")
track=p("app/src/main/java/com/servoc/app/ui/ControlTab.java")
assert "applicationId 'com.servoc.app'" in app
assert "versionCode 31" in app and "versionName '0.10.2'" in app
assert 'RELEASE_CANDIDATE="RC3"' in release
assert "if(refreshing)return;" in ui and "List<FieldSessionRepository.Site> all=displayedSites" in ui
assert 'UiKit.button(c,"الإدارة")' in ui and 'UiKit.button(c,"التقارير")' in ui
assert 'approved?"معتمدة":"جديدة"' in ui
assert "approvedSessionsForSite" in reports and "approvedSessionsForSite" in ui
assert "CREATE TABLE IF NOT EXISTS field_session_approvals" in schema
assert "restartUnapproved(" in repository and "isApproved(" in repository
assert "readableCheckBox(" in kit and "UiKit.readableCheckBox(b)" in settings
assert "UiKit.readableOptions(c,java.util.Arrays.asList(labels))" in measure
assert "REFRESH_MS=1000L" in mic
assert "CM_PER_MICROSECOND=0.0343/2.0" in echo and "PREF_OFFSET_CM" in echo
assert "if(landscape)actions.addView(relayButton" in track
assert "TARGET=30" in r0 and "next=now+1000" in r0
assert Path("app/src/main/res/drawable/ic_hazrob_hr069_r1.png").exists()
assert "ChemCoreEmbeddedStableInstaller.ensureInstalled(context)" in p("app/src/main/java/com/servoc/app/chemical/ChemicalSeedBootstrap.java")
assert "prepareHr069EmbeddedStable" in app and "assets.srcDir generatedHazrobAssets" in app
print("RC3 source boundary and requested UI/measurement regressions: PASS (STATIC ONLY)")
