#!/usr/bin/env python3
"""HazRob 0.10.2 / HR069 P8 arrow-only directional tracking contract."""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def txt(p): return (ROOT/p).read_text(encoding='utf-8')
def req(s,n,label):
    if n not in s: raise AssertionError(f'{label}: missing {n!r}')
def forbid(s,n,label):
    if n in s: raise AssertionError(f'{label}: forbidden {n!r}')

def main():
    estimator=txt('app/src/main/java/com/servoc/app/navigation/GasDirectionEstimator.java')
    overlay=txt('app/src/main/java/com/servoc/app/vision/HazardOverlayView.java')
    control=txt('app/src/main/java/com/servoc/app/ui/ControlTab.java')
    release=txt('app/src/main/java/com/servoc/app/ReleaseAudit.java')
    tests=txt('app/src/test/java/com/servoc/app/navigation/GasDirectionEstimatorTest.java')

    # Tracking is an Android-only relative bearing hypothesis, never a control authority.
    req(estimator,'POLICY_VERSION="gas-direction-assist/2-arrow-only"','P8 policy')
    req(estimator,'MqSensorAffinity.forChemical(chemical).mask','target-specific sensor affinity')
    req(estimator,'MAX_TELEMETRY_GAP_MS=2_000L','stale telemetry reset')
    req(estimator,'double linear=(left+right)/200.0','translation-only evidence')
    req(estimator,'speed>=MIN_TRANSLATION','minimum translation gate')
    req(estimator,'quality.optBoolean("valid",true)','canonical quality gate')
    req(estimator,'manufacturer_heater_protocol_valid','heater qualification gate')
    req(estimator,'DIRECTION_HYPOTHESIS','explicit hypothesis semantics')
    for forbidden in ['RobotApi','setDriveCommand','robotApi()','relayMask','mqPower','motionStop','emergencyStop']:
        forbid(estimator,forbidden,'estimator control isolation')

    # The operator HUD expresses direction only as an arrow.
    req(overlay,'private void drawGasArrow','gas arrow renderer')
    req(overlay,'float len=Math.min(frame.width(),frame.height())*0.24f','fixed arrow geometry')
    forbid(overlay,'Math.round(gasConfidence*100)','direction percentage must not be displayed')
    forbid(overlay,'drawLabel(c,"تتبع','tracking text must not be drawn in arrow overlay')
    req(control,'GasDirectionEstimator.Result g=gasDirection.update','ControlTab consumes estimator result')
    req(control,'if(g.valid)overlay.setGasDirection(g.angleRad,g.confidence);else overlay.clearGas();','arrow-only projection')
    req(control,'gasDirection.reset();return;','telemetry loss clears direction state')
    forbid(control,'MqRelayPlanner','tracking UI must not show relay recommendations')
    forbid(control,'bearingName(','tracking UI must not duplicate arrow as direction text')
    forbid(control,'100*g.confidence','tracking UI must not expose directional coherence as probability')

    # Release markers lock the product policy.
    req(release,'GAS_TRACKING_ARROW_ONLY=true','release arrow-only marker')
    req(release,'GAS_TRACKING_AUTONOMOUS_DRIVE=false','no autonomous tracking drive')
    req(release,'GAS_TRACKING_ABSOLUTE_LOCALIZATION=false','no absolute localization claim')
    req(release,'GAS_TRACKING_RELAY_RECOMMENDATION=false','no tracking relay recommendation')
    req(release,'GAS_TRACKING_RELAY_BANK_CONTROL=false','no tracking relay control')

    # Behavioral regression coverage: translation required; spin/stale/invalid/target-change fail closed.
    for name in [
        'risingGasEvidenceWhileDrivingForwardPointsForward',
        'noMotionDoesNotInventDirection',
        'spinInPlaceDoesNotInventDirection',
        'telemetryGapClearsDirectionalVotes',
        'targetChangeClearsPreviousDirection',
        'invalidCanonicalChannelsNeverCreateArrow',
        'unsupportedTargetFailsClosed']:
        req(tests,name,'P8 behavioral tests')

    print('HazRob 0.10.2 / HR069 P8 arrow-only tracking contract: PASS')
    return 0

if __name__=='__main__':
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f'HazRob 0.10.2 / HR069 P8 arrow-only tracking contract: FAIL: {exc}')
        raise SystemExit(1)
