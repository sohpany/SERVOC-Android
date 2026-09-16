# HazRob 0.10.1 — HR-0101-061 Release Qualification Status

Date: 2026-09-16
Scope: dev/hazrob-0.10.1

## Architecture acceptance criteria

1. DIRECT is the mandatory boot/default profile.
2. The main ESP32 is the sole HazRob SoftAP authority in DIRECT.
3. Visible robot SSID is `HazRob`; master local address is `192.168.4.1`.
4. ESP32-CAM is STA-only and uses canonical `192.168.4.2` on the HazRob subnet.
5. Saved WAN credentials are configuration only; they never authorize automatic REMOTE boot.
6. REMOTE is runtime-only and requires explicit operator action.
7. Local HazRob AP remains the local control authority while REMOTE transport is active.
8. Android Direct routing targets the canonical HazRob SSID/subnet rather than stale saved addresses.

## Source qualification reviewed 2026-09-16

- Main ESP32 network authority: PASS (source review).
- ESP32-CAM STA-only authority: PASS (source review).
- Android Direct routing contract: PASS (source review).
- Remote activation isolation / saved-WAN behavior: PASS (source review).
- Version identity: PASS (`HazRob 0.10.1`, `HazRob-CAM 0.10.1`, Android `0.10.1`, versionCode 21).
- HR061 dedicated automated contract exists and is enforced by both APK and firmware workflows.
- APK workflow verifies pinned scientific datasets, canonical ChemCore snapshot packaging, unit tests, release signing certificate and APK SHA-256.
- Firmware workflow records exact source SHA, compiles master/CAM from controlled production paths and hashes flash binaries.

## Controlled build caveat

The flammability seed is content-addressed by SHA-256. The repository does not currently contain `app/src/main/assets/generated/flammability/niosh_npg_flammability.json`; Gradle therefore recovers the exact controlled bytes from the previously accepted 0.10.1 APK and fails closed if the recovered bytes do not match the pinned SHA-256. This is reproducible with the accepted artifact endpoint available, but it is not a fully offline build.

## CI infrastructure blocker

Recent GitHub Actions attempts failed before runner allocation (no executed steps / no job log). This is classified separately from source or compilation failure. No APK/firmware artifact may be called qualified solely from those failed runs.

## Physical qualification status

HR061-T01 remains OPEN/FAIL from the prior field observation: a camera AP was visible while the required `HazRob` main-controller AP was not. The reviewed current source cannot intentionally produce a CAM AP in its production path, so exact artifact provenance and re-flashing both boards from one controlled SHA are mandatory before repeating T01.

## Final release gate

A 0.10.1 candidate may be labelled SOFTWARE-QUALIFIED only after:

- HR061 Direct-first audit PASS;
- 0.10.1 integration audit PASS;
- motion and HR047-HR050 release gates PASS;
- scientific dataset checks PASS;
- Android unit tests PASS;
- signed release APK build PASS and signing digest verification PASS;
- master ESP32 compile PASS;
- ESP32-CAM compile PASS;
- APK, source ZIP and firmware binary SHA-256 records captured from the same source SHA.

It may be labelled FIELD-QUALIFIED / FINAL only after physical cold-boot verification confirms:

- visible SSID `HazRob`;
- master reachable at `192.168.4.1`;
- CAM joins as STA at `192.168.4.2`;
- no CAM AP is visible;
- Direct control/video work without Internet;
- explicit REMOTE transition works without removing local HazRob authority;
- reboot returns to DIRECT regardless of saved WAN credentials.

Current disposition: SOURCE REVIEW QUALIFIED; BUILD/CI AND PHYSICAL ACCEPTANCE PENDING.
