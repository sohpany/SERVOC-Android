#!/usr/bin/env python3
"""Canonical HazRob 0.10.2 release contract. Historical HR audits are provenance only."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def txt(p): return (ROOT/p).read_text(encoding="utf-8")
def req(s,n,l):
    if n not in s: raise AssertionError(f"{l}: missing {n!r}")
def forbid(s,n,l):
    if n in s: raise AssertionError(f"{l}: forbidden {n!r}")
def main():
    main=txt("app/src/main/java/com/servoc/app/MainActivity.java")
    entry=txt("app/src/main/java/com/servoc/app/EntryActivity.java")
    hw=txt("firmware/HazRob/Hardware.cpp")
    cfg=txt("firmware/HazRob/Config.h")
    lib=txt("app/src/main/java/com/servoc/app/field/HazRobUserLibrary.java")
    arc=txt("app/src/main/java/com/servoc/app/field/FieldSessionArchive.java")
    req(main,'String[] names={"القيادة","الجلسات","القياسات","التقارير","الحالة","الإعدادات","المطور"}',"0.10.2 navigation")
    req(entry,"showFirstRunLibrarySetup()","first-run durable library")
    req(entry,"HazRobUserLibrary.createPickerIntent()","SAF library picker")
    req(lib,"FieldSessionArchive.exportSession","persistent source archive")
    req(lib,"FieldSessionArchive.importSession","persistent source restore")
    req(arc,"m.operator_role='USER'","user archive isolation")
    req(arc,"m.kind LIKE 'field_%'","field archive isolation")
    forbid(arc,"chemcore_feature_references","scientific reference isolation")
    req(cfg,"ULTRA_WARN_CM=10.0f","collision warning threshold")
    req(cfg,"ULTRA_STOP_CM=5.0f","collision stop threshold")
    req(hw,"forwardBlocked=irWarning||ultraStop","forward safety interlock")
    req(hw,'\"forward_blocked\"',"safety telemetry")
    print("HazRob 0.10.2 canonical release contract: PASS")
    return 0
if __name__=="__main__":
    try: raise SystemExit(main())
    except AssertionError as e:
        print(f"HazRob 0.10.2 canonical release contract: FAIL: {e}")
        raise SystemExit(1)
