# HazRob v0.7.0

HazRob 0.7.0 is the hardware-integration release for the revised robot electronics and ESP32-CAM path.

## Android

- Version code 10 / version name 0.7.0; same application id and prototype signing certificate as v0.6.2, so supported devices can update in place.
- Direct ESP32-CAM MJPEG remains on `http://192.168.4.2:81/stream` and now uses the buffered/latest-frame display reader introduced by the hardware-sync branch.
- IR telemetry is shown as a warning only; it is not a drive interlock.
- ESP32 retrace state is surfaced to the operator.
- Gas-source tracking remains an Android-side analytical aid. Selecting a target maps its relevant installed MQ channels to the minimum required powered bank(s):
  - Bank 1: MQ2, MQ4, MQ5, MQ8.
  - Bank 2: MQ3, MQ7, MQ9, MQ135.
  - Both banks are enabled only when the target affinity requires channels from both.
- Relay bank selection uses the additive API v3 `relayMask` contract.

## Main ESP32 hardware contract

- SSID `HazRob`, AP `192.168.4.1`.
- Two ADS1115 devices at `0x48` and `0x49` on SDA21/SCL22.
- Four active-low relay outputs; R1/R2 power the two four-sensor MQ banks, R3/R4 remain reserved.
- MAX4466 raw RMS/peak-to-peak telemetry is exposed in volts and is not labelled dB SPL without calibration.
- IR is warning-only telemetry.
- Up to 60 s of recent drive history may be locally retraced after command loss according to the firmware safety logic.

## ESP32-CAM

- Dedicated `firmware/HazRob-CAM/HazRob-CAM.ino` source is now tracked in the repository.
- Static station address `192.168.4.2` on the `HazRob` AP.
- QVGA RGB565 capture with software JPEG/MJPEG streaming and flash/status endpoints compatible with the Android camera client.

## Scientific boundary

The hardware and app integration does not make the MQ array selective by itself. PPM remains conditional on accepted calibration/evidence. MQ7/MQ9 quantitative use still requires a validated heater protocol for the exact hardware configuration.
