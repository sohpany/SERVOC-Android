#!/usr/bin/env python3
"""Compatibility entry point for the camera release gate.

HR065 changed the controlled ESP32-CAM runtime identity and hardened it to fail-closed STA-only.
Keep the historical HR051 entry point for release workflows, but execute the current HR065 camera
/network authority contract so stale pre-HR065 expectations cannot reject or weaken the release.
"""
from audit_hr065_camera_contract import main

if __name__ == "__main__":
    raise SystemExit(main())
