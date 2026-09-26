#!/usr/bin/env python3
"""Fail-closed HR-0101-047 gas-safety/release integration gate.

This supplements audit_0101_integration_contract.py. It deliberately checks the
cross-file scientific contract introduced by HR047 rather than re-testing numerical
unit behaviour already covered by JUnit. On the consolidated 0.10.2 branch it also
invokes HR048 drive, HR049 control/remote, HR050 field-operability and HR051 camera
recovery contracts so the release gate cannot pass with steering, latency, Remote,
sensor-visibility, reproducibility or camera-path drift.
"""
from pathlib import Path
import os
import re

ROOT = Path(__file__).resolve().parents[1]


def txt(path: str) -> str:
    p = ROOT / path
    if not p.is_file():
        raise AssertionError(f"missing required HR047 file: {path}")
    return p.read_text(encoding="utf-8")


def req(src: str, needle: str, label: str) -> None:
    if needle not in src:
        raise AssertionError(f"{label}: missing {needle!r}")


def forbid(src: str, needle: str, label: str) -> None:
    if needle in src:
        raise AssertionError(f"{label}: forbidden {needle!r}")


def main() -> int:
    gradle = txt("app/build.gradle")
    gas = txt("app/src/main/java/com/servoc/app/exposure/GasPpmExposureAssessment.java")
    priority = txt("app/src/main/java/com/servoc/app/exposure/GasHazardPriority.java")
    priority_test = txt("app/src/test/java/com/servoc/app/exposure/GasHazardPriorityTest.java")
    gas_test = txt("app/src/test/java/com/servoc/app/exposure/GasPpmExposureAssessmentTest.java")
    decision_repo = txt("app/src/main/java/com/servoc/app/chemcore/ChemCoreDecisionRepository.java")
    panel = txt("app/src/main/java/com/servoc/app/ui/GasSafetyFindingsPanel.java")
    analysis_tab = txt("app/src/main/java/com/servoc/app/ui/ChemCoreAnalysisTab.java")
    pdf = txt("app/src/main/java/com/servoc/app/report/ReportExporter.java")
    xlsx = txt("app/src/main/java/com/servoc/app/report/XlsxReportWriter.java")
    fl_boot = txt("app/src/main/java/com/servoc/app/exposure/GasFlammabilitySeedBootstrap.java")
    fl_import = txt("app/src/main/java/com/servoc/app/exposure/GasFlammabilitySeedImporter.java")
    r0_app = txt("app/src/main/java/com/servoc/app/calibration/FieldR0Baseline.java")
    r0_fw = txt("firmware/HazRob/Config.h")

    seed_sha = "457b9a1d88b8c54d8863772ca3741f8d2afd152ebd0a9fc007ccb106f4d50434"
    req(gradle, seed_sha, "Gradle flammability pin")
    req(gradle, "HazRob flammability seed SHA-256 mismatch", "Gradle fail-closed seed verification")
    req(gradle, "assets.srcDir generatedHazrobAssets", "APK offline seed packaging")
    req(fl_boot, "niosh_npg_flammability.json", "flammability bootstrap")
    req(fl_import, '"LFL"', "flammability importer LFL")
    req(fl_import, '"UFL"', "flammability importer UFL")

    req(gas, 'SCHEMA="hazrob-gas-exposure-ppm/2"', "gas safety schema")
    req(gas, 'unit_policy","TOXICOLOGY_PPM_ONLY_NO_MASS_CONCENTRATION_CONVERSION', "ppm-only policy")
    req(gas, 'if(!isPpmUnit(l.unit)){nonPpm++;continue;}', "non-ppm exclusion")
    req(gas_test, "massConcentrationLimitsAreExcludedWithoutConversion", "ppm-only regression")

    for needle in [
        'ppm_reportable',
        'quantitative_occupational_interpretation_permitted',
        '"QUANTIFIED"',
        '"M4_HAZROB_VALIDATED"',
        'chemical_uid_required',
    ]:
        req(gas, needle, "M4/reportability gate")
    req(decision_repo, "GasPpmExposureAssessment.evaluate", "session-bound gas safety enrichment")

    req(gas, "PPM_PER_VOL_PERCENT=10_000.0", "vol-percent conversion")
    req(gas, 'lfl_threshold_crossed', "LFL threshold state")
    req(gas, 'symbols(toxicReferenceCrossed,flame)', "hazard symbols")
    req(gas, 'if(skull)s.append("☠")', "skull marker")
    req(gas, 'if(flame)s.append("🔥")', "flame marker")
    req(gas_test, "lflUsesVolPercentToPpmAndFlameOnlyAtFullLfl", "LFL marker regression")

    req(priority, "enum Mode { HAZARD, CONCENTRATION }", "sort modes")
    req(priority, 'return mode==Mode.CONCENTRATION?"التركيز":"الخطر"', "Arabic sort labels")
    req(priority, 'hazard_priority_ratio', "hazard sort key")
    req(priority, 'return m==Mode.HAZARD?"hazard_priority_ratio_desc":"ppm_desc"', "sort descriptors")
    req(priority_test, "hazardSortPrioritizesSixPpmAtFivePpmLimitOverThreeHundredPpmAtOneThousandPpmLimit", "relative-hazard regression")
    req(priority_test, "concentrationSortIsAvailableWithoutChangingHazardScores", "concentration-view regression")

    req(panel, "GasHazardPriority.Mode.HAZARD", "default hazard UI")
    req(panel, "Mode.CONCENTRATION", "concentration UI option")
    req(analysis_tab, "GasSafetyFindingsPanel", "analysis-tab safety panel")
    forbid(priority, "posterior", "safety sorter must not mutate identity ranking")

    req(pdf, "GasPpmExposureAssessment.KEY", "PDF gas payload")
    req(pdf, "gasSafety(c,gas)", "PDF gas safety page")
    req(xlsx, '"سلامة الغاز"', "XLSX gas safety worksheet")
    req(xlsx, "gasSafetySheet(chemcore)", "XLSX session-bound gas safety")
    req(xlsx, 'sheet7.xml', "XLSX 7-sheet contract")

    req(r0_app, 'SESSION="FIELD-R0-3SESSION-20260913"', "app R0 campaign")
    req(r0_fw, 'R0_SESSION[]="FIELD-R0-3SESSION-20260913"', "firmware R0 campaign")
    app_vals = dict(re.findall(r'case "(mq\d+)": return ([0-9.]+);', r0_app))
    fw_vals = dict(re.findall(r'\{"(mq\d+)",\d,\d,([0-9.]+)f\}', r0_fw))
    if set(app_vals) != set(fw_vals) or len(app_vals) != 8:
        raise AssertionError(f"R0 channel-set mismatch app={sorted(app_vals)} fw={sorted(fw_vals)}")
    for key in app_vals:
        a, b = float(app_vals[key]), float(fw_vals[key])
        if abs(a-b) > max(1e-5, abs(a)*1e-6):
            raise AssertionError(f"R0 mismatch {key}: app={a} fw={b}")

    old_skip = os.environ.get("HR048_SKIP_INTEGRATION")
    os.environ["HR048_SKIP_INTEGRATION"] = "1"
    try:
        from audit_hr048_drive_contract import main as drive_main
        rc = drive_main()
        if rc not in (None, 0):
            raise AssertionError(f"HR048 drive contract returned {rc}")
    finally:
        if old_skip is None:
            os.environ.pop("HR048_SKIP_INTEGRATION", None)
        else:
            os.environ["HR048_SKIP_INTEGRATION"] = old_skip

    from audit_hr049_control_remote_contract import main as control_remote_main
    hr049_rc = control_remote_main()
    if hr049_rc not in (None, 0):
        raise AssertionError(f"HR049 control/remote contract returned {hr049_rc}")

    from audit_hr050_field_operability_contract import main as field_operability_main
    hr050_rc = field_operability_main()
    if hr050_rc not in (None, 0):
        raise AssertionError(f"HR050 field-operability contract returned {hr050_rc}")

    from audit_hr051_camera_contract import main as camera_main
    hr051_rc = camera_main()
    if hr051_rc not in (None, 0):
        raise AssertionError(f"HR051 camera recovery contract returned {hr051_rc}")

    print("HazRob HR-0101-047 gas-safety integration gate: PASS")
    print("Policy: reportable M4 ppm -> ppm-only health refs + pinned LFL/UFL -> relative hazard")
    print("Sort modes: hazard_priority_ratio_desc (default), ppm_desc (operator view)")
    print("R0 authority: FIELD-R0-3SESSION-20260913 synchronized app <-> ESP32 source")
    print("Consolidated release gate: HR048 PASS; HR049 PASS; HR050 PASS; HR051 PASS")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"HazRob HR-0101-047/048/049/050/051 release gate: FAIL: {exc}")
        raise SystemExit(1)
