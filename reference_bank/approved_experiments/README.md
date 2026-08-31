# HazRob Approved Experiment Reference Bank

This directory is the immutable reference bank for experiments explicitly approved in the HazRob Android application.

## Workflow

1. Complete the ESP32 MQ measurement cycle.
2. Accept the measurement in HazRob so telemetry and analytics are frozen in SQLite.
3. In **Experiments**, select the record and choose **Approve for GitHub reference bank**.
4. HazRob marks the local record approved and exports a JSON file named `HazRob_APPROVED_<id>_<timestamp>.json`.
5. Upload that JSON file to this directory (optionally grouped by year/month).
6. Never overwrite an approved record. If analysis changes later, create a derived/new record and preserve the original.

## Integrity

Each exported record contains SHA-256 digests of the stored telemetry and analytics payloads. The app deliberately does **not** embed a GitHub personal access token. Automated GitHub sync should use OAuth or a GitHub App in a later release.

## Schema

Current schema identifier: `hazrob-approved-experiment/1`.
