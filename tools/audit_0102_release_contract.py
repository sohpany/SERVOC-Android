#!/usr/bin/env python3
"""Canonical HazRob 0.10.2 / HR069 release contract.

This is the authoritative cross-layer source audit for the 0.10.2 baseline.
Historical HR-named audits remain supplemental provenance/compatibility gates.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def txt(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def req(source: str, needle: str, label: str) -> None:
    if needle not in source:
        raise AssertionError(f"{label}: missing {needle!r}")


def forbid(source: str, needle: str, label: str) -> None:
    if needle in source:
        raise AssertionError(f"{label}: forbidden {needle!r}")


def main() -> int:
    main_activity = txt("app/src/main/java/com/servoc/app/MainActivity.java")
    entry = txt("app/src/main/java/com/servoc/app/EntryActivity.java")
    release = txt("app/src/main/java/com/servoc/app/ReleaseAudit.java")
    db = txt("app/src/main/java/com/servoc/app/data/ExperimentsDb.java")
    experiment_queries = txt("app/src/main/java/com/servoc/app/data/ExperimentQueries.java")
    hw = txt("firmware/HazRob/Hardware.cpp")
    cfg = txt("firmware/HazRob/Config.h")
    user_library = txt("app/src/main/java/com/servoc/app/field/HazRobUserLibrary.java")
    archive = txt("app/src/main/java/com/servoc/app/field/FieldSessionArchive.java")
    report = txt("app/src/main/java/com/servoc/app/report/FieldSessionReportExporter.java")
    sessions_tab = txt("app/src/main/java/com/servoc/app/ui/FieldSessionsTab.java")

    # Canonical product identity.
    req(release, 'VERSION="0.10.2"', "app release identity")
    req(release, 'ENGINEERING_REVISION="HR069"', "engineering revision authority")

    # Database authority must agree with the actual SQLiteOpenHelper schema.
    req(release, "DATABASE_SCHEMA=9", "release database schema")
    req(db, "DB_VERSION=9", "database implementation schema")
    req(db, "if(oldVersion<9)createV9UserFieldTables(db);", "schema-9 migration")

    # User-facing navigation and durable field library.
    req(
        main_activity,
        'String[] names={"القيادة","الجلسات","القياسات","التقارير","الحالة","الإعدادات","المطور"}',
        "0.10.2 navigation",
    )
    req(entry, "showFirstRunLibrarySetup()", "first-run durable library")
    req(entry, "HazRobUserLibrary.createPickerIntent()", "SAF library picker")
    req(user_library, "FieldSessionArchive.exportSession", "persistent source archive")
    req(user_library, "FieldSessionArchive.importSession", "persistent source restore")

    # User archive isolation and exact report lineage.
    req(archive, "m.operator_role='USER'", "user archive isolation")
    req(archive, "m.kind LIKE 'field_%'", "field archive isolation")
    forbid(archive, "chemcore_feature_references", "scientific reference isolation")
    req(archive, "FROM experiments WHERE session_id=?", "derived report source backup")
    req(archive, 'w.delete("experiments","session_id=?"', "idempotent report source restore")
    req(report, "measurementSessionIds(fs)", "field report exact measurement binding")
    req(report, "ExperimentQueries.bySessionId(db,sid)", "field report exact experiment binding")
    forbid(report, "rawQuery(", "field report SQL ownership")
    forbid(report, ";\\nimport ", "field report literal-newline source corruption")
    req(experiment_queries, "WHERE session_id=? ORDER BY id", "exact experiment query")
    forbid(report, "ORDER BY ABS", "field report timestamp fallback forbidden")
    forbid(experiment_queries, "ORDER BY ABS", "experiment query timestamp fallback forbidden")
    req(sessions_tab, 'requestFieldSessionExport("pdf",q.id)', "field PDF action")
    req(sessions_tab, 'requestFieldSessionExport("xlsx",q.id)', "field XLSX action")

    # Current physical safety contract.
    req(cfg, "ULTRA_WARN_CM=10.0f", "collision warning threshold")
    req(cfg, "ULTRA_STOP_CM=5.0f", "collision stop threshold")
    req(hw, "forwardBlocked=irWarning||ultraStop", "forward safety interlock")
    req(hw, r'\"forward_blocked\"', "safety telemetry")

    print("HazRob 0.10.2 / HR069 canonical release contract: PASS")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"HazRob 0.10.2 / HR069 canonical release contract: FAIL: {exc}")
        raise SystemExit(1)
