# HazRob 0.10.1 — HR-0101-061 Field Failure T01

Date: 2026-09-16
Status: OPEN — FIELD FAILURE CONFIRMED

## Observed behavior

On physical startup, the intended HazRob main-controller SoftAP was not visible, while an ESP32-CAM Wi-Fi access point was visible.

## Required architecture

- Main ESP32 is the sole SoftAP authority during Direct boot.
- Main ESP32 must expose the controlled HazRob local network at 192.168.4.1.
- ESP32-CAM must be STA-only and join the HazRob network at canonical 192.168.4.2.
- ESP32-CAM must not create an independent fallback/test SoftAP in the HR061 production path.
- Every restart starts Direct; retained WAN credentials do not authorize Remote.

## Failure classification

HR061-T01: FAIL.

The source/integration qualification recorded before field testing is not physical acceptance. The field observation indicates that the flashed/runtime artifacts or an active CAM AP path do not satisfy the HR061 Direct-first contract.

## Corrective-action gate

Before a replacement field candidate is released:

1. Audit the exact main ESP32 production entrypoint for SoftAP startup and SSID authority.
2. Audit the exact ESP32-CAM production entrypoint and remove/disable any AP/AP+STA/test/fallback SoftAP path from the HR061 release path.
3. Confirm shared release identity and controlled SSID constants used by the artifacts actually built for 0.10.1.
4. Re-run source contract/release gates and compile both firmware targets from one exact SHA.
5. Flash both exact artifacts and repeat cold-boot T01.
6. Do not mark HR061 field-qualified until the main HazRob AP is visible and the CAM joins it as STA at 192.168.4.2.

No field-pass claim is made by this record.
