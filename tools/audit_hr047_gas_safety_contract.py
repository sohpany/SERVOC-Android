#!/usr/bin/env python3
"""Fail-closed HR-0101-047 gas-safety/release integration gate.

This supplements audit_0101_integration_contract.py.  It deliberately checks the
cross-file scientific contract introduced by HR047 rather than re-testing numerical
unit behaviour already covered by JUnit.
"""
from pathlib import Path
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

    # 1) Controlled offline flammability seed must be pinned and fail-closed in the build.
    seed_sha = "457b9a1d88b8c54d8863772ca3741f8d2afd152ebd0a9fc007ccb106f4d50434"
    req(gradle, seed_sha, "Gradle flammability pin")
    req(gradle, "HazRob flammability seed SHA-256 mismatch", "Gradle fail-closed seed verification")
    req(gradle, "assets.srcDir generatedHazrobAssets", "APK offline seed packaging")
    req(fl_boot, "niosh_npg_flammability.json", "flammability bootstrap")
    req(fl_import, '"LFL"', "flammability importer LFL")
    req(fl_import, '"UFL"', "flammability importer UFL")

    # 2) Toxicological interpretation is gas/ppm-only; no implicit mass-unit conversion.
    req(gas, 'SCHEMA="hazrob-gas-exposure-ppm/2"', "gas safety schema")
    req(gas, 'unit_policy","TOXICOLOGY_PPM_ONLY_NO_MASS_CONCENTRATION_CONVERSION', "ppm-only policy")
    req(gas, 'if(!isPpmUnit(l.unit)){nonPpm++;continue;}', "non-ppm exclusion")
    req(gas_test, "massConcentrationLimitsAreExcludedWithoutConversion", "ppm-only regression")

    # 3) Quantitative safety is impossible before the complete M4/reportable identity gate.
    for needle in [
        'ppm_reportable',
        'quantitative_occupational_interpretation_permitted',
        '"QUANTIFIED"',
        '"M4_HAZROB_VALIDATED"',
        'chemical_uid_required',
    ]:
        req(gas, needle, "M4/reportability gate")
    req(decision_repo, "GasPpmExposureAssessment.evaluate", "session-bound gas safety enrichment")

    # 4) Flammability ratio and markers retain the exact HR047 semantics.
    req(gas, "PPM_PER_VOL_PERCENT=10_000.0", "vol-percent conversion")
    req(gas, 'lfl_threshold_crossed', "LFL threshold state")
    req(gas, 'symbols(toxicReferenceCrossed,flame)', "hazard symbols")
    req(gas, 'if(skull)s.append("☠")', "skull marker")
    req(gas, 'if(flame)s.append("🔥")', "flame marker")
    req(gas_test, "lflUsesVolPercentToPpmAndFlameOnlyAtFullLfl", "LFL marker regression")

    # 5) Relative hazard is the default safety order; absolute ppm remains an operator view only.
    req(priority, "enum Mode { HAZARD, CONCENTRATION }", "sort modes")
    req(priority, 'return mode==Mode.CONCENTRATION?"التركيز":"الخطر"', "Arabic sort labels")
    req(priority, 'hazard_priority_ratio', "hazard sort key")
    req(priority, 'return m==Mode.HAZARD?"hazard_priority_ratio_desc":"ppm_desc"', "sort descriptors")
    req(priority_test, "hazardSortPrioritizesSixPpmAtFivePpmLimitOverThreeHundredPpmAtOneThousandPpmLimit", "relative-hazard regression")
    req(priority_test, "concentrationSortIsAvailableWithoutChangingHazardScores", "concentration-view regression")

    # 6) Identity ranking remains separate; safety list has an explicit UI sort control.
    req(panel, "GasHazardPriority.Mode.HAZARD", "default hazard UI")
    req(panel, "Mode.CONCENTRATION", "concentration UI option")
    req(analysis_tab, "GasSafetyFindingsPanel", "analysis-tab safety panel")
    forbid(priority, "posterior", "safety sorter must not mutate identity ranking")

    # 7) The same session-bound enriched payload reaches both report formats.
    req(pdf, "GasPpmExposureAssessment.KEY", "PDF gas payload")
    req(pdf, "gasSafety(c,gas)", "PDF gas safety page")
    req(xlsx, '"سلامة الغاز"', "XLSX gas safety worksheet")
    req(xlsx, "gasSafetySheet(chemcore)", "XLSX session-bound gas safety")
    req(xlsx, 'sheet7.xml', "XLSX 7-sheet contract")

    # 8) App and main firmware carry the same accepted HR045 R0 campaign/value set.
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

    print("HazRob HR-0101-047 gas-safety integration gate: PASS")
    print("Policy: reportable M4 ppm -> ppm-only health refs + pinned LFL/UFL -> relative hazard")
    print("Sort modes: hazard_priority_ratio_desc (default), ppm_desc (operator view)")
    print("R0 authority: FIELD-R0-3SESSION-20260913 synchronized app <-> ESP32 source")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"HazRob HR-0101-047 gas-safety integration gate: FAIL: {exc}")
        raise SystemExit(1)
