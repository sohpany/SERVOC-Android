#!/usr/bin/env python3
"""Current-source UI/drive/analytics contract for HazRob 0.10.2 / HR069 only."""
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
    wheel=read("app/src/main/java/com/servoc/app/control/SteeringWheelView.java")
    joy=read("app/src/main/java/com/servoc/app/control/JoystickView.java")
    pad=read("app/src/main/java/com/servoc/app/control/DirectionPadView.java")
    discrete=read("app/src/main/java/com/servoc/app/control/DiscreteDrive.java")
    measurements=read("app/src/main/java/com/servoc/app/ui/MeasurementsTab.java")
    reports=read("app/src/main/java/com/servoc/app/ui/ReportsTab.java")
    uikit=read("app/src/main/java/com/servoc/app/ui/UiKit.java")
    hardware=read("firmware/HazRob/Hardware.cpp")

    # Full-screen driving and overlays.
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
    req(control,"host.robotApi().mqPower(!on)","floating relay control")
    req(control,"WifiSignalReader.read(appContext)","Wi-Fi RSSI read")

    # Four independent drive modes.
    for marker in ['TANK("tank"','WHEEL("wheel"','JOYSTICK("joystick"','BUTTONS("buttons"']:
        req(mode,marker,"four-mode drive selector")
    req(control,"driveMode.next()","drive-mode cycling")
    req(control,"driveMode==DriveMode.TANK","tank mode retained")
    req(control,"driveMode==DriveMode.WHEEL","wheel mode retained")
    req(control,"driveMode==DriveMode.JOYSTICK","joystick added independently")
    req(control,"driveMode==DriveMode.BUTTONS","button mode retained")
    req(tank,"onCommand(int value)","independent signed tank side")
    req(wheel,"DriveMixer.mix(x,y)","wheel canonical mixer")
    req(joy,"DriveMixer.mix(x, y)","joystick canonical mixer")
    req(discrete,"PIVOT_RIGHT:return new DriveMixer.Command(100,-100)","right in-place pivot")
    req(discrete,"PIVOT_LEFT:return new DriveMixer.Command(-100,100)","left in-place pivot")
    req(discrete,"FORWARD_RIGHT:return new DriveMixer.Command(100,50)","progressive forward-right")
    req(discrete,"FORWARD_LEFT:return new DriveMixer.Command(50,100)","progressive forward-left")
    req(mixer,"double left=y+steer;","left differential output")
    req(mixer,"double right=y-steer;","right differential output")

    # Firmware telemetry semantics used by current UI.
    req(hardware,'\\"cm\\":'+"num(ultraCm,1)","Echo telemetry")
    req(hardware,'\\"ground_detected\\"',"IR ground telemetry")
    req(hardware,'\\"cliff_warning\\"',"IR cliff telemetry")

    # Current measurement/analysis/report surfaces.
    req(measurements,'String[] names={"الفيزيائية","القياس","التحليل","نتائج التحليل"}',"measurement workspace tabs")
    req(measurements,"LiveGasStateEngine.Snapshot gas=host.liveGasState()","live gas measurement state")
    req(measurements,"ChemCoreLabQuality.evaluate","analysis admissibility gate")
    req(measurements,"ChemCoreAnalysisPresenter","ChemCore presentation binding")
    req(reports,"AnalyticalCharts","analytical charts integration")
    req(reports,"screening144","144-screening report view")
    req(reports,"exposureRatios","exposure/flammability chart")
    req(uikit,"setTextColor(Color.WHITE)","default dark-surface foreground")

    print("HazRob 0.10.2 / HR069 operator UI contract: PASS")
    print("drive modes=TANK,WHEEL,JOYSTICK,BUTTONS; landscape=full-screen video; overlays=gas,relay,wifi,echo,IR,flame")
    return 0

if __name__=="__main__":
    try: raise SystemExit(main())
    except AssertionError as e:
        print("HazRob 0.10.2 / HR069 operator UI contract: FAIL:",e)
        raise SystemExit(1)
