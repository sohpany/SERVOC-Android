#!/usr/bin/env bash
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
PUBLIC="$ROOT/public"
PKG="$PUBLIC/hr065-flash-package"
RUNTIME_SOURCE_COMMIT="0d935b9aeecec192a71c45520809bfc36c6f37e9"

mkdir -p "$PKG/main" "$PKG/cam" "$PKG/logs"

# Re-run the applicable non-RF/non-physical gates after successful compilation.
cd "$ROOT"
python3 tools/audit_0101_integration_contract.py | tee "$PKG/logs/audit_0101_integration_contract.log"
python3 tools/audit_0101_motion_contract.py | tee "$PKG/logs/audit_0101_motion_contract.log"
python3 tools/audit_0101_relay_remote_guard.py | tee "$PKG/logs/audit_0101_relay_remote_guard.log"
python3 tools/audit_hr047_gas_safety_contract.py | tee "$PKG/logs/audit_hr047_gas_safety_contract.log"
python3 tools/audit_hr048_drive_contract.py | tee "$PKG/logs/audit_hr048_drive_contract.log"
python3 tools/audit_hr049_control_remote_contract.py | tee "$PKG/logs/audit_hr049_control_remote_contract.log"
python3 tools/audit_hr050_field_operability_contract.py | tee "$PKG/logs/audit_hr050_field_operability_contract.log"

cp "$PUBLIC"/master/*.bin "$PKG/main/"
cp "$PUBLIC"/camera/*.bin "$PKG/cam/"
cp "$PUBLIC"/master/compile.log "$PKG/logs/main-compile.log"
cp "$PUBLIC"/camera/compile.log "$PKG/logs/cam-compile.log"
cp "$PUBLIC"/source/HazRob-ESP32-v0.10.1-source.zip "$PKG/"
cp "$PUBLIC"/source/HazRob-ESP32-CAM-v0.10.1-source.zip "$PKG/"

MAIN_MERGED="$(find "$PKG/main" -maxdepth 1 -type f -name '*.merged.bin' | head -n1)"
CAM_MERGED="$(find "$PKG/cam" -maxdepth 1 -type f -name '*.merged.bin' | head -n1)"
test -n "$MAIN_MERGED"
test -n "$CAM_MERGED"

grep -aFq 'HR-0101-065-MAIN' "$MAIN_MERGED"
grep -aFq 'HazRob 0.10.1 HR065' "$MAIN_MERGED"
grep -aFq 'HR-0101-065-CAM' "$CAM_MERGED"
grep -aFq 'HazRob-CAM 0.10.1 HR065' "$CAM_MERGED"
if grep -aFq 'ESP_D62F69' "$CAM_MERGED"; then
  echo 'FAIL: stale ESP_D62F69 identity found in CAM binary' >&2
  exit 1
fi

cp "$MAIN_MERGED" "$PKG/HazRob-Main-HR065-merged.bin"
cp "$CAM_MERGED" "$PKG/HazRob-CAM-HR065-merged.bin"

cat > "$PKG/BUILD-PROVENANCE.txt" <<EOF
HazRob 0.10.1 HR065 controlled flash package
runtime_source_commit=$RUNTIME_SOURCE_COMMIT
packaging_commit=${RENDER_GIT_COMMIT:-unknown}
arduino_esp32_core=3.3.11
main_build_id=HR-0101-065-MAIN
cam_build_id=HR-0101-065-CAM
main_ssid=HazRob
main_direct_ip=192.168.4.1
cam_mode=STA-only
cam_static_ip=192.168.4.2
drive_contract=HR048 LEFT->L298N-B RIGHT->L298N-A inversions=false
field_acceptance=REQUIRED
EOF

cat > "$PKG/FLASH-INSTRUCTIONS.txt" <<'EOF'
HazRob 0.10.1 HR065 - ESP32 + ESP32-CAM

MANDATORY BEFORE FLASHING
Perform a full flash erase on BOTH boards. This is especially important for ESP32-CAM so stale generic firmware cannot continue advertising an ESP_* access point.

PREFERRED FLASH METHOD
1. Main ESP32: flash HazRob-Main-HR065-merged.bin at address 0x0.
2. ESP32-CAM: flash HazRob-CAM-HR065-merged.bin at address 0x0.

EXPECTED NETWORK CONTRACT AFTER REBOOT
- Main ESP32 is the sole AP and advertises SSID HazRob.
- Main Direct IP is 192.168.4.1.
- ESP32-CAM never creates an AP and must not advertise ESP_*.
- ESP32-CAM joins HazRob as STA with static IP 192.168.4.2.
- Every reboot starts Direct. Saved WAN credentials do not auto-activate Remote.
- Remote requires explicit operator activation; returning Direct shuts down remote transport while retaining WAN credentials.

DRIVE CONTRACT
HR048 mapping is preserved: logical LEFT -> L298N channel B; logical RIGHT -> channel A; both inversions false.

FIELD ACCEPTANCE
Software/build gates cannot replace physical verification. After flashing confirm: only HazRob appears from the robot, CAM is reachable at 192.168.4.2, and forward/reverse/left/right motor behavior is correct before operational use.
EOF

(
  cd "$PKG"
  find . -type f ! -name SHA256SUMS.txt -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS.txt
)

rm -f "$PUBLIC/HazRob-0.10.1-HR065-ESP32-CAM-FLASH.zip" "$PUBLIC/HazRob-0.10.1-HR065-ESP32-CAM-FLASH.sha256"
(
  cd "$PKG"
  /usr/bin/zip -qr "$PUBLIC/HazRob-0.10.1-HR065-ESP32-CAM-FLASH.zip" .
)
sha256sum "$PUBLIC/HazRob-0.10.1-HR065-ESP32-CAM-FLASH.zip" | tee "$PUBLIC/HazRob-0.10.1-HR065-ESP32-CAM-FLASH.sha256"

echo "HR065 PACKAGE PASS: $PUBLIC/HazRob-0.10.1-HR065-ESP32-CAM-FLASH.zip"
