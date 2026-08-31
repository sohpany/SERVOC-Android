# Approved Experiment JSON Schema (informative)

Required top-level fields:

- `schema`: `hazrob-approved-experiment/1`
- `status`: `APPROVED`
- `app`: `HazRob`
- `app_version`
- `experiment_id`
- `accepted_at_utc`
- `approved_at_utc`
- `protocol`
- `repository_target`
- `telemetry`: exact stored ESP32 telemetry snapshot
- `analytics`: exact stored HazRob analysis snapshot
- `integrity.telemetry_sha256`
- `integrity.analytics_sha256`

Approved files are immutable project evidence. Do not edit measurements or analytical results after export.
