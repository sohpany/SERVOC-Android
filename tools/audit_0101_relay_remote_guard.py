#!/usr/bin/env python3
"""Fail-closed HR-0101-039 guard for relay boot safety and Remote MQTT diagnostics.

This is intentionally narrow and complements audit_0101_integration_contract.py.
It makes the HR038/HR039 invariants easy to re-run before distribution builds.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def txt(path: str) -> str:
    p = ROOT / path
    if not p.is_file():
        raise AssertionError(f"missing required file: {path}")
    return p.read_text(encoding="utf-8")


def need(hay: str, needle: str, where: str) -> None:
    if needle not in hay:
        raise AssertionError(f"{where}: missing invariant: {needle}")


def main() -> None:
    cfg = txt("firmware/HazRob/Config.h")
    hw = txt("firmware/HazRob/Hardware.h")
    ino = txt("firmware/HazRob/HazRob.ino")
    rm = txt("firmware/HazRob/RemoteMqtt.cpp")
    android = txt("app/src/main/java/com/servoc/app/communication/SecureMqtt311Client.java")
    session = txt("app/src/main/java/com/servoc/app/communication/RemoteSessionManager.java")

    # Canonical active-low relay authority must not drift.
    need(cfg, "constexpr uint8_t RELAY_MQ=25,RELAY_ON=LOW,RELAY_OFF=HIGH;", "Config.h")

    # OFF latch must be loaded before GPIO25 becomes an output, then asserted again.
    sequence = "digitalWrite(Cfg::RELAY_MQ,Cfg::RELAY_OFF);pinMode(Cfg::RELAY_MQ,OUTPUT);digitalWrite(Cfg::RELAY_MQ,Cfg::RELAY_OFF);"
    need(hw, sequence, "Hardware.h relayBootSafeInit")
    need(ino, "void setup(){relayBootSafeInit();hwBegin();startAP();remoteBegin();remoteMqttBegin();web.begin();}", "HazRob.ino setup")

    # Main ESP32 must distinguish authentication and subscription authorization stages.
    for marker in (
        "mqtt_connack_timeout",
        "mqtt_connack_malformed",
        "mqtt_connack_",
        "mqtt_suback_timeout",
        "mqtt_suback_malformed",
        "mqtt_suback_mismatch",
        "mqtt_suback_rejected",
        "mqtt_subscribed",
        "bootstrap_contract_mismatch",
    ):
        need(rm, marker, "RemoteMqtt.cpp")

    # Android must preserve CONNACK reason and fail on denied/missing SUBACK.
    for marker in (
        "MQTT CONNACK rejected:",
        "MQTT SUBACK timeout:",
        "MQTT disconnected before SUBACK:",
        "MQTT SUBACK rejected by ACL:",
        "MQTT SUBACK unexpected QoS",
    ):
        need(android, marker, "SecureMqtt311Client.java")

    # Readiness UI must preserve the real exception message rather than class-only text.
    need(session, 'e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage())', "RemoteSessionManager.java")

    print("HazRob 0.10.1 HR039 relay/remote guard: PASS")


if __name__ == "__main__":
    main()
