# HazRob 0.10.2 / HR069-R1 — direct BME wiring and internal replay matrix

Date: 2026-10-09
Only branch: `release/hazrob-0.10.2-rc2`
Status: **WORKING / UNAPPROVED**; code/host simulation is not field hardware acceptance.

## Operator-supplied hardware clarification

**BME280 is wired straight to ESP32**, without ADS1115, I²C voltage-level shifter, multiplexing board, or external bridge.
This is not a claim of independent ADC wiring: a BME280 still communicates digitally through its native **I²C** or SPI protocol. The **current firmware specifically selects the ESP32's secondary hardware I²C controller**, using SDA GPIO13 / SCL GPIO14 (3.3V logic), with GND common and properly supported power. The board-side actual GPIO routing and VCC must be confirmed by an on-robot pin check. No ADS or external converter may be introduced in software.
- `EnvWire(1).begin(GPIO13,GPIO14)` dedicated to BME/BMP.
- The separate ESP32 `Wire` bus on GPIO21/22 serves the ADS1115 gas converters.
- Sensor acquisition interval `ENV_PERIOD=1000` ms; Android polling may occur more frequently but must show fresh valid telemetry, not held/smoothed environmental data.
- A BME/BMP diagnostic `env.ok=false` must *never* be treated as a valid environmental correction for ChemCore.

## Executable internal tests added

**Firmware replay:** `tools/test_hr069_r1_hardware_host.py` extracts **actual C++ implementations** of `ultraEchoIsr`, `ultraUpdate`, `irUpdate`, `envRecoverBus`, `envUpdate` and `driveSet` from `firmware/HazRob/Hardware.cpp`, compiles them with host `g++ -std=c++17` using deterministic ESP32 time/GPIO/I²C stubs, and executes assertions.

| Test target | Simulated cases | Acceptance focus |
| --- | --- | --- |
| Echo HC-SR04 | 4, 5, 10 and 25cm pulse edges | Quantized time-of-flight conversion, 5cm conditional stop zone and 10cm warning |
| Echo failure paths | No rising edge, no falling edge, input HIGH before TRIG, invalid pulse duration | Disjoint cause/status counters, invalid distance is NaN, no invented range |
| Down-facing IR | HIGH signal then LOW signal across debounce windows | HIGH = ground; LOW = cliff, no automatic redefinition |
| Direct BME280 bus | Fresh 1000ms sample; one/two/three successive read failures; 3000ms reprobe | Only the **native ESP32 GPIO13/14 I²C** bus is recovered; no ADS path; failed sample is not held |
| Left/right tank motor | Independent signed (25,0), (0,-67), asymmetric (40,95), neutral (0,0) | `driveSet()` pair semantics; PWM/output still needs real ESP32 + L298N |
| Protection | Existing CI motion and runtime source audits | 500ms deadman, E-stop latch, optional auto-stop OFF by default |

**Android developer/report smoke:** `app/src/test/java/com/servoc/app/HR069R1DeveloperWorkflowSmokeContractTest.java` adds tests for these **source-level** expectations:
1. Developer PIN unlock and access to all five developer tabs — experiments, analytical lab, R0, connectivity, library.
2. Developer experiment start/cancel and lineage gates; only developer approved reference candidates may enter central review inbox, never auto-activate Stable or promote M4.
3. R0 edits prohibited during active acquisition or analysis, and electrical display units in kΩ.
4. ChemCore M4 quantification gate, persisted decision, mathematical explanation.
5. Developer connectivity authorize/diagnose/reconnect/disconnect/clear via async calls.
6. Library version status, bounded rollback and active-session update block.
7. Smart PDF and XLSX provenance and time-series structure.
8. Raw Echo failure classifications and direct BME bus recovery counters mapped from ESP32 to Android status panes, with no last-value hold.

**Previously present JUnit/regression suites** cover ChemCore response priors, fingerprint comparison, unknown/OOD gating, mixture residual guards, confidence calibration, field/site/developer session separation, historical R0, PDFs/XLSX structure, XML ZIP workbook validity, and chemical library authority. They are run as `gradle --no-daemon :app:testDebugUnitTest`; results must be checked for the **exact final source SHA**, not inferred.

### Programmatic limitations

- Host GPIO stubs do **not** measure actual sensor voltage, BME humidity or pressure accuracy, HC-SR04 echo electrical pulse edges, loaded motor PWM/current, Wi-Fi robustness, Android WebView/video smoothness, actual developer button taps, camera, cloud connectivity, USB or phone file sharing.
- Existing PDF writer tests check page topology and evidence gating; PDF visual pagination, fonts, RTL and file opening need device-based end-to-end checks.
- Real 1000ms BME updates are not made faster by 150ms Android polling; no averaging of environmental data is performed.
- Machine learning ChemCore tests do not validate hazardous-gas identification or ppm against traceable reference gas mixtures.
- Developer UI source-level tests do **not** represent a completed emulator Espresso / Robolectric touch-interaction run.

### Current workflow

Added host replay step to `.github/workflows/validate-0102-hr069-development.yml` (branch source tests); existing same workflow compiles ESP32 Main and ESP32-CAM and runs Android/chemistry JUnit suites. Signed diagnostic APK and internal Android validation are run by separate existing workflows. Link only immutable exact-source successful results.

**Outstanding:** external hardware bench test, app emulator/phone tests, representative real saved-session PDF/XLSX opening, data replay across Developer/USER roles, actual direct and remote transport. No final release authorization.


## Final CI evidence for latest software-tested candidate — 2026-10-09

**LATEST VALIDATED WORKING/UNAPPROVED CANDIDATE (NOT STABLE):** source SHA `6281d3eb7c1c961ce63b0b5e4ae0792874626720`, versionName `0.10.2`, versionCode `30`, engineering revision `HR069-R1`. The source and Android/firmware tests all succeeded at this **exact same commit**. Subsequent documentation or packaging commits do not change the compiled app/firmware code.

| Pipeline and immutable run | Result | Primary evidence |
| --- | --- | --- |
| [Signed debug diagnostic APK, run 37870936815](https://github.com/sohpany/SERVOC-Android/actions/runs/37870936815) | SUCCESS | APK artifact ID `11590871098`; Android signing verification `PASS`, package `com.servoc.app` versionCode `30`, versionName `0.10.2`; APK SHA-256 `87226e1749d632e2016332a2a204fec09705eb27ebf378f500a9f5a3b8886039` |
| [Independent Android internal validation, run 37870936746](https://github.com/sohpany/SERVOC-Android/actions/runs/37870936746) | SUCCESS | Android unit/source contract tests |
| [ESP32, ESP32-CAM, app tests and host C++ replay, run 37870937034](https://github.com/sohpany/SERVOC-Android/actions/runs/37870937034) | SUCCESS | **35 native-host firmware assertions PASS** (actual firmware Echo/IR/BME/drive functions against simulated GPIO/time/I²C), Android unit tests PASS, Main + CAM compilation PASS with pinned Arduino ESP32 core 3.3.11 |
| [Main ESP32 firmware ZIP, same run](https://github.com/sohpany/SERVOC-Android/actions/runs/37870937034/artifacts/11590647043) | AVAILABLE | artifact ID `11590647043`; internal firmware ZIP SHA-256 `28474851e87ea431f5beb38f03ff0c016625960ac9dd129132128d53043f55d6`, Main sketch 1,132,833 bytes (36% of allowed app partition) |
| [ESP32-CAM firmware ZIP, same run](https://github.com/sohpany/SERVOC-Android/actions/runs/37870937034/artifacts/11590297778) | AVAILABLE BUT **NO REFLASH NEEDED** | CAM source unchanged by this change set, compilation PASS |

The latest actual firmware source change is Echo pulse classification and direct BME `EnvWire` bus read error recovery. Android v30 forwards these diagnostics and shows operator information.

### Explicit exclusions and release criteria

- **Not field-accepted:** Echo physical pulse and 5V-to-3.3V ECHO protection, BME wiring/power in a live chamber, real distance measurement, L298N load handling, battery under power load, on-device PDF/XLSX rendering, developer Android tap flows, Remote WAN/Railway.
- BME wiring confirmed by user as **direct to ESP32 without ADS1115, external level shifter or multiplexer**. Current firmware uses ESP32 secondary I²C peripheral SDA=GPIO13, SCL=GPIO14; this is an internal protocol and does not imply external modules.
- **Development SHA is not a released stable version**. The controlled `vCode26` authority is not overridden by this record; `vCode30` is the latest successful **diagnostic candidate**.
- Never delete user local data during install; where permitted, install matching signed build on top, retaining application data.
- Main ESP32 **must be reprogrammed** to receive firmware Echo/BME diagnostics; **ESP32-CAM need not** be reprogrammed because CAM code was unchanged.
- The software E-STOP in current Android source still uses two-second hold. Avoid representing it as instant hardwired stop.
