#!/usr/bin/env python3
"""Current-source UI/drive/analytics/report contract for HazRob 0.10.2 / HR069 only."""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

def read(path):
    p=ROOT/path
    if not p.is_file(): raise AssertionError(f"missing current HR069 file: {path}")
    return p.read_text(encoding="utf-8")

def req(src,needle,label):
    if needle not in src: raise AssertionError(f"{label}: missing {needle!r}")

def main():
    control=read("app/src/main/java/com/servoc/app/ui/ControlTab.java")
    mode=read("app/src/main/java/com/servoc/app/control/DriveMode.java")
    mixer=read("app/src/main/java/com/servoc/app/control/DriveMixer.java")
    tank=read("app/src/main/java/com/servoc/app/control/DualTankDriveControl.java")
    joy=read("app/src/main/java/com/servoc/app/control/JoystickView.java")
    discrete=read("app/src/main/java/com/servoc/app/control/DiscreteDrive.java")
    measurements=read("app/src/main/java/com/servoc/app/ui/MeasurementsTab.java")
    reports=read("app/src/main/java/com/servoc/app/ui/ReportsTab.java")
    intelligence=read("app/src/main/java/com/servoc/app/report/ReportIntelligence.java")
    uikit=read("app/src/main/java/com/servoc/app/ui/UiKit.java")
    hardware=read("firmware/HazRob/Hardware.cpp")
    protocol=read("app/src/main/java/com/servoc/app/communication/RobotProtocolV4.java")

    # UI-01: full-screen driving and overlays.
    req(control,"Configuration.ORIENTATION_LANDSCAPE","landscape detection")
    req(control,"applyLandscapeFullscreenChrome","landscape immersive driving")
    req(control,"cameraBox.addView(video,new FrameLayout.LayoutParams(-1,-1))","full video surface")
    for marker,label in [
        ("gasBadge","live gas overlay"),("relayButton","MQ relay overlay"),("wifiBadge","Wi-Fi overlay"),
        ("distanceBadge","Echo distance overlay"),("irBadge","IR ground/cliff overlay"),("flameBadge","flame overlay")]:
        req(control,marker,label)
    req(control,'distanceBadge.setText(String.format(Locale.US,"أمام %.1f cm",cm))',"Echo centimetre display")
    req(control,'cliff?"⚠ لا توجد أرضية":ground?"أرض ●":"أرض ؟"',"IR no-ground warning semantics")
    req(control,'mqOn?"MQ ON":"MQ OFF"',"relay state rendering")
    req(control,"boolean target=!on;","relay toggle target")
    req(control,"host.robotApi().mqPower(target)","floating relay control")
    req(control,"WifiSignalReader.read(appContext)","Wi-Fi RSSI read")

    # UI-02: current independent drive modes. Tank sides map directly to L/R; speed rail is hidden in tank mode.
    for marker in ['TANK("tank"','JOYSTICK("joystick"','WHEEL("wheel"','BUTTONS("buttons"']:
        req(mode,marker,"current drive selector")
    req(control,"driveMode.next()","drive-mode cycling")
    req(control,"driveMode==DriveMode.TANK","tank mode retained")
    req(control,"driveMode==DriveMode.JOYSTICK","joystick retained")
    req(control,"driveMode==DriveMode.WHEEL","wheel mode retained")
    req(control,"driveMode==DriveMode.BUTTONS","button mode retained")
    req(tank,"onCommand(int value)","independent signed tank side")
    req(control,"tankLeft.setListener(v->{tankLeftRaw=v;if(driveMode==DriveMode.TANK)applyTankDrive();})","left tank side binding")
    req(control,"tankRight.setListener(v->{tankRightRaw=v;if(driveMode==DriveMode.TANK)applyTankDrive();})","right tank side binding")
    req(control,"host.setDriveCommand(tankLeftRaw,tankRightRaw)","tank direct left/right command ordering")
    req(control,"driveMode!=DriveMode.TANK&&driveMode!=DriveMode.WHEEL","tank/wheel speed rail isolation")
    req(joy,"DriveMixer.mix(x, y)","joystick canonical mixer")
    req(discrete,"PIVOT_RIGHT:return new DriveMixer.Command(50,-50)","right in-place pivot")
    req(discrete,"PIVOT_LEFT:return new DriveMixer.Command(-50,50)","left in-place pivot")
    req(discrete,"FORWARD_RIGHT:return new DriveMixer.Command(100,40)","progressive forward-right")
    req(discrete,"FORWARD_LEFT:return new DriveMixer.Command(40,100)","progressive forward-left")
    req(mixer,"MIN_INNER_RATIO = 0.40","minimum inside-wheel ratio")
    req(mixer,"PIVOT_MAX = 0.50","pivot command cap")
    req(mixer,"if(x>0)return new Command(pct(outside),pct(inside))","right curvature semantics")
    req(mixer,"if(x<0)return new Command(pct(inside),pct(outside))","left curvature semantics")

    # UI-03: canonical Echo/IR semantics used by current operator UI.
    req(hardware,'\\"cm\\":"+num(ultraCm,1)',"raw Echo centimetre telemetry")
    req(protocol,'proximity.put("distance_cm",finiteAny(p,"cm","distance_cm"))',"canonical Echo cm normalization")
    req(hardware,'\\"ground_detected\\"',"IR ground telemetry")
    req(hardware,'\\"cliff_warning\\"',"IR cliff telemetry")
    req(protocol,'x.put("installed",true);x.put("warning",warning);x.put("cliff_warning",cliff);x.put("ground_detected",i.optBoolean("ground_detected",!cliff))',"canonical IR semantics")

    # UI-04: operational measurement workspace.
    req(measurements,'String[] names={"الفيزيائية","القياس الحي","التحليل","النتائج"}',"current measurement workspace tabs")
    for marker,label in [
        ('powerCard=kpi("MQ\\n—")',"MQ power KPI"),('arrayCard=kpi("المصفوفة\\n—")',"array KPI"),
        ('gasCard=kpi("الاستجابة\\n—")',"gas response KPI"),('sessionCard=kpi("الجلسة\\n—")',"session KPI"),
        ("ProgressBar","session progress"),("togglePower()","operator relay action"),("startMeasurement()","measurement start path")]:
        req(measurements,marker,label)
    req(measurements,"LiveGasStateEngine.Snapshot gas=host.liveGasState()","live gas measurement state")
    req(measurements,"data.setTextColor(0xFFCFD8DC)","sensor-detail contrast")

    # UI-05: production ChemCore analysis/results dashboard.
    req(measurements,'UiKit.title(c,"ChemCore • لوحة التحليل")',"analysis dashboard title")
    req(measurements,'quantitative=kpi("PPM الكمي\\n—")',"quantitative gate KPI")
    req(measurements,"ChemCoreLabQuality.evaluate","analysis admissibility gate")
    req(measurements,"top_posterior","top confidence KPI")
    req(measurements,"posterior_separation_margin","candidate separation KPI")
    req(measurements,"ood_score","OOD KPI")
    req(measurements,"normalized_entropy","entropy KPI")
    req(measurements,"ppm_reportable","quantitative reportability gate")
    req(measurements,"إظهار فحص 144","on-demand 144 screening")
    req(measurements,"foregroundFor","candidate contrast selection")
    req(measurements,"ChemCoreAnalysisPresenter.equationsAndResiduals","expandable technical residual details")

    # UI-06: smart reports and chart evidence gating.
    req(reports,'UiKit.title(c,"التقارير الذكية • HR069")',"smart report workspace")
    req(reports,"لا توجد نقاط مصطنعة","explicit chart evidence disclosure")
    req(reports,"ChemCoreAnalysisPresenter.summary","report technical provenance")
    req(reports,"AnalyticalCharts.screening144","144-screening chart")
    req(reports,"AnalyticalCharts.exposureRatios","exposure/flammability chart")
    req(intelligence,"enum State{EMPTY,SIGNAL_ONLY,ANALYTICAL_ONLY,SCREENING,IDENTIFIED,QUANTIFIED}","report readiness states")
    req(intelligence,"recorded_time_series_missing","series fail-closed chart gate")
    req(intelligence,"chemcore_result_missing","ChemCore fail-closed chart gate")
    req(intelligence,"screening_144_missing","144-screening evidence gate")
    req(intelligence,"gas_safety_gate_not_passed","gas safety evidence gate")
    req(intelligence,"recommendedCharts","smart chart recommendation list")

    # UI-07: explicit HazRob dark-theme contrast rules and runtime readability fail-safe.
    req(uikit,"HAZ_BLACK=0xFF0A0D11","HazRob black background")
    req(uikit,"HAZ_GRAY_DARK=0xFF263238","secondary-action background")
    req(uikit,"HAZ_WHITE=0xFFF5F7F8","high-contrast foreground")
    req(uikit,"HAZ_YELLOW=0xFFFFC107","primary HazRob accent")
    req(uikit,"LIGHT_BG=HAZ_GRAY_DARK,LIGHT_TEXT=HAZ_WHITE","compatibility palette aliases")
    req(uikit,"MIN_TEXT_CONTRAST=4.5","minimum readable contrast")
    req(uikit,"foregroundFor(int background)","luminance-aware foreground")
    req(uikit,"contrast(TextView t,int background)","shared contrast application")
    req(uikit,"effectiveBackground(View v)","actual-parent background lookup")
    req(uikit,"contrastRatio(current,bg)<MIN_TEXT_CONTRAST","runtime low-contrast correction")
    req(uikit,"setHintTextColor(0xFF90A4AE)","dark-input hint contrast")

    print("HazRob 0.10.2 / HR069 operator UI contract: PASS")
    print("UI-01..UI-07: drive contract, overlays, measurement/analysis dashboards, smart reports, explicit 4.5:1 contrast guard")
    return 0

if __name__=="__main__":
    try: raise SystemExit(main())
    except AssertionError as e:
        print("HazRob 0.10.2 / HR069 operator UI contract: FAIL:",e)
        raise SystemExit(1)
