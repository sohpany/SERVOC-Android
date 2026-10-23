# HazRob wheeled RC2: Tank session/report parity without inherited crash
Revision: 2026-10-24.
Target: sohpany/SERVOC-Android@release/hazrob-0.10.2-rc2.
Reference: HazRob/HazRob-Tank@develop, docs/requirements/PHASE-1-ANDROID-MEASUREMENT-SESSIONS.md and V3-ANDROID-BACKLOG-2026-10-20.md.

Tank's two-pane [الإدارة, التقارير] and [جديدة, معتمدة] lifecycle is an approved V3 target, **not a completed independent Tank source implementation**. The Tank APK inherits older wheeled field-session code from a pinned source commit; the owner reports that Sessions crashes in both apps. Do not copy legacy Tank FieldSessionsTab source.

Changes in wheeled RC2:
- Two inner tabs: إدارة for site dropdown, dependent two-column sessions table (name/status) and contextual create/close/approve/retry/delete. تقارير for site dropdown, dependent approved-only sessions and site/session PDF/XLSX.
- Transactional explicit field approval: exactly one linked, finalized USER/FIELD capture; legacy CLOSED remains NEW until approved, never upgraded automatically. Existing user and scientific sessions untouched.
- Report bundles enforce approved-only status; site summaries include only approved sessions, preserve independent ppm validity and never merge ppm across sessions.
- Retry of a CLOSED, unapproved session deletes only local unapproved USER/FIELD attempt data after explicit warning; rejects protected scientific/reference records. Does not bypass firmware interlocks.
- Portable v1 JSON archives carry optional field_approval; restore validates linkage, status and one completed sample in the same transaction. A v1 archive without approval is NEW.
- RC2 original crash containment retained: stable site/session snapshots; non-reentrant refresh; aggregate count query; error logging; outer navigation catch. No raw shared Tank UI is copied.
- HazRob Tank repository, original robot logo, app identity, ChemCore models, firmware and developer/library segregation unchanged.

Limits:
- The wheeled ACTIVE/CLOSED acquisition container remains, with manual end/approval; the Tank V3 dedicated timed-acquisition flow is a separate integration goal. Thus UI/status/report parity is implemented, but complete Tank V3 workflow parity is not yet claimed.
- CI cannot yet be treated as passing: build and device checks required. Test first run, upgrade, no-site state, zero/one/multiple linked captures, approved only, retry rollback, SAF PDF/XLSX, offline navigation and Logcat of initial crash before release.
